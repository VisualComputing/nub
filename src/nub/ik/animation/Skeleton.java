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

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Constraint;
import nub.core.constraint.Hinge;
import nub.ik.solver.Solver;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.processing.Scene;
import processing.core.PGraphics;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Wrapper class that facilitates the creation of an Skeleton i.e. a tree of {@link Node}s,
 * its manipulation and its animation.
 */
public class Skeleton {
  HashMap<String, Node> _joints;
  HashMap<Node, String> _names;
  HashMap<Node, Constraint> _constraints;
  HashMap<Node, Solver> _solvers;
  HashMap<String, Node> _targets;

  Node _reference;
  Scene _scene;
  float _targetRadius;

  /**
   * Constructor for a Skeleton.
   * Default constructor creates an Empty Skeleton (contains no {@link Node}s).
   * Here we will prefer the term Joint over Node, being the former a Node with
   * a custom visual representation.
   * By default, a {@link Node} is represented as a small ball attached by a line
   * to its reference node.
   *
   * A skeleton could be manipulated using Inverse Kinematics solver, to do so,
   * you require to relate some targets with the appropiate end effectors. A Target
   * is represented by default as a red ball.
   * */
  public Skeleton(Scene scene) {
    _joints = new HashMap<String, Node>();
    _names = new HashMap<Node, String>();
    _targets = new HashMap<String, Node>();
    _constraints = new HashMap<Node, Constraint>();
    _solvers = new HashMap<Node, Solver>();
    _reference = new Node(); //dummy node to contain all generated Joints
    _reference.enableTagging(false);
    _scene = scene;
    _targetRadius = 0.06f * scene.radius();
  }

  /**
   * Load an Skeleton from the given file. This file must have a .json extension containing
   * the skeleton structure.
   * @see  Skeleton#save(String)
   * @see  Skeleton#_load(String)
   * */

  public Skeleton(Scene scene, String file) {
    this(scene);
    _load(file);
  }

  /**
   * Adds a Joint with the given name, color and radius to the skeleton.
   * Since Node references are handled internally and no one was specified the created node
   * will be child of the Skeleton reference.
   * @param name    The name of the new Joint
   * @param color   The rgb color of the joint
   * @param radius  The radius of the ball that represents the Joint.
   * @return the created {@link Joint}, use this reference to modify its position and orientation.
   * Warning: you must not set explicitly the reference of the created Joint.
   */
  public Joint addJoint(String name, int color, float radius) {
    int r = (int) _scene.context().red(color);
    int g = (int) _scene.context().green(color);
    int b = (int) _scene.context().blue(color);
    return addJoint(name, r, g, b, radius);
  }

  /**
   * Same as {@code return addJoint(name, color(r,g,b), radius)}
   */
  public Joint addJoint(String name, int red, int green, int blue, float radius) {
    if (_joints.containsKey(name)) {
      Node node = _joints.remove(name);
      _names.remove(node);
    }

    Joint joint = new Joint(red, green, blue, radius);
    _joints.put(name, joint);
    _names.put(joint, name);
    _constraints.put(joint, joint.constraint());
    joint.setReference(_reference);
    joint.setRoot(true);
    return joint;
  }

  /**
   * @return the Scene related with the skeleton.
   */
  public Scene scene() {
    return _scene;
  }

  /**
   * Same as {@code return addJoint(name, color(255 * random(), 255 * random(), 255 * random()), radius)}
   */
  public Joint addJoint(String name, float radius) {
    return addJoint(name, (int) (255 * Math.random()), (int) (255 * Math.random()), (int) (255 * Math.random()), radius);
  }

  /**
   * Same as {@code return addJoint(name, color(255 * random(), 255 * random(), 255 * random()), scene.radius() * 0.05)}
   */
  public Joint addJoint(String name) {
    return addJoint(name, _scene.radius() * 0.05f);
  }

  /**
   * @param name    The name of the new Joint
   * @param color   The rgb color of the joint
   * @param radius  The radius of the ball that represents the Joint.
   * @return the created {@link Joint}, use this reference to modify its position and orientation.
   * Warning: you must not set explicitly the reference of the created Joint.
   */


