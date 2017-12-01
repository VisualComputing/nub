package third;

import common.InteractiveNode;
import processing.core.PApplet;
import processing.core.PVector;
import remixlab.core.Node;
import remixlab.input.Grabber;
import remixlab.primitives.Vector;
import remixlab.proscene.MouseAgent;
import remixlab.proscene.Scene;
import remixlab.timing.TimingTask;

import java.util.ArrayList;

/**
 * Flock is a work in progress still
 */
public class Flock extends PApplet {
    static int count=0;
    Scene scene;
    //flock bounding box
    static int flockWidth = 1280;
    static int flockHeight = 720;
    static int flockDepth = 600;
    static boolean avoidWalls = true;
    static float hue = 255;
    static int FPS = 50;

    int initBoidNum = 300; // amount of boids to start the program with
    static ArrayList<Boid> flock;
    static Node thirdPerson;

    public void settings() {
        size(1000, 800, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setSequentialTimers();
        scene.mouseAgent().setPickingMode(MouseAgent.PickingMode.CLICK);
        scene.setBoundingBox(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
        scene.setAnchor(scene.center());
        InteractiveNode eye = new InteractiveNode(scene);
        scene.setEye(eye);
        scene.setFieldOfView(PI/3);
        //interactivity defaults to the eye
        scene.setDefaultNode(eye);
        scene.fitBall();
        // create and fill the list of boids
        flock = new ArrayList();
        /*
        for (int i = 0; i < initBoidNum; i++)
            flock.add(new Boid(scene, new PVector(flockWidth/2, flockHeight/2, flockDepth/2 )));
        scene.startAnimation();
        */

        /*
        frameRate(FPS);
        for (int i = 0; i < initBoidNum; i++)
            flock.add(new Boid(scene, new PVector(flockWidth/2, flockHeight/2, flockDepth/2 )));
        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                for (Boid boid : flock) {
                    boid.run(flock);
                    boid.render();
                }
            }
        };
        scene.registerTask(task);
        task.run(1000/FPS);
        //*/

        /*
        frameRate(FPS);
        for (int i = 0; i < initBoidNum; i++) {
            Boid boid = new Boid(scene, new PVector(flockWidth/2, flockHeight/2, flockDepth/2 ));
            flock.add(boid);
            TimingTask task = new TimingTask() {
                @Override
                public void execute() {
                    boid.run(flock);
                    boid.render();
                }
            };
            scene.registerTask(task);
            task.run(1000/FPS);
        }
        //*/

        //frameRate(FPS);
        for (int i = 0; i < initBoidNum; i++)
            flock.add(new Boid(scene, new PVector(flockWidth/2, flockHeight/2, flockDepth/2 )));

        frameRate(FPS);
    }

    public void draw() {
        background(0);
        ambientLight(128, 128, 128);
        directionalLight(255, 255, 255, 0, 1, -100);
        noFill();
        stroke(255);

        line(0, 0, 0, 0, flockHeight, 0);
        line(0, 0, flockDepth, 0, flockHeight, flockDepth);
        line(0, 0, 0, flockWidth, 0, 0);
        line(0, 0, flockDepth, flockWidth, 0, flockDepth);

        line(flockWidth, 0, 0, flockWidth, flockHeight, 0);
        line(flockWidth, 0, flockDepth, flockWidth, flockHeight, flockDepth);
        line(0, flockHeight, 0, flockWidth, flockHeight, 0);
        line(0, flockHeight, flockDepth, flockWidth, flockHeight, flockDepth);

        line(0, 0, 0, 0, 0, flockDepth);
        line(0, flockHeight, 0, 0, flockHeight, flockDepth);
        line(flockWidth, 0, 0, flockWidth, 0, flockDepth);
        line(flockWidth, flockHeight, 0, flockWidth, flockHeight, flockDepth);

        //for (Boid boid : flock)
          //  boid.render();

        /*
        triggered = scene.timer().trigggered();
        for (Boid boid : flock) {
            if (triggered)
                boid.run(flock);
            boid.render();
        }
        */

        /*
        if(thirdPerson && scene.eye().reference() == null && !scene.interpolator().started())
            scene.eye().setReference(nodeInterpolator.node());
        */
    }

    public void keyPressed() {
        /*
        if(key == 'i') {
            thirdPerson = true;
            scene.interpolateTo(nodeInterpolator.node());
        }
        if(key == 'I') {
            thirdPerson = false;
            scene.eye().setReference(null);
        }
        */
        switch (key) {
            case 'a':
                scene.fitBall();
            case 't':
                scene.shiftTimers();
            case 'p':
                println("Frame rate: " + frameRate);
                break;
            case 'v':
                avoidWalls = !avoidWalls;
                break;
            case 's':
                for (Boid boid : flock)
                    boid.stop();
                break;
            case 'S':
                for (Boid boid : flock)
                    boid.start();
                break;
            case ' ':
                if(scene.eye().reference() != null) {
                    scene.eye().setReference(null);
                    scene.lookAt(scene.center());
                    scene.fitBallInterpolation();
                    //TODO broken: eye got removed from inputHandler
                    /*
                    scene.lookAt(scene.center());
                    scene.fitBallInterpolation();
                    scene.eye().setReference(null);
                    //*/
                }
                else if (thirdPerson != null) {
                    ((Node)scene.eye()).setReference(thirdPerson);
                    scene.interpolateTo(thirdPerson);
            }
                break;
                /*
            case '+':
                scene.setAnimationPeriod(scene.animationPeriod()-2, false);
                break;
            case '-':
                scene.setAnimationPeriod(scene.animationPeriod()+2, false);
                break;
            case ' ':
                if ( scene.avatar() == null && lastAvatar != null)
                    scene.setAvatar(lastAvatar);
                else
                    lastAvatar = scene.resetAvatar();
                break;
                */
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"third.Flock"});
    }
}
