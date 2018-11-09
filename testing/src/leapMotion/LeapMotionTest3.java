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
public class LeapMotionTest3 extends PApplet {
    LeapMotion leap;
    PVector previousNormal;
    PVector center, current;
    // frames stuff:
    Scene scene;

    float threshold = 1f;
    float y_max;
    float x_max;
    boolean rangeSet = false;

    public void settings() {
        size(800, 600, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setFieldOfView(PI / 3);
        scene.setRadius(1500);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.fitBallInterpolation();
        center = new PVector();
        /*Shape[] shapes = new Shape[50];
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = new Shape(scene, shape());
            scene.randomize(shapes[i]);
            shapes[i].setRotation(new Quaternion());
        }*/
        center = new PVector(width/2, height/2);
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
        if(!rangeSet){
            rangeSet = setupRange();
            System.out.println("max = " + x_max);
        }
        background(0);
        scene.drawAxes();
        scene.traverse();
        scene.beginHUD();
        fill(255,0,0);
        ellipse(center.x, center.y, x_max*threshold, y_max*threshold);
        fill(255,255,0);
        ellipse(center.x, center.y, 10, 10);
        if(current != null){
            ellipse(current.x, current.y, 10, 10);
        }

        scene.endHUD();
        if (isPicking())
            leapMotionPicking();
        else
            leapMotionInteraction();

    }

    void setCenter(){
        if(leap.getLeftHand() == null) return;
        if(leap.getLeftHand().getIndexFinger() == null) return;
        if(leap.getLeftHand().getThumb() == null) return;

        float y_max = leap.getDevices().get(0).getRange()*0.8f;
        float x_max = y_max*0.3f * tan(leap.getDevices().get(0).getHorizontalViewAngle()/2);

        PVector pos = leap.getLeftHand().getIndexFinger().getRawPosition();
        float y = max(min(pos.y, y_max), 0);
        float x = max(min(pos.x, x_max), -x_max);

        center.y = height*1.2f - map(y, 0, y_max, 0, height*1.2f);
        center.x = map(x, -x_max, x_max, 0, width*1.2f);
    }



    void leapMotionPicking(){
        if(leap.getLeftHand() == null) return;
        if(leap.getLeftHand().getIndexFinger() == null) return;
        if(leap.getLeftHand().getThumb() == null) return;

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
            previousNormal = null;
            return;
        }
        if(leap.getLeftHand().getIndexFinger() == null){
            previousNormal = null;
            return;
        }
        Finger index = leap.getLeftHand().getIndexFinger();
        PVector pos = leap.getLeftHand().getIndexFinger().getRawPosition();
        float y = max(min(pos.y, y_max), 0);
        float x = max(min(pos.x, x_max), -x_max);
        pos.y = height*1.2f - map(y, 0, y_max, 0, height*1.2f);
        pos.x = map(x, -x_max, x_max, 0, width*1.2f);

        PVector v = PVector.sub(pos, center);
        v.x *= 1.f/(threshold*x_max);
        v.y *= 1.f/(threshold*y_max);

        current = v;
        //Vector velocity = getDelta(position, center, 50, 1, 40);
        Frame reference = scene.trackedFrame("LEAP") == null ? null :
                scene.trackedFrame("LEAP").reference();

        scene.translate("LEAP", v.x, v.y);


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

    public void keyPressed(){
        setCenter();
    }

    public void mouseWheel(MouseEvent event) {
        scene.zoom(event.getCount() * 20);
    }


    void setupLeapMotion(){
        leap = new LeapMotion(this);
    }

    boolean setupRange(){
        if(!leap.getDevices().isEmpty()) {
            y_max = leap.getDevices().get(0).getRange() * 0.8f;
            x_max = y_max * 0.3f * tan(leap.getDevices().get(0).getHorizontalViewAngle() / 2);
            return true;
        }
        return false;
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"leapMotion.LeapMotionTest3"});
    }

}