  /**
   * Adds a Joint with the given name, color and radius to the skeleton as a child of the Joint related with
   * the given reference name.
   * @param name    The name of the new Joint
   * @param reference The name of the new Joint's reference (must exists)
   * @param color   The rgb color of the joint
   * @param radius  The radius of the ball that represents the Joint.
   * @return
   */
  public Joint addJoint(String name, String reference, int color, float radius) {
    int r = (int) _scene.context().red(color);
    int g = (int) _scene.context().green(color);
    int b = (int) _scene.context().blue(color);
    return addJoint(name, reference, r, g, b, radius);
  }

  public Joint addJoint(String name, String reference, int red, int green, int blue, float radius) {
    if (_joints.containsKey(name)) {
      Node node = _joints.remove(name);
      _names.remove(node);
    }
    Joint joint = new Joint(red, green, blue, radius);
    _joints.put(name, joint);
    _names.put(joint, name);
    _constraints.put(joint, joint.constraint());
    joint.setReference(_joints.get(reference));
    return joint;
  }

  public Joint addJoint(String name, String reference) {
    return addJoint(name, reference, (int) (255 * Math.random()), (int) (255 * Math.random()), (int) (255 * Math.random()), _scene.radius() * 0.05f);
  }

  public void addJoint(String name, String reference, Node node) {
    if (_joints.containsKey(name)) {
      Node n = _joints.remove(name);
      _names.remove(n);
    }
    _joints.put(name, node);
    _names.put(node, name);
    _constraints.put(node, node.constraint());
    node.setReference(_joints.get(reference));
  }

  public void addJoint(String name, Node node) {
    if (_joints.containsKey(name)) {
      Node n = _joints.remove(name);
      _names.remove(n);
    }
    _joints.put(name, node);
    _names.put(node, name);
    _constraints.put(node, node.constraint());
    node.setReference(_reference);
  }

  /**
   * @return the Skeleton reference. It is created automatically when the skeleton is instantiated and
   * any other Joint must be a successor of it.
   * Reference is the identity Node by default, but if you require you could set its transformation information.
   */
  public Node reference() {
    return _reference;
  }

  /**
   * @return a List of the Joints in the Skeleton structure in a Breadth-first search (BFS) manner
   */
  public List<Node> BFS() {
    List<Node> list = new ArrayList<Node>();
    List<Node> frontier = new ArrayList<Node>();
    frontier.add(_reference);
    while (!frontier.isEmpty()) {
      Node next = frontier.remove(0);
      list.add(next);
      for (Node child : next.children()) {
        if (_names.containsKey(child))
          frontier.add(child);
      }
    }
    list.remove(0);
    return list;
  }

  /**
   * Adds a target related with an end effector (leaf of the tree structure) related with the given name.
   * @param name    The name of the end effector
   * @return  a Node that represents the target related with the corresponding end effector of the skeleton
   */
  public Node addTarget(String name) {
    Node endEffector = _joints.get(name);
    //Create a Basic target
    Node target = new Node() {
      @Override
      public void graphics(PGraphics pGraphics) {
        pGraphics.noStroke();
        pGraphics.fill(255, 0, 0);
        if (pGraphics.is3D()) pGraphics.sphere(_targetRadius);
        else pGraphics.ellipse(0, 0, 2 * _targetRadius, 2 * _targetRadius);
      }
    };
    _targets.put(name, target);
    target.setReference(_reference);
    target.setPosition(endEffector.position().get());
    target.setOrientation(endEffector.orientation().get());
    target.setPickingThreshold(0f);

    _scene.addIKTarget(endEffector, target);
    return target;
  }

  /**
   * Defines the radius of the targets.
   * @param radius
   */
  public void setTargetRadius(float radius) {
    _targetRadius = radius;
  }

