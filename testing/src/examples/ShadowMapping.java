package examples;

import nub.core.Node;
import nub.core.Scene;
import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

import java.nio.file.Paths;

public class ShadowMapping extends PApplet {
  Node eye;
  Scene scene, shadowMapScene;
  Node landscape1, landscape2, landscape3, light;
  PShader depthShader;
  PShader shadowShader;
  PGraphics shadowMap;
  float fov = THIRD_PI;
  Matrix biasMatrix = new Matrix(
          0.5f, 0, 0, 0,
          0, 0.5f, 0, 0,
          0, 0, 0.5f, 0,
          0.5f, 0.5f, 0.5f, 1
  );
  boolean debug;
  //Scene.Type shadowMapType = Scene.Type.ORTHOGRAPHIC;
  float zNear = 10;
  float zFar = 600;
  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.radius = max(w, h) / 3;
    eye = new Node();
    eye.setWorldPosition(0,0,800);
    scene.setEye(eye);
    //scene.togglePerspective();
    Node floor = new Node((PGraphics pg) -> {
      pg.pushStyle();
      pg.noStroke();
      pg.fill(0xff222222);
      pg.box(360, 5, 360);
      pg.popStyle();
    });
    floor.tagging = false;
    landscape1 = new Node(this::landscape1);
    landscape2 = new Node(this::landscape2);
    landscape2.cull = true;
    landscape3 = new Node(this::landscape3);
    landscape3.cull = true;
    // initShadowPass
    depthShader = loadShader(Paths.get("testing/data/depth/depth_frag.glsl").toAbsolutePath().toString());

    shadowMap = createGraphics(2048, 2048, P3D);
    shadowMap.shader(depthShader);
    // Testing the appearance of artifacts first
    //shadowMap.noSmooth();
    // initDefaultPass
    shadowShader = loadShader(Paths.get("testing/data/shadow/shadow_frag.glsl").toAbsolutePath().toString(), Paths.get("testing/data/shadow/shadow_vert.glsl").toAbsolutePath().toString());
    shader(shadowShader);
    // light node
    light = new Node();
    light.enableHint(Node.BULLSEYE | Node.AXES | Node.CAMERA);
    // shadow map scene
    shadowMapScene = new Scene(shadowMap);
    shadowMapScene.setEye(light);
    /*
    shadowMapScene.setZNear(() -> 10f);
    shadowMapScene.setZFar(() -> 600f);
    shadowMapScene.setType(Graph.Type.ORTHOGRAPHIC);
    // */
    shadowMapScene.picking = false;
  }

  void landscape1(PGraphics pg) {
    pg.pushStyle();
    pg.noStroke();
    float offset = -frameCount * 0.01f;
    pg.fill(0xffff5500);
    for (int z = -5; z < 6; ++z)
      for (int x = -5; x < 6; ++x) {
        pg.pushMatrix();
        pg.translate(x * 12, sin(offset + x) * 20 + cos(offset + z) * 20, z * 12);
        pg.box(10, 100, 10);
        pg.popMatrix();
      }
    pg.popStyle();
  }

  void landscape2(PGraphics pg) {
    pg.pushStyle();
    pg.noStroke();
    float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
    pg.fill(0xffff5500);
    for (int n = 0; n < 20; ++n, angle += rotation) {
      pg.pushMatrix();
      pg.translate(sin(angle) * 70, cos(angle * 4) * 10, cos(angle) * 70);
      pg.box(10, 100, 10);
      pg.popMatrix();
    }
    pg.fill(0xff0055ff);
    pg.sphere(50);
    pg.popStyle();
  }

  void landscape3(PGraphics pg) {
    pg.pushStyle();
    pg.noStroke();
    float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
    pg.fill(0xffff5500);
    for (int n = 0; n < 20; ++n, angle += rotation) {
      pg.pushMatrix();
      pg.translate(sin(angle) * 70, cos(angle) * 70, 0);
      pg.box(10, 10, 100);
      pg.popMatrix();
    }
    pg.fill(0xff00ff55);
    pg.sphere(50);
    pg.popStyle();
  }

  public void draw() {
    // 0. animate
    animate();
    // 1. Render the shadowmap
    shadowMapScene.openContext();
    shadowMapScene.context().background(0xffffffff);
    shadowMapScene.render();
    shadowMapScene.closeContext();
    // 2. Display the scene
    if (!debug) {
      Matrix lightMatrix = Matrix.multiply(biasMatrix, shadowMapScene.projectionView());
      Scene.setUniform(shadowShader, "shadowTransform", Matrix.multiply(lightMatrix, eye.viewInverse()));
      Vector lightDirection = eye.displacement(light.zAxis(false));
      Scene.setUniform(shadowShader, "lightDirection", lightDirection);
      shadowShader.set("shadowMap", shadowMap);
    }
    background(0);
    scene.render();
  }

  void animate() {
    if (!scene.isTagged(light)) {
      float lightAngle = frameCount * 0.002f;
      light.setPosition(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);
    }
    light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), Vector.plusJ));
    light.setZAxis(light.position());
  }

  public void keyPressed() {
    if (key == '1' || key == '2' || key == '3') {
      landscape1.cull = key != '1';
      landscape2.cull = key != '2';
      landscape3.cull = key != '3';
    }
    /*
    if (key == ' ') {
      if (shadowMapScene.type() == Graph.Type.PERSPECTIVE) {
        shadowMapScene.setType(Graph.Type.ORTHOGRAPHIC);
        light.setMagnitude(1);
      } else {
        shadowMapScene.setType(Graph.Type.PERSPECTIVE);
        light.setMagnitude(tan(fov / 2));
      }
    }
    */
    if (key == 'd') {
      light.toggleHint(Node.BULLSEYE | Node.AXES | Node.CAMERA | Node.BOUNDS);
      debug = !debug;
      if (debug)
        resetShader();
      else
        shader(shadowShader);
    }
    if (key == 'c') {
      light.toggleHint(Node.CAMERA);
    }
  }

  public void mouseMoved() {
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
    if (event.isShiftDown()) {
      int shift = event.getCount() * 20;
      if (zFar + shift > zNear)
        zFar += shift;
    } else
      scene.zoom(event.getCount() * 20);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"examples.ShadowMapping"});
  }
}