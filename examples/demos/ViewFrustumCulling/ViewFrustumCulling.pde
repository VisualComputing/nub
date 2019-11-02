/**
 * View Frustum Culling.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates a basic view frustum culling implementation which is performed
 * by analytically solving the frustum plane equations.
 *
 * A hierarchical octree structure is clipped against the camera's frustum clipping planes.
 * A second viewer displays an external view of the scene that exhibits the clipping
 * (using scene.drawFrustum(Scene otherScene) to display the frustum).
 *
 * Press the space-bar to change the scene type: PERSPECTIVE or ORTHOGRAPHIC.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

OctreeNode root;
Scene scene1, scene2, focus;

int w = 1110;
int h = 1110;

void settings() {
  size(w, h, P3D);
}

void setup() {
  // declare and build the octree hierarchy
  Vector p = new Vector(100, 70, 130);
  root = new OctreeNode(p, Vector.multiply(p, -1));
  root.buildBoxHierarchy(4);

  scene1 = new Scene(this, P3D, w, h /2);
  scene1.togglePerspective();
  scene1.enableBoundaryEquations();
  scene1.fit(1);

  // Note that we pass the upper left corner coordinates where the scene
  // is to be drawn (see drawing code below) to its constructor.
  scene2 = new Scene(this, P3D, w, h / 2, 0, h / 2);
  scene2.togglePerspective();
  scene2.setRadius(200);
  scene2.fit();
}

void draw() {
  handleMouse();
  background(0);
  scene1.beginDraw();
  scene1.context().background(0);
  root.drawIfAllChildrenAreVisible(scene1.context(), scene1);
  scene1.endDraw();
  scene1.display();

  scene2.beginDraw();
  scene2.context().background(0);
  root.drawIfAllChildrenAreVisible(scene2.context(), scene1);
  scene2.context().pushStyle();
  scene2.context().stroke(255, 255, 0);
  scene2.context().fill(255, 255, 0, 160);
  scene2.drawFrustum(scene1);
  scene2.context().popStyle();
  scene2.endDraw();
  scene2.display();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    focus.spin();
  else if (mouseButton == RIGHT)
    focus.translate();
  else
    focus.scale(focus.mouseDX());
}

void mouseWheel(MouseEvent event) {
  focus.moveForward(event.getCount() * 50);
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 2)
    if (event.getButton() == LEFT)
      focus.focus();
    else
      focus.align();
}

void keyPressed() {
  if (key == ' ')
    focus.togglePerspective();
  if (key == 'f') {
    scene1.flip();
    scene2.flip();
  }
}

void handleMouse() {
  focus = mouseY < h / 2 ? scene1 : scene2;
}
