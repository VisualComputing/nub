/***************************************************************************************
 * nub
 * Copyright (c) 2019-2020 Universidad Nacional de Colombia
 * @author Sebastian Chaparro Cuevas, https://github.com/VisualComputing
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.ik.animation;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A Posture saves the current sate of each joint of a skeleton to use it further for
 * create an Animation, for instance, using a {@link PostureInterpolator}.
 * A posture containts a detached copy of a Skeleton structure and relate each of the Nodes
 * with its corresponding name.
 */
public class Posture {
  protected Node _reference;
  protected HashMap<String, Node> _nodeInformation;
  protected HashMap<Node, String> _namesInformation;

  public Posture() {
    _nodeInformation = new HashMap<String, Node>();
    _namesInformation = new HashMap<Node, String>();
  }

  /**
   * Constructor for a Posture.
   * Given a Skeleton structure its current posture will be extracted for future usage.
   * @param skeleton a {@link Skeleton} structure.
   */
  public Posture(Skeleton skeleton) {
    this();
    saveCurrentValues(skeleton);
  }

  /**
   * Load a posture from the given file. A PApplet is required since it allows to easily load / save json objects.
   * @see PApplet#loadJSONObject(File)
   * @see PApplet#saveJSONObject(JSONObject, String)
   * @param pApplet
   * @param file
   */
  public Posture(PApplet pApplet, String file){
    this();
    _load(pApplet, file);
  }

  /**
   * Creates a detached copy of the {@link Skeleton} structure to use later in an animation task.
   * @param skeleton the skeleton from which the posture information will be extracted.
   */
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

  /**
   * Set the {@link Skeleton} current posture (information of each joint translation and rotation) to be
   * the same as the saved in this Posture.
   * @param skeleton
   */
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

  /**
   * @param name The name of a joint in the Posture
   * @return the joint translation and rotation related with the given name
   */
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

  /**
   * Saves a List of postures in a .json file
   * @param pApplet The current PApplet
   * @param postures The List of postures to save
   * @param filename  Path in which the postures will be saved
   */
  public static void savePostures(PApplet pApplet, List<Posture> postures, String filename){
    JSONArray array = new JSONArray();
    for(Posture posture : postures){
      array.append(posture._save());
    }
    pApplet.saveJSONArray(array, filename);
  }

  /**
   * Loads a List of postures from a .json file
   * @param pApplet The current PApplet
   * @param filename Path from which the .json will be loaded
   * @return  The list of postures contained in the given file.
   */
  public static List<Posture> loadPostures(PApplet pApplet, String filename){
    JSONArray array = pApplet.loadJSONArray(filename);
    List<Posture> postures = new ArrayList<Posture>();
    for(int i = 0; i < array.size(); i++){
      postures.add(new Posture(pApplet, filename));
    }
    return postures;
  }
}
