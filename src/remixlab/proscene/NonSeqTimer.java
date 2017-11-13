/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import remixlab.timing.Taskable;
import remixlab.timing.Timer;

/**
 * Non-seq _timer based on java.util.Timer and java.util.TimerTask.
 */
class NonSeqTimer implements Timer {
  Scene _scene;
  java.util.Timer _timer;
  java.util.TimerTask _timerTask;
  Taskable _task;
  boolean _once;
  boolean _active;
  long _period;

  public NonSeqTimer(Scene scn, Taskable o) {
    this(scn, o, false);
  }

  public NonSeqTimer(Scene scn, Taskable o, boolean singleShot) {
    _scene = scn;
    _once = singleShot;
    _task = o;
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
