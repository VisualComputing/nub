package intellij;

import nub.core.*;
import nub.primitives.*;
import nub.processing.*;
import processing.core.*;
import processing.event.*;
import processing.opengl.*;

public class ShadowsImmediateMode extends PApplet {
  Scene scene;
  Node nodeLandscape, light;
  PShader depthShader;
  PShader shadowShader;
  PGraphics shadowMap;
  PMatrix3D pmatrix = new PMatrix3D();
  Matrix biasMatrix = new Matrix(
      0.5f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.5f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.5f, 0.0f,
      0.5f, 0.5f, 0.5f, 1.0f
  );
  boolean spotLight;
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
    scene.setRadius(max(w, h)/3);
    scene.fit(1);
    nodeLandscape = new Node(scene) {
      @Override
      public boolean graphics(PGraphics pg) {
        renderLandscape(pg);
        return true;
      }
    };
    nodeLandscape.setPickingThreshold(20);
    light = new Node(scene) {
      @Override
      public boolean graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(0,255,0);
        pg.box(5);
        Scene.drawAxes(pg, 200);
        pg.popStyle();
        return true;
      }
    };
    light.setPickingThreshold(20);
    light.setMagnitude(400f / 2048f);
    // initShadowPass
    depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth/depth_frag.glsl");
    shadowMap = createGraphics(2048, 2048, P3D);
    shadowMap.shader(depthShader);
    // TODO testing the appearance of artifacts fist
    //shadowMap.noSmooth();

    // initDefaultPass
    shadowShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/shadow/shadow_frag.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/shadow/shadow_vert.glsl");
    shader(shadowShader);
    noStroke();
  }

  public void draw() {
    // Calculate the light direction (actually scaled by negative distance)
    float lightAngle = frameCount * 0.002f;
    light.setPosition(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);
    light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), new Vector(0,1,0)));
    light.setZAxis(new Vector(light.position().x(), light.position().y(), light.position().z()));

    // 1. Fill in shadow map using the light point of view
    shadowMap.beginDraw();
    shadowMap.noStroke();
    shadowMap.background(0xffffffff); // Will set the depth to 1.0 (maximum depth)
    scene.render(shadowMap, spotLight ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC, light, zNear, zFar);
    shadowMap.endDraw();

    // 2. Render default pass
    Matrix projectionView = light.projectionView(spotLight ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC, shadowMap.width, shadowMap.height, zNear, zFar);
    Matrix lightMatrix = Matrix.multiply(biasMatrix, projectionView);
    // Apply the inverted modelview matrix from the default pass to get the original vertex
    // positions inside the shader. This is needed because Processing is pre-multiplying
    // the vertices by the modelview matrix (for better performance).
    // TODO (last step?) What a horrible detail! maybe reset shader comes handy!?
    Matrix modelviewInv = Scene.toMatrix(((PGraphicsOpenGL)g).modelviewInv);
    lightMatrix.apply(modelviewInv);
    // Convert Matrix to PMatrix and send it to the shader.
    // PShader.set(String, float[16]) doesn't work for some reason.
    pmatrix.set(lightMatrix.get(new float[16]));
    shadowShader.set("shadowTransform", pmatrix);
    Vector lightDirection = scene.eye().displacement(light.zAxis(false));
    shadowShader.set("lightDirection", lightDirection.x(), lightDirection.y(), lightDirection.z());
    // Send the shadowmap to the default shader
    shadowShader.set("shadowMap", shadowMap);
    scene.render();
  }

  public void renderLandscape(PGraphics canvas) {
    switch(landscape) {
      case 1: {
        float offset = -frameCount * 0.01f;
        canvas.fill(0xffff5500);
        for(int z = -5; z < 6; ++z)
          for(int x = -5; x < 6; ++x) {
            canvas.pushMatrix();
            canvas.translate(x * 12, sin(offset + x) * 20 + cos(offset + z) * 20, z * 12);
            canvas.box(10, 100, 10);
            canvas.popMatrix();
          }
      } break;
      case 2: {
        float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
        canvas.fill(0xffff5500);
        for(int n = 0; n < 20; ++n, angle += rotation) {
          canvas.pushMatrix();
          canvas.translate(sin(angle) * 70, cos(angle * 4) * 10, cos(angle) * 70);
          canvas.box(10, 100, 10);
          canvas.popMatrix();
        }
        canvas.fill(0xff0055ff);
        canvas.sphere(50);
      } break;
      case 3: {
        float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
        canvas.fill(0xffff5500);
        for(int n = 0; n < 20; ++n, angle += rotation) {
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
    if(key != CODED) {
      if(key >= '1' && key <= '3')
        landscape = key - '0';
      else if(key == ' ') {
        spotLight = !spotLight;
        if(spotLight)
          light.setMagnitude(tan(PI / 6));
        else
          light.setMagnitude(400f / 2048f);
      }
    }
  }

  public void mouseMoved(MouseEvent event) {
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
    }
    else
      scene.scale(event.getCount() * 20);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShadowsImmediateMode"});
  }
}