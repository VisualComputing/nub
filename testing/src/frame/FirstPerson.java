package frame;

import frames.core.Graph;
import frames.core.Node;
import frames.input.Shortcut;
import frames.input.event.KeyEvent;
import frames.input.event.KeyShortcut;
import frames.input.event.MotionEvent;
import frames.processing.Mouse;
import frames.processing.Scene;
import processing.core.PApplet;

public class FirstPerson extends PApplet {
  Scene scene;
  Node iFrame;

  @Override
  public void settings() {
    size(800, 800, P3D);
  }

  @Override
  public void setup() {
    scene = new Scene(this);
    iFrame = new Node(scene) {
      @Override
      public void interact(MotionEvent event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            rotate(event);
            break;
          case PApplet.CENTER:
            scale(event);
            break;
          case PApplet.RIGHT:
            translate(event);
            break;

        }
      }
    };
    iFrame.translate(30, 30);

    scene.mouse().setMode(Mouse.Mode.CLICK);

    InteractiveFrame eye = new InteractiveFrame();
    scene.setEye(eye);
    scene.setDefaultNode(eye);
    scene.fitBallInterpolation();
  }

  @Override
  public void draw() {
    background(0);
    fill(204, 102, 0, 150);
    scene.drawTorusSolenoid();

    // Save the current model view matrix
    pushMatrix();
    // Multiply matrix to get in the frame coordinate system.
    // applyMatrix(Scene.toPMatrix(iFrame.matrix())); //is possible but inefficient
    iFrame.applyTransformation();//very efficient
    // Draw an axis using the Scene static function
    scene.drawAxes(20);

    // Draw a second torus
    if (scene.mouse().defaultGrabber() == iFrame) {
      fill(0, 255, 255);
      scene.drawTorusSolenoid();
    } else if (iFrame.grabsInput()) {
      fill(255, 0, 0);
      scene.drawTorusSolenoid();
    } else {
      fill(0, 0, 255, 150);
      scene.drawTorusSolenoid();
    }

    popMatrix();
  }

  public void keyPressed() {
    if (key == 'i')
      scene.inputHandler().shiftDefaultGrabber((Node) scene.eye(), iFrame);
  }

  public class InteractiveFrame extends Node {
    Shortcut left = new Shortcut(PApplet.LEFT);
    Shortcut right = new Shortcut(PApplet.RIGHT);
    Shortcut wheel = new Shortcut(processing.event.MouseEvent.WHEEL);
    KeyShortcut upArrow = new KeyShortcut(PApplet.UP);
    KeyShortcut downArrow = new KeyShortcut(PApplet.DOWN);
    KeyShortcut leftArrow = new KeyShortcut(PApplet.LEFT);
    KeyShortcut rightArrow = new KeyShortcut(PApplet.RIGHT);

    public InteractiveFrame() {
      super(scene);
    }

    protected InteractiveFrame(Graph otherGraph, InteractiveFrame otherFrame) {
      super(otherGraph, otherFrame);
    }

    @Override
    public InteractiveFrame get() {
      return new InteractiveFrame(this.graph(), this);
    }

    @Override
    public void interact(MotionEvent event) {
      if (event.shortcut().matches(left))
        translate(event);
      else if (event.shortcut().matches(right))
        rotate(event);
      else if (event.shortcut().matches(wheel))
        if (isEye() && graph().is3D())
          translateZ(event);
        else
          scale(event);
    }

    @Override
    public void interact(KeyEvent event) {
      if (event.shortcut().matches(upArrow))
        translateYPos();
      else if (event.shortcut().matches(downArrow))
        translateYNeg();
      else if (event.shortcut().matches(leftArrow))
        translateXNeg();
      else if (event.shortcut().matches(rightArrow))
        translateXPos();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.FirstPerson"});
  }
}
