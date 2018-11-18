package ik.interactive;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.MatrixHandler;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.Constraint;
import frames.core.constraint.FixedConstraint;
import frames.core.constraint.Hinge;
import frames.primitives.Matrix;
import frames.primitives.Point;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PConstants;
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
    //focus;
    //OptionPanel panel;
    //PGraphics canvas1;
    MultipleViews views;

    float radius = 15;
    int w = 1000, h = 700;
    /*Create different skeletons to interact with*/
    //Choose FX2D, JAVA2D, P2D or P3D
    String renderer = P3D;

    /*Constraint Parameters*/
    float minAngle = radians(60);
    float maxAngle = radians(60);

    List<Target> targets = new ArrayList<Target>();


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactive.SkeletonBuilder"});
    }

    public void settings() {
        size(w, h, renderer);
    }

    public void setup(){
        //canvas1 = createGraphics((int)(0.7f*w), h, renderer);
        //canvas1 = createGraphics(w, h, renderer);
        //canvas1 = this.g;
        scene = new Scene(this);
        scene.setRadius(300);
        if(scene.is3D())scene.setType(Graph.Type.ORTHOGRAPHIC);
        new InteractiveJoint(scene, radius).setRoot(true);
        // = new OptionPanel(this, 0.7f * width, 0, (int)(0.3f * width), h );
        //scene.fitBallInterpolation();
        //create an auxiliary view per Orthogonal Plane
        views = new MultipleViews(scene);
        //create an auxiliary view to look at the XY Plane
        Constraint constraint = new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Frame frame) {
                return translation;
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
                return new Quaternion();
            }
        };

        Frame eyeXY = new Frame();
        eyeXY.setMagnitude(0.5f);
        eyeXY.setPosition(scene.eye().position());
        eyeXY.setOrientation(scene.eye().orientation());
        AuxiliaryView  viewXY = new AuxiliaryView(scene, eyeXY, 0, 2*h/3, w/3, h/3);
        views.addAuxiliaryView(viewXY);
        viewXY.setBackground(color(255));
        eyeXY.setConstraint(constraint);

        //create an auxiliary view to look at the XY Plane
        Frame eyeXZ = new Frame();
        eyeXZ.setMagnitude(0.5f);
        eyeXZ.setPosition(0, scene.radius(), 0);
        eyeXZ.setOrientation(new Quaternion(new Vector(1,0,0), -HALF_PI));
        AuxiliaryView  viewXZ = new AuxiliaryView(scene, eyeXZ, w/3, 2*h/3, w/3, h/3);
        views.addAuxiliaryView(viewXZ);
        viewXZ.setBackground(color(100));
        eyeXZ.setConstraint(constraint);
        //create an auxiliary view to look at the XY Plane
        Frame eyeYZ = new Frame();
        eyeYZ.setMagnitude(0.5f);
        eyeYZ.setPosition(scene.radius(), 0, 0);
        eyeYZ.setOrientation(new Quaternion(new Vector(0,1,0), HALF_PI));
        AuxiliaryView  viewYZ = new AuxiliaryView(scene, eyeYZ, 2*w/3, 2*h/3, w/3, h/3);
        views.addAuxiliaryView(viewYZ);
        viewYZ.setBackground(color(50));
        eyeYZ.setConstraint(constraint);

    }

    void handleMouse() {
        //focus = scene;
        //focus = mouseX > 0.7f * w ? panel._scene : scene ;
    }

    public void draw() {
        //handleMouse();
        //scene.beginDraw();
        //canvas1.background(0);
        InteractiveJoint.setPGraphics(scene.frontBuffer());
        MatrixHandler matrixHandler = scene.matrixHandler(scene.frontBuffer());
        matrixHandler._bindProjection(views._defaultView.eye().projection(views._defaultView._type, views._defaultView._width, views._defaultView._height, views._defaultView.zNear(), views._defaultView.zFar(), scene.isLeftHanded()));
        matrixHandler._bindModelView(views._defaultView.eye().view());


        background(0);
        //canvas1.stroke(255,0,0);
        stroke(255,0,0);
        scene.drawAxes();
        scene.traverse(scene.frontBuffer(), views._defaultView._type, views._defaultView.eye(), views._defaultView.zNear(), views._defaultView.zFar());
        for(Target target : targets){
            scene.drawPath(target._interpolator, 5);
        }
        //for(AuxiliaryView view : views) {
            //Scene.drawEye(scene.frontBuffer(), view._pGraphics, view.type(), view.eye(), view.zNear(), view.zFar());
        //}
        //scene.endDraw();
        //scene.display();

        /*
        panel._scene.beginDraw();
        panel._scene.frontBuffer().background(0);
        if(panel._frame != null)
            panel._scene.traverse();
        panel._scene.endDraw();
        panel._scene.display();
        */
        /*
        scene.beginHUD();
        for(AuxiliaryView view : views) {
            InteractiveJoint.setPGraphics(scene.frontBuffer());
            image(view._pGraphics,0,0, width, height);
        }
        scene.endHUD();
        */
        //setBackBuffer(mouseX, mouseY);

        views.draw();

        /*InteractiveJoint.setPGraphics(scene.frontBuffer());
        background(0);

        scene.traverse();
        scene.beginHUD();

        scene.pApplet().pushStyle();
        scene.pApplet().fill(255,0,0);
        Point point = views.cursorLocation(scene.pApplet().mouseX, scene.pApplet().mouseY);
        scene.pApplet().ellipse(point.x(), point.y(), 10, 10);
        scene.pApplet().popStyle();
        scene.endHUD();

        scene.beginHUD();
        scene.pApplet().image(views._currentView._pGraphics, views._currentView._x, views._currentView._y, views._currentView._width, views._currentView._height);
        scene.endHUD();*/

    }

    //mouse events
    @Override
    public void mouseMoved() {
        if(!mousePressed) {
            scene.track(views.cursorLocation(mouseX, mouseY));
        }
    }

    public void mouseDragged(MouseEvent event) {
        Point previous = views.cursorLocation(pmouseX, pmouseY);
        Point point = views.cursorLocation(mouseX, mouseY);
        if (mouseButton == RIGHT && event.isControlDown()) {
            Vector mouse = new Vector(point.x(), point.y());
            if(scene.trackedFrame() != null)
                scene.trackedFrame().interact("OnAdding", mouse);
        } else if (mouseButton == LEFT) {
            scene.spin(previous, point);
        } else if (mouseButton == RIGHT) {
            scene.translate(point.x() - previous.x(), point.y() - previous.y());
            Target.multipleTranslate();
        } else if (mouseButton == CENTER){
            scene.scale(scene.mouseDX());
        } else if(scene.trackedFrame() != null)
            scene.trackedFrame().interact("Reset");
        //PANEL
        //else {
            //panel._scene.defaultFrame().interact();
        //}
        //if(focus == scene)panel.updateFrameOptions();
        //if(focus == scene && !Target.selectedTargets().contains(focus.trackedFrame())){
        //    Target.clearSelectedTargets();
        //}
        if(!Target.selectedTargets().contains(scene.trackedFrame())){
            Target.clearSelectedTargets();
        }
    }

    public void mouseReleased(){
        Point previous = views.cursorLocation(pmouseX, pmouseY);
        Point point = views.cursorLocation(mouseX, mouseY);
        Vector mouse = new Vector(point.x(), point.y());
        //mouse = scene.location(mouse);
        //mouse = Vector.projectVectorOnPlane(mouse, scene.viewDirection());
        //mouse.add(scene.defaultFrame().position());
        if(scene.trackedFrame() != null)
            scene.trackedFrame().interact("Add", mouse, false);
    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getButton() == LEFT) {
            if (event.getCount() == 1) {
                //panel.setFrame(scene.trackedFrame());
                if(event.isControlDown()){
                    if(scene.trackedFrame() != null)
                        scene.trackedFrame().interact("KeepSelected");
                }
            }
            else if (event.getCount() == 2) {
                if (event.isShiftDown())
                    if(scene.trackedFrame() != null)
                        scene.trackedFrame().interact("Remove");
                else
                    scene.focus();
            }
            else {
                scene.align();
            }
        }
    }

    public void keyPressed(){
        if(key == '+'){
            new InteractiveJoint(scene, radius).setRoot(true);
        }
        if(key == 'A' || key == 'a'){
            addTreeSolver();
        }
        if(key == 'C' || key == 'c'){
            addConstraint(scene.trackedFrame());
        }
        if(key == 'S' || key == 's'){
            minAngle += radians(5);
            if(minAngle >= radians(170) ) minAngle = radians(170);
            System.out.println("minAngle : " + degrees(minAngle));
        }
        if(key == 'D' || key == 'd'){
            minAngle -= radians(5);
            if(minAngle <= radians(0) ) minAngle = radians(0);
            System.out.println("minAngle : " + degrees(minAngle));
        }
        if(key == 'F' || key == 'f'){
            maxAngle += radians(5);
            if(maxAngle >= radians(170) ) maxAngle = radians(170);
            System.out.println("maxAngle : " + degrees(maxAngle));
        }
        if(key == 'G' || key == 'g'){
            maxAngle -= radians(5);
            if(maxAngle <= radians(0) ) maxAngle = radians(0);
            System.out.println("maxAngle : " + degrees(maxAngle));
        }
        if(key==' '){
            for(Target target : targets){
                target._interpolator.start();
            }
        }

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

    public void addConstraint(Frame frame){
        //If has a child
        if(frame == null) return;
        if(frame.children().size() != 1) return;
        if(scene.is3D()) {
            BallAndSocket constraint = new BallAndSocket(minAngle, minAngle, maxAngle, maxAngle);
            Vector twist = frame.children().get(0).translation().get();
            constraint.setRestRotation(frame.rotation().get(), Vector.orthogonalVector(twist), twist);
            frame.setConstraint(constraint);
        } else{
            Hinge constraint = new Hinge(true, minAngle, maxAngle);
            constraint.setRestRotation(frame.rotation().get());
            frame.setConstraint(constraint);
        }

    }

    public void addTreeSolver(){
        if(scene.trackedFrame() == null) return;
        scene.registerTreeSolver(scene.trackedFrame());
        //add target
        //get leaf nodes
        ArrayList<Frame> endEffectors = new ArrayList<Frame>();
        findEndEffectors(scene.trackedFrame(), endEffectors);
        for(Frame endEffector : endEffectors) {
            Target target = new Target(scene, endEffector);
            scene.addIKTarget(endEffector, target);
            targets.add(target);
        }
    }
}

