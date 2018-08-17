import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

Scene scene;
Vector randomVector;
boolean cad, lookAround;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

void setup() {
  size(1600, 800, renderer);
  rectMode(CENTER);
  scene = new Scene(this);
  scene.setFieldOfView(PI / 3);
  //scene.setType(Graph.Type.ORTHOGRAPHIC);
  scene.setRadius(1000);
  scene.fitBallInterpolation();

  Shape shape1 = new Shape(scene) {
    @Override
    public void setGraphics(PGraphics pGraphics) {
      Scene.drawAxes(pGraphics, scene.radius() / 3);
      pGraphics.pushStyle();
      pGraphics.rectMode(CENTER);
      pGraphics.fill(255, 0, 255);
      if (scene.is3D())
        Scene.drawTorusSolenoid(pGraphics, 80);
      else
        pGraphics.rect(10, 10, 200, 200);
      pGraphics.popStyle();
    }
  };
  shape1.setRotation(Quaternion.random());
  shape1.translate(-375, 175);

  Shape shape2 = new Shape(shape1);
  shape2.setGraphics(shape());
  shape2.translate(275, 275);

  randomVector = Vector.random();
  randomVector.setMagnitude(scene.radius() * 0.5f);
}

void draw() {
  background(0);
  fill(0, 255, 255);
  scene.drawArrow(randomVector);
  scene.drawAxes();
  // visit scene frames (shapes simply get drawn)
  scene.traverse();
}

void control() {
  control(scene.defaultFrame(null));
}

void control(String hid) {
  control(scene.defaultFrame(hid));
}

void control(Frame frame) {
  if (frame == null)
    println("null");
  else
    println("ctrl");
}

void keyPressed() {
  if (key == 'e')
    scene.enableBackBuffer();
  if (key == 'd')
    scene.disableBackBuffer();
  if (key == 'f')
    scene.flip();
  if (key == 's')
    scene.fitBallInterpolation();
  if (key == 'f')
    scene.fitBall();
  if (key == 'c') {
    cad = !cad;
    if (cad) {
      scene.eye().setYAxis(randomVector);
      scene.fitBall();
    }
  }
  if (key == 'a')
    lookAround = !lookAround;
  if (key == 'r')
    scene.setRightHanded();
  if (key == 'l')
    scene.setLeftHanded();
  if (key == 'p')
    if (scene.type() == Graph.Type.PERSPECTIVE)
      scene.setType(Graph.Type.ORTHOGRAPHIC);
    else
      scene.setType(Graph.Type.PERSPECTIVE);
}

void mouseMoved() {
  scene.cast("mouseMoved", scene.mouse());
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin("mouseMoved", scene.pmouse(), scene.mouse());
  else if (mouseButton == RIGHT)
    scene.translate("mouseMoved", scene.mouseDX(), scene.mouseDY());
  else
    control("mouseMoved");
}

void mouseWheel(MouseEvent event) {
  scene.zoom("mouseMoved", event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 1)
    scene.cast("mouseClicked", scene.mouse());
  else if (event.getCount() == 2)
    if (event.getButton() == LEFT)
      scene.focus("mouseMoved");
    else
      scene.align("mouseMoved");
}

PShape shape() {
  PShape fig = scene.is3D() ? createShape(BOX, 150) : createShape(RECT, 0, 0, 150, 150);
  fig.setStroke(255);
  fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return fig;
}