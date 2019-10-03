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
    protected List<InterestingEvent> _events = new ArrayList<InterestingEvent>();
    protected List<VisualStep> _steps = new ArrayList<VisualStep>(); //Execution flow
    protected List<VisualStep> _drawingQueue = new ArrayList<VisualStep>(); //Steps to be drawn (even if they have no more updates)
    protected List<Long> _drawingTimers = new ArrayList<Long>();

    protected HashMap<String, Joint> _structures = new HashMap<String, Joint>();
    protected HashMap<Node, Joint> _nodeToJoint = new HashMap<Node, Joint>();
    protected float _radius;

    protected Scene _scene;

    protected int _next = 0;
    protected long _period, _stepDuration, _wait = 0;

    public Visualizer(Scene scene, float radius, long period, long stepDuration){
        _scene = scene;
        _radius = radius;
        _period = period;
        _stepDuration = stepDuration;
    }


    public void execute(){
        _initSteps();
        _executeSteps();
        _wait+=_period;
    }

    protected void _initSteps(){
        //Get next events at given time
        while(_next < _events.size() && _wait == _events.get(_next).waitDuration() * _stepDuration){
            VisualStep step = eventToVizMapping(_events.get(_next));
            step.initialize();
            //add to steps to be drawn
            _drawingQueue.add(step);
            _drawingTimers.add(0L);
            _steps.add(step);
            _next++;
            _wait = 0;
        }
    }

    protected void _executeSteps(){
        for(int i = _steps.size() - 1; i >= 0; i--){
            _steps.get(i).execute();
            if(_steps.get(i).completed()) _steps.remove(i);
        }
    }

    public void reset(){
        _wait = 0;
        _next = 0;
    }


    public void render(){
        for(int i = _drawingQueue.size() - 1; i >= 0; i--){
            System.out.println("i :" + i + "timer : " + _drawingTimers.get(i) + "rend : " + _drawingQueue.get(i)._renderingDuration + " s dur" + _stepDuration);

            if(_drawingTimers.get(i) == _drawingQueue.get(i)._renderingDuration) {
                _drawingQueue.remove(i);
                _drawingTimers.remove(i);
            }else{
                _drawingQueue.get(i).render();
                _drawingTimers.set(i, _drawingTimers.get(i) + _period);
            }
        }
    }


    //TODO: ADD "SAME" OPTION i.e animate the same structure instead of create a new one
    //Register the structures to Animate
    public void registerStructure(String name, Node root){
        Joint joint = _generateBranch(root);
        _structures.put(name, joint);
    }

    public void registerStructure(String name, List<? extends Node> structure){
        Joint joint = _generateChain(structure);
        _structures.put(name, joint);
    }

    //TODO: Move this constructor
    protected Joint _generateBranch(Node root){
        Joint joint = new Joint(_scene);
        joint.setRoot(true);
        joint.setPosition(root.position().get());
        joint.setOrientation(root.orientation().get());
        joint.setConstraint(root.constraint());
        _nodeToJoint.put(root, joint);
        _addBranch(joint, root);
        return joint;
    }

    protected void _addBranch(Joint reference, Node node){
        //here we assume that node is attached to a graph
        for(Node child : node.children()){
            Joint joint = new Joint(_scene);
            child.setReference(reference);
            child.setTranslation(node.translation().get());
            child.setRotation(node.rotation().get());
            child.setConstraint(node.constraint());
            _nodeToJoint.put(child, joint);
            _addBranch(joint, child);
        }
    }

    protected Joint _generateChain(List<?  extends Node> chain){
        Node prev = chain.get(0);
        Joint root = new Joint(_scene, _radius);
        root.setPosition(prev.position().get());
        root.setOrientation(prev.orientation().get());
        root.setRoot(true);
        _nodeToJoint.put(prev, root);
        for(int i = 1; i < chain.size(); i++){
            Node current = chain.get(i);
            Joint joint = new Joint(_scene, _radius);
            Joint ref = _nodeToJoint.get(prev);
            joint.setReference(ref);
            joint.setPosition(current.position().get());
            joint.setOrientation(current.orientation().get());
            joint.setConstraint(current.constraint());
            _nodeToJoint.put(current, joint);
            prev = current;
        }
        return root;
    }

    public void addEvent(InterestingEvent event){
        //Insert the event at the corresponding position o(n)
        _events.add(event);
        /*for(int i = _events.size() -1; i >= 0; i--){
            if(event.init() >= _events.get(i).init()){
                if(i + 1 < _events.size()) _events.add(i + 1, event);
                else _events.add(event);
                break;
            }
        }*/
    }

    public List<InterestingEvent> events(){
        return _events;
    }


    /*
    * This method must establish the mapping between an event of a given type to a Visual Step
    * Here a default mapping is provided
    * */
    public VisualStep eventToVizMapping(InterestingEvent event){
        return EventToViz.generateDefaultViz(this, event);
    }

}
