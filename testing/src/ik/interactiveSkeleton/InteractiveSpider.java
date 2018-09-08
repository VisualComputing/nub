package ik.interactiveSkeleton;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.Interpolator;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import ik.common.LinearBlendSkinning;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 8/09/18.
 */
public class InteractiveSpider extends PApplet {

    /*
    * Idea taken from: https://www.youtube.com/watch?v=GtHzpX0FCFY
    *
    * */

    boolean showSkeleton = false;
    Scene scene;
    Shape shape;

    float bodyWidth = 100;
    float bodyHeight = 50;
    float bodyLength = 200;
    int legs = 8;
    float velocity = 0.2f;

    Interpolator targetInterpolator;

    float targetRadius = 7;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setFieldOfView(PI / 3);
        scene.fitBallInterpolation();
        //create targets
        Frame[] targets = new Frame[legs];

        for(int i = 0; i < legs; i++){
            targets[i] = createTarget(targetRadius);
        }
        shape = new Shape(scene, createShape(BOX, bodyWidth, bodyHeight, bodyLength));
        spiderSkeleton(shape,legs,bodyWidth,bodyHeight,bodyLength, targets);
        shape.rotate(new Quaternion(new Vector(0,1,0), PI/2));
    }

    public Shape createTarget(float radius){
        PShape redBall = createShape(SPHERE, radius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));
        return new Shape(scene, redBall);
    }


    public void draw() {
        //skinning.updateParams();
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();
        //comment this line if you're using Linear Blending Skinning with CPU
        //shader(skinning.shader);
        scene.traverse();
        //resetShader();
        //Uncomment to use Linear Blending Skinning with CPU
        shape.translate(velocity,0,0);
    }

    public Joint leg(Frame reference, Vector upper, Vector middle, Vector lower, Frame target, boolean invert){
        Joint j1 = new Joint(scene);
        j1.setReference(reference);
        j1.setPosition(reference.worldLocation(upper));
        Joint j2 = new Joint(scene);
        j2.setReference(j1);
        j2.setPosition(reference.worldLocation(middle));
        Joint j3 = new Joint(scene);
        j3.setReference(j2);
        j3.setPosition(reference.worldLocation(lower));
        j1.setRoot(true);
        addIk(j1, j3, target, invert);
        return j1;
    }

    public void addIk(Frame root, Frame endEffector, Frame target, boolean invert){
        target.setReference(root.reference());
        target.setPosition(endEffector.position());
        legPath(target, Vector.distance(root.position(), endEffector.position())*0.1f, invert);
        Solver solver = scene.registerTreeSolver(root);
        solver.error = 0.01f;
        scene.addIKTarget(endEffector, target);
    }

    public Interpolator legPath(Frame target, float amplitude, boolean invert) {
        Interpolator targetInterpolator = new Interpolator(target);
        targetInterpolator.setLoop();
        targetInterpolator.setSpeed(5.2f);
        // Create an initial path
        int nbKeyFrames = 10;
        float step = 2*PI / (nbKeyFrames - 1);
        int inv = invert ? 1 : -1;
        for (int i = 0; i < nbKeyFrames; i++) {
            float z = target.translation().z() + inv*amplitude*cos(step*i);
            float y = target.translation().y() - abs(amplitude*sin(step*i));

            Frame iFrame = new Frame(scene);
            iFrame.setReference(target.reference());
            iFrame.setTranslation(new Vector(target.translation().x(), y, z));
            targetInterpolator.addKeyFrame(iFrame);
        }
        targetInterpolator.start();
        return targetInterpolator;
    }



    public void spiderSkeleton(Frame reference, int legs, float bodyWidth, float bodyHeigth, float bodyLength, Frame[] targets){
        if(legs < 4 ) legs = 4;
        legs = legs% 2 != 0 ? legs + 1 : legs;
        boolean invert = false;

        for(int i = 0; i < legs/2; i++){
            //offset w.r.t length
            float z = i*(bodyLength*0.8f/(legs/2)) - bodyLength*0.3f;
            float upper_x = bodyWidth/2.f;
            float middle_x = upper_x + bodyWidth/2.f;
            float lower_x = middle_x + bodyWidth/2.f;

            leg(reference, new Vector(-upper_x, 0, z), new Vector(-middle_x, -bodyHeigth/2, z), new Vector(-lower_x, bodyHeigth*2, z), targets[2*i], invert);
            leg(reference, new Vector(upper_x, 0, z), new Vector(middle_x, -bodyHeigth/2, z), new Vector(lower_x, bodyHeigth*2, z), targets[2*i +1], !invert);
            invert = !invert;
        }
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
            scene.zoom(scene.mouseDX());
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
        PApplet.main(new String[]{"ik.interactiveSkeleton.InteractiveSpider"});
    }
}
