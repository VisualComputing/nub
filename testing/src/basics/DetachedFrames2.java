package basics;

import frames.core.Frame;
import frames.primitives.Matrix;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;

/**
 * Created by pierre on 11/15/16.
 */
public class DetachedFrames2 extends PApplet {
  Frame eye;
  Frame[] frames;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    eye = new Frame();
    eye.setPosition(0, 0, 200);
    frames = new Frame[5];
    for (int i = 0; i < frames.length; i++) {
      frames[i] = new Frame();
      frames[i].randomize();
    }
  }

  public void draw() {
    background(0);
    float fov = PI / 3.0f;
    float cameraZ = (height / 2.0f) / tan(fov / 2.0f);
    perspective(fov, width / height, cameraZ / 10.0f, cameraZ * 10.0f);
    //((PGraphicsOpenGL)g).setProjection(Scene.toPMatrix(Matrix.perspective(cameraZ / 10.0f, cameraZ * 10.0f, width / height, -tan(fov / 2.0f))));
    //resetMatrix();
    //applyMatrix(Scene.toPMatrix(eye.view()));
    setMatrix(Scene.toPMatrix(eye.view()));
    Scene.drawAxes(g, 100);
    for (int i = 0; i < frames.length; i++) {
      pushMatrix();
      Scene.applyTransformation(g, frames[i]);
      Scene.drawTorusSolenoid(g);
      popMatrix();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.DetachedFrames2"});
  }
}
