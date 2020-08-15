package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class MouseDragInteraction extends PApplet {
  Scene scene;
  Vector randomVector;
  boolean cad, lookAround;
  Node node, shape1, shape2;

  public void settings() {
    size(1200, 600, P3D);
  }

  public void setup() {
    rectMode(CENTER);

    //scene = new Scene(this, 100, 1600);
    //scene.eye().setPosition(new Vector(0, 0, 850));
    scene = new Scene(this, 1500);
    scene.setHUD((PGraphics pg) -> {
      pg.pushStyle();
      pg.stroke(255, 255, 0);
      pg.strokeWeight(5);
      pg.fill(255, 255, 0, 125);
      pg.rect(pixelX, pixelY, 2 * h, h);
      //pg.point(pixelX, pixelY);
      pg.popStyle();
    });
    //scene.togglePerspective();
    scene.enableHint(Scene.AXES);
    scene.enableHint(Scene.BACKGROUND, color(0));
    //scene.fit(1);

    shape1 = new Node((PGraphics pGraphics) -> {
      Scene.drawAxes(pGraphics, scene.radius() / 3);
      pGraphics.pushStyle();
      pGraphics.rectMode(CENTER);
      pGraphics.fill(255, 0, 255);
      if (scene.is3D())
        Scene.drawTorusSolenoid(pGraphics, 80);
      else
        pGraphics.rect(10, 10, 200, 200);
      pGraphics.popStyle();
    }
    );
    shape1.setRotation(Quaternion.random());
    shape1.translate(-375, 175, 0);

    shape2 = new Node(shape1, (PGraphics pGraphics) -> {
      Scene.drawAxes(pGraphics, scene.radius() / 3);
      pGraphics.pushStyle();
      pGraphics.rectMode(CENTER);
      pGraphics.fill(255, 255, 0);
      if (scene.is3D())
        pGraphics.box(150);
      else
        pGraphics.rect(10, 10, 200, 200);
      pGraphics.popStyle();
    }
    );
    shape2.translate(275, 275, 0);
    randomVector = Vector.random();
    randomVector.setMagnitude(scene.radius() * 0.5f);
  }

  public void draw() {
    // render scene nodes (shapes simply get drawn)
    scene.render();
    fill(0, 255, 255);
    scene.drawArrow(randomVector);
  }

  public void keyPressed() {
    if (key == 'd') {
      if (node == null) {
        node = shape1.detach();
        scene.randomize(node);
        node.setShape(shape());
        node.setReference(shape2);
      }
    }
    if (key == 'e') {
      if (node != null)
        node.setReference(shape1);
    }
    if (key == 'x') {
      Scene.prune(node);
    }
    if (key == 'y') {
      if (node != null)
        node.resetReference();
    }
    if (key == 'i')
      Scene.leftHanded = !Scene.leftHanded;
    if (key == 's')
      scene.fit();
    if (key == 'f')
      scene.fit(1);
    if (key == 'c') {
      cad = !cad;
      if (cad) {
        scene.eye().setYAxis(randomVector);
        scene.fit();
      }
    }
    if (key == 'q') {
      println(shape2.orientation().toString());
      //Quaternion q = shape2.worldDisplacement(new Quaternion());
      Quaternion q = shape2.worldDisplacement(shape2.rotation());
      println(q.toString());
    }
    if (key == 'a')
      lookAround = !lookAround;
    if (key == 'r')
      Scene.leftHanded = !Scene.leftHanded;
    if (key == 'p')
      scene.togglePerspective();

    if (key == 'g') {
      scene.fit(pixelX, pixelY, 2 * h, h, 1);
    }
  }

  int pixelX = 300, pixelY = 400, h = 100;

  @Override
  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      if (cad) {
        //scene.mouseRotateCAD(randomVector);
        scene.mouseRotateCAD();
      } else if (lookAround) {
        scene.mouseLookAround();
      } else {
        scene.mouseSpin();
        //if (!scene.mouseSpinTag(1))
        //scene.mouseSpinEye(1);
      }
    else if (mouseButton == RIGHT) {
      scene.mouseTranslate();
    } else {
      scene.scale(scene.mouseDX());
    }
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.alignTag();
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 150) : createShape(RECT, 0, 0, 150, 150);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.MouseDragInteraction"});
  }
}
