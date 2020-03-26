package ik.interactiveSkeleton;

import nub.core.Node;
import nub.core.Graph;
import nub.core.constraint.BallAndSocket;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import nub.ik.visual.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;


/**
 * Created by sebchaparr on 07/02/18.
 */

public class GaitGenerator extends PApplet {
    //TODO : Update
    Scene scene;
    Node target;
    float y_floor = 0;

    float boneLength = 50;
    float targetRadius = 7;


    public void settings() {
        size(700, 700, P3D);
    }

    Node ref;
    public void setup() {
        Joint.depth = true;
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setFOV(PI / 3);
        scene.setRadius(boneLength * 5);
        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI / 4));
        scene.fit();

        ref = new Node();
        ref.setPickingThreshold(0.0001f);
        for(int k = 0; k < 15; k++) {
            PShape body = createShape(BOX, 90, boneLength, 5.2f * boneLength);
            body.setStroke(false);
            body.setTexture(scene.pApplet().loadImage(scene.pApplet().sketchPath() + "/testing/data/textures/spider.jpg"));
            body.setShininess(10.0f);

            body.setFill(color(random(255), random(255), random(255)));
            Node com = new Node(body);

            com.setReference(ref);
            int n = 5;
            float x = random(0, 30) * 180;
            for (int i = 0; i < n; i++) {
                Node f1 = new Node(new Vector(-boneLength, 0, i * 5 * boneLength / n - boneLength * 10 / n), new Quaternion(), 1);
                f1.setReference(com);
                Node f2 = new Node(new Vector(boneLength, 0, i * 5 * boneLength / n - boneLength * 10 / n), new Quaternion(new Vector(0, 1, 0), PI), 1);
                f2.setReference(com);
                leg(f1, i % 2 == 1, i % 2 == 1, x);
                leg(f2, i % 2 == 1, i % 2 == 0, x);
            }
            com.translate(k/3 * scene.radius()/2f - scene.radius(),0, k % 3 == 0 ? -2.5f * boneLength : k % 3 == 1 ? 0 : 2.5f * boneLength);
            com.scale(0.25f);
        }


        //for(String key : keys){
         //   ArrayList<Joint> skeleton = limbs.get(key);
         //   targets.get(key).setPosition(skeleton.get(skeleton.size()-1).position());
            //TESTING WITH FABRIK
         //   scene.addIKTarget(skeleton.get(skeleton.size() - 1), targets.get(key));
        //}
    }

    public void leg(Node reference, boolean mirror, boolean inv, float d){
        Node target = new Node();
        target.setReference(reference);

        //Create a leg
        Vector v = new Vector(-boneLength, 0, 0);
        Joint j1 = new Joint();
        j1.setReference(reference);
        j1.setDrawConstraint(false);
        BallAndSocket c1 = new BallAndSocket(radians(80),radians(80),radians(80), radians(80));
        c1.setRestRotation(j1.rotation().get(), new Vector(0,1,0), new Vector(-1,0,0));
        j1.setConstraint(c1);

        Joint j2 = new Joint();
        j2.setDrawConstraint(false);
        j2.setReference(j1);
        j2.translate(-boneLength,0,0);

        Joint j3 = new Joint();
        j3.setDrawConstraint(false);
        j3.setReference(j2);
        j3.translate(new Quaternion(new Vector(0,0,1), radians(-35)).rotate(v));

        Joint j4 = new Joint();
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

        Solver solver = scene.registerTreeSolver(j1);
        solver.setMaxIterations(3);
        solver.setTimesPerFrame(1f);

        target.setPosition(j4.position());
        y_floor = j4.position().y();
        scene.addIKTarget(j4, target);
        Vector o = reference.location(new Vector(), j4);
        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                updateTarget(Scene.timingHandler().frameCount *8, o, target, mirror, inv, d);
            }
        };
        task.run(100);
    }


    public void draw() {
        background(0);
        ambientLight(102, 102, 102);
        lightSpecular(204, 204, 204);
        directionalLight(102, 102, 102, 0, 0, -1);
        specular(255, 255, 255);
        fill(255,0,0);
        scene.drawAxes();
        scene.render();
        ref.translate(0,0,-0.5f);
        if(ref.translation().z() < -boneLength * 12f)
            ref.setTranslation(0, 0, boneLength*6f);
        fill(100);
        shininess(10.0f);
        translate(0,y_floor/4 + scene.radius()*0.025f,0);
        box(2.5f*scene.radius(), scene.radius()*0.05f, 5f*scene.radius());
    }

    public void updateTarget(float t, Vector o, Node target, boolean mirror, boolean inv, float d){
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


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactiveSkeleton.GaitGenerator"});
    }
}
