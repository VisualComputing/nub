package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class ShadowMappingImmediateMode extends PApplet {
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  Scene scene;
  Node[] shapes;
  Node light;
  PGraphics shadowMap;
  PMatrix3D pmatrix = new PMatrix3D();
  PShader depthShader, shadowShader;
  boolean shadows = true;
  float zNear = 50;
  float zFar = 1000;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.togglePerspective();
    scene.setRadius(max(w, h));
    scene.fit(1);
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(scene) {
        @Override
        public boolean graphics(PGraphics pg) {
          pg.pushStyle();
          if (scene.trackedNode("light") == this) {
            if(shadows) {
              Scene.drawAxes(pg, 150);
              stroke(255,0,0);
              scene.drawShooterTarget(this);
            }
            else {
              pg.fill(0, scene.isTrackedNode(this) ? 255 : 0, 255, 120);
              Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
            }
          } else {
            if (pg == shadowMap)
              pg.noStroke();
            else {
              pg.strokeWeight(3);
              pg.stroke(0, 255, 255);
            }
            pg.fill(255, 0, 0);
            pg.box(80);
          }
          pg.popStyle();
          return true;
        }
      };
      shapes[i].randomize();
      // set picking precision to the pixels of the node projection
      shapes[i].setPickingThreshold(shadows ? 50 : 0);
      shapes[i].setHighlighting(Node.Highlighting.NONE);
    }
    // Do w&h must match main context size? I think so
    shadowMap = createGraphics(w, h, P3D);
    depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth/depth_nonlinear.glsl");
    shadowMap.shader(depthShader);

    shadowShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/shadow/shadowfrag.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/shadow/shadowvert.glsl");

    light = shapes[(int) random(0, shapes.length - 1)];
    // this line is good to set a condition like: scene.trackedNode("light") == this
    // which is used to render the light node as a frustum volume above
    scene.setTrackedNode("light", light);
    light.setOrientation(new Quaternion(new Vector(0, 0, 1), light.position()));
    light.setMagnitude(0.6f);
  }

  public void draw() {
    background(75, 25, 15);
    // 1. Fill in shadow map using the light point of view
    shadowMap.beginDraw();
    shadowMap.background(140, 160, 125);
    scene.render(shadowMap, shadowMapType, light, zNear, zFar);
    shadowMap.endDraw();
    // 2. set shadow shader stuff
    // Weird but it seems I need to do this here
    resetShader();
    if(shadows) {
      shader(shadowShader);
      Vector lightPosition = light.position();
      pointLight(255, 255, 255, lightPosition.x(), lightPosition.y(), lightPosition.z());
      Matrix lightMatrix = Matrix.multiply(light.projection(shadowMapType, width, height, zNear, zFar, scene.isLeftHanded()), light.view());
      pmatrix.set(lightMatrix.get(new float[16]));
      shadowShader.set("lightSpaceMatrix", pmatrix);
      shadowShader.set("shadowMap", shadowMap);
    }
    // 3. Fill in and display front-buffer
    scene.render();
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

  public void keyPressed() {
    if (key == ' ' || key == 's') {
      shadows = !shadows;
      for(Node node : scene.nodes())
        node.setPickingThreshold(shadows ? 50 : 0);
      println("shadows " + (shadows ? "activated!" : "de-activated"));
    }
    if (key == 't')
      shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
    if (key == 'p')
      scene.togglePerspective();
    if(key == '+')
      zFar += 20;
    if(key == '-')
      if(zFar - 20 > zNear + 60)
        zFar -= 20;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShadowMappingImmediateMode"});
  }
}
