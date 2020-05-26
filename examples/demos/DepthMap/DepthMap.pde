/**
 * Depth Map.
 * by Jean Pierre Charalambos.
 *
 * This example shows how to generate and display a depth map from a light point-of-view.
 *
 * The actual shadow mapping implementation is left as an exercise. Readers may refer to:
 * http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-16-shadow-mapping/ and,
 * https://learnopengl.com/Advanced-Lighting/Shadows/Shadow-Mapping
 *
 * Press shift and pick (mouse move) a box to define it as the light source.
 * Press shift and scroll (mouse wheel) the light source to change the shadow map zFar plane.
 * Press ' ' to change the shadow map volume (ORTHOGRAPHIC / PERSPECTIVE).
 * Press 'p' to toggle the scene perspective.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
Scene scene;
Node[] shapes;
PGraphics shadowMap;
PShader depthShader;
float zNear = 50;
float zFar = 1000;
int w = 1000;
int h = 1000;

void settings() {
  size(w, h, P3D);
}

void setup() {
  scene = new Scene(this);
  scene.setRadius(max(w, h));
  scene.fit(1);
  shapes = new Node[20];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        if (scene.node("light") == this) {
          Scene.drawAxes(pg, 150);
          pg.fill(0, isTagged(scene) ? 255 : 0, 255, 120);
          Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
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
      }
    };
    scene.randomize(shapes[i]);
    shapes[i].setHighlighting(0);
  }
  shadowMap = createGraphics(w / 2, h / 2, P3D);
  depthShader = loadShader("depth.glsl");
  depthShader.set("near", zNear);
  depthShader.set("far", zFar);
  shadowMap.shader(depthShader);

  scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
  scene.node("light").setOrientation(Quaternion.from(Vector.plusK, scene.node("light").position()));
}

void draw() {
  background(75, 25, 15);
  // 1. Fill in and display front-buffer
  scene.render();
  // 2. Fill in shadow map using the light point of view
  if (scene.isTagValid("light")) {
    shadowMap.beginDraw();
    shadowMap.background(140, 160, 125);
    Scene.render(shadowMap, shadowMapType, scene.node("light"), zNear, zFar);
    shadowMap.endDraw();
    // 3. Display shadow map
    scene.beginHUD();
    image(shadowMap, w / 2, h / 2);
    scene.endHUD();
  }
}

void mouseMoved(MouseEvent event) {
  if (event.isShiftDown())
    scene.mouseTag("light");
  else
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
  if (event.isShiftDown() && scene.isTagValid("light"))
    depthShader.set("far", zFar += event.getCount() * 20);
  else
    scene.scale(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ')
    shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
  if (key == 'p')
    scene.togglePerspective();
}
