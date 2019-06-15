package ik.obj;

import ik.common.Joint;
import ik.common.LinearBlendSkinning;
import ik.common.LinearBlendSkinningGPU;
import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.ik.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.*;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 10/05/19.
 */
public class Eagle extends PApplet {
    Scene scene;
    //LinearBlendSkinning skinning;
    LinearBlendSkinningGPU skinningGPU;

    String shapePath = "/testing/data/objs/EAGLE_2.OBJ";
    String texturePath = "/testing/data/objs/EAGLE2.jpg";
    PShape psh;

    int activeJ = 0;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        Joint.markers = true;
        Joint.depth = false;

        //Setting the scene and loading mesh
        //1. Create the scene
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        //2. Import the model
        //2.1 create a root node which is parent of shape node
        Node root = new Node(scene);
        root.enableTracking(false);
        //2.2 Load the mesh
        psh = createShapeTri(loadShape(sketchPath() + shapePath), sketchPath() + texturePath, 100);
        //Node shape = new Node(scene, model);
        //shape.enableTracking(false); //use exact picking precision
        //2.3 set root as reference
        //shape.setReference(root);
        //3. Scale scene
        float size = max(psh.getHeight(), psh.getWidth());
        scene.setRightHanded();
        scene.setRadius(size);
        scene.fit();
        //4. Adding the Skeleton
        //4.1 Use SimpleBuilder example (or a Modelling Sw if desired) and position each Joint accordingly to mesh
        //4.2 Create the Joints based on 1.
        ArrayList<Node> skeleton = (ArrayList<Node>) buildSkeleton(root);
        //skinning = new LinearBlendSkinning(shape, model);
        //skinning.setup(skeleton);

        skinningGPU = new LinearBlendSkinningGPU(skeleton, this.g, loadShape(sketchPath() + shapePath),  sketchPath() + texturePath, 100);
        //skinningGPU.setup(skeleton, psh);
        skinningGPU.initParams();

        /* IDEALLY
        Node skinningNode = new Node(scene){
            @Override
            public void graphics(PGraphics pGraphics) {
                skinningGPU.updateParams();
                shader(skinningGPU.shader());
                pGraphics.shape(skinningGPU.shapes().get(0));
                resetShader();
            }
        };
        skinningNode.enableTracking(false);
        skinningNode.setReference(skeleton.get(0));
         */

        //5. Adding IK behavior
        //5.1 register an IK Solver
        Solver solver = scene.registerTreeSolver(skeleton.get(0));
        //Update params
        solver.setMaxError(1f);

        //5.2 Identify end effectors (leaf nodes)
        List<Node> endEffectors = new ArrayList<>();
        List<Node> targets = new ArrayList<>();
        for(Node node : skeleton){
            if(node.children().size() == 0) {
                endEffectors.add(node);
                //5.3 Create targets
                Target target = new Target(scene, scene.radius() * 0.01f);
                target.setReference(root);
                target.setPosition(node.position().get());
                //add IK target to solver
                scene.addIKTarget(node, target);
            }
        }

