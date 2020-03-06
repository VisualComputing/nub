package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class ShadowMapping extends PApplet {
  Scene scene;
  Node[] shapes;
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
    scene.setRadius(max(w, h));
    scene.fit(1);
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(scene) {
        @Override
        public void graphics(PGraphics pg) {
          pg.pushStyle();
          if (scene.node("light") == this) {
            if (debug) {
              pg.fill(0, scene.hasTag(this) ? 255 : 0, 255, 120);
              Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
            }
            Scene.drawAxes(pg, 300);
          } else {
            pg.noStroke();
            pg.fill(255, 0, 0);
            pg.box(80);
          }
          pg.popStyle();
        }
      };
      shapes[i].randomize();
      // set picking precision to the pixels of the node projection
      shapes[i].setPickingThreshold(debug ? 0 : 20);
      //shapes[i].setHighlighting(Node.Highlighting.NONE);
    }
    scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
    scene.node("light").setMagnitude(shadowMapType == Graph.Type.ORTHOGRAPHIC ? 400f / 2048f : tan(fov / 2));
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
    if (scene.node("light") != null) {
      Node light = scene.node("light");
      light.setMagnitude(shadowMapType == Graph.Type.ORTHOGRAPHIC ? 400f / 2048f : tan(fov / 2));
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
    } else
      background(0);
    scene.render();
  }

  public void mouseMoved(MouseEvent event) {
    if (event.isShiftDown())
      scene.mouseTag("light");
    else
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

  public void keyPressed() {
    if (key == ' ') {
      shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
      Node light = scene.node("light");
      if (light != null)
        light.setMagnitude(shadowMapType == Graph.Type.ORTHOGRAPHIC ? 400f / 2048f : tan(fov / 2));
    }
    if (key == 'd') {
      debug = !debug;
      if (debug)
        resetShader();
      else
        shader(shadowShader);
      for (Node shape : shapes)
        shape.setPickingThreshold(debug ? 0 : 20);
    }
    if (key == 'p')
      scene.togglePerspective();
    if (key == '+')
      zFar += 20;
    if (key == '-')
      if (zFar - 20 > zNear + 60)
        zFar -= 20;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ShadowMapping"});
  }
}
