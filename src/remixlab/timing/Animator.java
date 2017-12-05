/*******************************************************************************************
 * fpstiming_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.timing;

/**
 * Interface defining the behavior animated objects should implement.
 */
public interface Animator {
  /**
   * Main call back animated method.
   */
  void animate();

  /**
   * Returns the animation period in milliseconds.
   */
  long period();

  /**
   * Sets the animation period in milliseconds. Restarts the animation if it
   * already {@link #started()}.
   */
  void setPeriod(long period);

  /**
   * Stops the animation.
   */
  void stop();

  /**
   * Starts the animation executing periodically the animated call back method.
   */
  void start();

  /**
   * Simply calls {@link #stop()} and then {@link #start()}.
   */
  void restart();

  /**
   * Toggles the animation
   */
  void toggle();

  /**
   * Returns {@code true} if animation was started and {@code false} otherwise.
   */
  boolean started();

  /**
   * Returns the sequential timer.
   */
  SequentialTimer timer();
}
