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

public class ShadowsImmediateModeDebug extends PApplet {
  // TODO
  // 1. Make the scene more compatible with nub, then;
  // 2.
  // ported to nub from: https://forum.processing.org/two/discussion/12775/simple-shadow-mapping
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
        renderLandscape(pg);
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

  public void draw() {
    // 1. Calculate the light position and orientation
    float lightAngle = frameCount * 0.002f;
    light.setPosition(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);
    light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), new Vector(0, 1, 0)));
    light.setZAxis(new Vector(light.position().x(), light.position().y(), light.position().z()));

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
      //shadowShader.set("mvINV", ((PGraphicsOpenGL) g).modelviewInv);
      Scene.setUniform(shadowShader, "mvINV", Scene.toMatrix(((PGraphicsOpenGL) g).modelviewInv));
      // TODO: how to avoid calling g.modelviewInv?
      //Matrix shadowTransform = Matrix.multiply(lightMatrix, Scene.toMatrix(((PGraphicsOpenGL) g).modelviewInv));
      //Scene.setUniform(shadowShader, "shadowTransform", shadowTransform);
      Vector lightDirection = scene.eye().displacement(light.zAxis(false));
      Scene.setUniform(shadowShader, "lightDirection", lightDirection);
      shadowShader.set("shadowMap", shadowMap);
    }
    scene.render();
  }

  public void renderLandscape(PGraphics canvas) {
    switch (landscape) {
      case 1: {
        float offset = -frameCount * 0.01f;
        canvas.fill(0xffff5500);
        for (int z = -5; z < 6; ++z)
          for (int x = -5; x < 6; ++x) {
            canvas.pushMatrix();
            canvas.translate(x * 12, sin(offset + x) * 20 + cos(offset + z) * 20, z * 12);
            canvas.box(10, 100, 10);
            canvas.popMatrix();
          }
      }
      break;
      case 2: {
        float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
        canvas.fill(0xffff5500);
        for (int n = 0; n < 20; ++n, angle += rotation) {
          canvas.pushMatrix();
          canvas.translate(sin(angle) * 70, cos(angle * 4) * 10, cos(angle) * 70);
          canvas.box(10, 100, 10);
          canvas.popMatrix();
        }
        canvas.fill(0xff0055ff);
        canvas.sphere(50);
      }
      break;
      case 3: {
        float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
        canvas.fill(0xffff5500);
        for (int n = 0; n < 20; ++n, angle += rotation) {
          canvas.pushMatrix();
          canvas.translate(sin(angle) * 70, cos(angle) * 70, 0);
          canvas.box(10, 10, 100);
          canvas.popMatrix();
        }
        canvas.fill(0xff00ff55);
        canvas.sphere(50);
      }
    }
    canvas.fill(0xff222222);
    canvas.box(360, 5, 360);
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
      }
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