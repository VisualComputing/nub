package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

import java.nio.file.Paths;

public class DepthMap extends PApplet {
  Scene scene;
  Scene depthMapScene;
  Node[] shapes;
  PGraphics depthMap;
  PShader depthShader;
  float zNear = 50;
  float zFar = 700;
  int w = 1400;
  int h = 1400;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    depthMap = createGraphics(w / 2, h / 2, P3D);
    depthShader = loadShader(Paths.get("testing/data/depth/depth_linear.glsl").toAbsolutePath().toString());
    depthShader.set("near", zNear);
    depthShader.set("far", zFar);
    depthMap.shader(depthShader);
    scene = new Scene(this, max(w, h));
    scene.fit(1000);
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(this::cube);
      scene.randomize(shapes[i]);
      shapes[i].setHighlight(0);
    }
    scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
    scene.node("light").toggleHint(Node.SHAPE | Node.AXES | Node.BOUNDS);
    scene.node("light").setWorldOrientation(Quaternion.from(Vector.plusK, scene.node("light").worldPosition()));
    // scene.enablePicking(false);
    depthMapScene = new Scene(depthMap, scene.node("light"));
    depthMapScene.setZNear(() -> zNear);
    depthMapScene.setZFar(() -> zFar);
    depthMapScene.togglePerspective();
    depthMapScene.picking = false;
    frameRate(1000);
  }

  public void cube(PGraphics pg) {
    pg.pushStyle();
    if (pg == depthMap)
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
    background(75, 25, 15);
    scene.render();
    // 2. Fill in shadow map using the light point of view
    if (scene.isTagValid("light")) {
      depthMapScene.openContext();
      depthMapScene.context().background(140, 160, 125);
      depthMapScene.drawAxes();
      depthMapScene.render();
      depthMapScene.closeContext();
      depthMapScene.image(w / 2, h / 2);
    }
  }

  public void mouseMoved(MouseEvent event) {
    if (event.isShiftDown()) {
      if (scene.isTagValid("light")) {
        scene.node("light").toggleHint(Node.SHAPE | Node.AXES | Node.BOUNDS);
      }
      // no calling mouseTag since we need to immediately update the tagged node
      scene.updateTag("light");
      if (scene.isTagValid("light")) {
        depthMapScene.setEye(scene.node("light"));
        scene.node("light").toggleHint(Node.SHAPE | Node.AXES | Node.BOUNDS);
      }
    } else
      scene.tag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.shift();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (event.isShiftDown() && scene.isTagValid("light")) {
      depthShader.set("far", zFar += event.getCount() * 20);
      depthMapScene.setZFar(() -> zFar);
    }
    else
      scene.zoom(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ' && scene.isTagValid("light")) {
      depthMapScene.togglePerspective();
    }
    if (key == 'p')
      scene.togglePerspective();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.DepthMap"});
  }
}
