package intellij;

import nub.core.Node;
import nub.core.constraint.AxisPlaneConstraint;
import nub.core.constraint.EyeConstraint;
import nub.core.constraint.LocalConstraint;
import nub.core.constraint.WorldConstraint;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.MouseEvent;

public class ConstrainedNode extends PApplet {
  Scene scene;
  PFont font;
  int transDir;
  int rotDir;
  Node node;
  AxisPlaneConstraint constraints[] = new AxisPlaneConstraint[3];
  int activeConstraint;

  //Choose P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    //size(700, 700, renderer);
    font = loadFont("FreeSans-16.vlw");
    textFont(font);
    scene = new Scene(this);
    constraints[0] = new LocalConstraint();
    // Note that an EyeConstraint(eye) would produce the same results:
    // An EyeConstraint is a LocalConstraint when applied to the eye
    constraints[1] = new WorldConstraint();
    constraints[2] = new EyeConstraint(scene);
    transDir = 0;
    rotDir = 0;
    activeConstraint = 0;
    node = new Node();
    node.enableHint(Node.TORUS | Node.BULLSEYE | Node.AXES);
    scene.randomize(node);
    node.translate(new Vector(20, 20, 0));
    node.setConstraint(constraints[activeConstraint]);
    scene.enableHint(Scene.GRID | Scene.AXES | Scene.BACKGROUND);
    scene.configHint(Scene.GRID, Scene.GridType.LINES, color(0, 255, 0));
  }

  public void draw() {
    scene.render();
    fill(0, 255, 255);
    scene.beginHUD();
    displayText();
    scene.endHUD();
  }

  public void mouseMoved() {
    if (!scene.isTagValid("key"))
      scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      if (scene.node("key") != null)
        scene.mouseSpin("key");
      else
        scene.mouseSpin();
    } else if (mouseButton == RIGHT) {
      if (scene.node("key") != null)
        scene.mouseShift("key");
      else
        scene.mouseShift();
    } else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.zoom(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'i')
      if (scene.hasTag("key", node)) {
        scene.removeTag("key");
      } else {
        scene.tag("key", node);
      }
    if (key == 'b' || key == 'B') {
      rotDir = (rotDir + 1) % 3;
    }
    if (key == 'd' || key == 'D') {
      transDir = (transDir + 1) % 3;
    }
    if (key == 'u' || key == 'U') {
      changeConstraint();
    }
    if (key == 't' || key == 'T') {
      constraints[activeConstraint]
              .setTranslationConstraintType(nextTranslationConstraintType(constraints[activeConstraint]
                      .translationConstraintType()));
    }
    if (key == 'r' || key == 'R') {
      constraints[activeConstraint]
              .setRotationConstraintType(nextRotationConstraintType(constraints[activeConstraint]
                      .rotationConstraintType()));
    }
    constraints[activeConstraint].setTranslationConstraintDirection(
            transDir == 0 ? Vector.plusI : transDir == 1 ? Vector.plusJ : Vector.plusK
    );
    constraints[activeConstraint].setRotationConstraintDirection(
            rotDir == 0 ? Vector.plusI : rotDir == 1 ? Vector.plusJ : Vector.plusK
    );
  }

  AxisPlaneConstraint.Type nextTranslationConstraintType(AxisPlaneConstraint.Type transType) {
    AxisPlaneConstraint.Type type;
    switch (transType) {
      case FREE:
        type = AxisPlaneConstraint.Type.PLANE;
        break;
      case PLANE:
        type = AxisPlaneConstraint.Type.AXIS;
        break;
      case AXIS:
        type = AxisPlaneConstraint.Type.FORBIDDEN;
        break;
      case FORBIDDEN:
        type = AxisPlaneConstraint.Type.FREE;
        break;
      default:
        type = AxisPlaneConstraint.Type.FREE;
    }
    return type;
  }

  AxisPlaneConstraint.Type nextRotationConstraintType(AxisPlaneConstraint.Type rotType) {
    AxisPlaneConstraint.Type type;
    switch (rotType) {
      case FREE:
        type = AxisPlaneConstraint.Type.AXIS;
        break;
      case AXIS:
        type = AxisPlaneConstraint.Type.FORBIDDEN;
        break;
      case PLANE:
      case FORBIDDEN:
        type = AxisPlaneConstraint.Type.FREE;
        break;
      default:
        type = AxisPlaneConstraint.Type.FREE;
    }
    return type;
  }

  void changeConstraint() {
    int previous = activeConstraint;
    activeConstraint = (activeConstraint + 1) % 3;

    constraints[activeConstraint]
            .setTranslationConstraintType(constraints[previous]
                    .translationConstraintType());
    constraints[activeConstraint]
            .setTranslationConstraintDirection(constraints[previous]
                    .translationConstraintDirection());
    constraints[activeConstraint]
            .setRotationConstraintType(constraints[previous]
                    .rotationConstraintType());
    constraints[activeConstraint]
            .setRotationConstraintDirection(constraints[previous]
                    .rotationConstraintDirection());

    node.setConstraint(constraints[activeConstraint]);
  }

  void displayType(AxisPlaneConstraint.Type type, int x, int y, char c) {
    String textToDisplay = new String();
    switch (type) {
      case FREE:
        textToDisplay = "FREE (";
        textToDisplay += c;
        textToDisplay += ")";
        break;
      case PLANE:
        textToDisplay = "PLANE (";
        textToDisplay += c;
        textToDisplay += ")";
        break;
      case AXIS:
        textToDisplay = "AXIS (";
        textToDisplay += c;
        textToDisplay += ")";
        break;
      case FORBIDDEN:
        textToDisplay = "FORBIDDEN (";
        textToDisplay += c;
        textToDisplay += ")";
        break;
    }

    text(textToDisplay, x, y);
  }

  void displayDir(int dir, int x, int y, char c) {
    String textToDisplay = new String();
    switch (dir) {
      case 0:
        textToDisplay = "X (";
        textToDisplay += c;
        textToDisplay += ")";
        break;
      case 1:
        textToDisplay = "Y (";
        textToDisplay += c;
        textToDisplay += ")";
        break;
      case 2:
        textToDisplay = "Z (";
        textToDisplay += c;
        textToDisplay += ")";
        break;
      case 3:
        textToDisplay = "All (";
        textToDisplay += c;
        textToDisplay += ")";
        break;
      case 4:
        textToDisplay = "None (";
        textToDisplay += c;
        textToDisplay += ")";
        break;
    }
    text(textToDisplay, x, y);
  }

  void displayText() {
    text("TRANSLATION :", 350, height - 30);
    displayDir(transDir, (350 + 105), height - 30, 'D');
    displayType(constraints[activeConstraint].translationConstraintType(),
            350, height - 60, 'T');

    text("ROTATION :", width - 120, height - 30);
    displayDir(rotDir, width - 40, height - 30, 'B');
    displayType(constraints[activeConstraint].rotationConstraintType(),
            width - 120, height - 60, 'R');

    switch (activeConstraint) {
      case 0:
        text("Constraint direction defined w/r to LOCAL (U)", 350, 20);
        break;
      case 1:
        text("Constraint direction defined w/r to WORLD (U)", 350, 20);
        break;
      case 2:
        text("Constraint direction defined w/r to EYE (U)", 350, 20);
        break;
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ConstrainedNode"});
  }
}
