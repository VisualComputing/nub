package ik.collada.test;

import frames.core.Graph;
import frames.core.constraint.FixedConstraint;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.collada.animation.AnimatedModel;
import ik.collada.colladaParser.colladaLoader.ColladaLoader;
import ik.common.SkinningAnimationModel;
import processing.core.*;
import processing.event.MouseEvent;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 23/07/18.
 */
public class LoadMesh extends PApplet {
    //TODO : Update
    Scene scene;
    String path = "/testing/data/dae/";
    String dae = "humanoid.dae";
    String tex = "texture.png";
    AnimatedModel model;
    SkinningAnimationModel skinning;

    ArrayList<Shape> targets = new ArrayList<Shape>();

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        this.g.textureMode(NORMAL);
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.disableBackBuffer();


        PShape redBall = createShape(SPHERE, 0.3f);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        targets.add(new Shape(scene, redBall));
        targets.add(new Shape(scene, redBall));
        targets.add(new Shape(scene, redBall));
        targets.add(new Shape(scene, redBall));
        targets.add(new Shape(scene, redBall));

        model = ColladaLoader.loadColladaModel(sketchPath() + path, dae, tex, scene, 3);
        scene.setRadius(model.getModel().getWidth()*2);
        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2));
        scene.eye().rotate(new Quaternion(new Vector(0,0,1), PI));
        scene.fitBallInterpolation();
        skinning = new SkinningAnimationModel(model);

        Solver solver = scene.registerTreeSolver(model.getRootJoint());
        //model.getJoints().get("Chest").setConstraint(new FixedConstraint());
        model.getRootJoint().setConstraint(new FixedConstraint());

        model.printNames();
        targets.get(0).setPosition(model.getJoints().get("Foot_R").position());
        targets.get(1).setPosition(model.getJoints().get("Foot_L").position());
        targets.get(2).setPosition(model.getJoints().get("Hand_L").position());
        targets.get(3).setPosition(model.getJoints().get("Hand_R").position());
        targets.get(4).setPosition(model.getJoints().get("Head").position());

        scene.addIKTarget(model.getJoints().get("Foot_R"), targets.get(0));
        scene.addIKTarget(model.getJoints().get("Foot_L"), targets.get(1));
        scene.addIKTarget(model.getJoints().get("Hand_L"), targets.get(2));
        scene.addIKTarget(model.getJoints().get("Hand_R"), targets.get(3));
        scene.addIKTarget(model.getJoints().get("Head"), targets.get(4));

        solver.maxIter = 20;
        solver.timesPerFrame = 20;
        solver.error = 0.01f;
    }
    public void draw() {
        skinning.updateParams();
        background(0);
        lights();
        shader(skinning.shader);
        shape(model.getModel());
        resetShader();
        hint(DISABLE_DEPTH_TEST);
        scene.drawAxes();
        scene.traverse();
        hint(ENABLE_DEPTH_TEST);
    }

    @Override
    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.spin();
        } else if (mouseButton == RIGHT) {
            scene.translate();
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

    boolean constraint_hips = true;
    public void keyPressed(){
        if(key == 'h' || key == 'H'){
            if(constraint_hips) {
                System.out.println("Constraint Hips Enabled");
                model.getJoints().get("Torso").setConstraint(new FixedConstraint());
            } else {
                System.out.println("Constraint Hips Disabled");
                model.getJoints().get("Torso").setConstraint(null);
            }
            constraint_hips = !constraint_hips;
        }
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.collada.test.LoadMesh"});
    }
}