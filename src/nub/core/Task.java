/***************************************************************************************
 * nub
 * Copyright (c) 2019-2021 Universidad Nacional de Colombia
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.core;

/**
 * Tasks are (non)recurrent (see {@link #isRecurrent()}) callbacks
 * defined by overridden {@link #execute()}.
 * <p>
 * A task runs (see {@link #run(long)}) at a certain {@link #period()}
 * which define the interval duration between two consecutive executions
 * (see also {@link #frequency()}). Do not use the task for drawing
 * since it will not necessarily run every frame.
 */
class Task {
  @FunctionalInterface
  public interface Callback {
    void execute();
  }

  protected Callback _callback;
  protected boolean _active;
  protected boolean _recurrence;
  protected long _counter;
  protected long _period;
  protected long _startTime;

  /**
   * Constructs a sequential recurrent task that will execute {@code callback}
   * (see {@link #setCallback(Callback)}) with a {@link #period()} of 40ms
   * (i.e., a {@link #frequency()} of 25 Hz).
   */
  public Task(Callback callback) {
    _recurrence = true;
    _period = 40;
    setCallback(callback);
  }

  /**
   * Constructs a sequential recurrent task with a {@link #period()} of 40ms
   * (i.e., a {@link #frequency()} of 25 Hz). The task {@link #execute()} is
   * set as its {@link #callback()} method.
   */
  public Task() {
    _recurrence = true;
    _period = 40;
    setCallback(this::execute);
  }

  /**
   * Sets the task callback method.
   * <p>
   * The callback will be executed (see {@link #run(long)}) at a certain
   * {@link #period()}. Do not implement drawing in your callback since
   * it will not necessarily be executed every frame.
   *
   * @see #callback()
   * @see #execute()
   */
  public void setCallback(Callback callback) {
    _callback = callback;
  }

  /**
   * Returns the task callback method.
   *
   * @see #setCallback(Callback)
   */
  public Callback callback() {
    return _callback;
  }

  /**
   * Callback method which should be implemented by derived classes.
   * Otherwise simply use {@link #setCallback(Callback)}.
   * <p>
   * The task will be executed (see {@link #run(long)}) at a certain
   * {@link #period()}. Do not implement this method for drawing
   * since it will not necessarily be executed every frame.
   *
   * @see #setCallback(Callback)
   * @see #callback()
   */
  public void execute() {
  }

  /**
   * Executes the callback method defined by the {@link #execute()}.
   *
   * <b>Note:</b> This method is called by the timing handler
   * (see {@link Graph#_update()}).
   */
  public boolean _execute() {
    boolean result = false;
    if (_active) {
      long elapsedTime = System.nanoTime() - _startTime;
      float timePerFrame = (1 / Graph.frameRate) * 1e9f;
      long threshold = _counter * _period * (long) 1e6;
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
      if (_callback != null)
        _callback.execute();
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
   * @see #enableRecurrence(boolean)
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
    _startTime = System.nanoTime();
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
   * Same as {@code setPeriod(period() + delta)}.
   *
   * @see #setPeriod(long)
   * @see #setFrequency(float)
   */
  public void increasePeriod(long delta) {
    setPeriod(period() + delta);
  }

  /**
   * Defines the task {@link #period()} in milliseconds.
   *
   * @see #period()
   * @see #setFrequency(float)
   */
  public void setPeriod(long period) {
    if (period <= 0) {
      System.out.println("Task period not set as it should have non-negative value");
      return;
    }
    _period = period;
    float target = frequency();
    if (Graph.frameRate < target) {
      System.out.println("Warning: Your task period of " + period + " ms requires at least a " + target + " Hz frameRate, " +
          "but currently it just achieves " + Graph.frameRate + " Hz." + '\n' + "Either set a period of at least "
          + 1000 / Graph.frameRate + " ms or call enableConcurrence() to execute the task concurrently.");
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
    return 1000 / (float) _period;
  }

  /**
   * Same as {@code enableRecurrence(true)}.
   *
   * @see #disableRecurrence()
   * @see #enableRecurrence(boolean)
   * @see #isRecurrent()
   */
  public void enableRecurrence() {
    enableRecurrence(true);
  }

  /**
   * Same as {@code enableRecurrence(false)}.
   *
   * @see #enableRecurrence()
   * @see #enableRecurrence(boolean)
   * @see #isRecurrent()
   */
  public void disableRecurrence() {
    enableRecurrence(false);
  }

  /**
   * Enables or (disables) the task recurrence according to {@code enable}.
   * Non-recurrent tasks execute only once using {@link #period()} as their execution delay.
   *
   * @see #isRecurrent()
   * @see #enableRecurrence()
   * @see #disableRecurrence()
   */
  public void enableRecurrence(boolean enable) {
    boolean isActive = isActive();
    stop();
    _recurrence = enable;
    if (isActive)
      run();
  }

  /**
   * Returns whether or not the task is scheduled to be executed recurrently.
   * <p>
   * A recurrent task is periodically executed at fixed duration time intervals
   * (see {@link #period()} and {@link #setPeriod(long)}). A non-recurrent task
   * will only be executed once just after a delay of {@link #period()} ms.
   * <p>
   * Tasks are recurrent by default, see {@link #Task()}.
   *
   * @see #enableRecurrence(boolean)
   */
  public boolean isRecurrent() {
    return _recurrence;
  }
}
