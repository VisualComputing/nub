package ik.constraintTest;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class DrawingConstraint extends PApplet {
  Scene constraintScene, thetaScene, baseScene, focus;
  Node constraintRoot, thetaRoot, baseRoot;

  int w = 900;
  int h = 500;

  ThetaControl t_lr, t_ud;
  BaseControl base;
  Joint j0, j1, target;

  SimpleTRIK solver;
  boolean solve = false;

  static PFont font;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    font = createFont("Zapfino", 38);
    constraintScene = new Scene(this, P3D, w / 3, h);
    constraintScene.setType(Graph.Type.ORTHOGRAPHIC);
    constraintScene.fit(1);
    constraintRoot = new Node();
    constraintRoot.enableTagging(false);

    thetaScene = new Scene(this, P2D, w / 3, h, w / 3, 0);
    thetaScene.fit(1);
    thetaRoot = new Node();
    thetaRoot.enableTagging(false);

    baseScene = new Scene(this, P2D, w / 3, h, 2 * w / 3, 0);
    baseScene.fit(1);
    baseRoot = new Node();
    baseRoot.enableTagging(false);

    //Create a Joint
    Joint.constraintFactor = 0.9f;
    j0 = new Joint(constraintScene, color(255), 0.1f * constraintScene.radius());
    j0.setRoot(true);
    j0.setReference(constraintRoot);
    j0.translate(-constraintScene.radius() * 0.5f, 0, 0);
    j1 = new Joint(constraintScene, color(255), 0.1f * constraintScene.radius());
    j1.setReference(j0);

    Vector v = new Vector(1f, 0.f, 0);
    v.normalize();
    v.multiply(constraintScene.radius());
    j1.translate(v);
    j1.enableTagging(false);

    //Add constraint to joint j0
    BallAndSocket constraint = new BallAndSocket(radians(5), radians(34), radians(5), radians(84));
    constraint.setRestRotation(j0.rotation(), new Vector(0, 1, 0), new Vector(1, 0, 0), j1.translation());
    j0.setConstraint(constraint);

    List<Node> structure = new ArrayList<>();
    structure.add(j0);
    structure.add(j1);

    solver = new SimpleTRIK(structure, SimpleTRIK.HeuristicMode.FINAL);
    target = new Joint(constraintScene, color(255, 0, 0), 0.2f * constraintScene.radius());
    target.setRoot(true);
    target.setReference(constraintRoot);
    solver.setTarget(target);
    target.set(j1);

    solver.setTimesPerFrame(1);

    //Create controllers
    t_lr = new ThetaControl(thetaScene, color(255, 154, 31));
    t_lr.setReference(thetaRoot);
    t_lr.translate(-thetaScene.radius() * 0.3f, -thetaScene.radius() * 0.7f, 0);
    t_lr.setNames("Right", "Left");
    t_ud = new ThetaControl(thetaScene, color(31, 132, 255));
    t_ud.setReference(thetaRoot);
    t_ud.translate(-thetaScene.radius() * 0.3f, thetaScene.radius() * 0.8f, 0);
    t_ud.setNames("Down", "Up");
    base = new BaseControl(baseScene, color(100, 203, 30));
    base.setReference(baseRoot);
    //Update controllers
    updateControllers(constraint, t_lr, t_ud, base);
  }

  public void draw() {
    handleMouse();
    drawScene(constraintScene, constraintRoot, "Constraint View");
    drawScene(thetaScene, thetaRoot, "Side / Top View");
    drawScene(baseScene, baseRoot, "Front View");
    updateCostraint((BallAndSocket) j0.constraint(), t_lr, t_ud, base);
    if (solve) solver.solve();
  }

  public void updateCostraint(BallAndSocket constraint, ThetaControl lr, ThetaControl ud, BaseControl b) {
    if (lr.modified()) {
      constraint.setLeft(lr.maxAngle());
      constraint.setRight(lr.minAngle());
      updateControllers(constraint, lr, ud, b);
      lr.setModified(false);
    } else if (ud.modified()) {
      constraint.setUp(ud.maxAngle());
      constraint.setDown(ud.minAngle());
      ud.setModified(false);
      updateControllers(constraint, lr, ud, b);

    } else if (b.modified()) {
      constraint.setLeft(b.toAngle(b.left()));
      constraint.setRight(b.toAngle(b.right()));
      constraint.setUp(b.toAngle(b.up()));
      constraint.setDown(b.toAngle(b.down()));
      b.setModified(false);
      updateControllers(constraint, lr, ud, b);
    }
  }

  public void updateControllers(BallAndSocket constraint, ThetaControl lr, ThetaControl ud, BaseControl b) {
    lr.update(constraint.right(), constraint.left());
    ud.update(constraint.down(), constraint.up());
    b.update(constraint.left(), constraint.right(), constraint.up(), constraint.down());
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

  static class BaseControl extends Node {
    int _color;
    float _left = 74, _right = 0, _up = 0, _down = 32;
    float _pleft = 74, _pright = 0, _pup = 0, _pdown = 32;
    Vector _initial, _end;
    boolean _modified = false;
    float _max;
    float _max_tan;
    float _height;
    Scene _scene;

    public BaseControl(Scene scene, int color) {
      super();
      _scene = scene;
      _color = color;
      setPickingThreshold(0);
      setHighlighting(0);
      _max = _scene.radius() * 0.8f;
      _max_tan = tan(radians(70));
      _height = _max / _max_tan;
    }

    public boolean modified() {
      return _modified;
    }

    public void setModified(boolean modified) {
      _modified = modified;
    }

    public void update(float tl, float tr, float tu, float td) {
      _left = _pleft = _height * tan(tl);
      _right = _pright = _height * tan(tr);
      _up = _pup = _height * tan(tu);
      _down = _pdown = _height * tan(td);
    }

    public float toAngle(float l) {
      return atan2(l, _height);
    }

    public float left() {
      return _left;
    }

    public float right() {
      return _right;
    }

    public float up() {
      return _up;
    }

    public float down() {
      return _down;
    }

    @Override
    public void graphics(PGraphics pg) {
      pg.pushStyle();
      //Draw base according to each radius
      pg.fill(_color, _scene.node() == this ? 255 : 100);
      pg.noStroke();
      drawCone(pg, 64, 0, 0, 0, _left, _up, _right, _down, false);
      //draw semi-axes
      pg.fill(255, 0, 0);
      pg.stroke(255, 0, 0);
      pg.strokeWeight(3);
      pg.line(0, 0, -_left, 0);
      pg.line(0, 0, _right, 0);
      pg.ellipse(-_left, 0, 3, 3);
      pg.ellipse(_right, 0, 3, 3);

      pg.fill(0, 255, 0);
      pg.stroke(0, 255, 0);
      pg.line(0, 0, 0, _up);
      pg.line(0, 0, 0, -_down);
      pg.ellipse(0, _up, 3, 3);
      pg.ellipse(0, -_down, 3, 3);

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

      _scene.beginHUD(pg);
      Vector l = _scene.screenLocation(this.worldLocation(new Vector(-_left, 0)));
      Vector r = _scene.screenLocation(this.worldLocation(new Vector(_right, 0)));
      Vector u = _scene.screenLocation(this.worldLocation(new Vector(0, _up)));
      Vector d = _scene.screenLocation(this.worldLocation(new Vector(0, -_down)));

      pg.fill(255);
      pg.textFont(font, 16);
      pg.textAlign(RIGHT, CENTER);
      pg.text("Left", l.x() - 5, l.y());
      pg.textAlign(LEFT, CENTER);
      pg.text("Right", r.x() + 5, r.y());
      pg.textAlign(CENTER, TOP);
      pg.text("Up", u.x(), u.y());
      pg.textAlign(CENTER, BOTTOM);
      pg.text("Down", d.x(), d.y());

      _scene.endHUD(pg);
      pg.popStyle();
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
          _pleft = _left;
          _pright = _right;
          _pdown = _down;
          _pup = _up;
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
      float horizontal = _end.x() - _initial.x();
      float vertical = _end.y() - _initial.y();
      //determine Which radius to scale
      if (_initial.x() > 0) {
        //Scale right radius
        _right = _pright + horizontal;
        //Clamp
        _right = max(min(_scene.radius(), _right), 5);
      } else {
        _left = _pleft - horizontal;
        //Clamp
        _left = max(min(_scene.radius(), _left), 5);
      }
      if (_initial.y() > 0) {
        //Scale right radius
        _up = _pup + vertical;
        //Clamp
        _up = max(min(_scene.radius(), _up), 5);
      } else {
        _down = _pdown - vertical;
        //Clamp
        _down = max(min(_scene.radius(), _down), 5);
      }
    }
  }


  public static void drawCone(PGraphics pGraphics, int detail, float x, float y, float height, float left_radius, float up_radius, float right_radius, float down_radius, boolean is3D) {
    pGraphics.pushStyle();
    detail = detail % 4 != 0 ? detail + (4 - detail % 4) : detail;
    detail = Math.min(64, detail);

    float unitConeX[] = new float[detail + 1];
    float unitConeY[] = new float[detail + 1];

    int d = detail / 4;

    for (int i = 0; i <= d; i++) {
      float a1 = (PApplet.PI * i) / (2.f * d);
      unitConeX[i] = right_radius * (float) Math.cos(a1);
      unitConeY[i] = up_radius * (float) Math.sin(a1);
      unitConeX[i + d] = left_radius * (float) Math.cos(a1 + PApplet.HALF_PI);
      unitConeY[i + d] = up_radius * (float) Math.sin(a1 + PApplet.HALF_PI);
      unitConeX[i + 2 * d] = left_radius * (float) Math.cos(a1 + PApplet.PI);
      unitConeY[i + 2 * d] = down_radius * (float) Math.sin(a1 + PApplet.PI);
      unitConeX[i + 3 * d] = right_radius * (float) Math.cos(a1 + 3 * PApplet.PI / 2);
      unitConeY[i + 3 * d] = down_radius * (float) Math.sin(a1 + 3 * PApplet.PI / 2);
    }
    pGraphics.pushMatrix();
    pGraphics.translate(x, y);
    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    if (is3D) pGraphics.vertex(0, 0, 0);
    else pGraphics.vertex(0, 0);
    for (int i = 0; i <= detail; i++) {
      if (is3D) pGraphics.vertex(unitConeX[i], unitConeY[i], height);
      else pGraphics.vertex(unitConeX[i], unitConeY[i]);
    }
    pGraphics.endShape();
    pGraphics.popMatrix();
    pGraphics.popStyle();
  }

  static class ThetaControl extends Node {

    int _color;
    float _min = 20, _max = 20;
    float _pmin = 20, _pmax = 20;
    boolean _modified = false;

    Vector _initial, _end;
    String _min_name, _max_name;
    Scene _scene;

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
        _max = max(min(radians(80), _max), radians(5));
      } else {
        _min = _pmin - angle;
        //Clamp
        _min = max(min(radians(80), _min), radians(5));
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
        if (pg.is2D()) {
          pg.line(_radius * v.x() / m, _radius * v.y() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m);
        } else {
          pg.line(_radius * v.x() / m, _radius * v.y() / m, _radius * v.z() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m, (m - _radius) * v.z() / m);
        }
      }
      pg.fill(_color);
      pg.noStroke();
      if (pg.is2D()) pg.ellipse(0, 0, _radius * 2, _radius * 2);
      else pg.sphere(_radius);
      pg.strokeWeight(_radius / 4f);
      if (constraint() != null) {
        drawConstraint(_scene, pg, constraintFactor);
      }
      if (axes) Scene.drawAxes(pg, _radius * 2);
      if (!depth) pg.hint(PConstants.ENABLE_DEPTH_TEST);

      pg.stroke(255);
      //pg.strokeWeight(2);
      //if(markers) scene.drawBullsEye(this);
      pg.popStyle();

    }

    public void drawConstraint(Scene scene, PGraphics pGraphics, float factor) {

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

      if (constraint() instanceof BallAndSocket) {
        BallAndSocket constraint = (BallAndSocket) constraint();
        reference.rotate(((BallAndSocket) constraint()).orientation());
        scene.applyTransformation(pGraphics, reference);
        float width = boneLength * factor;
        float max = Math.max(Math.max(Math.max(constraint.up(), constraint.down()), constraint.left()), constraint.right());
        //Max value will define max radius length
        float height = (float) (width / Math.tan(max));
        if (height > boneLength * factor) height = width;
        //drawAxes(pGraphics,height*1.2f);
        //get all radius
        float up_r = (float) Math.abs((height * Math.tan(constraint.up())));
        float down_r = (float) Math.abs((height * Math.tan(constraint.down())));
        float left_r = (float) Math.abs((height * Math.tan(constraint.left())));
        float right_r = (float) Math.abs((height * Math.tan(constraint.right())));
        scene.drawCone(pGraphics, 20, 0, 0, height, left_r, up_r, right_r, down_r);
        //Draw Up - Down Triangle
        pGraphics.pushStyle();
        //Draw offset
        Quaternion q = Quaternion.compose(constraint.restRotation().inverse(), constraint.offset());
        q.compose(constraint.restRotation());
        Vector off = q.rotate(new Vector(0, 0, boneLength));
        pGraphics.stroke(255, 255, 0);
        pGraphics.line(0, 0, 0, off.x(), off.y(), off.z());
        pGraphics.noStroke();
        pGraphics.fill(255, 154, 31, 100);
        pGraphics.beginShape(PConstants.TRIANGLES);
        pGraphics.vertex(0, 0, 0);
        pGraphics.vertex(-left_r, 0, height);
        pGraphics.vertex(right_r, 0, height);
        pGraphics.endShape(CLOSE);
        pGraphics.popStyle();

        //Draw Left - Right Triangle
        pGraphics.pushStyle();
        pGraphics.fill(31, 132, 255, 100);
        pGraphics.beginShape(PConstants.TRIANGLES);
        pGraphics.vertex(0, 0, 0);
        pGraphics.vertex(0, -down_r, height);
        pGraphics.vertex(0, up_r, height);
        pGraphics.endShape(CLOSE);
        pGraphics.popStyle();

        //Write names
        pGraphics.pushStyle();
        pGraphics.noLights();
        pGraphics.fill(255);
        pGraphics.textFont(font, 12);
        pGraphics.textAlign(RIGHT);
        pGraphics.text("Left", -left_r, 0, height);
        pGraphics.textAlign(LEFT);
        pGraphics.text("Right", right_r, 0, height);
        pGraphics.textAlign(CENTER, BOTTOM);
        pGraphics.text("Down", 0, -down_r, height);
        pGraphics.textAlign(CENTER, TOP);
        pGraphics.text("Up", 0, up_r, height);
        pGraphics.textAlign(CENTER, CENTER);
        pGraphics.text("Offset", off.x(), off.y(), off.z());
        pGraphics.lights();
        pGraphics.popStyle();
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
    focus = mouseX < w / 3 ? constraintScene : mouseX < 2 * w / 3 ? thetaScene : baseScene;
    if (prev != focus && prev != null && prev.node() != null) {
      prev.node().interact("Clear");
      if (focus != null && focus.node() != null) focus.node().interact("Clear");
    }

  }

  public void mouseMoved() {
    focus.mouseTag();
  }

  public void mouseDragged() {
    if (focus == thetaScene || focus == baseScene) {
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
    if (focus == thetaScene || focus == baseScene) {
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
    if (key == 's' || key == 'S') {
      solver.solve();
    }
    if (key == 'w' || key == 'W') {
      solve = !solve;
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.constraintTest.DrawingConstraint"});
  }

}


