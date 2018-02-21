package ik.common;

import common.InteractiveShape;
import frames.primitives.Frame;
import frames.primitives.Vector;
import frames.primitives.constraint.BallAndSocket;
import frames.primitives.constraint.PlanarPolygon;
import frames.primitives.constraint.SphericalPolygon;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;

import static ik.basic.BasicIK.*;

/**
 * Created by sebchaparr on 28/01/18.
 */
public class Joint extends InteractiveShape {
  private int color;

  public Joint(Scene scene) {
    this(scene, scene.pApplet().color(scene.pApplet().random(0, 255), scene.pApplet().random(0, 255), scene.pApplet().random(0, 255), 100));
  }

  public Joint(Scene scene, int color) {
    super(scene);
    this.color = color;
  }

  public void set(PGraphics pg) {
    pg.pushStyle();
    pg.fill(color);
    pg.strokeWeight(5);
    pg.stroke(color);
    if (reference() != null) {
      Vector v = this.coordinatesOfFrom(new Vector(), reference());
      if (pg.is2D())
        pg.line(0, 0, v.x(), v.y());
      else
        pg.line(0, 0, 0, v.x(), v.y(), v.z());
    }
    pg.popStyle();
    graph().drawAxes(5);

    if (constraint() != null) {
      pg.pushMatrix();
      pg.pushStyle();
      Frame reference = new Frame(new Vector(), rotation().inverse());
      if (constraint() instanceof BallAndSocket) {
        reference.rotate(((BallAndSocket) constraint()).getRestRotation());
        graph().applyTransformation(reference);
        drawCone(pg, boneLength / 2.f, (boneLength / 2.f) * PApplet.tan(PApplet.radians(constraint_factor_x)), (boneLength / 2.f) * PApplet.tan(PApplet.radians(constraint_factor_y)), 20);
      } else if (constraint() instanceof PlanarPolygon) {
        reference.rotate(((PlanarPolygon) constraint()).getRestRotation());
        graph().applyTransformation(reference);
        drawCone(pg, ((PlanarPolygon) constraint()).getHeight(), ((PlanarPolygon) constraint()).getVertices());
      } else if (constraint() instanceof SphericalPolygon) {
        reference.rotate(((SphericalPolygon) constraint()).getRestRotation());
        graph().applyTransformation(reference);
        drawCone(pg, ((SphericalPolygon) constraint()).getVertices(), boneLength);
      }
      pg.popStyle();
      pg.popMatrix();
    }

  }

  //Draw constraints
  public void drawCone(PGraphics pg, float height, float a, float b, int detail) {
    float x[] = new float[detail + 1];
    float y[] = new float[detail + 1];

    for (int i = 0; i <= detail; i++) {
      float theta = PApplet.TWO_PI * i / detail;
      float r = a * b / (float) (Math.sqrt(b * b * Math.cos(theta) * Math.cos(theta) + a * a * Math.sin(theta) * Math.sin(theta)));
      x[i] = r * (float) Math.cos(theta);
      y[i] = r * (float) Math.sin(theta);
    }
    pg.pushStyle();
    pg.noStroke();
    pg.fill(246, 117, 19, 80);
    pg.beginShape(PApplet.TRIANGLE_FAN);
    pg.vertex(0, 0, 0);
    for (int i = 0; i <= detail; i++) {
      pg.vertex(x[i], y[i], height);
    }
    pg.endShape(PApplet.CLOSE);
    pg.popStyle();
  }

  public void drawCone(PGraphics pg, float height, ArrayList<Vector> vertices) {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(246, 117, 19, 80);
    pg.beginShape(PApplet.TRIANGLE_FAN);
    pg.vertex(0, 0, 0);
    for (Vector v : vertices) {
      pg.vertex(v.x(), v.y(), height);
    }
    if (!vertices.isEmpty()) pg.vertex(vertices.get(0).x(), vertices.get(0).y(), height);
    pg.endShape();
    pg.popStyle();
  }

  public void drawCone(PGraphics pg, ArrayList<Vector> vertices, float scale) {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(246, 117, 19, 80);
    pg.beginShape(PApplet.TRIANGLE_FAN);
    pg.vertex(0, 0, 0);
    for (Vector v : vertices) {
      pg.vertex(scale * v.x(), scale * v.y(), scale * v.z());
    }
    if (!vertices.isEmpty())
      pg.vertex(scale * vertices.get(0).x(), scale * vertices.get(0).y(), scale * vertices.get(0).z());
    pg.endShape();
    pg.popStyle();
  }


}