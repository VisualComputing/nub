package nub.ik.animation;

import nub.core.Graph;
import nub.core.Node;
import nub.ik.visual.Joint;
import nub.processing.Scene;
import processing.core.PShape;
import java.util.HashMap;
import java.util.Map;

import static processing.core.PConstants.ELLIPSE;
import static processing.core.PConstants.SPHERE;

/**
    Wrapper class that facilitates the creation of an Skeleton, its manipulation and its animation.
*/
public class Skeleton {
    HashMap<String, Node> _joints;
    HashMap<String, Node> _targets;
    Node _reference;
    Scene _scene;

    public Skeleton(Scene scene){
        _joints = new HashMap<String, Node>();
        _targets = new HashMap<String, Node>();
        _reference = new Node(); //dummy node to contain all generated Joints
        _reference.enableTagging(false);
        _scene = scene;
    }

    public Joint addJoint(String name){
        Joint joint = new Joint(_scene.radius() * 0.05f);
        _joints.put(name, joint);
        joint.setReference(_reference);
        joint.setRoot(true);
        return joint;
    }

    public Joint addJoint(String name, String reference){
        Joint joint = new Joint();
        _joints.put(name, joint);
        joint.setReference(_joints.get(reference));
        return joint;
    }

    public void addJoint(String name, String reference, Node node){
        _joints.put(name, node);
        node.setReference(_joints.get(reference));
    }

    public void addJoint(String name, Node node){
        _joints.put(name, node);
        node.setReference(_reference);
    }

    public Node addTarget(String name){
        Node endEffector = _joints.get(name);
        //Create a Basic target
        PShape redBall;
        if(_scene.is3D())
            redBall = _scene.context().createShape(SPHERE, _scene.radius() * 0.07f);
        else
            redBall = _scene.context().createShape(ELLIPSE, 0,0, 2 * _scene.radius() * 0.07f, 2 * _scene.radius() * 0.07f);
        redBall.setFill(_scene.context().color(0,255,0));
        redBall.setStroke(false);
        Node target = new Node(redBall);
        _targets.put(name, target);
        target.setReference(_reference);
        target.setPosition(endEffector.position().get());
        target.setOrientation(endEffector.orientation().get());
        _scene.addIKTarget(endEffector, target);
        return target;
    }

    public void addTarget(String endEffector, Node target){
        _scene.addIKTarget(_joints.get(endEffector), target);
    }

    public Node getJoint(String name){
        return _joints.get(name);
    }

    public Node getTarget(String name){
        return _targets.get(name);
    }

    public void enableIK(){
        for(Node child : _reference.children()){
            Graph.registerTreeSolver(child);
        }
    }

    //Send the targets to the eff position / orientation
    public void restoreTargetsState(){
        for(Map.Entry<String, Node> entry : _targets.entrySet()){
            Node eff = _joints.get(entry.getKey());
            entry.getValue().setPosition(eff);
            entry.getValue().setOrientation(eff);
        }
    }

    public void cull(boolean cull){
        _reference.cull(cull);
    }
}
