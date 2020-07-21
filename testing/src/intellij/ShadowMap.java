package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

import java.nio.file.Paths;

public class ShadowMap extends PApplet {
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  Scene scene;
  //Scene shadowMapScene;
  ShadowScene shadowMapScene;
  Node[] shapes;
  PGraphics shadowMap;
  PShader depthShader;
  float zNear = 50;
  float zFar = 700;
  int w = 700;
  int h = 700;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    shadowMap = createGraphics(w / 2, h / 2, P3D);
    depthShader = loadShader(Paths.get("testing/data/depth/depth_linear.glsl").toAbsolutePath().toString());
    depthShader.set("near", zNear);
    depthShader.set("far", zFar);
    shadowMap.shader(depthShader);
    scene = new Scene(this);
    scene.enableHint(Scene.BACKGROUND, color(75, 25, 15));
    scene.setRadius(max(w, h));
    scene.fit(1);
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(this::cube);
      shapes[i].configHint(Node.FRUSTUM, shadowMap, shadowMapType, zNear, zFar);
      scene.randomize(shapes[i]);
      shapes[i].setHighlight(0);
    }
    scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
    scene.node("light").toggleHint(Node.SHAPE | Node.FRUSTUM | Node.AXES);
    scene.node("light").setOrientation(Quaternion.from(Vector.plusK, scene.node("light").position()));
    // scene.enablePicking(false);
    ///*
    //shadowMapScene = new Scene(this, shadowMap, light);
    shadowMapScene = new ShadowScene(shadowMap, scene.node("light"));
    shadowMapScene.resetHint();
    shadowMapScene.enableHint(Scene.BACKGROUND, color(140, 160, 125));
    shadowMapScene.picking = false;
    //shadowMapScene.setRadius(300);
    shadowMapScene.setType(shadowMapType);
    // */
    frameRate(1000);
  }

  public void cube(PGraphics pg) {
    pg.pushStyle();
    if (pg == shadowMap)
      pg.noStroke();
    else {
      pg.strokeWeight(3);
      pg.stroke(0, 255, 255);
    }
    pg.fill(255, 0, 0);
    pg.box(80);
    pg.popStyle();
  }

  public void draw() {
    // 1. Fill in and display front-buffer
    scene.render();
    // 2. Fill in shadow map using the light point of view
    if (scene.isTagValid("light")) {
      shadowMapScene.display(w / 2, h / 2);
    }
    //println("frameCount: " + Scene.TimingHandler.frameCount + " (nub) " + frameCount + " (p5) ");
    println("-> frameRate: " + Scene.TimingHandler.frameRate + " (nub) " + frameRate + " (p5)");
  }

  public void mouseMoved(MouseEvent event) {
    if (event.isShiftDown()) {
      if (scene.isTagValid("light"))
        scene.node("light").toggleHint(Node.SHAPE | Node.FRUSTUM | Node.AXES);
      // no calling mouseTag since we need to immediately update the tagged node
      scene.updateMouseTag("light");
      if (scene.isTagValid("light")) {
        scene.node("light").toggleHint(Node.SHAPE | Node.FRUSTUM | Node.AXES);
        shadowMapScene.setEye(scene.node("light"));
      }
    } else
      scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (event.isShiftDown() && scene.isTagValid("light")) {
      depthShader.set("far", zFar += event.getCount() * 20);
      scene.node("light").configHint(Node.FRUSTUM, shadowMap, shadowMapType, zNear, zFar);
    }
    else
      scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ' && scene.isTagValid("light")) {
      shadowMapType = shadowMapType == Scene.Type.ORTHOGRAPHIC ? Scene.Type.PERSPECTIVE : Scene.Type.ORTHOGRAPHIC;
      scene.node("light").configHint(Node.FRUSTUM, shadowMap, shadowMapType, zNear, zFar);
    }
    if (key == 'p')
      scene.togglePerspective();
  }

  public class ShadowScene extends Scene {
    public ShadowScene(PGraphics pGraphics, Node eye) {
      super(pGraphics, eye);
    }

    @Override
    public float zNear() {
      return zNear;
    }

    @Override
    public float zFar() {
      return zFar;
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ShadowMap"});
  }
}
