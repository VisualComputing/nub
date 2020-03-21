package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class AdaptivePrecision2 extends PApplet {
  Scene scene;
  Node[] shapes;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.fit(1);
    shapes = new Node[25];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node() {
        @Override
        public void graphics(PGraphics pg) {
          Scene.drawAxes(pg, scene.radius() / 3);
          pg.pushStyle();
          pg.rectMode(CENTER);
          pg.fill(255, 0, 255);
          if (scene.is3D())
            Scene.drawCylinder(pg, 30, scene.radius() / 4, 200);
          else
            pg.rect(10, 10, 200, 200);
          pg.stroke(255, 255, 0);
          scene.drawSquaredBullsEye(this);
          pg.popStyle();
        }
      };
      scene.randomize(shapes[i]);
      shapes[i].setPickingThreshold(0.25f);
    }
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    for (int i = 0; i < shapes.length; i++) {
      pushMatrix();
      scene.applyTransformation(shapes[i]);
      scene.draw(shapes[i]);
      popMatrix();
      pushStyle();
      stroke(255);
      scene.drawSquaredBullsEye(shapes[i]);
      popStyle();
    }
  }

  public void keyPressed() {
    if (key == 's')
      scene.fit(1);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 50);
  }

  public void mouseClicked(MouseEvent event) {
    scene.removeTag();
    for (int i = 0; i < shapes.length; i++)
      if (scene.mouseTracks(shapes[i])) {
        scene.tag(shapes[i]);
        break;
      }
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 15) : createShape(RECT, 0, 0, 15, 15);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.AdaptivePrecision2"});
  }
}
