/**
 * Cajas Orientadas.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates some basic properties of frames, particularly
 * how to orient them.
 *
 * Use the arrow keys to select and move the sphere and see how the boxes
 * will immediately be oriented towards it. You can also pick and move the
 * boxes (by dragging them with the mouse right button) and still they
 * will be oriented towards the sphere.
 *
 * Both the sphere and the boxes are implemented as detached-frames
 * specializations and hence they require to apply the local node
 * transformations, check their drawing routines.
 *
 * Contrast this example with the attached-node version with the same name.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

Scene scene;
boolean drawAxes = true, drawShooterTarget = true, adaptive;
Box[] cajas;
Sphere esfera;

void setup() {
  size(800, 800, P3D);
  scene = new Scene(this);
  scene.setRadius(200);
  scene.fit();
  scene.setType(Graph.Type.ORTHOGRAPHIC);
  esfera = new Sphere();
  esfera.setPosition(new Vector(0.0f, 1.4f, 0.0f));

  cajas = new Box[30];
  for (int i = 0; i < cajas.length; i++)
    cajas[i] = new Box(color(random(255), random(255), random(255)));

  scene.fit(1);
  scene.setTrackedFrame("keyboard", esfera);
}

void draw() {
  background(0);

  // Frame drawing
  esfera.draw();
  for (int i = 0; i < cajas.length; i++) {
    cajas[i].setOrientation(esfera.position());
    cajas[i].draw();
  }

  String text = "Cajas Orientadas";
  float w = textWidth(text);
  float h = textAscent() + textDescent();
  scene.beginScreenDrawing();
  //textFont(font);
  text(text, 20, 20, w + 1, h);
  scene.endScreenDrawing();
}

void mouseMoved() {
  scene.track(cajas);
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.translate();
  else
    scene.scale(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  scene.zoom(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ') {
    adaptive = !adaptive;
    for (Frame node : cajas)
      if (adaptive)
        node.setPrecision(Frame.Precision.ADAPTIVE);
      else
        node.setPrecision(Frame.Precision.FIXED);
  }
  if (key == 'a')
    drawAxes = !drawAxes;
  if (key == 'p')
    drawShooterTarget = !drawShooterTarget;
  if (key == 'e')
    scene.setType(Graph.Type.ORTHOGRAPHIC);
  if (key == 'E')
    scene.setType(Graph.Type.PERSPECTIVE);
  if (key == 's')
    scene.fit(1);
  if (key == 'S')
    scene.fit();
  if (key == 'u')
    if (scene.trackedFrame("keyboard") == null)
      scene.setTrackedFrame("keyboard", esfera);
    else
      scene.resetTrackedFrame("keyboard");
  if (key == CODED)
    if (keyCode == UP)
      scene.translate("keyboard", 0, -10);
    else if (keyCode == DOWN)
      scene.translate("keyboard", 0, 10);
    else if (keyCode == LEFT)
      scene.translate("keyboard", -10, 0);
    else if (keyCode == RIGHT)
      scene.translate("keyboard", 10, 0);
}
