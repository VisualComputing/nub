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

import java.util.ArrayList;

/**
 * A timing handler holds a {@link #timerPool()} and an {@link #animatorPool()}. The timer
 * pool are all the tasks scheduled to be performed in the future (one single time or
 * periodically). The animation pool are all the objects that implement an animation
 * callback function. For an introduction to FPSTiming please refer to
 * <a href="http://nakednous.github.io/projects/fpstiming">this</a>.
 */
public class TimingHandler {
  // T i m e r P o o l
  protected ArrayList<TimingTask> tPool;
  protected long frameRateLastMillis;
  public float frameRate;
  protected long fCount;

  // A N I M A T I O N
  protected ArrayList<Animator> aPool;

  /**
   * Main constructor.
   */
  public TimingHandler() {
    fCount = 0;
    frameRate = 10;
    frameRateLastMillis = System.currentTimeMillis();
    tPool = new ArrayList<TimingTask>();
    aPool = new ArrayList<Animator>();
  }

  /**
   * Constructor that takes and registers an animation object.
   */
  public TimingHandler(Animator aObject) {
    this();
    this.registerAnimator(aObject);
  }

  /**
   * Handler's main method. It should be called from within your main event loop. It does
   * the following: 1. Recomputes the frame rate; 2. Executes the all timers (those in the
   * {@link #timerPool()}) callback functions; and, 3. Performs all the animated objects
   * (those in the {@link #animatorPool()}) animation functions.
   */
  public void handle() {
    updateFrameRate();
    for (TimingTask task : tPool)
      if (task.timer() != null)
        if (task.timer() instanceof SeqTimer)
          if (((SeqTimer) task.timer()).timingTask() != null)
            ((SeqTimer) task.timer()).execute();
    // Animation
    for (Animator aObj : aPool)
      if (aObj.animationStarted())
        if (aObj.timer().trigggered())
          if (!aObj.invokeAnimationHandler())
            aObj.animate();
  }

  /**
   * Returns the timer pool.
   */
  public ArrayList<TimingTask> timerPool() {
    return tPool;
  }

  /**
   * Register a task in the timer pool and creates a sequential timer for it.
   */
  public void registerTask(TimingTask task) {
    task.setTimer(new SeqTimer(this, task));
    tPool.add(task);
  }

  /**
   * Register a task in the timer pool with the given timer.
   */
  public void registerTask(TimingTask task, Timer timer) {
    task.setTimer(timer);
    tPool.add(task);
  }

  /**
   * Unregisters the timer. You may also unregister the task this timer is attached to.
   *
   * @see #unregisterTask(TimingTask)
   */
  public void unregisterTask(SeqTimer t) {
    tPool.remove(t.timingTask());
  }

  /**
   * Unregisters the timer task.
   *
   * @see #unregisterTask(SeqTimer)
   */
  public void unregisterTask(TimingTask task) {
    tPool.remove(task);
  }

  /**
   * Returns {@code true} if the task is registered and {@code false} otherwise.
   */
  public boolean isTaskRegistered(TimingTask task) {
    return tPool.contains(task);
  }

  /**
   * Recomputes the frame rate based upon the frequency at which {@link #handle()} is
   * called from within the application main event loop. The frame rate is needed to sync
   * all timing operations.
   */
  protected void updateFrameRate() {
    long now = System.currentTimeMillis();
    if (fCount > 1) {
      // update the current frameRate
      double rate = 1000.0 / ((now - frameRateLastMillis) / 1000.0);
      float instantaneousRate = (float) rate / 1000.0f;
      frameRate = (frameRate * 0.9f) + (instantaneousRate * 0.1f);
    }
    frameRateLastMillis = now;
    fCount++;
  }

  /**
   * Returns the approximate frame rate of the software as it executes. The initial value
   * is 10 fps and is updated with each frame. The value is averaged (integrated) over
   * several frames. As such, this value won't be valid until after 5-10 frames.
   */
  public float frameRate() {
    return frameRate;
  }

  /**
   * Returns the number of frames displayed since the program started.
   */
  public long frameCount() {
    return fCount;
  }

  /**
   * Converts all registered timers to single-threaded timers.
   */
  public void restoreTimers() {
    boolean isActive;

    for (TimingTask task : tPool) {
      long period = 0;
      boolean rOnce = false;
      isActive = task.isActive();
      if (isActive) {
        period = task.period();
        rOnce = task.timer().isSingleShot();
      }
      task.stop();
      task.setTimer(new SeqTimer(this, task));
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
    return aPool;
  }

  /**
   * Registers the animation object.
   */
  public void registerAnimator(Animator object) {
    if (object.timingHandler() != this)
      object.setTimingHandler(this);
    aPool.add(object);
  }

  /**
   * Unregisters the animation object.
   */
  public void unregisterAnimator(Animator object) {
    aPool.remove(object);
  }

  /**
   * Returns {@code true} if the animation object is registered and {@code false}
   * otherwise.
   */
  public boolean isAnimatorRegistered(Animator object) {
    return aPool.contains(object);
  }
}
