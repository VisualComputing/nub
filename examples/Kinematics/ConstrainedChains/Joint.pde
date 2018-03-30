/**
 * Joint.
 * by Sebastian Chaparro.
 *
 * This class implements a shape behavior which requires
 * overriding the interact(Event) method.
 * Furthermore, it overrides  set(PGraphics) method to
 * draw a skeleton.
 *
 * Feel free to copy paste it.
 */

public class Joint extends OrbitShape  {
  private int colour;
  private boolean root = false;

  public Joint(Scene scene) {
    this(scene, color(scene.pApplet().random(0, 255), scene.pApplet().random(0, 255), scene.pApplet().random(0, 255)));
  }

  public Joint(Scene scene, int colour) {
    super(scene);
    this.colour = colour;
  }

  /* Whenever the joint is the root use this method to specify it
  otherwise it will be drawn an additional bone */
  public void setRoot(boolean root){
    this.root = root;
  }

  /* Draws the bone that connects the previous Joint with this Joint
  and the kind of constraint that is used by this Joint. */
  public void set(PGraphics pg) {
    pg.pushStyle();
    pg.fill(colour);
    pg.noStroke();
    if (pg.is2D()) pg.ellipse(0, 0, 3, 3);
    else pg.sphere(3);

    if(!root) {
      pg.strokeWeight(5);
      pg.stroke(colour);
      Vector v = localCoordinatesOf(new Vector());
      if (pg.is2D()) {
        pg.line(0, 0, v.x(), v.y());
      } else {
        pg.line(0, 0, 0, v.x(), v.y(), v.z());
      }
      pg.popStyle();
    }

    if (constraint() != null) {
      pg.pushMatrix();
      pg.pushStyle();
      Frame reference = new Frame(new Vector(), rotation().inverse());
      if (constraint() instanceof BallAndSocket) {
        BallAndSocket constraint = (BallAndSocket) constraint();
        reference.rotate(constraint.restRotation());
        graph().applyTransformation(reference);
        graph().drawAxes(5);
        drawCone(pg, boneLength / 2.f, (boneLength / 2.f) * PApplet.tan(PApplet.radians(constraint.left())), (boneLength / 2.f) * PApplet.tan(PApplet.radians(constraint.down())), 20);
      } else if (constraint() instanceof PlanarPolygon) {
        reference.rotate(((PlanarPolygon) constraint()).restRotation());
        graph().applyTransformation(reference);
        graph().drawAxes(5);
        drawCone(pg, ((PlanarPolygon) constraint()).height(), ((PlanarPolygon) constraint()).vertices());
      } else if (constraint() instanceof SphericalPolygon) {
        reference.rotate(((SphericalPolygon) constraint()).restRotation());
        graph().applyTransformation(reference);
        graph().drawAxes(5);
        drawCone(pg, ((SphericalPolygon) constraint()).vertices(), boneLength);
      } else if (constraint() instanceof Hinge) {
        Hinge constraint = (Hinge) constraint();
        if(children().size() == 1){
          Vector axis = constraint.restRotation().rotate(constraint.axis());
          reference.rotate(constraint.restRotation());
          Vector rest = Vector.projectVectorOnPlane(rotation().inverse().rotate(children().get(0).translation()), axis);
          //Align Z-Axis with Axis
          reference.rotate(new Quaternion(new Vector(0,0,1), axis));
          //Align X-Axis with rest Axis
          reference.rotate(new Quaternion(new Vector(1,0,0), reference.rotation().inverse().rotate(rest)));
          graph().applyTransformation(reference);
          graph().drawAxes(5);
          drawArc(pg, boneLength/2.f, -constraint.minAngle(), constraint.maxAngle(), 10);
        }
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

  public void drawArc(PGraphics pg, float radius, float minAngle, float maxAngle, int detail){
    pg.pushStyle();
    pg.noStroke();
    pg.fill(246, 117, 19, 80);
    pg.beginShape(PApplet.TRIANGLE_FAN);
    pg.vertex(0,0,0);
    float step = (maxAngle - minAngle)/detail;
    for(float theta = minAngle; theta <= maxAngle; theta += step) {
      pg.vertex(radius * cos(theta), radius * sin(theta));
    }
    pg.endShape(PApplet.CLOSE);
    pg.popStyle();
  }
}