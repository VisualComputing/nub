/**
 * Shadow Mapping.
 * by Jean Pierre Charalambos.
 *
 * Shadow mapping technique ported to nub from Poersch's:
 * https://forum.processing.org/two/discussion/12775/simple-shadow-mapping
 * See also:
 * 1. http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-16-shadow-mapping/
 * 2. https://learnopengl.com/Advanced-Lighting/Shadows/Shadow-Mapping
 *
 * Press 1, 2 and 3 to display the different landscapes.
 * Press ' ' to change the shadow volume from orthographic to perspective.
 * Press 'd' to toggle visual debugging mode.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene, shadowMapScene;
Node landscape1, landscape2, landscape3, light;
TimingTask animation;
PShader depthShader;
PShader shadowShader;
PGraphics shadowMap;
float fov = THIRD_PI;
Matrix biasMatrix = new Matrix(
  0.5, 0, 0, 0,
  0, 0.5, 0, 0,
  0, 0, 0.5, 0,
  0.5, 0.5, 0.5, 1
  );
boolean debug;
Scene.Type shadowMapType = Scene.Type.ORTHOGRAPHIC;
float zNear = 10;
float zFar = 600;
int w = 1200;
int h = 1200;

void settings() {
  size(w, h, P3D);
}

void setup() {
  scene = new Scene(this, max(w, h) / 3);
  scene.enableHint(Scene.BACKGROUND, color(0));
  scene.togglePerspective();
  scene.fit(1);
  Node floor = new Node() {
    @Override
      public void graphics(PGraphics pg) {
      pg.pushStyle();
      pg.noStroke();
      pg.fill(0xff222222);
      pg.box(360, 5, 360);
      pg.popStyle();
    }
  };
  floor.tagging = false;
  landscape1 = new Node() {
    @Override
    public void graphics(PGraphics pg) {
      pg.pushStyle();
      pg.noStroke();
      float offset = -frameCount * 0.01;
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
  };
  landscape2 = new Node() {
    @Override
    public void graphics(PGraphics pg) {
      pg.pushStyle();
      pg.noStroke();
      float angle = -frameCount * 0.0015, rotation = TWO_PI / 20;
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
  };
  landscape2.cull = true;
  landscape3 = new Node() {
    @Override
    public void graphics(PGraphics pg) {
      pg.pushStyle();
      pg.noStroke();
      float angle = -frameCount * 0.0015, rotation = TWO_PI / 20;
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
  };
  landscape3.cull = true;
  // initShadowPass
  depthShader = loadShader("depth_frag.glsl");
  shadowMap = createGraphics(2048, 2048, P3D);
  shadowMap.shader(depthShader);
  // Testing the appearance of artifacts first
  //shadowMap.noSmooth();
  // initDefaultPass
  shadowShader = loadShader("shadow_frag.glsl", "shadow_vert.glsl");
  shader(shadowShader);
  // light node
  light = new Node();
  light.enableHint(Node.BULLSEYE | Node.AXES | Node.CAMERA);
  light.configHint(Node.BOUNDS, shadowMap, shadowMapType, zNear, zFar);
  animation = new TimingTask() {
    @Override
    public void execute() {
      if (!scene.isTagged(light)) {
        float lightAngle = frameCount * 0.002;
        light.setPosition(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);
      }
      light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), Vector.plusJ));
      light.setZAxis(light.position());
    }
  };
  animation.run(60);
  // shadow map scene
  shadowMapScene = new Scene(shadowMap, light, 10, 600);
  shadowMapScene.setType(Graph.Type.ORTHOGRAPHIC);
  shadowMapScene.enableHint(Scene.BACKGROUND, 0xffffffff);
  shadowMapScene.picking = false;
}

void draw() {
  // 1. Render the shadowmap
  shadowMapScene.render();
  // 2. Display the scene
  if (!debug) {
    Matrix lightMatrix = Matrix.multiply(biasMatrix, shadowMapScene.projectionView());
    Scene.setUniform(shadowShader, "shadowTransform", Matrix.multiply(lightMatrix, scene.eye().viewInverse()));
    Vector lightDirection = scene.eye().displacement(light.zAxis(false));
    Scene.setUniform(shadowShader, "lightDirection", lightDirection);
    shadowShader.set("shadowMap", shadowMap);
  }
  scene.display();
}

void keyPressed() {
  if (key == '1' || key == '2' || key == '3') {
    landscape1.cull = key != '1';
    landscape2.cull = key != '2';
    landscape3.cull = key != '3';
  } else if (key == ' ') {
    if (shadowMapScene.type() == Graph.Type.PERSPECTIVE) {
      shadowMapScene.setType(Graph.Type.ORTHOGRAPHIC);
      light.setMagnitude(1);
    } else {
      shadowMapScene.setType(Graph.Type.PERSPECTIVE);
      light.setMagnitude(tan(fov / 2));
    }
  } else if (key == 'd') {
    light.toggleHint(Node.BULLSEYE | Node.AXES | Node.CAMERA | Node.BOUNDS);
    debug = !debug;
    if (debug)
      resetShader();
    else
      shader(shadowShader);
  }
}

void mouseMoved() {
  scene.mouseTag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpin();
  else if (mouseButton == RIGHT)
    scene.mouseTranslate();
  else
    scene.moveForward(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if (event.isShiftDown()) {
    int shift = event.getCount() * 20;
    if (zFar + shift > zNear)
      zFar += shift;
  } else
    scene.scale(event.getCount() * 20);
}
