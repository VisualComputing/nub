package nub.ik.visualization.visualsteps;

import nub.core.Node;
import nub.ik.visualization.VisualStep;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;

public class FollowTrajectoryStep extends VisualStep {
  protected Node _reference;
  protected Vector[] _trajectory;
  protected Vector _current, _delta;
  protected int _idx = 0;

  public FollowTrajectoryStep(Scene scene, long period, long stepDuration, long executionTimes, long renderingTimes) {
    super(scene, period, stepDuration, executionTimes, renderingTimes);
  }

  public void setTrajectory(Node node, Vector... trajectory) {
    _reference = node;
    _trajectory = trajectory;
  }

  @Override
  protected void _onTimeUpdate(int remainingTimes) {
    //Define delta value given current configuration and remaining times
    int remainingTimesPerEdge = remainingTimes / _trajectory.length;
    Vector v1 = _current;
    Vector v2 = _trajectory[_idx + 1];
    _delta = Vector.subtract(v2, v1);
    if (remainingTimesPerEdge > 0) _delta.divide(remainingTimesPerEdge);
  }

  @Override
  public void _onInit() {
    _idx = 0;
    _current = _trajectory[_idx].get();
  }

  @Override
  public void reverse() {

  }

  @Override
  protected void _onComplete() {
    //Do nothing...
  }

  @Override
  protected void _onRunning() {
    Vector v2 = _trajectory[_idx + 1];
    if (Vector.distance(_current, v2) < 0.1) {
      _idx++;
      if (_idx == _trajectory.length - 1) {
        _idx--;
      } else {
        _current = v2;
        _onTimeUpdate(_totalExecutionTimes - (_times + 1));
      }
    } else {
      _current.add(_delta);
    }
  }

  @Override
  public void render() {
    Vector v1 = _trajectory[_idx];
    Vector v2 = _trajectory[_idx + 1 < _trajectory.length ? _idx + 1 : _idx];
    switch ((String) attributes().get("mode")) {
      case "V1_TO_V": {
        v2 = _current;
        break;
      }
      case "V_TO_V2": {
        v1 = _current;
        break;
      }
      case "V1_TO_V2": {
        break;
      }
      default: {
        throw new RuntimeException("mode attribute must have either of the following values:" +
            "V1_TO_V, V_TO_V2 or V1_TO_V2 ");
      }
    }

    float _radius = (float) _attributes.get("radius");
    int cv = (int) _attributes.get("v_color");
    int cv1 = (int) _attributes.get("v1_color");
    int cv2 = (int) _attributes.get("v2_color");
    int cline = (int) _attributes.get("line_color");

    PGraphics pg = _scene.context();
    pg.pushStyle();
    pg.pushMatrix();
    if (_reference != null) _scene.applyWorldTransformation(_reference);
    pg.hint(PConstants.DISABLE_DEPTH_TEST);
    if (_scene.is3D()) {
      pg.noStroke();
      pg.pushMatrix();
      pg.translate(_current.x(), _current.y(), _current.z());
      if (!_completed) pg.fill(cv2);
      else pg.fill(cv2, 150);
      pg.sphere(_radius);
      pg.popMatrix();
      pg.fill(cline);
      if (Vector.distance(v1, v2) > 1.2f * _radius) _scene.drawArrow(v1, v2, _radius / 4f);
    } else {
      drawSegment2D(pg, v1, v2, _radius, pg.color(cline, _completed ? 255 : 100), pg.color(cv1, _completed ? 255 : 100), pg.color(cv2, _completed ? 255 : 100));
      if (!_completed) pg.fill(cv);
      else pg.stroke(cv, 150);
      pg.ellipse(_current.x(), _current.y(), _radius, _radius);
    }
    pg.hint(PConstants.ENABLE_DEPTH_TEST);
    pg.popMatrix();
    pg.popStyle();
  }

  @Override
  protected void _defineAttributes() {
    _attributes.put("mode", "V1_TO_V"); //Choose among the following modes: V1_TO_V2, V_TO_V2, V1_TO_V
    _attributes.put("radius", _scene.radius() * 0.02f);
    _attributes.put("line_color", _scene.pApplet().color(255, 0, 0));
    _attributes.put("v1_color", _scene.pApplet().color(0, 0, 255));
    _attributes.put("v2_color", _scene.pApplet().color(0, 0, 255));
    _attributes.put("v_color", _scene.pApplet().color(0, 0, 255));
  }

  public static void drawSegment2D(PGraphics pg, Vector v1, Vector v2, float radius, int cline, int cv1, int cv2) {
    pg.pushStyle();
    pg.strokeWeight(radius / 2.f);
    pg.stroke(cline);
    pg.fill(cline);
    pg.line(v1.x(), v1.y(), v2.x(), v2.y());
    pg.strokeWeight(radius);
    pg.stroke(cv1);
    pg.fill(cv1);
    pg.ellipse(v1.x(), v1.y(), radius, radius);
    pg.stroke(cv2);
    pg.fill(cv2);
    pg.ellipse(v2.x(), v2.y(), radius, radius);
    pg.popStyle();
  }
}
