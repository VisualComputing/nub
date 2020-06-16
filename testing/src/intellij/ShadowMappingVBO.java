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

public class ShadowMappingVBO extends PApplet {
  // ported to nub from: https://forum.processing.org/two/discussion/12775/simple-shadow-mapping
  Scene scene;
  Node[] shapes;
  Node floor, light;
  PShader depthShader;
  PShader shadowShader;
  PGraphics shadowMap;
  float fov = THIRD_PI;
  Matrix biasMatrix = new Matrix(
      0.5f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.5f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.5f, 0.0f,
      0.5f, 0.5f, 0.5f, 1.0f
  );
  boolean debug;
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  int landscape = 1;
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
      shapes[i] = new Node(loadShape("/home/pierre/IdeaProjects/nub/testing/data/interaction/rocket.obj"));
      scene.randomize(shapes[i]);
      shapes[i].setPickingPolicy(Node.PickingPolicy.PRECISE);
      shapes[i].scale(0.2f);
    }
    light = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        if (debug) {
          pg.fill(0, scene.hasTag(this) ? 255 : 0, 255, 120);
          Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
        }
        Scene.drawAxes(pg, 300);
        pg.noStroke();
        pg.fill(0, 255, 0);
        Scene.drawCone(pg, 100f, 10, 100);
        pg.pushStyle();
      }
    };
    light.setMagnitude(400f / 2048f);
    light.setPosition(0, 160, 160);
    light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), new Vector(0, 1, 0)));
    light.setZAxis(new Vector(light.position().x(), light.position().y(), light.position().z()));
    light.setBullsEyeSize(50);
    light.configHint(Node.BULLSEYE, Node.BullsEyeShape.CIRCLE);

    PShape box = createShape(BOX, 360, 5, 360);
    //rectMode(CENTER);
    //PShape box = createShape(RECT, 0, 0, 360, 360);
    box.setFill(0xff222222);
    box.setStroke(false);
    floor = new Node();
    floor.setShape(box);

    // initShadowPass
    depthShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/depth/depth_frag.glsl");
    //depthShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/depth_alt/depth_nonlinear.glsl");
    shadowMap = createGraphics(2048, 2048, P3D);
    shadowMap.shader(depthShader);
    // TODO testing the appearance of artifacts first
    //shadowMap.noSmooth();

    // initDefaultPass
    shadowShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/shadow/shadow_frag.glsl", "/home/pierre/IdeaProjects/nub/testing/data/shadow/shadow_vert.glsl");
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
      Matrix projectionView = scene.projectionView(light, shadowMapType, shadowMap.width, shadowMap.height, zNear, zFar);
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
    if (key == 'd') {
      debug = !debug;
      if (debug)
        resetShader();
      else
        shader(shadowShader);
    }
  }

  public void mouseMoved() {
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
      int shift = event.getCount() * 20;
      if (zFar + shift > zNear)
        zFar += shift;
    } else
      scene.scale(event.getCount() * 20);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ShadowMappingVBO"});
  }
}
