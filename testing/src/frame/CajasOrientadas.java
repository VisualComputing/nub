package frame;

import processing.core.PApplet;
import remixlab.dandelion.primitives.Mat;
import remixlab.dandelion.primitives.Vec;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/15/16.
 */
public class CajasOrientadas extends PApplet {
  Scene scene;
  Box[] cajas;
  Sphere esfera;

  public void settings() {
    size(640, 360, P3D);
  }

  public void setup() {
    //size(640, 360, P3D);
    //size(640, 360, OPENGL);
    scene = new Scene(this);
    scene.setGridVisualHint(true);
    //scene.setCameraType(Camera.Type.ORTHOGRAPHIC);
    scene.setRadius(160);
    //scene.camera().setPosition(new PVector(10,0,0));
    //scene.camera().lookAt( scene.center() );
    scene.showAll();
    //scene.disableBackgroundHanddling();

    esfera = new Sphere(scene);
    esfera.setPosition(new Vec(0.0f, 1.4f, 0.0f));
    esfera.setColor(color(0, 0, 255));

    cajas = new Box[30];
    for (int i = 0; i < cajas.length; i++)
      cajas[i] = new Box(scene);

    //frameRate(500);
  }

  public void draw() {
    background(0);

    esfera.draw();
    for (int i = 0; i < cajas.length; i++) {
      cajas[i].setOrientation(esfera.getPosition());
      cajas[i].draw(true);
    }
  }

  public void keyPressed() {
    if ((key == 'y') || (key == 'Y')) {
      scene.setDottedGrid(!scene.gridIsDotted());
    }
    if ((key == 'u') || (key == 'U')) {
      println("papplet's frame count: " + frameCount);
      println("scene's frame count: " + scene.timingHandler().frameCount());
      Mat view = new Mat();
      scene.camera().getView(view, false);
      println("reported view: ");
      view.print();
      scene.camera().fromView(view, true);
      scene.camera().getView(view, false);
      println("after setting from external: ");
      view.print();
    }
    if ((key == 'v') || (key == 'V')) {
      println("papplet's frame rate: " + frameRate);
      println("scene's frame rate: " + scene.timingHandler().frameRate());
    }
    if (key == 't' || key == 'T')
      scene.shiftTimers();
    if (key == '+')
      frameRate(frameRate + 10);
    if (key == '-')
      frameRate(frameRate - 10);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.CajasOrientadas"});
  }
}
