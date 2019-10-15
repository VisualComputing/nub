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
 * Tasks are (non)recurrent, (non)concurrent (see {@link #isRecurrent()}
 * and {@link #isConcurrent()} resp.) callbacks defined by overridden
 * {@link #execute()}.
 * <p>
 * Note that concurrence of the task execution (which requires a separate
 * execution thread) should be implemented by derived classes, i.e., this
 * class just implements the task sequential api.
 * <p>
 * Call {@link TimingHandler#unregisterTask(Task)} to cancel the task.
 */
abstract public class Task {
  protected TimingHandler _timingHandler;
  protected boolean _active;
  protected boolean _recurrence;
  protected boolean _concurrence;
  protected long _counter;
  protected long _period;
  protected long _startTime;

  /**
   * Constructs a sequential recurrent task with a {@link #period()} of 40ms
   * (i.e., a {@link #frequency()} of 25 Hz).
   */
  public Task(TimingHandler timingHandler) {
    _timingHandler = timingHandler;
    _timingHandler.registerTask(this);
    _recurrence = true;
    _period = 40;
  }

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
      if (!_recurrence)
        _active = false;
    }
    return result;
  }

  /**
   * Sets the task {@link #period()} in milliseconds and call {@link #run()}.
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
   * Returns the task period (the task execution interval duration) in milliseconds.
   * Non-recurrent tasks execute only once taking this value as their execution delay.
   * Default value is 40 ms.
   *
   * @see #setPeriod(long)
   * @see #frequency()
   */
  public long period() {
    return _period;
  }

  /**
   * Defines the task {@link #period()} in milliseconds.
   *
   * @see #period()
   * @see #setFrequency(float)
   */
  public void setPeriod(long period) {
    _period = Math.abs(period);
    float target = frequency();
    if (!isConcurrent() && _timingHandler.frameRate() < target) {
      System.out.println("Warning: Your task period of " + period + " ms requires at least a " + target + " Hz frameRate, " +
          "but currently it just achieves " + _timingHandler.frameRate() + " Hz." + '\n' + "Either set a period of at least "
          + 1000 / _timingHandler.frameRate() + " ms or call toggleConcurrence() to execute the task concurrently.");
    }
  }

  /**
   * Defines the task {@link #frequency()} in Hz.
   *
   * @see #frequency()
   * @see #setPeriod(long)
   */
  public void setFrequency(float frequency) {
    setPeriod((long) (1000 / frequency));
  }

  /**
   * Returns the task execution frequency in milliseconds. Default value is 25 Hz.
   *
   * @see #setFrequency(float)
   * @see #period()
   */
  public float frequency() {
    return 1000 / _period;
  }

  /**
   * Toggles the task recurrence. Non-recurrent tasks execute only once
   * using {@link #period()} as their execution delay.
   *
   * @see #isRecurrent()
   */
  public void toggleRecurrence() {
    boolean isActive = isActive();
    stop();
    _recurrence = !_recurrence;
    if (isActive)
      run();
    System.out.println("Task made " + (_recurrence ? "recurrent" : "non-recurrent"));
  }

  /**
   * Returns whether or not the task is scheduled to be executed recurrently.
   * <p>
   * A recurrent task (see {@link #isRecurrent()}) is periodically executed
   * at fixed duration time intervals (see {@link #period()} and
   * {@link #setPeriod(long)}). A non-recurrent task will only be executed once
   * just after a delay of {@link #period()} ms.
   *
   * Tasks are recurrent by default, see {@link #Task(TimingHandler)}.
   *
   * @see #toggleRecurrence()
   * @see #isConcurrent()
   */
  public boolean isRecurrent() {
    return !_recurrence;
  }

  /**
   * Toggles the task concurrence.
   *
   * @see #isConcurrent()
   * @see #toggleRecurrence()
   */
  public void toggleConcurrence() {
    System.out.println("Task can't be made concurrent. Concurrence should be implemented by derived classes.");
  }

  /**
   * Returns {@code true} if the task is concurrent, i.e., if it runs in parallel, and
   * {@code false} otherwise.
   * <p>
   * Task recurrence should be implemented by derived classes.
   *
   * @see #toggleConcurrence()
   * @see #isRecurrent()
   */
  public boolean isConcurrent() {
    return _concurrence;
  }
}
