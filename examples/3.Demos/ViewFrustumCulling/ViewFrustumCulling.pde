/**
 * View Frustum Culling.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates a basic view frustum culling implementation which is performed
 * by analytically solving the frustum plane equations.
 *
 * A hierarchical octree structure is clipped against the camera's frustum clipping planes.
 * A second viewer displays an external view of the scene that exhibits the clipping
 * (using scene.drawEye(Scene otherScene) to display the frustum).
 *
 * Press the space-bar to change the scene type: PERSPECTIVE or ORTHOGRAPHIC.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

OctreeNode root;
Scene scene1, scene2, focus;
PGraphics canvas1, canvas2;

//Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
String renderer = P3D;
int w = 1110;
int h = 1110;

void settings() {
  size(w, h, renderer);
}

void setup() {
  // declare and build the octree hierarchy
  Vector p = new Vector(100, 70, 130);
  root = new OctreeNode(p, Vector.multiply(p, -1.0f));
  root.buildBoxHierarchy(4);

  canvas1 = createGraphics(w, h / 2, P3D);
  scene1 = new Scene(this, canvas1);
  scene1.setType(Graph.Type.ORTHOGRAPHIC);
  scene1.enableBoundaryEquations();
  scene1.setFieldOfView(PI / 3);
  scene1.fitBallInterpolation();

  canvas2 = createGraphics(w, h / 2, P3D);
  // Note that we pass the upper left corner coordinates where the scene
  // is to be drawn (see drawing code below) to its constructor.
  scene2 = new Scene(this, canvas2, 0, h / 2);
  scene2.setType(Graph.Type.ORTHOGRAPHIC);
  scene2.setRadius(200);
  scene1.setFieldOfView(PI / 3);
  scene2.fitBall();
}

void draw() {
  handleMouse();
  background(0);
  scene1.beginDraw();
  canvas1.background(0);
  root.drawIfAllChildrenAreVisible(scene1.frontBuffer(), scene1);
  scene1.endDraw();
  scene1.display();

  scene2.beginDraw();
  canvas2.background(0);
  root.drawIfAllChildrenAreVisible(scene2.frontBuffer(), scene1);
  scene2.frontBuffer().pushStyle();
  scene2.frontBuffer().stroke(255, 255, 0);
  scene2.frontBuffer().fill(255, 255, 0, 160);
  scene2.drawEye(scene1);
  scene2.frontBuffer().popStyle();
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
  focus.zoom(event.getCount() * 50);
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
    if (focus.type() == Graph.Type.PERSPECTIVE)
      focus.setType(Graph.Type.ORTHOGRAPHIC);
    else
      focus.setType(Graph.Type.PERSPECTIVE);
}

void handleMouse() {
  focus = mouseY < h / 2 ? scene1 : scene2;
}
