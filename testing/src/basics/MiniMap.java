package basics;

import processing.core.*;
import remixlab.dandelion.geom.Vec;
import remixlab.proscene.*;

import java.util.Arrays;
import java.util.List;

/**
 * Created by pierre on 5/1/17.
 */
public class MiniMap extends PApplet {
  Scene scene, auxScene;
  PGraphics canvas, auxCanvas;
  InteractiveFrame frame1, auxFrame1, frame2, auxFrame2, frame3, auxFrame3;
  InteractiveFrame iFrame;

  int w = 1110;
  int h = 510;
  int oW = w/3;
  int oH = h/3;
  int oX = w - oW;
  int oY = h - oH;
  boolean showMiniMap  = false;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    canvas = createGraphics(w, h, renderer);
    scene = new Scene(this, canvas);
    frame1 = new InteractiveFrame(scene, "frameDrawing");
    frame1.translate(30, 30);
    frame2 = new InteractiveFrame(scene, frame1, "frameDrawing");
    frame2.translate(40, 0);
    frame3 = new InteractiveFrame(scene, frame2, "frameDrawing");
    frame3.translate(40, 0);

    auxCanvas = createGraphics(oW, oH, renderer);
    auxScene = new Scene(this, auxCanvas, oX, oY);
    auxScene.setVisualHints(0);
    auxScene.setRadius(200);
    auxScene.showAll();

    auxFrame1 = new InteractiveFrame(auxScene);
    auxFrame1.set(frame1);
    auxFrame2 = new InteractiveFrame(auxScene, auxFrame1);
    auxFrame2.set(frame2);
    auxFrame3 = new InteractiveFrame(auxScene, auxFrame2);
    auxFrame3.set(frame3);

    iFrame = new InteractiveFrame(auxScene);
    //to not scale the iFrame on mouse hover uncomment:
    //iFrame.setHighlightingMode(InteractiveFrame.HighlightingMode.NONE);
    iFrame.setWorldMatrix(scene.eyeFrame());
    iFrame.setShape(scene.eyeFrame());
  }

  public void draw() {
    InteractiveFrame.sync(scene.eyeFrame(), iFrame);
    InteractiveFrame.sync(frame1, auxFrame1);
    InteractiveFrame.sync(frame2, auxFrame2);
    InteractiveFrame.sync(frame3, auxFrame3);
    scene.beginDraw();
    canvas.background(0);
    scene.drawFrames();
    scene.endDraw();
    scene.display();
    if (showMiniMap) {
      auxScene.beginDraw();
      auxCanvas.background(29, 153, 243);
      auxScene.pg().fill(255, 0, 255, 125);
      auxScene.drawFrames();
      auxScene.endDraw();
      auxScene.display();
    }
  }

  public void keyPressed() {
    if (key == ' ') {
      showMiniMap = !showMiniMap;
      if(showMiniMap)
        auxScene.eyeFrame().setWorldMatrix(scene.eyeFrame());
    }
    if (key == 'x')
      iFrame.setShape("eyeDrawing");
    if (key == 'y')
      iFrame.setShape(scene.eyeFrame());
  }

  public void frameDrawing(PGraphics pg) {
    pg.fill(random(0, 255), random(0, 255), random(0, 255));
    if (scene.is3D())
      pg.box(40, 10, 5);
    else
      pg.rect(0, 0, 40, 10, 5);
  }

  public void eyeDrawing(PGraphics pg) {
    if (auxScene.is3D())
      pg.box(200);
    else {
      pg.pushStyle();
      pg.rectMode(CENTER);
      pg.rect(0, 0, 200, 200);
      pg.popStyle();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.MiniMap"});
  }
}
