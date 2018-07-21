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

import java.util.ArrayList;

/**
 * A timing handler holds a {@link #timerPool()} and an {@link #animatorPool()}. The timer
 * pool are all the tasks scheduled to be performed in the future (one single time or
 * periodically). The animation pool are all the objects that implement an animation
 * callback function. For an introduction to FPSTiming please refer to
 * <a href="http://nakednous.github.io/projects/fpstiming">this</a>.
 */
public class TimingHandler {
  static public long frameCount;
  protected float _frameRate;

  protected long _deltaCount;
  // T i m e r P o o l
  protected ArrayList<TimingTask> _taskPool;
  protected long _frameRateLastMillis;
  protected long _localCount;

  // A N I M A T I O N
  protected ArrayList<Animator> _animatorPool;

  /**
   * Main constructor.
   */
  public TimingHandler() {
    _localCount = 0;
    _deltaCount = frameCount;
    _frameRate = 10;
    _frameRateLastMillis = System.currentTimeMillis();
    _taskPool = new ArrayList<TimingTask>();
    _animatorPool = new ArrayList<Animator>();
  }

  /**
   * Handler's main method. It should be called from within your main event loop. It does
   * the following: 1. Recomputes the frame rate; 2. Executes the all timers (those in the
   * {@link #timerPool()}) callback functions; and, 3. Performs all the animated objects
   * (those in the {@link #animatorPool()}) animation functions.
   */
  public void handle() {
    _updateFrameRate();
    for (TimingTask task : _taskPool)
      if (task.timer() != null)
        if (task.timer() instanceof SequentialTimer)
          if (task.timer().timingTask() != null)
            ((SequentialTimer) task.timer())._execute();
    // Animation
    for (Animator animator : _animatorPool)
      if (animator.started())
        if (animator.timer().trigggered())
          animator.animate();
  }

  /**
   * Returns the timer pool.
   */
  public ArrayList<TimingTask> timerPool() {
    return _taskPool;
  }

  /**
   * Register a task in the timer pool and creates a sequential timer for it.
   */
  public void registerTask(TimingTask task) {
    task.setTimer(new SequentialTimer(this, task));
    _taskPool.add(task);
  }

  /**
   * Register a task in the timer pool with the given timer.
   */
  public void registerTask(TimingTask task, Timer timer) {
    task.setTimer(timer);
    _taskPool.add(task);
  }

  /**
   * Unregisters the timer. You may also unregister the task this timer is attached to.
   *
   * @see #unregisterTask(TimingTask)
   */
  public void unregisterTask(SequentialTimer timer) {
    _taskPool.remove(timer.timingTask());
  }

  /**
   * Unregisters the timer task.
   *
   * @see #unregisterTask(SequentialTimer)
   */
  public void unregisterTask(TimingTask task) {
    _taskPool.remove(task);
  }

  /**
   * Returns {@code true} if the task is registered and {@code false} otherwise.
   */
  public boolean isTaskRegistered(TimingTask task) {
    return _taskPool.contains(task);
  }

  /**
   * Recomputes the frame rate based upon the frequency at which {@link #handle()} is
   * called from within the application main event loop. The frame rate is needed to sync
   * all timing operations.
   */
  protected void _updateFrameRate() {
    long now = System.currentTimeMillis();
    if (_localCount > 1) {
      // update the current _frameRate
      double rate = 1000.0 / ((now - _frameRateLastMillis) / 1000.0);
      float instantaneousRate = (float) rate / 1000.0f;
      _frameRate = (_frameRate * 0.9f) + (instantaneousRate * 0.1f);
    }
    _frameRateLastMillis = now;
    _localCount++;
    //TODO needs testing but I think is also safe and simpler
    //if (TimingHandler.frameCount < frameCount())
    //TimingHandler.frameCount = frameCount();
    if (frameCount < frameCount() + _deltaCount)
      frameCount = frameCount() + _deltaCount;
  }

  /**
   * Returns the approximate frame rate of the software as it executes. The initial value
   * is 10 fps and is updated with each frame. The value is averaged (integrated) over
   * several frames. As such, this value won't be valid until after 5-10 frames.
   */
  public float frameRate() {
    return _frameRate;
  }

  /**
   * Returns the number of frames displayed since this timing handler was instantiated.
   */
  public long frameCount() {
    return _localCount;
  }

  /**
   * Converts all registered timers to single-threaded timers.
   */
  public void restoreTimers() {
    boolean isActive;

    for (TimingTask task : _taskPool) {
      long period = 0;
      boolean rOnce = false;
      isActive = task.isActive();
      if (isActive) {
        period = task.period();
        rOnce = task.timer().isSingleShot();
      }
      task.stop();
      task.setTimer(new SequentialTimer(this, task));
      if (isActive) {
        if (rOnce)
          task.runOnce(period);
        else
          task.run(period);
      }
    }

    System.out.println("single threaded timers set");
  }

  // Animation -->

  /**
   * Returns all the animated objects registered at the handler.
   */
  public ArrayList<Animator> animatorPool() {
    return _animatorPool;
  }

  /**
   * Registers the animation object.
   */
  public void registerAnimator(Animator animator) {
    _animatorPool.add(animator);
  }

  /**
   * Unregisters the animation object.
   */
  public void unregisterAnimator(Animator animator) {
    _animatorPool.remove(animator);
  }

  /**
   * Returns {@code true} if the animation object is registered and {@code false}
   * otherwise.
   */
  public boolean isAnimatorRegistered(Animator animator) {
    return _animatorPool.contains(animator);
  }
}
