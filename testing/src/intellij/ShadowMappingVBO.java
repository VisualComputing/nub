package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class ShadowMappingVBO extends PApplet {
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  Scene scene;
  Node[] shapes;
  Node light;
  boolean debug = false;
  boolean shadows = true;
  PShader depthShader, shadowShader;
  PGraphics shadowMap;
  PMatrix3D pmatrix = new PMatrix3D();
  float zNear = 50;
  float zFar = 1000;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(max(w, h));
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(scene, caja());
      shapes[i].randomize();
      // TODO: fix picking (1) [there are SOME picking thresholds > 10, but at least one is 0]
      // To test/fix disable shadowMap rendering (fix picking (2))
      shapes[i].setPickingThreshold(50);
    }
    light = new Node(scene) {
      @Override
      public boolean graphics(PGraphics pg) {
        pg.pushStyle();
        Scene.drawAxes(pg, 150);
        pg.fill(isTracked() ? 255 : 25, isTracked() ? 0 : 255, 255);
        Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
        pg.popStyle();
        return true;
      }
    };
    light.setPickingThreshold(50);
    scene.setRadius(scene.radius() * 1.2f);
    scene.fit(1);

    if(debug) {
      shadowMap = createGraphics(w / 2, h / 2, P3D);
      depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth_alt/depth_linear.glsl");
      depthShader.set("near", zNear);
      depthShader.set("far", zFar);
    }
    else {
      shadowMap = createGraphics(w, h, P3D);
      depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth_alt/depth_nonlinear.glsl");
    }
    shadowMap.shader(depthShader);

    shadowShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/shadow1/shadowfrag.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/shadow1/shadowvert.glsl");
  }

  public void draw() {
    background(90, 80, 125);
    // /*
    // 1. Fill in shadow map using the light point of view
    // TODO: fix picking (2) [all picking thresholds > 10]
    shadowMap.beginDraw();
    shadowMap.background(120);
    scene.render(shadowMap, shadowMapType, light, zNear, zFar);
    shadowMap.endDraw();
    // /*
    // /*
    // 2. Fill in and display front-buffer
    if(shadows) {
      shader(shadowShader);
      Vector lightPosition = light.position();
      pointLight(255, 255, 255, lightPosition.x(), lightPosition.y(), lightPosition.z());
      Matrix lightMatrix = Matrix.multiply(light.projection(shadowMapType, width, height, zNear, zFar, scene.isLeftHanded()), light.view());
      pmatrix.set(lightMatrix.get(new float[16]));
      shadowShader.set("lightSpaceMatrix", pmatrix);
      shadowShader.set("shadowMap", shadowMap);
    }
    else
      resetShader();
    // */
    // */
    scene.render();
    /*
    // TODO: fix picking (debug both cases 1, and 2)
    stroke(255);
    for(Node node : scene.nodes())
      scene.drawShooterTarget(node);
    // */
    /*
    // 3. Display shadow map
    if (debug) {
      scene.beginHUD();
      image(shadowMap, w / 2, h / 2);
      scene.endHUD();
    }
    // */
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
    scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'f')
      scene.fitFOV(1);
    if (key == 'a')
      scene.fitFOV();
    if (key == '1')
      scene.setFOV(1);
    if (key == '3')
      scene.setFOV(PI / 3);
    if (key == '4')
      scene.setFOV(PI / 4);
    if (key == ' ') {
      shadows = !shadows;
      if(shadows)
        println("shadows activated!");
      else
        println("shadows de-activated");
    }
    if (key == 'o')
      if (shadowMapType == Graph.Type.ORTHOGRAPHIC)
        shadowMapType = Graph.Type.PERSPECTIVE;
      else
        shadowMapType = Graph.Type.ORTHOGRAPHIC;
    if (key == 't')
      scene.togglePerspective();
    if (key == 'p')
      scene.eye().position().print();
  }

  PShape caja() {
    PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
    caja.setStrokeWeight(3);
    caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    caja.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return caja;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShadowMappingVBO"});
  }
}
