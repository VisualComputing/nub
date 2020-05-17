package nub.ik.animation;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Posture {
  protected Node _reference;
  protected HashMap<String, Node> _nodeInformation;
  protected HashMap<Node, String> _namesInformation;

  public Posture() {
    _nodeInformation = new HashMap<String, Node>();
    _namesInformation = new HashMap<Node, String>();
  }

  public Posture(Skeleton skeleton) {
    this();
    saveCurrentValues(skeleton);
  }

  public Posture(PApplet pApplet, String file){
    this();
    _load(pApplet, file);
  }

  public void saveCurrentValues(Skeleton skeleton) {
    _reference = Node.detach(skeleton.reference().position().get(), skeleton.reference().orientation().get(), skeleton.reference().magnitude());
    _namesInformation.put(_reference, "");
    for (Node original : skeleton.BFS()) {
      //Create a detached copy of the node basic information
      String name = skeleton.jointName(original);
      Node copy = Node.detach(new Vector(), new Quaternion(), 1);
      _nodeInformation.put(name, copy);
      _namesInformation.put(copy, name);
      //set reference
      String refName = skeleton.jointName(original.reference());
      if (refName != null) {
        copy.setReference(_nodeInformation.get(refName));
      } else {
        copy.setReference(_reference);
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

  protected JSONObject _save(){
    JSONObject jsonObject = new JSONObject();
    JSONArray jsonJoints = new JSONArray();
    //traverse in a BFS manner
    List<Node> frontier = new ArrayList<Node>();
    frontier.add(_reference);
    while(!frontier.isEmpty()){
      Node next = frontier.remove(0);
      for(Node child : next.children()){
        frontier.add(child);
      }
      JSONObject joint = new JSONObject();
      if(next != _reference) joint.setString("reference", _namesInformation.get(next.reference()));
      joint.setString("name", _namesInformation.get(next));
      joint.setFloat("x", next.translation().x());
      joint.setFloat("y", next.translation().y());
      joint.setFloat("z", next.translation().z());
      joint.setFloat("q_x", next.rotation().x());
      joint.setFloat("q_y", next.rotation().y());
      joint.setFloat("q_z", next.rotation().z());
      joint.setFloat("q_w", next.rotation().w());
      jsonJoints.append(joint);
    }
    jsonObject.setJSONArray("Joints", jsonJoints);
    return jsonObject;
  }

  //Load and Save a Posture
  public void save(PApplet pApplet, String filename){
    pApplet.saveJSONObject(_save(), filename);
  }


  //Load and Save a Posture
  protected void _load(PApplet pApplet, String filename){
    JSONObject jsonObject = pApplet.loadJSONObject(filename);
    JSONArray jointsArray = jsonObject.getJSONArray("Joints");
    for(int i = 0; i < jointsArray.size(); i++) {
      JSONObject jsonJoint = jointsArray.getJSONObject(i);
      //add joint to the skeleton
      Vector translation = new Vector(jsonJoint.getFloat("x"), jsonJoint.getFloat("y"), jsonJoint.getFloat("z"));
      Quaternion rotation = new Quaternion(jsonJoint.getFloat("q_x"), jsonJoint.getFloat("q_y"), jsonJoint.getFloat("q_z"), jsonJoint.getFloat("q_w"));
      Node joint;
      if(!jsonJoint.hasKey("reference")) {
        //create the reference
        joint = _reference = Node.detach(new Vector(), new Quaternion(), 1);
      } else{
        joint = Node.detach(new Vector(), new Quaternion(), 1);
        joint.setReference(_nodeInformation.get(jsonJoint.getString("reference")));
      }
      _namesInformation.put(joint, jsonJoint.getString("name"));
      joint.setTranslation(translation);
      joint.setRotation(rotation);
    }
  }

  public static void savePostures(PApplet pApplet, List<Posture> postures, String filename){
    JSONArray array = new JSONArray();
    for(Posture posture : postures){
      array.append(posture._save());
    }
    pApplet.saveJSONArray(array, filename);
  }

  public static List<Posture> loadPostures(PApplet pApplet, String filename){
    JSONArray array = pApplet.loadJSONArray(filename);
    List<Posture> postures = new ArrayList<Posture>();
    for(int i = 0; i < array.size(); i++){
      postures.add(new Posture(pApplet, filename));
    }
    return postures;
  }
}
