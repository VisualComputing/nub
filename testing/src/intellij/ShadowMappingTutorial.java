package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class ShadowMappingTutorial extends PApplet {
  // ported to nub from: https://forum.processing.org/two/discussion/12775/simple-shadow-mapping
  Scene scene;
  Node[] shapes;
  Node floor, light;
  PShader depthShader;
  PShader shadowShader;
  PGraphics shadowMap;
  boolean immediate;
  float fov = THIRD_PI;
  Matrix biasMatrix = new Matrix(
      0.5f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.5f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.5f, 0.0f,
      0.5f, 0.5f, 0.5f, 1.0f
  );
  boolean debug;
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  float zNear = 10;
  float zFar = 1000;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.togglePerspective();
    scene.setRadius(max(w, h) / 3);
    scene.fit(1);
    shapes = new Node[50];
    for (int i = 0; i < shapes.length; i++) {
      tint(random(0, 255), random(0, 255), random(0, 255), random(150, 255));
      shapes[i] = new Node(scene, loadShape("/home/pierre/IdeaProjects/nubjs/testing/data/interaction/rocket.obj")) {
        @Override
        public void graphics(PGraphics pg) {
          pg.pushStyle();
          pg.fill(255, 255, 0);
          Scene.drawTorusSolenoid(pg);
          pg.pushStyle();
        }
      };
      scene.randomize(shapes[i]);
      shapes[i].setPickingThreshold(0);
      shapes[i].scale(0.2f);
    }
    light = new Node(scene) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        if (debug) {
          pg.fill(0, scene.isTrackedNode(this) ? 255 : 0, 255, 120);
          Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
        }
        Scene.drawAxes(pg, 500);
        pg.popStyle();
      }
    };
    light.setPickingThreshold(0);
    light.setMagnitude(400f / 2048f);
    light.setPosition(0, 160, 160);
    light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), new Vector(0, 1, 0)));
    light.setZAxis(new Vector(light.position().x(), light.position().y(), light.position().z()));

    PShape box = createShape(BOX, 360, 5, 360);
    //rectMode(CENTER);
    //PShape box = createShape(RECT, 0, 0, 360, 360);
    box.setFill(0xff222222);
    box.setStroke(false);
    floor = new Node(scene);
    floor.setShape(box);
    // initShadowPass
    depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth/depth_frag.glsl");
    //depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth_alt/depth_nonlinear.glsl");
    shadowMap = createGraphics(2048, 2048, P3D);
    shadowMap.shader(depthShader);
    // TODO testing the appearance of artifacts first
    //shadowMap.noSmooth();

    // initDefaultPass
    shadowShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/shadow/shadow_frag.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/shadow/shadow_vert.glsl");
    shader(shadowShader);
    noStroke();
  }

  public void draw() {
    // 1. Render the shadowmap from light node 'point-of-view'
    shadowMap.beginDraw();
    shadowMap.noStroke();
    shadowMap.background(0xffffffff); // Will set the depth to 1.0 (maximum depth)
    scene.render(shadowMap, shadowMapType, light, zNear, zFar);
    shadowMap.endDraw();

    // 2. Render the scene from the scene.eye() node
    background(0xff222222);
    if (!debug) {
      Matrix projectionView = light.projectionView(shadowMapType, shadowMap.width, shadowMap.height, zNear, zFar);
      Matrix lightMatrix = Matrix.multiply(biasMatrix, projectionView);
      Scene.setUniform(shadowShader, "shadowTransform", Matrix.multiply(lightMatrix, Matrix.inverse(scene.view())));
      Vector lightDirection = scene.eye().displacement(light.zAxis(false));
      Scene.setUniform(shadowShader, "lightDirection", lightDirection);
      shadowShader.set("shadowMap", shadowMap);
    }
    scene.render();
  }

  public void keyPressed() {
    if (key == ' ') {
      shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
      light.setMagnitude(shadowMapType == Graph.Type.ORTHOGRAPHIC ? 400f / 2048f : tan(fov / 2));
    }
    if (key == 'i') {
      immediate = !immediate;
      for (Node node : shapes)
        if (immediate)
          node.resetShape();
        else
          node.setShape(loadShape("/home/pierre/IdeaProjects/nubjs/testing/data/interaction/rocket.obj"));
    }
    if (key == 'd') {
      debug = !debug;
      if (debug)
        resetShader();
      else
        shader(shadowShader);
    }
  }

  public void mouseMoved() {
    scene.cast();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (event.isShiftDown()) {
      int shift = event.getCount() * 20;
      if (zFar + shift > zNear)
        zFar += shift;
    } else
      scene.scale(event.getCount() * 20);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ShadowMappingTutorial"});
  }
}