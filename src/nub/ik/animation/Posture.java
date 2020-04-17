package nub.ik.animation;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.HashMap;
import java.util.Map;

public class Posture {
    protected class NodeInformation{
        Vector _translation;
        Quaternion _rotation;

        protected NodeInformation(Node node){
            _translation = node.translation().get();
            _rotation = node.rotation().get();
        }
    }

    protected HashMap<String, NodeInformation> _nodeInformation;

    public Posture(){
        _nodeInformation = new HashMap<String, NodeInformation>();
    }

    public Posture(Skeleton skeleton){
        this();
        saveCurrentValues(skeleton);
    }

    public void saveCurrentValues(Skeleton skeleton){
        for(Map.Entry<String, Node> entry : skeleton._joints.entrySet()){
            _nodeInformation.put(entry.getKey(), new NodeInformation(entry.getValue()));
        }
    }

    public void loadValues(Skeleton skeleton){
        for(Map.Entry<String, NodeInformation> entry : _nodeInformation.entrySet()){
            Node node = skeleton.joint(entry.getKey());
            Constraint constrain = node.constraint();
            node.setConstraint(null);
            node.setTranslation(entry.getValue()._translation.get());
            node.setRotation(entry.getValue()._rotation.get());
            node.setConstraint(constrain);
        }
    }

    public NodeInformation jointState(String name){
        return _nodeInformation.get(name);
    }

}
