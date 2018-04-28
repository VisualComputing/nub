package demos;

import common.InteractiveNode;
import frames.processing.Scene;
import processing.core.PApplet;

public class Luxo extends PApplet {
  Scene scene;
  Lamp lamp;

  public void settings() {
    size(1000, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    InteractiveNode eye = new InteractiveNode(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    //interactivity defaults to the eye
    scene.setDefaultNode(eye);
    scene.setRadius(100);
    scene.fitBallInterpolation();
    lamp = new Lamp(scene);
  }

  public void draw() {
    background(0);
    lights();

    //draw the lamp
    scene.traverse();

    //draw the ground
    noStroke();
    fill(120, 120, 120);
    float nbPatches = 100;
    normal(0.0f, 0.0f, 1.0f);
    for (int j = 0; j < nbPatches; ++j) {
      beginShape(QUAD_STRIP);
      for (int i = 0; i <= nbPatches; ++i) {
        vertex((200 * (float) i / nbPatches - 100), (200 * j / nbPatches - 100));
        vertex((200 * (float) i / nbPatches - 100), (200 * (float) (j + 1) / nbPatches - 100));
      }
      endShape();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"demos.Luxo"});
  }
}
