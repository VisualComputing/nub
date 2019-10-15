/******************************************************************************************
 * nub
 * Copyright (c) 2019 Universidad Nacional de Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ******************************************************************************************/

package nub.processing;

import nub.timing.Task;

/**
 * Parallel task based on java.util.Timer and java.util.TimerTask.
 */
public abstract class TimingTask extends Task {
  java.util.Timer _timer;
  java.util.TimerTask _timerTask;

  public TimingTask(Scene scene) {
    scene.registerTask(this);
  }

  @Override
  public void run() {
    if (isConcurrent()) {
      stop();
      _timer = new java.util.Timer();
      _timerTask = new java.util.TimerTask() {
        public void run() {
          execute();
        }
      };
      if (_recurrence)
        _timer.schedule(_timerTask, _period);
      else
        _timer.scheduleAtFixedRate(_timerTask, 0, _period);
      _active = true;
    } else
      super.run();
  }

  @Override
  public void stop() {
    if (isConcurrent()) {
      if (_timer != null) {
        _timer.cancel();
        _timer.purge();
      }
      _active = false;
    } else
      super.stop();
  }

  @Override
  public boolean isActive() {
    return isConcurrent() ? _timer != null && _active : super.isActive();
  }

  @Override
  public void toggleConcurrence() {
    _concurrence = !_concurrence;
  }
}
