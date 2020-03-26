package nub.ik.solver;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class KinematicStructure {
    public static class KNode{
        /**
         * Convenient class to keep cache of global transformations
         */
        //TODO : This structure has troubles when reference node or ancestors are updated (must update path explicitly)
        protected long _lastProcessedUpdate, _lastUpdate;
        protected KinematicStructure _structure;
        protected Node _node;
        protected Vector _position;
        protected Quaternion _orientation;
        protected KNode _reference;
        protected boolean _enableConstraint = true;

        //TODO : Don't use a Node as an Attribute (decouple)?
        public KNode(KinematicStructure structure, Node node){
            _node = node;
            _position = node.position().get();
            _orientation = node.orientation().get();
            _structure = structure;
            _lastProcessedUpdate = _structure._lastUpdate;
        }

        public Node node(){
            return _node;
        }

        public KNode reference(){
            return _reference;
        };

        protected void _setLastUpdate(){
            _structure._lastUpdate++;
            _lastUpdate = _structure._lastUpdate;
        }

        public long lastUpdate(){
            return _lastUpdate;
        }

        public Vector translation(){
            return _node.translation();
        }
        public Quaternion rotation() {
            return _node.rotation();
        }

        public void rotate(Quaternion quaternion) {
            Constraint constraint = _node.constraint();
            if(constraint != null && _enableConstraint == false){
                _node.setConstraint(null);
                _node.rotate(quaternion);
                _node.setConstraint(constraint);
            }else{
                _node.rotate(quaternion);
            }
            _setLastUpdate();
        }

        public void rotate(Vector axis, float angle) {
            rotate(new Quaternion(axis, angle));
        }

        public void rotate(float x, float y, float z, float angle) {
            rotate(new Vector(x, y, z), angle);
        }

        public void setOrientation(Quaternion quaternion) {
            rotate(_reference != null ? Quaternion.compose(_reference.orientation().inverse(), quaternion) : quaternion);
        }

        public void setTranslation(Vector translation) {
            Constraint constraint = _node.constraint();
            if(constraint != null && _enableConstraint == false){
                _node.setConstraint(null);
                _node.setTranslation(translation);
                _node.setConstraint(constraint);
            }else{
                _node.setTranslation(translation);
            }
            _setLastUpdate();
        }

        public void translate(Vector vector) {
            Constraint constraint = _node.constraint();
            if(constraint != null && _enableConstraint == false){
                _node.setConstraint(null);
                _node.translate(vector);
                _node.setConstraint(constraint);
            }else{
                _node.translate(vector);
            }
            _setLastUpdate();
        }

        public void setPosition(Vector position) {
            if(_reference == null){
                setTranslation(position);
            } else{
                Vector t = _reference._orientation.inverseRotate(Vector.subtract(position, _reference._position));
                setTranslation(t);
            }
        }

        public void enableConstraint(boolean enable){
            _enableConstraint = enable;
        }


        public Quaternion orientation() {
            if(_structure._lastUpdate != _lastProcessedUpdate){
                _updateFollowingPath(null, this);
            }
            return _orientation;
        }

        public Vector position() {
            if(_structure._lastUpdate != _lastProcessedUpdate){
                _updateFollowingPath(null, this);
            }
            return _position;
        }

        //Use this method only when you're sure that the last changes applied to the structure doesnt affect this Node
        public Vector cachePosition() {
            return _position;
        }

        //Use this method only when you're sure that the last changes applied to the structure doesnt affect this Node
        public Quaternion cacheOrientation() {
            return _orientation;
        }

        protected void _updateFollowingPath(KNode head, KNode tail){
            //TODO: here we assume that no Node above head is modified
            KNode node = tail;
            KNode last = tail;
            long lastUpdate = tail.lastUpdate();
            //get last ancestor modified
            while(node != head){
                if(node.lastUpdate() > tail._lastProcessedUpdate){
                    lastUpdate = node.lastUpdate();
                }
                if(node._lastProcessedUpdate > tail._lastProcessedUpdate){
                    last = node;
                }
                node = node._reference;
            }
            //update from last ancestor modified to tail
            if(last._lastProcessedUpdate >= lastUpdate && last != head) _recursiveUpdate(last, tail);
            else _recursiveUpdate(head, tail);

        }

        protected void _recursiveUpdate(KNode head, KNode tail){
            if(tail == null) return;
            if(tail._reference != head){
                _recursiveUpdate(head , tail._reference);
            }
            //Update orientation
            Quaternion reference = tail._reference == null ? tail._node.reference() != null ? tail._node.reference().orientation() : new Quaternion() : tail._reference.orientation();
            tail._orientation = Quaternion.compose(reference, tail.rotation());
            //Update position
            Vector o = tail._reference == null ? tail._node.reference() != null ? tail._node.reference().position() : new Vector() : tail._reference.position();
            tail._position = Vector.add(o, reference.rotate(tail.translation()));
            tail._lastProcessedUpdate = _structure._lastUpdate;
        }

        //Get the location of a Vector v (in world space) in terms of this local space
        public Vector location(Vector v, boolean useCache){
            if(useCache) return _orientation.inverseRotate(Vector.subtract(v, _position));
            return orientation().inverseRotate(Vector.subtract(v, position()));
        }

        public Vector location(Vector v){
            return location(v, false);
        }

        //Get the location in world space of a Vector v (in local space) in
        public Vector worldLocation(Vector v, boolean useCache){
            if(useCache) return Vector.add(_orientation.rotate(v), _position);
            return Vector.add(orientation().rotate(v), position());
        }

        public Vector worldLocation(Vector v){
            return worldLocation(v, false);
        }

        public void updatePath(KNode init){
            _recursiveUpdate(init, this);
        }

    }

    protected long _lastUpdate;
    protected KNode _root;
    protected List<KNode> _endEffectors = new ArrayList<KNode>();

    //Useful operations between Node / KNode structures
    public KinematicStructure(){
        _lastUpdate = 0;
    }

    /*
    * The main difference of a Node and a KNode is that the later is a container of a node
    * with additional attributes and methods to keep an orientation/position cache
    * */

    //Obtains a Node chain given a KNode chain
    public List<Node> nodeChain(List<KNode> kchain){
        List<Node> chain = new ArrayList<Node>();
        for(KNode knode : kchain){
            chain.add(knode._node);
        }
        return chain;
    }

    //Generates a KNode chain given a Node chain
    public static List<KNode> generateKChain(List<? extends Node> chain){
        KinematicStructure structure = new KinematicStructure();
        List<KNode> kchain = new ArrayList<KNode>();
        HashMap<Node, KNode> map = new HashMap<Node, KNode>();

        Node reference = chain.get(0).reference();
        if (reference != null) {
            KNode kNode = new KNode(structure, Node.detach(reference.position().get(), reference.orientation().get(), reference.scaling()));
            map.put(reference, kNode);
        }

        for(Node node : chain){
            KNode knode = new KNode(structure, node);
            KNode ref = map.get(node.reference());
            if(ref != null){
                knode._reference = ref;
                //ref._children.add(knode);
            }
            map.put(node, knode);
            kchain.add(knode);
        }
        structure._endEffectors.add(map.get(chain.size() - 1));
        return kchain;
    }

    //Generates a KNode branch given a Node root (assume that node is attached to a Graph)
    public static KNode generateKBranch(Node root){
        KinematicStructure structure = new KinematicStructure();
        KNode kroot = new KNode(structure, root);
        _addKBranch(kroot, root);
        structure._root = kroot;
        return kroot;
    }

    protected static void _addKBranch(KNode reference, Node node){
        if(node.children() == null || node.children().isEmpty()){
            reference._structure._endEffectors.add(reference);
        }

        //here we assume that node is attached to a graph
        for(Node child_node : node.children()){
            KNode child = new KNode(reference._structure, child_node);
            child._reference = reference;
            //reference._children.add(child);
            _addKBranch(child, child_node);
        }
    }

    //Clone a chain Node
    public static List<Node> _copy(List<? extends Node> chain, Node reference) {
        List<Node> copy = new ArrayList<Node>();
        for (Node joint : chain) {
            Node newJoint = Node.detach(new Vector(), new Quaternion(), 1f);
            newJoint.setReference(reference);
            newJoint.setPosition(joint.position().get());
            newJoint.setOrientation(joint.orientation().get());
            newJoint.setConstraint(joint.constraint());
            copy.add(newJoint);
            reference = newJoint;
        }
        return copy;
    }

    public static List<Node> _copy(List<? extends Node> chain) {
        Node reference = chain.get(0).reference();
        if (reference != null) {
            reference = Node.detach(reference.position().get(), reference.orientation().get(), 1);
        }
        return _copy(chain, reference);
    }

    //Given that init rotates by delta which is the State (Node/Position) of end
    public Node predict(KNode init, Quaternion delta, KNode end){
        Quaternion o_init = init.orientation();
        Quaternion o_end = end.orientation();
        if(init._enableConstraint && init._node.constraint() != null){
            delta = init._node.constraint().constrainRotation(delta, init._node);
        }
        Quaternion q = Quaternion.compose(Quaternion.compose(o_init, delta), o_init.inverse());
        Quaternion orientation = Quaternion.compose(q, o_end);
        Vector p = Vector.subtract(end.position(), init.position());
        Vector position = Vector.add(init.position(), q.rotate(p));
        return Node.detach(position, orientation, 1);
    }
}
