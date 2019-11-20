package leapMotion;
/*
import nub.core.Node;
import nub.core.Graph;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import de.voidplus.leapmotion.*;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sebchaparr on 31/07/18.

public class LeapMotionTest2 extends PApplet {
    HashMap<String, Node> fingers;

    LeapMotion leap;
    PVector[] previousPosition = new PVector[10];

    Node index, index_w;

    // frames stuff:
    Scene scene;

    public void settings() {
        size(800, 600, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setFOV(PI / 3);
        scene.setRadius(1500);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.fit(1);
        Node[] shapes = new Node[10];
        for (int i = 0; i < shapes.length; i++) {
            if(i == i)break;
            shapes[i] = new Node(scene, shape());
            shapes[i].setPosition( (i*1.f/shapes.length)*scene.radius()*2 - scene.radius(),0,0);
            shapes[i].setPickingThreshold(0);
            scene.setTrackedNode("LEAP"+i, shapes[i]);
        }
        smooth();
        setupLeapMotion();
        index = new Node(scene, shape());
        index.setTranslation(10,10,10);
        index.setPickingThreshold(0);
        index_w = new Node(scene, shape());
        index_w.setTranslation(-10,-10,-10);
        index_w.setPickingThreshold(0);
    }

    PShape shape() {
        PShape fig = createShape(SPHERE, 50);
        fig.setStroke(false);
        fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
        return fig;
    }

    public void draw() {
        background(100);
        scene.drawAxes();
        scene.render();
        updatePos();
        scene.beginHUD();
        for (Hand hand : leap.getHands ()) {
            hand.draw();
        }
        scene.endHUD();

    }

    Vector worldLocation(PVector rawPosition){
        Vector position = new Vector();
        position.setX(map(rawPosition.x, -200, 200, -scene.radius(), scene.radius()));
        position.setY(map(rawPosition.y, 0, 500, -scene.radius(), scene.radius()));
        position.setZ(map(rawPosition.z, -200, 200, -scene.radius(), scene.radius()));
        return position;
    }

    void updateHands(){

    }

    void updateHand(Hand hand){
        updateFinger("pinky", hand.getPinkyFinger());
        updateFinger("ring", hand.getRingFinger());
        updateFinger("middle", hand.getMiddleFinger());
        updateFinger("index", hand.getIndexFinger());
    }

    void updateFinger(String name, Finger finger){
        Node frame = fingers.get(name);
        frame.setPosition(worldLocation(finger.getRawPositionOfJointMcp()));
        frame = frame.reference();
        frame.setPosition(worldLocation(finger.getRawPositionOfJointPip()));
        frame = frame.reference();
        frame.setPosition(worldLocation(finger.getRawPositionOfJointDip()));
        frame = frame.reference();
        frame.setPosition(worldLocation(finger.getRawPositionOfJointTip()));
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
                PVector pv = hand.getIndexFinger().getPosition();
                Vector p = scene.location(new Vector(pv.x, pv.y, pv.z/50));
                index.setTranslation(p.get());
                Vector pw = worldLocation(hand.getIndexFinger().getRawPosition());
                index_w.setTranslation(pw);
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
                System.out.println(hand.getIndexFinger().getPosition());
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
            PVector position = f.getPosition();
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
            //Avoid Jittering
            PVector delta = PVector.sub(position,previousPosition[i]);
            if(delta.x*delta.x + delta.y*delta.y < 15){
                //no movement
                return;
            }
            previousPosition[i] = position;
            delta.z *= min(height, width)/50.f;

            if(scene.trackedNode("LEAP" + i) != null )scene.translate("LEAP" + i,delta.x, delta.y, delta.z);
            i++;
        }
    }

    public void mouseMoved() {
        scene.mouseTag();
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
        scene.scale(event.getCount() * 20);
    }


    void setupLeapMotion(){
        leap = new LeapMotion(this);
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"leapMotion.LeapMotionTest2"});
    }
}

*/