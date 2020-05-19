/**
 * Drawing a Ball & Socket constraint.
 * by Sebastian Chaparro Cuevas.
 *
 * This demo shows visually the meaning of each parameter of a Ball & Socket constraint.
 * (See https://github.com/sechaparroc/nub/wiki/4.-Rotational-constraints)
 * 
 * Here three scenes are displayed:
 * ConstraintScene: The leftmost scene shows a simple hierarchy of nodes where the first joint contains the constraint
 * to modify,
 * ThetaScene: The middle scene allows you to change the Up, Down, Left and Right parameters of the constraint. 
 * Base Scene: The rightmost scene allows you to change the shape of the base of the contraint. 
 */


import nub.core.*;
import nub.core.constraint.*;
import nub.primitives.*;
import nub.processing.*;

Scene constraintScene, thetaScene, baseScene, focus;
Node constraintRoot, thetaRoot, baseRoot;

ThetaControl t_lr, t_ud;
BaseControl base;
Joint j0, j1, target;

PFont font;


public void setup() {
    size(800, 600, P3D);
    font = createFont("Zapfino", 38);
    constraintScene = new Scene(this, P3D, width/3, height);
    constraintScene.setType(Graph.Type.ORTHOGRAPHIC);
    constraintScene.fit(1);
    constraintRoot = new Node();
    constraintRoot.enableTagging(false);

    thetaScene = new Scene(this, P2D, width/3, height, width/3, 0);
    thetaScene.fit(1);
    thetaRoot = new Node();
    thetaRoot.enableTagging(false);

    baseScene = new Scene(this, P2D, width/3, height, 2*width/3, 0);
    baseScene.fit(1);
    baseRoot = new Node();
    baseRoot.enableTagging(false);


    //Create a Joint
    j0 = new Joint(constraintScene, color(255), 0.1f * constraintScene.radius());
    j0.setRoot(true);
    j0.setReference(constraintRoot);
    j0.translate(-constraintScene.radius() * 0.5f,0,0);
    j1 = new Joint(constraintScene, color(255), 0.1f * constraintScene.radius());
    j1.setReference(j0);

    Vector v = new Vector(1f,0.f,0);
    v.normalize();
    v.multiply(constraintScene.radius());
    j1.translate(v);
    j1.enableTagging(false);

    //Add constraint to joint j0
    BallAndSocket constraint = new BallAndSocket(radians(60), radians(60));
    constraint.setRestRotation(j0.rotation(), new Vector(0,1,0), new Vector(1,0,0), j1.translation());
    j0.setConstraint(constraint);

    ArrayList<Node> structure = new ArrayList<Node>();
    structure.add(j0);
    structure.add(j1);

    //Create controllers
    t_lr = new ThetaControl(thetaScene, color(255, 154, 31));
    t_lr.setReference(thetaRoot);
    t_lr.translate(-thetaScene.radius() * 0.3f, -thetaScene.radius() * 0.7f, 0);
    t_lr.setNames("Right", "Left");
    t_ud = new ThetaControl(thetaScene, color(31, 132, 255));
    t_ud.setReference(thetaRoot);
    t_ud.translate(-thetaScene.radius() * 0.3f, thetaScene.radius() * 0.8f, 0);
    t_ud.setNames("Down", "Up");
    base = new BaseControl(baseScene, color(100,203,30));
    base.setReference(baseRoot);
    //Update controllers
    updateControllers(constraint, t_lr, t_ud, base);
}

public void draw() {
    handleMouse();
    drawScene(constraintScene, constraintRoot, "Constraint View");
    drawScene(thetaScene, thetaRoot, "Side / Top View");
    drawScene(baseScene, baseRoot, "Front View");
    updateCostraint((BallAndSocket) j0.constraint(), t_lr, t_ud, base);
}

public void updateCostraint(BallAndSocket constraint, ThetaControl lr, ThetaControl ud, BaseControl b){
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

public void updateControllers(BallAndSocket constraint, ThetaControl lr, ThetaControl ud, BaseControl b){
    lr.update(constraint.right(), constraint.left());
    ud.update(constraint.down(), constraint.up());
    b.update(constraint.left(), constraint.right(), constraint.up(), constraint.down());
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
    focus = mouseX < width / 3 ? constraintScene : mouseX < 2 * width / 3 ? thetaScene : baseScene;
    if(prev != focus && prev != null && prev.node() != null){
        prev.node().interact("Clear");
        if(focus != null && focus.node() != null)focus.node().interact("Clear");
    }

}

public void mouseMoved() {
    focus.mouseTag();
}

public void mouseDragged() {
    if(focus == thetaScene || focus == baseScene) {
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
    if(focus == thetaScene || focus == baseScene) {
        if(focus.node() != null )focus.node().interact("Scale");
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
