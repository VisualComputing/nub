import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Setting up a viewer orthogonal to the current view helps
 * out to appreciate current objects depth within a scene
 */
public class OrthogonalViewer extends PApplet {
  Scene scene, visualHint, focus;
  int w = 500, h = 500, atX, atY;
  Node[] models;

  Vector orig = new Vector();
  Vector dir = new Vector();
  Vector end = new Vector();
  Vector pup;

  @Override
  public void settings() {
    size(1800, 1400, P3D);
  }

  public void setup() {
    scene = new Scene(this, 1000);
    scene.fit(1000);
    models = new Node[100];
    for (int i = 0; i < models.length; i++) {
      models[i] = new Node(boxShape());
      scene.randomize(models[i]);
    }
    visualHint = new Scene(createGraphics(w, h, P3D), 300);
  }

  public void draw() {
    focus = pup != null ? visualHint.hasFocus() ? visualHint : scene : scene;
    background(0);
    drawRay();
    scene.drawAxes();
    scene.render();
    if (pup != null) {
      visualHint.openContext();
      visualHint.context().background(125);
      visualHint.render();
      visualHint.closeContext();
      visualHint.image(atX, atY);
    }
  }

  void drawRay() {
    PGraphics pg = scene.context();
    if (pup != null) {
      pg.pushStyle();
      pg.strokeWeight(20);
      pg.stroke(255, 255, 0);
      pg.point(pup.x(), pup.y(), pup.z());
      pg.strokeWeight(8);
      pg.stroke(0, 0, 255);
      pg.line(orig.x(), orig.y(), orig.z(), end.x(), end.y(), end.z());
      pg.popStyle();
    }
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 1)
      scene.focus();
    else
      scene.align();
  }

  PShape boxShape() {
    PShape box = createShape(BOX, 60);
    box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return box;
  }

  @Override
  public void mouseMoved(MouseEvent event) {
    if (event.isControlDown()) {
      pup = scene.location();
      if (pup != null) {
        visualHint.setCenter(pup);
        visualHint.setRadius(300);
        visualHint.eye().setWorldPosition(pup);
        //hint.setViewDirection(scene.displacement(Vector.plusJ));
        visualHint.setViewDirection(scene.displacement(new Vector(0, 1, 0)));
        visualHint.setUpVector(scene.displacement(new Vector(0, 0, -1)));
        visualHint.fit();
        atX = mouseX - w / 2;
        atY = mouseY - h;
        // debug
        scene.pixelToLine(orig, dir);
        end = Vector.add(orig, Vector.multiply(dir, 4000.0f));
      }
    } else {
      focus.tag();
    }
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.spin();
    else if (mouseButton == RIGHT)
      focus.shift();
    else
      focus.moveForward(focus.mouseDX());
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    focus.zoom(event.getCount() * 20);
  }

  @Override
  public void keyPressed() {
    if (key == ' ')
      scene.togglePerspective();
    if (key == 'f')
      Scene.leftHanded = !Scene.leftHanded;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"OrthogonalViewer"});
  }
}
