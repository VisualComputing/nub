/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package frames.ik;

import frames.timing.TimingTask;

/**
 * A Solver is a convenient class to solve IK problem
 * Given a Chain or a Tree Structure of Frames, this class will
 * solve the configuration that the frames must have to reach
 * a desired position
 */

public abstract class Solver {
  //TODO paper idea: optimize values per _solver / timer / local config
  public float error = 0.01f;
  public int maxIter = 200;
  public float minDistance = 0.01f;
  public float timesPerFrame = 1.f;
  public float frameCounter = 0;
  public int iterations = 0;

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

  public boolean solve() {
    //Reset counter
    if (_changed()) {
      _reset();
    }

    if (iterations == maxIter) {
      return true;
    }
    frameCounter += timesPerFrame;

    while (Math.floor(frameCounter) > 0) {
      //Returns a boolean that indicates if a termination condition has been accomplished
      if (_iterate()) {
        iterations = maxIter;
        break;
      } else iterations += 1;
      frameCounter -= 1;
    }
    //update positions
    _update();
    return false;
  }
}