package ik.constraintTest;

import common.InteractiveNode;
import frames.core.Graph;
import frames.core.Node;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.FABRIKSolver;
import frames.ik.Solver;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.primitives.constraint.BallAndSocket;
import frames.primitives.constraint.PlanarPolygon;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import ik.common.Target;
import ik.mocap.BVHParser;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sebchaparr on 8/07/18.
 */
public class Cone extends PApplet{
    Scene scene;
    Node eye;
    //String path = "/testing/data/bvh/walk-03-sneak-yokoyama.bvh";
    CCDSolver ccd_solver;
    ArrayList<ChainSolver> chain_solvers = new ArrayList<ChainSolver>();

    ArrayList<Target> targets = new ArrayList<Target>();

    public void settings() {
        size(700, 700, P3D);
    }

    int num = 0;
    float boneLength = 50;
    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        eye = new InteractiveNode(scene);
        scene.setRadius(200);

        scene.setEye(eye);
        scene.setFieldOfView(PI / 3);
        scene.setDefaultGrabber(eye);
        scene.fitBallInterpolation();

        targets.add(new Target(scene));
        targets.add(new Target(scene));
        targets.add(new Target(scene));

        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        ArrayList<Node> structure1 = generateStructure(boneLength,new Vector(-boneLength,0,0));
        chain_solvers.add(new ChainSolver(structure1));

        ArrayList<Node> structure2 = generateStructure(boneLength,new Vector(0,0,0));
        ccd_solver = new CCDSolver(structure2);

        ArrayList<Node> structure3 = generateStructure(boneLength,new Vector(boneLength,0,0));
        chain_solvers.add(new ChainSolver(structure3));

        //((FABRIKSolver) solver).pg = scene.pApplet().getGraphics();
        int i = 0;
        for(ChainSolver s : chain_solvers){
            s.pg = scene.pApplet().getGraphics();
            //s.timesPerFrame = 20;
            s.error = 0.5f;
            s.timesPerFrame = 0.5f;
            s.maxIter = 30;
            //s.error = 1f;
            s.setTarget(targets.get(i));
            if(i != 0)targets.get(i).setReference(targets.get(0));
            targets.get(i++).setPosition(s.endEffector().position());
        }

        ccd_solver.timesPerFrame = 0.5f;
        ccd_solver.error = 0.5f;

