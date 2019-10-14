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
 * Interface defining timers.
 */
public interface Taskable {
  /**
   * Callback method which should be implemented by derived classes.
   */
  void execute();

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
  void run(long period);

  /**
   * Runs the task according to {@link #period()}. The timer may be scheduled for
   * repeated fixed-rate execution according to {@link #isRecurrent()}.
   */
  void run();

  /**
   * Deactivates the task. See {@link #isActive()}.
   */
  void stop();

  /**
   * Calls {@link #stop()} if the task {@link #isActive()}, and {@link #run()} otherwise.
   */
  void toggle();

  /**
   * Tells whether or not the timer is active.
   */
  boolean isActive();

  /**
   * Returns the task period in milliseconds.
   */
  long period();

  /**
   * Defines the task period in milliseconds.
   */
  void setPeriod(long period);

  /**
   * Toggles the task recurrence.
   */
  void toggleRecurrence();

  /**
   * Returns whether or not the task is scheduled to be executed recurrently.
   */
  boolean isRecurrent();
}