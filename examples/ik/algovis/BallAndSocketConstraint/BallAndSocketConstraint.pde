/*
 * Ball and Socket Explanation
 * by Sebastian Chaparro Cuevas.
 *
 * This example shows an interactive explanation of a Ball and Socket constraint.
 * A Ball and Socket constraint is a 3-DOF rotational constraint (the node could rotate around any direction) that decomposes a rotation
 * into two components called Swing (2-DOF) and Twist (1-DOF) rotations and limits each of them (see nub wiki).
 * 
 * On The left you could visualize a Node with a Hinge constraint.
 * On the right you could modify the Hinge constraint by dragging and moving the constraint limits.   
 */

import nub.core.*;
import nub.core.constraint.BallAndSocket;
import nub.primitives.*;
import nub.processing.Scene;


Scene sceneConstraint, sceneTheta, sceneBase, focus;
int w = 900;
int h = 500;

ThetaControl t_lr, t_ud;
BaseControl base;
Joint j0, j1;
static PFont font;

void settings() {
    size(w, h, P3D);
}

void setup() {
    font = createFont("Zapfino", 38);
    sceneConstraint = new Scene(this, P3D, w/3, h);
    sceneConstraint.setType(Graph.Type.ORTHOGRAPHIC);
    sceneConstraint.fit(1);
    sceneTheta = new Scene(this, P2D, w/3, h, w/3, 0);
    sceneTheta.fit(1);
    sceneBase = new Scene(this, P2D, w/3, h, 2*w/3, 0);
    sceneBase.fit(1);

    //Create a Joint
    j0 = new Joint(sceneConstraint, color(255), 0.1f * sceneConstraint.radius());
    j0.setRoot(true);
    j0.translate(-sceneConstraint.radius() * 0.5f,0,0);
    j1 = new Joint(sceneConstraint, color(255), 0.1f * sceneConstraint.radius());
    j1.setReference(j0);

    Vector v = new Vector(1f,0.3f,0);
    v.normalize();
    v.multiply(sceneConstraint.radius());
    j1.translate(v);

    //Add constraint to joint j0
    BallAndSocket constraint = new BallAndSocket(radians(30), radians(30));
    constraint.setRestRotation(j0.rotation(), new Vector(0,1,0), new Vector(1,0,0), j1.translation());
    j0.setConstraint(constraint);

    //Create controllers
    t_lr = new ThetaControl(sceneTheta, color(255, 154, 31));
    t_lr.translate(-sceneTheta.radius() * 0.3f, -sceneTheta.radius() * 0.7f);
    t_lr.setNames("Right", "Left");
    t_ud = new ThetaControl(sceneTheta, color(31, 132, 255));
    t_ud.translate(-sceneTheta.radius() * 0.3f, sceneTheta.radius() * 0.8f);
    t_ud.setNames("Down", "Up");
    base = new BaseControl(sceneBase, color(100,203,30));

    //Update controllers
    updateControllers(constraint, t_lr, t_ud, base);
}

void draw() {
    handleMouse();
    drawScene(sceneConstraint, "Constraint View");
    drawScene(sceneTheta, "Side / Top View");
    drawScene(sceneBase, "Front View");
    updateCostraint((BallAndSocket) j0.constraint(), t_lr, t_ud, base);
}

void updateCostraint(BallAndSocket constraint, ThetaControl lr, ThetaControl ud, BaseControl b){
    if(lr.modified()){
        constraint.setLeft(lr.maxAngle());
        constraint.setRight(lr.minAngle());
        updateControllers(constraint, lr, ud, b);
        lr.setModified(false);
    } else if(ud.modified()){
        constraint.setUp(ud.maxAngle());
        constraint.setDown(ud.minAngle());
        ud.setModified(false);
        updateControllers(constraint, lr, ud, b);

    } else if(b.modified()){
        constraint.setLeft(b.toAngle(b.left()));
        constraint.setRight(b.toAngle(b.right()));
        constraint.setUp(b.toAngle(b.up()));
        constraint.setDown(b.toAngle(b.down()));
        b.setModified(false);
        updateControllers(constraint, lr, ud, b);
    }
}

void updateControllers(BallAndSocket constraint, ThetaControl lr, ThetaControl ud, BaseControl b){
    lr.update(constraint.right(), constraint.left());
    ud.update(constraint.down(), constraint.up());
    b.update(constraint.left(), constraint.right(), constraint.up(), constraint.down());
}

void drawScene(Scene scene, String title){
    scene.beginDraw();
    scene.context().background(0);
    scene.context().lights();
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
    focus = mouseX < w / 3 ? sceneConstraint : mouseX < 2 * w / 3 ? sceneTheta : sceneBase;
    if(prev != focus && prev != null){
        prev.defaultNode().interact("Clear");
        focus.defaultNode().interact("Clear");
    }

}

void mouseMoved() {
    focus.cast();
}

void mouseDragged() {
    if(focus == sceneTheta || focus == sceneBase) {
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
    if(focus == sceneTheta || focus == sceneBase) {
        focus.defaultNode().interact("Scale");
        return;
    }
}

void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
        if (event.getButton() == LEFT)
            focus.focus();
        else
            focus.align();
}
