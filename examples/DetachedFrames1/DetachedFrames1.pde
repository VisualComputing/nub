import frames.core.Frame;
import frames.primitives.Vector;
import frames.processing.Scene;

Frame[] frames;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = JAVA2D;

void setup() {
  size(800, 800, renderer);
  frames = new Frame[50];
  for (int i = 0; i < frames.length; i++)
    frames[i] = Frame.random(new Vector(400, 400, 0), 400, g.is3D());
}

void draw() {
  background(0);
  for (int i = 0; i < frames.length; i++) {
    pushMatrix();
    Scene.applyTransformation(g, frames[i]);
    Scene.drawTorusSolenoid(g);
    popMatrix();
  }
}
