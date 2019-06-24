package nub.ik.loader.bvh;

import nub.core.Node;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
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
 * <p>
 * For more info look at http://www.dcs.shef.ac.uk/intranet/research/public/resmes/CS0111.pdf
 * Created by sebchaparr on 23/03/18.
 */
public class BVHLoader {
  //TODO : Update
  protected BufferedReader _buffer;
  //A Joint is a Node with some Properties
  //TODO: Consider _id() public?
  protected HashMap<Integer, Properties> _joint;
  protected int _frames;
  protected int _period;
  protected Class<? extends Node> _class;
  protected Node _root;
  protected List<Node> _branch;
  protected HashMap<Integer, ArrayList<Node>> _poses;
  protected int _currentPose;
  protected boolean _loop;


  public BVHLoader(String path, Scene scene, Node reference) {
    _class = Joint.class;
    _setup(path, scene, reference);
  }

  public BVHLoader(Class<? extends Node> frameClass, String path, Scene scene, Node reference) {
    _class = frameClass;
    _setup(path, scene, reference);
  }

  public HashMap<Integer, Properties> joint() {
    return _joint;
  }

  public class Properties {
    protected String _name;
    protected int channels;
    protected List<String> _channelType;
    protected String _parametrization; //Possible options: XYZ, EULER
    protected List<Vector> _feasibleRegion;

    Properties(String name) {
      _name = name;
      channels = 0;
      _channelType = new ArrayList<String>();
      _parametrization = "XYZ";
      _feasibleRegion = new ArrayList<Vector>();
    }

    public String name() {
      return _name;
    }

    public boolean addChannelType(String type) {
      return _channelType.add(type);
    }

    public void setParametrization(String parametrization) {
      this._parametrization = parametrization;
    }

  }

  public Node root() {
    return _root;
  }

