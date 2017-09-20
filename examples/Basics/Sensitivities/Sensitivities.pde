/**
 * Sensitivities
 * by Jean Pierre Charalambos.
 *
 * This example illustrates the variables that can be fine tuned to control
 * the mouse behavior.
 *
 * Follow the onscreen indications to modify the available mouse sensitivities.
 *
 * Press 'd' to reset all variables to their default values.
 * Press 'o' to switch the control between eye frame and interactive frame.
 * Press ' ' to toggle the display of the controls.
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;

Scene scene;
int xM = 10;
InteractiveFrame interactiveFrame;
boolean isIFrame = false;
boolean dispControls = true;
PFont myFont;
float defRotSens, defTransSens, defSpngSens, defWheelSens, defDampFrict;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

public enum Sensitivity {
  ROTATION, TRANSLATION, WHEEL, SPINNING, DAMPING
}

void setup() {
  size(840, 560, renderer);

  myFont = loadFont("FreeSans-16.vlw");
  textFont(myFont);
  textAlign(LEFT);

  scene = new Scene(this);
  scene.setGridVisualHint(false);
  interactiveFrame = new InteractiveFrame(scene);
  interactiveFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
  interactiveFrame.setGrabsInputThreshold(scene.radius()/4);
  interactiveFrame.translate(60, 60);

  //init defaults (eye and ingeractiveFrame are the same):
  defRotSens   = interactiveFrame.rotationSensitivity();
  defTransSens = interactiveFrame.translationSensitivity();
  defSpngSens  = interactiveFrame.spinningSensitivity();
  defWheelSens = interactiveFrame.wheelSensitivity();
  defDampFrict = interactiveFrame.damping();

  scene.setRadius(150);
  scene.showAll();
}

void draw() {
  background(0);

  // Draw 3D scene first
  fill(204, 102, 0, 150);
  scene.drawTorusSolenoid();
  // Save the current model view matrix
  pushMatrix();
  // Multiply matrix to get in the frame coordinate system.
  //applyMatrix(Scene.toPMatrix(interactiveFrame.matrix())); //is possible but inefficient
  interactiveFrame.applyTransformation();//very efficient
  // Draw an axis using the Scene static function
  scene.drawAxes(20);
  // Draw a second box
  // Draw a second box
  if (scene.motionAgent().defaultGrabber() == interactiveFrame) {
    fill(0, 255, 255);
    scene.drawTorusSolenoid();
  }
  else if (interactiveFrame.grabsInput(scene.motionAgent())) {
    fill(255, 0, 0);
    scene.drawTorusSolenoid();
  }
  else {
    fill(0, 0, 255, 150);
    scene.drawTorusSolenoid();
  }
  popMatrix();

  // Finally draw 2D controls on top of the 3D scene
  displayControls();
}

void displayControls() {
  fill(200);
  if ( !dispControls ) {
    scene.beginScreenDrawing();
    displayText("Press the space-bar to display info/controls", xM, 10);
    scene.endScreenDrawing();
    return;
  }
  else {
    scene.beginScreenDrawing();
    displayText("Press the space-bar to hide info/controls", xM, 10);
    scene.endScreenDrawing();
  }

  InteractiveFrame iFrame;
  if ( isIFrame ) {
    iFrame = interactiveFrame;
    scene.beginScreenDrawing();
    displayText("Interactive frame sensitivities (Press 'o' to view/set those of Eye frame)", xM, 30);
    scene.endScreenDrawing();
  }
  else {
    iFrame = scene.eyeFrame();
    scene.beginScreenDrawing();
    displayText("Eye frame sensitivities (Press 'o' to view/set those of Interactive frame)", xM, 30);
    scene.endScreenDrawing();
  }

  fill(200, 255, 0);
  scene.beginScreenDrawing();
  displayText("Rotation sensitivity" + (equals(iFrame.rotationSensitivity(), defRotSens) ? " " : " * ") + "(increase/decrease it with 't'/'T'):", xM, 50);
  displayText(String.format("%.2f", iFrame.rotationSensitivity()), xM + 400, 50);
  displayText("Translation sensitivity" + (equals(iFrame.translationSensitivity(), defTransSens) ? " " : " * ") + "(increase/decrease it with 'u'/'U'):", xM, 70);
  displayText(String.format("%.2f", iFrame.translationSensitivity()), xM + 400, 70);
  displayText("Spinning sensitivity" + (equals(iFrame.spinningSensitivity(), defSpngSens) ? " " : " * ") + "(increase/decrease it with 'v'/'V'):", xM, 90);
  displayText(String.format("%.2f", iFrame.spinningSensitivity()), xM + 400, 90);
  displayText("Wheel sensitivity" + (equals(iFrame.wheelSensitivity(), defWheelSens) ? " " : " * ") + "(increase/decrease it with 'x'/'X'):", xM, 110);
  displayText(String.format("%.2f", iFrame.wheelSensitivity()), xM + 400, 110);
  displayText("Damping" + (equals(iFrame.damping(), defDampFrict) ? " " : " * ") + "(increase/decrease it with 'y'/'Y'):", xM, 130);
  displayText(String.format("%.2f", iFrame.damping()), xM + 400, 130);
  scene.endScreenDrawing();

  fill(200);
  if (!areDefaultsSet(iFrame)) {
    scene.beginScreenDrawing();
    displayText("Press 'd' to set sensitivities to their default values", xM, 190);
    scene.endScreenDrawing();
  }
}

void increaseSensitivity(Sensitivity sens) {
  if (isIFrame)
    increaseSensitivity(interactiveFrame, sens);
  else
    increaseSensitivity(scene.eyeFrame(), sens);
}

void decreaseSensitivity(Sensitivity sens) {
  if (isIFrame)
    decreaseSensitivity(interactiveFrame, sens);
  else
    decreaseSensitivity(scene.eyeFrame(), sens);
}

void increaseSensitivity(InteractiveFrame iFrame, Sensitivity sens) {
  changeSensitivity(iFrame, sens, true);
}

void decreaseSensitivity(InteractiveFrame iFrame, Sensitivity sens) {
  changeSensitivity(iFrame, sens, false);
}

void changeSensitivity(InteractiveFrame iFrame, Sensitivity sens, boolean increase) {
  float step = 1;
  float res;
  switch (sens) {
  case ROTATION:
    step = increase ? 0.5f : -0.5f;
    res = iFrame.rotationSensitivity() + step;
    if (0<= res && res <=10)
      iFrame.setRotationSensitivity(res);
    break;
  case TRANSLATION:
    step = increase ? 0.5f : -0.5f;
    res = iFrame.translationSensitivity() + step;
    if (0<= res && res <=10)
      iFrame.setTranslationSensitivity(res);
    break;
  case SPINNING:
    step = increase ? 0.1f : -0.1f;
    res = iFrame.spinningSensitivity() + step;
    if (0<= res && res <=100)
      iFrame.setSpinningSensitivity(res);
    break;
  case WHEEL:
    step = increase ? 5 : -5;
    res = iFrame.wheelSensitivity() + step;
    if (-100<= res && res <=100)
      iFrame.setWheelSensitivity(res);
    break;
  case DAMPING:
    step = increase ? 0.05f : -0.05f;
    res = iFrame.damping() + step;
    if (0<= res && res <=1)
      iFrame.setDamping(res);
    break;
  }
}

boolean areDefaultsSet(InteractiveFrame iFrame) {
  if (   equals(iFrame.rotationSensitivity(), defRotSens)
      && equals(iFrame.translationSensitivity(), defTransSens)
      && equals(iFrame.spinningSensitivity(), defSpngSens)
      && equals(iFrame.wheelSensitivity(), defWheelSens)
      && equals(iFrame.damping(), defDampFrict)
      )
    return true;
  return false;
}

void setDefaults(InteractiveFrame iFrame) {
  iFrame.setRotationSensitivity(defRotSens);
  iFrame.setTranslationSensitivity(defTransSens);
  iFrame.setSpinningSensitivity(defSpngSens);
  iFrame.setWheelSensitivity(defWheelSens);
  iFrame.setDamping(defDampFrict);
}

void displayText(String text, int x, int y) {
  int width = (int) textWidth(text);
  int height = (int) (textAscent() + textDescent());
  pushStyle();
  text(text, x, y, width + 1, height);
  popStyle();
}

static boolean equals(float a, float b) {
  if (abs(a-b) < 0.01f)
    return true;
  return false;
}

void keyPressed() {
  if (key == 'o' || key == 'O')
    isIFrame = !isIFrame;
  if (key == ' ')
    dispControls = !dispControls;
  if (key == 'd' || key == 'D') {
    if ( isIFrame )
      setDefaults( interactiveFrame );
    else
      setDefaults( scene.eyeFrame() );
  }
  if(key == 't')
    increaseSensitivity(Sensitivity.ROTATION);
  if(key == 'T')
    decreaseSensitivity(Sensitivity.ROTATION);
  if(key == 'u')
    increaseSensitivity(Sensitivity.TRANSLATION);
  if(key == 'U')
    decreaseSensitivity(Sensitivity.TRANSLATION);
  if(key == 'v')
    increaseSensitivity(Sensitivity.SPINNING);
  if(key == 'V')
    decreaseSensitivity(Sensitivity.SPINNING);
  if(key == 'x')
    increaseSensitivity(Sensitivity.WHEEL);
  if(key == 'X')
    decreaseSensitivity(Sensitivity.WHEEL);
  if(key == 'y')
    increaseSensitivity(Sensitivity.DAMPING);
  if(key == 'Y')
    decreaseSensitivity(Sensitivity.DAMPING);
  if ( key == 'i')
    scene.inputHandler().shiftDefaultGrabber(scene.eyeFrame(), interactiveFrame);
}