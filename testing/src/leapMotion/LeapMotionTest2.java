package leapMotion;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import de.voidplus.leapmotion.*;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;
import processing.event.MouseEvent;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 31/07/18.
 */
public class LeapMotionTest2 extends PApplet {
    //TODO : Update
    LeapMotion leap;
    PVector[] previousPosition = new PVector[10];

    // frames stuff:
    Scene scene;

    public void settings() {
        size(800, 600, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setFieldOfView(PI / 3);
        scene.setRadius(1500);
        scene.fitBallInterpolation();
        Shape[] shapes = new Shape[10];
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = new Shape(scene, shape());
            shapes[i].setPosition( (i*1.f/shapes.length)*scene.radius()*2 - scene.radius(),0,0);
            //scene.randomize(shapes[i]);
            //shapes[i].setRotation(new Quaternion());
            scene.setTrackedFrame("LEAP"+i, shapes[i]);
        }
        smooth();
        setupLeapMotion();
    }

    PShape shape() {
        PShape fig = createShape(BOX, 150);
        fig.setStroke(color(0, 255, 0));
        fig.setStrokeWeight(3);
        fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
        return fig;
    }

    public void draw() {
        background(0);
        scene.drawAxes();
        scene.traverse();
        updatePos();
    }

    ArrayList<Finger> sortedFingers(){
        ArrayList<Finger> fingers = new ArrayList<Finger>();
        boolean trackingHand = false;
        if(leap.getLeftHand() != null){
            if(leap.getLeftHand().isLeft()) {
                trackingHand = true;
                Hand hand = leap.getLeftHand();
                fingers.add(hand.getPinkyFinger());
                fingers.add(hand.getRingFinger());
                fingers.add(hand.getMiddleFinger());
                fingers.add(hand.getIndexFinger());
                fingers.add(hand.getThumb());
            }
        }

        if(!trackingHand){
            for(int i = 0; i < 5; i++){
                fingers.add(null);
            }
        }

        trackingHand = false;

        if(leap.getRightHand() != null){
            if(leap.getRightHand().isRight()) {
                trackingHand = true;
                Hand hand = leap.getRightHand();
                fingers.add(hand.getThumb());
                fingers.add(hand.getIndexFinger());
                fingers.add(hand.getMiddleFinger());
                fingers.add(hand.getRingFinger());
                fingers.add(hand.getPinkyFinger());
            }
        }

        if(!trackingHand){
            for(int i = 0; i < 5; i++){
                fingers.add(null);
            }
        }

        return fingers;
    }

    void updatePos(){
        int i = 0;
        for(Finger f : sortedFingers()){
            if(f == null){
                previousPosition[i] = null;
                i++;
                continue;
            }
            PVector position = f.getRawPosition();
            if(previousPosition[i] == null){
                previousPosition[i] = position;
                i++;
                continue;
            }
            if(PVector.dist(previousPosition[i], position) < 10){
                //no movement
                i++;
                continue;
            }
            PVector v = PVector.sub(position, previousPosition[i]);

            float min = 8;
            Vector velocity = new Vector((int)v.x - (int)v.x % min,(int)v.y - (int)v.y % min,(int)v.z - (int)v.z % min);
            Frame reference = scene.trackedFrame("LEAP" + i) == null ? null :
                    scene.trackedFrame("LEAP" + i).reference();
            previousPosition[i] = position;

            velocity = reference == null ? velocity : reference.displacement(velocity);
            scene.translate("LEAP" + i, velocity.x(), velocity.y(), velocity.z());
            i++;
        }
    }

    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT)
            scene.spin();
        else if (mouseButton == RIGHT)
            scene.translate();
        else
            scene.scale(scene.mouseDX());
    }

    public void mouseWheel(MouseEvent event) {
        scene.zoom(event.getCount() * 20);
    }


    void setupLeapMotion(){
        leap = new LeapMotion(this);
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"leapMotion.LeapMotionTest2"});
    }
}

