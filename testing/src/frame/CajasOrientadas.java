package frame;

import processing.core.PApplet;
import remixlab.bias.event.*;
import remixlab.geom.InteractiveFrame;
import remixlab.primitives.Vec;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/15/16.
 */
public class CajasOrientadas extends PApplet {
  Scene scene;
  Box[] cajas;
  Sphere esfera;
  InteractiveFrame eye1, eye2;

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

  public void info() {
    println(scene.radius());
    scene.center().print();
    println(scene.fieldOfView());
    println(scene.zNearCoefficient());
    println(scene.zClippingCoefficient());
    println("dist to scn center: " + scene.distanceToSceneCenter());
    println(scene.zNear());
    println(scene.zFar());
    scene.matrixHandler().projection().print();
    scene.matrixHandler().view().print();
    scene.matrixHandler().modelView().print();
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

    //if(scene.keyAgent().defaultGrabber() == scene.frame())
      //println("is eyeFrame!");
    //frameRate(500);

    eye1 = new InteractiveFrame(scene) {
      @Override
      public void interact(MotionEvent event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            rotate(event);
            break;
          case PApplet.RIGHT:
            translate(event);
            break;
          case processing.event.MouseEvent.WHEEL:
            scale(event);
            break;
        }
      }

      @Override
      public void interact(KeyEvent event) {
        if (event.id() == PApplet.UP)
          translateY(true);
        if (event.id() == PApplet.DOWN)
          translateY(false);
        if (event.id() == PApplet.LEFT)
          translateX(false);
        if (event.id() == PApplet.RIGHT)
          translateX(true);
      }
    };

    eye2 = new InteractiveFrame(scene) {
      @Override
      public void interact(MotionEvent event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            translate(event);
            break;
          case PApplet.RIGHT:
            rotate(event);
            break;
          case processing.event.MouseEvent.WHEEL:
            scale(event);
            break;
        }
      }

      @Override
      public void interact(KeyEvent event) {
        if (event.id() == PApplet.UP)
          translateY(true);
        if (event.id() == PApplet.DOWN)
          translateY(false);
        if (event.id() == PApplet.LEFT)
          translateX(false);
        if (event.id() == PApplet.RIGHT)
          translateX(true);
      }
    };

    scene.setEye(eye1);
    scene.setFieldOfView((float)Math.PI/3);
    scene.inputHandler().setDefaultGrabber(eye1);
    scene.showAll();

    if(scene.is3D())
      println("Scene is 3D");
    else
      println("Scene is 2D");
    //scene.lookAt(new Vec());
    //info();
  }

  public void draw() {
    background(0);

    esfera.draw(false);
    for (int i = 0; i < cajas.length; i++) {
      cajas[i].setOrientation(esfera.getPosition());
      cajas[i].draw(true);
    }
  }

  public void keyPressed() {
    if(key == ' ') {
      if(eye1 == scene.eye()) {
        scene.setEye(eye2);
        scene.setFieldOfView((float)Math.PI/3);
        scene.inputHandler().setDefaultGrabber(eye2);
        scene.showAll();
      }
      else {
        scene.setEye(eye1);
        scene.setFieldOfView((float)Math.PI/3);
        scene.inputHandler().setDefaultGrabber(eye1);
        scene.showAll();
      }
    }
    //TODO restore
    //if(key == ' ')
      //scene.keyAgent().shiftDefaultGrabber(scene.frame(), esfera.iFrame);
      //scene.keyAgent().shiftDefaultGrabber(scene.eyeFrame(), scene);
    if(key ==' ')
      info();
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
