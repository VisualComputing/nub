/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.ik;

import nub.core.Node;
import nub.timing.TimingTask;

/**
 * A Solver is a convenient class to solve IK problem
 * Given a Chain or a Tree Structure of Frames, this class will
 * solve the configuration that the frames must have to reach
 * a desired position
 */

public abstract class Solver {
  //TODO : Add visual hints to show how the solver's algorithm works.
  //TODO paper idea: optimize values per _solver / timer / local config
  public float error = 0.01f;
  public int maxIter = 50;
  public float minDistance = 0.01f;
  public float timesPerFrame = 5.f;
  public float frameCounter = 0;
  public int iterations = 0;
  public int final_iteration = 0; //TODO : Clean this!
  public boolean change_temp = false; //TODO : Clean this!

  protected TimingTask _task;

  public TimingTask task() {
    return _task;
  }

  public Solver() {
    _task = new TimingTask() {
      @Override
      public void execute() {
        solve();
      }
    };
  }

  /*Performs an Iteration of Solver Algorithm */
  protected abstract boolean _iterate();

  protected abstract void _update();

  protected abstract boolean _changed();

  protected abstract void _reset();

  public abstract float error();

  public void change(boolean change){
    change_temp = change;
  }

  public boolean solve() {
    //Reset counter
    if (_changed() || change_temp) {
      _reset();
      change_temp = false;
    }

    if (iterations >= maxIter) {
      return true;
    }
    frameCounter += timesPerFrame;

    while (Math.floor(frameCounter) > 0) {
      //Returns a boolean that indicates if a termination condition has been accomplished
      if (_iterate()) {
        final_iteration = iterations;
        iterations = maxIter;
        break;
      } else {
        final_iteration = iterations;
        iterations += 1;
      }
      frameCounter -= 1;
    }
    //update positions
    _update();
    return false;
  }

  public abstract void setTarget(Node endEffector, Node target);
}