        skinningGPU.paintAllJoints();

    }

    public void draw(){
        background(0);
        lights();
        scene.drawAxes();
        skinningGPU.updateParams();
        shader(skinningGPU.shader());
        shape(skinningGPU.shapes().get(0));
        resetShader();
        scene.render();

        scene.beginHUD();
        for(int i = 0; i < skinningGPU.skeleton().size(); i++){
            if(skinningGPU.skeleton().get(i).translation().magnitude() == 0){
                continue;
            }
            fill(255);
            Vector p = scene.screenLocation(skinningGPU.skeleton().get(i).position());
            text(skinningGPU.ids().get(skinningGPU.skeleton().get(i)).toString(), p.x(), p.y());

            Vector pos = skinningGPU.skeleton().get(i).position();
            String spos = "" + Math.round(pos.x()) + ", " + Math.round(pos.y()) + ", " + Math.round(pos.z());

            //text(spos, p.x(), p.y() + 10);

        }
        String msg = activeJ == 0 ? "Painting all Joints" : activeJ == -1 ? "" : "Painting Joint : " + activeJ;
        text(msg,width/2, height - 50);
        scene.endHUD();
        //skinning.applyTransformations();
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
            scene.scale(mouseX - pmouseX);
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

    public void keyPressed(){
        if(key == 'A' || key == 'a') {
            activeJ = (activeJ + 1) % skinningGPU.skeleton().size();
            skinningGPU.paintJoint(activeJ);
        }
        if(key == 's' || key == 'S') {
            activeJ = activeJ > 0 ? (activeJ - 1) : skinningGPU.skeleton().size() -1;
            skinningGPU.paintJoint(activeJ);
        }
        if(key == 'd' || key == 'D'){
            skinningGPU.disablePaintMode();
        }
    }

    //Skeleton is founded by interacting with SimpleBuilder
    /* No Local coordinate has rotation (all are aligned with respect to reference system coordinates)
        J1 |-> Node translation: [ -1.7894811E-7, -1.2377515, -1.5709928 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J2 |-> Node translation: [ 6.425498E-7, 1.2980552, 5.463369 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J3 |-> Node translation: [ 6.5103023E-7, 0.23802762, 5.4746757 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J4 |-> Node translation: [ -4.70038E-7, -2.0343544, -4.0577974 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J5 |-> Node translation: [ -4.5386977E-7, -4.236917, -4.046496 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J6 |-> Node translation: [ -6.223473E-7, 1.202842, -5.1527314 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J7 |-> Node translation: [ -7.298398E-7, -0.33323926, -6.1411514 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                    J8 |-> Node translation: [ -6.5542355E-7, -0.4284538, -5.5222764 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J9 |-> Node translation: [ -5.269003, 3.248286, -1.3883741 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J10 |-> Node translation: [ -12.133301, 5.3501167, 0.6687609 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                    J11 |-> Node translation: [ -19.107552, 5.445654, 2.483986 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J12 |-> Node translation: [ 8.201833, 3.9170508, -1.8660631 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J13 |-> Node translation: [ 11.942226, 5.541193, 1.8152181 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                    J14 |-> Node translation: [ 13.184211, 3.8215134, 2.3884451 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
    */

    public List<Node> buildSkeleton(Node reference){
        Joint j1 = new Joint(scene, scene.radius() * 0.01f);
        j1.setPickingThreshold(-0.01f);
        j1.setReference(reference);
        j1.setTranslation(-1.7894811E-7f, -1.2377515f, -1.5709928f);

        Joint dummy;
        dummy = new Joint(scene, scene.radius() * 0.01f);
        dummy.setReference(j1);
        dummy.enableTracking(false);

        Joint j2 = new Joint(scene, scene.radius() * 0.01f);
        j2.setPickingThreshold(-0.01f);
        j2.setReference(dummy);
        j2.setTranslation(6.425498E-7f, 1.2980552f, 5.463369f);
        Joint j3 = new Joint(scene, scene.radius() * 0.01f);
        j3.setPickingThreshold(-0.01f);
        j3.setReference(j2);
        j3.setTranslation(6.5103023E-7f, 0.23802762f, 5.4746757f);


        dummy = new Joint(scene, scene.radius() * 0.01f);
        dummy.setReference(j1);
        dummy.enableTracking(false);
        Joint j4 = new Joint(scene, scene.radius() * 0.01f);
        j4.setPickingThreshold(-0.01f);
        j4.setReference(dummy);
        j4.setTranslation(-4.70038E-7f, -2.0343544f, -4.0577974f);
        Joint j5 = new Joint(scene, scene.radius() * 0.01f);
        j5.setPickingThreshold(-0.01f);
        j5.setReference(j4);
        j5.setTranslation( -4.5386977E-7f, -4.236917f, -4.046496f);

        dummy = new Joint(scene, scene.radius() * 0.01f);
        dummy.setReference(j1);
        dummy.enableTracking(false);
        Joint j6 = new Joint(scene, scene.radius() * 0.01f);
        j6.setPickingThreshold(-0.01f);
        j6.setReference(dummy);
        j6.setTranslation( -6.223473E-7f, 1.202842f, -5.1527314f);
        Joint j7 = new Joint(scene, scene.radius() * 0.01f);
        j7.setPickingThreshold(-0.01f);
        j7.setReference(j6);
        j7.setTranslation( -7.298398E-7f, -0.33323926f, -6.1411514f);
        Joint j8 = new Joint(scene, scene.radius() * 0.01f);
        j8.setPickingThreshold(-0.01f);
        j8.setReference(j7);
        j8.setTranslation( -6.5542355E-7f, -0.4284538f, -5.5222764f);

        dummy = new Joint(scene, scene.radius() * 0.01f);
        dummy.setReference(j1);
        dummy.enableTracking(false);
        Joint j9 = new Joint(scene, scene.radius() * 0.01f);
        j9.setPickingThreshold(-0.01f);
        j9.setReference(dummy);
        j9.setTranslation( -5.269003f, 3.248286f, -1.3883741f);
        Joint j10 = new Joint(scene, scene.radius() * 0.01f);
        j10.setPickingThreshold(-0.01f);
        j10.setReference(j9);
        j10.setTranslation( -12.133301f, 5.3501167f, 0.6687609f);
        Joint j11 = new Joint(scene, scene.radius() * 0.01f);
        j11.setPickingThreshold(-0.01f);
        j11.setReference(j10);
        j11.setTranslation( -19.107552f, 5.445654f, 2.483986f);

        dummy = new Joint(scene, scene.radius() * 0.01f);
        dummy.setReference(j1);
        dummy.enableTracking(false);
        Joint j12 = new Joint(scene, scene.radius() * 0.01f);
        j12.setPickingThreshold(-0.01f);
        j12.setReference(dummy);
        j12.setTranslation( 8.201833f, 3.9170508f, -1.8660631f);
        Joint j13 = new Joint(scene, scene.radius() * 0.01f);
        j13.setPickingThreshold(-0.01f);
        j13.setReference(j12);
        j13.setTranslation( 11.942226f, 5.541193f, 1.8152181f);
        Joint j14 = new Joint(scene, scene.radius() * 0.01f);
        j14.setPickingThreshold(-0.01f);
        j14.setReference(j13);
        j14.setTranslation( 13.184211f, 3.8215134f, 2.3884451f);

        j1.setRoot(true);
        return scene.branch(j1);
    }




    //Adapted from http://www.cutsquash.com/2015/04/better-obj-model-loading-in-processing/
    public PShape createShapeTri(PShape r, String texture, float size) {
        float scaleFactor = size / max(r.getWidth(), r.getHeight());
        PImage tex = loadImage(texture);
        PShape s = createShape();
        s.beginShape(TRIANGLES);
        s.noStroke();
        s.texture(tex);
        s.textureMode(NORMAL);
        for (int i=100; i<r.getChildCount (); i++) {
            if (r.getChild(i).getVertexCount() ==3) {
                for (int j=0; j<r.getChild (i).getVertexCount(); j++) {
                    PVector p = r.getChild(i).getVertex(j).mult(scaleFactor);
                    PVector n = r.getChild(i).getNormal(j);
                    float u = r.getChild(i).getTextureU(j);
                    float v = r.getChild(i).getTextureV(j);
                    s.normal(n.x, n.y, n.z);
                    s.vertex(p.x, p.y, p.z, u, v);
                }
            }
        }
        s.endShape();
        return s;
    }

    public PShape createShapeQuad(PShape r, String texture, float size) {
        float scaleFactor = size / max(r.getWidth(), r.getHeight());
        PImage tex = loadImage(texture);
        PShape s = createShape();
        s.beginShape(QUADS);
        s.noStroke();
        s.texture(tex);
        s.textureMode(NORMAL);
        for (int i=100; i<r.getChildCount (); i++) {
            if (r.getChild(i).getVertexCount() ==4) {
                for (int j=0; j<r.getChild (i).getVertexCount(); j++) {
                    PVector p = r.getChild(i).getVertex(j).mult(scaleFactor);
                    PVector n = r.getChild(i).getNormal(j);
                    float u = r.getChild(i).getTextureU(j);
                    float v = r.getChild(i).getTextureV(j);
                    s.normal(n.x, n.y, n.z);
                    s.vertex(p.x, p.y, p.z, u, v);
                }
            }
        }
        s.endShape();
        return s;
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.obj.Eagle"});
    }

}
