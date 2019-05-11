package ik.obj;

import ik.common.Joint;
import ik.common.LinearBlendSkinning;
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
public class Rex extends PApplet {
    Scene scene;
    LinearBlendSkinning skinning;

    String shapePath = "/testing/data/objs/T-Rex-Model.obj";
    String texturePath = "/testing/data/objs/T-Rex.jpg";

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        Joint.markers = false;
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
        PShape model = createShapeTri(loadShape(sketchPath() + shapePath), sketchPath() + texturePath, 100);
        Node shape = new Node(scene, model);
        shape.enableTracking(false); //use exact picking precision
        //2.3 set root as reference
        shape.setReference(root);
        //3. Scale scene
        float size = max(model.getHeight(), model.getWidth());
        scene.setRightHanded();
        scene.setRadius(size);
        scene.fit();
        //4. Adding the Skeleton
        //4.1 Use SimpleBuilder example (or a Modelling Sw if desired) and position each Joint accordingly to mesh
        //4.2 Create the Joints based on 1.
        ArrayList<Node> skeleton = (ArrayList<Node>) buildSkeleton(root);
        skinning = new LinearBlendSkinning(shape, model);
        skinning.setup(skeleton);

        //5. Adding IK behavior
        //5.1 register an IK Solver
        Solver solver = scene.registerTreeSolver(skeleton.get(0));
        //Update params
        solver.error = 1f;

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
    }

    public void draw(){
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        skinning.applyTransformations();
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
        Node f = scene.trackedNode();
        if(f == null) return;
        Hinge c = f.constraint() instanceof Hinge ? (Hinge) f.constraint() : null;
        if(c == null) return;
        scene.trackedNode().rotate(new Quaternion( c.orientation().rotate(new Vector(0,0,1)), radians(5)));
    }

    //Skeleton is founded by interacting with SimpleBuilder
    public List<Node> buildSkeleton(Node reference){
        Joint j1 = new Joint(scene, scene.radius() * 0.01f);
        j1.setPickingThreshold(-0.01f);
        j1.setReference(reference);
        j1.setTranslation(3.4285722f ,40.571423f ,26.857159f);
        j1.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j2 = new Joint(scene, scene.radius() * 0.01f);
        j2.setPickingThreshold(-0.01f);
        j2.setReference(j1);
        j2.setTranslation(0.0f ,0.0f ,0.0f);
        j2.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3 = new Joint(scene, scene.radius() * 0.01f);
        j3.setPickingThreshold(-0.01f);
        j3.setReference(j2);
        j3.setTranslation(10.571428f ,-1.1428577f ,-3.0778125E-8f);
        j3.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j4 = new Joint(scene, scene.radius() * 0.01f);
        j4.setPickingThreshold(-0.01f);
        j4.setReference(j3);
        j4.setTranslation(10.884712f ,-0.28756276f ,-1.18158985E-8f);
        j4.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j5 = new Joint(scene, scene.radius() * 0.01f);
        j5.setPickingThreshold(-0.01f);
        j5.setReference(j4);
        j5.setTranslation(11.714285f ,1.42858f ,2.613221E-8f);
        j5.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j6 = new Joint(scene, scene.radius() * 0.01f);
        j6.setPickingThreshold(-0.01f);
        j6.setReference(j5);
        j6.setTranslation(10.285713f ,1.1428397f ,2.0452614E-8f);
        j6.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j7 = new Joint(scene, scene.radius() * 0.01f);
        j7.setPickingThreshold(-0.01f);
        j7.setReference(j6);
        j7.setTranslation(10.571428f ,-2.2857053f ,-5.6322772E-8f);
        j7.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j29 = new Joint(scene, scene.radius() * 0.01f);
        j29.setPickingThreshold(-0.01f);
        j29.setReference(j1);
        j29.setTranslation(0.0f ,0.0f ,0.0f);
        j29.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j30 = new Joint(scene, scene.radius() * 0.01f);
        j30.setPickingThreshold(-0.01f);
        j30.setReference(j29);
        j30.setTranslation(-10.571427f ,3.0402553f ,-0.102601975f);
        j30.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j31 = new Joint(scene, scene.radius() * 0.01f);
        j31.setPickingThreshold(-0.01f);
        j31.setReference(j30);
        j31.setTranslation(-9.115286f ,4.2838655f ,1.0026427E-7f);
        j31.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j32 = new Joint(scene, scene.radius() * 0.01f);
        j32.setPickingThreshold(-0.01f);
        j32.setReference(j31);
        j32.setTranslation(-7.1428566f ,4.285714f ,9.932917E-8f);
        j32.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j33 = new Joint(scene, scene.radius() * 0.01f);
        j33.setPickingThreshold(-0.01f);
        j33.setReference(j32);
        j33.setTranslation(-7.4285707f ,3.142857f ,7.392577E-8f);
        j33.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j34 = new Joint(scene, scene.radius() * 0.01f);
        j34.setPickingThreshold(-0.01f);
        j34.setReference(j33);
        j34.setTranslation(-6.8571424f ,-0.28571427f ,-2.9916498E-9f);
        j34.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j100 = new Joint(scene, scene.radius() * 0.01f);
        j100.setPickingThreshold(-0.01f);
        j100.setReference(j32);
        j100.setTranslation(-4.8571424f ,-3.7142854f ,-8.061626E-8f);
        j100.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j101 = new Joint(scene, scene.radius() * 0.01f);
        j101.setPickingThreshold(-0.01f);
        j101.setReference(j100);
        j101.setTranslation(-5.1428566f ,-2.8571427f ,-6.1316186E-8f);
        j101.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j362 = new Joint(scene, scene.radius() * 0.01f);
        j362.setPickingThreshold(-0.01f);
        j362.setReference(j30);
        j362.setTranslation(0.0f ,0.0f ,0.0f);
        j362.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j363 = new Joint(scene, scene.radius() * 0.01f);
        j363.setPickingThreshold(-0.01f);
        j363.setReference(j362);
        j363.setTranslation(-7.3342543f ,-3.425026f ,-6.142557f);
        j363.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j364 = new Joint(scene, scene.radius() * 0.01f);
        j364.setPickingThreshold(-0.01f);
        j364.setReference(j363);
        j364.setTranslation(0.39589864f ,-1.2802304f ,-2.5469143f);
        j364.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j365 = new Joint(scene, scene.radius() * 0.01f);
        j365.setPickingThreshold(-0.01f);
        j365.setReference(j364);
        j365.setTranslation(-3.3720455f ,-2.5707324E-8f ,0.8169972f);
        j365.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j1816 = new Joint(scene, scene.radius() * 0.01f);
        j1816.setPickingThreshold(-0.01f);
        j1816.setReference(j30);
        j1816.setTranslation(0.0f ,0.0f ,0.0f);
        j1816.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j1817 = new Joint(scene, scene.radius() * 0.01f);
        j1817.setPickingThreshold(-0.01f);
        j1817.setReference(j1816);
        j1817.setTranslation(-6.960158f ,-3.3860307f ,4.7941914f);
        j1817.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j1818 = new Joint(scene, scene.radius() * 0.01f);
        j1818.setPickingThreshold(-0.01f);
        j1818.setReference(j1817);
        j1818.setTranslation(0.32144654f ,-1.413284f ,2.6967328f);
        j1818.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j1819 = new Joint(scene, scene.radius() * 0.01f);
        j1819.setPickingThreshold(-0.01f);
        j1819.setReference(j1818);
        j1819.setTranslation(-4.5706f ,5.390251E-8f ,-0.3815417f);
        j1819.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j9143 = new Joint(scene, scene.radius() * 0.01f);
        j9143.setPickingThreshold(-0.01f);
        j9143.setReference(j1);
        j9143.setTranslation(0.0f ,0.0f ,0.0f);
        j9143.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j9144 = new Joint(scene, scene.radius() * 0.01f);
        j9144.setPickingThreshold(-0.01f);
        j9144.setReference(j9143);
        j9144.setTranslation(-1.7870715f ,-8.088837f ,-6.772049f);
        j9144.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j9145 = new Joint(scene, scene.radius() * 0.01f);
        j9145.setPickingThreshold(-0.01f);
        j9145.setReference(j9144);
        j9145.setTranslation(5.0281043f ,-7.958227f ,-1.803694E-7f);
        j9145.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j9146 = new Joint(scene, scene.radius() * 0.01f);
        j9146.setPickingThreshold(-0.01f);
        j9146.setReference(j9145);
        j9146.setTranslation(-1.9751868f ,-8.182893f ,-1.819241E-7f);
        j9146.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j45721 = new Joint(scene, scene.radius() * 0.01f);
        j45721.setPickingThreshold(-0.01f);
        j45721.setReference(j1);
        j45721.setTranslation(0.0f ,0.0f ,0.0f);
        j45721.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j45722 = new Joint(scene, scene.radius() * 0.01f);
        j45722.setPickingThreshold(-0.01f);
        j45722.setReference(j45721);
        j45722.setTranslation(-1.6930151f ,-8.276945f ,6.1136546f);
        j45722.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j45723 = new Joint(scene, scene.radius() * 0.01f);
        j45723.setPickingThreshold(-0.01f);
        j45723.setReference(j45722);
        j45723.setTranslation(5.3834977f ,-7.86417f ,0.1498184f);
        j45723.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j45724 = new Joint(scene, scene.radius() * 0.01f);
        j45724.setPickingThreshold(-0.01f);
        j45724.setReference(j45723);
        j45724.setTranslation(-2.069244f ,-8.182891f ,-1.8187754E-7f);
        j45724.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
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
        PApplet.main(new String[]{"ik.obj.Rex"});
    }

}
