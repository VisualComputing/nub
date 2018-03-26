package ik.flock;

import common.InteractiveNode;
import frames.core.Node;
import frames.primitives.Vector;
import frames.processing.Mouse;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 11/03/18.
 */
public class Flock extends PApplet {
    static int count = 0;
    Scene scene;
    //flock bounding box
    static int flockWidth = 1280;
    static int flockHeight = 720;
    static int flockDepth = 600;
    static boolean avoidWalls = true;
    static float hue = 255;
    static int FPS = 100;

    int initBoidNum = 100; // amount of boids to start the program with
    static ArrayList<Boid> flock;
    static Node thirdPerson;

    String shapePath = "/testing/data/objs/TropicalFish01.obj";
    String texturePath = "/testing/data/objs/TropicalFish01.jpg";


    public void settings() {
        size(1000, 800, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        //scene.setSequentialTimers();
        scene.mouse().setMode(Mouse.Mode.CLICK);
        scene.setBoundingBox(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
        scene.setAnchor(scene.center());
        InteractiveNode eye = new InteractiveNode(scene);
        scene.setEye(eye);
        scene.setFieldOfView(PI / 3);
        //interactivity defaults to the eye
        scene.setDefaultGrabber(eye);
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
            flock.add(new Boid(scene, new PVector(flockWidth / 2, flockHeight / 2, flockDepth / 2), shapePath, texturePath));
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

        for(Node n : scene.nodes()){
            //if(n instanceof Shape) ((Shape) n).draw();
        }

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
            case 'w' :
                for(Boid boid : flock){
                    boid.norun = !boid.norun;
                }

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
                if (scene.eye().reference() != null) {
                    scene.eye().setReference(null);
                    scene.lookAt(scene.center());
                    scene.fitBallInterpolation();
                    //TODO broken: eye got removed from inputHandler
                    /*
                    scene.lookAt(scene.center());
                    scene.fitBallInterpolation();
                    scene.eye().setReference(null);
                    //*/
                } else if (thirdPerson != null) {
                    ((Node) scene.eye()).setReference(thirdPerson);
                    scene.interpolateTo(thirdPerson);
                }
                break;
            case '+':
                for (Boid boid : flock) {
                    boid.setPeriod(boid.period() - 2);
                    /*
                    FPS = (int)boid.period()-2;
                    frameRate(boid.period());
                    boid.setPeriod(1000/FPS);
                    //*/
                }
                break;
            case '-':
                for (Boid boid : flock) {
                    boid.setPeriod(boid.period() + 2);
                    /*
                    //boid.setPeriod(60);
                    FPS = (int)boid.period()+2;
                    frameRate(boid.period());
                    boid.setPeriod(1000/FPS);
                    //*/
                }
                break;
            case 'u':
                for (Boid boid : flock)
                    boid.toggle();
                break;
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.flock.Flock"});
    }
}
