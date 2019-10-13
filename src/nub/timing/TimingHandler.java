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

import java.util.ArrayList;

/**
 * A timing handler holds a {@link #timerPool()} with all the tasks
 * scheduled to be performed in the future (one single time or periodically).
 */
public class TimingHandler {
  static public long frameCount;
  protected float _frameRate;

  protected long _deltaCount;
  // T i m e r P o o l
  protected ArrayList<Task> _taskPool;
  protected long _frameRateLastMillis;
  protected long _localCount;

  /**
   * Main constructor.
   */
  public TimingHandler() {
    _localCount = 0;
    _deltaCount = frameCount;
    _frameRate = 10;
    _frameRateLastMillis = System.currentTimeMillis();
    _taskPool = new ArrayList<Task>();
  }

  /**
   * Handler's main method. It should be called from within your main event loop. It
   * recomputes the node rate, and executes the all timers (those in the
   * {@link #timerPool()}) callback functions.
   */
  public void handle() {
    _updateFrameRate();
    for (Task task : _taskPool)
      if (task.timer() != null)
        if (task.timer() instanceof SequentialTimer)
          if (task.timer().task() != null)
            ((SequentialTimer) task.timer())._execute();
  }

  /**
   * Returns the timer pool.
   */
  public ArrayList<Task> timerPool() {
    return _taskPool;
  }

  /**
   * Register a task in the timer pool and creates a sequential timer for it.
   */
  public void registerTask(Task task) {
    task.setTimer(new SequentialTimer(this, task));
    _taskPool.add(task);
  }

  /**
   * Register a task in the timer pool with the given timer.
   */
  public void registerTask(Task task, Timer timer) {
    task.setTimer(timer);
    _taskPool.add(task);
  }

  /**
   * Unregisters the timer task.
   */
  public void unregisterTask(Task task) {
    _taskPool.remove(task);
  }

  /**
   * Returns {@code true} if the task is registered and {@code false} otherwise.
   */
  public boolean isTaskRegistered(Task task) {
    return _taskPool.contains(task);
  }

  /**
   * Recomputes the node rate based upon the frequency at which {@link #handle()} is
   * called from within the application main event loop. The node rate is needed to sync
   * all timing operations.
   */
  protected void _updateFrameRate() {
    long now = System.currentTimeMillis();
    if (_localCount > 1) {
      // update the current _frameRate
      float rate = 1000.0f / ((now - _frameRateLastMillis) / 1000.0f);
      float instantaneousRate = rate / 1000.0f;
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
   * Returns the approximate node rate of the software as it executes. The initial value
   * is 10 fps and is updated with each node. The value is averaged (integrated) over
   * several nodes. As such, this value won't be valid until after 5-10 nodes.
   */
  public float frameRate() {
    return _frameRate;
  }

  /**
   * Returns the number of nodes displayed since this timing handler was instantiated.
   */
  public long frameCount() {
    return _localCount;
  }

  /**
   * Converts all registered timers to single-threaded timers.
   */
  public void restoreTimers() {
    boolean isActive;
    for (Task task : _taskPool) {
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
}
