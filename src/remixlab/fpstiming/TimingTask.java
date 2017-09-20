/**************************************************************************************
 * fpstiming_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.fpstiming;

/**
 * An abstract wrapper class holding a {@link #timer()} together with its call back method
 * ( {@link remixlab.fpstiming.Taskable#execute()}) which derived classes should
 * implement.
 */
public abstract class TimingTask implements Taskable {
  protected Timer tmr;

  /**
   * Returns the timer instance.
   */
  public Timer timer() {
    return tmr;
  }

  /**
   * Sets the timer instance.
   */
  public void setTimer(Timer t) {
    tmr = t;
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
