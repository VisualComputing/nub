package ik.mocap;
import frames.core.Node;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.primitives.constraint.DistanceFieldConstraint;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class must be used to load a bvh file and
 * generate an animation.
 *
 * For more info look at http://www.dcs.shef.ac.uk/intranet/research/public/resmes/CS0111.pdf
 * Created by sebchaparr on 23/03/18.
 */
public class BVHParser {
    protected BufferedReader _buffer;
    //A Joint is a Node with some Properties
    //TODO: Consider _id() public?
    protected HashMap<Node, Properties> _joint;
    protected int _frames;
    protected int _period;
    protected Class< ? extends  Node> _nodeClass;
    protected Node _root;
    protected List<Node> _branch;
    protected HashMap<Node, ArrayList<Frame>> _poses;
    protected int _currentPose;
    protected boolean _loop;


    public BVHParser(Class<? extends Node> nodeClass, String path, Scene scene, Node reference){
        _nodeClass = nodeClass;
        _setup(path, scene, reference);
    }

    class Properties{
        protected String _name;
        protected int channels;
        protected List<String> _channelType;
        protected String _parametrization; //Possible options: XYZ, EULER
        protected List<Vector> _feasibleRegion;

        Properties(String name){
            _name = name;
            channels = 0;
            _channelType = new ArrayList<String>();
            _parametrization = "XYZ";
            _feasibleRegion = new ArrayList<Vector>();
        }

        boolean addChannelType(String type){
            return _channelType.add(type);
        }

        void setParametrization(String parametrization){
            this._parametrization = parametrization;
        }

    }

    public Node root(){
        return _root;
    }

    protected void _setup(String path, Scene scene, Node reference){
        _frames = 0;
        _period = 0;
        _currentPose = 0;
        _joint = new HashMap<>();
        _poses = new HashMap<>();
        _loop = true;
        _readHeader(path, scene, reference);
        _saveFrames();
    }



