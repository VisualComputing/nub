/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.ik.solver.geometric;

import nub.core.Node;
import nub.core.constraint.ConeConstraint;
import nub.core.constraint.DistanceFieldConstraint;
import nub.core.constraint.Hinge;
import nub.ik.animation.IKAnimation;
import nub.ik.animation.InterestingEvent;
import nub.ik.solver.Solver;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public abstract class FABRIKSolver extends Solver {
  //TODO : Update
  //TODO: It will be useful that any Joint in the chain could have a Target ?
  //TODO: Enable Translation of Head (Skip Backward Step)
  /*
   * Some ideas:
   * Increase of weight direction: Last segments must move a lot, but First ones must try
   * to keep it's direction, due last segments could be fixed by first ones ?
   * Weights depends on _iterations: First _iterations must be exploratory whereas last ones must
   * preserve directions ?
   *
   * FABRIK K-Steps
   * Direction D given by J_n and J_(n-1) is quite important:
   * Choose randomly k different directions:
   *     Axis : (J_n - J_(n-1)) cross (Target - J_(n-1))
   *     Angle : (J_n - J_(n-1)) dot (Target - J_(n-1))
   *     A_i = Angle + random(-1,1) * 2PI/k
   *     D_i = Rotate (J_n - J_(n-1)) by A_i angles w.r.t Axis
   *     J'_(n-1) = Target - D_i
   * Apply FABRIK k times in parallel with depth d
   * Dist^d_i  = dist(J'_(n-1-d), J_(n-1-d-1))
   * a. Discard all executions but i* s.t Dist_i* < Dist_i for i in (1,k)
   * Perform 1st phase of FABRIK. If dist(J'_0, J_0) is to Big discard (keep track of all Dist^d_i)
   * Execute repeat from a. and discard at depth d if Dist^d_i is worst than best execution so far.
   * Worst case O(k*FABRIK_Iteration_Complexity(n))
   *
   * FABRIK UP-DOWN (Further Thinking) - Basic idea : when movement is constrained, try to favor it by looking upward
   * Define a threshold Thr globally or per Joint.
   * Execute FABRIK as usual on fist Phase.
   * At time t:
   *   idx = n-1-t
   *   Possible conditions:
   *     1. dist(J'_(idx), J_(idx-1)) < Thr_(idx) and J_(idx) has constraint
   *     2. Movement to perform is far away due to constraint
   *   Get max movement with constaint
   *   Move J'_(idx)  according to obtained movement
   *   Execute Phase 2 and Get position of J_n
   *   ...
   *   Sub-problem Reach phi w.rt A axis while keeping J_n position
   *   Distribute phi in remaining chain J_i to J_n
   *   Calculate bounds phi_max_t, phi_min_t w.r.t A axis for each joint -> get phi_max_sum
   *   Define a way to propagate:
   *     phi_max_sum - phi_min_sum must always be > 0
   *
   *   There're some cases in which is desirable to Propagate all movement through Joints
   * */

  public static class Properties {
    //Enable constraint or not
    protected boolean _useConstraint = true;
    protected boolean _enableFixWeight = true;
    //It is useful when the chain is highly constrained
    protected boolean _enableDirectionWeight = false; // TODO : Clean
    protected float _directionWeight = 0.5f; //TODO: experiment with this parameter
    //When it is 1 the Joint will not move at all
    protected float _fixWeight = 0;

    public Properties(boolean basic) {
      if (basic) {
        _useConstraint = false;
        _enableFixWeight = false;
      }
    }
  }

  protected HashMap<Integer, Properties> _properties = new HashMap<>(); //TODO : Use an Array instead of a HashMap
  /*Store Joint's desired position*/
  protected ArrayList<Vector> _positions = new ArrayList<Vector>();
  protected ArrayList<Quaternion> _orientations = new ArrayList<Quaternion>();
  protected ArrayList<Float> _distances = new ArrayList<Float>();
  protected ArrayList<Float> _jointChange = new ArrayList<Float>();

  protected ArrayList<Vector> _positions() {
    return _positions;
  }

  protected int _head = 0;

  //Animation Stuff
  //TODO : Animation with proposed heuristics
  protected boolean _enableHistory;
  protected IKAnimation.NodeStates _history;
  protected int _last_time_event = 0;

  /*Heuristic Parameters*/
  protected boolean _fixTwisting = true;
  protected boolean _keepDirection = true;

  //TODO : Clean code
  static Random r = new Random();
  public static boolean rand = false;
  public static boolean debug = false; //TODO : discard

  /*
   * Move vector u to v while keeping certain distance.
   *
   * Find the point that lies on the segment v - u that is at the given distance to
   * Point u.
   * Fix weight is a parameter in range [0,1] that will scale the distance between V and
   * the found point. A bigger value of this parameter implies to perform a smaller movement.
   * */
  public boolean fixTwisting() {
    return _fixTwisting;
  }

  public boolean keepDirection() {
    return _keepDirection;
  }

  public void setFixTwisting(boolean fixTwisting) {
    _fixTwisting = fixTwisting;
  }

  public void setKeepDirection(boolean keepDirection) {
    _keepDirection = keepDirection;
  }

  public void setDirectionWeight(float weight) {
    for (Integer i : _properties.keySet()) {
      _properties.get(i)._enableDirectionWeight = true;
      _properties.get(i)._directionWeight = weight;
    }
  }

  public void setFixedWeight(float weight) {
    for (Integer i : _properties.keySet()) {
      _properties.get(i)._enableFixWeight = true;
      _properties.get(i)._fixWeight = weight;
    }
  }

  public static Vector _move(Vector u, Vector v, float distance, float fixWeight) {
    float r = Vector.distance(u, v);
    float lambda_i = distance / r;
    lambda_i *= (1 - fixWeight);
    Vector new_u = Vector.multiply(u, 1.f - lambda_i);
    new_u.add(Vector.multiply(v, lambda_i));
    return new_u;
  }

  public Vector _move(Vector u, Vector v, float distance) {
    return _move(u, v, distance, 0);
  }

  /*
   * Performs First Stage of FABRIK Algorithm, receives a chain of Frames, being the Frame at i
   * the reference frame of the Frame at i + 1
   * */
  protected float _forwardReaching(List<? extends Node> chain) {
    float change = 0;
    if(_enableMediator) {
      //Create the event
      InterestingEvent event1 = new InterestingEvent("FWD_SET_TARGET", "Trajectory", _last_time_event, 1, 2);
      //Add the convenient attributes
      event1.addAttribute("positions", chain.get(chain.size() - 1).position(), _positions.get(chain.size() - 1).get());
      //Add it to the event queue
      mediator().addEvent(event1);
      //Create the event
      InterestingEvent message1 = new InterestingEvent("FWD_SET_TARGET_MESSAGE", "Message", _last_time_event, 0, 1);
      //Add the convenient attributes
      message1.addAttribute("message", "Step 1: Find the segment line defined by the end effector and the desired position");
      //Add it to the event queue
      mediator().addEvent(message1);
      _last_time_event++;
      //Create the event
      InterestingEvent event2 = new InterestingEvent("FWD_MOVE_TO_TARGET", "NodeTranslation", _last_time_event, 1, 1);
      //Add the convenient attributes
      event2.addAttribute("node", chain.get(chain.size() - 1));
      Vector translation = null;
      if(chain.get(chain.size() - 1).reference() != null)
        translation = Vector.subtract(_positions.get(chain.size() - 1), chain.get(chain.size() - 1).position());
      else
        translation = _positions.get(chain.size() - 1).get();
      event2.addAttribute("translation", translation);
      event2.addAttribute("enableConstraint", false);
      event2.addAttribute("useGlobalCoordinates", true);

      //Add it to the event queue
      mediator().addEvent(event2);
      //Create the event
      InterestingEvent message2 = new InterestingEvent("FWD_MOVE_TO_TARGET_MESSAGE", "Message", _last_time_event, 0, 1);
      //Add the convenient attributes
      message2.addAttribute("message", "Step 2: Assume that end effector reach the desired position");
      //Add it to the event queue
      mediator().addEvent(message2);
      _last_time_event++;
    }
    for (int i = chain.size() - 2; i >= 0; i--) {
      Vector pos_i = _positions.get(i);
      Vector pos_i1 = _positions.get(i + 1);
      float dist_i = _distances.get(i + 1);
      if (dist_i <= 10e-4) {
        //As rigid segment between J_i and J_i1 lies on same position, J_i must match exactly J_i1 position
        _positions.set(i, pos_i1.get());
        if (_enableHistory) {
          history().addNodeState("Forward step", chain.get(i), chain.get(i).reference(), pos_i1.get(), null);
          history().incrementStep();
        }

        if(_enableMediator) {
          //Create the event
          InterestingEvent event = new InterestingEvent("FWD_FIX_LENGTH", "NodeTranslation", _last_time_event, 1, 2);
          //Add the convenient attributes
          event.addAttribute("node", chain.get(i));
          Vector translation = null;
          if(chain.get(i).reference() != null)
           translation = Vector.subtract(pos_i1, chain.get(i).position());
          else
            translation = pos_i1;
          event.addAttribute("translation", translation);
          event.addAttribute("enableConstraint", false);
          event.addAttribute("modifyChildren", false);
          event.addAttribute("useGlobalCoordinates", true);

          //Add it to the event queue
          mediator().addEvent(event);
          InterestingEvent message = new InterestingEvent("FWD_MOVE_TO_TARGET_MESSAGE", "Message", _last_time_event, 0, 1);
          //Add the convenient attributes
          message.addAttribute("message", "Step 3: Fix the bone's length");
          //Add it to the event queue
          mediator().addEvent(message);
          _last_time_event++;
        }
        continue;
      }

      Properties props_i = _properties.get(chain.get(i).id());
      Properties props_i1 = _properties.get(chain.get(i + 1).id());
      //TODO : Is it necessary to check children?
      //if(chain.get(i).children().size() < 2 && chain.get(i).constraint() != null && opt < 1){
      if ((chain.get(i).constraint() != null && _keepDirection) || props_i1._enableDirectionWeight) {
        /*
         * H1 : If Joint has a constraint it is desirable that:
         * a) J_i reach its target position
         * b) J_i keeps its orientation
         * */
        Vector o_hat = chain.get(i + 1).position();
        Vector tr = Vector.subtract(pos_i, pos_i1);
        Vector n_tr = Vector.subtract(pos_i, o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(), rand ? (float) (Math.abs(r.nextGaussian())) : 1 * props_i1._directionWeight * delta.angle());
        Vector desired = delta.rotate(tr);
        _positions.set(i, Vector.add(pos_i1, desired));
      }
      if (props_i1._useConstraint) {
        pos_i = _constrainForwardReaching(chain, i);
      }
      if (props_i._enableFixWeight) {
        _positions.set(i, _move(pos_i1, pos_i, dist_i, props_i._fixWeight));
      } else {
        _positions.set(i, _move(pos_i1, pos_i, dist_i, 0));
      }

      if (_enableHistory) {
        history().addNodeState("Forward step", chain.get(i), chain.get(i).reference(), _positions.get(i).get(), null);
        history().incrementStep();
      }

      if(_enableMediator) {
        //Create the event
        InterestingEvent event = new InterestingEvent("FWD_FIX_LENGTH", "NodeTranslation", _last_time_event, 1, 2);
        //Add the convenient attributes
        event.addAttribute("node", chain.get(i));
        Vector translation = null;
        if(chain.get(i).reference() != null)
          translation = Vector.subtract(_positions.get(i), chain.get(i).position());
        else
          translation = _positions.get(i);
        event.addAttribute("translation", translation);
        event.addAttribute("enableConstraint", false);
        event.addAttribute("modifyChildren", false);
        event.addAttribute("useGlobalCoordinates", true);

        //Add it to the event queue
        mediator().addEvent(event);
        //Create the event
        InterestingEvent message = new InterestingEvent("FWD_MOVE_TO_TARGET_MESSAGE", "Message", _last_time_event, 0, 1);
        //Add the convenient attributes
        message.addAttribute("message", "Step 3: Fix the bone's length");
        //Add it to the event queue
        mediator().addEvent(message);
        _last_time_event++;
      }
      change += Vector.distance(pos_i, _positions().get(i));
    }
    if (_enableHistory) history().incrementIteration();
    return change;
  }

  protected static float _forwardReaching(List<? extends Node> chain, ArrayList<Vector> positions, ArrayList<Float> distances, HashMap<Integer, Properties> properties, boolean kd) {
    float change = 0;
    for (int i = chain.size() - 2; i >= 0; i--) {
      Vector pos_i = positions.get(i);
      Vector pos_i1 = positions.get(i + 1);
      float dist_i = distances.get(i + 1);
      if (dist_i <= 10e-4) {
        //As rigid segment between J_i and J_i1 lies on same position, J_i must match exactly J_i1 position
        positions.set(i, pos_i1.get());
        continue;
      }

      Properties props_i = properties.get(chain.get(i).id());
      Properties props_i1 = properties.get(chain.get(i + 1).id());
      //TODO : Is necessary to check children?
      //if(chain.get(i).children().size() < 2 && chain.get(i).constraint() != null && opt < 1){
      if (chain.get(i).constraint() != null && kd) {
        Vector o_hat = chain.get(i + 1).position();
        Vector tr = Vector.subtract(pos_i, pos_i1);
        Vector n_tr = Vector.subtract(pos_i, o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(), rand ? (float) (Math.abs(r.nextGaussian())) : 1 * props_i1._directionWeight * delta.angle());
        Vector desired = delta.rotate(tr);
        positions.set(i, Vector.add(pos_i1, desired));
      }
      if (props_i1._useConstraint) {
        pos_i = _constrainForwardReaching(chain, positions, i);
      }
      if (props_i._enableFixWeight) {
        positions.set(i, _move(pos_i1, pos_i, dist_i, props_i._fixWeight));
      } else {
        positions.set(i, _move(pos_i1, pos_i, dist_i, 0));
      }
      change += Vector.distance(pos_i, positions.get(i));
    }
    return change;
  }

  //TODO : Check for scaling when chain has constraints
  protected static float _backwardReaching(List<? extends Node> chain, ArrayList<Vector> positions, ArrayList<Float> distances, HashMap<Integer, Properties> properties, boolean kd, Vector o) {
    float change = 0;
    Quaternion orientation;
    float magnitude;
    orientation = chain.get(0).reference() != null ? chain.get(0).reference().orientation() : new Quaternion();
    magnitude = chain.get(0).reference() != null ? chain.get(0).reference().scaling() : 1;

    Vector o_hat = o;
    for (int i = 0; i < chain.size() - 1; i++) {
      if (distances.get(i + 1) <= 10e-4) {
        positions.set(i + 1, positions.get(i)); //keep pos
        orientation.compose(chain.get(i).rotation()); //update orientation
        continue;
      }
      magnitude *= chain.get(i).scaling();
      //Find delta rotation
      Properties props_i = properties.get(chain.get(i).id());
      Properties props_i1 = properties.get(chain.get(i + 1).id());
      //TODO : Is necessary to check children?
      //if(chain.get(i).children().size() < 2 && chain.get(i).constraint() != null && opt < 1){
      if (chain.get(i).constraint() != null && kd) {
        Vector tr = Vector.subtract(positions.get(i + 1), chain.get(i).position());
        Vector n_tr = Vector.subtract(positions.get(i + 1), o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(), rand ? (float) (Math.abs(r.nextGaussian())) : 1 * props_i1._directionWeight * delta.angle());
        Vector desired = delta.rotate(tr);
        o_hat = positions.get(i + 1);
        positions.set(i + 1, Vector.add(chain.get(i).position(), desired));
      } else {
        o_hat = positions.get(i + 1);
      }
      Vector newTranslation = Quaternion.compose(orientation, chain.get(i).rotation()).inverse().rotate(Vector.divide(Vector.subtract(positions.get(i + 1), positions.get(i)), magnitude));
      Quaternion deltaRotation = new Quaternion(chain.get(i + 1).translation(), newTranslation);
      //Apply delta rotation
      Quaternion prev = chain.get(i).rotation().get();
      if (props_i._useConstraint) chain.get(i).rotate(deltaRotation);
      else {
        chain.get(i).setRotation(Quaternion.compose(chain.get(i).rotation(), deltaRotation));
      }
      //Get how much change was applied:
      change = Math.max(Quaternion.compose(prev.inverse(), chain.get(i).rotation()).angle(), change);
      orientation.compose(chain.get(i).rotation());
      Vector constrained_pos = orientation.rotate(chain.get(i + 1).translation().get());
      constrained_pos.multiply(magnitude);
      constrained_pos.add(positions.get(i));
      //change += Vector.distance(_positions.get(i + 1), constrained_pos);
      positions.set(i + 1, constrained_pos);
    }
    return change;
  }

  protected float _backwardReaching(List<? extends Node> chain, Vector o) {
    return _backwardReaching(chain, o, 0);
  }

  //TODO : All this code is a mess, remove duplicate code later, this is just to prove ideas
  protected float _backwardReaching(List<? extends Node> chain, Vector o, int start) {
    float change = 0;
    Quaternion orientation;
    float magnitude;
    orientation = chain.get(0).reference() != null ? chain.get(0).reference().orientation() : new Quaternion();
    magnitude = chain.get(0).reference() != null ? chain.get(0).reference().scaling() : 1;

    if(_enableMediator) {
      //Create the event
      InterestingEvent event1 = new InterestingEvent("BKW_SET_TARGET", "Trajectory", _last_time_event, 1, 2);
      //Add the convenient attributes
      event1.addAttribute("positions", o, chain.get(0).position());
      //Add it to the event queue
      mediator().addEvent(event1);
      //Create the event
      InterestingEvent message1 = new InterestingEvent("BKW_SET_TARGET_MESSAGE", "Message", _last_time_event, 0, 1);
      //Add the convenient attributes
      message1.addAttribute("message", "Step 4: Find the segment line defined by the root and the initial position");
      //Add it to the event queue
      mediator().addEvent(message1);
      _last_time_event++;
      //Create the event
      InterestingEvent event2 = new InterestingEvent("FWD_MOVE_TO_INIT", "NodeTranslation", _last_time_event, 1, 1);
      //Add the convenient attributes
      event2.addAttribute("node", chain.get(0));
      Vector translation = null;
      if(chain.get(0).reference() != null)
        translation = Vector.subtract(o, _positions.get(0));
      else
        translation = Vector.multiply(o,-1);
      event2.addAttribute("translation", translation);
      event2.addAttribute("enableConstraint", false);
      event2.addAttribute("modifyChildren", false);
      event2.addAttribute("useGlobalCoordinates", true);

      //Add it to the event queue
      mediator().addEvent(event2);
      //Create the event
      InterestingEvent message2 = new InterestingEvent("BKW_MOVE_TO_INIT_MESSAGE", "Message", _last_time_event, 0, 1);
      //Add the convenient attributes
      message2.addAttribute("message", "Step 5: Assume that root reach the initial position");
      //Add it to the event queue
      mediator().addEvent(message2);
      _last_time_event++;
    }


    Vector o_hat = o;

    for(int i = 0; i < start; i++){
      orientation.compose(chain.get(i).rotation());
    }

    for (int i = start; i < chain.size() - 1; i++) {
      if (_distances.get(i + 1) <= 10e-4) {
        _positions.set(i + 1, _positions.get(i)); //keep pos
        orientation.compose(chain.get(i).rotation()); //update orientation
        //Animation
        if (_enableHistory) {
          history().addNodeState("Backward step", chain.get(i + 1), chain.get(i + 1).reference(), _positions.get(i + 1).get(), null);
          history().incrementStep();
        }

        if(_enableMediator) {
          //Create the event
          InterestingEvent event = new InterestingEvent("BKW_FIX_LENGTH", "NodeTranslation", _last_time_event, 1, 2);
          //Add the convenient attributes
          event.addAttribute("node", chain.get(i + 1));
          Vector translation = null;
          if(chain.get(i + 1).reference() != null)
            translation = Vector.subtract(_positions.get(i), chain.get(i + 1).position());
          else
            translation = _positions.get(i);
          event.addAttribute("translation", translation);
          event.addAttribute("enableConstraint", false);
          event.addAttribute("modifyChildren", false);
          event.addAttribute("useGlobalCoordinates", true);

          //Add it to the event queue
          mediator().addEvent(event);
          InterestingEvent message = new InterestingEvent("BKW_MOVE_TO_TARGET_MESSAGE", "Message", _last_time_event, 0, 1);
          //Add the convenient attributes
          message.addAttribute("message", "Step 6: Fix the bone's length");
          //Add it to the event queue
          mediator().addEvent(message);
          _last_time_event++;
        }

        continue;
      }
      magnitude *= chain.get(i).scaling();
      //Find delta rotation
      Properties props_i = _properties.get(chain.get(i).id());
      Properties props_i1 = _properties.get(chain.get(i + 1).id());
      Vector prevPos = _positions.get(i+1);
      //TODO : Is necessary to check children?
      //if(chain.get(i).children().size() < 2 && chain.get(i).constraint() != null && opt < 1){
      if (chain.get(i).constraint() != null && _keepDirection) {
        Vector tr = Vector.subtract(_positions.get(i + 1), chain.get(i).position());
        Vector n_tr = Vector.subtract(_positions.get(i + 1), o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(), rand ? (float) (Math.abs(r.nextGaussian())) : 1 * props_i1._directionWeight * delta.angle());
        Vector desired = delta.rotate(tr);
        o_hat = _positions.get(i + 1);
        _positions.set(i + 1, Vector.add(chain.get(i).position(), desired));
      } else {
        o_hat = _positions.get(i + 1);
      }
      Vector newTranslation = Quaternion.compose(orientation, chain.get(i).rotation()).inverse().rotate(Vector.divide(Vector.subtract(_positions.get(i + 1), _positions.get(i)), magnitude));
      Quaternion deltaRotation = new Quaternion(chain.get(i + 1).translation(), newTranslation);
      //Apply delta rotation
      Quaternion prev = chain.get(i).rotation().get();
      if (props_i._useConstraint) chain.get(i).rotate(deltaRotation);
      else {
        chain.get(i).setRotation(Quaternion.compose(chain.get(i).rotation(), deltaRotation));
      }
      //Get how much change was applied:
      float jointChange = Quaternion.compose(prev.inverse(), chain.get(i).rotation()).angle();
      change = Math.max(jointChange, change);

      _jointChange.set(i, _jointChange.get(i) + jointChange);
      orientation.compose(chain.get(i).rotation());
      _orientations.set(i, orientation.get());
      Vector constrained_pos = orientation.rotate(chain.get(i + 1).translation().get());
      constrained_pos.multiply(magnitude);
      constrained_pos.add(_positions.get(i));
      //change += Vector.distance(_positions.get(i + 1), constrained_pos);
      _positions.set(i + 1, constrained_pos);

      if (_enableHistory) {
        history().addNodeState("Backward step", chain.get(i + 1), chain.get(i + 1).reference(), _positions.get(i + 1).get(), null);
        history().incrementStep();
      }
      if(_enableMediator) {
        //Create the event
        InterestingEvent event = new InterestingEvent("BKW_FIX_LENGTH", "NodeTranslation", _last_time_event, 1, 2);
        //Add the convenient attributes
        event.addAttribute("node", chain.get(i + 1));
        Vector translation = null;
        if(chain.get(i + 1).reference() != null)
          translation = Vector.subtract(_positions.get(i + 1), prevPos);
        else
          translation = _positions.get(i + 1);
        event.addAttribute("translation", translation);
        event.addAttribute("enableConstraint", false);
        event.addAttribute("modifyChildren", false);
        event.addAttribute("useGlobalCoordinates", true);
        //Add it to the event queue
        mediator().addEvent(event);
        //Create the event
        InterestingEvent message = new InterestingEvent("BKW_MOVE_TO_TARGET_MESSAGE", "Message", _last_time_event, 0, 1);
        //Add the convenient attributes
        message.addAttribute("message", "Step 6: Fix the bone's length");
        //Add it to the event queue
        mediator().addEvent(message);
        _last_time_event++;
      }

    }
    if (_enableHistory) history().incrementIteration();
    return change;
  }

  /*
   * Check the type of the constraint related to the Frame Parent (at the i-th position),
   * Frame J is the frame used to verify if the orientation of Parent is appropriate,
   * Vector o is a Vector where Parent is located, whereas p is express the position of J
   * Vector q is the position of Child of J.
   * */

  public Vector _constrainForwardReaching(List<? extends Node> chain, int i) {
    Node j = chain.get(i + 1);
    Vector o = _positions.get(i);
    Vector p = _positions.get(i + 1);
    Vector q = i + 2 >= chain.size() ? null : _positions.get(i + 2);

    if (j.constraint() instanceof ConeConstraint) {
      if (q == null) return o.get();
      Vector x = chain.get(i + 1).displacement(Vector.subtract(chain.get(i).position(), chain.get(i + 1).position()));
      Vector y = chain.get(i + 1).displacement(Vector.subtract(chain.get(i + 2).position(), chain.get(i + 1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i + 1).displacement(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i + 1).displacement(w);
      Quaternion delta = new Quaternion(z, y);

      w = delta.rotate(w);
      Quaternion desired = new Quaternion(w, x);
      ConeConstraint constraint = (ConeConstraint) chain.get(i + 1).constraint();
      Quaternion constrained = constraint.constrainRotation(desired, chain.get(i + 1));

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i + 1).worldDisplacement(target);
      target.add(p);
      return target;
    } else if (j.constraint() instanceof Hinge) {
      if (q == null) return o.get();
      Vector x = chain.get(i + 1).displacement(Vector.subtract(chain.get(i).position(), chain.get(i + 1).position()));
      Vector y = chain.get(i + 1).displacement(Vector.subtract(chain.get(i + 2).position(), chain.get(i + 1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i + 1).displacement(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i + 1).displacement(w);

      Quaternion delta = new Quaternion(z, y);

      w = delta.rotate(w);
      Hinge constraint = (Hinge) chain.get(i + 1).constraint();

      //project w to plane
      //TODO : Check Projection on plane - Axis
      //w = Vector.projectVectorOnPlane(w, constraint.axis());

      Quaternion desired = new Quaternion(w, x);

      Quaternion constrained = constraint.constrainRotation(desired, chain.get(i + 1));

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i + 1).worldDisplacement(target);
      target.add(p);
      return target;
    } else if (j.constraint() instanceof DistanceFieldConstraint) {
      if (q == null) return o.get();
      Vector x = chain.get(i + 1).displacement(Vector.subtract(chain.get(i).position(), chain.get(i + 1).position()));
      Vector y = chain.get(i + 1).displacement(Vector.subtract(chain.get(i + 2).position(), chain.get(i + 1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i + 1).displacement(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i + 1).displacement(w);
      Quaternion delta = new Quaternion(z, y);

      w = delta.rotate(w);
      Quaternion desired = new Quaternion(w, x);
      DistanceFieldConstraint constraint = (DistanceFieldConstraint) chain.get(i + 1).constraint();
      Quaternion constrained = Quaternion.compose(chain.get(i + 1).rotation().get(), desired);
      constrained = constraint.apply(constrained);
      constrained = Quaternion.compose(chain.get(i + 1).rotation().inverse(), constrained);

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i + 1).worldDisplacement(target);
      target.add(p);

      return target;
    }
    return o.get();
  }

  public static Vector _constrainForwardReaching(List<? extends Node> chain, List<Vector> positions, int i) {
    Node j = chain.get(i + 1);
    Vector o = positions.get(i);
    Vector p = positions.get(i + 1);
    Vector q = i + 2 >= chain.size() ? null : positions.get(i + 2);

    if (j.constraint() instanceof ConeConstraint) {
      if (q == null) return o.get();
      Vector x = chain.get(i + 1).displacement(Vector.subtract(chain.get(i).position(), chain.get(i + 1).position()));
      Vector y = chain.get(i + 1).displacement(Vector.subtract(chain.get(i + 2).position(), chain.get(i + 1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i + 1).displacement(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i + 1).displacement(w);
      Quaternion delta = new Quaternion(z, y);

      w = delta.rotate(w);
      Quaternion desired = new Quaternion(w, x);
      ConeConstraint constraint = (ConeConstraint) chain.get(i + 1).constraint();
      Quaternion constrained = constraint.constrainRotation(desired, chain.get(i + 1));

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i + 1).worldDisplacement(target);
      target.add(p);
      return target;
    } else if (j.constraint() instanceof Hinge) {
      if (q == null) return o.get();
      Vector x = chain.get(i + 1).displacement(Vector.subtract(chain.get(i).position(), chain.get(i + 1).position()));
      Vector y = chain.get(i + 1).displacement(Vector.subtract(chain.get(i + 2).position(), chain.get(i + 1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i + 1).displacement(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i + 1).displacement(w);

      Quaternion delta = new Quaternion(z, y);

      w = delta.rotate(w);
      Hinge constraint = (Hinge) chain.get(i + 1).constraint();

      //project w to plane
      // TODO : Check Projection on plane
      //w = Vector.projectVectorOnPlane(w, constraint.axis());

      Quaternion desired = new Quaternion(w, x);

      Quaternion constrained = constraint.constrainRotation(desired, chain.get(i + 1));

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i + 1).worldDisplacement(target);
      target.add(p);
      return target;
    } else if (j.constraint() instanceof DistanceFieldConstraint) {
      if (q == null) return o.get();
      Vector x = chain.get(i + 1).displacement(Vector.subtract(chain.get(i).position(), chain.get(i + 1).position()));
      Vector y = chain.get(i + 1).displacement(Vector.subtract(chain.get(i + 2).position(), chain.get(i + 1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i + 1).displacement(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i + 1).displacement(w);
      Quaternion delta = new Quaternion(z, y);

      w = delta.rotate(w);
      Quaternion desired = new Quaternion(w, x);
      DistanceFieldConstraint constraint = (DistanceFieldConstraint) chain.get(i + 1).constraint();
      Quaternion constrained = Quaternion.compose(chain.get(i + 1).rotation().get(), desired);
      constrained = constraint.apply(constrained);
      constrained = Quaternion.compose(chain.get(i + 1).rotation().inverse(), constrained);

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i + 1).worldDisplacement(target);
      target.add(p);

      return target;
    }
    return o.get();
  }

  protected void _forwardTwist(ArrayList<Vector> positions, int idx, Vector o, Vector p, Vector q, Vector t) {
    //1. Get The vector normal to Plane of interest
    Vector n = Vector.subtract(p, o);
    //2. Project Vector q,p into Plane
    Vector q_proj = Vector.projectVectorOnPlane(Vector.subtract(q, p), n);
    //3. Project Vector t,p into Plane
    Vector t_proj = Vector.projectVectorOnPlane(Vector.subtract(t, p), n);
    //4. Find angle between projected Vectors
    Quaternion theta = new Quaternion(q_proj, t_proj);
    //5. Apply rotation and update Positions from o to head
    _applyRotation(true, positions, idx, theta);

  }

  float min_v = 1e10f, min_u = 1e10f;

  protected static void _applyTwistRotation(List<? extends Node> chain, Vector t) {
    //Change chain state in such a way that the target approach to chain
    //TODO : consider alternatives to twisting approach (is it possible as intermediary step)
    Vector eff = chain.get(chain.size() - 1).position();
    for (int i = chain.size() - 2; i >= 0; i--) {
      Node f_i = chain.get(i);
      //Project Vectors to Plane given by Twist Vector
      Vector twist = chain.get(i + 1).translation();
      //Get Vector from EFF to this Joint
      Vector localEff = f_i.location(eff);
      Vector localTarget = f_i.location(t);
      Vector v, u;
      try {
        v = Vector.projectVectorOnPlane(localEff, twist);
        //Get Vector from EFF to this Joint
        u = Vector.projectVectorOnPlane(localTarget, twist);
      } catch (Exception e) {
        continue;
      }
      //Perform this operation only when Projected Vectors have not a despicable length
      if (v.magnitude() > 0.5 * localEff.magnitude() && u.magnitude() > 0.5 * localEff.magnitude()) {
        Quaternion q = new Quaternion(v, u);
        if (debug) {
          System.out.println("+-------------------+");
          System.out.println("ON APPLY TWIST MEDHOD");
          System.out.println("NODE : " + i);
          System.out.println("--> Local EFF : " + localEff);
          System.out.println("--> Local Target : " + localTarget);
          System.out.println("--> Local EFF Proj : " + v);
          System.out.println("--> Local Target Proj : " + u);
          System.out.println("--> Twist : " + twist);
          System.out.println("--> Twist Rotation: " + q.axis() + " angle : " + Math.toDegrees(q.angle()));
          System.out.println("+-------------------+");
        }
        //Perform this operation only when change is not despicable
        if (q.angle() > Math.toRadians(10)) {
          f_i.rotate(new Quaternion(q.axis(), q.angle() * 0.7f)); //TODO : Apply whole rotation or part of it (explore)
          eff = chain.get(chain.size() - 1).position();
        }
      }
    }
  }

  protected static void _applyRotation(boolean reverse, ArrayList<Vector> positions, int idx, Quaternion q) {
    Vector v_i = positions.get(idx);
    int update = reverse ? -1 : 1;
    for (int i = idx + update; reverse ? i >= 0 : i < positions.size(); i = i + update) {
      Vector v_j = Vector.subtract(positions.get(i), v_i);
      v_j = q.rotate(v_j);
      v_j.add(v_i);
      positions.set(i, v_j);
    }
  }

  protected float _distance(ArrayList<? extends Node> chain) {
    float distance = 0.f;
    for (int i = 0; i < chain.size(); i++) {
      distance += Vector.distance(chain.get(i).position(), _positions.get(i));
    }
    return distance;
  }

  protected static List<Node> _copy(List<? extends Node> chain, Node reference) {
    List<Node> copy = new ArrayList<Node>();
    for (Node joint : chain) {
      Node newJoint = new Node();
      newJoint.setReference(reference);
      newJoint.setPosition(joint.position().get());
      newJoint.setOrientation(joint.orientation().get());
      newJoint.setConstraint(joint.constraint());
      copy.add(newJoint);
      reference = newJoint;
    }
    return copy;
  }

  protected static List<Node> _copy(List<? extends Node> chain) {
    Node reference = chain.get(0).reference();
    if (reference != null) {
      reference = new Node(reference.position().get(), reference.orientation().get(), 1);
    }
    return _copy(chain, reference);
  }

  /*TODO: remove this! (debug purposes)*/
  protected static List<Node> _copy(List<? extends Node> chain, Node reference, Scene scene) {
    Node ref = reference;
    List<Node> copy = new ArrayList<Node>();
    if(ref == null){
      reference = chain.get(0).reference();
      if (reference != null) {
        ref = new Node(scene);
        ref.setPosition(reference.position().get());
        ref.setOrientation(reference.orientation().get());
        ref.enableTracking(false);
      }
    }

    for (Node joint : chain) {
      Joint newJoint = new Joint(scene, scene.context().color(0,255,0,200), 3);
      if(copy.isEmpty()){
        newJoint.setRoot(true);
      }
      newJoint.setReference(ref);
      newJoint.setPosition(joint.position().get());
      newJoint.setOrientation(joint.orientation().get());
      newJoint.setConstraint(joint.constraint());
      copy.add(newJoint);
      ref = newJoint;
    }
    return copy;
  }

  public ArrayList<Vector> positions() {
    return _positions;
  }

  public FABRIKSolver() {
    super();
  }

  //Animation Stuff
  public IKAnimation.NodeStates history() {
    return _history;
  }

  public void enableHistory(boolean enable) {
    _enableHistory = enable;
  }

}