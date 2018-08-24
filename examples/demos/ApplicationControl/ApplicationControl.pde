/**
 * Application Control.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates how to customize shape behaviors by
 * overriding the frame interact(Object... gesture) method.
 *
 * The toruses color and number of faces are controled with the
 * keys and the mouse. To pick a torus press the [0..9] keys
 * or double-click on it; press other key or double-click on the
 * the background to reset your picking selection. Once a torus
 * is picked try the key arrows, the mouse wheel or a single 
 * mouse click, to control its color and number of faces.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

Scene scene;
Shape[] shapes;
PFont font36;
int totalShapes;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

public void settings() {
  size(1240, 840, renderer);
}

void setup() {
  scene = new Scene(this);
  scene.setFieldOfView(PI / 3);
  scene.fitBallInterpolation();
  shapes = new Shape[10];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Shape(scene) {
      int id = totalShapes++;
      int _faces = randomFaces(), _color = randomColor();

      @Override
      public void setGraphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(_color);
        Scene.drawTorusSolenoid(pg, _faces, scene.radius() / 20);
        scene.beginHUD(pg);
        Vector position = scene.screenLocation(position());
        pg.fill(isTracked() ? 0 : 255, isTracked() ? 255 : 0, isTracked() ? 0 : 255);
        pg.textFont(font36);
        pg.text(id, position.x(), position.y());
        scene.endHUD(pg);
        pg.popStyle();
      }

      @Override
      public void interact(Object... gesture) {
        if (gesture.length == 0)
          _color = randomColor();
        if (gesture.length == 1)
          if (gesture[0] instanceof String) {
            if (((String) gesture[0]).matches("mas"))
              _faces++;
            else if (((String) gesture[0]).matches("menos"))
              if (_faces > 2)
                _faces--;
          } else if (gesture[0] instanceof Integer) {
            int delta = (Integer) gesture[0];
            if (_faces +  delta > 1)
              _faces = _faces + delta;
          }
      }
    };
    shapes[i].randomize();
  }
  font36 = loadFont("FreeSans-36.vlw");
}

int randomColor() {
  return color(random(255), random(255), random(255), random(125, 255));
}

int randomFaces() {
  return (int) random(3, 15);
}

void draw() {
  background(0);
  scene.drawAxes();
  scene.traverse();
}

void keyPressed() {
  int value = Character.getNumericValue(key);
  if (value >= 0 && value < 10)
    scene.setTrackedFrame(shapes[value]);
  if (key == ' ')
    scene.resetTrackedFrame();
  if (key == CODED)
    if (keyCode == UP)
      scene.translate(0, -10);
    else if (keyCode == DOWN)
      scene.translate(0, 10);
    else if (keyCode == LEFT)
      scene.defaultHIDControl("menos");
    else if (keyCode == RIGHT)
      scene.defaultHIDControl("mas");
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == CENTER)
    scene.zoom(scene.mouseDX());
  else
    scene.translate();
}

void mouseWheel(MouseEvent event) {
  scene.defaultHIDControl(event.getCount());
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 1)
    scene.defaultHIDControl();
  if (event.getCount() == 2)
    scene.cast();
}
