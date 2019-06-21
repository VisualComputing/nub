package ik.common;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sebchaparr on 11/03/18.
 */
public class LinearBlendSkinningCPU {
    //Skeleton & Geometry information
    protected List<PShape> _shapes;
    protected List<Node> _skeleton;
    //Shader information
    protected Quaternion[] _initialOrientations;
    protected Vector[] _initialPositions;
    protected Quaternion[] _currentOrientations;
    protected Vector[] _currentPositions;
    protected Map<Node, Integer> _ids;
    protected List<Vertex> _vertices;
    protected PGraphics _pg;

    protected class Vertex{
        protected int[] _joints;
        protected float[] _weights;
        protected int _vertexId;
        protected PVector _initial;
        protected PShape _shape;

        protected Vertex(PShape shape, int vertexId, int[] joints, float[] weights){
            _shape = shape;
            _vertexId = vertexId;
            _joints = joints;
            _weights = weights;
            _initial = shape.getVertex(_vertexId).copy();
        }

        protected void applyTransformation(){
            Vector curPos = new Vector(_initial.x, _initial.y, _initial.z);

            Vector normal = new Vector(_shape.getNormalX(_vertexId),
                    _shape.getNormalY(_vertexId), _shape.getNormalZ(_vertexId));

            Vector v = new Vector();
            Vector n = new Vector();
            for(int i = 0; i < _joints.length; i++){
                int idx = _joints[i];
                if(_weights[i] == 0) continue;
                Quaternion quat = _currentOrientations[idx];
                Vector pos = _initialPositions[idx];
                Vector off = _currentPositions[idx];
                Vector u = Vector.subtract(curPos, pos);
                u = quat.rotate(u);
                u.add(pos);
                u.add(off);
                u.multiply(_weights[i]);
                v.add(u);
                n.add(Vector.multiply(quat.rotate(normal), _weights[i]));
            }

            _shape.setVertex(_vertexId,v.x(),v.y(),v.z());
        }
    }

    public LinearBlendSkinningCPU(List<Node> skeleton, PGraphics pg, String shape, String texture, float factor) {
        this(skeleton, pg, shape, texture, factor, false);
    }


    public LinearBlendSkinningCPU(List<Node> skeleton, PGraphics pg, String shape, String texture, float factor, boolean quad) {
        this._shapes = new ArrayList<>();
        _ids = new HashMap<>();
        _skeleton = skeleton;
        int joints = skeleton.size();
        for(int i = 0; i < joints; i++){
            _ids.put(_skeleton.get(i), i);
            if(_skeleton.get(i) instanceof Joint) {
                int c = Color.HSBtoRGB((i + 1.0f) / skeleton.size(), 1f, 1f);
                ((Joint) _skeleton.get(i)).setColor(c);
            }
        }
        _initialOrientations = new Quaternion[joints];
        _initialPositions = new Vector[joints];
        _currentOrientations = new Quaternion[joints];
        _currentPositions = new Vector[joints];


        _vertices = new ArrayList<Vertex>();
        _shapes.add(createShape(pg, pg.loadShape(shape), texture, factor, quad));
        _pg = pg;
        initParams();
    }

    public List<PShape> shapes(){
        return _shapes;
    }

    public PShape shape(){
        return _shapes.get(0);
    }

    public List<Node> skeleton(){
        return _skeleton;
    }

    public Map<Node, Integer> ids(){
        return _ids;
    }

    public void initParams() {
        for(int i = 0; i < _skeleton.size(); i++){
            Vector v = _skeleton.get(i).position();
            Quaternion q = _skeleton.get(i).orientation();
            _initialOrientations[i] = q;
            _initialPositions[i] = v.get();
        }
    }

    public void updateParams() {
        //TODO: IT COULD BE DONE WITH LESS OPERATIONS
        for(int i = 0; i < _skeleton.size(); i++){
            Vector v = Vector.subtract(_skeleton.get(i).position(), _initialPositions[i]);
            Quaternion q = Quaternion.compose(_skeleton.get(i).orientation(), _initialOrientations[i].inverse());
            _currentPositions[i] = v;
            _currentOrientations[i] = q;
        }
        for(Vertex vertex : _vertices){
            vertex.applyTransformation();
        }

    }

