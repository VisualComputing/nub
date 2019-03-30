package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

// ported to nub from: https://forum.processing.org/two/discussion/12775/simple-shadow-mapping
public class ShadowsImmediateModeDebug extends PApplet {
  // TODO
  // 1. Try simplify the mvINV used within shadow_vert,
  // which requires to make the scene more compatible with nub (no transforms within graphics). See and then;
  // 2. If the eye to light 'clip-space' transform still is needed, then compute
  // the shader shadowTransform by software using the Node API, e.g.,
  // eye -> world: eye().worldMatrix(); and, world -> light -> 'clip-space' biasMatrix * light.projectionView
  Scene scene;
  Node nodeLandscape, light;
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
  int counter;
  boolean animate = true;
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
    nodeLandscape = new Node(scene) {
      @Override
      public boolean graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(0xffff5500);
        pg.box(10, 100, 10);
        pg.fill(0xff222222);
        pg.box(360, 5, 360);
        pg.fill(0xff00ff55);
        pg.sphere(10);
        pg.popStyle();
        return true;
      }
    };
    light = new Node(scene) {
      @Override
      public boolean graphics(PGraphics pg) {
        pg.pushStyle();
        if (debug) {
          pg.fill(0, scene.isTrackedNode(this) ? 255 : 0, 255, 120);
          Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
        }
        Scene.drawAxes(pg, 300);
        pg.pushStyle();
        return true;
      }
    };
    light.setMagnitude(400f / 2048f);
    // initShadowPass
    depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth/depth_frag.glsl");
    //depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth_alt/depth_nonlinear.glsl");
    shadowMap = createGraphics(2048, 2048, P3D);
    shadowMap.shader(depthShader);
    // TODO testing the appearance of artifacts first
    //shadowMap.noSmooth();

    // initDefaultPass
    shadowShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/shadow-debug/shadow_frag.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/shadow-debug/shadow_vert.glsl");
    shader(shadowShader);
    noStroke();
  }

  public void moveLight(int count) {
    // 1. Calculate the light position and orientation
    float lightAngle = count * 0.002f;
    light.setPosition(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);
    light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), new Vector(0, 1, 0)));
    light.setZAxis(new Vector(light.position().x(), light.position().y(), light.position().z()));
  }

  public void draw() {
    // 1. Calculate the light position and orientation
    if (animate)
      moveLight(counter++);

    // 2. Render the shadowmap from light node 'point-of-view'
    shadowMap.beginDraw();
    shadowMap.noStroke();
    shadowMap.background(0xffffffff); // Will set the depth to 1.0 (maximum depth)
    scene.render(shadowMap, shadowMapType, light, zNear, zFar);
    shadowMap.endDraw();

    // 3. Render the scene from the scene.eye() node
    background(0xff222222);
    if (!debug) {
      Matrix projectionView = light.projectionView(shadowMapType, shadowMap.width, shadowMap.height, zNear, zFar);
      Matrix lightMatrix = Matrix.multiply(biasMatrix, projectionView);
      Scene.setUniform(shadowShader, "lightMatrix", lightMatrix);
      // TODO: how to avoid calling g.modelviewInv?
      Scene.setUniform(shadowShader, "mvINV", Scene.toMatrix(((PGraphicsOpenGL) g).modelviewInv));

      // uncomment case 2a
      //Matrix shadowTransform = Matrix.multiply(lightMatrix, Scene.toMatrix(((PGraphicsOpenGL) g).modelviewInv));
      //Scene.setUniform(shadowShader, "shadowTransform", shadowTransform);

      Vector lightDirection = scene.eye().displacement(light.zAxis(false));
      Scene.setUniform(shadowShader, "lightDirection", lightDirection);
      shadowShader.set("shadowMap", shadowMap);
    }
    scene.render();
  }

  public void keyPressed() {
    if (key != CODED) {
      if (key >= '1' && key <= '3')
        landscape = key - '0';
      else if (key == ' ') {
        shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
        light.setMagnitude(shadowMapType == Graph.Type.ORTHOGRAPHIC ? 400f / 2048f : tan(fov / 2));
      } else if (key == 'd') {
        debug = !debug;
        if (debug)
          resetShader();
        else
          shader(shadowShader);
      } else if (key == 'a')
        animate = !animate;
    }
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

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShadowsImmediateModeDebug"});
  }
}