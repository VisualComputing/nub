package basics;

import processing.core.PApplet;
import processing.core.PGraphics;
import remixlab.core.Node;
import remixlab.input.Event;
import remixlab.input.event.*;
import remixlab.proscene.KeyAgent;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/15/16.
 */
public class SimpleUse extends PApplet {
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
      public void interact(Event event) {
        if(event instanceof MotionEvent1)
          interact1((MotionEvent1)event);
        if(event instanceof MotionEvent2)
          interact2((MotionEvent2)event);
      }

      public void interact1(MotionEvent1 event) {
        scale(event);
      }

      public void interact2(MotionEvent2 event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            rotate(event);
            break;
          case PApplet.RIGHT:
            screenTranslate(event);
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
    PApplet.main(new String[]{"basics.SimpleUse"});
  }
}
