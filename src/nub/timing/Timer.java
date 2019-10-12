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
public interface Timer {
  /**
   * Calls {@link #setPeriod(long)} followed by {@link #run()}.
   *
   * @param period time in milliseconds between successive task executions
   */
  void run(long period);

  /**
   * Runs the timer according to {@link #period()}. The timer may be scheduled for
   * repeated fixed-rate execution according to {@link #isSingleShot()}.
   */
  void run();

  /**
   * Returns the object defining the timer callback method. May be null.
   */
  Task task();

  /**
   * Stops the timer.
   */
  void stop();

  /**
   * Stops the timer.
   */
  void cancel();

  /**
   * Creates the timer.
   */
  void create();

  /**
   * Tells whether or not the timer is active.
   */
  boolean isActive();

  /**
   * Returns the timer period in milliseconds.
   */
  long period();

  /**
   * Defines the timer period in milliseconds.
   */
  void setPeriod(long period);

  /**
   * Returns whether or not the timer is scheduled to be executed only once.
   */
  boolean isSingleShot();

  /**
   * Defines the timer as a single shot or for repeated execution.
   */
  void setSingleShot(boolean singleShot);
}
