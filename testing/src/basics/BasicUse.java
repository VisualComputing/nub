package basics;

import processing.core.*;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.KeyShortcut;
import remixlab.input.event.MotionEvent;
import remixlab.core.Node;
import remixlab.proscene.*;

/**
 * Created by pierre on 11/15/16.
 */
public class BasicUse extends PApplet {
  Scene scene;
  Node iFrame;
  float length = 100;
  PGraphics pg;

  public void settings() {
    size(800, 800);
  }

  public void setup() {
    pg = this.g;
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(200);

    iFrame = new Node(scene) {
      @Override
      public void visit() {
        graphics(pg);
      }

      @Override
      public void motionInteraction(MotionEvent event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            //translate(_event);
            rotate(event);
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
      public void keyInteraction(KeyEvent event) {
        if(event.shortcut().matches(new KeyShortcut(KeyAgent.RIGHT_KEY)))
          translateXPos();
        if(event.shortcut().matches(new KeyShortcut(KeyAgent.LEFT_KEY)))
          translateXNeg();
      }
    };

    iFrame.setPrecision(Node.Precision.ADAPTIVE);
    iFrame.setPrecisionThreshold(length);
    iFrame.translate(50,50);
    scene.fitBall();
  }

  public void graphics(PGraphics pg) {
    pg.fill(255,0,255);
    pg.rect(0,0,length,length);
  }

  public void draw() {
    background(0);
    scene.traverse();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.BasicUse"});
  }
}
