import frames.primitives.Vector;
import frames.core.Frame;
import frames.processing.Scene;

Frame eye;
Frame[] frames;

void setup() {
  size(800, 800, P3D);
  eye = new Frame();
  eye.setPosition(0, 0, 200);
  frames = new Frame[50];
  for (int i = 0; i < frames.length; i++)
    frames[i] = Frame.random(new Vector(), 200, g.is3D());
}

void draw() {
  background(0);
  if (g.is3D()) {
    float fov = PI / 3.0f;
    float cameraZ = (height / 2.0f) / tan(fov / 2.0f);
    perspective(fov, width / height, cameraZ / 10.0f, cameraZ * 10.0f);
    eye.orbit(new Vector(0, 1, 0), 0.01f);
  }
  else
    eye.orbit(new Vector(0, 0, 1), 0.01f);
  setMatrix(Scene.toPMatrix(eye.view()));
  Scene.drawAxes(g, 100);
  for (int i = 0; i < frames.length; i++) {
    pushMatrix();
    Scene.applyTransformation(g, frames[i]);
    Scene.drawTorusSolenoid(g);
    popMatrix();
  }
}
