package ik.collada.test;

import nub.ik.solver.Solver;
import nub.ik.visual.Joint;
import nub.ik.skinning.GPULinearBlendSkinning;
import nub.core.Node;
import nub.core.Graph;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.FixedConstraint;
import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.ChainSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.ik.loader.collada.data.Model;
import nub.ik.loader.collada.BlenderLoader;
import ik.interactive.Target;
import nub.processing.TimingTask;
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
    Model model;
    GPULinearBlendSkinning skinning;
    ArrayList<Node> targets = new ArrayList<Node>();
    String[] roots = {"Upper_Arm_R", "Upper_Arm_L", "Upper_Leg_R", "Upper_Leg_L", "Neck"};
    String[] effectors = {"Hand_R", "Hand_L", "Foot_R", "Foot_L", "Head"};


    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        randomSeed(14);
        //Joint.markers = true;
        textureMode(NORMAL);
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);

        model = BlenderLoader.loadColladaModel(sketchPath() + path, dae, tex, scene, 3);
        scene.setRadius(model.mesh().getWidth()*2);
        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2));
        scene.eye().rotate(new Quaternion(new Vector(0,0,1), PI));
        scene.fit();
        skinning = new GPULinearBlendSkinning(model.structure(), this.g, model.mesh());

        for(int i = 0; i < effectors.length; i++){
            Node target = new Target(scene, ((Joint)model.root()).radius() * 0.6f);
            target.setPickingThreshold(0);
            targets.add(target);
        }

        Quaternion rot;
        BallAndSocket root_constraint = new BallAndSocket(radians(5), radians(5), radians(5), radians(5));
        root_constraint.setRestRotation(model.root().rotation().get(), model.root().displacement(new Vector(0, 1, 0)), model.root().displacement(new Vector(0, 0, 1)));
        model.root().setConstraint(root_constraint);

/**/
        BallAndSocket chest_constraint = new BallAndSocket(radians(70), radians(20), radians(70), radians(70));
        Node chest = model.skeleton().get("Chest");
        chest_constraint.setRestRotation(chest.rotation().get(), chest.displacement(new Vector(0, 1, 0)), chest.displacement(new Vector(0, 0, 1)));

        //Adding constraints

        //ARMS
        Node r_arm = model.skeleton().get("Upper_Arm_R");
        BallAndSocket r_arm_constraint = new BallAndSocket(radians(85), radians(50), radians(89), radians(89));
        r_arm_constraint.setTwistLimits(radians(90), radians(90));
        rot = r_arm.rotation().get();
        r_arm_constraint.setRestRotation(rot, r_arm.displacement(new Vector(0, 1, 0)), r_arm.displacement(new Vector(-1, 0, 0)), r_arm.children().get(0).translation());
        r_arm.setConstraint(r_arm_constraint);
        //Elbow constraint
        Node r_elbow = model.skeleton().get("Lower_Arm_R");
        Hinge r_elbow_constraint = new Hinge(radians(90), radians(5), r_elbow.rotation().get(), new Vector(0,1,0), new Vector(0,0,1));
        r_elbow.setConstraint(r_elbow_constraint);

        Node l_arm = model.skeleton().get("Upper_Arm_L");
        BallAndSocket l_arm_constraint = new BallAndSocket(radians(85), radians(50), radians(89), radians(89));
        l_arm_constraint.setTwistLimits(radians(90), radians(90));
        rot = l_arm.rotation().get();
        l_arm_constraint.setRestRotation(rot, l_arm.displacement(new Vector(0, 1, 0)), l_arm.displacement(new Vector(1, 0, 0)), l_arm.children().get(0).translation());
        l_arm.setConstraint(l_arm_constraint);
        //Elbow constraint
        Node l_elbow = model.skeleton().get("Lower_Arm_L");
        Hinge l_elbow_constraint = new Hinge(radians(90), radians(5), l_elbow.rotation().get(), new Vector(0,1,0), new Vector(0,0,1));
        l_elbow.setConstraint(l_elbow_constraint);

        //LEGS
        Node r_leg = model.skeleton().get("Upper_Leg_R");
        BallAndSocket r_leg_constraint = new BallAndSocket(radians(30), radians(50), radians(50), radians(89));
        r_leg_constraint.setTwistLimits(radians(30), radians(30));
        rot = r_leg.rotation().get();
        r_leg_constraint.setRestRotation(rot, new Vector(1, 0, 0), new Vector(0,1,0));
        r_leg.setConstraint(r_leg_constraint);
        //Knee constraint
        Node r_knee = model.skeleton().get("Lower_Leg_R");
        Hinge r_knee_constraint = new Hinge(radians(5), radians(90), r_knee.rotation().get(), new Vector(0,1,0), new Vector(1,0,0));
        r_knee.setConstraint(r_knee_constraint);
