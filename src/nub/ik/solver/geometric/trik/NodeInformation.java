package nub.ik.solver.geometric.trik;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.List;

public class NodeInformation {
    //THIS CLASS IS USED IN ORDER TO AVOID REDUNDANT WORK
    //TODO: Move this class and refine
    NodeInformation _reference;
    Node _node;
    Quaternion _orientationCache;
    Vector _positionCache;

    protected NodeInformation(NodeInformation ref, Node node){
        this._reference = ref;
        this._node = node;
    }

    protected void setPositionCache(Vector position){
        _positionCache = position.get();
    }

    protected void setOrientationCache(Quaternion orientation){
        _orientationCache = orientation.get();
    }

    protected void setCache(Vector position, Quaternion orientation){
        setPositionCache(position);
        setOrientationCache(orientation);
    }

    protected Node node(){
        return _node;
    }

    protected NodeInformation reference(){
        return _reference;
    }

    protected Quaternion orientationCache(){
        return _orientationCache;
    }

    protected Vector positionCache(){
        return _positionCache;
    }

    protected void setOrientationWithCache(Quaternion orientation){
        if(_node.constraint() == null) {
            Quaternion delta = Quaternion.compose(_orientationCache.inverse(), orientation);
            _orientationCache = orientation;
            _node.rotate(delta);
        } else{
            Quaternion delta = _node.constraint().constrainRotation(Quaternion.compose(_orientationCache.inverse(), orientation), _node);
            _orientationCache.compose(delta);
            Constraint constraint = _node.constraint();
            _node.setConstraint(null);
            _node.rotate(delta);
            _node.setConstraint(constraint);
        }
    }

    protected void setPositionWithCache(Vector position){
        Vector translation = Vector.subtract(position, _positionCache);
        //diff w.r.t ref
        if(_reference != null){
            translation = _reference._orientationCache.inverseRotate(translation);
        }
        if(_node.constraint() == null) {
            _positionCache = position;
            _node.translate(translation);
        } else{
            translation = _node.constraint().constrainTranslation(translation, _node);
            _positionCache.add(translation);
            Constraint constraint = _node.constraint();
            _node.setConstraint(null);
            _node.translate(translation);
            _node.setConstraint(constraint);
        }
    }

    protected Vector locationWithCache(Vector worldVector){
        Vector translation = Vector.subtract(worldVector, _positionCache);
        return _orientationCache.inverseRotate(translation);
    }

    protected Vector locationWithCache(NodeInformation node){
        return locationWithCache(node.positionCache());
    }

    /*
     * NOTE : This update the cache taking into account only the current action, if parents were modified, the cache
     * must be updated explicitly.
     * */

    //Translate the node by delta and update the orientation/position of the remaining nodes
    protected void translateAndUpdateCache(Vector delta, boolean useConstraint, NodeInformation... nodeInfoList){
        Constraint constraint = _node.constraint();
        if(useConstraint && constraint != null) delta = constraint.constrainTranslation(delta, _node);
        Quaternion ref = Quaternion.compose(_orientationCache, _node.rotation().inverse());
        ref.normalize();
        Vector t = ref.rotate(delta); //w.r.t world
        _node.setConstraint(null);
        _node.translate(delta);
        _node.setConstraint(constraint);
        _positionCache.add(t);
        if(nodeInfoList.length > 0) {
            for (NodeInformation other : nodeInfoList) {
                other._positionCache.add(t);
            }
        }
    }


    //Rotate the node by delta and update the orientation/position of the remaining nodes
    protected void rotateAndUpdateCache(Quaternion delta, boolean useConstraint, NodeInformation... nodeInfoList){
        Constraint constraint = _node.constraint();
        if(useConstraint && constraint != null) delta = constraint.constrainRotation(delta, _node);

        _node.setConstraint(null);
        Quaternion orientation = Quaternion.compose(_orientationCache, delta);
        orientation.normalize(); // Prevents numerical drift
        if(nodeInfoList.length > 0) {
            Quaternion q = Quaternion.compose(orientation, _orientationCache.inverse());
            q.normalize();
            for (NodeInformation other : nodeInfoList) {
                Quaternion o = Quaternion.compose(q, other.orientationCache());
                o.normalize();
                other.setOrientationCache(o);
                Vector p = Vector.subtract(other.positionCache(), _positionCache);
                other.setPositionCache(Vector.add(_positionCache, q.rotate(p)));
            }
        }
        _node.rotate(delta);
        _node.setConstraint(constraint);
        _orientationCache = orientation;
    }

    //Updates cache assuming that reference contains an updated cache
    protected void updateCacheUsingReference(){
        _orientationCache = Quaternion.compose(reference().orientationCache(), node().rotation());
        _orientationCache.normalize();
        _positionCache = Vector.add(reference().positionCache(), reference().orientationCache().rotate(node().translation()));
    }

    protected static List<NodeInformation> _createInformationList(List<? extends Node> nodeList, boolean updateCache){
        List<NodeInformation> infoList = new ArrayList<NodeInformation>();
        NodeInformation ref = null;
        Quaternion orientation = nodeList.get(0).reference() != null && updateCache ? nodeList.get(0).reference().orientation() : new Quaternion();
        Vector position = nodeList.get(0).reference() != null && updateCache ? nodeList.get(0).reference().position() : new Vector();
        for(Node node : nodeList){
            NodeInformation nodeInfo = new NodeInformation(ref, node);
            infoList.add(nodeInfo);
            ref = nodeInfo;
            //update cache
            if(updateCache) {
                position.add(orientation.rotate(node.translation()));
                orientation.compose(node.rotation());
                orientation.normalize();
                nodeInfo.setCache(position.get(), orientation.get());
            }
        }
        return infoList;
    }

    protected static void _updateCache(List<NodeInformation> nodeInfoList){
        Quaternion orientation = nodeInfoList.get(0).node().reference() != null ? nodeInfoList.get(0).node().reference().orientation() : new Quaternion();
        orientation.normalize();
        Vector position = nodeInfoList.get(0).node().reference() != null ? nodeInfoList.get(0).node().reference().position() : new Vector();
        for(NodeInformation nodeInfo : nodeInfoList){
            Node node = nodeInfo.node();
            position.add(orientation.rotate(node.translation()));
            orientation.compose(node.rotation());
            orientation.normalize();
            nodeInfo.setCache(position.get(), orientation.get());
        }
    }

    protected static void _copyCache(List<NodeInformation> origin, List<NodeInformation> dest){
        for(int i = 0; i < origin.size(); i++){
            dest.get(i).setPositionCache(origin.get(i).positionCache());
            dest.get(i).setOrientationCache(origin.get(i).orientationCache());
        }
    }
}
