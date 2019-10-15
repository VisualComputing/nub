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
 * A timing handler holds a {@link #taskPool()} with all the tasks
 * scheduled to be performed in the future (one single time or periodically).
 */
public class TimingHandler {
  static public long frameCount;
  protected float _frameRate;

  protected long _deltaCount;
  // T i m e r P o o l
  protected ArrayList<Task> _taskPool;
  protected long _frameRateLastNanos;
  protected long _localCount;

  /**
   * Main constructor.
   */
  public TimingHandler() {
    _localCount = 0;
    _deltaCount = frameCount;
    _frameRate = 60;
    _frameRateLastNanos = 0;
    _taskPool = new ArrayList<Task>();
  }

  /**
   * Handler's main method. It should be called from within your main event loop.
   * It recomputes the frame rate, and executes all tasks (those in the
   * {@link #taskPool()}) callback functions.
   */
  public void handle() {
    _updateFrameRate();
    for (Task task : _taskPool)
      if (!task.isConcurrent())
        task._execute(frameRate());
  }

  /**
   * Returns the task pool.
   */
  public ArrayList<Task> taskPool() {
    return _taskPool;
  }

  /**
   * Register a task in the task pool.
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
    _taskPool.remove(task);
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
   */
  protected void _updateFrameRate() {
    long now = System.nanoTime();
    if (_localCount > 1) {
      float frameTimeSecs = (float) (now - this._frameRateLastNanos) / 1.0E9f;
      float avgFrameTimeSecs = 1.0f / _frameRate;
      avgFrameTimeSecs = 0.95f * avgFrameTimeSecs + 0.05f * frameTimeSecs;
      _frameRate = (1.0f / avgFrameTimeSecs);
    }
    _frameRateLastNanos = now;
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
}
