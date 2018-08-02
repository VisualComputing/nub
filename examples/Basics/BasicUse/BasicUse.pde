import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.processing.Scene;
import frames.processing.Shape;

Scene scene;
Frame frame, shape;

void settings() {
  size(800, 800, P3D);
}

void setup() {
  rectMode(CENTER);
  scene = new Scene(this);
  scene.setFieldOfView(PI / 3);
  scene.setRadius(1000);
  scene.fitBallInterpolation();

  frame = new Frame(scene) {
    @Override
      public void visit() {
      scene.drawAxes(scene.radius() / 3);
      pushStyle();
      rectMode(CENTER);
      fill(255, 0, 255);
      if (scene.is3D())
        scene.drawCylinder(30, scene.radius() / 4, 200);
      else
        rect(10, 10, 200, 200);
      stroke(255, 255, 0);
      scene.drawShooterTarget(this);
      popStyle();
    }
  };
  frame.setRotation(Quaternion.random());
  shape = new Shape(scene, shape());
  shape.setRotation(Quaternion.random());
  shape.translate(275, 275, 275);
}

void draw() {
  background(0);
  scene.drawAxes();
  if (mousePressed)
    scene.cast();
  else
    scene.traverse();
}

void mouseMoved() {
  scene.mouseSpin();
}

PShape shape() {
  PShape fig = scene.is3D() ? createShape(BOX, 150) : createShape(RECT, 0, 0, 150, 150);
  fig.setStroke(255);
  fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return fig;
}

void keyPressed() {
  if (key == 'f')
    scene.flip();
}