  /**
   * Adds a target related with an end effector (leaf of the tree structure) related with the given name.
   * Contrary to @see Skeleton{@link #addTarget(String)} with this method you could provide any custom representation
   * of the target (by default a target is represented by a red ball).
   *
   * @param name    The name of the end effector
   * @param target  A node that represents the target to relate with the given end effector
   */
  public void addTarget(String name, Node target) {
    target.setReference(_reference);
    _scene.addIKTarget(_joints.get(name), target);
  }

  /**
   * @return a Map of the contained targets along with the name of the related End Effector
   */
  public HashMap<String, Node> targets(){
    return _targets;
  }

  /**
   * @param name    The name of a Joint in the Skeleton structure
   * @return  the Node associated with the given name.
   */
  public Node joint(String name) {
    return _joints.get(name);
  }

  /**
   * @param node  A Joint of the Skeleton structure
   * @return  the name related with the given Joint
   */
  public String jointName(Node node) {
    return _names.get(node);
  }

  public Node target(String name) {
    return _targets.get(name);
  }

  /**
   * @return a Map of the contained Nodes along with its related name.
   */
  public HashMap<String, Node> joints() {
    return _joints;
  }

  /**
   * @param name      Specify the name of the Joint
   * @return the Solver that modifies the state of the given Joint
   */
  public Solver solver(String name){
    Node node = joint(name);
    if(node == null) return null;
    Node ref = node;
    while(ref.reference() != _reference){
      ref = ref.reference();
    }
    return _solvers.get(ref);
  }

  /**
   * @return a List of Solvers related with this Skeleton (one solver per reference's child)
   */
  public List<Solver> solvers(){
    return new ArrayList<Solver>(_solvers.values());
  }

  /**
   * Changes the name of an existing Joint by a new one.
   * @param name  the current name of the Joint
   * @param newName the new name of the Joint
   */
  public void setName(String name, String newName) {
    if (!_joints.containsKey(name)) return;
    Node joint = _joints.remove(name);
    Node target = _targets.remove(name);
    _names.remove(joint);
    _joints.put(newName, joint);
    if (target != null) _targets.put(newName, target);
    _names.put(joint, newName);
  }

  /**
   * Convinient method to prune the subtree of the skeleton whose root is the given joint.
   * @see Graph#prune(Node)
   * @param joint the joint to prune.
   */
  public void prune(Node joint) {
    List<Node> branch = Graph.branch(joint);
    Graph.prune(joint);
    for (Node node : branch) {
      //remove the reference of this node
      String name = _names.get(node);
      if (name != null) {
        _joints.remove(name);
        _targets.remove(name);
        _names.remove(node);
      }
    }
  }

  /**
   * Updates the reference of the constraint nodes.
   * Call this method whenever you modify the constraint of a Joint after being added to the Skeleton.
   */
  public void updateConstraints() {
    for (Node node : BFS()) {
      _constraints.put(node, node.constraint());
    }
  }

  /**
   * Enables /disables the constraint of a Joint.
   * @param joint the joint whose constraint will be enabled
   * @param enable  defines if the joint will be enable or disabled
   */
  public void enableConstraint(Node joint, boolean enable) {
    if (enable) enableConstraint(joint);
    else disableConstraint(joint);
  }

  public void enableConstraint(Node joint) {
    joint.setConstraint(_constraints.get(joint));
  }

  public void disableConstraint(Node joint) {
    joint.setConstraint(null);
  }

  public void enableConstraints(boolean enable) {
    if (enable) enableConstraints();
    else disableConstraints();
  }

  public void enableConstraints() {
    for (Map.Entry<Node, Constraint> entry : _constraints.entrySet()) {
      entry.getKey().setConstraint(entry.getValue());
    }
  }

  public void disableConstraints() {
    for (Node node : BFS()) {
      node.setConstraint(null);
    }
  }

  /**
   * @param enable  enable / disable the IK {@link Solver}s related with this Skeleton
   */
  public void enableIK(boolean enable) {
    if (enable) enableIK();
    else disableIK();
  }

  public void enableIK() {
    for (Node child : _reference.children()) {
      if (!_solvers.containsKey(child)) {
        _solvers.put(child, Graph.registerTreeSolver(child));
      } else {
        Graph.executeSolver(_solvers.get(child));
      }
    }
  }

