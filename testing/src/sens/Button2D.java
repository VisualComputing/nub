package sens;

import remixlab.bias.*;
import remixlab.bias.event.*;
import remixlab.proscene.*;
import processing.core.*;

/**
 * Created by pierre on 12/26/16.
 */
public abstract class Button2D extends GrabberObject {
  Sensitivities parent;
  public Scene scene;
  String myText;
  PFont myFont;
  float myWidth;
  float myHeight;
  PVector position;

  public Button2D(Scene scn, PVector p, PFont font) {
    this(scn, p, font, "");
  }

  public Button2D(Scene scn, PVector p, PFont font, String t) {
    scene = scn;
    parent = (Sensitivities) scn.pApplet();
    position = p;
    myFont = font;
    parent.textFont(myFont);
    parent.textAlign(PApplet.LEFT);
    setText(t);
    scene.motionAgent().addGrabber(this);
  }

  public void setText(String text) {
    myText = text;
    myWidth = parent.textWidth(myText);
    myHeight = parent.textAscent() + parent.textDescent();
  }

  public void display() {
    parent.pushStyle();
    parent.fill(255);
    if (grabsInput(scene.motionAgent()))
      parent.fill(255);
    else
      parent.fill(100);
    scene.beginScreenDrawing();
    parent.text(myText, position.x, position.y, myWidth+1, myHeight);
    scene.endScreenDrawing();
    parent.popStyle();
  }

  @Override
  public boolean checkIfGrabsInput(DOF2Event event) {
    float x = event.x();
    float y = event.y();
    return ((position.x <= x) && (x <= position.x + myWidth) && (position.y <= y) && (y <= position.y + myHeight));
  }
}
