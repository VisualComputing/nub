/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import remixlab.fpstiming.Taskable;
import remixlab.fpstiming.Timer;

/**
 * Non-seq timer based on java.util.Timer and java.util.TimerTask.
 */
class NonSeqTimer implements Timer {
  Scene scene;
  java.util.Timer timer;
  java.util.TimerTask timerTask;
  Taskable tmnTask;
  boolean runOnlyOnce;
  boolean active;
  long prd;

  public NonSeqTimer(Scene scn, Taskable o) {
    this(scn, o, false);
  }

  public NonSeqTimer(Scene scn, Taskable o, boolean singleShot) {
    scene = scn;
    runOnlyOnce = singleShot;
    tmnTask = o;
  }

  @Override
  public Taskable timingTask() {
    return tmnTask;
  }

  @Override
  public void create() {
    stop();
    timer = new java.util.Timer();
    timerTask = new java.util.TimerTask() {
      public void run() {
        tmnTask.execute();
      }
    };
  }

  @Override
  public void run(long period) {
    setPeriod(period);
    run();
  }

  @Override
  public void run() {
    create();
    if (isSingleShot())
      timer.schedule(timerTask, prd);
    else
      timer.scheduleAtFixedRate(timerTask, 0, prd);
    active = true;
  }

  @Override
  public void cancel() {
    stop();
  }

  @Override
  public void stop() {
    if (timer != null) {
      timer.cancel();
      timer.purge();
    }
    active = false;
  }

  @Override
  public boolean isActive() {
    return timer != null && active;
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
