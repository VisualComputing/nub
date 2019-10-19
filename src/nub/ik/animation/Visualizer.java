package nub.ik.animation;

import nub.core.Node;
import nub.ik.visual.Joint;
import nub.processing.Scene;

import java.util.*;

/**
* A Visualizer allows us to generate a smoothly animation of an IK Algorithm
 * highlighting key steps executed by it.
 * Here we annotate the interesting events and relate them to a visual representation.
* */

public class Visualizer {
    protected List<VisualStep> _steps = new ArrayList<VisualStep>(); //Execution flow
    protected HashMap<Node, Joint> _nodeToJoint = new HashMap<Node, Joint>();
    protected float _radius;


    protected VisualizerMediator _mediator;

    protected Scene _scene;

    protected long _next = 0; //Event counter
    protected long _period, _stepStamp, _time = 0;

    public Visualizer(Scene scene, float radius, long period, long stepDuration){
        _scene = scene;
        _radius = radius;
        _period = period;
        _stepStamp = stepDuration;
    }

    public long period(){
        return _period;
    }


    public void execute(){
        _initSteps();
        _executeSteps();
        if(_next < _mediator.lastEvent()) _time += _period;
    }

    protected void _initSteps(){
        //Get next events at given time
        while(_next < _mediator.lastEvent() && _time == _mediator.event(_next).startingTime() * _stepStamp){
            VisualStep step = eventToVizMapping( _mediator.event(_next));
            step.initialize();
            //add to steps to be drawn
            _steps.add(step);
            _next++;
        }
    }

    protected void _executeSteps(){
        for(int i = _steps.size() - 1; i >= 0; i--){
            _steps.get(i).execute();
            if(!_steps.get(i).keepDrawing() && _steps.get(i).completed()){
                _steps.remove(i);
            }
        }
    }

    public void reset(){
        _time = 0;
        _next = 0;
        _steps.clear();
    }


    public void render(){
        for(int i = _steps.size() - 1; i >= 0; i--){
            if(_steps.get(i).keepDrawing()) _steps.get(i).render();
        }
    }

    public void setMediator(VisualizerMediator mediator){
        _mediator = mediator;
    }

    //TODO: ADD "SAME" OPTION i.e animate the same structure instead of create a new one
    //Register the structures to Animate
    public void registerNode(Node node){
        Joint joint = new Joint(_scene, _scene.pApplet().color(255, 0, 0));
        joint.setRoot(true);
        joint.setPosition(node.position().get());
        joint.setOrientation(node.orientation().get());
        joint.setConstraint(node.constraint());
        _nodeToJoint.put(node, joint);
    }

    public void registerStructure(Node root){
        _generateBranch(root);
    }

    public void registerStructure(List<? extends Node> structure){
        _generateChain(structure);
    }

    //TODO: Move this constructor
    protected Joint _generateBranch(Node root){
        Joint joint = new Joint(_scene, _radius);
        joint.setRoot(true);
        joint.setPosition(root.position().get());
        joint.setOrientation(root.orientation().get());
        joint.setConstraint(root.constraint());
        if(root instanceof Joint){
            joint.setColor(((Joint) root).color());
        }
        _nodeToJoint.put(root, joint);
        _addBranch(joint, root);
        return joint;
    }

    protected void _addBranch(Joint reference, Node node){
        //here we assume that node is attached to a graph
        for(Node child : node.children()){
            Joint joint = new Joint(_scene, _radius);
            child.setReference(reference);
            child.setTranslation(node.translation().get());
            child.setRotation(node.rotation().get());
            child.setConstraint(node.constraint());
            if(node instanceof Joint){
                joint.setColor(((Joint) node).color());
            }
            _nodeToJoint.put(child, joint);
            _addBranch(joint, child);
        }
    }

    protected Joint _generateChain(List<?  extends Node> chain){
        Joint root = new Joint(_scene, _radius);
        root.setPosition(chain.get(0).position().get());
        root.setOrientation(chain.get(0).orientation().get());
        root.setRoot(true);
        if(chain.get(0) instanceof Joint){
            root.setColor(((Joint) chain.get(0)).color());
        }
        _nodeToJoint.put(chain.get(0), root);
        for(int i = 1; i < chain.size(); i++){
            Node current = chain.get(i);
            Joint joint = new Joint(_scene, _radius);
            Joint ref = _nodeToJoint.get(current.reference());
            joint.setReference(ref);
            joint.setPosition(current.position().get());
            joint.setOrientation(current.orientation().get());
            joint.setConstraint(current.constraint());
            if(current instanceof Joint){
                joint.setColor(((Joint) current).color());
            }
            _nodeToJoint.put(current, joint);
        }
        return root;
    }

    public void resetStructures(){
        for(Map.Entry<Node, Joint> entry : _nodeToJoint.entrySet()){
            entry.getValue().setOrientation(entry.getKey().orientation().get());
            entry.getValue().setPosition(entry.getKey().position().get());
        }
    }

    public void jumpTo(long nextStep){
        nextStep = Math.min(nextStep, _mediator.lastEvent());
        while(_next < nextStep){
            if(_next < _mediator.lastEvent() && _time == _mediator.event(_next).startingTime() * _stepStamp){
                VisualStep step = eventToVizMapping( _mediator.event(_next));
                step.initialize();
                //add steps to be drawn
                _steps.add(step);
                _next++;
            }
            _executeSteps();
            if(_next < _mediator.lastEvent() && _mediator.event(_next).startingTime() * _stepStamp >  _time){
                _time += _period;
            }
        }
    }

    protected void setTime(long t){
        _time = t;
    }

    /*
    * This method must establish the mapping between an event of a given type to a Visual Step
    * Here a default mapping is provided
    * */
    public VisualStep eventToVizMapping(InterestingEvent event){
        return EventToViz.generateDefaultViz(this, event, setVisualFeatures(event));
    }

    /*
     * Use this method to set up the visual features of the given step. To do so consider the Type or the Id of the
     * Event
     * */
    public HashMap<String, Object> setVisualFeatures(InterestingEvent event){
        return null;
    }

    public HashMap<Node, Joint> nodeToJoint(){
        return _nodeToJoint;
    }
}
