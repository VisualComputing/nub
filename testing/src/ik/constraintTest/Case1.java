package ik.constraintTest;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.Hinge;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.Solver;
import frames.ik.animation.IKAnimation;
import frames.ik.evolution.BioIk;
import frames.primitives.Vector;
import frames.processing.Scene;
import ik.basic.Util;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
public class Case1 extends PApplet {
    Scene scene, auxiliar, focus;
    boolean displayAuxiliar = false;

    float boneLenght = 50;
    float targetRadius = 7;

    ArrayList<Frame>[] skeletons = new ArrayList[3];
    Frame[] targets = new Frame[3];
    Solver[] solvers = new Solver[3];
    IKAnimation.FABRIKAnimation FABRIKAnimator = null;
    IKAnimation.CCDAnimation CCDAnimator = null;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setFOV(PI / 3);
        scene.setRadius(boneLenght * 5);
        scene.fit(1);

        auxiliar = new Scene(this, P3D, width, height , 0, 0);
        auxiliar.setType(Graph.Type.ORTHOGRAPHIC);
        auxiliar.setFOV(PI / 3);
        auxiliar.setRadius(boneLenght * 2f);
        auxiliar.fit(1);

        PShape redBall = createShape(SPHERE, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        //create targets
        for(int i = 0; i < targets.length; i++) {
            Frame target = new Frame(scene, redBall);
            target.setPickingThreshold(0);
            targets[i] = target;
        }

        //create skeleton
        for(int i = 0; i < skeletons.length; i++) {
            skeletons[i] = generateSkeleton(new Vector(0.8f*(scene.radius()*i - scene.radius()), 0, 0));
            targets[i].setPosition(skeletons[i].get(skeletons[i].size() - 1).position().get());
        }

        //create solvers
        solvers[0] = new CCDSolver(skeletons[0]);
        solvers[1] = new ChainSolver(skeletons[1]);
        solvers[2] = new BioIk(skeletons[2], 10, 4);


        for(int i = 0; i < solvers.length; i++) {
            Solver solver = solvers[i];
            solver.setTarget(skeletons[i].get(skeletons[i].size() - 1) , targets[i]);
            //TimingTask task = null;
            /*task = new TimingTask() {
                    @Override
                    public void execute() {
                        solver.solve();
                    }
                };
            //scene.registerTask(task);
            //task.run(40);*/
        }
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
        for (int i = 0; i < solvers.length; i++) {
            Util.printInfo(scene, solvers[i], skeletons[i].get(0).position());
        }

        if(displayAuxiliar) {
            auxiliar.beginDraw();
            auxiliar.frontBuffer().lights();
            auxiliar.frontBuffer().background(0);
            auxiliar.drawAxes();
            auxiliar.render();
            if(FABRIKAnimator != null)  FABRIKAnimator.draw();
            if(CCDAnimator != null)  CCDAnimator.draw();
            auxiliar.endDraw();
            auxiliar.display();
        }
        scene.endHUD();

    }

    public ArrayList<Frame> generateSkeleton(Vector position){
        //3-Segment-Arm
        ArrayList<Frame> skeleton = new ArrayList<>();
        Joint j1 = new Joint(scene);
        Joint j2 = new Joint(scene);
        j2.setReference(j1);
        j2.translate(boneLenght, 0, 0);
        Joint j3 = new Joint(scene);
        j3.setReference(j2);
        Vector v = new Vector(boneLenght, -boneLenght, 0);
        v.normalize();
        v.multiply(boneLenght);
        j3.translate(v);
        j1.setRoot(true);
        j1.translate(position);
        skeleton.add(j1);
        skeleton.add(j2);
        skeleton.add(j3);
        //Add constraints
        BallAndSocket c1 = new BallAndSocket(radians(45),radians(45));
        c1.setRestRotation(j1.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0));
        j1.setConstraint(c1);

        Hinge c2 = new Hinge(0,radians(120));
        c2.setRestRotation(j2.rotation().get());
        c2.setAxis(j2.rotation().get(), new Vector(0,0,-1));
        j2.setConstraint(c2);
        return skeleton;
    }


    @Override
    public void mouseMoved() {
        focus.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            focus.spin();
        } else if (mouseButton == RIGHT) {
            for(Frame target : targets){
                if(focus.trackedFrame() == target){
                    for(Frame t : targets) focus.translate(t);
                    return;
                }
            }
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
        if(key == '1'){
            displayAuxiliar = true;
            for (Solver s : solvers) s.solve();
            FABRIKAnimator = new IKAnimation.FABRIKAnimation(auxiliar, (ChainSolver) solvers[1], targetRadius);
        } else if(key == '2'){
            displayAuxiliar = true;
            for (Solver s : solvers) s.solve();
            CCDAnimator = new IKAnimation.CCDAnimation(auxiliar, (CCDSolver) solvers[0], targetRadius);
        } else if(key == ' '){
            displayAuxiliar = !displayAuxiliar;
        } else if(key == 's'){
            for (Solver s : solvers) s.solve();
        }

    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.constraintTest.Case1"});
    }
}