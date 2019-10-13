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
 * Tasks are single-threaded recurrent callbacks defined by {@link #execute()}.
 * Tasks should be registered after instantiation calling {@link TimingHandler#registerTask(Task)}.
 * <p>
 * Call {@link #toggleRecurrence()} to toggle recurrence, i.e., the tasks
 * will only be executed once.
 */
abstract public class Task {
  protected boolean _active;
  protected boolean _once;
  private long _counter;
  private long _period;
  private long _startTime;

  /**
   * Callback method which should be implemented by derived classes.
   * Default implementation is empty.
   */
  abstract public void execute();

  /**
   * Executes the callback method defined by the {@link #execute()}.
   *
   * <b>Note:</b> You should not call this method since it's done by the timing handler
   * (see {@link nub.timing.TimingHandler#handle()}).
   */
  protected boolean _execute(float frameRate) {
    boolean result = false;
    if (_active) {
      long elapsedTime = System.currentTimeMillis() - _startTime;
      float timePerFrame = (1 / frameRate) * 1000;
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
      execute();
      if (_once)
        _active = false;
    }
    return result;
  }

  /**
   * Sets the task {@link #period()} and call {@link #run()}.
   * If task {@link #isRecurrent()} the {@link #execute()} method
   * will be invoked recurrently every {@link #period()} milliseconds;
   * otherwise it will be invoked once after a {@link #period()} delay
   * milliseconds.
   *
   * @see #run()
   * @see #isRecurrent()
   * @see #toggleRecurrence()
   * @see #period()
   * @see #setPeriod(long)
   */
  public void run(long period) {
    setPeriod(period);
    run();
  }

  /**
   * Runs the timer according to {@link #period()}. The timer may be scheduled for
   * repeated fixed-rate execution according to {@link #isRecurrent()}.
   */
  public void run() {
    if (_period <= 0)
      return;
    _active = true;
    _counter = 1;
    _startTime = System.currentTimeMillis();
  }

  /**
   * Deactivates the task. See {@link #isActive()}.
   */
  public void stop() {
    _active = false;
  }

  /**
   * Tells whether or not the timer is active.
   */
  public boolean isActive() {
    return _active;
  }

  // others

  // TODO find better name: merge period and delay
  // maybe setPeriod and setDelay
  public long period() {
    return _period;
  }

  public void setPeriod(long period) {
    _period = period;
  }

  public void toggleRecurrence() {
    _once = !_once;
  }

  public boolean isRecurrent() {
    return !_once;
  }
}
