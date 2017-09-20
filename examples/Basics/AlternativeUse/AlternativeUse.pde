/**
 * Alternative Use.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates how to use proscene through inheritance.
 * 
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;

MyScene scene;
//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P2D;

void setup() {
  size(640, 360, renderer);
  // We instantiate our MyScene class defined below
  scene = new MyScene(this);
}

// Make sure to define the draw() method, even if it's empty.
void draw() {
}

class MyScene extends Scene {
  // We need to call super(p) to instantiate the base class
  public MyScene(PApplet p) {
    super(p);
  }

  // Initialization stuff could have also been performed at
  // setup(), once after the Scene object have been instantiated 
  public void init() {
    setGridVisualHint(false);
  }

  //Define here what is actually going to be drawn.
  public void proscenium() {
    background(0);
    fill(204, 102, 0, 150);
    drawTorusSolenoid();
  }
}