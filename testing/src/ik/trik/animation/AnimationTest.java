package ik.trik.animation;

import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Constraint;
import nub.ik.animation.Skeleton;
import nub.ik.animation.SkeletonAnimation;
import nub.ik.skinning.GPULinearBlendSkinning;
import nub.ik.skinning.Skinning;
import nub.ik.solver.Solver;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class AnimationTest extends PApplet{
    Scene mainScene, controlScene, focus;
    String jsonPath = "/testing/data/skeletons/Hand_constrained.json";
    String shapePath = "/testing/data/objs/Rigged_Hand.obj";
    String texturePath = "/testing/data/objs/HAND_C.jpg";
    AnimationPanel panel;
    Skeleton skeleton;
    Skinning skinning;


    public void settings() {
        size(1200, 800, P3D);
    }

    public void setup() {
        //Kinematic scene
        mainScene = new Scene(this);
        mainScene.fit(0);
        mainScene.setRightHanded();
        //skeleton = generateSkeleton(mainScene, 5);
        skeleton = new Skeleton(mainScene, sketchPath() + jsonPath);
        skeleton.enableIK();
        skeleton.addTargets();
        skeleton.setTargetRadius(0.03f * mainScene.radius());

        Solver s = new SimpleTRIK(skeleton.joints(), SimpleTRIK.HeuristicMode.FORWARD_TRIANGULATION);
        //Dummy constraints in all joints
        /*for(Node node : skeleton.joints()){
            if(node.children().size() == 1){
                float angle = radians(85);
                BallAndSocket constraint = new BallAndSocket(angle, angle);
                Vector twist = node.children().get(0).translation().get();
                constraint.setRestRotation(node.rotation().get(), twist.orthogonalVector(), twist);
                constraint.setTwistLimits(0,0);
                node.setConstraint(constraint);
            }
        }*/


        //Relate the shape with a skinning method (CPU or GPU)
        skinning = new GPULinearBlendSkinning(skeleton, sketchPath() + shapePath, sketchPath() + texturePath, mainScene.radius());


        //Set the control scene
        controlScene = new Scene(this, P2D, width, (int)(height * 0.3), 0,(int)(height * 0.7f));
        controlScene.setRadius( height * 0.3f / 2.f);
        controlScene.fit();
        //Setting the panel
        panel = new AnimationPanel(controlScene, skeleton);
        //set eye constraint
        //set eye constraint
        controlScene.eye().enableTagging(false);
        controlScene.eye().setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                return new Vector(translation.x(), 0); //no vertical translation allowed
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                return new Quaternion(); //no rotation is allowed
            }
        });

    }


    public void draw() {
        //cull main scene
        lights();
        //cull panel
        skeleton.cull(false);
        panel.cull(true);
        mainScene.context().background(0);
        mainScene.drawAxes();
        skinning.render(mainScene);
        mainScene.render();

        noLights();
        skeleton.cull(true);
        panel.cull(false);
        mainScene.beginHUD();
        controlScene.beginDraw();
        controlScene.context().background(150);
        controlScene.render();
        controlScene.endDraw();
        controlScene.display();
        mainScene.endHUD();

        skeleton.cull(false);
        panel.cull(true);
    }

    //Skeleton definition methods
    public Skeleton generateSkeleton(Scene scene, int n){
        Skeleton skeleton = new Skeleton(scene);
        //1. create a basic skeleton composed of n Joints
        int idx = 0;
        skeleton.addJoint("J" + idx++);
        Vector t = new Vector(0, scene.radius() / n);
        for( ; idx < n; idx++){
            Joint joint = skeleton.addJoint("J" +idx, "J" + (idx - 1));
            joint.translate(t);
        }
        //2. create solvers
        skeleton.enableIK();
        //3. add targets
        skeleton.addTarget("J" + --idx);
        return skeleton;
    }


    //Interaction methods

    public void mouseMoved(){
        focus = mouseY > 0.7 * height ? controlScene : mainScene;
        focus.mouseTag();
    }

    public void mouseDragged(){
        if(focus == controlScene && focus.node() instanceof ik.trik.expressive.Slider){
            focus.node().interact("OnMovement", new Vector(focus.mouseX(), focus.mouseY()));
        }else {
            if (mouseButton == LEFT) {
                focus.mouseSpin();
            } else if (mouseButton == RIGHT)
                focus.mouseTranslate(0);
            else
                focus.moveForward(mouseX - pmouseX);
        }
    }

    public void mouseReleased(){
        if(focus.node() instanceof ik.trik.expressive.Slider){
            focus.node().interact("OnFinishedMovement", new Vector(focus.mouseX(), focus.mouseY()));
        }
    }

    public void mouseWheel(MouseEvent event) {
        if(focus != controlScene && focus.node() == null) focus.scale(event.getCount() * 50);
    }

    public void keyPressed(){
        if(key == 'r' || key == 'R'){
            panel.play();
        }

        if(key == 's' || key == 'S'){
            //save skeleton posture
            panel.savePosture();
        }

        if(key == 'e' || key == 'E'){
            panel.toggleCurrentKeyPoint();
        }

        if(key == 'd' || key == 'D'){
            panel.deletePostureAtKeyPoint();
        }

        if(Character.isDigit(key)){
            panel._postureInterpolator.setSpeed(Integer.valueOf("" + key));
        }
    }

    public void mouseClicked(MouseEvent event) {
        if(focus == mainScene) {
            if (event.getCount() == 2)
                if (event.getButton() == LEFT)
                    focus.focus();
                else
                    focus.align();
        } else if(focus == controlScene){
            if(focus.node() != null) focus.node().interact("onClicked", event.getButton());
        }
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{"ik.trik.animation.AnimationTest"});
    }


}
