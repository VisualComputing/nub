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

Scene scene;
Node landscape1, landscape2, landscape3, floor, light;
TimingTask animation;
PShader depthShader;
PShader shadowShader;
PGraphics shadowMap;
float fov = THIRD_PI;
Matrix biasMatrix = new Matrix(
  0.5,   0,   0, 0,
    0, 0.5,   0, 0,
    0,   0, 0.5, 0,
  0.5, 0.5, 0.5, 1
  );
boolean debug;
Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
float zNear = 10;
float zFar = 600;
int w = 1000;
int h = 1000;

void settings() {
  size(w, h, P3D);
}

void setup() {
  scene = new Scene(this);
  scene.togglePerspective();
  scene.setRadius(max(w, h) / 3);
  scene.fit(1);
  landscape1 = new Node() {
    @Override
    public void graphics(PGraphics pg) {
      if (!isCulled()) {
        float offset = -frameCount * 0.01;
        pg.fill(0xffff5500);
        for (int z = -5; z < 6; ++z)
          for (int x = -5; x < 6; ++x) {
            pg.pushMatrix();
            pg.translate(x * 12, sin(offset + x) * 20 + cos(offset + z) * 20, z * 12);
            pg.box(10, 100, 10);
            pg.popMatrix();
          }
      }
    }
  };
  landscape2 = new Node() {
    @Override
    public void graphics(PGraphics pg) {
      if (!isCulled()) {
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
      }
    }
  };
  landscape2.cull();
  landscape3 = new Node() {
    @Override
    public void graphics(PGraphics pg) {
      if (!isCulled()) {
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
      }
    }
  };
  landscape3.cull();
  floor = new Node() {
    @Override
    public void graphics(PGraphics pg) {
      pg.fill(0xff222222);
      pg.box(360, 5, 360);
    }
  };
  floor.disableTagging();
  light = new Node() {
    @Override
    public void graphics(PGraphics pg) {
      pg.pushStyle();
      if (debug) {
        pg.fill(0, isTagged(scene) ? 255 : 0, 255);
        Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
      } else {
        pg.fill(0, 255, 255);
        Scene.drawCone(pg, 150.0, 60.0, 240.0);
      }
      Scene.drawAxes(pg, 300);
      pg.pushStyle();
    }
  };
  light.setMagnitude(0.195);

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

  // initShadowPass
  depthShader = loadShader("depth_frag.glsl");
  shadowMap = createGraphics(2048, 2048, P3D);
  shadowMap.shader(depthShader);
  // Testing the appearance of artifacts first
  //shadowMap.noSmooth();

  // initDefaultPass
  shadowShader = loadShader("shadow_frag.glsl", "shadow_vert.glsl");
  shader(shadowShader);
  noStroke();
}

void draw() {
  // 1. Render the shadowmap from light node 'point-of-view'
  shadowMap.beginDraw();
  shadowMap.noStroke();
  shadowMap.background(0xffffffff); // Will set the depth to 1.0 (maximum depth)
  Scene.render(shadowMap, shadowMapType, light, zNear, zFar);
  shadowMap.endDraw();

  // 2. Render the scene from the scene.eye() node
  background(0);
  if (!debug) {
    Matrix projectionView = Scene.projectionView(light, shadowMapType, shadowMap.width, shadowMap.height, zNear, zFar);
    Matrix lightMatrix = Matrix.multiply(biasMatrix, projectionView);
    Scene.setUniform(shadowShader, "shadowTransform", Matrix.multiply(lightMatrix, scene.eye().viewInverse()));
    Vector lightDirection = scene.eye().displacement(light.zAxis(false));
    Scene.setUniform(shadowShader, "lightDirection", lightDirection);
    shadowShader.set("shadowMap", shadowMap);
  }
  scene.render();
}

void keyPressed() {
  if (key == '1' || key == '2' || key == '3') {
    landscape1.cull(key != '1');
    landscape2.cull(key != '2');
    landscape3.cull(key != '3');
  } else if (key == ' ') {
    shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
    light.setMagnitude(shadowMapType == Graph.Type.ORTHOGRAPHIC ? 0.195 : tan(fov / 2));
  } else if (key == 'd') {
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
