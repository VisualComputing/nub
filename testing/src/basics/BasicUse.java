package basics;

import processing.core.PApplet;
import processing.core.PGraphics;
import proscene.core.Node;
import proscene.input.Event;
import proscene.input.Shortcut;
import proscene.input.event.KeyShortcut;
import proscene.input.event.MotionEvent1;
import proscene.input.event.MotionEvent2;
import proscene.processing.Scene;

/**
 * Created by pierre on 11/15/16.
 */
public class BasicUse extends PApplet {
  Scene scene;
  Node iFrame;
  float length = 100;
  PGraphics pg;

  public void settings() {
    size(800, 800, P2D);
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
        Shortcut s1 = new Shortcut(PApplet.LEFT);
        Shortcut s2 = new Shortcut(PApplet.RIGHT);
        Shortcut s3 = new Shortcut(processing.event.MouseEvent.WHEEL);
        //look at the ugly casts needed Java that actually was
        //preventing the following syntax:
        //those casts aren't needed in js
        ///*
        if (s1.matches(event.shortcut()))
          rotate((MotionEvent2) event);
        if (s2.matches(event.shortcut()))
          screenTranslate((MotionEvent2) event);
        if (s3.matches(event.shortcut()))
          scale((MotionEvent1) event);
        // */
        //Check symmetry of Shortcut.matches
        /*
        if(event.shortcut().matches(s1))
          rotate((MotionEvent2) event);
        if(event.shortcut().matches(s2))
          screenTranslate((MotionEvent2) event);
        if(event.shortcut().matches(s3))
          scale((MotionEvent1) event);
        if(event.shortcut().matches(s4))
          translateXPos();
        if(event.shortcut().matches(s5))
          translateXNeg();
        // */
      }

      /*
      @Override
      public void interact(MotionEvent event) {
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
      public void interact(KeyEvent event) {
        if(event.shortcut().matches(new KeyShortcut(KeyAgent.RIGHT_KEY)))
          translateXPos();
        if(event.shortcut().matches(new KeyShortcut(KeyAgent.LEFT_KEY)))
          translateXNeg();
      }
      */
    };

    iFrame.setPrecision(Node.Precision.ADAPTIVE);
    iFrame.setPrecisionThreshold(length);
    iFrame.translate(50, 50);

    scene.setDefaultNode(iFrame);
    scene.fitBallInterpolation();
  }

  public void graphics(PGraphics pg) {
    pg.fill(255, 0, 255);
    pg.rect(0, 0, length, length);
  }

  public void draw() {
    background(0);
    scene.traverse();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.BasicUse"});
  }
}
