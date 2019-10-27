/**
 * DepthMap.
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
    shapes[i] = new Node(scene) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        if (scene.trackedNode("light") == this) {
          Scene.drawAxes(pg, 150);
          pg.fill(0, scene.isTrackedNode(this) ? 255 : 0, 255, 120);
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
      @Override
      public void interact(Object... gesture) {
        if (gesture.length == 1)
          if (gesture[0] instanceof Integer)
            if (zFar + (Integer) gesture[0] > zNear) {
              zFar += (Integer) gesture[0];
              depthShader.set("far", zFar);
            }
      }
    };
    shapes[i].setPickingThreshold(0);
    shapes[i].randomize();
    // set picking precision to the pixels of the node projection
    shapes[i].setPickingThreshold(0);
    shapes[i].setHighlighting(0);
  }
  shadowMap = createGraphics(w / 2, h / 2, P3D);
  depthShader = loadShader("depth.glsl");
  depthShader.set("near", zNear);
  depthShader.set("far", zFar);
  shadowMap.shader(depthShader);

  scene.setTrackedNode("light", shapes[(int) random(0, shapes.length - 1)]);
  scene.trackedNode("light").setOrientation(new Quaternion(new Vector(0, 0, 1), scene.trackedNode("light").position()));
}

void draw() {
  background(75, 25, 15);
  // 1. Fill in and display front-buffer
  scene.render();
  // 2. Fill in shadow map using the light point of view
  if (scene.trackedNode("light") != null) {
    shadowMap.beginDraw();
    shadowMap.background(140, 160, 125);
    scene.render(shadowMap, shadowMapType, scene.trackedNode("light"), zNear, zFar);
    shadowMap.endDraw();
    // 3. Display shadow map
    scene.beginHUD();
    image(shadowMap, w / 2, h / 2);
    scene.endHUD();
  }
}

void mouseMoved(MouseEvent event) {
  if (event.isShiftDown())
    scene.cast("light");
  else
    scene.cast();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.translate();
  else
    scene.moveForward(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if (event.isShiftDown())
    // application control of the light: setting the light zFar plane
    // is implemented as a custom behavior by node.interact()
    scene.interact("light", event.getCount() * 20);
  else
    scene.scale(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ')
    shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
  if (key == 'p')
    scene.togglePerspective();
}
