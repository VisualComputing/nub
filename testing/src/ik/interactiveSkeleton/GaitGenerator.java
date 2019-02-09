package ik.interactiveSkeleton;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.TimingTask;
import ik.common.Joint;
import processing.core.PApplet;
import processing.event.MouseEvent;


/**
 * Created by sebchaparr on 07/02/18.
 */

public class GaitGenerator extends PApplet {
    //TODO : Update
    Scene scene;
    Frame target;

    float boneLength = 50;
    float targetRadius = 7;


    public void settings() {
        size(700, 700, P3D);
    }

    Frame ref;
    public void setup() {
        Joint.deph = true;
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setFOV(PI / 3);
        scene.setRadius(boneLength * 5);
        scene.fit(1);
        ref = new Frame(scene, createShape(BOX, 90, boneLength, 5.2f*boneLength));

        int n = 5;
        for(int i = 0; i < n; i++){
            Frame f1 = new Frame(scene, new Vector(-boneLength,0,i*5*boneLength/n - boneLength*10/n), new Quaternion(), 1);
            f1.setReference(ref);
            Frame f2 = new Frame(scene, new Vector(boneLength,0,i*5*boneLength/n - boneLength*10/n), new Quaternion(new Vector(0,1,0), PI), 1);
            f2.setReference(ref);
            float x = random(0,30)*180;
            leg(f1, i % 2 == 1, i % 2 == 1, 0);
            leg(f2, i % 2 == 1, i % 2 == 0, 0);
        }


        //for(String key : keys){
         //   ArrayList<Joint> skeleton = limbs.get(key);
         //   targets.get(key).setPosition(skeleton.get(skeleton.size()-1).position());
            //TESTING WITH FABRIK
         //   scene.addIKTarget(skeleton.get(skeleton.size() - 1), targets.get(key));
        //}
    }

    public void leg(Frame reference, boolean mirror, boolean inv, float d){
        Frame target = new Frame(scene);
        target.setReference(reference);

        //Create a leg
        Vector v = new Vector(-boneLength, 0, 0);
        Joint j1 = new Joint(scene);
        j1.setReference(reference);
        j1.setDrawConstraint(false);
        BallAndSocket c1 = new BallAndSocket(radians(80),radians(80),radians(80), radians(80));
        c1.setRestRotation(j1.rotation().get(), new Vector(0,1,0), new Vector(-1,0,0));
        j1.setConstraint(c1);

        Joint j2 = new Joint(scene);
        j2.setDrawConstraint(false);
        j2.setReference(j1);
        j2.translate(-boneLength,0,0);

        Joint j3 = new Joint(scene);
        j3.setDrawConstraint(false);
        j3.setReference(j2);
        j3.translate(new Quaternion(new Vector(0,0,1), radians(-35)).rotate(v));

        Joint j4 = new Joint(scene);
        j4.setDrawConstraint(false);
        j4.setReference(j3);
        j4.translate(new Quaternion(new Vector(0,0,1), radians(-35)).rotate(v));

        //Apply constraints
        BallAndSocket c2 = new BallAndSocket(radians(25),radians(25));
        c2.setRestRotation(j2.rotation().get(), j3.translation().orthogonalVector(), j3.translation().get());
        j2.setConstraint(c2);

        BallAndSocket c3 = new BallAndSocket(radians(15),radians(10),radians(15), radians(15));
        c3.setRestRotation(j3.rotation().get(), j4.translation().orthogonalVector(), j4.translation().get());
        j3.setConstraint(c3);

        //Set initial configuration
        j1.rotate(new Quaternion(new Vector(0,0,1), radians(-30)));

        scene.registerTreeSolver(j1);
        target.setPosition(j4.position());
        scene.addIKTarget(j4, target);
        Vector o = reference.location(new Vector(), j4);
        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                updateTarget(scene.timingHandler().frameCount()*3, o, target, mirror, inv, d);
            }
        };
        scene.registerTask(task);
        task.run(40);
    }


    public void draw() {
        background(0);
        lights();
        fill(255,0,0);
        scene.drawAxes();
        scene.render();
        ref.translate(0,0,-0.5f);
        if(ref.translation().z() < -boneLength * 3f)
            ref.setTranslation(0, 0, boneLength*3f);
    }

    public void updateTarget(float t, Vector o, Frame target, boolean mirror, boolean inv, float d){
        t += d;
        float z = -boneLength/2.5f * cos(radians(t));
        z = mirror ? -z : z;
        float y = boneLength/2.5f * abs(sin(radians(t)));
        if(!inv && t % 360 > 180 ) y = 0;
        else if(inv && t % 360 < 180 ) y = 0;

        target.setTranslation(o.x() - boneLength*0.4f, o.y()*0.95f - y, o.z() - z);
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
        PApplet.main(new String[]{"ik.interactiveSkeleton.GaitGenerator"});
    }
}
