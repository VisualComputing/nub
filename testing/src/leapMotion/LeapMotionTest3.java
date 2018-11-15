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
    PVector previousPosition;
    PVector center, current;
    float maxSpeed = 5;

    boolean updateCenter = false;

    // frames stuff:
    Scene scene;

    float threshold = 50f; // In terms of pixels

    public void settings() {
        size(800, 600, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setAperture(PI / 3);
        scene.setRadius(1500);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.fitBallInterpolation();
        center = new PVector();
        Shape[] shapes = new Shape[50];
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = new Shape(scene, shape());
            scene.randomize(shapes[i]);
            shapes[i].setRotation(new Quaternion());
        }
        center = new PVector(width/2, height/2);

        //Draw scene on XY, XZ and YZ PLANES



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
        PVector position = null;
        background(0);
        scene.drawAxes();
        scene.traverse();
        if (isPicking())
            leapMotionPicking();
        else
            position = leapMotionInteraction();
        scene.beginHUD();
        if(position != null)drawPAD(0, 3*height/4, height/4, position);
        scene.endHUD();

    }

    void drawPAD(float x, float y, float w, PVector position){
        pushMatrix();
        pushStyle();
        translate(x + w/2.f,y + w/2.f);
        noStroke();
        fill(100, 50);
        rect(-w/2.f, -w/2.f, w, w);
        fill(100);
        ellipse(0, 0, w/10, w/10);
        fill(100);
        PVector vector = PVector.sub(position, center).mult(w*1.f/width);
        if(vector.mag() > w/2){
            vector = vector.normalize().mult(w/2);
        }
        ellipse(vector.x, vector.y, w/10, w/10);
        stroke(255);
        line(vector.x, vector.y, 0,0);
        popStyle();
        popMatrix();
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

    PVector leapMotionInteraction() {
        if(leap.getLeftHand() == null){
            previousPosition = null;
            return null;
        }
        if(leap.getLeftHand().getIndexFinger() == null){
            previousPosition = null;
            return null;
        }
        Finger index = leap.getLeftHand().getIndexFinger();
        PVector position = index.getPosition();

        if(previousPosition == null){
            previousPosition = index.getPosition();
            return null;
        }

        //Avoid Jittering
        PVector delta = PVector.sub(position,center);
        if(delta.x*delta.x + delta.y*delta.y < 15){
            //no movement
            return null;
        }
        previousPosition = position;
        delta.z = 0;
        //damp vector
        if(delta.mag() > width/2.f){
            delta = delta.normalize().mult(width/2.f);
        }
        //linear interpolation
        delta.x = map(delta.x, -width/2.f, width/2.f, -maxSpeed, maxSpeed);
        delta.y = map(delta.y, -width/2.f, width/2.f, -maxSpeed, maxSpeed);


        scene.translate("LEAP",delta.x, delta.y);
        return position;
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
        scene.scale(event.getCount() * 20);
    }


    void setupLeapMotion(){
        leap = new LeapMotion(this);
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"leapMotion.LeapMotionTest3"});
    }

}

