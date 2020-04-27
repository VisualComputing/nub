package ik.obj;

import nub.ik.animation.Skeleton;
import nub.ik.skinning.GPULinearBlendSkinning;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.*;
import processing.event.MouseEvent;


/**
 * Created by sebchaparr on 10/05/19.
 */
public class Rex extends PApplet {
    Scene scene;
    Skeleton skeleton;
    GPULinearBlendSkinning skinning;

    String shapePath = "/testing/data/objs/Rex.obj";
    String texturePath = "/testing/data/objs/T-Rex.jpg";
    String jsonPath = "/testing/data/skeletons/rex.json";

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        //1. Create and set the scene
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRightHanded();
        scene.fit(1);
        //2. Load the Skeleton (this file was generated using SimpleBuilder example)
        skeleton = new Skeleton(scene, jsonPath);
        //3. Relate the shape with a skinning method (CPU or GPU)
        skinning = new GPULinearBlendSkinning(skeleton, sketchPath() + shapePath, sketchPath() + texturePath, scene.radius());
        //4. enable IK behavior
        skeleton.enableIK();
        //5. add targets for each leaf-node
        skeleton.addTargets();
        //6. (optional) set target radius
        skeleton.setTargetRadius(3f);
    }

    public void draw(){
        background(0);
        lights();
        scene.drawAxes();
        //Render mesh and deform properly
        skinning.render(scene);
        scene.render();
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
        Node f = scene.node();
        if(f == null) return;
        Hinge c = f.constraint() instanceof Hinge ? (Hinge) f.constraint() : null;
        if(c == null) return;
        scene.node().rotate(new Quaternion( c.orientation().rotate(new Vector(0,0,1)), radians(5)));
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.obj.Rex"});
    }

}
