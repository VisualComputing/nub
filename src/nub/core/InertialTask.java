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

/**
 * Translate, Orbit & Rotate (Euler angles) damped task based on PeasyCam, see
 * <a href="https://github.com/jdf/peasycam/blob/master/src/peasy/DampedAction.java">DampedAction</a.
 * in turn based on a "damned clever and aesthetic idea by David Bollinger".
 */
// JS should go:
//class InertialTask extends Task {
abstract class InertialTask extends nub.processing.TimingTask {
  // orbit center:
  protected Vector _center = new Vector();
  protected float _inertia;
  protected float _x, _y, _z;

  /**
   * Sets inertia in [0..1].
   */
  void setInertia(float inertia) {
    float val = Math.abs(inertia);
    while (val > 1)
      val /= 10;
    if (val != inertia)
      System.out.println("Warning: inertia should be in [0..1]. Setting it as " + val);
    _inertia = val;
  }

  @Override
  public void execute() {
    _x *= _inertia;
    if (Math.abs(_x) < .001)
      _x = 0;
    _y *= _inertia;
    if (Math.abs(_y) < .001)
      _y = 0;
    _z *= _inertia;
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
