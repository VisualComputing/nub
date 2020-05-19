/**
 * Drawing a Hinge constraint.
 * by Sebastian Chaparro Cuevas.
 *
 * This demo shows visually the meaning of each parameter of a Hinge constraint.
 * (See https://github.com/sechaparroc/nub/wiki/4.-Rotational-constraints)
 * 
 * Here two scenes are displayed:
 * ConstraintScene: The leftmost scene shows a simple hierarchy of nodes where the first joint contains the constraint
 * to modify,
 * ThetaScene: The rightmost scene allows you to change the. Min and Max. parameters of the constraint. 
 */

import nub.core.*;
import nub.core.constraint.*;
import nub.primitives.*;
import nub.processing.*;

Scene constraintScene, thetaScene, focus;
Node constraintRoot, thetaRoot;


ThetaControl control;
Joint j0, j1;
PFont font;

public void setup() {
    size(800, 600, P3D);
    font = createFont("Zapfino", 38);
    constraintScene = new Scene(this, P3D, width/2, height);
    constraintScene.setType(Graph.Type.ORTHOGRAPHIC);
    constraintScene.fit(1);
    constraintRoot = new Node();
    constraintRoot.enableTagging(false);

    thetaScene = new Scene(this, P2D, width/2, height, width/2, 0);
    thetaScene.fit(1);
    thetaRoot = new Node();
    thetaRoot.enableTagging(false);

    //Create a Joint
    j0 = new Joint(constraintScene, color(255), 0.1f * constraintScene.radius());
    j0.setReference(constraintRoot);
    j0.setRoot(true);
    j0.translate(-constraintScene.radius() * 0.1f,0,0);
    j1 = new Joint(constraintScene, color(255), 0.1f * constraintScene.radius());
    j1.setReference(j0);

    Vector v = new Vector(1f,0,0);
    v.normalize();
    v.multiply(constraintScene.radius());
    j1.translate(v);

    //Add constraint to joint j0
    Hinge constraint = new Hinge(radians(30), radians(30), j0.rotation(), new Vector(1,0,0), new Vector(0,0,1));
    j0.setConstraint(constraint);

    //Create controllers
    control = new ThetaControl(thetaScene, color(100,203,30));
    control.setReference(thetaRoot);
    control.setNames("Min", "Max");

    //Update controllers
    updateControllers(constraint, control);
}

public void draw() {
    handleMouse();
    drawScene(constraintScene, constraintRoot, "Constraint View");
    drawScene(thetaScene, thetaRoot, "Hinge Control");
    thetaScene.drawBullsEye(control);
    updateCostraint((Hinge) j0.constraint(), control);
}

public void updateCostraint(Hinge constraint, ThetaControl control){
    if(control.modified()){
        constraint.setMaxAngle(control.maxAngle());
        constraint.setMinAngle(control.minAngle());
        updateControllers(constraint, control);
        control.setModified(false);
    }
}

public void updateControllers(Hinge constraint, ThetaControl control){
    control.update(constraint.minAngle(), constraint.maxAngle());
}

public void drawScene(Scene scene, Node root, String title){
    scene.beginDraw();
    scene.context().background(0);
    scene.context().lights();
    scene.render(root);
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
    scene.context().rect(0,0, constraintScene.context().width, constraintScene.context().height);
    scene.context().popStyle();
    scene.endHUD();
    scene.endDraw();
    scene.display();
}




public void handleMouse() {
    Scene prev = focus;
    focus = mouseX < width / 2 ? constraintScene : thetaScene;
    if(prev != focus && prev != null){
        if(prev.node() != null) prev.node().interact("Clear");
        if(focus != null && focus.node() != null) focus.node().interact("Clear");
    }

}

public void mouseMoved() {
    focus.mouseTag();
}

public void mouseDragged() {
    if(focus == thetaScene) {
        if(focus.node() != null) focus.node().interact("OnScaling", new Vector(focus.mouseX(), focus.mouseY()));
        return;
    }
    if (mouseButton == LEFT)
        focus.mouseSpin();
    else if (mouseButton == RIGHT){
        focus.mouseTranslate();
    }
    else
        focus.moveForward(mouseX - pmouseX);
}

public void mouseReleased(){
    if(focus == thetaScene) {
        if(focus.node() != null) focus.node().interact("Scale");
        return;
    }
}

public void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
    //focus.zoom(event.getCount() * 50);
}

public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
        if (event.getButton() == LEFT)
            focus.focus();
        else
            focus.align();
}
