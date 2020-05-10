package nub.ik.solver.trik;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.solver.geometric.oldtrik.NodeInformation;

import java.util.ArrayList;
import java.util.List;

//Keeps information of the state of a Node
public class State {

  protected List<NodeState> _nodeStates;

  public State() {
    _nodeStates = new ArrayList<NodeState>();
  }

  public State(NodeInformation... nodes) {
    _nodeStates = new ArrayList<NodeState>();
    for (NodeInformation node : nodes) {
      _nodeStates.add(new NodeState(node));
    }
  }

  public void addNodeState(NodeInformation... nodes) {
    for (NodeInformation node : nodes) {
      _nodeStates.add(new NodeState(node));
    }
  }

  public void restoreState() {
    for (NodeState nodeState : _nodeStates) {
      Node node = nodeState._nodeInformation.node();
      Constraint constraint = node.constraint();
      node.setConstraint(null);
      node.setRotation(nodeState._rotation);
      node.setTranslation(nodeState._translation);
      node.setConstraint(constraint);
      nodeState._nodeInformation.setCache(nodeState._position, nodeState._orientation);
    }
  }
}
