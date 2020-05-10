package nub.ik.animation;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.HashMap;

public class Posture {
  protected HashMap<String, Node> _nodeInformation;

  public Posture() {
    _nodeInformation = new HashMap<String, Node>();
  }

  public Posture(Skeleton skeleton) {
    this();
    saveCurrentValues(skeleton);
  }

  public void saveCurrentValues(Skeleton skeleton) {
    Node ref = Node.detach(skeleton.reference().position().get(), skeleton.reference().orientation().get(), skeleton.reference().magnitude());
    for (Node original : skeleton.BFS()) {
      //Create a detached copy of the node basic information
      String name = skeleton.jointName(original);
      Node copy = Node.detach(new Vector(), new Quaternion(), 1);
      _nodeInformation.put(name, copy);
      //set reference
      String refName = skeleton.jointName(original.reference());
      if (refName != null) {
        copy.setReference(_nodeInformation.get(refName));
      } else {
        copy.setReference(ref);
      }
      //set values
      copy.setPosition(original.position().get());
      copy.setOrientation(original.orientation().get());
      copy.setMagnitude(original.magnitude());
    }
  }

  public void loadValues(Skeleton skeleton) {
    for (Node node : skeleton.BFS()) {
      if (node == skeleton.reference() || !skeleton._names.containsKey(node)) continue;
      String name = skeleton.jointName(node);
      if (!_nodeInformation.containsKey(name)) continue;
      Constraint constrain = node.constraint();
      node.setConstraint(null);
      Node info = _nodeInformation.get(name);
      node.setPosition(info.position().get());
      node.setOrientation(info.orientation().get());
      node.setConstraint(constrain);
    }
    skeleton.restoreTargetsState();
  }

  public Node jointState(String name) {
    return _nodeInformation.get(name);
  }

}
