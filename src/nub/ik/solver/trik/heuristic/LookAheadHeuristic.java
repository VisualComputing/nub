package nub.ik.solver.trik.heuristic;

import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.NodeState;
import nub.ik.solver.trik.State;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.List;

public class LookAheadHeuristic extends Heuristic {

  protected ForwardHeuristic _heuristic;
  protected int _lookAhead = 3; //how many action to look in the future

  public LookAheadHeuristic(ForwardHeuristic heuristic) {
    super(heuristic._context);
    _heuristic = heuristic;
    _states = new ArrayList<>();
  }

  @Override
  public void prepare() {
    _heuristic.prepare();
  }

  @Override
  public void applyActions(int i) {
    //Keep state of joints that are modified by the heuristic
    List<NodeInformation> chain = _context.usableChainInformation();
    NodeState initialState = new NodeState(_context.endEffectorInformation());
    for (int k = i; k < Math.min(chain.size() - 1, i + _lookAhead); k++) {
      System.out.println(k + "initial : " + chain.get(k).positionCache());
    }

    NodeState finalState = _applyActions(chain, i, 0);
    _context.endEffectorInformation().setCache(initialState.position().get(), initialState.orientation().get());
    _context.endEffectorInformation().node().setRotation(initialState.rotation());

    for (int k = i; k < Math.min(chain.size() - 1, i + _lookAhead); k++) {
      System.out.println(k + "after : " + chain.get(k).positionCache());
    }
    //Apply fixing rotation based on initial and final states
    _heuristic.applyActions(i);
    Vector p = chain.get(i).locationWithCache(_context.endEffectorInformation());
    Vector q = chain.get(i).locationWithCache(finalState.position());
    Quaternion delta = new Quaternion(p, q);
    //chain.get(i).rotateAndUpdateCache(delta, true, _context.endEffectorInformation());
  }

  protected NodeState _applyActions(List<NodeInformation> chain, int i, int k) {
    if (k == _lookAhead + 1 || i + k == _context.last()) {
      //Return the state of the Node j_{i + k}
      NodeState finalState = new NodeState(_context.endEffectorInformation());
      return finalState;
    }
    NodeState state = new NodeState(_context.usableChainInformation().get(i + k));
    if (k != 0) _context.usableChainInformation().get(i + k).updateCacheUsingReference();
    _heuristic.applyActions(i + k);
    NodeState finalState = _applyActions(chain, i, k + 1);
    _context.usableChainInformation().get(i + k).setCache(state.position().get(), state.orientation().get());
    _context.usableChainInformation().get(i + k).node().setRotation(state.rotation());
    return finalState;
  }


  @Override
  public NodeInformation[] nodesToModify(int i) {
    return new NodeInformation[0];
  }

  /**
   * There will be cases in which we modify the chain based on a heuristic and look at some intermediary solution.
   * This solution could give us information of better local actions.
   * <p>
   * To do so, we require to save and restore previous states.
   */
  protected List<State> _states;

  //saves the state of the specified nodes of usable chain
  public void saveState(Heuristic heuristic, int i) {
    State state = new State();
    for (NodeInformation ni : heuristic.nodesToModify(i)) {
      state.addNodeState(ni);
    }
    _states.add(state);
  }

  //restore the last state saved
  public void restoreState() {
    State state = _states.remove(_states.size() - 1);
    state.restoreState();
  }

}
