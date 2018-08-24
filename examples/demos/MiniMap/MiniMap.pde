/**
 * Mini Map
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates how to use off-screen rendering to build
 * a mini-map of the main Scene where all objects are interactive. It
 * also shows Frame syncing among views.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

Scene scene, minimap, focus;
PGraphics sceneCanvas, minimapCanvas;
Shape torus1, torus2, minimapTorus1, minimapTorus2, eye;

int w = 1800;
int h = 1200;
int oW = w / 3;
int oH = h / 3;
int oX = w - oW;
int oY = h - oH;
boolean showMiniMap = true;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

void setup() {
  size(1800, 1200, renderer);
  sceneCanvas = createGraphics(w, h, renderer);
  scene = new Scene(this, sceneCanvas);
  torus1 = new Torus(scene, color(255, 0, 0));
  torus1.translate(-30, -30);
  torus2 = new Torus(torus1, color(0, 0, 255));
  torus2.translate(80, 0);
  scene.setFieldOfView((float) Math.PI / 3);
  scene.setRadius(150);
  scene.fitBall();

  minimapCanvas = createGraphics(oW, oH, renderer);
  minimap = new Scene(this, minimapCanvas, oX, oY);
  minimapTorus1 = new Torus(minimap, color(255, 0, 0));
  minimapTorus1.translate(-30, -30);
  minimapTorus2 = new Torus(minimapTorus1, color(0, 0, 255));
  minimapTorus2.translate(80, 0);
  if (minimap.is3D())
    minimap.setType(Graph.Type.ORTHOGRAPHIC);
  minimap.setRadius(500);
  minimap.fitBall();

  eye = new Eye(minimap);
  //to scale the eye on mouse hover comment:
  eye.setHighlighting(Shape.Highlighting.NONE);
  eye.set(scene.eye());
}

void draw() {
  sync();
  handleMouse();
  scene.beginDraw();
  sceneCanvas.background(0);
  scene.traverse();
  scene.drawAxes();
  scene.endDraw();
  scene.display();
  if (showMiniMap) {
    minimap.beginDraw();
    minimapCanvas.background(29, 153, 243);
    minimap.frontBuffer().fill(255, 0, 255, 125);
    minimap.traverse();
    minimap.drawAxes();
    minimap.endDraw();
    minimap.display();
  }
}

void sync() {
  Frame.sync(scene.eye(), eye);
  Frame.sync(torus1, minimapTorus1);
  Frame.sync(torus2, minimapTorus2);
}

void handleMouse() {
  if (!showMiniMap)
    focus = scene;
  else
    focus = mouseX > width-oW && mouseY > height-oH ? minimap : scene;
}

void mouseMoved() {
  focus.cast();
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
    showMiniMap = !showMiniMap;
  if (key == 's')
    scene.fitBallInterpolation();
  if (key == 'S')
    minimap.fitBallInterpolation();
  if (key == 't')
    if (focus.type() == Graph.Type.PERSPECTIVE)
      focus.setType(Graph.Type.ORTHOGRAPHIC);
    else
      focus.setType(Graph.Type.PERSPECTIVE);
}
