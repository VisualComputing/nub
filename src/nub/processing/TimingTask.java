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

import java.util.Timer;
import java.util.TimerTask;

/**
 * Tasks are (non)recurrent, (non)concurrent (see {@link #isRecurrent()}
 * and {@link #isConcurrent()} resp.) callbacks defined by overridden
 * {@link #execute()}.
 * <p>
 * This class implements both the task's sequential and parallel apis.
 * <p>
 * Call {@link Scene#unregisterTask(Task)} to cancel the task.
 */
public abstract class TimingTask extends Task {
  java.util.Timer _timer;
  java.util.TimerTask _timerTask;

  /**
   * Constructs a sequential recurrent task with a {@link #period()} of 40ms
   * (i.e., a {@link #frequency()} of 25 Hz).
   */
  public TimingTask(Scene scene) {
    super(scene.timingHandler());
  }

  @Override
  public void run() {
    if (isConcurrent()) {
      stop();
      _timer = new Timer();
      _timerTask = new TimerTask() {
        public void run() {
          execute();
        }
      };
      if (isRecurrent()) {
        _timer.scheduleAtFixedRate(_timerTask, 0, _period);
        _active = true;
      } else {
        _timer.schedule(_timerTask, _period);
        _active = false;
      }
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
  public void enableConcurrence(boolean enable) {
    boolean isActive = isActive();
    stop();
    _concurrence = enable;
    if (isActive)
      run();
  }
}