        ccd_solver.setTarget(targets.get(targets.size()-1));
        //scene.addIKTarget(structure3.get(structure3.size()-1), targets.get(0));
        targets.get(targets.size()-1).setReference(targets.get(0));
        targets.get(targets.size()-1).setPosition(structure2.get(structure3.size()-1).position());
        chain_solvers.get(1).opt = 1;
        chain_solvers.get(0).opt = 1;

    }

    public void draw() {
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();
        for (Node frame : scene.nodes()) {
            if (frame instanceof Shape) ((Shape) frame).draw();
        }
        for(ChainSolver chain_solver : chain_solvers){
            if(show1) draw_pos(prev, color(0,255,0), 3);
            if(show2) draw_pos(chain_solver.get_p(), color(255,0,100), 3);
            if(show3) draw_pos(constr, color(100,100,0), 3);
        }
        if(solve) {
            ccd_solver.solve();
            for(ChainSolver chain_solver : chain_solvers) chain_solver.solve();
        }
    }

    public void setConstraint(ArrayList<Vector> vertices, Frame f, Vector twist, float boneLength){
        PlanarPolygon constraint = new PlanarPolygon(vertices);
        constraint.setHeight(boneLength / 2.f);
        constraint.setRestRotation(f.rotation().get(), f.transformOf(new Vector(0, 1, 0)), f.transformOf(twist));
        f.setConstraint(constraint);
    }

    public ArrayList<Node> generateStructure(float boneLength, Vector o){
        ArrayList<Vector> vertices = new ArrayList<Vector>();
        vertices.add(new Vector(-10, -10));
        vertices.add(new Vector(0, -10));
        vertices.add(new Vector(0, 0));
        vertices.add(new Vector(-10, 0));

        int color = color(random(0, 255), random(0, 255), random(0, 255), 100);
        Joint prev = new Joint(scene, color);
        Joint current = prev;
        Joint root = current;
        root.setRoot(true);
        //current.setRotation(Quaternion.random());

        color = color(random(0, 255), random(0, 255), random(0, 255), 100);
        current = new Joint(scene, color);
        current.setReference(prev);
        prev = current;
        //current.setRotation(Quaternion.random());
        current.setPosition(0,boneLength,0);
        setConstraint(vertices,current, new Vector(0,boneLength,0),boneLength);


        color = color(random(0, 255), random(0, 255), random(0, 255), 100);
        current = new Joint(scene, color);
        current.setReference(prev);
        prev = current;
        //current.setRotation(Quaternion.random());
        current.setPosition(0,boneLength*2,0);
        setConstraint(vertices,current, new Vector(0,0,1),boneLength);

        color = color(random(0, 255), random(0, 255), random(0, 255), 100);
        current = new Joint(scene, color);
        current.setReference(prev);
        //current.setRotation(Quaternion.random());
        current.setPosition(0,boneLength*2,boneLength*2);
        setConstraint(vertices,current, new Vector(0,0,1),boneLength);


        root.setPosition(o);

        return scene.branch(root);
    }


    boolean read = false;
    boolean solve = false;
    Node n = null;
    float d = 5;
    boolean show1 = true, show2 = true, show3 = true;

    ArrayList<Vector> prev = new ArrayList<Vector>();
    ArrayList<Vector> constr = new ArrayList<Vector>();

    public void keyPressed(){
        if(scene.mouse().trackedGrabber() != null) {
            n =  (Node) scene.mouse().trackedGrabber();
            if(n != null) {
                if (key == 'A' || key == 'a') {
                    d += 1;
                    System.out.println("Speed --- : " + d);
                }
                if (key == 's' || key == 'S') {
                    d *= -1;
                    System.out.println("Speed --- : " + d);
                }

                Frame ref = n.reference() != null ? n.reference() : new Frame();

                if (key == 'X' || key == 'x') {
                    n.rotate(Quaternion.multiply(ref.rotation().inverse(), new Quaternion(new Vector(1, 0, 0), radians(d))));
                }
                if (key == 'Y' || key == 'y') {
                    n.rotate(Quaternion.multiply(ref.rotation().inverse(), new Quaternion(new Vector(0, 1, 0), radians(d))));
                }
                if (key == 'Z' || key == 'z') {
                    n.rotate(Quaternion.multiply(ref.rotation().inverse(), new Quaternion(new Vector(0, 0, 1), radians(d))));
                }

                if (key == 'd' || key == 'D') {
                    n.reference().setConstraint(null);
                }
            }
        }
        if(key == ' ') {
            read = !read;
        }
        if(key== 'Q' || key == 'q'){
            ccd_solver.solve();
            for(Solver s : chain_solvers) {
                s.solve();
            }

            //chain_solver._iterate(scene.frontBuffer());
        }
        if(key == 'j' || key == 'J'){
            for(ChainSolver chain_solver : chain_solvers) {
                chain_solver.forward();
                prev = copy_p(chain_solver.get_p());
                constr = copy_p(prev);
            }
        }
        if(key == 'k' || key == 'K'){
            for(ChainSolver chain_solver : chain_solvers) chain_solver.backward();
        }
        if(key == 'l' || key == 'L'){
            for(ChainSolver chain_solver : chain_solvers) {
                for (int i = 0; i < chain_solver.chain().size() - 1; i++) {
                    constr.set(i + 1, chain_solver._constrainBackwardReaching(chain_solver.chain(), i));
                }
            }
        }
        if(key == 'n' || key == 'N'){
            int i = 0;
            for(ChainSolver chain_solver : chain_solvers) {
                constr = copy_p(prev);
                constr.set(0, chain_solver.chain().get(0).position());
                constr.set(i + 1, chain_solver._constrainBackwardReaching(chain_solver.chain(), i));
            }
        }
        if(key == 'm' || key == 'M'){
            int i = 1;
            for(ChainSolver chain_solver : chain_solvers)
                constr.set(i+1, chain_solver._constrainBackwardReaching(chain_solver.chain(), i));
        }

        if(key == '1'){
            show1 = !show1;
        }
        if(key == '2'){
            show2 = !show2;
        }
        if(key == '3'){
            show3 = !show3;
        }

        if(key == 'w' || key == 'W'){
            solve = !solve;
        }
    }

    ArrayList<Vector> copy_p(ArrayList<Vector> _positions){
        ArrayList<Vector> copy = new ArrayList<Vector>();
        for(Vector p : _positions){
            copy.add(p.get());
        }
        return copy;
    }


    void draw_pos(ArrayList<Vector> _positions, int color, float str) {
        if(_positions == null) return;
        Vector prev = null;
        for(Vector p : _positions){
            pushMatrix();
            pushStyle();
            stroke(color);
            strokeWeight(str);
            if(prev != null) line(prev.x(),prev.y(),prev.z(), p.x(),p.y(),p.z());
            noStroke();
            fill(color, 100);
            translate(p.x(),p.y(),p.z());
            sphere(3);
            popStyle();
            popMatrix();
            prev = p;
        }

    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.constraintTest.Cone"});
    }

}
