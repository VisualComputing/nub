/**
 * Cajas Orientadas.
 * by Jean Pierre Charalambos.
 * 
 * Cajas Orientadas proscene example ported to frames.
 */

import frames.core.*;
import frames.primitives.*;
import frames.processing.*;

Scene scene;
Box[] cajas;
Sphere esfera;
Node eye;

void setup() {
  size(640, 360, P3D);
  scene = new Scene(this);
  scene.setRadius(200);
  scene.fitBall();
  scene.setType(Graph.Type.ORTHOGRAPHIC);
  esfera = new Sphere();
  esfera.setPosition(new Vector(0.0f, 1.4f, 0.0f));
  esfera.setColor(color(0, 0, 255));

  cajas = new Box[30];
  for (int i = 0; i < cajas.length; i++)
    cajas[i] = new Box();

  eye = new OrbitNode(scene);

  scene.setEye(eye);
  scene.setFieldOfView((float) Math.PI / 3);
  scene.setDefaultGrabber(eye);
  scene.fitBall();
}

void draw() {
  background(0);

  esfera.draw(false);
  for (int i = 0; i < cajas.length; i++) {
    cajas[i].setOrientation(esfera.getPosition());
    cajas[i].draw(true);
  }

  String text = "Cajas Orientadas";
  float w = scene.frontBuffer().textWidth(text);
  float h = scene.frontBuffer().textAscent() + scene.frontBuffer().textDescent();
  scene.beginScreenCoordinates();
  //textFont(font);
  text(text, 20, 20, w + 1, h);
  scene.endScreenCoordinates();
}

void keyPressed() {
  if (key == 'e')
    scene.setType(Graph.Type.ORTHOGRAPHIC);
  if (key == 'E')
    scene.setType(Graph.Type.PERSPECTIVE);
  if (key == 's')
    scene.fitBallInterpolation();
  if (key == 'S')
    scene.fitBall();
  if (key == 'u')
    scene.shiftDefaultGrabber((Node) scene.eye(), esfera.iFrame);
}