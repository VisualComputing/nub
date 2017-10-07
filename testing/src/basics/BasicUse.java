package basics;

import processing.core.*;
import remixlab.bias.event.KeyEvent;
import remixlab.bias.event.KeyShortcut;
import remixlab.bias.event.MotionEvent;
import remixlab.geom.InteractiveFrame;
import remixlab.primitives.Rot;
import remixlab.proscene.*;

/**
 * Created by pierre on 11/15/16.
 */
public class BasicUse extends PApplet {
  Scene scene;
  InteractiveFrame iFrame;
  float length = 100;
  PGraphics pg;

  public void settings() {
    size(800, 800);
  }

  public void setup() {
    pg = this.g;
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setPickingVisualHint(true);
    scene.setRadius(200);

    iFrame = new InteractiveFrame(scene) {
      @Override
      public void visit() {
        graphics(pg);
      }

      @Override
      public void interact(MotionEvent event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            translate(event);
            break;
          case PApplet.RIGHT:
            screenTranslate(event);
            break;
          case processing.event.MouseEvent.WHEEL:
            scale(event);
            break;
        }
      }

      @Override
      public void interact(KeyEvent event) {
        if(event.shortcut().matches(new KeyShortcut(KeyAgent.RIGHT_KEY)))
          translateXPos();
        if(event.shortcut().matches(new KeyShortcut(KeyAgent.LEFT_KEY)))
          translateXNeg();
      }
    };

    iFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    iFrame.setGrabsInputThreshold(length);
    iFrame.translate(50,50);
    iFrame.rotate(new Rot(QUARTER_PI));
    scene.showAll();
  }

  public void graphics(PGraphics pg) {
    pg.fill(255,0,255);
    pg.rect(0,0,length,length);
  }

  public void draw() {
    background(0);
    scene.drawFrames();
    //TODO decide to just leave scene.traverseTree(); throws a npe on scene.targetPGraphics though. The 'hack' would be somethng like:
    //scene.targetPGraphics = pg;
    //scene.traverseTree();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.BasicUse"});
  }
}
