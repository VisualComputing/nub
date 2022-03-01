package examples;

import nub.core.Node;
import nub.core.Scene;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

import java.nio.file.Paths;

public class ShadowMap extends PApplet {
  Scene scene;
  Scene shadowMapScene;
  Node eye;
  Node[] shapes;
  PGraphics shadowMap;
  PShader depthShader;
  float zNear = 50;
  float zFar = 700;
  int w = 1400;
  int h = 1400;

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
    scene.radius = max(w, h);
    eye = new Node();
    eye.setWorldPosition(0,0,2000);
    scene.setEye(eye);
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(this::cube);
      scene.randomize(shapes[i]);
      shapes[i].setHighlight(0);
      shapes[i].setWorldMagnitude(1);
    }
    scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
    scene.node("light").toggleHint(Node.SHAPE | Node.AXES | Node.BOUNDS);
    scene.node("light").setWorldOrientation(Quaternion.from(Vector.plusK, scene.node("light").worldPosition()));
    // scene.enablePicking(false);
    shadowMapScene = new Scene(shadowMap);
    /*
    float fov = PI/3.0f;
    float cameraZ = (shadowMap.height/2.0f) / tan(fov/2.0f);
    shadowMap.perspective(fov, (float)shadowMap.width/(float)shadowMap.height,cameraZ/5.0f, cameraZ*5.0f);
    */
    shadowMapScene.setEye(scene.node("light"));
    shadowMapScene.picking = false;
    frameRate(1000);
    //shadowMap.ortho(-shadowMap.width/2, shadowMap.width/2, -shadowMap.height/2, shadowMap.height/2, zNear, zFar);
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

  float magic = 2.0f;

  public void draw() {
    // 1. Fill in and display front-buffer
    background(75, 25, 15);
    scene.render();
    // 2. Fill in shadow map using the light point of view
    if (scene.isTagValid("light")) {
      //println("1 " + ((PGraphicsOpenGL)shadowMap).projection.m33);
      //shadowMap.ortho(-1000, 1000, -1000, 1000, zNear, zFar);
      //shadowMap.ortho(-(float)shadowMap.width/magic, (float)shadowMap.width/magic, -(float)shadowMap.height/magic, (float)shadowMap.height/magic, zNear, zFar);
      //shadowMap.ortho();
      //shadowMap.perspective();
      float fov = PI/3.0f;
      float cameraZ = (height/2.0f) / tan(fov/2.0f);
      //shadowMap.perspective(fov, (float)width/(float)height, cameraZ/10.0f, cameraZ*10.0f);
      shadowMap.perspective(fov, (float)width/(float)height, 10, 600);
      shadowMapScene.openContext();
      // Warning: processing changes projection within pg.beginDraw
      //println("2 " + ((PGraphicsOpenGL)shadowMap).projection.m33);
      shadowMapScene.context().background(140, 160, 125);
      shadowMapScene.drawAxes();
      shadowMapScene.render();
      shadowMapScene.closeContext();
      shadowMapScene.image(w / 2, h / 2);
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
        shadowMapScene.setEye(scene.node("light"));
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
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (event.isShiftDown() && scene.isTagValid("light")) {
      depthShader.set("far", zFar += event.getCount() * 20);
      //shadowMap.ortho(-shadowMap.width/2, shadowMap.width/2, -shadowMap.height/2, shadowMap.height/2, zNear, zFar);
    } else
      scene.zoom(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == '+') {
      magic++;
    }
    if (key == '-' && magic > 0) {
      magic--;
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"examples.ShadowMap"});
  }
}