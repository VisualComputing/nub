package ik.interactive;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 27/10/18.
 */
public class SkeletonBuilder extends PApplet{
    Scene scene;
    float radius = 5;

    /*Create different skeletons to interact with*/
    //Choose FX2D, JAVA2D, P2D or P3D
    String renderer = P3D;

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactive.SkeletonBuilder"});
    }

    public void settings() {
        size(700, 700, renderer);
    }

    public void setup(){
        if(renderer.equals(P2D)) radius = 10;
        scene = new Scene(this);
        scene.fitBallInterpolation();
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        createInteractiveJoint().setRoot(true);
    }

    public void draw() {
        background(0);
        scene.drawAxes();
        scene.traverse();
    }


    //mouse events
    @Override
    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged(MouseEvent event) {
        if (mouseButton == RIGHT && event.isControlDown()) {
            Vector mouse = new Vector(scene.mouse().x(), scene.mouse().y());
            scene.defaultFrame().interact("OnAdding", scene.location(mouse));
            return;
        } else if (mouseButton == LEFT) {
            scene.spin();
        }
        else if (mouseButton == RIGHT) {
            scene.translate();
        } else {
            scene.zoom(scene.mouseDX());
        }
        scene.defaultFrame().interact("Reset");
    }

    public void mouseReleased(){
        Vector mouse = new Vector(scene.mouse().x(), scene.mouse().y());
        mouse = scene.location(mouse);
        mouse = Vector.projectVectorOnPlane(mouse, scene.viewDirection());

        mouse.add(scene.defaultFrame().position());

        scene.defaultFrame().interact("Add", mouse);
    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 2)
            if (event.getButton() == LEFT)
                if(event.isControlDown())
                    scene.defaultFrame().interact("Remove");
                else
                    scene.focus();
            else
                scene.align();
    }

    public void keyPressed(){
        if(key == '+'){
            createInteractiveJoint().setRoot(true);
        }

        if(key == 'A' || key == 'a'){
            addTreeSolver();
        }
    }

    //------------------------------------

    //Interactive actions - same method found in Graph Class
    public Vector translateDesired(Frame frame){
        float dx = mouseX - scene.screenLocation(frame.position()).x();
        float dy = mouseY - scene.screenLocation(frame.position()).y();

        dy = scene.isRightHanded() ? -dy : dy;
        if(scene.type() == Graph.Type.PERSPECTIVE){
            float k = (float) Math.tan(scene.fieldOfView() / 2.0f) * Math.abs(
                    scene.eye().location(scene.isEye(frame) ? scene.anchor() : frame.position())._vector[2] * scene.eye().magnitude());
            dx *= 2.0 * k / scene.height();
            dy *= 2.0 * k / scene.height();
        }
        else {
            float[] wh = scene.boundaryWidthHeight();
            dx *= 2.0 * wh[0] / scene.width();
            dy *= 2.0 * wh[1] / scene.height();
        }
        Vector eyeVector = new Vector(dx / scene.eye().magnitude(), dy / scene.eye().magnitude(), 0);
        return frame.reference() == null ? scene.eye().worldDisplacement(eyeVector) : frame.reference().displacement(eyeVector, scene.eye());
    }

    public Joint createInteractiveJoint(){
        return new Joint(scene, radius){
            Vector desiredTranslation;
            @Override
            public void interact(Object... gesture){
                String command = (String) gesture[0];
                if(command.matches("Add")){
                    if(desiredTranslation != null) {
                        addChild();
                    }
                    desiredTranslation = null;
                } else if(command.matches("OnAdding")){
                    desiredTranslation = translateDesired(this);
                } else if(command.matches("Reset")){
                    desiredTranslation = null;
                } else if(command.matches("Remove")){
                    removeChild();
                }
            }
            @Override
            public void visit(){
                super.visit();
                //Draw desired position
                Scene scene = (Scene) this._graph;
                PGraphics pg = scene.frontBuffer();
                if(desiredTranslation != null){
                    pg.pushStyle();
                    pg.stroke(pg.color(0,255,0));
                    pg.strokeWeight(_radius/2);
                    pg.line(0,0,0, desiredTranslation.x(), desiredTranslation.y(), desiredTranslation.z());
                    pg.popStyle();
                }
            }
        };
    }

    public void findEndEffectors(Frame frame, List<Frame> endEffectors){
        if(frame.children().isEmpty()){
            endEffectors.add(frame);
            return;
        }
        for(Frame child : frame.children()){
            findEndEffectors(child, endEffectors);
        }
    }

    public void addTreeSolver(){
        if(scene.trackedFrame() == null) return;
        scene.registerTreeSolver(scene.trackedFrame());
        //add target
        //get leaf nodes
        ArrayList<Frame> endEffectors = new ArrayList<Frame>();
        findEndEffectors(scene.trackedFrame(), endEffectors);
        PShape redBall =
                scene.is3D() ? createShape(SPHERE, ((Joint) scene.trackedFrame()).radius() * 2f) :
                        createShape(ELLIPSE, 0,0, ((Joint) scene.trackedFrame()).radius() * 2f, ((Joint) scene.trackedFrame()).radius() * 2f);
        redBall.setStroke(false);
        redBall.setFill(color(255, 0, 0));
        for(Frame endEffector : endEffectors) {
            Shape target = new Shape(scene, redBall);
            target.setReference(scene.trackedFrame().reference());
            target.setPosition(endEffector.position());
            scene.addIKTarget(endEffector, target);
        }
    }

    public void addChild(){
        Joint j = createInteractiveJoint();
        j.setReference(scene.trackedFrame());
        j.setTranslation(translateDesired(j));
    }

    public void removeChild(){
        scene.pruneBranch(scene.trackedFrame());
    }


}

