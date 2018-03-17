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
 * An abstract wrapper class holding a {@link #timer()} together with its call back method
 * ( {@link frames.timing.Taskable#execute()}) which derived classes should
 * implement.
 */
public abstract class TimingTask implements Taskable {
  protected Timer _timer;

  /**
   * Returns the timer instance.
   */
  public Timer timer() {
    return _timer;
  }

  /**
   * Sets the timer instance.
   */
  public void setTimer(Timer t) {
    _timer = t;
  }

  // Wrappers

  /**
   * Timer wrapper method.
   */
  public void run(long period) {
    if (timer() != null) {
      timer().setSingleShot(false);
      timer().run(period);
    }
  }

  /**
   * Timer wrapper method.
   */
  public void runOnce(long period) {
    if (timer() != null) {
      timer().setSingleShot(true);
      timer().run(period);
    }
  }

  /**
   * Timer wrapper method.
   */
  public void stop() {
    if (timer() != null) {
      timer().stop();
    }
  }

  /**
   * Timer wrapper method.
   */
  public void cancel() {
    if (timer() != null) {
      timer().cancel();
    }
  }

  /**
   * Timer wrapper method.
   */
  public void create() {
    if (timer() != null) {
      timer().create();
    }
  }

  /**
   * Timer wrapper method.
   */
  public boolean isActive() {
    if (timer() != null) {
      return timer().isActive();
    }
    return false;
  }

  /**
   * Timer wrapper method.
   */
  public long period() {
    return timer().period();
  }
}
