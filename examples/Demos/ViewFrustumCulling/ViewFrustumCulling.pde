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
 * Press any key to change the scene type: PERSPECTIVE or ORTHOGRAPHIC.
 */

import frames.input.*;
import frames.core.*;
import frames.primitives.*;
import frames.processing.*;

OctreeNode root;
Scene scene, auxScene;
PGraphics canvas, auxCanvas;

int w = 1000;
int h = 1000;

void settings() {
  size(w, h, P3D);
}

void setup() {
  // declare and build the octree hierarchy
  Vector p = new Vector(100, 70, 130);
  root = new OctreeNode(p, Vector.multiply(p, -1.0f));
  root.buildBoxHierarchy(4);

  canvas = createGraphics(w, h / 2, P3D);
  scene = new Scene(this, canvas);
  //scene.setType(Graph.Type.ORTHOGRAPHIC);
  scene.enableBoundaryEquations();
  OrbitShape eye = new OrbitShape(scene);
  scene.setEye(eye);
  scene.setDefaultNode(eye);
  scene.setFieldOfView(PI / 3);
  scene.fitBallInterpolation();

  auxCanvas = createGraphics(w, h / 2, P3D);
  // Note that we pass the upper left corner coordinates where the scene
  // is to be drawn (see drawing code below) to its constructor.
  auxScene = new Scene(this, auxCanvas, 0, h / 2);
  auxScene.setType(Graph.Type.ORTHOGRAPHIC);
  OrbitShape auxEye = new OrbitShape(auxScene);
  auxScene.setEye(auxEye);
  auxScene.setDefaultNode(auxEye);
  auxScene.setRadius(250);
  scene.setFieldOfView(PI / 3);
  auxScene.fitBall();
}

void draw() {
  background(0);
  scene.beginDraw();
  canvas.background(0);
  root.drawIfAllChildrenAreVisible(scene.frontBuffer(), scene);
  scene.endDraw();
  scene.display();

  auxScene.beginDraw();
  auxCanvas.background(0);
  root.drawIfAllChildrenAreVisible(auxScene.frontBuffer(), scene);
  auxScene.frontBuffer().pushStyle();
  auxScene.frontBuffer().stroke(255, 255, 0);
  auxScene.frontBuffer().fill(255, 255, 0, 160);
  auxScene.drawEye(scene);
  auxScene.frontBuffer().popStyle();
  auxScene.endDraw();
  auxScene.display();
}

void keyPressed() {
  scene.setType(scene.type() == Graph.Type.PERSPECTIVE ? Graph.Type.ORTHOGRAPHIC : Graph.Type.PERSPECTIVE);
}
