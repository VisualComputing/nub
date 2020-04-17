package nub.ik.animation;

import nub.core.Graph;
import nub.core.Node;
import nub.ik.visual.Joint;
import nub.processing.Scene;
import processing.core.PShape;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static processing.core.PConstants.ELLIPSE;
import static processing.core.PConstants.SPHERE;

/**
    Wrapper class that facilitates the creation of an Skeleton, its manipulation and its animation.
*/
public class Skeleton {
    HashMap<String, Node> _joints;
    HashMap<Node, String> _names;
    HashMap<String, Node> _targets;
    Node _reference;
    Scene _scene;

    public Skeleton(Scene scene){
        _joints = new HashMap<String, Node>();
        _names = new HashMap<Node, String>();
        _targets = new HashMap<String, Node>();
        _reference = new Node(); //dummy node to contain all generated Joints
        _reference.enableTagging(false);
        _scene = scene;
    }

    public Skeleton(Scene scene, String file){
        this(scene);
        _load(file);
    }


    public Joint addJoint(String name, int color, float radius){
        int r = (int) _scene.context().red(color);
        int g = (int) _scene.context().green(color);
        int b = (int) _scene.context().blue(color);
        return addJoint(name, r, g, b, radius);
    }

    public Joint addJoint(String name, int red, int green, int blue, float radius){
        if(_joints.containsKey(name)){
            Node node = _joints.remove(name);
            _names.remove(node);
        }

        Joint joint = new Joint(red, green, blue, radius);
        _joints.put(name, joint);
        _names.put(joint, name);
        joint.setReference(_reference);
        joint.setRoot(true);
        return joint;
    }

    public Joint addJoint(String name, float radius){
        return addJoint(name, (int) (255 * Math.random()), (int) (255 * Math.random()), (int) (255 * Math.random()), radius);
    }

    public Joint addJoint(String name){
        return addJoint(name, _scene.radius() * 0.05f);
    }


    public Joint addJoint(String name, String reference, int color, float radius){
        int r = (int) _scene.context().red(color);
        int g = (int) _scene.context().green(color);
        int b = (int) _scene.context().blue(color);
        return addJoint(name, reference, r, g, b, radius);
    }

    public Joint addJoint(String name, String reference, int red, int green, int blue, float radius){
        if(_joints.containsKey(name)){
            Node node = _joints.remove(name);
            _names.remove(node);
        }
        Joint joint = new Joint(red, green, blue, radius);
        _joints.put(name, joint);
        _names.put(joint, name);
        joint.setReference(_joints.get(reference));
        return joint;
    }

    public Joint addJoint(String name, String reference){
        return  addJoint(name, reference, (int) (255 * Math.random()), (int) (255 * Math.random()), (int) (255 * Math.random()), _scene.radius() * 0.05f);
    }

    public void addJoint(String name, String reference, Node node){
        if(_joints.containsKey(name)){
            Node n = _joints.remove(name);
            _names.remove(n);
        }
        _joints.put(name, node);
        _names.put(node, name);
        node.setReference(_joints.get(reference));
    }

    public void addJoint(String name, Node node){
        if(_joints.containsKey(name)){
            Node n = _joints.remove(name);
            _names.remove(n);
        }
        _joints.put(name, node);
        _names.put(node, name);
        node.setReference(_reference);
    }

    public Node addTarget(String name, float radius){
        Node endEffector = _joints.get(name);
        //Create a Basic target
        PShape redBall;
        if(_scene.is3D())
            redBall = _scene.context().createShape(SPHERE, radius);
        else
            redBall = _scene.context().createShape(ELLIPSE, 0,0, 2 *radius, 2 * radius);
        redBall.setFill(_scene.context().color(255,0,0));
        redBall.setStroke(false);
        Node target = new Node(redBall);
        _targets.put(name, target);
        target.setReference(_reference);
        target.setPosition(endEffector.position().get());
        target.setOrientation(endEffector.orientation().get());
        _scene.addIKTarget(endEffector, target);
        return target;
    }

    public Node addTarget(String name){
        return addTarget(name, _scene.radius() * 0.07f);
    }

    public void addTarget(String name, Node target){
        target.setReference(_reference);
        _scene.addIKTarget(_joints.get(name), target);
    }

