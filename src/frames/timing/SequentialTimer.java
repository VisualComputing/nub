/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.timing;

/**
 * Sequential timers are single-threaded timers handled by a TimingHandler.
 */
public class SequentialTimer implements Timer {
  protected Taskable _task;
  protected TimingHandler _handler;
  protected boolean _active;
  protected boolean _once;
  private long _counter;
  private long _period;
  private long _startTime;

  /**
   * Defines a single shot sequential (single-threaded) timer.
   *
   * @param timingHandler timing handler owner
   */
  public SequentialTimer(TimingHandler timingHandler) {
    this(timingHandler, false, null);
  }

  /**
   * Defines a sequential (single-threaded) timer.
   *
   * @param timingHandler timing handler owner
   * @param singleShot
   */
  public SequentialTimer(TimingHandler timingHandler, boolean singleShot) {
    this(timingHandler, singleShot, null);
  }

  public SequentialTimer(TimingHandler timingHandler, Taskable task) {
    this(timingHandler, false, task);
  }

  public SequentialTimer(TimingHandler timingHandler, boolean singleShot, Taskable task) {
    _handler = timingHandler;
    _once = singleShot;
    _task = task;
    create();
  }

  @Override
  public Taskable timingTask() {
    return _task;
  }

  /**
   * Executes the callback method defined by the {@link #timingTask()}.
   *
   * <b>Note:</b> You should not call this method since it's done by the timing handler
   * (see {@link frames.timing.TimingHandler#handle()}).
   */
  protected boolean _execute() {
    boolean result = trigggered();
    if (result) {
      timingTask().execute();
      if (_once)
        inactivate();
    }
    return result;
  }

  @Override
  public void cancel() {
    stop();
    _handler.unregisterTask(this);
  }

  @Override
  public void create() {
    inactivate();
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
    inactivate();
    _counter = 1;
    _active = true;
    _startTime = System.currentTimeMillis();
  }

  @Override
  public void stop() {
    inactivate();
  }

  @Override
  public boolean isActive() {
    return _active;
  }

  // others

  /**
   * Deactivates the SequentialTimer.
   */
  public void inactivate() {
    _active = false;
  }

  /**
   * Returns {@code true} if the timer was triggered at the given frame.
   *
   * <b>Note:</b> You should not call this method since it's done by the timing handler
   * (see {@link frames.timing.TimingHandler#handle()}).
   */
  public boolean trigggered() {
    if (!_active)
      return false;

    long elapsedTime = System.currentTimeMillis() - _startTime;

    float timePerFrame = (1 / _handler.frameRate()) * 1000;
    long threshold = _counter * _period;

    boolean result = false;
    if (threshold >= elapsedTime) {
      long diff = elapsedTime + (long) timePerFrame - threshold;
      if (diff >= 0) {
        if ((threshold - elapsedTime) < diff) {
          result = true;
        }
      }
    } else {
      result = true;
    }

    if (result) {
      _counter++;
    }

    return result;
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
