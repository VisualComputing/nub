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
 * Class implementing the main {@link frames.timing.Animator} behavior.
 */
public class AnimatorObject implements Animator {
  protected SequentialTimer _animationTimer;
  protected boolean _started;
  protected long _animationPeriod;
  protected TimingHandler _handler;

  /**
   * Constructs an animated object with a default {@link #period()} of 40
   * milliseconds (25Hz).
   */
  public AnimatorObject(TimingHandler handler) {
    _handler = handler;
    _handler.registerAnimator(this);
    _animationTimer = new SequentialTimer(_handler);
    setPeriod(40); // 25Hz
    stop();
  }

  @Override
  public SequentialTimer timer() {
    return _animationTimer;
  }

  /**
   * Return {@code true} when the animation loop is started.
   * <p>
   * The timing handler will check when {@link #started()} and then called the
   * animation callback method every {@link #period()} milliseconds.
   * <p>
   * Use {@link #start()} and {@link #stop()}.
   *
   * @see #start()
   * @see #animate()
   */
  @Override
  public boolean started() {
    return _started;
  }

  /**
   * The animation loop period, in milliseconds. When {@link #started()}, this is
   * the delay that takes place between two consecutive iterations of the animation loop.
   * <p>
   * This delay defines a target frame rate that will only be achieved if your
   * {@link #animate()} methods is fast enough.
   * <p>
   * Default value is 40 milliseconds (25 Hz).
   *
   * @see #setPeriod(long)
   */
  @Override
  public long period() {
    return _animationPeriod;
  }

  @Override
  public void setPeriod(long period) {
    if (period > 0) {
      _animationPeriod = period;
      if (started())
        restart();
    }
  }

  /**
   * Stops animation.
   *
   * @see #started()
   */
  @Override
  public void stop() {
    _started = false;
    if (timer() != null)
      timer().stop();
  }

  /**
   * Starts the animation loop.
   *
   * @see #started()
   */
  @Override
  public void start() {
    _started = true;
    if (timer() != null)
      timer().run(_animationPeriod);
  }

  @Override
  public void toggle() {
    if (started())
      stop();
    else
      start();
  }

  /**
   * Restart the animation.
   * <p>
   * Simply calls {@link #stop()} and then {@link #start()}.
   */
  @Override
  public void restart() {
    stop();
    start();
  }

  @Override
  public void animate() {
  }
}
