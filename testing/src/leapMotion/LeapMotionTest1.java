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

/**
 * Created by sebchaparr on 31/07/18.
 */
public class LeapMotionTest1 extends PApplet {
    //TODO : Update
    LeapMotion leap;
    PVector previousNormal;
    PVector previousPosition;

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
        Shape[] shapes = new Shape[50];
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = new Shape(scene, shape());
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
        background(0);
        scene.drawAxes();
        scene.traverse();
        if (isPicking())
            leapMotionPicking();
        else
            leapMotionInteraction();
    }

    void leapMotionPicking(){
        if(leap.getLeftHand() == null) return;
        if(leap.getLeftHand().getIndexFinger() == null) return;
        float y_max = leap.getDevices().get(0).getRange()*0.8f;
        float x_max = y_max*0.3f * tan(leap.getDevices().get(0).getHorizontalViewAngle()/2);

        PVector pos = leap.getLeftHand().getIndexFinger().getRawPosition();
        float y = max(min(pos.y, y_max), 0);
        float x = max(min(pos.x, x_max), -x_max);

        y = height*1.2f - map(y, 0, y_max, 0, height*1.2f);
        x = map(x, -x_max, x_max, 0, width*1.2f);

        scene.cast("LEAP", x, y);
        //hand.getPinchStrength();
        // draw picking visual hint
        pushStyle();
        strokeWeight(3);
        stroke(0, 255, 0);
        scene.drawCross(x, y, 30);
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
        PVector position = index.getRawPosition();
        if(previousPosition == null){
            previousPosition = index.getRawPosition();
            return;
        }

        if(PVector.dist(previousPosition, position) < 10){
            //no movement
            return;
        }

        PVector v = PVector.sub(position, previousPosition);

        //PVector center = new PVector(0, 140, 0);
        //Vector velocity = getDelta(position, center, 50, 1, 40);
        float min = 8;
        Vector velocity = new Vector((int)v.x - (int)v.x % min,(int)v.y - (int)v.y % min,(int)v.z - (int)v.z % min);
        Frame reference = scene.trackedFrame("LEAP") == null ? null :
                scene.trackedFrame("LEAP").reference();
        previousPosition = position;

        velocity = reference == null ? velocity : reference.displacement(velocity);
        scene.translate("LEAP", velocity.x(), velocity.y(), velocity.z());

        PVector normal = new PVector(leap.getLeftHand().getRaw().palmNormal().getX(),
                leap.getLeftHand().getRaw().palmNormal().getY(),
                leap.getLeftHand().getRaw().palmNormal().getZ());
        normal.mult(100);

        if(leap.getLeftHand().getThumb() == null) return;

        PVector axis =
                PVector.sub(leap.getLeftHand().getThumb().getRawPositionOfJointTip(),
                        leap.getLeftHand().getThumb().getRawPositionOfJointDip());

        System.out.println(axis);
        //System.out.println(normal);


        Vector direction = new Vector(leap.getLeftHand().getRawDirection().x,
                leap.getLeftHand().getRawDirection().y,
                leap.getLeftHand().getRawDirection().z);
    }

    Vector getDelta(PVector position, PVector center, float threshold, float speed, float step){
        PVector offset = PVector.sub(position, center);
        PVector direction = new PVector();
        offset.normalize(direction);
        Vector delta = new Vector();
        if( abs(offset.x) > threshold ){
            delta.setX(direction.x*speed*(abs(offset.x)/step));
        }
        if( abs(offset.y) > threshold ){
            delta.setZ(direction.y*speed*(abs(offset.y)/step));
        }
        if( abs(offset.z) > threshold ){
            delta.setY(direction.z*speed*(abs(offset.z)/step));
        }
        return delta;
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
        scene.zoom(event.getCount() * 20);
    }


    void setupLeapMotion(){
        leap = new LeapMotion(this);
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"leapMotion.LeapMotionTest1"});
    }
}

