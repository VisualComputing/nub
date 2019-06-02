package ik.collada.test;

import ik.common.Joint;
import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.ik.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import ik.collada.animation.AnimatedModel;
import ik.collada.colladaParser.colladaLoader.ColladaBlenderLoader;
import ik.common.SkinningAnimationModel;
import processing.core.*;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 18/05/19.
 */
public class LoadMesh2 extends PApplet {
    //TODO : Update
    Scene scene;
    String path = "/testing/data/dae/";
    String dae = "dummy.dae";
    String tex = null;
    AnimatedModel model;
    SkinningAnimationModel skinning;
    Solver solver;
    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        randomSeed(14);
        textSize(24);

        Joint.markers = true;
        this.g.textureMode(NORMAL);
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);

        model = ColladaBlenderLoader.loadColladaModel(sketchPath() + path, dae, tex, scene, 3);

        scene.setRadius(model.getModels().get(null).getWidth()*2);
        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2));
        scene.eye().rotate(new Quaternion(new Vector(0,0,1), PI));
        scene.fit();
        skinning = new SkinningAnimationModel(model);
        //Adding IK behavior
        //Register an IK Solver
        solver = scene.registerTreeSolver(model.getRootJoint());
        //Update params
        solver.setMaxError(1f);

        //solver = scene.registerTreeSolver(model.getJoints().get("Bone_004"));
        //Update params
        solver.setMaxError(1f);

        //solver = scene.registerTreeSolver(model.getJoints().get("Bone_009"));

        //Update params
        solver.setMaxError(1f);
        //Identify end effectors (leaf nodes)
        List<Node> endEffectors = new ArrayList<>();
        for (String s : model.getJoints().keySet()) {
            Node node = model.getJoints().get(s);
            if(s.equals("Bone_020") || s.equals("Bone_016") || s.equals("Bone_008") || s.equals("Bone_007")) {
                endEffectors.add(node);
                // Create targets
                Target target = new Target(scene, scene.radius() * 0.02f);
                //target.setReference(root);
                target.setPosition(node.position().get());
                //add IK target to solver
                scene.addIKTarget(node, target);
            }
        }
    }

    public void draw() {
        skinning.updateParams();
        background(0);
        lights();
        scene.drawAxes();
        shader(skinning.shader);
        shape(model.getModels().get(null));
        resetShader();
        hint(DISABLE_DEPTH_TEST);
        scene.render();
        hint(ENABLE_DEPTH_TEST);

        scene.beginHUD();
        for (String s : model.getJoints().keySet()) {
            Node n = model.getJoints().get(s);
            if(n.isCulled() || !n.isTrackingEnabled()) continue;
            Vector sc = scene.screenLocation(new Vector(), n);
            text(s, sc.x(), sc.y());
        }
        scene.endHUD();

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

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.collada.test.LoadMesh2"});
    }
}