package nub.ik.solver.geometric;

import javafx.util.Pair;
import nub.core.Node;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TRIKTree extends Solver {
    protected static class TreeNode {
        protected TreeNode _parent;
        protected List<TreeNode> _children;

        protected TRIK _solver;
        protected boolean _modified;
        protected float _weight = 1.f;

        public TreeNode() {
            _children = new ArrayList<TreeNode>();
        }

        public TreeNode(TRIK solver) {
            this._solver = solver;
            _solver.setTimesPerFrame(1);

            _children = new ArrayList<TreeNode>();
        }

        protected TreeNode(TreeNode parent, TRIK solver) {
            this._parent = parent;
            this._solver = solver;
            if (parent != null) {
                parent._addChild(this);
            }
            _children = new ArrayList<TreeNode>();
        }

        protected boolean _addChild(TreeNode n) {
            return _children.add(n);
        }

        protected List<TreeNode> _children() {
            return _children;
        }

        protected float _weight() {
            return _weight;
        }

        protected TRIK _solver() {
            return _solver;
        }
    }

    protected TreeNode _root;
    protected float _current = 10e10f, _best = 10e10f;
    protected HashMap<Node, Node> _endEffectorMap = new HashMap<>();

    public TRIKTree(Node root){
        super();
        TreeNode dummy = new TreeNode(); //Dummy TreeNode to Keep Reference
        _setup(dummy, root, new ArrayList<Node>());
        //dummy must have only a child,
        this._root = dummy._children().get(0);
        this._root._parent = null;
    }


    protected void _setup(TreeNode parent, Node node, List<Node> list){
        if(node == null) return;
        if(node.children().isEmpty()) { //Is a leaf node, hence we've found a chain of the structure
            list.add(node);
            TRIK solver = new TRIK(list);
            new TreeNode(parent, solver);
        } else if(node.children().size() > 1){
            list.add(node);
            TRIK solver = new TRIK(list);
            TreeNode treeNode = new TreeNode(parent, solver);
            for(Node child : node.children()){
                List<Node> newList = new ArrayList<>();
                //newList.add(node);
                _setup(treeNode, child, newList);
            }
        } else{
            list.add(node);
            _setup(parent, node.children().get(0), list);
        }
    }
    Vector[] _current_coords;
    Vector[] _desired_coords;
    protected boolean _solve(TreeNode treeNode){
        if(treeNode._children == null || treeNode._children.isEmpty()){
            TRIK solver = treeNode._solver;
            if(solver._target == null) return false;
            //solve ik for current chain
            solver._reset();
            solver.solve(); //Perform a given number of iterations
            return true;
        }

        int childrenModified = 0;
        TRIK solver = treeNode._solver;
        int c = 0;
        Vector[] current_coords = new Vector[treeNode._children.size()];
        Vector[] desired_coords = new Vector[treeNode._children.size()];
        for(TreeNode child : treeNode._children()){
            if( _solve(child)){
                childrenModified++;
                Vector o = solver._original.get(solver._original.size() - 1).location(child._solver._original.get(0));
                Vector eff = solver._original.get(solver._original.size() - 1).location(child._solver._original.get(child._solver._original.size() - 1));
                Vector t = solver._original.get(solver._original.size() - 1).location(child._solver._target);
                Vector diff = Vector.subtract(t, eff);
                current_coords[c] = o;
                //current_coords[c] = Vector.subtract(current_coords[c],o);
                desired_coords[c] = Vector.add(o,diff);
                //desired_coords[c] = Vector.subtract(local_t,o);
            }
            c++;
        }

        if(childrenModified <= 0) return false;

        //in case a children was modified and the node is not a leaf
        Pair<Quaternion, Vector> transformation = QCP.CalcRMSDRotationalMatrix(desired_coords, current_coords, null);
        Node target = new Node();
        target.setPosition(solver._original.get(solver._original.size() - 1).worldLocation(transformation.getValue()));
        target.setOrientation(Quaternion.compose(solver._original.get(solver._original.size() - 1).orientation(), transformation.getKey()));
        solver.setTarget(target);
        //solve ik for current chain
        if(solver._chain.size() < 2){//If the solver has only a node we require to update manually
            for(int i = 0; i < current_coords.length; i++){
                System.out.println("curr " + current_coords[i] + " des " + desired_coords[i] + " cur rot : " + transformation.getKey().rotate(current_coords[i]) + " other r " + transformation.getKey().inverse().rotate(current_coords[i]));
            }

            System.out.println("Quat : " + transformation.getKey().axis() + " a " + transformation.getKey().angle());

            solver._original.get(solver._original.size() - 1).rotate(transformation.getKey().inverse());
        } else {
            solver.solve(); //Perform a given number of iterations
        }
        return true;
    }

    @Override
    protected boolean _iterate() {
        _solve(_root);
        return false;
    }

    @Override
    protected void _update() {

    }

    protected boolean _changed(TreeNode treeNode) {
        if (treeNode == null) return false;
        if (treeNode._solver()._changed() && treeNode._children().isEmpty()) return true;
        for (TreeNode child : treeNode._children()) {
            if (_changed(child)) return true;
        }
        return false;
    }

    @Override
    protected boolean _changed() {
        return _changed(_root);
    }

    protected void _reset(TreeNode treeNode) {
        if (treeNode == null) return;
        //Update Previous Target
        if (treeNode._solver()._changed()) treeNode._solver()._reset();
        for (TreeNode child : treeNode._children()) {
            _reset(child);
        }
    }


    @Override
    protected void _reset() {
        _iterations = 0;
        _best = 0;
        _current = 10e10f;
        _reset(_root);
    }

    @Override
    public float error() {
        float e = 0;
        for(Map.Entry<Node, Node> entry : _endEffectorMap.entrySet()){
            e += Vector.distance(entry.getKey().position(), entry.getValue().position());
        }
        return e;
    }

    @Override
    public void setTarget(Node endEffector, Node target) {
        addTarget(endEffector, target);
    }


    protected boolean _addTarget(TreeNode treeNode, Node endEffector, Node target) {
        if (treeNode == null) return false;
        for(Node node : treeNode._solver().chain()){
            if (node == endEffector) {
                treeNode._solver().setTarget(target);
                _endEffectorMap.put(endEffector, target);
                return true;
            }
        }
        for (TreeNode child : treeNode._children()) {
            _addTarget(child, endEffector, target);
        }
        return false;
    }

    public boolean addTarget(Node endEffector, Node target) {
        return _addTarget(_root, endEffector, target);
    }



}
