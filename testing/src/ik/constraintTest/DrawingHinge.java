package ik.constraintTest;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.event.MouseEvent;


public class DrawingHinge extends PApplet {
  Scene constraintScene, thetaScene, focus;
  Node constraintRoot, thetaRoot;

  int w = 900;
  int h = 500;
  int mode = -1;

  ThetaControl control;
  Joint j0, j1;
  static PFont font;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    font = createFont("Zapfino", 38);
    constraintScene = new Scene(this, P3D, w / 2, h);
    constraintScene.setType(Graph.Type.ORTHOGRAPHIC);
    constraintScene.fit(1);
    constraintRoot = new Node();
    constraintRoot.enableTagging(false);

    thetaScene = new Scene(this, P2D, w / 2, h, w / 2, 0);
    thetaScene.fit(1);
    thetaRoot = new Node();
    thetaRoot.enableTagging(false);

    //Create a Joint
    Joint.constraintFactor = 0.9f;
    j0 = new Joint(constraintScene, color(255), 0.1f * constraintScene.radius());
    j0.setReference(constraintRoot);
    j0.setRoot(true);
    j0.translate(-constraintScene.radius() * 0.5f, 0, 0);
    j1 = new Joint(constraintScene, color(255), 0.1f * constraintScene.radius());
    j1.setReference(j0);

    Vector v = new Vector(1f, 0, 0);
    v.normalize();
    v.multiply(constraintScene.radius());
    j1.translate(v);

    //Add constraint to joint j0
    Hinge constraint = new Hinge(radians(30), radians(30), j0.rotation(), new Vector(1, 0, 0), new Vector(0, 0, 1));
    j0.setConstraint(constraint);

    //Create controllers
    control = new ThetaControl(thetaScene, color(100, 203, 30));
    control.setReference(thetaRoot);
    control.setNames("Min", "Max");

