package ik.interactive;

import nub.core.Node;
import nub.core.Graph;
import nub.core.MatrixHandler;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Constraint;
import nub.core.constraint.FixedConstraint;
import nub.core.constraint.Hinge;
import nub.ik.Solver;
import nub.ik.TreeSolver;
import nub.primitives.Matrix;
import nub.primitives.Point;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import ik.common.Joint;
import net.java.games.input.Mouse;
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
    //TODO Check Fitting curve method for target path
    Scene scene, focus;
    Scene[] views;
    boolean debug = false;
    boolean showPath = false, showLast = false, showGrid = false;
    //focus;
    //OptionPanel panel;
    //PGraphics canvas1;

    FitCurve fitCurve;

    float radius = 30;
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
        focus = scene;
        if(scene.is3D())scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(800);
        scene.fit();

        new InteractiveJoint(scene, radius).setRoot(true);
        // = new OptionPanel(this, 0.7f * width, 0, (int)(0.3f * width), h );
        //scene.fit(1);
        views = createViews();
    }

    public void draw() {
        //setFocus();
        //handleMouse();
        //scene.beginDraw();
        //canvas1.background(0);
        background(0);
        ambientLight(102, 102, 102);
        lightSpecular(204, 204, 204);
        directionalLight(102, 102, 102, 0, 0, -1);
        specular(255, 255, 255);
        shininess(10);
        //canvas1.stroke(255,0,0);
        stroke(255);
        if(showGrid) scene.drawGrid();
        stroke(255,0,0);
        //scene.drawAxes();
        scene.render();
        for(Target target : targets){
            if(showPath)scene.drawPath(target._interpolator, 5);
            if(showLast) {
                pushStyle();
                colorMode(HSB);
                strokeWeight(radius / 3.f);
                Vector p = !target.last().isEmpty() ? target.last().get(0) : null;
                for (int i = 0; i < target.last().size(); i++) {
                    Vector v = target.last().get(i);
                    fill((frameCount + i) % 255, 255, 255, 100);
                    stroke((frameCount + i) % 255, 255, 255, 100);
                    line(v.x(), v.y(), v.z(), p.x(), p.y(), p.z());
                    p = v;
                }
                popStyle();
            }
        }
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
        if(debug) {
            pushStyle();
            for (ArrayList<Vector> list : solver.aux_p) {
                Vector prev = null;
                for (Vector v : list) {
                    pushMatrix();
                    strokeWeight(radius / 4);
                    stroke(0, 0, 255);
                    if (prev != null) line(v.x(), v.y(), v.z(), prev.x(), prev.y(), prev.z());
                    translate(v.x(), v.y(), v.z());
                    noStroke();
                    fill(0, 0, 255);
                    sphere(radius / 2);
                    popMatrix();
                    prev = v;
                }
            }

            for (ArrayList<Vector> list : solver.aux_prev) {
                Vector prev = null;
                for (Vector v : list) {
                    pushMatrix();
                    strokeWeight(radius / 4);
                    stroke(0, 255, 0);
                    if (prev != null) line(v.x(), v.y(), v.z(), prev.x(), prev.y(), prev.z());
                    translate(v.x(), v.y(), v.z());
                    noStroke();
                    fill(0, 255, 0);
                    sphere(radius / 2);
                    popMatrix();
                    prev = v;
                }
            }
            popStyle();
        }

        if(fitCurve != null)
            if(fitCurve._interpolator != null)
                scene.drawPath(fitCurve._interpolator, 5);

        scene.beginHUD();
        if(fitCurve != null) fitCurve.drawCurves(scene.context());
        scene.endHUD();
        /*for(int i = 0; i < views.length; i++) {
            scene.shift(views[i]);
            Joint.setPGraphics(views[i].frontBuffer());
            scene.beginHUD();
            views[i].beginDraw();
            views[i].frontBuffer().background(100);
            views[i].drawAxes();
            views[i].render();
            views[i].endDraw();
            views[i].display();
            if(fitCurve != null) fitCurve.drawCurves(scene.frontBuffer());
            scene.endHUD();
            views[i].shift(scene);
        }*/
        if(solve && debug){
            solver.solve();
        }
    }

    public void setFocus(){
        if(mouseY <= 2*h/3){
            focus = scene;
        } else if(mouseX <= w/3){
            focus =  views[0];
        }
        else if(mouseX <= 2*w/3){
            focus =  views[1];
        }
        else{
            focus = views[2];
        }
    }

    //mouse events
    @Override
    public void mouseMoved() {
        if(!mousePressed) {
            focus.cast();
        }
    }

    public void mouseDragged(MouseEvent event) {
        if (mouseButton == RIGHT && event.isControlDown()) {
            Vector vector = new Vector(focus.mouse().x(), focus.mouse().y());
            if(focus.trackedNode() != null)
                if(focus.trackedNode() instanceof  InteractiveJoint)
                    focus.trackedNode().interact("OnAdding", focus, vector);
                else
                    focus.trackedNode().interact("OnAdding", vector);
        } else if (mouseButton == LEFT) {
            if(event.isControlDown() && fitCurve != null ){
                if(fitCurve.started()) {
                    fitCurve.add(mouseX, mouseY);
                    fitCurve.fitCurve();
                }
            } else {
                focus.spin(focus.pmouse(), focus.mouse());
            }
        } else if (mouseButton == RIGHT) {
            focus.translate(focus.mouse().x() - focus.pmouse().x(), focus.mouse().y() - focus.pmouse().y());
            Target.multipleTranslate();
        } else if (mouseButton == CENTER){
            focus.scale(focus.mouseDX());
        } else if(focus.trackedNode() != null)
            focus.trackedNode().interact("Reset");
        //PANEL
        //else {
            //panel._scene.defaultFrame().interact();
        //}
        //if(focus == scene)panel.updateFrameOptions();
        //if(focus == scene && !Target.selectedTargets().contains(focus.trackedFrame())){
        //    Target.clearSelectedTargets();
        //}
        if(!Target.selectedTargets().contains(focus.trackedNode())){
            Target.clearSelectedTargets();
        }
    }

    public void mousePressed(MouseEvent event){
        if(event.isControlDown() && event.getButton() == LEFT){
            //Reset Curve
            fitCurve = new FitCurve();
            fitCurve.setStarted(true);
        }
    }

    public void mouseReleased(MouseEvent event){
        /*if(event.isControlDown() && event.getButton() == LEFT){
            //Reset Curve
            if(fitCurve != null){
                fitCurve.setStarted(false);
                fitCurve.printCurves();
                fitCurve.getCatmullRomCurve(scene, 0);
                fitCurve._interpolator.start();
                scene.drawPath(fitCurve._interpolator, 5);

            }
        }*/

        //mouse = scene.location(mouse);
        //mouse = Vector.projectVectorOnPlane(mouse, scene.viewDirection());
        //mouse.add(scene.defaultFrame().position());
        Vector vector = new Vector(focus.mouse().x(), focus.mouse().y());
        if(focus.trackedNode() != null)
            if(focus.trackedNode() instanceof  InteractiveJoint)
                focus.trackedNode().interact("Add", scene, focus, vector);
            //else focus.trackedFrame().interact("Add", vector, false);
            else{
                if(fitCurve != null){
                    fitCurve.setStarted(false);
                    fitCurve.getCatmullRomCurve(scene, 0);
                    fitCurve._interpolator.start();
                    focus.trackedNode().interact("AddCurve", fitCurve);
                }
            }
        fitCurve = null;

    }

    public void mouseWheel(MouseEvent event) {
        focus.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getButton() == LEFT) {
            if (event.getCount() == 1) {
                //panel.setFrame(scene.trackedFrame());
                if(event.isControlDown()){
                    if(focus.trackedNode() != null)
                        focus.trackedNode().interact("KeepSelected");
                }
            }
            else if (event.getCount() == 2) {
                if (event.isShiftDown())
                    if(scene.trackedNode() != null)
                        scene.trackedNode().interact("Remove");
                else
                    focus.focus();
            }
            else {
                focus.align();
            }
        }
    }

    boolean solve = false, keepDirections = true, fixTwisting = true;
    public void keyPressed(){
        if(key == '+'){
            new InteractiveJoint(scene, radius).setRoot(true);
        }
        if(key == 'A' || key == 'a'){
            addTreeSolver();
        }
        if(key == 'C' || key == 'c'){
            addConstraint(focus.trackedNode(), false);
        }
        if(key == 'H' || key == 'h'){
            addConstraint(focus.trackedNode(), true);
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
        if(key == 'i' || key == 'I'){
            printTree(scene.trackedNode(), "");
        }

        if(key==' '){
            for(Target target : targets){
                target._interpolator.start();
            }
        }
        if(key == '1'){
            if(debug) solver.solve();
        }
        if(key == '2'){
            if(debug) solve = !solve;
        }
        if(key == '3'){
            showLast = !showLast;
        }
        if(key == '4'){
            showPath = !showPath;
        }
        if(key == '5'){
            showGrid = !showGrid;
        }
        if(key == '6'){
            Joint.axes = !Joint.axes;
        }

        if(key == 'r' || key == 'R'){
            for(Target target : targets){
                target._interpolator.setLoop(!target._interpolator.loop());
            }
        }

        if(key == 'o' || key == 'O'){
            keepDirections = !keepDirections;
            for(TreeSolver solver : scene.treeSolvers()){
                solver.setKeepDirection(keepDirections);
            }
            System.out.println("Keep directions : " + (keepDirections ? "Enabled" : "Disabled"));
        }

        if(key == 'p' || key == 'P'){
            fixTwisting = !fixTwisting;
            for(TreeSolver solver : scene.treeSolvers()){
                solver.setFixTwisting(fixTwisting);
            }
        }
    }

    public void findEndEffectors(Node frame, List<Node> endEffectors){
        if(frame.children().isEmpty()){
            endEffectors.add(frame);
            return;
        }
        for(Node child : frame.children()){
            findEndEffectors(child, endEffectors);
        }
    }

    public void addConstraint(Node frame, boolean hinge){
        //If has a child
        hinge = hinge || scene.is2D();
        if(frame == null) return;
        if(frame.children().size() != 1) return;
        if(!hinge) {
            BallAndSocket constraint = new BallAndSocket(minAngle, minAngle, maxAngle, maxAngle);
            Vector twist = frame.children().get(0).translation().get();
            constraint.setRestRotation(frame.rotation().get(), Vector.orthogonalVector(twist), twist);
            frame.setConstraint(constraint);
        } else{
            Vector twist = new Vector(0,0,1);
            Vector up = new Vector(0,1,0);
            if(frame.children() != null){
                up = frame.children().get(0).translation().get();
                twist = Vector.orthogonalVector(up);
            }
            Hinge constraint = new Hinge(minAngle, maxAngle, frame.rotation().get(), up, twist);
            frame.setConstraint(constraint);
        }

    }

    TreeSolver solver;
    public void addTreeSolver(){
        if(scene.trackedNode() == null) return;
        if(debug) {
            solver = new TreeSolver(scene.trackedNode());
            solver.timesPerFrame = 1;
        } else {
            if(scene.trackedNode() != null) {
                solver = scene.registerTreeSolver(scene.trackedNode());
                solver.timesPerFrame = 1; //TODO : Allow more times
            }
        }
        solver.maxIter = 50;
        solver.setFixTwisting(fixTwisting);
        solver.setKeepDirection(keepDirections);

        //add target
        //get leaf nodes
        ArrayList<Node> endEffectors = new ArrayList<Node>();
        findEndEffectors(focus.trackedNode(), endEffectors);
        for(Node endEffector : endEffectors) {
            endEffector.setPickingThreshold(0.00001f);
            Target target = new Target(scene, ((Joint) scene.trackedNode()).radius(), endEffector);
            target.setReference(((Joint) scene.trackedNode()).reference());
            //scene.addIKTarget(endEffector, target);
            solver.addTarget(endEffector, target);
            targets.add(target);
        }
    }

    public void printTree(Node root, String sep){
        if(root == null) return;
        System.out.print(sep + "|-> Node ");
        System.out.println("translation: " + root.translation() + "rotation axis: " + root.rotation().axis() + "rotation angle : " + root.rotation().angle());
        for(Node child : root.children()){
            printTree(child, sep + "\t");
        }
    }

    public Scene[] createViews(){
        //create an auxiliary view per Orthogonal Plane
        //Disable rotation
        Constraint constraint = new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node frame) {
                return translation;
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node frame) {
                return new Quaternion();
            }
        };

        Scene[] views = new Scene[3];
        Node eyeXY = new Node();
        eyeXY.scale(2f);
        eyeXY.setPosition(scene.eye().position());
        eyeXY.setOrientation(scene.eye().orientation());
        eyeXY.setConstraint(constraint);
        views[0] = new Scene(this, P3D, w/3, h/3, 0, 2*h/3);
        views[0].setRadius(scene.radius()*4);
        views[0].setEye(eyeXY);
        views[0].setType(Graph.Type.ORTHOGRAPHIC);
        //create an auxiliary view to look at the XY Plane
        Node eyeXZ = new Node();
        eyeXZ.scale(2f);
        eyeXZ.setPosition(0, scene.radius(), 0);
        eyeXZ.setOrientation(new Quaternion(new Vector(1,0,0), -HALF_PI));
        eyeXZ.setConstraint(constraint);
        views[1] = new Scene(this, P3D, w/3, h/3, w/3, 2*h/3);
        views[1].setRadius(scene.radius()*4);
        views[1].setEye(eyeXZ);
        views[1].setType(Graph.Type.ORTHOGRAPHIC);
        //create an auxiliary view to look at the XY Plane
        Node eyeYZ = new Node();
        //eyeYZ.setMagnitude(0.5f);
        eyeYZ.scale(2f);
        eyeYZ.setPosition(scene.radius(), 0, 0);
        eyeYZ.setOrientation(new Quaternion(new Vector(0,1,0), HALF_PI));
        eyeYZ.setConstraint(constraint);
        views[2] = new Scene(this, P3D, w/3, h/3, 2*w/3, 2*h/3);
        views[2].setEye(eyeYZ);
        views[2].setRadius(scene.radius()*4);
        views[2].setType(Graph.Type.ORTHOGRAPHIC);
        return views;
    }
}

