package broken;

import processing.core.*;
import remixlab.proscene.*;
import remixlab.dandelion.geom.*;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Created by pierre on 5/1/17.
 */
public class Projectors extends PApplet {
  Scene scene, auxScene;
  PGraphics canvas, auxCanvas;
  InteractiveFrame frame1, auxFrame1;
  InteractiveFrame iFrame;

  int w = 800;
  int h = 640;
  boolean showMiniMap;

  List<Vec> vertices = Arrays.asList(new Vec(50, -115, 0), new Vec(-70, -50, 0), new Vec(90, -45, 0));

  String renderer = P3D;
  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    canvas = createGraphics(w, h, renderer);

    scene = new Scene(this, canvas);
    scene.eyeFrame().setMagnitude(1);
    frame1 = new InteractiveFrame(scene, "frameDrawing");
    frame1.translate(30, 30);

    auxCanvas = createGraphics(w, h, renderer);
    auxScene = new Scene(this, auxCanvas);
    auxScene.setVisualHints(0);
    auxScene.setRadius(200);
    auxScene.showAll();

    auxFrame1 = new InteractiveFrame(auxScene);
    auxFrame1.set(frame1);

    iFrame = new InteractiveFrame(auxScene);
    //to not scale the iFrame on mouse hover uncomment:
    //iFrame.setHighlightingMode(InteractiveFrame.HighlightingMode.NONE);
    iFrame.setWorldMatrix(scene.eyeFrame());
    iFrame.setShape(scene.eyeFrame());
    smooth(8);
  }

  public void draw() {
    InteractiveFrame.sync(scene.eyeFrame(), iFrame);
    InteractiveFrame.sync(frame1, auxFrame1);
    if (!showMiniMap) {
    scene.beginDraw();
    canvas.background(120);
    scene.drawFrames();
    scene.endDraw();
    scene.display();
    }
    if (showMiniMap) {
      auxScene.pg().noFill();
      auxScene.pg().strokeWeight(4);
      auxScene.beginDraw();
      ///*
      auxCanvas.background(120);
      auxScene.drawFrames();
      // convert vertices from frame to world
      List<Vec> worldVertices = new ArrayList<Vec>();
      for (Vec v : vertices)
        worldVertices.add(frame1.inverseCoordinatesOf(new Vec(v.x(), v.y(), v.z())));
      auxScene.pg().pushStyle();
      auxScene.pg().stroke(0,255,0);
      auxScene.drawProjectors(scene.eye(), worldVertices);
      auxScene.pg().popStyle();
      auxScene.pg().fill(0, 0, 255, 125);
      auxScene.pg().stroke(0,0,255);
      //*/
      auxScene.endDraw();
      auxScene.display();
    }
  }

  public void keyPressed() {
    if (key == ' ') {
      showMiniMap = !showMiniMap;
      if(showMiniMap) {
        /*
        InteractiveFrame f = new InteractiveFrame(auxScene);
        f.fromMatrix(scene.eyeFrame().matrix());
        auxScene.eye().addKeyFrameToPath(1);
        auxScene.eye().resetPath(1);
        auxScene.eye().keyFrameInterpolatorArray()[0].addKeyFrame(f);
        auxScene.eye().playPath(1);
        //*/
        ///*
        auxScene.eyeFrame().setWorldMatrix(iFrame);

        //auxScene.eyeFrame().setWorldMatrix(scene.eyeFrame());
        //scene.eyeFrame().worldMatrix().print();
        //auxScene.eyeFrame().worldMatrix().print();

        //auxScene.eyeFrame().setPosition(scene.eyeFrame().position());
        //auxScene.eyeFrame().setOrientation(scene.eyeFrame().orientation());
        //auxScene.eyeFrame().setMagnitude(scene.eyeFrame().magnitude());

        //scene.eyeFrame().position().print();
        //auxScene.eyeFrame().position().print();
        //scene.eyeFrame().orientation().print();
        //auxScene.eyeFrame().orientation().print();
        //println(scene.eyeFrame().magnitude());
        //println(auxScene.eyeFrame().magnitude());
        //*/
      }
      else {

      }
    }
    if(key == 'p') {
      //scene.eyeFrame().worldMatrix().print();
      //auxScene.eyeFrame().worldMatrix().print();
      //iFrame.worldMatrix().print();

      scene.eyeFrame().position().print();
      auxScene.eyeFrame().position().print();
      //scene.eyeFrame().orientation().print();
      //auxScene.eyeFrame().orientation().print();
      //println(scene.eyeFrame().magnitude());
      //println(auxScene.eyeFrame().magnitude());
    }
  }

  public void frameDrawing(PGraphics pg) {
    pg.fill(random(0, 255), random(0, 255), random(0, 255));
    pg.beginShape(TRIANGLES);
    for (Vec v : vertices)
      pg.vertex(v.x(), v.y());
    pg.endShape();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"broken.Projectors"});
  }
}
