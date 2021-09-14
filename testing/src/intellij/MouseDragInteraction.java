package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.function.BiFunction;

/**
 * Created by pierre on 11/15/16.
 */
public class MouseDragInteraction extends PApplet {
  Scene scene;
  Vector randomVector;
  boolean cad, lookAround;
  Node shape1, shape2, shape3;

  public void settings() {
    size(1200, 600, P3D);
  }

  public void setup() {
    rectMode(CENTER);

    scene = new Scene(this, 1500);
    //scene.togglePerspective();
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
    shape1.setOrientation(Quaternion.random());
    shape1.translate(-375, 175, 0);
    shape1.setScalingFilter(scalarFilter, new Object[] { (float) height });

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
    shape2.setTranslationFilter(Node.translationAxisFilter, new Object[] { Vector.plusI });
    shape2.setScalingFilter(scalarFilter, new Object[] { (float) height });

    shape3 = new Node();
    shape3.detach();
    shape3.set(shape1);
    scene.randomize(shape3);
    shape3.setShape(shape());
    shape3.setReference(shape2);
    shape3.enableHint(Node.AXES);
    shape3.setTranslationFilter(Node.translationAxisFilter, new Object[] { Vector.plusJ });
    shape3.setScalingFilter(scalarFilter, new Object[] { (float) height });
    randomVector = Vector.random();
    randomVector.setMagnitude(scene.radius() * 0.5f);
  }

  public void draw() {
    // render scene nodes (shapes simply get drawn)
    background(0);
    scene.render();
    scene.drawAxes();
    scene.beginHUD();
    pushStyle();
    stroke(255, 255, 0);
    strokeWeight(5);
    fill(255, 255, 0, 125);
    rect(pixelX, pixelY, 2 * h, h);
    popStyle();
    scene.endHUD();
    fill(0, 255, 255);
    scene.drawArrow(randomVector);
  }

  static BiFunction<Node, Object[], Float> scalarFilter = (node, params) -> {
    float delta = node.cacheTargetScaling * 20;
    float factor = 1 + Math.abs(delta) / (float)params[0];
    float scl = delta >= 0 ? factor : 1 / factor;
    float bottom = 0.5f;
    float top = 2;
    if((delta < 0 && node.magnitude() < bottom) || (delta > 0 && node.magnitude() > top))
      return 1.0f;
    return scl;
  };

  public void keyPressed() {
    if (key == 'e') {
      if (shape3 != null)
        shape3.setReference(shape1);
    }
    if (key == 'x') {
      shape3.detach();
    }
    if (key == 'y') {
      if (shape3 != null)
        shape3.resetReference();
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
      println(shape2.worldOrientation().toString());
      //Quaternion q = shape2.worldDisplacement(new Quaternion());
      Quaternion q = shape2.worldDisplacement(shape2.orientation());
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
    if (key == 'w') {
      shape2.setPosition(new Vector());
    }
    if (key == 'z') {
      shape3.setWorldPosition(new Vector());
    }
  }

  int pixelX = 300, pixelY = 400, h = 100;

  @Override
  public void mouseMoved() {
    scene.tag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      if (cad) {
        //scene.mouseRotateCAD(randomVector);
        scene.cad();
      } else if (lookAround) {
        scene.lookAround();
      } else {
        scene.spin();
      }
    else if (mouseButton == RIGHT) {
      if (scene.node() == shape2) {
        Vector vector = scene.displacement(new Vector(scene.mouseDX(), scene.mouseDY(), 0), shape2);
        Vector axis = Vector.plusI;
        // uncomment to express the filter axis in the world coordinate system
        // axis = shape2.displacement(axis);
        shape2.translate(vector, 0);
      }
      else if (scene.node() == null) {
        Node _node = scene.eye().copy();
        _node.detach();
        _node.setWorldPosition(scene.center());
        Vector vector = scene.displacement(new Vector(scene.mouseDX(), scene.mouseDY(), 0), _node);
        vector = scene.eye().displacement(vector, _node);
        Vector axis = Vector.plusI;
        // uncomment to express the filter axis in the world coordinate system
        // axis = scene.eye().displacement(axis);
        scene.eye().translate(vector, 0.8f);
      }
      /*
      else {
        scene.mouseShift();
      }
      // */
    } else {
      scene.zoom(scene.mouseDX());
      //scene.turn(scene.mouseRADX(), 0, 0);
    }
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.node() != null) {
      scene.node().scale((float) event.getCount());
    }
    else
      scene.moveForward(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
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