    public float[] addWeights(List<Node> branch, PVector vector){
        Vector position = new Vector(vector.x, vector.y, vector.z);
        float total_dist = 0.f;
        int[] joints = new int[]{-1, -1, -1};
        float[] w = new float[]{0, 0, 0};
        float[] d = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        //Find the nearest 3 joints
        //TODO : Perhaps enable more joints - use QuickSort
        for (Node joint : branch) {
            if (joint == branch.get(0)) continue;
            if (joint.translation().magnitude() <= Float.MIN_VALUE) continue;
            float dist = (float) Math.pow(getDistance(position, joint), 10);
            if(dist <= d[0] || dist <= d[1] || dist <= d[2]){
                int start = dist <= d[0] ? 0 : dist <= d[1] ? 1 : 2;
                for(int l = joints.length-1; l > start; l--){
                    joints[l] = joints[l-1];
                    d[l] = d[l-1];
                }
                joints[start] = _ids.get(joint.reference());
                d[start] = dist;
            }
        }

        for(int k = 0; k < joints.length; k++){
            total_dist += 1.f/d[k];
        }
        for(int k = 0; k < joints.length; k++){
            w[k] += 1.f/d[k] / total_dist;
        }
        return new float[]{joints[0], joints[1], joints[2], w[0], w[1], w[2]};
    }

    public void renderMesh(PGraphics pg){
        updateParams();
        for(PShape shape : _shapes){
            pg.shape(shape);
        }
    }

    public void renderMesh(){
        renderMesh(_pg);
    }

    public void renderMesh(Node reference){
        PGraphics pg = _pg;
        if(reference.graph() instanceof Scene){
            pg = ((Scene) reference.graph()).context();
        }
        reference.graph().applyWorldTransformation(reference);
        renderMesh(pg);
    }


    /*
     * Get the distance from vertex to line formed by frame and the reference frame of frame
     * Distance will be measure according to root coordinates.
     * In case of reference frame of frame is root, it will return distance from vertex to frame
     * */
    public static float getDistance(Vector vertex, Node node) {
        if (node == null) return Float.MAX_VALUE;
        Vector position = node.position();
        if(node.reference() == null) return Vector.distance(position, vertex);
        Vector parentPosition = node.reference().position();
        //is the distance between line formed by b and its parent and v
        Vector line = Vector.subtract(position, parentPosition);
        Vector projection = Vector.subtract(vertex, parentPosition);
        float dot = Vector.dot(projection, line);
        float magnitude = line.magnitude();
        float u = dot * (float) 1. / (magnitude * magnitude);
        Vector distance = new Vector();
        if (u >= 0 && u <= 1) {
            distance = new Vector(parentPosition.x() + u * line.x(), parentPosition.y() + u * line.y(),
                    parentPosition.z() + u * line.z());
            distance = Vector.subtract(distance, vertex);
        }
        if (u < 0) {
            distance = Vector.subtract(position, vertex);
        }
        if (u > 1) {
            distance = Vector.subtract(position, vertex);
        }
        return distance.magnitude();
    }


    //Adapted from http://www.cutsquash.com/2015/04/better-obj-model-loading-in-processing/
    public PShape createShape(PGraphics pg, PShape r, String texture, float size, boolean quad) {
        float scaleFactor = size / Math.max(r.getWidth(), r.getHeight());
        PImage tex = pg.parent.loadImage(texture);
        PShape s = pg.createShape();
        s.beginShape(quad ? PConstants.QUADS : PConstants.TRIANGLES);
        s.noStroke();
        s.texture(tex);
        s.textureMode(PConstants.NORMAL);
        if(r.getChildCount() == 0) {
            for (int j = 0; j < r.getVertexCount(); j++) {
                PVector p = r.getVertex(j).mult(scaleFactor);
                PVector n = r.getNormal(j);
                float u = r.getTextureU(j);
                float v = r.getTextureV(j);
                s.normal(n.x, n.y, n.z);
                float[] params = addWeights(_skeleton, p);
                s.vertex(p.x, p.y, p.z, u, v);
                //create vertex
                _vertices.add(new Vertex(s, j, new int[]{(int)params[0] ,
                        (int)params[1], (int)params[2]}, new float[]{params[3], params[4], params[5]}));
            }
        } else {
            int vc = 0;
            for (int i = 0; i < r.getChildCount(); i++) {
                for (int j = 0; j < r.getChild(i).getVertexCount(); j++) {
                    PVector p = r.getChild(i).getVertex(j).mult(scaleFactor);
                    PVector n = r.getChild(i).getNormal(j);
                    float u = r.getChild(i).getTextureU(j);
                    float v = r.getChild(i).getTextureV(j);
                    s.normal(n.x, n.y, n.z);

                    float[] params = addWeights(_skeleton, p);
                    s.vertex(p.x, p.y, p.z, u, v);
                    //create vertex
                    for(float f : params)
                        System.out.print(" =>V : " +  f);
                    System.out.println();
                    _vertices.add(new Vertex(s, vc, new int[]{(int)params[0] ,
                            (int)params[1], (int)params[2]}, new float[]{params[3], params[4], params[5]}));
                    vc++;
                }
            }
        }
        s.endShape();
        return s;
    }
}