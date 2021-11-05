/***************************************************************************************
 * nub
 * Copyright (c) 2019-2021 Universidad Nacional de Colombia
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
  // T i m e r P o o l
  public static HashSet<Task> _tasks = new HashSet<Task>();

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

}
