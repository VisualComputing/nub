package intellij;

import nub.core.Interpolator;
import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

public class Framework extends PApplet {
  Node n2, n4, lastKeyFrame;
  Scene scene1, scene2, focus;
  Interpolator interpolator;
  boolean displayAuxiliarViewers = true;
  // whilst scene1 is either on-screen or not; scene2 and scene3 are off-screen
  // test both cases here
  boolean onScreen = true;

  int pixelX = 600;
  int pixelY = 600;
  int w = 1400;
  int h = 1400;
  float alpha = 125;
  int steps = 5;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene1 = onScreen ? new Scene(g) : new Scene(createGraphics(w, h, P3D));
    //scene1.enableHint(Scene.BACKGROUND, color(75, 25, 15, 100));
    scene1.enableHint(Scene.AXES);
    //scene1.enableHint(Scene.BACKGROUND, color(75, 25, 15));
    scene1.enableHint(Scene.BACKGROUND, color(255, 165, 15));
    //scene1.enableHint(Scene.BACKGROUND, color(255, 255, 15));
    scene1.enableHint(Scene.GRID, color(0, 255, 255/*0, 225, 15*/));
    scene1.eye().tagging = false;
    //scene1.setBounds(1000);

    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    scene2 = new Scene(createGraphics(w / 2, h / 2, P3D));
    scene2.enableHint(Scene.BACKGROUND /*| Scene.AXES*/);
    //scene2.configHint(Scene.BACKGROUND, color(75, 25, 175, 100));
    //rgba(144, 238, 144, 1)
    //scene2.configHint(Scene.BACKGROUND, color(#90EE90));
    scene2.configHint(Scene.BACKGROUND, color(144, 238, 144, 175));
    scene2.eye().tagging = false;
    //scene2.setBounds(1000);

    n2 = new Node();
    n2.enableHint(Node.TORUS, color(255, 0, 125, alpha), 4);
    n4 = new Node(scene1.eye(), boxShape());
    //n4.enableHint(Node.TORUS, );
    scene1.randomize(n4);

    interpolator = new Interpolator(n2);
    interpolator.configHint(Interpolator.STEPS, Node.TORUS);
    interpolator.setSteps(steps);
  }

  PShape boxShape() {
    PShape box = createShape(BOX, 30);
    box.setFill(color(50, 75, 255));
    box.setStrokeWeight(5);
    box.setStroke(color(60, 255, 25));
    return box;
  }

  public void keyPressed() {
    if (key == 'L' && alpha + 5 < 256) {
      alpha += 5;
      n2.configHint(Node.TORUS, color(255, 0, 125, alpha), 4);
      for (Node node : interpolator.keyFrames().values()) {
        node.configHint(Node.TORUS, color(255, 0, 125, alpha), 4);
      }
    }
    if (key == 'l' && alpha - 5 >= 0) {
      alpha -= 5;
      n2.configHint(Node.TORUS, color(255, 0, 125, alpha), 4);
      for (Node node : interpolator.keyFrames().values()) {
        node.configHint(Node.TORUS, color(255, 0, 125, alpha), 4);
      }
    }
    if (key == 'r') {
      interpolator.removeKeyFrame(0);
    }
    /*
    if (key == 'a')
      interpolator.addKeyFrame(n2.get());
    // */
    if (key == 'a') {
      lastKeyFrame = n2.get();
      lastKeyFrame.configHint(Node.TORUS, color(255, 0, 125, alpha));
      interpolator.addKeyFrame(lastKeyFrame);
    }
    if (key == 's')
      interpolator.toggleHint(/*Interpolator.SPLINE |*/ Interpolator.STEPS);
    if (key == ' ')
      displayAuxiliarViewers = !displayAuxiliarViewers;
    if (key == 'f')
      focus.fit(1);
    if (key == 't') {
      if (focus == null)
        return;
      focus.togglePerspective();
    }
    if (key == 'u' && lastKeyFrame != null) {
      n2.setPosition(lastKeyFrame);
      n2.setOrientation(lastKeyFrame);
      n2.setMagnitude(lastKeyFrame);
      n2.configHint(Node.TORUS, color(255, 0, 125));
    }
    if (key == 'U' && lastKeyFrame != null) {
      n2.setPosition(lastKeyFrame);
      n2.setOrientation(lastKeyFrame);
      n2.setMagnitude(lastKeyFrame);
      n2.configHint(Node.TORUS, color(255, 0, 125, alpha));
    }
    if (key == '+')
      interpolator.setSteps(++steps);
    if (key == '-' && steps - 1 > 0)
      interpolator.setSteps(--steps);
  }

  public void mouseMoved() {
    if (focus == null)
      return;
    focus.mouseTag();
  }

  public void mouseDragged() {
    if (focus == null)
      return;
    if (mouseButton == LEFT)
      focus.mouseSpin();
    else if (mouseButton == RIGHT)
      focus.mouseTranslate();
    else
      focus.moveForward(focus.mouseDX());
  }

  public void mouseWheel(MouseEvent event) {
    if (focus == null)
      return;
    focus.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (focus == null)
      return;
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.alignTag();
  }

  public void draw() {
    focus = scene2.hasMouseFocus() ? scene2 : scene1.hasMouseFocus() ? scene1 : null;
    scene1.display();
    if (displayAuxiliarViewers) {
      scene2.display(pixelX, pixelY);
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Framework"});
  }
}
