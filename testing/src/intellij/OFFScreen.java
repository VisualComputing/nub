package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class OFFScreen extends PApplet {
  Scene scene;
  Node shape1, shape2;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(800, 600, renderer);
  }

  public void setup() {
    // TODO render at 400, 200
    //scene = new Scene(this, createGraphics(1300, 900, renderer));
    scene = new Scene(createGraphics(800, 600, P3D));
    scene.enableHint(Scene.BACKGROUND | Scene.AXES);
    scene.configHint(Scene.BACKGROUND, color(0));
    scene.setFOV(PI / 3);
    scene.setRadius(300);
    scene.fit(1);

    shape1 = new Node() {
      @Override
      public void graphics(PGraphics pGraphics) {
        Scene.drawAxes(pGraphics, scene.radius() / 3);
        pGraphics.pushStyle();
        pGraphics.rectMode(CENTER);
        pGraphics.fill(255, 0, 255);
        if (scene.is3D())
          Scene.drawCone(pGraphics, 30, 90);
        else
          pGraphics.rect(10, 10, 200, 200);
        pGraphics.popStyle();
      }
    };
    shape1.setRotation(Quaternion.random());
    shape1.translate(-55, -55, -55);
    //shape1.setPickingPolicy(Node.SHAPE);
    shape1.enableHint(Node.BULLSEYE);

    shape2 = new Node(shape1);
    shape2.enableHint(Node.BULLSEYE);
    shape2.setShape(shape());
    shape2.translate(-55, -85, 135);
    //shape2.setPickingPolicy(Node.SHAPE);
  }

  public void draw() {
    scene.display(50, 50);
  }

  @Override
  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.alignTag();
  }

  public void keyPressed() {
    if (key == 'f')
      Scene.leftHanded = !Scene.leftHanded;
    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 35) : createShape(RECT, 0, 0, 40, 40);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.OFFScreen"});
  }
}
