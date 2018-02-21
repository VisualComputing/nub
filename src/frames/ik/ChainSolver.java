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

public class ChainSolver extends FABRIKSolver {

  //TODO: It will be useful that any Joint in the chain could have a Target ?
  //TODO: Enable Translation of Head (Skip Backward Step)

  protected ArrayList<? extends Frame> chain;
  private ArrayList<Frame> bestSolution;

  protected Frame target;
  private Frame prevTarget;

  public ArrayList<? extends Frame> getChain() {
    return chain;
  }

  private ArrayList<Frame> copyChain(ArrayList<? extends Frame> list) {
    ArrayList<Frame> copy = new ArrayList<Frame>();
    Frame reference = list.get(0).reference();
    if (reference != null) {
      reference = new Frame(reference.position().get(), reference.orientation().get());
    }
    for (Frame joint : list) {
      Frame newJoint = new Frame();
      newJoint.setReference(reference);
      newJoint.setPosition(joint.position().get());
      newJoint.setOrientation(joint.orientation().get());
      newJoint.setConstraint(joint.constraint());
      copy.add(newJoint);
      reference = newJoint;
    }
    return copy;
  }

  public void setChain(ArrayList<? extends Frame> chain) {
    this.chain = chain;
    bestSolution = copyChain(chain);
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

  public ChainSolver(ArrayList<? extends Frame> chain) {
    this(chain, null);
  }

  public ChainSolver(ArrayList<? extends Frame> chain, Frame target) {
    super();
    setChain(chain);
    positions = new ArrayList<Vector>();
    distances = new ArrayList<Float>();
    orientations = new ArrayList<Quaternion>();
    Vector prevPosition = chain.get(0).reference() != null
        ? chain.get(0).reference().position().get() : new Vector(0, 0, 0);
    Quaternion prevOrientation = chain.get(0).reference() != null
        ? chain.get(0).reference().orientation().get() : new Quaternion();
    for (Frame joint : chain) {
      Vector position = joint.position().get();
      Quaternion orientation = prevOrientation.get();
      orientation.compose(joint.rotation().get());
      positions.add(position);
      distances.add(Vector.subtract(position, prevPosition).magnitude());
      orientations.add(orientation);
      prevPosition = position;
      prevOrientation = orientation.get();
    }
    this.target = target;
    this.prevTarget =
        target == null ? null : new Frame(target.position().get(), target.orientation().get());
  }

  /*Get maximum length of a given chain*/
  public float getLength() {
    float dist = 0;
    for (int i = 1; i < chain.size(); i++) {
      dist += chain.get(i).translation().magnitude() / chain.get(i).magnitude();
    }
    return dist;
  }

  public void stretchChain(ArrayList<? extends Frame> chain, Vector target) {
    for (int i = 0; i < chain.size() - 1; i++) {
      //Get the distance between Joint i and the Target
      Vector pos_i = positions.get(i);
      float r_i = Vector.distance(pos_i, target);
      float dist_i = chain.get(i + 1).translation().magnitude() / chain.get(i + 1).magnitude();
      float lambda_i = dist_i / r_i;
      Vector new_pos = Vector.multiply(pos_i, 1.f - lambda_i);
      new_pos.add(Vector.multiply(target, lambda_i));
      positions.set(i + 1, new_pos);
    }
  }


  /*
   * Performs a FABRIK ITERATION
   *
   * */
  public boolean iterate() {
    //As no target is specified there is no need to perform FABRIK
    if (target == null) return true;
    Frame root = chain.get(0);
    Frame end = chain.get(chain.size() - 1);
    Vector target = this.target.position().get();

    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= ERROR) {
      return true;
    }

    //Get the distance between the Root and the End Effector
    float length = getLength();
    //Get the distance between the Root and the Target
    float dist = Vector.distance(root.position(), target);
    //When Target is unreachable        //Debug methods
                    /*if(dist > length){
                    stretchChain(chain, target);
                    return true;
                }else{*/
    //Initial root position
    Vector initial = positions.get(0).get();
    //Stage 1: Forward Reaching
    positions.set(chain.size() - 1, target.get());
    executeForwardReaching();
    //Stage 2: Backward Reaching
    positions.set(0, initial);
    float change = executeBackwardReaching();
    //Save best solution
    if (Vector.distance(target, end.position()) < Vector.distance(target, bestSolution.get(chain.size() - 1).position())) {
      bestSolution = copyChain(chain);
    }
    //Check total position change
    if (change <= MINCHANGE) return true;
    return false;
  }

  public void executeForwardReaching() {
    executeForwardReaching(chain);
  }

  public float executeBackwardReaching() {
    return executeBackwardReaching(chain);
  }

  public void update() {
    //for(int i = 0; i < chain.size(); i++){
    //    chain.get(i).setRotation(bestSolution.get(i).rotation().get());
    //}
  }

  public boolean stateChanged() {
    if (target == null) {
      prevTarget = null;
      return false;
    } else if (prevTarget == null) {
      return true;
    }
    return !(prevTarget.position().equals(target.position()) && prevTarget.orientation().equals(target.orientation()));
  }

  public void reset() {
    prevTarget = target == null ? null : new Frame(target.position().get(), target.orientation().get());
    iterations = 0;
    //We know that State has change but not where, then it is better to reset Global Positions and Orientations
    initialize();
  }

  public void initialize() {
    //Initialize List with info about Positions and Orientations
    positions = new ArrayList<Vector>();
    distances = new ArrayList<Float>();
    orientations = new ArrayList<Quaternion>();
    Vector prevPosition = chain.get(0).reference() != null
        ? chain.get(0).reference().position().get() : new Vector(0, 0, 0);
    Quaternion prevOrientation = chain.get(0).reference() != null
        ? chain.get(0).reference().orientation().get() : new Quaternion();
    for (Frame joint : chain) {
      Vector position = joint.position().get();
      Quaternion orientation = prevOrientation.get();
      orientation.compose(joint.rotation().get());
      positions.add(position);
      distances.add(Vector.subtract(position, prevPosition).magnitude());
      orientations.add(orientation);
      prevPosition = position;
      prevOrientation = orientation.get();
    }
  }
}