package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class HolaMundo extends PApplet {
  /*
  // 1. Nodes
  Node root, torus, box, can;
  // 2. Main-Scene
  String renderer = P3D;
  Scene mainScene;
  // 3. Visual hint off-screen scene
  Scene hintScene;
  int w, h;
  int atX, atY;
  // 4. can off-screen scene
  Scene canScene;
  boolean displayCan;
  PShader texShader;
  // 5. Scene handler
  Scene scene;
  // 6. Debug (draw) ray
  Vector orig = new Vector();
  Vector dir = new Vector();
  Vector end = new Vector();
  Vector pup;
   */

  // 1. Nodes
  Node root, torus, box;
  // 2. Main-Scene
  String renderer = P3D;
  Scene mainScene;
  // 3. Visual hint off-screen scene
  Scene hintScene;
  int w = 500, h = 500, atX, atY;
  // 4. can off-screen scene
  Scene canScene;
  boolean displayCan;
  PShader texShader;
  // 5. Scene handler
  Scene scene;
  // 6. Debug (draw) ray
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
    mainScene = new Scene(this);
    mainScene.setRadius(500);
    mainScene.fit(1);
    mainScene.setVisualHint(Scene.AXES | Scene.GRID);
    mainScene.configHint(Scene.GRID, color(0, 255, 0));
    hintScene = new Scene(this, P3D, w, h);
    hintScene.setRadius(300);
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
  }

  @Override
  public void draw() {
    scene = pup != null ? hintScene.hasMouseFocus() ? hintScene : mainScene : mainScene;
    background(0);
    mainScene.render(root);
    if (pup != null) {
      drawRay();
      mainScene.beginHUD();
      hintScene.beginDraw();
      hintScene.context().background(125, 80, 90);
      hintScene.render(root);
      hintScene.endDraw();
      hintScene.display(atX, atY);
      mainScene.endHUD();
    }
  }

  @Override
  public void mouseMoved(MouseEvent event) {
    if (event.isControlDown()) {
      pup = mainScene.mouseLocation();
      // position the auxiliar viewer
      if (pup != null) {
        hintScene.setCenter(pup);
        hintScene.eye().setPosition(pup);
        hintScene.setViewDirection(mainScene.displacement(Vector.plusJ));
        hintScene.setUpVector(mainScene.displacement(Vector.minusK));
        hintScene.fit();
        atX = mouseX - w / 2;
        atY = mouseY - h;
        // debug
        mainScene.mouseToLine(orig, dir);
        end = Vector.add(orig, Vector.multiply(dir, 4000.0f));
      }
    } else {
      scene.mouseTag();
    }
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.scale(mouseX - pmouseX);
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  @Override
  public void keyPressed() {
    if (key == 'f') {
      scene.flip();
    } else if (key == 'g') {
      scene.toggleHint(Scene.GRID);
    } else if (key == 'a') {
      scene.toggleHint(Scene.AXES);
    }/* else if (key == 'r') {
      ray = !ray;
    }*/
  }

  // debug ray
  void drawRay() {
    PGraphics pg = mainScene.context();
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
