package frame;

import processing.core.*;
import remixlab.proscene.*;

/**
 * Created by pierre on 12/26/16.
 */
public class Button2D {
  Scene scene;
  InteractiveFrame iFrame;
  PVector position;
  String text =  new String();
  PFont font;
  float width;
  float height;

  public Button2D(Scene scn, PVector p, PFont f, String t) {
    scene = scn;
    iFrame = new InteractiveFrame(scene);
    iFrame.removeBindings();
    iFrame.disableVisualHint();
    iFrame.setFrontShape(this, "display");
    iFrame.setPickingShape(this, "highlight");
    iFrame.setHighlightingMode(InteractiveFrame.HighlightingMode.FRONT_PICKING_SHAPES);
    setPosition(p);
    setFont(f);
    setText(t);
  }

  public void setPosition(PVector pos) {
    position = pos;
  }

  public void setFont(PFont f) {
    this.font = f;
    update();
  }

  public void setText(String t) {
    this.text = t;
    update();
  }

  protected void update() {
    scene.pg().textAlign(PApplet.LEFT);
    scene.pg().textFont(font);
    width = scene.pg().textWidth(text);
    height = scene.pg().textAscent() + scene.pg().textDescent();
  }

  public void display(PGraphics pg) {
    pg.pushStyle();
    pg.fill(255);
    scene.beginScreenDrawing(pg);
    scene.pg().textFont(font);
    pg.text(text, position.x, position.y, width +1, height);
    scene.endScreenDrawing(pg);
    pg.popStyle();
  }

  public void highlight(PGraphics pg) {
    pg.noStroke();
    pg.fill(255, 0, 0, 126);
    scene.beginScreenDrawing(pg);
    pg.rect(position.x, position.y, width, height);
    scene.endScreenDrawing(pg);
  }
}
