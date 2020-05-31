package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class HolaMundo extends PApplet {
  // 1. Nodes
  Node root, torus, box;
  // 2. Main-Scene
  String renderer = P3D;
  Scene scene;
  // 3. Off-screen scene
  Scene visualHint;
  int w = 500, h = 500, atX, atY;
  boolean ray = true;
  // 4. Scene handler
  Scene focus;
  // 5. Debug (draw) ray
  Vector orig = new Vector();
  Vector dir = new Vector();
  Vector end = new Vector();
  Vector pup;

  @Override
  public void settings() {
    size(1400, 1200, renderer);
  }

  @Override
  public void setup() {
    scene = new Scene(this);
    scene.setRadius(500);
    scene.fit(1);
    root = new Node();
    root.disableTagging();
    torus = new Node(root, (pg) -> {
      pg.push();
      pg.fill(255, 0, 0);
      Scene.drawTorusSolenoid(pg);
      pg.pop();
    });
    torus.scale(10);
    torus.translate(-200, -200, 0);
    torus.setPickingThreshold(0);
    PShape pbox = createShape(BOX, 200);
    pbox.setFill(color(255, 255, 0));
    box = new Node(root, pbox);
    box.translate(200, 200, 0);
    box.setPickingThreshold(0);
    scene.setVisualHint(Scene.AXES | Scene.GRID);
    scene.configHint(Scene.GRID, color(0, 255, 0));
    visualHint = new Scene(this, P3D, w, h);
    visualHint.setRadius(300);
  }

  @Override
  public void draw() {
    focus = pup != null ? visualHint.hasMouseFocus() ? visualHint : scene : scene;
    background(0);
    scene.render(root);
    if (ray)
      drawRay();
    if (pup != null) {
      scene.beginHUD();
      visualHint.beginDraw();
      visualHint.context().background(125, 80, 90);
      visualHint.render(root);
      visualHint.endDraw();
      visualHint.display(atX, atY);
      scene.endHUD();
    }
  }

  @Override
  public void mouseMoved(MouseEvent event) {
    if (event.isControlDown()) {
      pup = scene.mouseLocation();
      // position the auxiliar viewer
      if (pup != null) {
        visualHint.setCenter(pup);
        visualHint.eye().setPosition(pup);
        visualHint.setViewDirection(scene.displacement(Vector.plusJ));
        visualHint.setUpVector(scene.displacement(Vector.minusK));
        visualHint.fit();
        atX = mouseX - w / 2;
        atY = mouseY - h;
        // debug
        scene.mouseToLine(orig, dir);
        end = Vector.add(orig, Vector.multiply(dir, 4000.0f));
      }
    } else {
      focus.mouseTag();
    }
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.mouseSpin();
    else if (mouseButton == RIGHT)
      focus.mouseTranslate();
    else
      focus.scale(mouseX - pmouseX);
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    focus.moveForward(event.getCount() * 20);
  }

  @Override
  public void keyPressed() {
    if (key == 'f') {
      focus.flip();
    } else if (key == 'g') {
      focus.toggleHint(Scene.GRID);
    } else if (key == 'a') {
      focus.toggleHint(Scene.AXES);
    } else if (key == 'r') {
      ray = !ray;
    }
  }

  // debug ray
  void drawRay() {
    PGraphics pg = scene.context();
    if (pup != null) {
      pg.pushStyle();
      pg.strokeWeight(20);
      pg.stroke(0, 255, 0);
      pg.point(pup.x(), pup.y(), pup.z());
      pg.strokeWeight(8);
      pg.stroke(0, 0, 255);
      pg.line(orig.x(), orig.y(), orig.z(), end.x(), end.y(), end.z());
      pg.popStyle();
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.HolaMundo"});
  }
}
