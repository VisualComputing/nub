package ik.animation;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Node;
import nub.ik.animation.*;
import nub.ik.solver.evolutionary.GASolver;
import nub.ik.visual.Joint;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GASolverAnim  extends PApplet {
    /*
     * This example shows how to visualize an IK  Algorithm step by step
     * */

    /*
    (*) Define a Visualizer to your algorithm.
    Here, you must specify the way that each interesting event annotated on the solver will be mapped to a visual step
     */
    class GAVisualizer extends Visualizer {
        public GAVisualizer(Scene scene, float radius, long period, long stepDuration) {
            super(scene, radius, period, stepDuration);
        }

        /*
        Override this method is critical, Although EventToViz contains a default way to map an Interesting event into a
        visual step, there could be times in which you require to generate your own mapping
         */
        @Override
        public VisualStep eventToVizMapping(InterestingEvent event){
            return EventToViz.generateDefaultViz(this, event, setVisualFeatures(event));
        }

        /*
         * Use this method to customize the aspect of a visual step, take into account the Type of the Events or their ids
         * */
        @Override
        public HashMap<String, Object> setVisualFeatures(InterestingEvent event){
            //In this basic example we customize the aspect of the rotation step:
            HashMap<String, Object> attribs = null;
            if(event.type() == "HighlightStructure" || event.type() == "HideStructure"){
                attribs = new HashMap<String, Object>();
                if(event.name().toLowerCase().contains("best")) {
                    attribs.put("highlight", color(0, 255, 0));
                }else if(event.name().toLowerCase().contains("parent")){
                    attribs.put("highlight", color(0, 0, 255));
                }else if(event.name().toLowerCase().contains("child")){
                    attribs.put("highlight", color(255, 0, 0));
                }
            }
            return attribs;
        }
    }


    Scene scene, auxiliar, focus;
    boolean displayAuxiliar = false, anim = false;

    int numJoints = 7;
    float targetRadius = 7;
    float boneLength = 50;

    int color;

    GASolver solver; //IK Algorithm that uses Solver template
    GAVisualizer visualizer; //A Visualizer manages a scene in which the IK algorithm will be animated
    VisualizerMediator mediator; //Since the interaction between a solver and a Visualizer is bidirectional a mediator is required to handle the events

    ArrayList<Node> structure = new ArrayList<>(); //Keep Structures
    Node target; //Keep targets
    boolean solve = false;



    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(numJoints * boneLength * 0.8f);
        scene.fit(1);
        scene.setRightHanded();

        //(*) The auxiliar Scene is where the animation will take place
        auxiliar = new Scene(this, P3D, width, height , 0, 0);
        auxiliar.setType(Graph.Type.ORTHOGRAPHIC);
        auxiliar.setRadius(numJoints * boneLength);
        auxiliar.setRightHanded();
        auxiliar.fit();

        PShape redBall = createShape(SPHERE, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        //create targets
        target = new Node(scene, redBall);
        target.setPickingThreshold(0);

        //create skeleton
        color = color(212,0,255);
        structure = Util.generateChain(scene, numJoints, targetRadius * 0.8f, boneLength, new Vector(), color);

        solver = new GASolver(structure, 10);
        solver.enableMediator(true);

        //Create the visualizer
        visualizer = new GAVisualizer(auxiliar, targetRadius * 0.8f, 40, 800);

        //Create Mediator that relates a Solver with at least one visualizer
        mediator = new VisualizerMediator(solver, visualizer);

        //all structures but main of visualizer must have a black color
        for(Map.Entry<Node, Joint> entry : visualizer.nodeToJoint().entrySet()){
            if(!structure.contains(entry.getKey())){
                entry.getValue().setColor(color(0,0,255));
                entry.getValue().setAlpha(0);
            }
        }

        solver.setMaxError(0.001f);
        solver.setTimesPerFrame(1);
        solver.setMaxIterations(200);
        //7. Set targets
        solver.setTarget(structure.get(numJoints - 1), target);
        target.setPosition(structure.get(numJoints - 1).position());

        //Defines a task to run the solver each 40 ms
        TimingTask task = new TimingTask(scene) {
            @Override
            public void execute() {
                if(solve) {
                    solver.solve();
                }
            }
        };
        task.run(40);

        //Defines a task to run the animation each 40 ms
        TimingTask animTask = new TimingTask(scene) {
            @Override
            public void execute() {
                if (anim) {
                    visualizer.execute();
                }
            }
        };
        animTask.run(visualizer.period());
        //Define Text Font
        textFont(createFont("Zapfino", 38));
    }

    public void draw() {
        focus = displayAuxiliar ? auxiliar : scene;
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        scene.beginHUD();
        Util.printInfo(scene, solver, structure.get(0).position());

        if(displayAuxiliar) {
            auxiliar.beginDraw();
            auxiliar.context().lights();
            auxiliar.context().background(0);
            auxiliar.drawAxes();
            auxiliar.render();
            visualizer.render();
            auxiliar.endDraw();
            auxiliar.display();
        }
        scene.endHUD();

    }



    @Override
    public void mouseMoved() {
        focus.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            focus.spin();
        } else if (mouseButton == RIGHT) {
            focus.translate();
        } else {
            focus.scale(mouseX - pmouseX);
        }
    }

    public void mouseWheel(MouseEvent event) {
        focus.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 2)
            if (event.getButton() == LEFT)
                focus.focus();
            else
                focus.align();
    }

    public void keyPressed(){
        if(key == 'w' || key == 'W'){
            solve = !solve;
        } else if(key == 'q' || key == 'Q'){
            displayAuxiliar = true;
            solver.solve();
            anim = true;
        } else if(key == ' '){
            displayAuxiliar = !displayAuxiliar;
        } else if(key == 's'){
            solver.solve();
        } else if(Character.isDigit(key)){
            if(visualizer != null) visualizer.setStepDuration(Integer.valueOf("" + key) * 1000);
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.animation.GASolverAnim"});
    }
}