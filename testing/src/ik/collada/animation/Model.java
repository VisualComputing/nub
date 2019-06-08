package ik.collada.animation;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PImage;
import processing.core.PShape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapted by sebchaparr on 22/07/18.
 * See https://github.com/TheThinMatrix/OpenGL-Animation
 *
 * Useful class that contains information about geometry (a Mesh or a List of Meshes) and Skeleton structure.
 */

public class Model {
    //Mesh
    protected HashMap<String, PShape> _mesh;
    protected List<PImage> _textures;
    protected Scene _scene;

    // skeleton
    protected Node _root;
    protected Map<String, Node> _skeleton;
    protected Map<String, Node> _meshMap;
    protected Map<Integer, Integer> idxs; //Frame idx - Data idx
    protected int jointCount;
    protected float _scaling = 1;


    public void setScaling(float scaling){
        this._scaling = scaling;
    }

    public float scaling(){
        return _scaling;
    }

    public void set_root(Node root){
        this._root = root;
    }

    public void setJointCount(int jointCount){
        this.jointCount = jointCount;
    }

    public Map<String, Node> skeleton(){
        return _skeleton;
    }

    public Map<Integer, Integer> getIdxs(){
        return idxs;
    }

    public Map<String, Node> meshMap(){
        return _meshMap;
    }


    public Model(Scene scene){
        _skeleton = new HashMap<String, Node>();
        idxs = new HashMap<Integer, Integer>();
        _meshMap = new HashMap<String, Node>();
        _scene = scene;
        _mesh = new HashMap<>();
        _textures = new ArrayList<>();
    }

    /**
     * @return The VAO containing all the mesh data for this entity.
     */
    public HashMap<String, PShape> mesh() {
        return _mesh;
    }

    public void addModel(String id, PShape model){
        _mesh.put(id, model);
    }

    public Scene getScene(){ return _scene;}

    /**
     * @return The diffuse texture for this entity.
     */
    public List<PImage> getTexture() {
        return _textures;
    }

    /**
     * @return The root joint of the joint hierarchy. This joint has no parent,
     *         and every other joint in the skeleton is a descendant of this
     *         joint.
     */
    public Node root() {
        return _root;
    }


    /**
     * Gets an array of the all important model-space transforms of all the
     * _skeleton (with the current animation pose applied) in the entity. The
     * _skeleton are ordered in the array based on their joint index. The position
     * of each joint's transform in the array is equal to the joint's index.
     *
     * @return The array of model-space transforms of the _skeleton in the current
     *         animation pose.
     */
    public Node[] getJointTransforms() {
        Node[] jointMatrices = new Node[jointCount];
        addJointsToArray(_root, jointMatrices);
        return jointMatrices;
    }

    public void addJointsToArray(Node frame, Node[] jointMatrices){
        if(idxs.get(frame.id()) != -1)
            jointMatrices[idxs.get(frame.id())]  = frame;
        for(Node child : frame.children()){
            addJointsToArray(child, jointMatrices);
        }
    }

    public void printNames(){
        for(String name : _skeleton.keySet()){
            System.out.println(name);
        }
    }
}