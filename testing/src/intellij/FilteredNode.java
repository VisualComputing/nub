package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.MouseEvent;

public class FilteredNode extends PApplet {
  Scene scene;
  PFont font;
  Node node;

  final int FREE = 0, FORBIDDEN = 1, AXIS = 2, PLANE = 3;
  final int LOCAL = 0, WORLD = 1, EYE = 2;
  int space = LOCAL;
  Vector translationAxis;
  int translationAxisType;
  int translationFilter = FREE;
  Vector rotationAxis;
  int rotationAxisType;
  int rotationFilter = FREE;

  //Choose P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    font = loadFont("FreeSans-16.vlw");
    textFont(font);
    scene = new Scene(this);
    node = new Node();
    node.enableHint(Node.TORUS | Node.BULLSEYE | Node.AXES);
    scene.randomize(node);
    node.translate(new Vector(20, 20, 0));
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
      rotationAxisType = (rotationAxisType + 1) % 3;
    }
    if (key == 'd' || key == 'D') {
      translationAxisType = (translationAxisType + 1) % 3;
    }
    if (key == 'u' || key == 'U') {
      space = (space + 1) % 3;
    }
    if (key == 't' || key == 'T') {
      translationFilter = (translationFilter + 1) % 4;
    }
    if (key == 'r' || key == 'R') {
      rotationFilter = (rotationFilter + 1) % 3;
    }
    updateFilters();
  }

  void updateFilters() {
    translationAxis = translationAxisType == 0 ? Vector.plusI : translationAxisType == 1 ? Vector.plusJ : Vector.plusK;
    rotationAxis = rotationAxisType == 0 ? Vector.plusI : rotationAxisType == 1 ? Vector.plusJ : Vector.plusK;
    if (space == WORLD || space == EYE) {
      translationAxis = node.displacement(translationAxis, space == EYE ? scene.eye() : null);
      rotationAxis = node.displacement(rotationAxis, space == EYE ? scene.eye() : null);
    }
    switch (translationFilter) {
      case FREE:
        node.resetTranslationFilter();
        break;
      case PLANE:
        node.setTranslationPlaneFilter(translationAxis);
        break;
      case AXIS:
        node.setTranslationAxisFilter(translationAxis);
        break;
      case FORBIDDEN:
        node.setForbidTranslationFilter();
        break;
    }
    switch (rotationFilter) {
      case FREE:
        node.resetRotationFilter();
        break;
      case AXIS:
        node.setRotationAxisFilter(rotationAxis);
        break;
      case FORBIDDEN:
        node.setForbidRotationFilter();
        break;
    }
  }

  void displayType(int type, int x, int y, char c) {
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

  void displayDirection(int dir, int x, int y, char c) {
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
    text("TRANSLATION :", 340, height - 30);
    displayDirection(translationAxisType, (340 + 125), height - 30, 'D');
    displayType(translationFilter, 340, height - 60, 'T');

    text("ROTATION :", width - 140, height - 30);
    displayDirection(rotationAxisType, width - 40, height - 30, 'B');
    displayType(rotationFilter, width - 140, height - 60, 'R');

    switch (space) {
      case LOCAL:
        text("Filter defined w/r to LOCAL (U)", 350, 20);
        break;
      case WORLD:
        text("Filter defined w/r to WORLD (U)", 350, 20);
        break;
      case EYE:
        text("Filter defined w/r to EYE (U)", 350, 20);
        break;
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.FilteredNode"});
  }
}
