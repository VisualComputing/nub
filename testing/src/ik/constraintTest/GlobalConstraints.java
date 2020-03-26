package ik.constraintTest;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.ChainSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GlobalConstraints extends PApplet {
    int numJoints = 5;
    float targetRadius = 7;
    float boneLength = 50;
    boolean solve = false;

    //Scene Parameters
    Scene scene;
    //Benchmark Parameters
    Random random = new Random();
    ArrayList<Solver> solvers; //Will store Solvers
    ArrayList<ArrayList<Node>> structures = new ArrayList<>(); //Keep Structures
    ArrayList<Node> targets = new ArrayList<Node>(); //Keep targets

    int randRotation = 0; //Set seed to generate initial random rotations, otherwise set to -1
    int randLength = 0; //Set seed to generate random segment lengths, otherwise set to -1
    int numSolvers = 1; //Set number of solvers

    public void settings() {
        size(1500, 800, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(numJoints * boneLength * 1.2f);
        scene.fit(1);
        scene.setRightHanded();

        //1. Create Targets
        targets = Util.createTargets(numSolvers, scene, targetRadius);
        float alpha = 1.f * width / height > 1.5f ? 0.5f * width / height : 0.5f;
        alpha *= numSolvers/4f; //avoid undesirable overlapping

        //2. Generate Structures
        for(int i = 0; i < numSolvers; i++){
            int red = (int) random(255);
            int green = (int) random(255);
            int blue = (int) random(255);
            structures.add(Util.generateAttachedChain(numJoints, 0.3f* targetRadius, boneLength, new Vector(i * 2 * alpha * scene.radius()/(max(numSolvers - 1,1)) - alpha * scene.radius(), 0, 0), red, green, blue, randRotation, randLength));
        }

        //3. Apply constraints
        float down = radians(10);
        float up = radians(10);
        float left = radians(10);
        float right = radians(10);

        for(ArrayList<Node> structure : structures){
            for (int i = 0; i < structure.size() - 1; i++) {
                Vector twist = structure.get(i + 1).translation().get();
                BallAndSocket constraint = new BallAndSocket(down, up, left, right);
                constraint.setTwistLimits(0,0);
                constraint.setRestRotation(structure.get(i).rotation().get(), new Vector(0, 1, 0), twist);
                structure.get(i).setConstraint(constraint);
            }
        }
        //rot eacho node
        for(ArrayList<Node> structure : structures) {
            for (int i = 0; i < structure.size() - 1; i++) {
                Vector twist = structure.get(i + 1).translation().get();
                structure.get(i).rotate(twist.orthogonalVector(), radians(5f));
            }
        }


        //4. Set eye scene
        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        //5. generate solvers
        solvers = new ArrayList<>();
        int i = 0;

        //FABRIK Fix Twisting (H1 & H2)
        ChainSolver chainSolver = new ChainSolver(structures.get(i++));
        chainSolver.setFixTwisting(true);
        chainSolver.setKeepDirection(true);
        solvers.add(chainSolver);

        for(i = 0; i < solvers.size(); i++){
            Solver solver = solvers.get(i);
            //6. Define solver parameters
            solver.setMaxError(0.001f);
            solver.setTimesPerFrame(1);
            solver.setMaxIterations(200);
            //7. Set targets
            solver.setTarget(structures.get(i).get(numJoints - 1), targets.get(i));
            targets.get(i).setPosition(structures.get(i).get(numJoints - 1).position());

            TimingTask task = new TimingTask() {
                @Override
                public void execute() {
                    if(solve) {
                        solver.solve();
                    }
                }
            };
            task.run(40);
        }
    }

    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        for(int  i = 0; i < solvers.size(); i++) {
            if(solvers.get(i) instanceof ChainSolver) {
                drawConstraints(structures.get(i), ((ChainSolver) solvers.get(i)).globalConstraints());
                drawDebug((ChainSolver) solvers.get(i));
            }
        }
        scene.beginHUD();
        for(int  i = 0; i < solvers.size(); i++) {
            Util.printInfo(scene, solvers.get(i), structures.get(i).get(0).position());
        }
        scene.endHUD();
    }

    public void drawDebug(ChainSolver s){
        Vector r = s.chain().get(0).position();
        int c = 0;
        for(ChainSolver.DebugGlobal d : s.dg){
            if(c != j || d == null){
                c++;
                continue;
            }
            push();
            strokeWeight(2);
            stroke(255,0,255);
            fill(255,0,255);
            line(r.x(), r.y(), r.z(), r.x() + d.axis.x(), r.y() + d.axis.y(), r.z() + d.axis.z());
            stroke(0,255,255);
            fill(0,255,255);
            line(r.x(), r.y(), r.z(), r.x() + d.up_axis.x(), r.y() + d.up_axis.y(), r.z() + d.up_axis.z());
            stroke(255,255,255);
            fill(255,255,255);
            line(r.x(), r.y(), r.z(), r.x() + d.right_axis.x(), r.y() + d.right_axis.y(), r.z() + d.right_axis.z());
            line(r.x(), r.y(), r.z(), s.chain().get(c).position().x(), s.chain().get(c).position().y(), s.chain().get(c).position().z());

            for(List<Node> chain : d.chain){
                Vector prev = null;
                int count = 0;
                for(Node node : chain){
                    Vector v = node.position();
                    push();
                    stroke(255,255,0);
                    fill(255,255,0);
                    if(count == chain.size() -1){
                        stroke(0,255,0);
                        fill(0,255,0);
                    }

                    if(prev != null){
                        line(prev.x(),prev.y(),prev.z(), v.x(), v.y(), v.z());
                    }
                    translate(v.x(), v.y(), v.z());
                    sphere(3);
                    pop();
                    prev = v;
                    count++;
                }
            }
            pop();
            c++;
        }
    }

    public void drawConstraints(List<Node> chain, BallAndSocket[] constraints){
        float factor = 0.6f;
        for(int c = 0; c < chain.size(); c++){
            if(c != j) continue;
            BallAndSocket constraint = constraints[c];
            if(constraint == null) continue;
            Node node = chain.get(c);
            float boneLength = 0;
            if (!node.children().isEmpty()) {
                for (Node child : node.children())
                    boneLength += child.translation().magnitude();
                boneLength = boneLength / (1.f * node.children().size());
            } else
                boneLength = node.translation().magnitude();
            if (boneLength == 0) continue;
            pushMatrix();
            pushStyle();
            noStroke();
            fill(255, 0, 0, 100);
            Node reference = new Node();
            reference.setTranslation(chain.get(0).position());
            reference.rotate(constraint.orientation());
            scene.applyTransformation(reference);
            float width = boneLength * factor;
            float max = Math.max(Math.max(Math.max(constraint.up(), constraint.down()), constraint.left()), constraint.right());
            //Max value will define max radius length
            float height = (float) (width / Math.tan(max));
            if (height > boneLength * factor) height = width;
            //drawAxes(pGraphics,height*1.2f);
            //get all radius
            float up_r = (float) Math.abs((height * Math.tan(constraint.up())));
            float down_r = (float) Math.abs((height * Math.tan(constraint.down())));
            float left_r = (float) Math.abs((height * Math.tan(constraint.left())));
            float right_r = (float) Math.abs((height * Math.tan(constraint.right())));
            scene.drawCone(scene.context(), 20, 0, 0, height, left_r, up_r, right_r, down_r);
            popMatrix();
            popStyle();
        }

    }

    int j = 0;
    public void keyPressed(){
        if(key == 'w' || key == 'W'){
            solve = !solve;
        }
        j = (j + 1)  % numJoints;
    }

    @Override
    public void mouseMoved() {
        scene.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.mouseSpin();
        } else if (mouseButton == RIGHT) {
            if(targets.contains(scene.node())){
                for(Node target : targets) scene.translateNode(target, scene.mouseDX(), scene.mouseDY(), 0, 0);
            }else{
                scene.mouseTranslate();
            }
        } else {
            scene.scale(scene.mouseDX());
        }
    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 2)
            if (event.getButton() == LEFT)
                scene.focus();
            else
                scene.align();
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.constraintTest.GlobalConstraints"});
    }
}