  public void disableIK() {
    for (Solver solver : _solvers.values()) {
      Graph.stopSolver(solver);
    }
  }

  /**
   * convinient method to relate a target with each of the End effectors of the Skeleton structure
   */
  public void addTargets() {
    for (Map.Entry<String, Node> entry : _joints.entrySet()) {
      if (entry.getValue().children().isEmpty()) {
        addTarget(entry.getKey());
      }
    }
  }

  /**
   * Same as @see Solver{@link #setMaxError(float)}
   * @param maxError
   */
  public void setMaxError(float maxError) {
    for (Solver solver : _solvers.values())
      solver.setMaxError(maxError);
  }

  /**
   * Same as @see Solver{@link #setMaxIterations(int)}
   * @param maxIterations
   */
  public void setMaxIterations(int maxIterations) {
    for (Solver solver : _solvers.values())
      solver.setMaxIterations(maxIterations);
  }

  /**
   * Same as @see Solver{@link #setMinDistance(float)}
   * @param minDistance
   */
  public void setMinDistance(float minDistance) {
    for (Solver solver : _solvers.values())
      solver.setMinDistance(minDistance);
  }

  /**
   * Same as @see Solver{@link #setTimesPerFrame(float)}
   * @param timesPerFrame
   */
  public void setTimesPerFrame(float timesPerFrame) {
    for (Solver solver : _solvers.values())
      solver.setTimesPerFrame(timesPerFrame);
  }

  /**
   * Convinient method to set the position and the orientation of each target to be the same as its corresponding end effector
   */
  public void restoreTargetsState() {
    for (Map.Entry<String, Node> entry : _targets.entrySet()) {
      Node eff = _joints.get(entry.getKey());
      entry.getValue().setPosition(eff);
      entry.getValue().setOrientation(eff);
    }
  }

  //Load and Save Skeleton model
  protected int _saveJoints(JSONArray jsonArray, Node node, String referenceName, int i) {
    if (node == null) return i;
    if (node != _reference && !_names.containsKey(node)) return i - 1;
    if (node != _reference) {
      JSONObject joint = new JSONObject();
      joint.setString("reference", referenceName);
      joint.setString("name", _names.get(node));
      joint.setFloat("x", node.translation().x());
      joint.setFloat("y", node.translation().y());
      joint.setFloat("z", node.translation().z());
      joint.setFloat("q_x", node.rotation().x());
      joint.setFloat("q_y", node.rotation().y());
      joint.setFloat("q_z", node.rotation().z());
      joint.setFloat("q_w", node.rotation().w());
      _saveConstraint(joint, node);
      if (node instanceof Joint) {
        joint.setFloat("radius", ((Joint) node).radius());
        joint.setFloat("red", ((Joint) node).red());
        joint.setFloat("green", ((Joint) node).green());
        joint.setFloat("blue", ((Joint) node).blue());
      } else {
        joint.setFloat("radius", _scene.radius() * 0.05f);
        joint.setFloat("red", 0);
        joint.setFloat("green", 255);
        joint.setFloat("blue", 0);
      }
      jsonArray.setJSONObject(i, joint);
    }
    int idx = i;
    String ref = _names.get(node);
    ref = ref == null ? "" : ref;
    for (Node child : node.children()) {
      idx = _saveJoints(jsonArray, child, ref, idx + 1);
    }
    return idx;
  }

