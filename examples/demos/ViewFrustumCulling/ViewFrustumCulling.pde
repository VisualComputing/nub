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
Scene mainScene, secondaryScene, focus;

int w = 1000;
int h = 800;
//octree
float a = 100, b = 70, c = 130;
int levels = 4;

void settings() {
  size(w, h, P3D);
}

void setup() {
  // main scene
  mainScene = new Scene(this, P3D, w, h /2);
  mainScene.togglePerspective();
  mainScene.enableBoundaryEquations();
  mainScene.fit(1);

  // declare and build the octree hierarchy
  root = new OctreeNode(mainScene);
  buildBoxHierarchy(root);

  // secondary scene
  secondaryScene = new Scene(this, P3D, w, h / 2, 0, h / 2);
  secondaryScene.togglePerspective();
  secondaryScene.setRadius(200);
  secondaryScene.fit();
}

void buildBoxHierarchy(OctreeNode parent) {
  if (parent.level() < levels)
    for (int i = 0; i < 8; ++i)
      buildBoxHierarchy(new OctreeNode(parent, new Vector((i & 4) == 0 ? a : -a, (i & 2) == 0 ? b : -b, (i & 1) == 0 ? c : -c)));
}

void draw() {
  root.cull(false);
  handleMouse();
  background(255);
  mainScene.beginDraw();
  mainScene.context().background(255);
  mainScene.context().noFill();
  mainScene.context().box(a, b, c);
  mainScene.render();
  mainScene.endDraw();
  mainScene.display();

  mainScene.shift(secondaryScene);
  secondaryScene.beginDraw();
  secondaryScene.context().background(185);
  secondaryScene.render();
  secondaryScene.context().pushStyle();
  secondaryScene.context().strokeWeight(2);
  secondaryScene.context().stroke(255, 0, 255);
  secondaryScene.context().fill(255, 0, 255, 160);
  secondaryScene.drawFrustum(mainScene);
  secondaryScene.context().popStyle();
  secondaryScene.endDraw();
  secondaryScene.display();
  secondaryScene.shift(mainScene);
}

void handleMouse() {
  focus = mouseY < h / 2 ? mainScene : secondaryScene;
}

void mouseDragged() {
  if (mouseButton == LEFT)
    focus.mouseSpinEye();
  else if (mouseButton == RIGHT)
    focus.mouseTranslateEye();
  else
    focus.scaleEye(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  focus.moveForward(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 2)
    if (event.getButton() == LEFT)
      focus.focusEye();
    else
      focus.alignEye();
}

void keyPressed() {
  if (key == ' ')
    focus.togglePerspective();
  if (key == 'f') {
    mainScene.flip();
    secondaryScene.flip();
  }
}
