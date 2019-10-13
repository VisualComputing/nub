/******************************************************************************************
 * nub
 * Copyright (c) 2019 Universidad Nacional de Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ******************************************************************************************/

package nub.timing;

/**
 * Sequential timers are single-threaded timers handled by a TimingHandler.
 */
class SequentialTimer implements Timer {
  protected Task _task;
  protected TimingHandler _handler;
  protected boolean _active;
  protected boolean _once;
  private long _counter;
  private long _period;
  private long _startTime;

  /**
   * Defines a multiple shot sequential (single-threaded) timer. Same as
   * {@code this(timingHandler, task, false)}.
   *
   * @param timingHandler timing handler owner
   * @param task task to be executed
   * @see #SequentialTimer(TimingHandler, Task, boolean)
   */
  SequentialTimer(TimingHandler timingHandler, Task task) {
    this(timingHandler, task, false);
  }

  /**
   * Defines a single or multiple shot (depending on {@code singleShot}) sequential (single-threaded) timer.
   *
   * @param singleShot    defines a single or multiple shot timer
   * @param timingHandler timing handler owner
   * @param task          task to be executed
   * @see #SequentialTimer(TimingHandler, Task)
   */
  SequentialTimer(TimingHandler timingHandler, Task task, boolean singleShot) {
    _handler = timingHandler;
    _once = singleShot;
    _task = task;
    _active = false;
  }

  @Override
  public Task task() {
    return _task;
  }

  /**
   * Executes the callback method defined by the {@link #task()}.
   *
   * <b>Note:</b> You should not call this method since it's done by the timing handler
   * (see {@link nub.timing.TimingHandler#handle()}).
   */
  protected boolean _execute() {
    boolean result = false;
    if (_active) {
      long elapsedTime = System.currentTimeMillis() - _startTime;
      float timePerFrame = (1 / _handler.frameRate()) * 1000;
      long threshold = _counter * _period;
      if (threshold >= elapsedTime) {
        long diff = elapsedTime + (long) timePerFrame - threshold;
        if (diff >= 0)
          if ((threshold - elapsedTime) < diff)
            result = true;
      } else
        result = true;
      if (result)
        _counter++;
    }
    if (result) {
      task().execute();
      if (_once)
        _active = false;
    }
    return result;
  }

  @Override
  public void cancel() {
    stop();
    _handler.unregisterTask(this);
  }

  @Override
  public void run(long period) {
    setPeriod(period);
    run();
  }

  @Override
  public void run() {
    if (_period <= 0)
      return;
    _active = false;
    _counter = 1;
    _active = true;
    _startTime = System.currentTimeMillis();
  }

  @Override
  public void stop() {
    _active = false;
  }

  @Override
  public boolean isActive() {
    return _active;
  }

  // others

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
