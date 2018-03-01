/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package frames.ik;

import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.ArrayList;

public class CCDSolver extends Solver {
  protected ArrayList<? extends Frame> chain;
  protected Frame target;
  private Frame previousTarget;

  public ArrayList<? extends Frame> getChain() {
    return chain;
  }

  public Frame getTarget() {
    return target;
  }

  public void setTarget(Frame target) {
    this.target = target;
  }

  public Frame getHead() {
    return chain.get(0);
  }

  public Frame getEndEffector() {
    return chain.get(chain.size() - 1);
  }

  public CCDSolver(ArrayList<? extends Frame> chain) {
    this(chain, null);
  }

  public CCDSolver(ArrayList<? extends Frame> chain, Frame target) {
    super();
    this.chain = chain;
    this.target = target;
    this.previousTarget =
        target == null ? null : new Frame(target.position().get(), target.orientation().get());
  }

  /*
   * Performs a CCD ITERATION
   * For further info please look at (https://sites.google.com/site/auraliusproject/ccd-algorithm)
   * */
  public boolean iterate() {
    //As no target is specified there is no need to perform an iteration
    if (target == null || chain.size() < 2) return true;
    Frame end = chain.get(chain.size() - 1);
    Vector target = this.target.position().get();
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= ERROR) {
      return true;
    }
    float change = 0.0f;
    Vector endLocalPosition = chain.get(chain.size() - 2).coordinatesOf(end.position());
    Vector targetLocalPosition = chain.get(chain.size() - 2).coordinatesOf(target);
    for (int i = chain.size() - 2; i >= 0; i--) {
      Quaternion delta = null;
      Quaternion initial = chain.get(i).rotation().get();
      delta = new Quaternion(endLocalPosition, targetLocalPosition);
      //update target local position
      targetLocalPosition = chain.get(i).localInverseCoordinatesOf(targetLocalPosition);
      chain.get(i).rotate(delta);
      //update end effector local position
      endLocalPosition = chain.get(i).localInverseCoordinatesOf(endLocalPosition);
      initial.compose(chain.get(i).rotation().get());
      change += Math.abs(initial.angle());
    }
    //Check total rotation change
    if (change <= MINCHANGE) return true;
    return false;
  }

  public void update() {
    /*Not required, since chain is updated inside iterate step*/
  }

  public boolean stateChanged() {
    if (target == null) {
      previousTarget = null;
      return false;
    } else if (previousTarget == null) {
      return true;
    }
    return !(previousTarget.position().equals(target.position()) && previousTarget.orientation().equals(target.orientation()));
  }

  public void reset() {
    previousTarget = target == null ? null : new Frame(target.position().get(), target.orientation().get());
    iterations = 0;
  }
}
