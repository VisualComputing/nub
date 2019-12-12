package nub.ik.solver.trik;

import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

public class NodeState{
    protected NodeInformation _nodeInformation;
    protected Quaternion _rotation, _orientation;
    protected Vector _translation, _position;

    public NodeState(NodeInformation nodeInformation){
        _nodeInformation = nodeInformation;
        _rotation = nodeInformation.node().rotation().get();
        _orientation = nodeInformation.orientationCache().get();
        _translation = nodeInformation.node().translation().get();
        _position = nodeInformation.positionCache().get();
    }

    public Quaternion rotation(){
        return _rotation;
    }

    public Vector position(){
        return _position;
    }

    public Quaternion orientation(){
        return _orientation;
    }

}
