public class Info {
  PVector position;
  String text;
  PFont font;
  boolean highlight;
  float width;
  float height;

  public Info(PVector p, PFont f) {
    this(p, f, "");
  }

  public Info(PVector p, PFont f, String t) {
    setPosition(p);
    this.font = f;
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

  public void display() {
    pushStyle();
    fill(255);
    scene.beginScreenDrawing();
    textFont(font);
    text(text, position.x, position.y, width + 1, height);
    scene.endScreenDrawing();
    popStyle();
    if(highlight)
      highlight();
  }

  public void highlight() {
    pushStyle();
    noStroke();
    fill(255, 255, 255, 96);
    scene.beginScreenDrawing();
    rect(position.x, position.y, width, height);
    scene.endScreenDrawing();
    popStyle();
  }
}
