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
  Node[] shapes;
  PGraphics shadowMap;
  PShader depthShader;
  float zNear = 50;
  float zFar = 1000;
  int w = 700;
  int h = 700;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    shadowMap = createGraphics(w / 2, h / 2, P3D);
    //depthShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/depth/depth_linear.glsl");
    //depthShader.set("near", zNear);
    //depthShader.set("far", zFar);
    depthShader = loadShader(Paths.get("testing/data/depth/depth_linear.glsl").toAbsolutePath().toString());
    shadowMap.shader(depthShader);
    scene = new Scene(this);
    scene.setRadius(max(w, h));
    scene.fit(1);
    scene.enableHint(Scene.BACKGROUND, color(75, 25, 15));
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(this::cube);
      //shapes[i].setPickingPolicy(Node.BULLSEYE);
      shapes[i].configHint(Node.FRUSTUM, shadowMap, shadowMapType, zNear, zFar);
      scene.randomize(shapes[i]);
      shapes[i].disableHint(Node.HIGHLIGHT);
      // shapes[i].disableTagging();
    }
    // /*
    scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
    scene.node("light").toggleHint(Node.SHAPE | Node.FRUSTUM | Node.AXES);
    scene.node("light").setOrientation(Quaternion.from(Vector.plusK, scene.node("light").position()));
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
    if (scene.node("light") != null) {
      shadowMap.beginDraw();
      shadowMap.background(140, 160, 125);
      Scene.render(shadowMap, scene.node("light"), shadowMapType, zNear, zFar);
      shadowMap.endDraw();
      // 3. Display shadow map
      scene.beginHUD();
      image(shadowMap, w / 2, h / 2);
      scene.endHUD();
    }
    println("-> frameRate: " + Scene.frameRate() + " (nub) " + frameRate + " (p5)");
  }

  public void mouseMoved(MouseEvent event) {
    if (event.isShiftDown()) {
      if (scene.isTagValid("light"))
        scene.node("light").toggleHint(Node.SHAPE | Node.FRUSTUM | Node.AXES);
      // no calling mouseTag since we need to immediately update the tagged node
      scene.updateMouseTag("light");
      if (scene.isTagValid("light"))
        scene.node("light").toggleHint(Node.SHAPE | Node.FRUSTUM | Node.AXES);
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
    if (event.isShiftDown()) {
      // custom interaction of the light node: setting the light
      // zFar plane is implemented as a custom behavior by node.interact()
      if (scene.node("light") != null)
        depthShader.set("far", zFar += event.getCount() * 20);
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

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ShadowMap"});
  }
}
