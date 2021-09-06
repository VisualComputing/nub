package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class NoHint extends PApplet {
  Scene scene;
  int sty;

  public void settings() {
    size(1200, 1200, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    /*
    scene.setHUD((PGraphics pg) -> {
      pg.pushStyle();
      pg.stroke(255, 255, 0);
      pg.strokeWeight(5);
      pg.fill(255, 255, 0, 125);
      pg.rect(200, 400, 500, 300);
      pg.popStyle();
    });
    */
    scene.resetHint();
    for (int i = 0; i <= 10; i++) {
      Node node = new Node();
      scene.randomize(node);
      node.enableHint(Node.TORUS);
    }
    //scene.enableHint(Scene.AXES);
    //scene.enableHint(Scene.BACKGROUND, color(0));
    //scene.fit(1);
  }

  public void draw() {
    switch (sty) {
      case 0:
        background(125);
        scene.render();
        scene.drawAxes();
        stroke(0,255,0);
        scene.drawGrid();
        scene.beginHUD();
        pushStyle();
        stroke(255, 255, 0);
        strokeWeight(5);
        fill(255, 255, 0, 125);
        rect(200, 400, 500, 300);
        popStyle();
        scene.endHUD();
        break;
      case 1:
        background(125);
        scene.beginHUD();
        pushStyle();
        stroke(255, 255, 0);
        strokeWeight(5);
        fill(255, 255, 0, 125);
        rect(200, 400, 500, 300);
        popStyle();
        scene.endHUD();
        scene.render();
        scene.drawAxes();
        stroke(0,255,0);
        scene.drawGrid();
        break;
      case 2:
        scene.openContext();
        background(125);
        scene.drawAxes();
        stroke(0, 255, 0);
        scene.drawGrid();
        scene.render();
        scene.beginHUD();
        pushStyle();
        stroke(255, 255, 0);
        strokeWeight(5);
        fill(255, 255, 0, 125);
        rect(200, 400, 500, 300);
        popStyle();
        scene.endHUD();
        scene.closeContext();
        break;
      case 3:
        scene.openContext();
        background(125);
        scene.drawAxes();
        stroke(0, 255, 0);
        scene.drawGrid();
        scene.beginHUD();
        pushStyle();
        stroke(255, 255, 0);
        strokeWeight(5);
        fill(255, 255, 0, 125);
        rect(200, 400, 500, 300);
        popStyle();
        scene.endHUD();
        scene.render();
        scene.closeContext();
        break;
      case 4:
        scene.openContext();
        background(125);
        scene.drawAxes();
        stroke(0,255,0);
        scene.drawGrid();
        scene.beginHUD();
        pushStyle();
        stroke(255, 255, 0);
        strokeWeight(5);
        fill(255, 255, 0, 125);
        rect(200, 400, 500, 300);
        popStyle();
        scene.endHUD();
        scene.closeContext();
        scene.render();
        break;
      case 5:
        background(125);
        scene.render();
        scene.openContext();
        scene.drawAxes();
        stroke(0,255,0);
        scene.drawGrid();
        scene.beginHUD();
        pushStyle();
        stroke(255, 255, 0);
        strokeWeight(5);
        fill(255, 255, 0, 125);
        rect(200, 400, 500, 300);
        popStyle();
        scene.endHUD();
        scene.closeContext();
        break;
      case 6:
        background(125);
        scene.openContext();
        scene.drawAxes();
        stroke(0,255,0);
        scene.drawGrid();
        scene.beginHUD();
        pushStyle();
        stroke(255, 255, 0);
        strokeWeight(5);
        fill(255, 255, 0, 125);
        rect(200, 400, 500, 300);
        popStyle();
        scene.endHUD();
        scene.closeContext();
        break;
    }
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseShift();
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
  }

  public void keyPressed() {
    sty = Character.getNumericValue(key);
    println(sty);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.NoHint"});
  }
}
