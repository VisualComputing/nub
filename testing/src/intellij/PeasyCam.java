package intellij;

import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class PeasyCam extends PApplet {
  Scene scene;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(200);
    scene.fit();
  }

  public void draw() {
    //background(0);
    rotateX(-.5f);
    rotateY(-.5f);
    lights();
    scale(10);
    strokeWeight(1 / 10f);
    background(0);
    fill(255, 0, 0);
    box(30);
    pushMatrix();
    translate(0, 0, 20);
    fill(0, 0, 255);
    box(5);
    popMatrix();
    scene.render();
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      if (!scene.mouseSpinTag())
        //scene.mouseSpinEye();
        scene.mouseDampedSpinEye();
    } else if (mouseButton == RIGHT)
      if (!scene.mouseTranslateTag())
        scene.mouseDampedTranslateEye();
        //scene.mouseTranslateEye();
      else
        scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.dampedMoveForward(event.getCount() * 40);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.PeasyCam"});
  }
}
