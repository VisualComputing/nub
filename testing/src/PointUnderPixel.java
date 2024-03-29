import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Pick a point under pixel using the scene location method
 */
public class PointUnderPixel extends PApplet {
  Scene scene;
  Node[] models;

  Vector orig = new Vector();
  Vector dir = new Vector();
  Vector end = new Vector();
  Vector pup;

  // offScreen breaks reading depths in Processing
  // try offScreen = false to see how it should work
  boolean offScreen = false;

  @Override
  public void settings() {
    size(800, 800, P3D);
    //noSmooth();
  }

  public void setup() {
    scene = offScreen ? new Scene(createGraphics(width, height, P3D), 1000) : new Scene(this, 1000);
    scene.fit(1000);
    models = new Node[100];
    for (int i = 0; i < models.length; i++) {
      models[i] = new Node(boxShape());
      scene.randomize(models[i]);
    }
    if (offScreen) {
      scene.context().hint(PConstants.ENABLE_BUFFER_READING);
    }
  }

  public void draw() {
    background(0);
    scene.render();
    scene.drawAxes();
    stroke(0,255,0);
    scene.drawGrid();
    drawRay();
  }

  void drawRay() {
    if (pup != null) {
      pushStyle();
      strokeWeight(20);
      stroke(255, 255, 0);
      point(pup.x(), pup.y(), pup.z());
      strokeWeight(8);
      stroke(0, 0, 255);
      line(orig.x(), orig.y(), orig.z(), end.x(), end.y(), end.z());
      popStyle();
    }
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getButton() == RIGHT) {
      pup = scene.location();
      if (pup != null) {
        scene.pixelToLine(orig, dir);
        end = Vector.add(orig, Vector.multiply(dir, 4000.0f));
      }
    } else {
      if (event.getCount() == 1)
        scene.focus();
      else
        scene.align();
    }
  }

  PShape boxShape() {
    PShape box = createShape(BOX, 60);
    box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return box;
  }

  @Override
  public void mouseMoved() {
    scene.tag();
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.shift();
    else
      scene.moveForward(scene.mouseDX());
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    scene.zoom(event.getCount() * 20);
  }

  @Override
  public void keyPressed() {
    if (key == ' ')
      scene.togglePerspective();
    if (key == 'f')
      Scene.leftHanded = !Scene.leftHanded;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"PointUnderPixel"});
  }
}
