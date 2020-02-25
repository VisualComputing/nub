/***************************************************************************************
 * nub
 * Copyright (c) 2019-2020 Universidad Nacional de Colombia
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.core;

import nub.primitives.Vector;
import nub.timing.Task;

/**
 * Translate, Orbit & Rotate (Euler angles) damped task based on PeasyCam, see
 * <a href="https://github.com/jdf/peasycam/blob/master/src/peasy/DampedAction.java">DampedAction</a.
 * in turn based on a "damned clever and aesthetic idea by David Bollinger".
 */
abstract class DampedTask extends Task {
  // original friction is 0.16
  protected Vector _center = new Vector();
  protected float _damp = 1.0f - 0.2f; // 1 - friction
  protected float _x, _y, _z;

  DampedTask(Graph graph) {
    super(graph.timingHandler());
  }

  @Override
  public void execute() {
    _x *= _damp;
    if (Math.abs(_x) < .001)
      _x = 0;
    _y *= _damp;
    if (Math.abs(_y) < .001)
      _y = 0;
    _z *= _damp;
    if (Math.abs(_z) < .001)
      _z = 0;
    if (_x == 0 && _y == 0 && _z == 0)
      stop();
    else
      action();
  }

  /**
   * Callback method for translate, orbit & rotate damped actions.
   */
  abstract void action();
}