  protected void _setup(String path, Scene scene, Node reference) {
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
   */
  protected Node _readHeader(String path, Scene scene, Node reference) {
    Node root = null;
    try {
      _buffer = new BufferedReader(new FileReader(path));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    }
    Node current = root;
    Node currentRoot = reference;
    Properties currentProperties = null;
    boolean boneBraceOpened = false;
    //READ ALL THE HEADER
    String line = "";
    while (!line.contains("FRAME_TIME") &&
        !line.contains("FRAME TIME")) {
      try {
        line = _buffer.readLine();
        if (line == null) return root;
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
      if (expression[0].equals("ROOT")) {
        if (root != null) {
          //TODO : Allow multiple ROOTS
          while (!line.equals("MOTION")) {
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
        //Create a Frame
        try {
          root = _class.getConstructor(Scene.class).newInstance(scene);
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
        current = root;
        currentRoot = root;
        currentProperties = new Properties(expression[1]);
        if (root instanceof Joint) {
          ((Joint) current).setName(expression[1]);
        }
        _joint.put(current.id(), currentProperties);
        _poses.put(current.id(), new ArrayList<>());
        boneBraceOpened = true;
      } else if (expression[0].equals("OFFSET")) {
        if (!boneBraceOpened) continue;
        float x = Float.valueOf(expression[1]);
        float y = Float.valueOf(expression[2]);
        float z = Float.valueOf(expression[3]);
        current.setTranslation(x, y, z);
      } else if (expression[0].equals("CHANNELS")) {
        currentProperties.channels = Integer.valueOf(expression[1]);
        for (int i = 0; i < currentProperties.channels; i++)
          currentProperties.addChannelType(expression[i + 2]);
      } else if (expression[0].equals("JOINT")) {
        //Create a node
        try {
          current = _class.getConstructor(Scene.class).newInstance(scene);
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (NoSuchMethodException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
        current.setReference(currentRoot);
        currentRoot = current;
        currentProperties = new Properties(expression[1]);
        _joint.put(current.id(), currentProperties);
        _poses.put(current.id(), new ArrayList<>());
        boneBraceOpened = true;
      } else if (expression[0].equals("END_SITE")) {
        boneBraceOpened = false;
      } else if (expression[0].equals("}")) {
        if (boneBraceOpened) {
          currentRoot = currentRoot.reference();
        } else {
          boneBraceOpened = true;
        }
      } else if (expression[0].equals("FRAMES")) {
        _frames = Integer.valueOf(expression[1]);
      } else if (expression[0].equals("FRAME_TIME")) {
        _period = Integer.valueOf(expression[1]);
      } else if (expression.length >= 2) {
        if ((expression[0] + " " + expression[1]).equals("END SITE")) {
          boneBraceOpened = false;
        }
      } else if (expression.length >= 3) {
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

  protected void _saveFrames() {
    boolean next = true;
    while (next)
      next = _readNextFrame();
  }

  protected boolean _readNextFrame() {
    //READ JUST ONE LINE
    String line = "";
    try {
      line = _buffer.readLine();
      if (line == null) return false;
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
    for (Node current : _branch) {
      Properties properties = _joint.get(current.id());
      boolean translationInfo = false;
      boolean rotationInfo = false;
      Vector translation = new Vector();
      Quaternion rotation = new Quaternion();
      Vector euler_params = new Vector();

      for (String channel : properties._channelType) {
        float value = Float.valueOf(expression[i]);
        switch (channel) {
          case "XPOSITION": {
            translationInfo = true;
            translation.setX(value);
            break;
          }
          case "YPOSITION": {
            translation.setY(value);
            break;
          }
          case "ZPOSITION": {
            translation.setZ(value);
            break;
          }
          case "ZROTATION": {
            rotationInfo = true;
            rotation.compose(new Quaternion(new Vector(0, 0, 1), PApplet.radians(value)));
            euler_params.setZ(PApplet.radians(value));
            break;
          }
          case "YROTATION": {
            rotation.compose(new Quaternion(new Vector(0, 1, 0), PApplet.radians(value)));
            euler_params.setY(PApplet.radians(value));
            break;
          }
          case "XROTATION": {
            rotation.compose(new Quaternion(new Vector(1, 0, 0), PApplet.radians(value)));
            euler_params.setX(PApplet.radians(value));
            break;
          }
        }
        i++;
      }
      Node next = new Node(current.translation().get(), current.rotation().get(), 1);

      switch (properties._parametrization) {
        case "XYZ": {
          if (current.reference() != null) {
            Quaternion newRotation = _poses.get(current.reference().id()).get(_poses.get(current.reference().id()).size() - 1).rotation();
            Quaternion delta = Quaternion.compose(newRotation, current.reference().rotation().inverse());
            Vector v = delta.multiply(translation.get());
            _joint.get(current.reference().id())._feasibleRegion.add(v);
          }
          break;
        }
        case "Euler": {
          properties._feasibleRegion.add(euler_params);
          break;
        }
      }

      if (rotationInfo) {
        next.setRotation(rotation);
      }
      if (translationInfo) {
        next.setTranslation(translation);
      }
      _poses.get(current.id()).add(next);
    }
    return true;
  }

  public void nextPose() {
    if (_currentPose >= _poses.get(_root.id()).size()) {
      if (_loop) _currentPose = 0;
      else return;
    }
    for (Node frame : _branch) {
      frame.setRotation(_poses.get(frame.id()).get(_currentPose).rotation().get());
      frame.setTranslation(_poses.get(frame.id()).get(_currentPose).translation().get());
    }
    _currentPose++;
  }


  //----- TESTING SOME BEHAVIOR -----
  //TODO : CHECK WHEN METHODS WORK PROPERLY
  //TODO : LEARNING CONSTRAINTS
//
//    protected int _dim = 20;
//
//    public void drawConstraint(PGraphics pg){
//        pg.pushStyle();
//        pg.strokeWeight(3);
//        pg.stroke(0,0,255);
//        for(Node node : _branch){
//            //if(node == _root) continue;
//            DistanceFieldConstraint constraint = (DistanceFieldConstraint) node.constraint();
//            pg.pushMatrix();
//            node.graph().applyWorldTransformation(node);
//            Vector tr = node.children().isEmpty() ? new Vector(0,0,1) : node.children().get(0).translation().get();
//            float step = (float)(2*Math.PI/ _dim);
//            for(int i = 0; i < _dim; i++){
//                for(int j = 0; j < _dim; j++){
//                    for(int k = 0; k < _dim; k++){
//                        //Transform to euler
//                        float x = (i + 0.5f) * step;
//                        float y = (j + 0.5f) * step;
//                        float z = (k + 0.5f) * step;
//                        Vector v = Vector.multiply(tr, 0.5f);
//                        v = new Quaternion(x,y,z).rotate(v);
//                        if(constraint.distance_field()[i][j][k] < 0.2){
//                            v.add(node.translation());
//                            pg.line(node.translation().x(), node.translation().y(), node.translation().z(), v.x(), v.y(), v.z());
//                        }
//                    }
//                }
//            }
//
//            /*
//            int i = 0;
//            for(float xx = 0; xx < 2*Math.PI; xx+= 2*Math.PI/16){
//                int j = 0;
//                for(float yy = 0; yy < 2*Math.PI; yy += 2*Math.PI/16){
//                    int k = 0;
//                    for(float zz = 0; zz < 2*Math.PI; zz+= 2*Math.PI/16){
//                        float x = xx > Math.PI ? (float)(xx - 2*Math.PI) : xx;
//                        float y = yy > Math.PI ? (float)(yy - 2*Math.PI) : yy;
//                        float z = zz > Math.PI ? (float)(zz - 2*Math.PI) : zz;
//                        Quaternion q = new Quaternion(x,y,z);
//                        tr.normalize();
//                        Vector v = Vector.multiply(tr, 5);//node.rotation().inverseRotate(tr);
//                        v = q.inverseRotate(v);
//                        DistanceFieldConstraint c = (DistanceFieldConstraint)node.constraint();
//                        if(c.distance_field()[(int)(24*xx/(2*Math.PI))][(int)(24*yy/(2*Math.PI))][(int)(24*zz/(2*Math.PI))] < 1) {
//                        //if(c.distance_field()[i][j][k] < 0.2) {
//                            v.add(node.translation());
//                            pg.line(node.translation().x(), node.translation().y(), node.translation().z(), v.x(), v.y(), v.z());
//                        }k++;
//                    }j++;
//                }i++;
//            }*/
//            pg.popMatrix();
//        }
//        pg.popStyle();
//    }

    /*
    public void drawFeasibleRegion(PGraphics pg){
        pg.pushStyle();
        pg.strokeWeight(3);
        pg.stroke(0,255,0);
        for(Node node : _branch){
            pg.pushMatrix();
            node.graph().applyWorldTransformation(node);
            for(Vector vv : _joint.get(node.id())._feasibleRegion){
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
    */

    /*
    public void constraintJoints(){
        for(Node node : _branch){
            //if(node == _root) continue;
            ArrayList<Quaternion> rots = new ArrayList<Quaternion>();
            //int j = 0;
            for(Node next : _poses.get(node.id())){
                //if(j == 20) break;
                rots.add(next.rotation());
                //j++;
            }
            node.setConstraint(new DistanceFieldConstraint(LearnConstraint.getDistanceField(rots, _dim, _dim, _dim)));
        }
    }
    */

}