/**/
        Node l_leg = model.skeleton().get("Upper_Leg_L");
        BallAndSocket l_leg_constraint = new BallAndSocket(radians(30), radians(50), radians(50), radians(89));
        l_leg_constraint.setTwistLimits(radians(30), radians(30));
        rot = l_leg.rotation().get();
        l_leg_constraint.setRestRotation(rot, new Vector(1, 0, 0), new Vector(0,1,0));
        l_leg.setConstraint(l_leg_constraint);
        //Knee constraint
        Node l_knee = model.skeleton().get("Lower_Leg_L");
        Hinge l_knee_constraint = new Hinge(radians(5), radians(90), l_knee.rotation().get(), new Vector(0,1,0), new Vector(1,0,0));
        l_knee.setConstraint(l_knee_constraint);
/**/

        model.printNames();

        boolean whole = true;
        //TODO: Fix Tree Solver when model has constraints!
        if(whole) {
            Solver s = scene.registerTreeSolver(model.root());
            s.setTimesPerFrame(1);
            s.setMaxIterations(50);
            s.setMaxError(0.01f);
            s.setMinDistance(0.01f);

            for (int i = 0; i < effectors.length; i++) {
                targets.get(i).setPosition(model.skeleton().get(effectors[i]).position());
                scene.addIKTarget(model.skeleton().get(effectors[i]), targets.get(i));
            }
        }else {
            /*Chain solver*/
            for (int i = 0; i < effectors.length; i++) {
                targets.get(i).setPosition(model.skeleton().get(effectors[i]).position());
                ChainSolver solver_r_leg = new ChainSolver(scene.branch(model.skeleton().get(roots[i])));
                solver_r_leg.setKeepDirection(false);
                solver_r_leg.setFixTwisting(false);
                solver_r_leg.explore(false);

                solver_r_leg.setTimesPerFrame(5);
                solver_r_leg.setMaxIterations(50);
                solver_r_leg.setMaxError(0.01f);
                solver_r_leg.setMinDistance(0.01f);
                solver_r_leg.setTarget(model.skeleton().get(effectors[i]), targets.get(i));
                TimingTask task = new TimingTask() {
                    @Override
                    public void execute() {
                        solver_r_leg.solve();
                    }
                };
                task.run(40);
            }
        }
    }
    public void draw() {
        background(0);
        lights();
        scene.drawAxes();

        //Render mesh
        skinning.render();
        //Render skeleton
        hint(DISABLE_DEPTH_TEST);
        scene.render();
        hint(ENABLE_DEPTH_TEST);
    }

    @Override
    public void mouseMoved() {
        scene.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.mouseSpin();
        } else if (mouseButton == RIGHT) {
            scene.mouseTranslate();
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
    int activeJ = 0;
    public void keyPressed(){
        if(key == 'h' || key == 'H'){
            if(constraint_hips) {
                System.out.println("Constraint Hips Enabled");
                model.skeleton().get("Torso").setConstraint(new FixedConstraint());
            } else {
                System.out.println("Constraint Hips Disabled");
                model.skeleton().get("Torso").setConstraint(null);
            }
            constraint_hips = !constraint_hips;
        }
        if(key == 'A' || key == 'a') {
            activeJ = (activeJ + 1) % skinning.skeleton().size();
            skinning.paintJoint(activeJ);
        }
        if(key == 's' || key == 'S') {
            activeJ = activeJ > 0 ? (activeJ - 1) : skinning.skeleton().size() -1;
            skinning.paintJoint(activeJ);
        }
        if(key == 'd' || key == 'D'){
            skinning.disablePaintMode();
        }
        if(key == ' '){
            if(scene.node() != null)
                scene.node().enableTagging(false);
        }
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.collada.test.LoadMesh"});
    }
}