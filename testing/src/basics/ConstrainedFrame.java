package basics;

import frames.core.Frame;
import frames.core.constraint.AxisPlaneConstraint;
import frames.core.constraint.EyeConstraint;
import frames.core.constraint.LocalConstraint;
import frames.core.constraint.WorldConstraint;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.MouseEvent;

public class ConstrainedFrame extends PApplet {

  public void settings() {
    size(800, 800, P3D);
  }

  Scene scene;
  boolean mouseTracking = true;
  PFont myFont;
  int transDir;
  int rotDir;
  Frame node;
  AxisPlaneConstraint constraints[] = new AxisPlaneConstraint[3];
  int activeConstraint;
  boolean wC = true;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void setup() {
    size(800, 800, renderer);
    myFont = loadFont("FreeSans-16.vlw");
    textFont(myFont);

    scene = new Scene(this);
    scene.setFieldOfView(PI / 3);

    constraints[0] = new LocalConstraint();
    // Note that an EyeConstraint(eye) would produce the same results:
    // An EyeConstraint is a LocalConstraint when applied to the eye
    constraints[1] = new WorldConstraint();
    constraints[2] = new EyeConstraint(scene);
    transDir = 0;
    rotDir = 0;
    activeConstraint = 0;

    node = new Frame(scene);
    node.translate(new Vector(20, 20, 0));
    node.setConstraint(constraints[activeConstraint]);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    pushMatrix();
    scene.applyTransformation(node);
    scene.drawAxes(40);
    if (scene.mouseTrack(node)) {
      fill(0, 255, 255);
      scene.drawTorusSolenoid();
    } else if (scene.isTrackedFrame(node)) {
      fill(255, 0, 0);
      scene.drawTorusSolenoid();
    } else {
      fill(0, 0, 255, 150);
      scene.drawTorusSolenoid();
    }
    popMatrix();

    fill(0, 0, 255);
    scene.beginScreenDrawing();
    displayText();
    scene.endScreenDrawing();
  }

  public void mouseMoved() {
    if (mouseTracking)
      scene.mouseTrack();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.mouseZoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.mouseZoom(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'i')
      if (scene.isMouseTrackedFrame(node)) {
        scene.resetMouseTrackedFrame();
        mouseTracking = true;
      } else {
        scene.setMouseTrackedFrame(node);
        mouseTracking = false;
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

    Vector dir = new Vector(0.0f, 0.0f, 0.0f);
    switch (transDir) {
      case 0:
        dir.setX(1.0f);
        break;
      case 1:
        dir.setY(1.0f);
        break;
      case 2:
        dir.setZ(1.0f);
        break;
    }
    constraints[activeConstraint].setTranslationConstraintDirection(dir);

    dir.set(0.0f, 0.0f, 0.0f);
    switch (rotDir) {
      case 0:
        dir.setX(1.0f);
        break;
      case 1:
        dir.setY(1.0f);
        break;
      case 2:
        dir.setZ(1.0f);
        break;
    }
    constraints[activeConstraint].setRotationConstraintDirection(dir);
  }

  static AxisPlaneConstraint.Type nextTranslationConstraintType(AxisPlaneConstraint.Type type) {
    AxisPlaneConstraint.Type rType;
    switch (type) {
      case FREE:
        rType = AxisPlaneConstraint.Type.PLANE;
        break;
      case PLANE:
        rType = AxisPlaneConstraint.Type.AXIS;
        break;
      case AXIS:
        rType = AxisPlaneConstraint.Type.FORBIDDEN;
        break;
      case FORBIDDEN:
        rType = AxisPlaneConstraint.Type.FREE;
        break;
      default:
        rType = AxisPlaneConstraint.Type.FREE;
    }
    return rType;
  }

  static AxisPlaneConstraint.Type nextRotationConstraintType(AxisPlaneConstraint.Type type) {
    AxisPlaneConstraint.Type rType;
    switch (type) {
      case FREE:
        rType = AxisPlaneConstraint.Type.AXIS;
        break;
      case AXIS:
        rType = AxisPlaneConstraint.Type.FORBIDDEN;
        break;
      case PLANE:
      case FORBIDDEN:
        rType = AxisPlaneConstraint.Type.FREE;
        break;
      default:
        rType = AxisPlaneConstraint.Type.FREE;
    }
    return rType;
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
    PApplet.main(new String[]{"basics.ConstrainedFrame"});
  }
}
