package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class HolaMundoNUB extends PApplet {
  // 1. Nodes
  Node root, torus, box, can;
  // 2. Main-Scene
  Scene mainScene;
  // 3. can off-screen scene
  Scene canScene;
  int canSceneWidth = 500, canSceneHeight = 500;
  boolean displayCan;
  PShader texShader;
  // 4. Scene handler
  Scene scene;

  @Override
  public void settings() {
    size(1400, 1200, P3D);
  }

  @Override
  public void setup() {
    // A. Scenes
    // 1. Main (onscreen) Scene
    mainScene = new Scene(this, P3D);
    mainScene.setRadius(500);
    mainScene.fit(1);
    mainScene.resetHint(Scene.AXES | Scene.GRID);
    mainScene.configHint(Scene.GRID, color(0, 255, 0));
    // 2. Can (offscreen) Scene
    canScene = new Scene(this, P3D, canSceneWidth, canSceneHeight);
    canScene.setRadius(300);
    canScene.fit();
    texShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/texture/texfrag.glsl");
    canScene.context().shader(texShader);
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
    can = new Node(createCan(100, 200, 32, mainScene.context()));
  }

  @Override
  public void draw() {
    scene = canScene.hasMouseFocus() ? canScene : mainScene;
    // subtree rendering
    displayOFFScreenScene(mainScene, root, color(125), 0, 0);
    if (displayCan) {
      displayOFFScreenScene(canScene, can, color(25, 170, 150), width - canSceneWidth, height - canSceneHeight);
    }
  }

  void displayOFFScreenScene(Scene offscreeScene, Node subtree, int background, int x, int y) {
    offscreeScene.beginDraw();
    offscreeScene.context().background(background);
    // subtree rendering
    offscreeScene.render(subtree);
    offscreeScene.endDraw();
    offscreeScene.display(x, y);
  }

  @Override
  public void mouseMoved(MouseEvent event) {
    scene.mouseTag();
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
    } else if (key == 'c') {
      displayCan = !displayCan;
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
    PApplet.main(new String[]{"intellij.HolaMundoNUB"});
  }
}
