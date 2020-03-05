/***************************************************************************************
 * nub
 * Copyright (c) 2019-2020 Universidad Nacional de Colombia
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.timing;

import java.util.ArrayList;

/**
 * A timing handler holds a {@link #taskPool()} with all the tasks
 * scheduled to be performed in the future (one single time or periodically).
 * <p>
 * A timing handler should be used as a static scene instance.
 */
public class TimingHandler {
  /**
   * Returns the approximate frame rate of the software as it executes. The initial value
   * is 10 fps and is updated with each frame. The value is averaged (integrated) over
   * several frames. As such, this value won't be valid until after 5-10 frames.
   */
  static public long frameCount;
  /**
   * Returns the number of frames displayed since this timing handler was instantiated.
   */
  static public float frameRate;

  //protected long _deltaCount;
  // T i m e r P o o l
  protected ArrayList<Task> _taskPool;
  protected long _frameRateLastNanos;
  //protected long _localCount;

  /**
   * Main constructor.
   */
  public TimingHandler() {
    frameRate = 60;
    _frameRateLastNanos = 0;
    _taskPool = new ArrayList<Task>();
  }

  /**
   * Handler's main method. It should be called from within your main event loop.
   * It recomputes the frame rate, and executes all non-concurrent tasks found in
   * the {@link #taskPool()}.
   */
  public void handle() {
    _updateFrameRate();
    for (Task task : _taskPool)
      if (!task.isConcurrent())
        task._execute();
  }

  /**
   * Returns the task pool.
   */
  public ArrayList<Task> taskPool() {
    return _taskPool;
  }

  /**
   * Register a task in the pool.
   */
  public void registerTask(Task task) {
    if (isTaskRegistered(task)) {
      System.out.println("Nothing done. Task is already registered");
    }
    _taskPool.add((Task) task);
  }

  /**
   * Unregisters the task.
   */
  public void unregisterTask(Task task) {
    if (isTaskRegistered(task)) {
      task.stop();
      _taskPool.remove(task);
    }
  }

  /**
   * Returns {@code true} if the task is registered and {@code false} otherwise.
   */
  public boolean isTaskRegistered(Task task) {
    return _taskPool.contains(task);
  }

  /**
   * Recomputes the frame rate based upon the frequency at which {@link #handle()} is
   * called from within the application main event loop. The frame rate is needed to sync
   * all timing operations.
   * <p>
   * Computation adapted from here (refer to handleDraw()):
   * https://github.com/processing/processing/blob/master/core/src/processing/core/PApplet.java
   */
  protected void _updateFrameRate() {
    long now = System.nanoTime();
    if (frameCount > 1) {
      float frameTimeSecs = (float) (now - this._frameRateLastNanos) / 1.0E9f;
      float avgFrameTimeSecs = 1.0f / frameRate;
      avgFrameTimeSecs = 0.95f * avgFrameTimeSecs + 0.05f * frameTimeSecs;
      frameRate = (1.0f / avgFrameTimeSecs);
    }
    _frameRateLastNanos = now;
    frameCount++;
  }
}
