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

package nub.processing;

import nub.timing.Task;
import nub.timing.TimingHandler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Timing tasks are (non)recurrent, (non)concurrent
 * (see {@link #isRecurrent()} and {@link #isConcurrent()} resp.)
 * callbacks defined by overridden {@link #execute()}.
 * <p>
 * A timing task runs (see {@link #run(long)}) at a certain
 * {@link #period()} which define the interval duration between
 * two consecutive executions (see also {@link #frequency()}).
 * Do not use the task for drawing since it will not necessarily
 * run every frame.
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

  /**
   * Constructs a sequential recurrent task with a {@link #period()} of 40ms
   * (i.e., a {@link #frequency()} of 25 Hz).
   */
  public TimingTask(TimingHandler timingHandler) {
    super(timingHandler);
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
  public void setPeriod(long period) {
    if (!isConcurrent()) {
      super.setPeriod(period);
      return;
    }
    if (period <= 0) {
      System.out.println("Task period not set as it should have non-negative value");
      return;
    }
    boolean active = isActive();
    if (active)
      stop();
    _period = period;
    if (active)
      run();
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
