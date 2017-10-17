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
    scene = new Scene(this) {
      KeyShortcut a = new KeyShortcut('a');
      KeyShortcut g = new KeyShortcut('g');
      KeyShortcut f = new KeyShortcut('f');
      KeyShortcut r = new KeyShortcut('r');
      KeyShortcut s = new KeyShortcut('s');
      KeyShortcut t = new KeyShortcut(Event.CTRL, '1');
      KeyShortcut u = new KeyShortcut(Event.ALT, '1');
      KeyShortcut v = new KeyShortcut('1');

      /*
      List<Shortcut> l =  new ArrayList<Shortcut>(Arrays.asList(a, g, f, r, s, t, u, v));

      @Override
      public boolean checkIfGrabsInput(KeyEvent event) {
        return matches(event.shortcut(), l);
      }
      */

      @Override
      public void interact(Event event) {
        if(event.shortcut().matches(a))
          toggleAxesVisualHint();
        if(event.shortcut().matches(g))
          toggleGridVisualHint();
        if(event.shortcut().matches(f))
          togglePickingVisualhint();
        if(event.shortcut().matches(r))
          togglePathsVisualHint();
        //TODO restore
        /*
        if(event.shortcut().matches(s))
          interpolateToFitScene();
        if(event.shortcut().matches(t))
          addKeyFrameToPath1();
        if(event.shortcut().matches(u))
          deletePath1();
        if(event.shortcut().matches(v))
          playPath1();
        //*/
      }
    };
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

    if(scene.keyAgent().defaultGrabber() == scene.eyeFrame())
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
    if(key == ' ')
      //scene.keyAgent().shiftDefaultGrabber(scene.eyeFrame(), esfera.iFrame);
      scene.keyAgent().shiftDefaultGrabber(scene.eyeFrame(), scene);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.CajasOrientadas"});
  }
}