  protected void _loadJoints(JSONArray jointsArray) {
    for (int i = 0; i < jointsArray.size(); i++) {
      JSONObject jsonJoint = jointsArray.getJSONObject(i);
      if (jsonJoint.getString("name").equals("")) continue;
      //add joint to the skeleton
      Joint joint;
      if (jsonJoint.getString("reference").equals("")) {
        joint = addJoint(jsonJoint.getString("name"),
            jsonJoint.getInt("red"),
            jsonJoint.getInt("green"),
            jsonJoint.getInt("blue"),
            jsonJoint.getFloat("radius"));
      } else {
        joint = addJoint(jsonJoint.getString("name"),
            jsonJoint.getString("reference"),
            jsonJoint.getInt("red"),
            jsonJoint.getInt("green"),
            jsonJoint.getInt("blue"),
            jsonJoint.getFloat("radius"));
      }
      joint.setTranslation(jsonJoint.getFloat("x"), jsonJoint.getFloat("y"), jsonJoint.getFloat("z"));
      joint.setRotation(jsonJoint.getFloat("q_x"), jsonJoint.getFloat("q_y"), jsonJoint.getFloat("q_z"), jsonJoint.getFloat("q_w"));
      _loadConstraint(jsonJoint, joint);
    }
  }

  protected void _saveConstraint(JSONObject jsonJoint, Node node) {
    JSONObject jsonConstraint = new JSONObject();
    Constraint constraint = node.constraint();
    if (constraint == null) {
      jsonConstraint.setString("type", "none");
    } else if (constraint instanceof Hinge) {
      Hinge h = (Hinge) node.constraint();
      jsonConstraint.setString("type", "hinge");
      jsonConstraint.setFloat("reference_x", h.idleRotation().x());
      jsonConstraint.setFloat("reference_y", h.idleRotation().y());
      jsonConstraint.setFloat("reference_z", h.idleRotation().z());
      jsonConstraint.setFloat("reference_w", h.idleRotation().w());
      jsonConstraint.setFloat("rest_x", h.restRotation().x());
      jsonConstraint.setFloat("rest_y", h.restRotation().y());
      jsonConstraint.setFloat("rest_z", h.restRotation().z());
      jsonConstraint.setFloat("rest_w", h.restRotation().w());
      jsonConstraint.setFloat("min", h.minAngle());
      jsonConstraint.setFloat("max", h.maxAngle());
    } else if (constraint instanceof BallAndSocket) {
      BallAndSocket b = (BallAndSocket) node.constraint();
      jsonConstraint.setString("type", "ball_socket");
      jsonConstraint.setFloat("reference_x", b.idleRotation().x());
      jsonConstraint.setFloat("reference_y", b.idleRotation().y());
      jsonConstraint.setFloat("reference_z", b.idleRotation().z());
      jsonConstraint.setFloat("reference_w", b.idleRotation().w());
      jsonConstraint.setFloat("rest_x", b.restRotation().x());
      jsonConstraint.setFloat("rest_y", b.restRotation().y());
      jsonConstraint.setFloat("rest_z", b.restRotation().z());
      jsonConstraint.setFloat("rest_w", b.restRotation().w());
      jsonConstraint.setFloat("min", b.minTwistAngle());
      jsonConstraint.setFloat("max", b.maxTwistAngle());
      jsonConstraint.setFloat("up", b.up());
      jsonConstraint.setFloat("down", b.down());
      jsonConstraint.setFloat("left", b.left());
      jsonConstraint.setFloat("right", b.right());
    }
    jsonJoint.setJSONObject("constraint", jsonConstraint);
  }

