package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class Luxo extends PApplet {
  Scene scene;
  Node floor, base, arm, forarm, shade;

  public void settings() {
    size(1000, 700, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.enableHint(Scene.BACKGROUND);
    //scene.setShape(this::floor);
    scene.fit(1);
    // Constraints
    /*
    WorldConstraint baseConstraint = new WorldConstraint();
    baseConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, Vector.plusK);
    baseConstraint.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, Vector.plusK);
    LocalConstraint tipConstraint = new LocalConstraint();
    tipConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, Vector.zero);
    tipConstraint.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, Vector.plusI);
    LocalConstraint shadeConstraint = new LocalConstraint();
    shadeConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, Vector.zero);
    // */
    // Nodes
    base = new Node(this::base);
    //base.setConstraint(baseConstraint);
    arm = new Node(base, this::limb);
    arm.setPosition(0, 0, 8);
    arm.setOrientation(Quaternion.from(Vector.plusI, 0.6));
    //arm.setConstraint(tipConstraint);
    forarm = new Node(arm, this::limb);
    forarm.setPosition(0, 0, 50);
    forarm.setOrientation(Quaternion.from(Vector.plusI, -2));
    //forarm.setConstraint(tipConstraint);
    shade = new Node(forarm, this::shade);
    shade.setPosition(0, 0, 50);
    shade.setOrientation(Quaternion.from(new Vector(1, -0.3f, 0), -1.7));
    //shade.setConstraint(shadeConstraint);
    // for the lights to work the floor node should be the last to be added
    floor = new Node(this::floor);
    floor.tagging = false;
  }

  void cone(PGraphics pg, float zMin, float zMax, float r1, float r2, int nbSub) {
    pg.pushStyle();
    pg.noStroke();
    pg.translate(0, 0, zMin);
    Scene.drawCone(pg, nbSub, 0, 0, r1, r2, zMax - zMin);
    pg.translate(0, 0, -zMin);
    pg.popStyle();
  }

  void base(PGraphics pg) {
    pg.pushStyle();
    pg.fill(0, 0, 255);
    cone(pg, 0, 3, 15, 15, 30);
    cone(pg, 3, 5, 15, 13, 30);
    cone(pg, 5, 7, 13, 1, 30);
    cone(pg, 7, 9, 1, 1, 10);
    pg.popStyle();
  }

  void limb(PGraphics pg) {
    pg.pushStyle();
    pg.pushMatrix();
    pg.rotate(HALF_PI, 0, 1, 0);
    cone(pg, -5, 5, 2, 2, 20);
    pg.popMatrix();
    pg.translate(2, 0, 0);
    cone(pg, 0, 50, 1, 1, 10);
    pg.translate(-4, 0, 0);
    cone(pg, 0, 50, 1, 1, 10);
    pg.translate(2, 0, 0);
    pg.popStyle();
  }

  void shade(PGraphics pg) {
    pg.pushStyle();
    pg.fill(0, 255, 255);
    cone(pg, -2, 6, 4, 4, 30);
    cone(pg, 6, 15, 4, 17, 30);
    cone(pg, 15, 17, 17, 17, 30);
    pg.spotLight(155, 255, 255, 0, 0, 0, 0, 0, 1, THIRD_PI, 1);
    pg.popStyle();
  }

  void floor(PGraphics pg) {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(120, 120, 120);
    float nbPatches = 100;
    pg.normal(0, 0, 1);
    for (int j = 0; j < nbPatches; ++j) {
      pg.beginShape(QUAD_STRIP);
      for (int i = 0; i <= nbPatches; ++i) {
        pg.vertex((200 * (float) i / nbPatches - 100), (200 * j / nbPatches - 100));
        pg.vertex((200 * (float) i / nbPatches - 100), (200 * (float) (j + 1) / nbPatches - 100));
      }
      pg.endShape();
    }
    pg.popStyle();
  }

  public void draw() {
    lights();
    scene.render();
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    // no inertia for the nodes, but for the eye
    if (mouseButton == LEFT) {
      scene.mouseSpin(scene.node() != null ? 0 : 0.85f);
    } else if (mouseButton == RIGHT) {
      scene.mouseShift(scene.node() != null ? 0 : 0.85f);
    } else {
      scene.zoom(mouseX - pmouseX, scene.node() != null ? 0 : 0.85f);
    }
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Luxo"});
  }
}
