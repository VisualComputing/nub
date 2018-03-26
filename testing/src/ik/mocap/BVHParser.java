package ik.mocap;
import frames.core.Node;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

    public BVHParser(Class<? extends Node> nodeClass){
        _nodeClass = nodeClass;
        _init();
    }



    class Properties{
        protected String _name;
        protected int channels;
        protected List<String> _channelType;

        Properties(String name){
            _name = name;
            channels = 0;
            _channelType = new ArrayList<String>();
        }

        boolean addChannelType(String type){
            return _channelType.add(type);
        }
    }

    protected void _init(){
        _frames = 0;
        _period = 0;
        _joint = new HashMap<>();
    }

    /**
     * Reads a .bvh file from the given path and builds
     * A Hierarchy of Nodes given by the .bvh header
     * */
    Node readHeader(String path, Scene scene, Node reference){
        _init();
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

    void readNextFrame(){
        //READ JUST ONE LINE
        String line = "";
        try {
            line = _buffer.readLine();
            if(line == null) return;
            line = line.toUpperCase();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println(line);

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
            System.out.println("BONE : " + properties._name);
            boolean translationInfo = false;
            boolean rotationInfo = false;
            Vector translation = new Vector();
            Quaternion rotation = new Quaternion();
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
                        break;
                    }
                    case "YROTATION":{
                        rotation.compose(new Quaternion(new Vector(0,1,0), PApplet.radians(value)));
                        break;
                    }
                    case "XROTATION":{
                        rotation.compose(new Quaternion(new Vector(1,0,0), PApplet.radians(value)));
                        break;
                    }
                }
                i++;
            }
            if(rotationInfo){
                node.setRotation(rotation);
            }if(translationInfo){
                node.setTranslation(translation);
            }
        }
    }
}