    /**
     * Reads a .bvh file from the given path and builds
     * A Hierarchy of Nodes given by the .bvh header
     * */
    protected Node _readHeader(String path, Scene scene, Node reference){
        Node root = null;
        try {
            _buffer = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        Node currentNode = root;
        Node currentRoot = reference;
        Properties currentProperties = null;
        boolean boneBraceOpened = false;
        //READ ALL THE HEADER
        String line = "";
        while(!line.contains("FRAME_TIME") &&
                !line.contains("FRAME TIME") ){
            try {
                line = _buffer.readLine();
                if(line == null) return root;
                line = line.toUpperCase();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            //clean line
            line.replace("\t", " ");
            line.replace("\n", "");
            line.replace("\r", "");
            line.replace(":", "");
            line = line.trim();
            //split
            String[] expression = line.split(" ");
            //Check Type
            if(expression[0].equals("ROOT")){
                if(root != null){
                    //TODO : Allow multiple ROOTS
                    while(!line.equals("MOTION")) {
                        try {
                            line = _buffer.readLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                        //clean line
                        line.replace("\t", " ");
                        line.replace("\n", "");
                        line.replace("\r", "");
                        line.replace(":", " ");
                    }
                    return root;
                }
                //Create a node
                try {
                    root = _nodeClass.getConstructor(Scene.class).newInstance(scene);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                root.setReference(reference);
                currentNode = root;
                currentRoot = root;
                currentProperties = new Properties(expression[1]);
                _joint.put(currentNode, currentProperties);
                _poses.put(currentNode, new ArrayList<>());
                boneBraceOpened = true;
            } else if(expression[0].equals("OFFSET")) {
                if(!boneBraceOpened) continue;
                float x = Float.valueOf(expression[1]);
                float y = Float.valueOf(expression[2]);
                float z = Float.valueOf(expression[3]);
                currentNode.setTranslation(x,y,z);
            } else if(expression[0].equals("CHANNELS")) {
                currentProperties.channels = Integer.valueOf(expression[1]);
                for (int i = 0; i < currentProperties.channels; i++)
                    currentProperties.addChannelType(expression[i+2]);
            } else if(expression[0].equals("JOINT")) {
                //Create a node
                try {
                    currentNode = _nodeClass.getConstructor(Scene.class).newInstance(scene);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                currentNode.setReference(currentRoot);
                currentRoot = currentNode;
                currentProperties = new Properties(expression[1]);
                _joint.put(currentNode, currentProperties);
                _poses.put(currentNode, new ArrayList<>());
                boneBraceOpened = true;
            } else if(expression[0].equals("END_SITE")) {
                boneBraceOpened = false;
            } else if(expression[0].equals("}")) {
                if(boneBraceOpened){
                    currentRoot = currentRoot.reference();
                }else{
                    boneBraceOpened = true;
                }
            } else if(expression[0].equals("FRAMES")) {
                _frames = Integer.valueOf(expression[1]);
            } else if(expression[0].equals("FRAME_TIME")) {
                _period = Integer.valueOf(expression[1]);
            } else if(expression.length >= 2) {
                if ((expression[0] + " " + expression[1]).equals("END SITE")) {
                    boneBraceOpened = false;
                }
            }else if(expression.length >= 3) {
                if ((expression[0] + " " + expression[1]).equals("FRAME TIME")) {
                    _period = Integer.valueOf(expression[2]);
                }
            }
        }
        _root = root;
        _branch = scene.branch(_root);
        return root;
    }

    /*Saves Frame info to be read in a later stage*/
    protected void _saveFrames(){
        boolean next = true;
        while(next)
            next = _readNextFrame();
    }

    protected boolean _readNextFrame(){
        //READ JUST ONE LINE
        String line = "";
        try {
            line = _buffer.readLine();
            if(line == null) return false;
            line = line.toUpperCase();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        //clean line
        line.replace("\t", " ");
        line.replace("\n", "");
        line.replace("\r", "");
        line = line.trim();
        //split
        String[] expression = line.split(" ");
        //traverse each line
        int i = 0;
        for(Node node : _branch){
            Properties properties = _joint.get(node);
            boolean translationInfo = false;
            boolean rotationInfo = false;
            Vector translation = new Vector();
            Quaternion rotation = new Quaternion();
            Vector euler_params = new Vector();

            for(String channel : properties._channelType){
                float value = Float.valueOf(expression[i]);
                switch(channel){
                    case "XPOSITION":{
                        translationInfo = true;
                        translation.setX(value);
                        break;
                    }
                    case "YPOSITION":{
                        translation.setY(value);
                        break;
                    }
                    case "ZPOSITION":{
                        translation.setZ(value);
                        break;
                    }
                    case "ZROTATION":{
                        rotationInfo = true;
                        rotation.compose(new Quaternion(new Vector(0,0,1), PApplet.radians(value)));
                        euler_params.setZ(PApplet.radians(value));
                        break;
                    }
                    case "YROTATION":{
                        rotation.compose(new Quaternion(new Vector(0,1,0), PApplet.radians(value)));
                        euler_params.setY(PApplet.radians(value));
                        break;
                    }
                    case "XROTATION":{
                        rotation.compose(new Quaternion(new Vector(1,0,0), PApplet.radians(value)));
                        euler_params.setX(PApplet.radians(value));
                        break;
                    }
                }
                i++;
            }
            Frame frame = new Frame(node.translation().get(), node.rotation().get());

            switch(properties._parametrization){
                case "XYZ":{
                    if(node.reference() != null) {
                        Quaternion newRotation = _poses.get(node.reference()).get(_poses.get(node.reference()).size()-1).rotation();
                        Quaternion delta = Quaternion.compose(newRotation, node.reference().rotation().inverse());
                        Vector v = delta.multiply(translation.get());
                        _joint.get(node.reference())._feasibleRegion.add(v);
                    }
                    break;
                }
                case "Euler":{
                    properties._feasibleRegion.add(euler_params);
                    break;
                }
            }

            if(rotationInfo){
                frame.setRotation(rotation);
            }if(translationInfo){
                frame.setTranslation(translation);
            }
            _poses.get(node).add(frame);
        }
        return true;
    }

    public void nextPose(){
        if(_currentPose >= _poses.get(_root).size()){
            if(_loop) _currentPose = 0;
            else return;
        }
        for(Node node : _branch){
            node.setRotation(_poses.get(node).get(_currentPose).rotation().get());
            node.setTranslation(_poses.get(node).get(_currentPose).translation().get());
        }
        _currentPose++;
    }

    int dim = 20;

    public void drawConstraint(PGraphics pg){
        pg.pushStyle();
        pg.strokeWeight(3);
        pg.stroke(0,0,255);
        for(Node node : _branch){
            //if(node == _root) continue;
            DistanceFieldConstraint constraint = (DistanceFieldConstraint) node.constraint();
            pg.pushMatrix();
            node.reference().applyWorldTransformation();
            Vector tr = node.children().isEmpty() ? new Vector(0,0,1) : node.children().get(0).translation().get();
            float step = (float)(2*Math.PI/dim);
            for(int i = 0; i < dim; i++){
                for(int j = 0; j < dim; j++){
                    for(int k = 0; k < dim; k++){
                        //Transform to euler
                        float x = (i + 0.5f) * step;
                        float y = (j + 0.5f) * step;
                        float z = (k + 0.5f) * step;
                        Vector v = Vector.multiply(tr, 0.5f);
                        v = new Quaternion(x,y,z).rotate(v);
                        if(constraint.distance_field()[i][j][k] < 0.2){
                            v.add(node.translation());
                            pg.line(node.translation().x(), node.translation().y(), node.translation().z(), v.x(), v.y(), v.z());
                        }
                    }
                }
            }

            /*
            int i = 0;
            for(float xx = 0; xx < 2*Math.PI; xx+= 2*Math.PI/16){
                int j = 0;
                for(float yy = 0; yy < 2*Math.PI; yy += 2*Math.PI/16){
                    int k = 0;
                    for(float zz = 0; zz < 2*Math.PI; zz+= 2*Math.PI/16){
                        float x = xx > Math.PI ? (float)(xx - 2*Math.PI) : xx;
                        float y = yy > Math.PI ? (float)(yy - 2*Math.PI) : yy;
                        float z = zz > Math.PI ? (float)(zz - 2*Math.PI) : zz;
                        Quaternion q = new Quaternion(x,y,z);
                        tr.normalize();
                        Vector v = Vector.multiply(tr, 5);//node.rotation().inverseRotate(tr);
                        v = q.inverseRotate(v);
                        DistanceFieldConstraint c = (DistanceFieldConstraint)node.constraint();
                        if(c.distance_field()[(int)(24*xx/(2*Math.PI))][(int)(24*yy/(2*Math.PI))][(int)(24*zz/(2*Math.PI))] < 1) {
                        //if(c.distance_field()[i][j][k] < 0.2) {
                            v.add(node.translation());
                            pg.line(node.translation().x(), node.translation().y(), node.translation().z(), v.x(), v.y(), v.z());
                        }k++;
                    }j++;
                }i++;
            }*/
            pg.popMatrix();
        }
        pg.popStyle();
    }


    public void drawFeasibleRegion(PGraphics pg){
        pg.pushStyle();
        pg.strokeWeight(3);
        pg.stroke(0,255,0);
        for(Node node : _branch){
            pg.pushMatrix();
            node.applyWorldTransformation();
            for(Vector vv : _joint.get(node)._feasibleRegion){
                Vector v = node.rotation().inverseRotate(vv);
                v.multiply(0.25f);
                pg.stroke(0,255,0);
                pg.line(0,0,0, v.x(), v.y(), v.z());
                pg.stroke(0,0,255);
                pg.line(0,0,0, -v.x(), -v.y(), -v.z());

            }
            pg.popMatrix();
        }
        pg.popStyle();
    }

    public void constraintJoints(){
        for(Node node : _branch){
            //if(node == _root) continue;
            ArrayList<Quaternion> rots = new ArrayList<Quaternion>();
            int j = 0;
            for(Frame f : _poses.get(node)){
                //if(j == 20) break;
                rots.add(f.rotation());
                j++;
            }
            node.setConstraint(new DistanceFieldConstraint(LearnConstraint.getDistanceField(rots,dim,dim,dim)));
        }
    }
}

