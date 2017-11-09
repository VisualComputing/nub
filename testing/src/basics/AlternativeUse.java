package basics;

import processing.core.PApplet;
import remixlab.bias.event.KeyEvent;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/22/16.
 */
public class AlternativeUse extends PApplet {
  MyScene scene;
  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P2D;

  public void settings() {
    size(700, 700, renderer);
  }

  public void setup() {
    // We instantiate our MyScene class defined below
    scene = new MyScene(this);
  }

  // Make sure to define the draw() method, even if it's empty.
  public void draw() {
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.AlternativeUse"});
  }

  public class MyScene extends Scene {
    // We need to call super(p) to instantiate the base class
    public MyScene(PApplet p) {
      super(p);
    }

    // Initialization stuff could have also been performed at
    // setup(), once after the Scene object have been instantiated
    public void init() {
      //setGridVisualHint(false);
    }

    //Define here what is actually going to be drawn.
    public void proscenium() {
      background(0);
      fill(204, 102, 0, 150);
      drawTorusSolenoid();
    }
  }
}
