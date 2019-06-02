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

/**
 * Created by sebchaparr on 31/07/18.

public class LeapMotionTest1 extends PApplet {
    LeapMotion leap;
    Vector previousNormal;
    PVector previousPosition;
    // frames stuff:
    Scene scene;

    public void settings() {
        size(800, 600, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setFOV(PI / 3);
        scene.setRadius(1500);
        scene.fit(1);
        Node[] shapes = new Node[50];
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = new Node(scene, shape());
            shapes[i].setPickingThreshold(0);
            scene.randomize(shapes[i]);
            shapes[i].setRotation(new Quaternion());
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
        background(255);
        scene.drawAxes();
        scene.render();
        if (isPicking())
            leapMotionPicking();
        else
            leapMotionInteraction();
        scene.beginHUD();
        for (Hand hand : leap.getHands ()) {
            hand.draw();
            pushStyle();
            fill(255,255,0, 200);
            ellipse(hand.getPosition().x, hand.getPosition().y, 30,30);
            popStyle();
        }
        scene.endHUD();
    }


    void leapMotionPicking(){
        if(leap.getLeftHand() == null) return;
        if(leap.getLeftHand().getIndexFinger() == null) return;
        PVector pos = leap.getLeftHand().getIndexFinger().getPosition();
        scene.cast("LEAP", pos.x, pos.y);
        // draw picking visual hint
        pushStyle();
        strokeWeight(3);
        stroke(0, 255, 0);
        scene.drawCross(pos.x, pos.y, 30);
        popStyle();
    }

    void leapMotionInteraction() {
        if(leap.getLeftHand() == null){
            previousPosition = null;
            previousNormal = null;
            return;
        }
        if(leap.getLeftHand().getIndexFinger() == null){
            previousPosition = null;
            previousNormal = null;
            return;
        }
        Finger index = leap.getLeftHand().getIndexFinger();
        PVector position = index.getPosition();

        if(previousPosition == null){
            previousPosition = index.getPosition();
            return;
        }

        //Avoid Jittering
        PVector delta = PVector.sub(position,previousPosition);
        if(delta.x*delta.x + delta.y*delta.y < 15){
            //no movement
            return;
        }
        previousPosition = position;
        delta.z *= min(height, width)/50.f;

        if(scene.trackedNode("LEAP") != null )scene.translate("LEAP",delta.x, delta.y, delta.z);


        //Rotation
        Vector normal = new Vector(leap.getLeftHand().getRaw().palmNormal().getX(),
                leap.getLeftHand().getRaw().palmNormal().getY(),
                leap.getLeftHand().getRaw().palmNormal().getZ());
        normal = new Quaternion(new Vector(1,0,0), -HALF_PI).rotate(normal);

        pushStyle();
        strokeWeight(3);
        stroke(0, 255, 0);
        //scene.drawArrow(normal, new Vector(), 15);
        popStyle();
        pushStyle();
        strokeWeight(3);
        stroke(0, 255, 0);
        scene.drawCross(position.x, position.y, 30);
        System.out.println("H - >"  + normal);

        popStyle();

        if(previousNormal == null){
            previousNormal = normal.get();
            return;
        }

        if(Vector.angleBetween(normal, previousNormal) < radians(10)){
            return;
        }
        Quaternion rotation = new Quaternion(previousNormal, normal);
        Quaternion quat = new Quaternion();
        Node frame =  scene.defaultNode("LEAP");
        if(frame.reference() != null){
            quat = frame.orientation().inverse();
        }
        rotation = Quaternion.multiply(quat, rotation);
        frame.rotate(rotation);
        previousNormal = normal;
    }

    boolean isPicking(){
        if(leap.getLeftHand() == null) return false;
        if(leap.getLeftHand().getIndexFinger() == null && leap.getLeftHand().getThumb() == null) return false;
        float diff = PVector.dist(leap.getLeftHand().getIndexFinger().getRawPosition(), leap.getLeftHand().getThumb().getRawPosition());
        return diff < 30;
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
        scene.scale(event.getCount() * 20);
    }


    void setupLeapMotion(){
        leap = new LeapMotion(this);
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"leapMotion.LeapMotionTest1"});
    }
}

*/