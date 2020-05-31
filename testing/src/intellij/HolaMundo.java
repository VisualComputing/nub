package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.event.MouseEvent;

public class HolaMundo extends PApplet {
  // 1. Nodes
  Node root, torus, box, can;
  // 2. Main-Scene
  Scene mainScene;
  // 3. Visual hint off-screen scene
  Scene hintScene;
  int hintSceneWidth = 500, hintSceneHeight = 500, atX, atY;
  // 5. Scene handler
  Scene scene;
  // 6. Debug (draw) ray
  Vector orig = new Vector();
  Vector dir = new Vector();
  Vector end = new Vector();
  Vector pup;

  @Override
  public void settings() {
    size(1400, 1200, P3D);
  }

  @Override
  public void setup() {
    // A. Scenes
    // 1. Main (onscreen) Scene
    mainScene = new Scene(this);
    mainScene.setRadius(500);
    mainScene.fit(1);
    mainScene.setVisualHint(Scene.AXES | Scene.GRID);
    mainScene.configHint(Scene.GRID, color(0, 255, 0));
    // 2. Hint (offscreen) Scene
    hintScene = new Scene(this, P3D, hintSceneWidth, hintSceneHeight);
    hintScene.setRadius(300);
    // B. Nodes
    // 1. root (mainScene and hintScene only)
    root = new Node();
    root.disableTagging();
    // 2. torus
    torus = new Node(root, (pg) -> {
      pg.push();
      pg.fill(255, 0, 0);
      Scene.drawTorusSolenoid(pg);
      pg.pop();
    });
    torus.scale(10);
    torus.translate(-200, -200, 0);
    torus.setPickingThreshold(0);
    // 3. box
    PShape pbox = createShape(BOX, 200);
    pbox.setFill(color(255, 255, 0));
    box = new Node(root, pbox);
    box.translate(200, 200, 0);
    box.setPickingThreshold(0);
    // 4. can (canScene only)
    //can = new Node(createCan(100, 200, 32, mainScene.context()));
    //can = new Node(createCan(100, 200, 32, loadImage("/home/pierre/IdeaProjects/nub/testing/data/texture/lachoy.jpg")));
  }

  @Override
  public void draw() {
    scene = hintScene.hasMouseFocus() ? hintScene : mainScene;
    background(0);
    // subtree rendering
    mainScene.render(root);
    if (pup != null) {
      // debug
      drawRay();
      displayOFFScreenScene(hintScene, root, color(125, 80, 90), atX, atY);
    }
  }

  void displayOFFScreenScene(Scene offscreeScene, Node subtree, int background, int x, int y) {
    mainScene.beginHUD();
    offscreeScene.beginDraw();
    offscreeScene.context().background(background);
    // subtree rendering
    offscreeScene.render(subtree);
    offscreeScene.endDraw();
    offscreeScene.display(x, y);
    mainScene.endHUD();
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
        atX = mouseX - hintSceneWidth / 2;
        atY = mouseY - hintSceneHeight;
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
    }
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

  PShape createCan(float r, float h, int detail, PImage tex) {
    textureMode(NORMAL);
    PShape sh = createShape();
    sh.beginShape(QUAD_STRIP);
    sh.noStroke();
    sh.texture(tex);
    for (int i = 0; i <= detail; i++) {
      float angle = TWO_PI / detail;
      float x = sin(i * angle);
      float z = cos(i * angle);
      float u = (float) i / detail;
      sh.normal(x, 0, z);
      sh.vertex(x * r, -h / 2, z * r, u, 0);
      sh.vertex(x * r, +h / 2, z * r, u, 1);
    }
    sh.endShape();
    return sh;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.HolaMundo"});
  }
}