    //Update controllers
    updateControllers(constraint, control);
  }

  public void draw() {
    handleMouse();
    drawScene(constraintScene, constraintRoot, "Constraint View");
    drawScene(thetaScene, thetaRoot, "Hinge Control");
    thetaScene.drawBullsEye(control);
    updateCostraint((Hinge) j0.constraint(), control);
    if (mode == 0) {
      j0.rotate(new Quaternion(new Vector(0, 0, 1), radians(1)));
    } else if (mode == 1) {
      j0.rotate(new Quaternion(new Vector(0, 0, 1), radians(-1)));
    }
  }

  public void updateCostraint(Hinge constraint, ThetaControl control) {
    if (control.modified()) {
      constraint.setMaxAngle(control.maxAngle());
      constraint.setMinAngle(control.minAngle());
      updateControllers(constraint, control);
      control.setModified(false);
    }
  }

  public void updateControllers(Hinge constraint, ThetaControl control) {
    control.update(constraint.minAngle(), constraint.maxAngle());
  }

  public void drawScene(Scene scene, Node root, String title) {
    scene.beginDraw();
    scene.context().background(0);
    scene.context().lights();
    scene.render(root);
    scene.beginHUD();
    scene.context().noLights();
    scene.context().pushStyle();
    scene.context().fill(255);
    scene.context().stroke(255);
    scene.context().textAlign(CENTER, CENTER);
    scene.context().textFont(font, 24);
    scene.context().text(title, scene.context().width / 2, 20);
    scene.context().noFill();
    scene.context().strokeWeight(3);
    scene.context().rect(0, 0, constraintScene.context().width, constraintScene.context().height);
    scene.context().popStyle();
    scene.endHUD();
    scene.endDraw();
    scene.display();
  }

  static class ThetaControl extends Node {

    int _color;
    float _min = 20, _max = 20;
    float _pmin = 20, _pmax = 20;
    boolean _modified = false;
    Scene _scene;


    Vector _initial, _end;
    String _min_name, _max_name;

    public ThetaControl(Scene scene, int color) {
      super();
      _scene = scene;
      _color = color;
      setPickingThreshold(0);
      setHighlighting(0);
    }

    public float maxAngle() {
      return _max;
    }

    public float minAngle() {
      return _min;
    }

    public void setNames(String min, String max) {
      _min_name = min;
      _max_name = max;
    }

    public void update(float min, float max) {
      _min = _pmin = min;
      _max = _pmax = max;
    }

    public boolean modified() {
      return _modified;
    }

    public void setModified(boolean modified) {
      _modified = modified;
    }

    @Override
    public void graphics(PGraphics pg) {
      pg.pushStyle();
      //Draw base according to each radius
      pg.fill(_color, _scene.node() == this ? 255 : 100);
      pg.noStroke();
      drawArc(pg, _scene.radius() * 0.7f, -_min, _max, 30);
      //draw semi-axe
      pg.fill(255);
      pg.stroke(255);
      pg.strokeWeight(3);
      pg.line(0, 0, _scene.radius() * 0.7f, 0);
      pg.ellipse(_scene.radius() * 0.7f, 0, 3, 3);

      pg.fill(255);
      pg.stroke(255);
      pg.ellipse(0, 0, 3, 3);

      if (_initial != null && _end != null) {
        pg.stroke(pg.color(255));
        pg.line(_initial.x(), _initial.y(), _end.x(), _end.y());
        pg.fill(pg.color(255, 0, 0));
        pg.noStroke();
        pg.ellipse(_initial.x(), _initial.y(), 5, 5);
        pg.ellipse(_end.x(), _end.y(), 5, 5);
        pg.fill(pg.color(255));
      }

      if (pg == _scene.context()) {
        _scene.beginHUD(pg);
        Vector min_position = _scene.screenLocation(new Vector(_scene.radius() * 0.7f * (float) Math.cos(-_min), _scene.radius() * 0.7f * (float) Math.sin(-_min)), this);
        Vector max_position = _scene.screenLocation(new Vector(_scene.radius() * 0.7f * (float) Math.cos(_max), _scene.radius() * 0.7f * (float) Math.sin(_max)), this);
        pg.fill(255);
        pg.textAlign(LEFT, CENTER);
        pg.textFont(font, 16);
        pg.text("\u03B8 " + _min_name, min_position.x() + 5, min_position.y());
        pg.text("\u03B8 " + _max_name, max_position.x() + 5, max_position.y());
        _scene.endHUD(pg);
        pg.popStyle();
      }
    }

    @Override
    public void interact(Object... gesture) {
      String command = (String) gesture[0];
      if (command.matches("Scale")) {
        if (_initial != null && _end != null) {
          //scale
          scale();
          _modified = true;
        }
        _initial = null;
        _end = null;
      } else if (command.matches("OnScaling")) {
        if (_initial == null) {
          //Get initial point
          _initial = _scene.location((Vector) gesture[1], this);
          _pmin = _min;
          _pmax = _max;
        } else {
          //Get final point
          _end = _scene.location((Vector) gesture[1], this);
          scale();
        }
      } else if (command.matches("Clear")) {
        _initial = null;
        _end = null;
      }
    }

    public void scale() {
      float angle = Vector.angleBetween(_initial, _end);
      angle *= Vector.cross(_initial, _end, null).dot(new Vector(0, 0, 1)) > 0 ? 1 : -1;

      //determine Which radius to scale
      if (_initial.y() > 0) {
        //Scale right radius
        _max = _pmax + angle;
        //Clamp
        _max = max(min(radians(180), _max), radians(5));
      } else {
        _min = _pmin - angle;
        //Clamp
        _min = max(min(radians(180), _min), radians(5));
      }
    }
  }


  public static void drawArc(PGraphics pGraphics, float radius, float minAngle, float maxAngle, int detail) {
    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    if (pGraphics.is3D()) {
      pGraphics.vertex(0, 0, 0);
    } else {
      pGraphics.vertex(0, 0);
    }
    float step = (maxAngle - minAngle) / detail;
    for (float theta = minAngle; theta < maxAngle; theta += step)
      pGraphics.vertex(radius * (float) Math.cos(theta), radius * (float) Math.sin(theta));
    pGraphics.vertex(radius * (float) Math.cos(maxAngle), radius * (float) Math.sin(maxAngle));
    pGraphics.endShape(PApplet.CLOSE);
  }


  public static class Joint extends Node {
    public static boolean depth = false;
    public static boolean markers = false;
    protected int _color;
    protected float _radius;
    public static boolean axes = true;
    public static float constraintFactor = 0.8f;
    //set to true only when the joint is the root (for rendering purposes)
    protected boolean _isRoot = false;
    protected Scene _scene;

    public Joint(Scene scene, int color, float radius) {
      super();
      _scene = scene;
      _color = color;
      _radius = radius;
      setPickingThreshold(-_radius * 2);
    }

    public Joint(Scene scene, int color) {
      this(scene, color, 5);
    }

    public Joint(Scene scene) {
      this(scene, scene.pApplet().color(scene.pApplet().random(0, 255), scene.pApplet().random(0, 255), scene.pApplet().random(0, 255)));
    }

    public Joint(Scene scene, float radius) {
      this(scene, scene.pApplet().color(scene.pApplet().random(0, 255), scene.pApplet().random(0, 255), scene.pApplet().random(0, 255)), radius);
    }


    @Override
    public void graphics(PGraphics pg) {
      if (!depth) pg.hint(PConstants.DISABLE_DEPTH_TEST);
      pg.pushStyle();
      if (!_isRoot) {
        pg.strokeWeight(Math.max(_radius / 4f, 2));
        pg.stroke(_color);
        Vector v = location(new Vector(), reference());
        float m = v.magnitude();
        if (_scene.is2D()) {
          pg.line(_radius * v.x() / m, _radius * v.y() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m);
        } else {
          pg.line(_radius * v.x() / m, _radius * v.y() / m, _radius * v.z() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m, (m - _radius) * v.z() / m);
        }
      }

      pg.fill(_color);
      pg.noStroke();
      if (_scene.is2D()) pg.ellipse(0, 0, _radius * 2, _radius * 2);
      else pg.sphere(_radius);

      pg.strokeWeight(_radius / 4f);
      if (constraint() != null) {
        drawConstraint(pg, constraintFactor);
      }
      if (!depth) pg.hint(PConstants.ENABLE_DEPTH_TEST);

      pg.stroke(255);
      //pg.strokeWeight(2);
      if (markers) _scene.drawBullsEye(this);

      pg.popStyle();
    }

    public void drawConstraint(PGraphics pGraphics, float factor) {
      if (this.constraint() == null) return;
      float boneLength = 0;
      if (!children().isEmpty()) {
        for (Node child : children())
          boneLength += child.translation().magnitude();
        boneLength = boneLength / (1.f * children().size());
      } else
        boneLength = translation().magnitude();
      if (boneLength == 0) return;

      pGraphics.pushMatrix();
      pGraphics.pushStyle();
      pGraphics.noStroke();

      pGraphics.fill(62, 203, 55, 150);
      Node reference = Node.detach(new Vector(), new Quaternion(), 1f);
      reference.setTranslation(new Vector());
      reference.setRotation(rotation().inverse());


      if (constraint() instanceof Hinge) {
        float radius = boneLength * factor;
        Hinge constraint = (Hinge) constraint();
        reference.rotate(constraint.orientation());
        reference.rotate(new Quaternion(new Vector(1, 0, 0), new Vector(0, 1, 0)));
        _scene.applyTransformation(pGraphics, reference);

        _scene.drawArc(pGraphics, radius, -constraint.minAngle(), constraint.maxAngle(), 30);

        //Draw axis
        pGraphics.pushStyle();
        pGraphics.fill(255, 154, 31);
        _scene.drawArrow(new Vector(), new Vector(radius / 2, 0, 0), 1f);
        pGraphics.fill(31, 132, 255);
        _scene.drawArrow(new Vector(), new Vector(0, 0, radius / 2), 1f);
        pGraphics.popStyle();


        //Write names

        Vector v = new Vector(radius * (float) Math.cos(-constraint.minAngle()) + 5, radius * (float) Math.sin(-constraint.minAngle()));
        //v = this.worldLocation(reference.worldLocation(v));
        //v = graph().screenLocation(v);

        Vector u = new Vector(radius * (float) Math.cos(constraint.maxAngle()) + 5, radius * (float) Math.sin(constraint.maxAngle()));
        //u = this.worldLocation(reference.worldLocation(u));
        //u = graph().screenLocation(u);

        Vector w = new Vector(radius / 2, 0, 0);
        //w = this.worldLocation(reference.worldLocation(w));
        //w = graph().screenLocation(w);

        Vector s = new Vector(0, 0, radius / 2);
        //s = this.worldLocation(reference.worldLocation(s));
        //s = graph().screenLocation(s);

        //((Scene) graph()).beginHUD(pGraphics);
        pGraphics.pushStyle();
        pGraphics.noLights();
        pGraphics.fill(255);
        pGraphics.textFont(font, 12);
        pGraphics.text("\u03B8 " + "min", v.x(), v.y());
        pGraphics.text("\u03B8 " + "max", u.x(), u.y());
        pGraphics.fill(255, 154, 31);
        pGraphics.text("Up vector", w.x() - 10, w.y() - 5, w.z());
        pGraphics.textAlign(RIGHT, BOTTOM);
        pGraphics.fill(31, 132, 255);
        pGraphics.text("Twist vector", s.x() - radius / 4, s.y(), s.z());
        pGraphics.lights();
        pGraphics.popStyle();
        //((Scene) graph()).endHUD(pGraphics);
      }
      pGraphics.popMatrix();
    }


    public void setRadius(float radius) {
      _radius = radius;
      setPickingThreshold(-_radius * 2);
    }

    public void setRoot(boolean isRoot) {
      _isRoot = isRoot;
    }

    public float radius() {
      return _radius;
    }

    public int color() {
      return _color;
    }
  }


  public void handleMouse() {
    Scene prev = focus;
    focus = mouseX < w / 2 ? constraintScene : thetaScene;
    if (prev != focus && prev != null) {
      if (prev.node() != null) prev.node().interact("Clear");
      if (focus != null && focus.node() != null) focus.node().interact("Clear");
    }

  }

  public void mouseMoved() {
    focus.mouseTag();
  }

  public void mouseDragged() {
    if (focus == thetaScene) {
      if (focus.node() != null) focus.node().interact("OnScaling", new Vector(focus.mouseX(), focus.mouseY()));
      return;
    }
    if (mouseButton == LEFT)
      focus.mouseSpin();
    else if (mouseButton == RIGHT) {
      focus.mouseTranslate();
    } else
      focus.moveForward(mouseX - pmouseX);
  }

  public void mouseReleased() {
    if (focus == thetaScene) {
      if (focus.node() != null) focus.node().interact("Scale");
      return;
    }
  }

  public void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
    //focus.zoom(event.getCount() * 50);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }


  public void keyPressed() {
    if (key == 'a' || key == 'A') {
      mode = 0;
    }

    if (key == 's' || key == 'S') {
      mode = 1;
    }

    if (key == 'd' || key == 'D') {
      mode = -1;
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.constraintTest.DrawingHinge"});
  }

}


