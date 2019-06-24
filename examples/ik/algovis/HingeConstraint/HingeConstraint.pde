/*
 * Hinge Explanation
 * by Sebastian Chaparro Cuevas.
 *
 * This example shows an interactive explanation of a Hinge constraint.
 * A Hinge constraint is a 1-DOF rotational constraint. i.e the node will rotate only around a single direction.
 * Furthermore, the rotation made by a node is enclosed on a minimum and maximum angle.
 * 
 * On The left you could visualize a Node with a Hinge constraint.
 * On the right you could modify the Hinge constraint by dragging and moving the constraint limits.   
 */

import nub.core.*;
import nub.core.constraint.Hinge;
import nub.primitives.*;
import nub.processing.Scene;



Scene sceneConstraint, sceneTheta, focus;
int w = 900;
int h = 500;
ThetaControl control;
Joint j0, j1;
PFont font;

void settings() {
    size(w, h, P3D);
}

void setup() {
    font = createFont("Zapfino", 38);
    sceneConstraint = new Scene(this, P3D, w/2, h);
    sceneConstraint.setType(Graph.Type.ORTHOGRAPHIC);
    sceneConstraint.fit(1);
    sceneTheta = new Scene(this, P2D, w/2, h, w/2, 0);
    sceneTheta.fit(1);
    //Create a Joint
    j0 = new Joint(sceneConstraint, color(255), 0.1f * sceneConstraint.radius());
    j0.setRoot(true);
    j0.translate(-sceneConstraint.radius() * 0.5f,0,0);
    j1 = new Joint(sceneConstraint, color(255), 0.1f * sceneConstraint.radius());
    j1.setReference(j0);

    Vector v = new Vector(1f,0,0);
    v.normalize();
    v.multiply(sceneConstraint.radius());
    j1.translate(v);

    //Add constraint to joint j0
    Hinge constraint = new Hinge(radians(30), radians(30), j0.rotation(), new Vector(1,0,0), new Vector(0,0,1));
    j0.setConstraint(constraint);

    //Create controllers
    control = new ThetaControl(sceneTheta, color(100,203,30));
    control.setNames("Min", "Max");

    //Update controllers
    updateControllers(constraint, control);
}

void draw() {
    handleMouse();
    drawScene(sceneConstraint, "Constraint View");
    drawScene(sceneTheta, "Hinge Control");
    updateCostraint((Hinge) j0.constraint(), control);
}

void updateCostraint(Hinge constraint, ThetaControl control){
    if(control.modified()){
        constraint.setMaxAngle(control.maxAngle());
        constraint.setMinAngle(control.minAngle());
        updateControllers(constraint, control);
        control.setModified(false);
    }
}

void updateControllers(Hinge constraint, ThetaControl control){
    control.update(constraint.minAngle(), constraint.maxAngle());
}

void drawScene(Scene scene, String title){
    scene.beginDraw();
    scene.context().background(0);
    scene.context().lights();
    //scene.drawAxes();
    scene.render();
    scene.beginHUD();
    scene.context().noLights();
    scene.context().pushStyle();
    scene.context().fill(255);
    scene.context().stroke(255);
    scene.context().textAlign(CENTER, CENTER);
    scene.context().textFont(font, 24);
    scene.context().text(title, scene.context().width / 2, 20);
    scene.context().noFill();
    scene.context().strokeWeight(3);
    scene.context().rect(0,0,sceneConstraint.context().width, sceneConstraint.context().height);
    scene.context().popStyle();
    scene.endHUD();
    scene.endDraw();
    scene.display();
}



void handleMouse() {
    Scene prev = focus;
    focus = mouseX < w / 2 ? sceneConstraint : sceneTheta;
    if(prev != focus && prev != null){
        prev.defaultNode().interact("Clear");
        focus.defaultNode().interact("Clear");
    }

}

void mouseMoved() {
    focus.cast();
}

void mouseDragged() {
    if(focus == sceneTheta) {
        focus.defaultNode().interact("OnScaling", new Vector(focus.mouse().x(), focus.mouse().y()));
        return;
    }
    if (mouseButton == LEFT)
        focus.spin();
    else if (mouseButton == RIGHT){
        focus.translate();
    }
    else
        focus.moveForward(mouseX - pmouseX);
}

void mouseReleased(){
    if(focus == sceneTheta) {
        focus.defaultNode().interact("Scale");
        return;
    }
}

void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
    //focus.zoom(event.getCount() * 50);
}

void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
        if (event.getButton() == LEFT)
            focus.focus();
        else
            focus.align();
}
