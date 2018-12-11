/**
 * Frame API.
 * by Jean Pierre Charalambos.
 * <p>
 * This example illustrates the powerful Frame API used to convert points and
 * vectors along a frame hierarchy. The following frame hierarchy is implemented:
 * <p>
 * world
 * ^
 * |\
 * | \
 * f1 eye
 * ^   ^
 * |\   \
 * | \   \
 * f2 f3  f5
 * ^
 * |
 * |
 * f4
 * <p>
 * Press the space bar to browse the different conversion methods shown here.
 */

package intellij;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.MouseEvent;

public class FrameAPI4 extends PApplet {
  Scene scene;
  InteractiveFrame f1, f2, f3, f4, f5;
  Vector pnt = new Vector(40, 30, 20);
  Vector vec = new Vector(50, 50, 50);
  PFont font16, font13;
  Mode mode;
  int wColor = color(255, 255, 255);
  int f1Color = color(255, 0, 0);
  int f2Color = color(0, 255, 0);
  int f3Color = color(0, 0, 255);
  int f4Color = color(255, 0, 255);
  int f5Color = color(255, 255, 0);

  public enum Mode {
    m1, m2, m3, m4, m5, m6
  }

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(900, 900, renderer);
  }

  public void setup() {
    scene = new Scene(this);
    mode = Mode.m1;

    scene.setRadius(200);
    scene.fit(1);

    f1 = new InteractiveFrame(f1Color);
    f1.translate(-50, -20, 30);
    f1.scale(1.3f);

    f2 = new InteractiveFrame(f1, f2Color);
    f2.translate(60, -40, -30);
    f2.scale(1.2f);

    f3 = new InteractiveFrame(f1, f3Color);
    f3.translate(60, 55, -30);
    f3.rotate(new Quaternion(new Vector(0, 1, 0), -HALF_PI));
    f3.scale(1.1f);

    f4 = new InteractiveFrame(f2, f4Color);
    f4.translate(60, -55, 30);
    f4.rotate(new Quaternion(new Vector(0, 1, 0), QUARTER_PI));
    f4.scale(0.9f);

    f5 = new InteractiveFrame(scene.eye(), f5Color);
    f5.translate(-100, 0, -250);

    font16 = loadFont("FreeSans-16.vlw");
    font13 = loadFont("FreeSans-13.vlw");
  }

  public void draw() {
    background(0);

    //world:
    scene.drawAxes();
    pushStyle();
    stroke(wColor);
    strokeWeight(10);
    point(pnt.x(), pnt.y(), pnt.z());
    popStyle();

    pushMatrix();
    scene.applyTransformation(f1);
    f1.draw(scene);
    pushMatrix();
    scene.applyTransformation(f3);
    f3.draw(scene);
    popMatrix();
    pushMatrix();
    scene.applyTransformation(f2);
    f2.draw(scene);
    pushMatrix();
    scene.applyTransformation(f4);
    f4.draw(scene);
    popMatrix();
    popMatrix();
    popMatrix();

    //eye
    pushMatrix();
    scene.applyTransformation(scene.eye());
    pushMatrix();
    scene.applyTransformation(f5);
    f5.draw(scene);
    popMatrix();
    popMatrix();

    drawMode();
    displayText();
  }

  void drawMode() {
    // points
    pushStyle();
    noStroke();
    fill(0, 255, 255);
    switch (mode) {
      ///*
      case m1: // f2 -> world
        drawArrowConnectingPoints(f2.worldLocation(pnt));
        break;
      case m2: // f2 -> f1
        drawArrowConnectingPoints(f1, f1.location(pnt, f2));
        break;
      case m3: // f1 -> f2
        drawArrowConnectingPoints(f2, f2.location(pnt, f1));
        break;
      case m4: // f3 -> f4
        drawArrowConnectingPoints(f4, f4.location(pnt, f3));
        break;
      case m5: // f4 -> f3
        drawArrowConnectingPoints(f3, f3.location(pnt, f4));
        break;
      case m6: // f5 -> f4
        drawArrowConnectingPoints(f4, f4.location(pnt, f5));
        break;
      //*/
      /*
      case m1: // f2 -> world
        drawArrowConnectingPoints(f2.worldLocation(pnt));
        break;
      case m2: // f2 -> f1
        drawArrowConnectingPoints(f1, f1.location(f2.worldLocation(pnt)));
        break;
      case m3: // f1 -> f2
        drawArrowConnectingPoints(f2, f2.location(f1.worldLocation(pnt)));
        break;
      case m4: // f3 -> f4
        drawArrowConnectingPoints(f4, f4.location(f3.worldLocation(pnt)));
        break;
      case m5: // f4 -> f3
        drawArrowConnectingPoints(f3, f3.location(f4.worldLocation(pnt)));
        break;
      case m6: // f5 -> f4
        drawArrowConnectingPoints(f4, f4.location(f5.worldLocation(pnt)));
        break;
        // */
    }
    popStyle();

    // vectors
    pushStyle();
    noStroke();
    fill(125);
    switch (mode) {
      ///*
      case m1: // f2 -> world
        drawVector(f2, vec);
        drawVector(f2.worldDisplacement(vec));
        break;
      case m2: // f2 -> f1
        drawVector(f2, vec);
        drawVector(f1, f1.displacement(vec, f2));
        break;
      case m3: // f1 -> f2
        drawVector(f1, vec);
        drawVector(f2, f2.displacement(vec, f1));
        break;
      case m4: // f3 -> f4
        drawVector(f3, vec);
        drawVector(f4, f4.displacement(vec, f3));
        break;
      case m5: // f4 -> f3
        drawVector(f4, vec);
        drawVector(f3, f3.displacement(vec, f4));
        break;
      case m6: // f5 -> f4
        drawVector(f5, vec);
        drawVector(f4, f4.displacement(vec, f5));
        break;
      //*/
      /*
      case m1: // f2 -> world
        drawVector(f2, vec);
        drawVector(f2.worldDisplacement(vec));
        break;
      case m2: // f2 -> f1
        drawVector(f2, vec);
        drawVector(f1, f1.displacement(f2.worldDisplacement(vec)));
        break;
      case m3: // f1 -> f2
        drawVector(f1, vec);
        drawVector(f2, f2.displacement(f1.worldDisplacement(vec)));
        break;
      case m4: // f3 -> f4
        drawVector(f3, vec);
        drawVector(f4, f4.displacement(f3.worldDisplacement(vec)));
        break;
      case m5: // f4 -> f3
        drawVector(f4, vec);
        drawVector(f3, f3.displacement(f4.worldDisplacement(vec)));
        break;
      case m6: // f5 -> f4
        drawVector(f5, vec);
        drawVector(f4, f4.displacement(f5.worldDisplacement(vec)));
        break;
        //*/
    }
    popStyle();
  }

  void displayText() {
    pushStyle();
    Vector pos;
    scene.beginHUD();
    textFont(font13);
    fill(f1Color);
    pos = scene.screenLocation(f1.position());
    text("Frame 1", pos.x(), pos.y());
    fill(f2Color);
    pos = scene.screenLocation(f2.position());
    text("Frame 2", pos.x(), pos.y());
    fill(f3Color);
    pos = scene.screenLocation(f3.position());
    text("Frame 3", pos.x(), pos.y());
    fill(f4Color);
    pos = scene.screenLocation(f4.position());
    text("Frame 4", pos.x(), pos.y());
    fill(f5Color);
    pos = scene.screenLocation(f5.position());
    text("Frame 5", pos.x(), pos.y());
    fill(wColor);
    textFont(font16);
    text("Press the space bar to change mode", 5, 15);
    switch (mode) {
      case m1: // f2 -> world
        text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 2 to world", 5, 35);
        break;
      case m2: // f2 -> f1
        text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 2 to frame 1", 5, 35);
        break;
      case m3: // f1 -> f2
        text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 1 to frame 2", 5, 35);
        break;
      case m4: // f3 -> f4
        text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 3 to frame 4", 5, 35);
        break;
      case m5: // f4 -> f3
        text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 4 to frame 3", 5, 35);
        break;
      case m6: // f5 -> f4
        text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 5 to frame 4", 5, 35);
        break;
    }
    scene.endHUD();
    popStyle();
  }

  void drawArrowConnectingPoints(Vector to) {
    drawArrow(null, pnt, to);
  }

  void drawArrowConnectingPoints(Frame frame, Vector to) {
    drawArrow(frame, pnt, to);
  }

  void drawVector(Vector to) {
    drawArrow(null, new Vector(), to);
  }

  void drawVector(Frame frame, Vector to) {
    drawArrow(frame, new Vector(), to);
  }

  void drawArrow(Frame frame, Vector from, Vector to) {
    if (frame != null) {
      pushMatrix();
      //scene.applyModelView(frame.worldMatrix());// world, is handy but inefficient
      scene.applyWorldTransformation(frame);
      scene.drawArrow(from, to, 1);
      popMatrix();
    } else
      scene.drawArrow(from, to, 1);
  }

  public void keyPressed() {
    if (key == ' ')
      switch (mode) {
        case m1:
          mode = Mode.m2;
          break;
        case m2:
          mode = Mode.m3;
          break;
        case m3:
          mode = Mode.m4;
          break;
        case m4:
          mode = Mode.m5;
          break;
        case m5:
          mode = Mode.m6;
          break;
        case m6:
          mode = Mode.m1;
          break;
      }
    if (key == 'v' || key == 'V')
      scene.flip();
    if (key == '+')
      scene.eye().setScaling(scene.eye().scaling() * 1.1f);
    if (key == '-')
      scene.eye().setScaling(scene.eye().scaling() / 1.1f);
    if (key == 'e')
      f1.enableTracking(!f1.isTrackingEnabled());
  }

  @Override
  public void mouseMoved() {
    scene.track(new Frame[]{f1, f2, f3, f4, f5});
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
  }

  public class InteractiveFrame extends Frame {
    int _c;
    Vector pnt;

    public InteractiveFrame(int color) {
      super();
      _c = color;
      pnt = new Vector(40, 30, 20);
    }

    public InteractiveFrame(Frame frame, int color) {
      super(frame);
      _c = color;
      pnt = new Vector(40, 30, 20);
    }

    public void draw(Scene scn) {
      pushStyle();
      scn.drawAxes(40);
      stroke(_c);
      scn.drawShooterTarget(this);
      strokeWeight(10);
      point(pnt.x(), pnt.y(), pnt.z());
      popStyle();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.FrameAPI4"});
  }
}
