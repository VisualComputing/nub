package nub.ik.visualization.visualsteps;

import nub.core.Node;
import nub.ik.visual.Joint;
import nub.ik.visualization.VisualStep;
import nub.processing.Scene;

import java.util.List;

public class HighLightStructure extends VisualStep {
  protected List<? extends Node> _structure;
  protected int _init = 0, _end = 255;
  float _transparency, _delta;

  public HighLightStructure(Scene scene, List<? extends Node> structure, long period, long stepDuration, long executionTimes, long renderingTimes) {
    super(scene, period, stepDuration, executionTimes, renderingTimes);
    _structure = structure;
  }

  public void setHighlight(int init, int end) {
    _init = init;
    _end = end;
  }

  @Override
  protected void _onTimeUpdate(int remainingTimes) {
    //Define delta value given current configuration and remaining times
    float remaining = (_end - _transparency);
    if (remainingTimes == 0) remainingTimes = 1;
    //Calculate deltas per frame
    _delta = remaining * 1.f / remainingTimes;
  }


  @Override
  public void _onInit() {
    int n = _structure.size();
    _completed = false;
    int color = (int) _attributes.get("highlight");
    for (int i = 0; i < n; i++) {
      if (_structure.get(i) instanceof Joint) {
        ((Joint) _structure.get(i)).setColor((int) _scene.context().red(color), (int) _scene.context().green(color), (int) _scene.context().blue(color));
      }
    }
    _transparency = _init;
  }

  @Override
  public void reverse() {

  }

  @Override
  public void _onComplete() {
    _keepDrawing = false;
    for (int i = 0; i < _structure.size(); i++) {
      if (_structure.get(i) instanceof Joint) {
        ((Joint) _structure.get(i)).setAlpha((int) _transparency);
      }
    }
  }

  @Override
  public void _onRunning() {
    for (int i = 0; i < _structure.size(); i++) {
      if (_structure.get(i) instanceof Joint) ((Joint) _structure.get(i)).setAlpha((int) _transparency);
    }
    _transparency += _delta;
  }

  @Override
  public void render() {
    //Do nothing
  }

  @Override
  protected void _defineAttributes() {
    //Do nothing
    _attributes.put("highlight", _scene.pApplet().color(255));
  }
}
