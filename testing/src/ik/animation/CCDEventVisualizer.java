package ik.animation;

import ik.animation.eventVisualizer.Board;
import ik.animation.eventVisualizer.EventCell;
import ik.animation.eventVisualizer.Slot;
import ik.basic.Util;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.animation.*;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.CCDSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.*;

public class CCDEventVisualizer extends PApplet {
    /*This example shows how to visualize the Interesting events of an IK Solver in order to
    * interactively define the starting time, the execution duration and the rendering duration of each of them.
    * */

    /*(*) Define a Visualizer to your algorithm.
    Here, you must specify the way that each interesting event annotated on the solver will be mapped to a visual step
     */
    class CCDVisualizer extends Visualizer {
        public CCDVisualizer(Scene scene, float radius, long period, long stepDuration) {
            super(scene, radius, period, stepDuration);
        }

        @Override
        public VisualStep eventToVizMapping(InterestingEvent event){
            return EventToViz.generateDefaultViz(this, event, setVisualFeatures(event));
        }

        @Override
        public HashMap<String, Object> setVisualFeatures(InterestingEvent event){
            //In this basic example we customize the aspect of the rotation step:
            HashMap<String, Object> attribs = null;
            if(event.type() == "NodeRotation"){
                attribs = new HashMap<String, Object>();
                attribs.put("color", color(0,255,0));
            }
            return attribs;
        }
    }

    int rows = 10, cols = 50;
    Scene eventScene, animScene, ikScene;
    Board board;

    //Define the Solver attributes
    int numJoints = 7;
    float targetRadius = 7;
    float boneLength = 50;
    CCDSolver solver; //IK Algorithm that uses Solver template
    CCDVisualizer visualizer; //A Visualizer manages a scene in which the IK algorithm will be animated
    VisualizerMediator mediator; //Since the interaction between a solver and a Visualizer is bidirectional a mediator is required to handle the events
    ArrayList<Node> structure = new ArrayList<>(); //Keep Structures


    public void settings() {
        size(1200, 800, P2D);
    }

    public void setup() {
        //Set Kinematic scene
        ikScene = new Scene(this, P3D, width, height , 0, 0);
        //create target
        Node target = new Node(animScene);
        structure = Util.generateChain(ikScene, numJoints, targetRadius * 0.8f, boneLength, new Vector(), color(255));
        solver = new CCDSolver(structure);
        solver.enableMediator(true);
        //Do only on iteration as we care just about plain events
        solver.setTimesPerFrame(1);
        solver.setMaxIterations(1);
        solver.setTarget(structure.get(numJoints - 1), target);
        //Move the target to any position
        target.setPosition(Vector.multiply(Vector.random(), random(10,numJoints * boneLength)));

        //set anim scene
        animScene = new Scene(this, P3D, width, height , 0, 0);
        //Create the visualizer
        visualizer = new CCDVisualizer(animScene, targetRadius * 0.8f, 40, 480);
        //Create Mediator that relates a Solver with at least one visualizer
        mediator = new VisualizerMediator(solver);

        //Set the event scene (only one that cares visually)
        eventScene = new Scene(this);
        eventScene.setRadius(height/2.f);
        eventScene.fit();
        //Setting the board
        board = new Board(eventScene, rows, cols);
        float rh = eventScene.radius(), rw = rh*eventScene.aspectRatio();
        board.setDimension(-rw, -rh, 10*rw, 2*rh);

        //set eye constraint
        eventScene.eye().setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                Vector v = Vector.add(node.translation(), translation);
                if(v.x() < -5) v.setX(-5);
                if(v.y() < -5) v.setY(-5);

                return Vector.subtract(v,node.translation());
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                return new Quaternion(); //no rotation is allowed
            }
        });

        //Adding some cells
        addEventsToBoard(solver, mediator, board);
    }

    public void addEventsToBoard(Solver solver, VisualizerMediator mediator, Board board){
        solver.solve(); //solve once
        //find and use events
        Set<String> names = new HashSet<String>();
        for(InterestingEvent event: mediator.eventQueue()){
            if(!names.contains(event.name())) {
                new EventCell(board, event.name(), event.startingTime(), event.executionDuration(), event.renderingDuration());
                names.add(event.name());
            }
        }
    }


    public void draw() {
        background(0);
        eventScene.render();
    }

    public void keyPressed() {
        if(key == 'r'){
            board.addRows(1, true);
        } else if (key == 'c') {
            board.addCols(1, true);
        }

    }



    public void mouseMoved(){
        eventScene.cast();
    }

    public void mouseDragged(){
        if(eventScene.trackedNode() instanceof Slot){
            eventScene.trackedNode().interact("OnMovement", new Vector(eventScene.mouse().x(), eventScene.mouse().y()));
        } else {
            eventScene.translate();
        }
    }

    public void mouseReleased(){
        if(eventScene.trackedNode() instanceof EventCell){
            //we must align the event to the closest row / col
            ((EventCell) eventScene.trackedNode()).applyMovement();
        } else if(eventScene.trackedNode() instanceof Slot){
            eventScene.trackedNode().interact("OnFinishedMovement", new Vector(eventScene.mouse().x(), eventScene.mouse().y()));
        }
    }

    public void mouseWheel(MouseEvent event) {
        if(eventScene.trackedNode() == null) eventScene.scale(event.getCount() * 50);
    }


    public static void main(String[] args) {
        PApplet.main(new String[]{"ik.animation.CCDEventVisualizer"});
    }
}
