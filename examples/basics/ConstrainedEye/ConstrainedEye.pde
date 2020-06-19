/**
 * Constrained Eye.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates how to add constraints to your eye
 * to limit its motion. Constraints can be defined respect to
 * the local, world or camera node. Try all the possibilities
 * following the on screen helping text.
 */

import nub.primitives.*;
import nub.core.*;
import nub.core.constraint.*;
import nub.processing.*;

Scene scene;
PFont font;

int transDir;
int rotDir;
AxisPlaneConstraint constraints[] = new AxisPlaneConstraint[2];
int activeConstraint;

//P2D or P3D
String renderer = P3D;

void setup() {
  size(700, 700, renderer);
  font = loadFont("FreeSans-16.vlw");
  textFont(font);
  scene = new Scene(this);
  scene.setRadius(400);
  scene.fit(1);
  constraints[0] = new WorldConstraint();
  // Note that an EyeConstraint(eye) would produce the same results:
  // An EyeConstraint is a LocalConstraint when applied to the eye node!
  constraints[1] = new LocalConstraint();
  transDir = 0;
  rotDir = 0;
  activeConstraint = 0;
  scene.eye().setConstraint(constraints[activeConstraint]);
  scene.enableHint(Scene.GRID | Scene.AXES | Scene.BACKGROUND);
  scene.configHint(Scene.GRID, color(0, 255, 0));
}

void draw() {
  fill(204, 102, 0, 150);
  scene.drawTorusSolenoid();
  fill(0, 255, 255);
  scene.beginHUD();
  displayText();
  scene.endHUD();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpinEye();
  else if (mouseButton == RIGHT)
    scene.mouseTranslateEye();
  else
    scene.scaleEye(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if (scene.is3D())
    scene.moveForward(event.getCount() * 20);
  else
    scene.scaleEye(event.getCount() * 20);
}

void keyPressed() {
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
    constraints[activeConstraint].setTranslationConstraintType(nextTranslationConstraintType(constraints[activeConstraint].translationConstraintType()));
  }
  if (key == 'r' || key == 'R') {
    constraints[activeConstraint].setRotationConstraintType(nextRotationConstraintType(constraints[activeConstraint].rotationConstraintType()));
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
  case PLANE:
    type = AxisPlaneConstraint.Type.FREE;
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

void changeConstraint() {
  int previous = activeConstraint;
  activeConstraint = (activeConstraint + 1) % 2;
  constraints[activeConstraint].setTranslationConstraintType(constraints[previous].translationConstraintType());
  constraints[activeConstraint].setTranslationConstraintDirection(constraints[previous].translationConstraintDirection());
  constraints[activeConstraint].setRotationConstraintType(constraints[previous].rotationConstraintType());
  constraints[activeConstraint].setRotationConstraintDirection(constraints[previous].rotationConstraintDirection());
  scene.eye().setConstraint(constraints[activeConstraint]);
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
  }
  text(textToDisplay, x, y);
}

void displayText() {
  text("TRANSLATION :", 350, height - 30);
  displayDir(transDir, (350 + 105), height - 30, 'D');
  displayType(constraints[activeConstraint].translationConstraintType(), 350, height - 60, 'T');

  text("ROTATION :", width - 120, height - 30);
  displayDir(rotDir, width - 40, height - 30, 'B');
  displayType(constraints[activeConstraint].rotationConstraintType(), width - 120, height - 60, 'R');

  switch (activeConstraint) {
  case 0:
    text("Constraint direction defined w/r to WORLD (U)", 350, 20);
    break;
  case 1:
    text("Constraint direction defined w/r to EYE (U)", 370, 20);
    break;
  }
}
