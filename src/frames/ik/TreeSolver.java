/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package frames.ik;

import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.core.Node;
import java.util.ArrayList;

public class TreeSolver extends FABRIKSolver {

    /*Convenient Class to store ChainSolvers in a Tree Structure*/
    private static class TreeNode {
        private TreeNode parent;
        private ArrayList<TreeNode> children;
        private ChainSolver solver;
        private boolean modified;
        private float weight = 1.f;

        public TreeNode() {
            children = new ArrayList<TreeNode>();
        }

        public TreeNode(ChainSolver solver) {
            this.solver = solver;
            children = new ArrayList<TreeNode>();
        }

        private TreeNode(TreeNode parent, ChainSolver solver) {
            this.parent = parent;
            this.solver = solver;
            if (parent != null) {
                parent.addChild(this);
            }
            children = new ArrayList<TreeNode>();
        }

        private boolean addChild(TreeNode n) {
            return children.add(n);
        }

        private ArrayList<TreeNode> getChildren() {
            return children;
        }

        private float getWeight() {
            return weight;
        }

        private ChainSolver getSolver() {
            return solver;
        }

        public boolean isModified() {
            return modified;
        }

        public void setModified(boolean modified) {
            this.modified = modified;
        }
    }

    //TODO Relate weights with End Effectors not with chains
    /*Tree structure that contains a list of Solvers that must be accessed in a BFS way*/
    private TreeNode root;

    public Node getHead() {
        return (Node) root.getSolver().getHead();
    }

    public void setup(TreeNode parent, Node frame, ArrayList<Node> list) {
        if (frame == null) return;
        if (frame.children().isEmpty()) {
            list.add(frame);
            ChainSolver solver = new ChainSolver(list, null);
            new TreeNode(parent, solver);
            return;
        }
        if (frame.children().size() > 1) {
            list.add(frame);
            ChainSolver solver = new ChainSolver(list, null);
            TreeNode treeNode = new TreeNode(parent, solver);
            for (Node child : frame.children()) {
                ArrayList<Node> newList = new ArrayList<Node>();
                newList.add(frame);
                setup(treeNode, child, newList);
            }
        } else {
            list.add(frame);
            setup(parent, frame.children().get(0), list);
        }
    }

    private boolean addTarget(TreeNode treeNode, Node endEffector, Frame target) {
        if (treeNode == null) return false;
        if (treeNode.getSolver().getEndEffector() == endEffector) {
            treeNode.getSolver().setTarget(target);
            return true;
        }
        for (TreeNode child : treeNode.getChildren()) {
            addTarget(child, endEffector, target);
        }
        return false;
    }

    public boolean addTarget(Node endEffector, Frame target) {
        return addTarget(root, endEffector, target);
    }

    public TreeSolver(Node genericFrame) {
        super();
        TreeNode dummy = new TreeNode(); //Dummy TreeNode to Keep Reference
        setup(dummy, genericFrame, new ArrayList<Node>());
        //dummy must have only a child,
        this.root = dummy.getChildren().get(0);
    }

    public int executeForward(TreeNode treeNode) {
        float totalWeight = 0;
        boolean modified = false;
        int chains = 0;
        for (TreeNode child : treeNode.getChildren()) {
            chains += executeForward(child);
            if (child.getSolver().getTarget() != null) totalWeight += child.getWeight();
            modified = modified || child.isModified();
        }
        //Stage 1: Forward Reaching
        ChainSolver solver = treeNode.getSolver();
        //TODO: add embedded target and enable to give it some weight - Weight/Target as an attribute of Chain or as TreeNode attribute?
        //Update Target according to children Head new Position
        Vector newTarget = new Vector();
        for (TreeNode child : treeNode.getChildren()) {
            //If Child Chain Joints new positions doesn't matter
            if (child.getSolver().getTarget() == null) continue;
            newTarget.add(Vector.multiply(child.getSolver().getPositions().get(0), 1.f / totalWeight));
        }
        if (newTarget.magnitude() > 0) {
            solver.setTarget(new Frame(newTarget, solver.getEndEffector().orientation().get()));
        }

        //Execute Until the distance between the end effector and the target is below a threshold
        if (solver.getTarget() == null) {
            treeNode.setModified(false);
            return 0;
        }
        if (Vector.distance(solver.getEndEffector().position(), solver.getTarget().position()) <= ERROR) {
            treeNode.setModified(false);
            return 0;
        }
        solver.getPositions().set(solver.getChain().size() - 1, solver.target.position().get());
        solver.executeForwardReaching();
        treeNode.setModified(true);
        return chains + 1;
    }

