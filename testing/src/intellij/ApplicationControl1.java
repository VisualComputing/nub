package intellij;

import frames.core.Frame;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class ApplicationControl1 extends PApplet {
  Scene scene;
  Shape[] shapes;
  PFont font36;
  int totalShapes;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ApplicationControl1"});
  }

  public void settings() {
    size(1240, 840, renderer);
  }

  public void setup() {
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
          scene.drawTorusSolenoid(pg, _faces, scene.radius() / 20);
          scene.beginScreenDrawing(pg);
          Vector position = scene.screenLocation(position());
          pg.fill(isTracked() ? 0 : 255, isTracked() ? 255 : 0, isTracked() ? 0 : 255);
          pg.textFont(font36);
          pg.text(id, position.x(), position.y());
          scene.endScreenDrawing(pg);
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
              if (_faces + delta > 1)
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

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.traverse();
  }

  void control(Object... gesture) {
    control(scene.defaultFrame(), gesture);
  }

  void control(Frame frame, Object... gesture) {
    frame.interact(gesture);
  }

  public void keyPressed() {
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
        control("menos");
      else if (keyCode == RIGHT)
        control("mas");
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == CENTER)
      scene.translate();
    else
      control();
  }

  public void mouseWheel(MouseEvent event) {
    control(event.getCount());
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 1)
      control();
    if (event.getCount() == 2)
      scene.cast();
  }
}
