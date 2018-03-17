/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.processing;

import frames.timing.Taskable;
import frames.timing.Timer;

/**
 * Parrallel timer based on java.util.Timer and java.util.TimerTask.
 */
class ParallelTimer implements Timer {
  java.util.Timer _timer;
  java.util.TimerTask _timerTask;
  Taskable _task;
  boolean _once;
  boolean _active;
  long _period;

  /**
   * Same as {@code this(task, false)}.
   *
   * @see #ParallelTimer(Taskable, boolean)
   */
  public ParallelTimer(Taskable task) {
    this(task, false);
  }

  /**
   * Defines a parallel (multi-threaded) timer.
   *
   * @param task
   * @param singleShot
   */
  public ParallelTimer(Taskable task, boolean singleShot) {
    _once = singleShot;
    _task = task;
  }

  @Override
  public Taskable timingTask() {
    return _task;
  }

  @Override
  public void create() {
    stop();
    _timer = new java.util.Timer();
    _timerTask = new java.util.TimerTask() {
      public void run() {
        _task.execute();
      }
    };
  }

  @Override
  public void run(long period) {
    setPeriod(period);
    run();
  }

  @Override
  public void run() {
    create();
    if (isSingleShot())
      _timer.schedule(_timerTask, _period);
    else
      _timer.scheduleAtFixedRate(_timerTask, 0, _period);
    _active = true;
  }

  @Override
  public void cancel() {
    stop();
  }

  @Override
  public void stop() {
    if (_timer != null) {
      _timer.cancel();
      _timer.purge();
    }
    _active = false;
  }

  @Override
  public boolean isActive() {
    return _timer != null && _active;
  }

  @Override
  public long period() {
    return _period;
  }

  @Override
  public void setPeriod(long period) {
    _period = period;
  }

  @Override
  public boolean isSingleShot() {
    return _once;
  }

  @Override
  public void setSingleShot(boolean singleShot) {
    _once = singleShot;
  }
}