  protected void _loadConstraint(JSONObject jsonJoint, Node node) {
    if (!jsonJoint.hasKey("constraint")) return;
    JSONObject jsonConstraint = jsonJoint.getJSONObject("constraint");
    if (jsonConstraint.getString("type").equals("none")) {
      return;
    } else if (jsonConstraint.getString("type").equals("hinge")) {
      float min = jsonConstraint.getFloat("min");
      float max = jsonConstraint.getFloat("max");
      Quaternion reference = new Quaternion(jsonConstraint.getFloat("reference_x"),
          jsonConstraint.getFloat("reference_y"),
          jsonConstraint.getFloat("reference_z"),
          jsonConstraint.getFloat("reference_w"));
      Quaternion rest = new Quaternion(jsonConstraint.getFloat("rest_x"),
          jsonConstraint.getFloat("rest_y"),
          jsonConstraint.getFloat("rest_z"),
          jsonConstraint.getFloat("rest_w"));
      Hinge h = new Hinge(min, max);
      h.setRotations(reference, rest);
      node.setConstraint(h);
    } else if (jsonConstraint.getString("type").equals("ball_socket")) {
      float min = jsonConstraint.getFloat("min");
      float max = jsonConstraint.getFloat("max");
      float up = jsonConstraint.getFloat("up");
      float down = jsonConstraint.getFloat("down");
      float left = jsonConstraint.getFloat("left");
      float right = jsonConstraint.getFloat("right");

      Quaternion reference = new Quaternion(jsonConstraint.getFloat("reference_x"),
          jsonConstraint.getFloat("reference_y"),
          jsonConstraint.getFloat("reference_z"),
          jsonConstraint.getFloat("reference_w"));
      Quaternion rest = new Quaternion(jsonConstraint.getFloat("rest_x"),
          jsonConstraint.getFloat("rest_y"),
          jsonConstraint.getFloat("rest_z"),
          jsonConstraint.getFloat("rest_w"));
      BallAndSocket b = new BallAndSocket(down, up, left, right);
      b.setRotations(reference, rest);
      b.setTwistLimits(min, max);
      node.setConstraint(b);
    }
  }


  protected void _saveTargets(JSONArray jsonTargets) {
    int i = 0;
    for (Map.Entry<String, Node> entry : _targets.entrySet()) {
      Node node = entry.getValue();
      JSONObject jsonTarget = new JSONObject();
      jsonTarget.setString("name", entry.getKey());
      jsonTarget.setFloat("x", node.translation().x());
      jsonTarget.setFloat("y", node.translation().y());
      jsonTarget.setFloat("z", node.translation().z());
      jsonTarget.setFloat("q_x", node.rotation().x());
      jsonTarget.setFloat("q_y", node.rotation().y());
      jsonTarget.setFloat("q_z", node.rotation().z());
      jsonTarget.setFloat("q_w", node.rotation().w());

      Node joint = _joints.get(entry.getKey());
      if (joint instanceof Joint) {
        jsonTarget.setFloat("radius", ((Joint) joint).radius() * 1.2f);
      } else {
        jsonTarget.setFloat("radius", _scene.radius() * 0.07f);
      }
      jsonTargets.setJSONObject(i++, jsonTarget);
    }
  }

  protected void _loadTargets(JSONArray jsonTargets) {
    for (int i = 0; i < jsonTargets.size(); i++) {
      JSONObject jsonTarget = jsonTargets.getJSONObject(i);
      setTargetRadius(jsonTarget.getFloat("radius"));
      Node target = addTarget(jsonTarget.getString("name"));
      target.setTranslation(jsonTarget.getFloat("x"), jsonTarget.getFloat("y"), jsonTarget.getFloat("z"));
      target.setRotation(jsonTarget.getFloat("q_x"), jsonTarget.getFloat("q_y"), jsonTarget.getFloat("q_z"), jsonTarget.getFloat("q_w"));
    }
  }

  /**
   * Save the Skeleton structure in a .json file.
   * @param filename  Path in which the .json will be saved.
   */
  public void save(String filename) {
    JSONObject jsonObject = new JSONObject();
    JSONArray jsonJoints = new JSONArray();
    JSONArray jsonTargets = new JSONArray();
    _saveJoints(jsonJoints, _reference, "", -1);
    jsonObject.setJSONArray("Joints", jsonJoints);
    //2. Save Targets
    _saveTargets(jsonTargets);
    jsonObject.setJSONArray("Targets", jsonTargets);
    _scene.pApplet().saveJSONObject(jsonObject, filename);
  }

  /**
   * Load the Skeleton structure from a .json file.
   * @param filename  Path from which the .json will be loaded.
   */
  protected void _load(String filename) {
    JSONObject jsonObject = _scene.pApplet().loadJSONObject(filename);
    _loadJoints(jsonObject.getJSONArray("Joints"));
    enableIK();
    _loadTargets(jsonObject.getJSONArray("Targets"));
  }

  public void cull(boolean cull) {
    _reference.cull(cull);
  }
}