    public float executeBackward(TreeNode treeNode) {
        float change = MINCHANGE;
        if (treeNode.isModified()) {
            ChainSolver solver = treeNode.getSolver();
            solver.getPositions().set(0, solver.getHead().position());
            change = solver.executeBackwardReaching();
                    /*When executing Backward Step, if the Frame is a SubBase (Has more than 1 Child) and
                     * it is not a "dummy Frame" (Convenient Frame that constraints position but no orientation of
                     * its children) then an additional step must be done: A Weighted Average of Positions to establish
                     * new Frame orientation
                     * */
            //TODO : Perhaps add an option to not execute this step
            // (Last chain modified determines Sub Base orientation)
            if (treeNode.getChildren().size() > 1) {
                Vector centroid = new Vector();
                Vector newCentroid = new Vector();
                float totalWeight = 0;
                for (TreeNode child : treeNode.getChildren()) {
                    //If target is null, then Joint must not be included
                    if (child.getSolver().getTarget() == null) continue;
                    if (child.getSolver().getChain().size() < 2) continue;
                    if (child.getSolver().getChain().get(1).translation().magnitude() == 0) continue;
                    Vector diff = solver.getEndEffector().coordinatesOf(child.getSolver().getChain().get(1).position());
                    centroid.add(Vector.multiply(diff, child.getWeight()));
                    if (child.isModified()) {
                        diff = solver.getEndEffector().coordinatesOf(child.getSolver().getPositions().get(1));
                        newCentroid.add(Vector.multiply(diff, child.getWeight()));
                    } else {
                        newCentroid.add(Vector.multiply(diff, child.getWeight()));
                    }
                    totalWeight += child.getWeight();
                }
                //Set only when Centroid and New Centroid varies
                if (Vector.distance(centroid, newCentroid) > 0.001) {
                    centroid.multiply(1.f / totalWeight);
                    newCentroid.multiply(1.f / totalWeight);
                    Quaternion deltaOrientation = new Quaternion(centroid, newCentroid);
                    treeNode.getSolver().getEndEffector().rotate(deltaOrientation);
                    for (TreeNode child : treeNode.getChildren()) {
                        if (child.getSolver().getChain().size() < 2) continue;
                        if (child.getSolver().getChain().get(1).translation().magnitude() == 0) continue;
                        if (child.isModified()) {
                            child.getSolver().getPositions().set(1, child.getSolver().getChain().get(1).position());
                        }
                    }
                }
            }
        }
        for (TreeNode child : treeNode.getChildren()) {
            change += executeBackward(child);
        }
        return change;
    }

    @Override
    public boolean iterate() {
        int modifiedChains = executeForward(root);
        float change = executeBackward(root);
        //Check total position change
        if (change / (modifiedChains * 1.) <= MINCHANGE) return true;
        return false;
    }

    @Override
    public void update() {
        //As BackwardStep modify chains, no update is required
    }

    //Update Subtree that have associated Frame as root
    public boolean updateTree(TreeNode treeNode, Node frame) {
        if (treeNode.getSolver().getEndEffector() == frame) {
            setup(treeNode, frame, new ArrayList<Node>());
            return true;
        }
        for (TreeNode child : treeNode.getChildren()) {
            updateTree(child, frame);
        }
        return false;
    }

    private boolean stateChanged(TreeNode treeNode) {
        if (treeNode == null) return false;
        if (treeNode.getSolver().stateChanged()) return true;
        for (TreeNode child : treeNode.getChildren()) {
            if (stateChanged(child)) return true;
        }
        return false;
    }

    @Override
    public boolean stateChanged() {
        return stateChanged(root);
    }

    private void reset(TreeNode treeNode) {
        if (treeNode == null) return;
        //Update Previous Target
        if (treeNode.getSolver().stateChanged()) treeNode.getSolver().reset();
        for (TreeNode child : treeNode.getChildren()) {
            reset(child);
        }
    }

    @Override
    public void reset() {
        iterations = 0;
        reset(root);
    }
}