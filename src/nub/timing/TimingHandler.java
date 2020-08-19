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

import java.util.HashSet;

/**
 * A timing handler holds a {@link #tasks()} with all the tasks
 * scheduled to be performed in the future (one single time or periodically).
 * <p>
 * A timing handler should be used as a static scene instance.
 */
public class TimingHandler {
  /**
   * Returns the number of frames displayed since this timing handler was instantiated.
   */
  static public long frameCount;

  /**
   * Returns the approximate frame rate of the software as it executes. The initial value
   * is 10 fps and is updated with each frame. The value is averaged (integrated) over
   * several frames. As such, this value won't be valid until after 5-10 frames.
   */
  static public float frameRate = 60;
  protected static long _frameRateLastNanos;

  // T i m e r P o o l
  protected static HashSet<Task> _tasks = new HashSet<Task>();

  /**
   * Handler's main method. It should be called from within your main event loop.
   * It recomputes the frame rate, and executes all non-concurrent tasks found in
   * the {@link #tasks()}.
   */
  public static void handle() {
    _updateFrameRate();
    for (Task task : _tasks)
      if (!task.isConcurrent())
        task._execute();
  }

  /**
   * Returns the task set.
   */
  public static HashSet<Task> tasks() {
    return _tasks;
  }

  /**
   * Register a task in the set.
   */
  public static void registerTask(Task task) {
    if (task == null) {
      System.out.println("Nothing done. Task is null");
      return;
    }
    _tasks.add(task);
  }

  /**
   * Unregisters the task.
   */
  public static void unregisterTask(Task task) {
    if (isTaskRegistered(task)) {
      task.stop();
      _tasks.remove(task);
    }
  }

  /**
   * Returns {@code true} if the task is registered and {@code false} otherwise.
   */
  public static boolean isTaskRegistered(Task task) {
    return _tasks.contains(task);
  }

  /**
   * Recomputes the frame rate based upon the frequency at which {@link #handle()} is
   * called from within the application main event loop. The frame rate is needed to sync
   * all timing operations.
   * <p>
   * Computation adapted from here (refer to handleDraw()):
   * https://github.com/processing/processing/blob/master/core/src/processing/core/PApplet.java
   */
  protected static void _updateFrameRate() {
    long now = System.nanoTime();
    if (frameCount > 0) {
      float frameTimeSecs = (now - _frameRateLastNanos) / 1e9f;
      float avgFrameTimeSecs = 1.0f / frameRate;
      avgFrameTimeSecs = 0.95f * avgFrameTimeSecs + 0.05f * frameTimeSecs;
      frameRate = 1.0f / avgFrameTimeSecs;
    }
    _frameRateLastNanos = now;
    frameCount++;
  }
}
