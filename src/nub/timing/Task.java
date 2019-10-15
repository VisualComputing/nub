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
 * <p>
 * Call {@link TimingHandler#unregisterTask(Task)} to cancel the task.
 */
abstract public class Task {
  protected boolean _active;
  protected boolean _recurrence;
  protected boolean _concurrence;
  protected long _counter;
  protected long _period;
  protected long _startTime;

  /**
   * Callback method which should be implemented by derived classes.
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
      if (_recurrence)
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
   * Runs the task according to {@link #period()}. The timer may be scheduled for
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
   * Calls {@link #stop()} if the task {@link #isActive()}, and {@link #run()} otherwise.
   */
  public void toggle() {
    if (isActive())
      stop();
    else
      run();
  }

  /**
   * Tells whether or not the timer is active.
   */
  public boolean isActive() {
    return _active;
  }

  /**
   * Returns the task period in milliseconds.
   */
  public long period() {
    return _period;
  }

  /**
   * Defines the task period in milliseconds.
   */
  public void setPeriod(long period) {
    _period = period;
  }

  /**
   * Toggles the task recurrence.
   *
   * @see #isRecurrent()
   */
  public void toggleRecurrence() {
    _recurrence = !_recurrence;
  }

  /**
   * Returns whether or not the task is scheduled to be executed recurrently.
   * <p>
   * If the task {@link #isRecurrent()}
   * it will be executed at fixed  time intervals defined with {@link #period()}.
   * If the task is not recurrent, it will be executed only once after delay of
   * {@link #period()}. The task {@link #isRecurrent()} by default.
   *
   * @see #isRecurrent()
   */
  public boolean isRecurrent() {
    return !_recurrence;
  }

  /**
   * Toggles the task concurrence.
   *
   * @see #isConcurrent()
   */
  public void toggleConcurrence() {
    System.out.println("Task can't be made recurrent. Recurrence should be implemented by derived classes.");
  }

  /**
   * Returns {@code true} if the task is concurrent, i.e., if it runs in parallel, and
   * {@code false} otherwise.
   * <p>
   * Task recurrence should be implemented by derived classes.
   *
   * @see #toggleConcurrence()
   */
  public boolean isConcurrent() {
    return _concurrence;
  }
}
