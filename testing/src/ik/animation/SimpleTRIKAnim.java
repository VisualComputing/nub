package ik.animation;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Hinge;
import nub.ik.animation.*;
import nub.ik.solver.geometric.oldtrik.TRIK;
import nub.ik.visual.Joint;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimpleTRIKAnim extends PApplet {
    /*
     * This example shows how to visualize an IK  Algorithm step by step
     * */

    /*
    (*) Define a Visualizer to your algorithm.
    Here, you must specify the way that each interesting event annotated on the solver will be mapped to a visual step
     */
    class TRIKVisualizer extends Visualizer {
        public TRIKVisualizer(Scene scene, float radius, long period, long stepDuration) {
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
            return null;
        }
    }


    Scene scene, auxiliar, focus;
    boolean displayAuxiliar = false, anim = false;

    int numJoints = 7;
    float targetRadius = 7;
    float boneLength = 50;

    int color;

    TRIK solver; //IK Algorithm that uses Solver template
    TRIKVisualizer visualizer; //A Visualizer manages a scene in which the IK algorithm will be animated
    VisualizerMediator mediator; //Since the interaction between a solver and a Visualizer is bidirectional a mediator is required to handle the events

    List<Node> structure = new ArrayList<>(); //Keep Structures
    Node target; //Keep targets
    boolean solve = false;



    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        Joint.axes = true;

        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(numJoints * boneLength * 0.8f);
        scene.fit(1);
        //scene.setRightHanded();

        //(*) The auxiliar Scene is where the animation will take place
        auxiliar = new Scene(this, P3D, width, height , 0, 0);
        auxiliar.setType(Graph.Type.ORTHOGRAPHIC);
        auxiliar.setRadius(numJoints * boneLength);
        //auxiliar.setRightHanded();
        auxiliar.fit();

        PShape redBall = createShape(SPHERE, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        //create targets
        target = new Node(scene){
            @Override
            public void graphics(PGraphics pg){
                pg.shape(redBall);
                scene.drawAxes(pg,targetRadius * 1.5f);
            }
        };
        target.setPickingThreshold(0);

        //create skeleton
        color = color(212,0,255);
        //structure = Util.generateChain(scene, numJoints, targetRadius * 0.8f, boneLength, new Vector(), color);
        structure = createLimb(scene, numJoints, boneLength, targetRadius * 0.8f, color(0,255,0), new Vector());

        solver = new TRIK(structure);
        solver.enableMediator(true);

        //Create the visualizer
        visualizer = new TRIKVisualizer(auxiliar, targetRadius * 0.8f, 40, 1000);

        //Create Mediator that relates a Solver with at least one visualizer
        mediator = new VisualizerMediator(solver, visualizer);

        solver.setMaxError(0.001f);
        solver.setTimesPerFrame(20);
        solver.setMaxIterations(200);
        //7. Set targets
        solver.setTarget(structure.get(structure.size() - 1), target);
        target.setPosition(structure.get(structure.size() - 1).position());

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

    public List<Node> createLimb(Scene scene, int segments, float length, float radius, int color, Vector translation){
        Node reference = new Node(scene);
        reference.translate(translation);

        Joint initial = new Joint(scene, color(255,0,0), radius);
        initial.setRoot(true);
        initial.setReference(reference);

        ArrayList<Node> joints = new ArrayList<>();
        Joint root = new Joint(scene, color, radius);
        //root.setReference(reference);
        joints.add(root);
        for(int i = 0; i < max(segments, 2); i++){
            Joint middle = new Joint(scene, color, radius);
            middle.setReference(joints.get(i));
            middle.translate(0, length, 0);
            if(i < max(segments, 2) - 1) {
                float max = 3;
                float min = 45;

                Hinge hinge = new Hinge(radians(min), radians(max), middle.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0));
                middle.setConstraint(hinge);
            }
            joints.add(middle);
        }
        BallAndSocket cone = new BallAndSocket(radians(20),radians(20));
        cone.setRestRotation(joints.get(joints.size() - 1).rotation().get(), new Vector(0,-1,0), new Vector(0,0,1));
        joints.get(joints.size() - 1).setConstraint(cone);

        Joint low = new Joint(scene, color, radius);
        low.setReference(joints.get(joints.size() - 1));
        low.translate(0,0,length);

        joints.add(low);
        root.translate(translation);
        root.setConstraint(new Hinge(radians(60), radians(60), root.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0)));
        return joints;
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
        focus.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            focus.mouseSpin();
        } else if (mouseButton == RIGHT) {
            focus.mouseTranslate();
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
        } else if(key == 'q'){
            displayAuxiliar = true;
            solver.solve();
            anim = true;
        } else if(key == ' '){
            displayAuxiliar = !displayAuxiliar;
        } else if(key == 's'){
            solver.solve();
        } else if(Character.isDigit(key)){
            if(visualizer != null) visualizer.setStepDuration(Integer.valueOf("" + key) * 10000);
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.animation.SimpleTRIKAnim"});
    }
}