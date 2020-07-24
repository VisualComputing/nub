package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class Navigation extends PApplet {
  // 1. Nodes
  Node root, torus, box;
  // 2. Main-Scene
  Scene mainScene;
  // 3. can off-screen scene
  Scene auxScene;
  int auxSceneWidth = 300, auxSceneHeight = 300;
  boolean displayAux;
  // 4. Scene handler
  Scene scene;

  @Override
  public void settings() {
    size(700, 700, P3D);
  }

  @Override
  public void setup() {
    // A. Scenes
    // 1. Main (Scene
    mainScene = new Scene(createGraphics(width, height, P3D), 10, 220);
    //mainScene.fit(1);
    mainScene.enableHint(Scene.AXES | Scene.GRID);
    mainScene.configHint(Scene.GRID, color(0, 255, 0));
    mainScene.enableHint(Scene.HUD);
    mainScene.enableHint(Scene.BACKGROUND, color(125));
    mainScene.eye().enableHint(Node.BOUNDS);
    mainScene.setHUD(this::hud);
    // 2. Can (offscreen) Scene
    auxScene = new Scene(createGraphics(auxSceneWidth, auxSceneHeight, P3D), 300);
    auxScene.enableHint(Scene.BACKGROUND, color(25, 170, 150));
    auxScene.fit();
    //texShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/texture/texfrag.glsl");
    //canScene.context().shader(texShader);
    // B. Nodes
    torus = new Node();
    torus.enableHint(Node.TORUS);
    torus.translate(-20, -20, -50);
    // 3. box
    PShape pbox = createShape(BOX, 20);
    pbox.setFill(color(255, 255, 0));
    box = new Node(torus, pbox);
    box.translate(20, 20, 0);
  }

  public void hud(PGraphics pg) {
    pg.fill(255, 0, 255, 125);
    pg.rect(50,50, 200, 200);
  }

  @Override
  public void draw() {
    scene = auxScene.hasMouseFocus() ? auxScene : mainScene;
    // subtree rendering
    mainScene.display(root);
    if (displayAux) {
      auxScene.display(width - auxSceneWidth, height - auxSceneHeight);
    }
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
      Scene.leftHanded = !Scene.leftHanded;
    } else if (key == 'g') {
      scene.toggleHint(Scene.GRID);
    } else if (key == 'a') {
      scene.toggleHint(Scene.AXES);
    } else if (key == 'c') {
      displayAux = !displayAux;
    } else if (key == 's') {
      mainScene.fit(1);
    } else if (key == 't') {
      mainScene.togglePerspective();
    }
    if (key == ' ') {
      mainScene.toggleHint(Scene.AXES | Scene.HUD);
      if (mainScene.isHintEnable(Scene.BACKGROUND)) println("Scene.BACKGROUND");
      if (mainScene.isHintEnable(Scene.BACKGROUND | Scene.AXES)) println("Scene.BACKGROUND | Scene.AXES)");
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Navigation"});
  }
}
