package frame;

import processing.core.PApplet;
import remixlab.bias.event.*;
import remixlab.bias.*;
import remixlab.primitives.Mat;
import remixlab.primitives.Vec;
import remixlab.proscene.Scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pierre on 11/15/16.
 */
public class CajasOrientadas extends PApplet {
  Scene scene;
  Box[] cajas;
  Sphere esfera;

  /*
  public boolean matches(Shortcut shortcut, List<Shortcut> list) {
    for(Shortcut s : list)
      if(s.matches(shortcut))
        return true;
    return false;
  }
  */

  public void settings() {
    size(640, 360, P3D);
  }

  public void setup() {
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

    //scene.keyAgent().setDefaultGrabber(null);

    if(scene.keyAgent().defaultGrabber() == scene.eye())
      println("is eyeFrame!");
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
    //TODO restore
    //if(key == ' ')
      //scene.keyAgent().shiftDefaultGrabber(scene.eye(), esfera.iFrame);
      //scene.keyAgent().shiftDefaultGrabber(scene.eyeFrame(), scene);
    if(key == 'a')
      scene.toggleAxesVisualHint();
    if(key == 'g')
      scene.toggleGridVisualHint();
    if(key == 'f')
      scene.togglePickingVisualhint();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.CajasOrientadas"});
  }
}
