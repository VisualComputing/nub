// This class shows an alternative way of using an InteractiveFrame by deriving from it.

public class Patch extends InteractiveFrame {
  private int number; // patch number
  private float size; // patch size in world
  private float padding = 0f; // space arround the patch
  private Board board; // reference to the board containing the patch
  private PImage img; // image for the patch (if null, the patch number will be rendered)

  public Patch(int number, float size, PImage img, Scene scene, Board board) {
    super(scene);
    this.number = number;
    this.size = size;
    this.img = img;
    this.board = board;
    removeBindings();
    disableVisualHint();
    setHighlightingMode(InteractiveFrame.HighlightingMode.NONE);
    setShape("display");
    setClickBinding(LEFT, 1, "play");
  }

  public void display(PGraphics pg) {
    pg.pushStyle();
    // set the appropriate fill and stroke weight based on the result of grabsMouse()
    if (grabsInput()) {
      pg.fill(250, 250, 60);
      pg.stroke(200, 200, 100);
      pg.strokeWeight(3);
    } else {
      if (number % 2 == 0) {
        pg.fill(250, 250, 255);
      } else {
        pg.fill(100, 100, 255);
      }
      pg.stroke(150, 150, 255);
      pg.strokeWeight(2);
    }
    // draw patch faces
    float thickness = 6;
    pg.beginShape();
    pg.vertex(-getSize() / 2, -getSize() / 2, -thickness);
    pg.vertex(getSize() / 2, -getSize() / 2, -thickness);
    pg.vertex(getSize() / 2, getSize() / 2, -thickness);
    pg.vertex(-getSize() / 2, getSize() / 2, -thickness);
    pg.endShape(CLOSE);
    pg.beginShape();
    pg.vertex(-getSize() / 2, -getSize() / 2, 0);
    pg.vertex(-getSize() / 2, getSize() / 2, 0);
    pg.vertex(-getSize() / 2, getSize() / 2, -thickness);
    pg.vertex(-getSize() / 2, -getSize() / 2, -thickness);
    pg.endShape(CLOSE);
    pg.beginShape();
    pg.vertex(getSize() / 2, -getSize() / 2, 0);
    pg.vertex(getSize() / 2, getSize() / 2, 0);
    pg.vertex(getSize() / 2, getSize() / 2, -thickness);
    pg.vertex(getSize() / 2, -getSize() / 2, -thickness);
    pg.endShape(CLOSE);
    pg.beginShape();
    pg.vertex(-getSize() / 2, -getSize() / 2, 0);
    pg.vertex(getSize() / 2, -getSize() / 2, 0);
    pg.vertex(getSize() / 2, -getSize() / 2, -thickness);
    pg.vertex(-getSize() / 2, -getSize() / 2, -thickness);
    pg.endShape(CLOSE);
    pg.beginShape();
    pg.vertex(-getSize() / 2, getSize() / 2, 0);
    pg.vertex(getSize() / 2, getSize() / 2, 0);
    pg.vertex(getSize() / 2, getSize() / 2, -thickness);
    pg.vertex(-getSize() / 2, getSize() / 2, -thickness);
    pg.endShape(CLOSE);
    // draw front face
    pg.textureMode(NORMAL);
    pg.beginShape();
    if (img != null) { // we've got an image, set it as a texture
      if ( grabsInput() )
        pg.fill(250, 250, 0);
      else
        pg.fill(10);
      pg.texture(img);
      pg.vertex(-getSize() / 2, -getSize() / 2, 0, 0, 0);
      pg.vertex(-getSize() / 2, getSize() / 2, 0, 0, 1);
      pg.vertex(getSize() / 2, getSize() / 2, 0, 1, 1);
      pg.vertex(getSize() / 2, -getSize() / 2, 0, 1, 0);
    } else { // no image, just another face without a texture
      pg.vertex(-getSize() / 2, -getSize() / 2, 0);
      pg.vertex(-getSize() / 2, getSize() / 2, 0);
      pg.vertex(getSize() / 2, getSize() / 2, 0);
      pg.vertex(getSize() / 2, -getSize() / 2, 0);
    }
    pg.endShape(CLOSE);
    if (img == null) { // no image, render the patch number
      pg.textFont(font2, 100 / board.getSize());
      pg.strokeWeight(1);
      pg.fill(0, 0, 20);
      pg.text("" + number, -10, 10, 0.1);
    }
    pg.popStyle();
  }

  public void play(ClickEvent event) {
    board.movePatch(this);
  }

  public float getSize() {
    return size;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }
}