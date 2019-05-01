package ik.collada.animation;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import java.util.HashMap;

/**
 * Adapted by sebchaparr on 22/07/18.
 */
public class AnimatedModel {
    //TODO : Update


    // skin
    private PShape model;
    private PImage texture;
    private Scene scene;

    // skeleton
    private Node rootJoint;
    private HashMap<String, Node> joints;
    private HashMap<Integer, Integer> idxs; //Frame idx - Data idx
    private int jointCount;

    private Animator animator;

    public void setRootJoint(Node root){
        this.rootJoint = root;
    }

    public void setJointCount(int jointCount){
        this.jointCount = jointCount;
    }

    public HashMap<String, Node> getJoints(){
        return joints;
    }

    public HashMap<Integer, Integer> getIdxs(){
        return idxs;
    }

    public AnimatedModel(Scene scene){
        joints = new HashMap<String, Node>();
        idxs = new HashMap<Integer, Integer>();
        this.scene = scene;
    }

    public AnimatedModel(PShape model, PImage texture, HashMap<String, Node> joints, Node rootJoint) {
        this.model = model;
        this.texture = texture;
        this.rootJoint = rootJoint;
        this.joints = joints;
        this.jointCount = joints.size();
        this.animator = new Animator(this);
    }

    public void setModel(PShape model){
        this.model = model;
    }

    /**
     * @return The VAO containing all the mesh data for this entity.
     */
    public PShape getModel() {
        return model;
    }

    public Scene getScene(){ return scene;}

    /**
     * @return The diffuse texture for this entity.
     */
    public PImage getTexture() {
        return texture;
    }

    /**
     * @return The root joint of the joint hierarchy. This joint has no parent,
     *         and every other joint in the skeleton is a descendant of this
     *         joint.
     */
    public Node getRootJoint() {
        return rootJoint;
    }

    /**
     * Instructs this entity to carry out a given animation. To do this it
     * basically sets the chosen animation as the current animation in the
     * {@link Animator} object.
     *
     * @param animation
     *            - the animation to be carried out.
     */
    public void doAnimation(Animation animation) {
        animator.doAnimation(animation, scene.pApplet().millis());
    }

    /**
     * Updates the animator for this entity, basically updating the animated
     * pose of the entity. Must be called every frame.
     */
    public void update(float time) {
        animator.update(scene.pApplet().millis());
    }

    /**
     * Gets an array of the all important model-space transforms of all the
     * joints (with the current animation pose applied) in the entity. The
     * joints are ordered in the array based on their joint index. The position
     * of each joint's transform in the array is equal to the joint's index.
     *
     * @return The array of model-space transforms of the joints in the current
     *         animation pose.
     */
    public Node[] getJointTransforms() {
        Node[] jointMatrices = new Node[jointCount];
        addJointsToArray(rootJoint, jointMatrices);
        return jointMatrices;
    }

    public void addJointsToArray(Node frame, Node[] jointMatrices){
        jointMatrices[idxs.get(frame.id())]  = frame;
        for(Node child : frame.children()){
            addJointsToArray(child, jointMatrices);
        }
    }

    public void printNames(){
        for(String name : joints.keySet()){
            System.out.println(name);
        }
    }
}