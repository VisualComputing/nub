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
 * Interface defining timers.
 */
public interface Timer {
  /**
   * Calls {@link #setPeriod(long)} followed by {@link #run()}.
   *
   * @param period time in milliseconds between successive task executions
   */
  public void run(long period);

  /**
   * Runs the timer according to {@link #period()}. The timer may be scheduled for
   * repeated fixed-rate execution according to {@link #isSingleShot()}.
   */
  public void run();

  /**
   * Returns the object defining the timer callback method. May be null.
   */
  public Taskable timingTask();

  /**
   * Stops the timer.
   */
  public void stop();

  /**
   * Stops the timer.
   */
  public void cancel();

  /**
   * Creates the timer.
   */
  public void create();

  /**
   * Tells whether or not the timer is active.
   */
  public boolean isActive();

  /**
   * Returns the timer period in milliseconds.
   */
  public long period();

  /**
   * Defines the timer period in milliseconds.
   */
  public void setPeriod(long period);

  /**
   * Returns whether or not the timer is scheduled to be executed only once.
   */
  public boolean isSingleShot();

  /**
   * Defines the timer as a single shot or for repeated execution.
   */
  public void setSingleShot(boolean singleShot);
}