    public Node joint(String name){
        return _joints.get(name);
    }

    public String jointName(Node node){
        return _names.get(node);
    }

    public Node target(String name){
        return _targets.get(name);
    }

    public void enableIK(){
        for(Node child : _reference.children()){
            Graph.registerTreeSolver(child);
        }
    }

    public List<Node> joints(){
        return new ArrayList<Node>(_joints.values());
    }

    //Send the targets to the eff position / orientation
    public void restoreTargetsState(){
        for(Map.Entry<String, Node> entry : _targets.entrySet()){
            Node eff = _joints.get(entry.getKey());
            entry.getValue().setPosition(eff);
            entry.getValue().setOrientation(eff);
        }
    }

    //Load and Save Skeleton model
    protected int _saveJoints(JSONArray jsonArray, Node node, String referenceName, int i){
        if(node == null) return 0;
        if(node != _reference && !_names.containsKey(node)) return 0;
        if(node != _reference) {
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
            System.out.println("-> " + i);
            jsonArray.setJSONObject(i, joint);
        }
        int idx = i;
        String ref = _names.get(node);
        ref = ref == null ? "" : ref;
        for(Node child : node.children()){
            idx = _saveJoints(jsonArray, child,  ref, idx + 1);
        }
        return idx;
    }

    protected void _loadJoints(JSONArray jointsArray){
        for(int i = 0; i < jointsArray.size(); i++){
            JSONObject jsonJoint = jointsArray.getJSONObject(i);
            if(jsonJoint.getString("name").equals("")) continue;
            //add joint to the skeleton
            Joint joint;
            if(jsonJoint.getString("reference").equals("")){
                joint = addJoint(jsonJoint.getString("name"),
                        jsonJoint.getInt("red"),
                        jsonJoint.getInt("green"),
                        jsonJoint.getInt("blue"),
                        jsonJoint.getFloat("radius"));
            } else{
                joint = addJoint(jsonJoint.getString("name"),
                        jsonJoint.getString("reference"),
                        jsonJoint.getInt("red"),
                        jsonJoint.getInt("green"),
                        jsonJoint.getInt("blue"),
                        jsonJoint.getFloat("radius"));
            }
            joint.setTranslation(jsonJoint.getFloat("x"), jsonJoint.getFloat("y"), jsonJoint.getFloat("z"));
            joint.setRotation(jsonJoint.getFloat("q_x"), jsonJoint.getFloat("q_y"), jsonJoint.getFloat("q_z"), jsonJoint.getFloat("q_w"));
        }
    }


    protected void _saveTargets(JSONArray jsonTargets){
        int i = 0;
        for(Map.Entry<String, Node> entry : _targets.entrySet()){
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

    protected void _loadTargets(JSONArray jsonTargets){
        for(int i = 0; i < jsonTargets.size(); i++){
            JSONObject jsonTarget = jsonTargets.getJSONObject(i);
            Node target = addTarget(jsonTarget.getString("name"), jsonTarget.getFloat("radius"));
            target.setTranslation(jsonTarget.getFloat("x"), jsonTarget.getFloat("y"), jsonTarget.getFloat("z"));
            target.setRotation(jsonTarget.getFloat("q_x"), jsonTarget.getFloat("q_y"), jsonTarget.getFloat("q_z"), jsonTarget.getFloat("q_w"));
        }
    }

    public void save(String filename){
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonJoints = new JSONArray();
        JSONArray jsonTargets = new JSONArray();
        //1. Save joints
        for(String s : _names.values()){
            System.out.println(" ***** " + s);
        }

        _saveJoints(jsonJoints, _reference, "", -1);
        jsonObject.setJSONArray("Joints", jsonJoints);
        //2. Save Targets
        _saveTargets(jsonTargets);
        jsonObject.setJSONArray("Targets", jsonTargets);
        _scene.pApplet().saveJSONObject(jsonObject, filename);
    }

    protected void  _load(String filename){
        JSONObject jsonObject = _scene.pApplet().loadJSONObject(filename);
        _loadJoints(jsonObject.getJSONArray("Joints"));
        enableIK();
        _loadTargets(jsonObject.getJSONArray("Targets"));
    }




    public void cull(boolean cull){
        _reference.cull(cull);
    }
}
