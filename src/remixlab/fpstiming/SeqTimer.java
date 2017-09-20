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
 * Sequential timers are single-threaded timers handled by a TimingHandler.
 */
public class SeqTimer implements Timer {
  protected Taskable task;
  protected TimingHandler handler;
  protected boolean active;
  protected boolean runOnlyOnce;
  private long counter;
  private long prd;
  private long startTime;

  /**
   * Defines a single shot sequential (single-threaded) timer.
   *
   * @param h timing handler owner
   */
  public SeqTimer(TimingHandler h) {
    this(h, false, null);
  }

  /**
   * Defines a sequential (single-threaded) timer.
   *
   * @param h          timing handler owner
   * @param singleShot
   */
  public SeqTimer(TimingHandler h, boolean singleShot) {
    this(h, singleShot, null);
  }

  public SeqTimer(TimingHandler h, Taskable t) {
    this(h, false, t);
  }

  public SeqTimer(TimingHandler h, boolean singleShot, Taskable t) {
    handler = h;
    runOnlyOnce = singleShot;
    task = t;
    create();
  }

  @Override
  public Taskable timingTask() {
    return task;
  }

  /**
   * Executes the callback method defined by the {@link #timingTask()}.
   * <p>
   * <b>Note:</b> You should not call this method since it's done by the timing handler
   * (see {@link remixlab.fpstiming.TimingHandler#handle()}).
   */
  protected boolean execute() {
    boolean result = trigggered();
    if (result) {
      timingTask().execute();
      if (runOnlyOnce)
        inactivate();
    }
    return result;
  }

  @Override
  public void cancel() {
    stop();
    handler.unregisterTask(this);
  }

  @Override
  public void create() {
    inactivate();
  }

  @Override
  public void run(long period) {
    setPeriod(period);
    run();
  }

  @Override
  public void run() {
    if (prd <= 0)
      return;
    inactivate();
    counter = 1;
    active = true;
    startTime = System.currentTimeMillis();
  }

  @Override
  public void stop() {
    inactivate();
  }

  @Override
  public boolean isActive() {
    return active;
  }

  // others

  /**
   * Deactivates the SeqTimer.
   */
  public void inactivate() {
    active = false;
  }

  /**
   * Returns {@code true} if the timer was triggered at the given frame.
   * <p>
   * <b>Note:</b> You should not call this method since it's done by the timing handler
   * (see {@link remixlab.fpstiming.TimingHandler#handle()}).
   */
  public boolean trigggered() {
    if (!active)
      return false;

    long elapsedTime = System.currentTimeMillis() - startTime;

    float timePerFrame = (1 / handler.frameRate()) * 1000;
    long threshold = counter * prd;

    boolean result = false;
    if (threshold >= elapsedTime) {
      long diff = elapsedTime + (long) timePerFrame - threshold;
      if (diff >= 0) {
        if ((threshold - elapsedTime) < diff) {
          result = true;
        }
      }
    } else {
      result = true;
    }

    if (result) {
      counter++;
      // if (prd < timePerFrame)
      // System.out.println("Your current frame rate (~" + handler.frameRate() +
      // " fps) is not high enough " + "to run the timer and reach the specified
      // " + prd + " ms period, "
      // + timePerFrame
      // + " ms period will be used instead. If you want to sustain a lower
      // timer " +
      // "period, define a higher frame rate (minimum of " + 1000f / prd + "
      // fps) " +
      // "before running the timer (you may need to simplify your drawing to
      // achieve it.)");

    }

    return result;
  }

  @Override
  public long period() {
    return prd;
  }

  @Override
  public void setPeriod(long period) {
    prd = period;
  }

  @Override
  public boolean isSingleShot() {
    return runOnlyOnce;
  }

  @Override
  public void setSingleShot(boolean singleShot) {
    runOnlyOnce = singleShot;
  }
}
