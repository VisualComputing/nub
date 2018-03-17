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
