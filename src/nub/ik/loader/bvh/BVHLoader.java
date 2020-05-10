package nub.ik.loader.bvh;

import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Constraint;
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

  public BVHLoader(Class<? extends Node> nodeClass, String path, Scene scene, Node reference) {
    _class = nodeClass;
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

  public List<Node> branch() {
    return _branch;
  }

  public int poses() {
    return _poses.size();
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
      line = line.toUpperCase();
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
          root = _class.getConstructor().newInstance();
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
          current = _class.getConstructor().newInstance();
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
    if (root instanceof Joint) ((Joint) root).setRoot(true);

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
      Node next = Node.detach(current.translation().get(), current.rotation().get(), 1);

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
    for (Node node : _branch) {
      Constraint c = node.constraint();
      if (node == _root) node.setConstraint(null);
      node.setRotation(_poses.get(node.id()).get(_currentPose).rotation().get());
      node.setTranslation(_poses.get(node.id()).get(_currentPose).translation().get());
      node.setConstraint(c);
    }
    _currentPose++;
  }

  public void poseAt(int idx) {
    if (idx >= _poses.get(_root.id()).size()) {
      return;
    }
    for (Node node : _branch) {
      node.setRotation(_poses.get(node.id()).get(idx).rotation().get());
      node.setTranslation(_poses.get(node.id()).get(idx).translation().get());
    }
  }

  protected Vector findRestVector(Node node) {
    List<Node> poses = _poses.get(node.id());
    Vector init = new Vector(0, 1, 0); //use any vector
    //if(node.children().size() == 1){
    //init = node.children().get(0).translation().get();
    //return init;
    //}

    //TODO : Prefer child direction
    Quaternion restRotation = node.rotation().get();

    Vector rest = init.get();
    System.out.println("init " + init);
    for (Node keyNode : poses) {
      Quaternion delta = Quaternion.compose(restRotation.inverse(), keyNode.rotation());
      delta.normalize();
      rest.add(delta.rotate(init));
      //System.out.println("delta " + delta.axis() + delta.angle() + " "  + " rest " + rest);
    }

    if (rest.magnitude() < 0.001f) //pick any vector
      rest = init;


    rest.multiply(1f / (poses.size() + 1));


    return rest;
  }

  public void generateConstraints() {
    for (Node node : _branch) {
      if (node == _root) continue;
      generateConstraint(node);
    }
  }


  protected void generateConstraint(Node node) {
    Vector rest = findRestVector(node);
    List<Node> poses = _poses.get(node.id());
    Quaternion restRotation = node.rotation().get();
    Vector up = rest.orthogonalVector();
    Vector right = Vector.cross(rest, up, null);

    if (node.children() == null || node.children().isEmpty()) {
      return;
    }

    float minTwist = 0, maxTwist = 0;
    float upAngle = 0, downAngle = 0, leftAngle = 0, rightAngle = 0;

    for (Node keyNode : poses) {
      Quaternion delta = Quaternion.compose(restRotation.inverse(), keyNode.rotation());
      delta.normalize();
      Vector local_rest = delta.inverseRotate(rest);
      Vector local_up = delta.inverseRotate(up);
      Vector local_right = delta.inverseRotate(right);

      Quaternion deltaRest = decomposeQuaternion(delta, local_rest);


      if (deltaRest.axis().dot(local_rest) >= 0) maxTwist = Math.max(deltaRest.angle(), maxTwist);
      else minTwist = Math.max(deltaRest.angle(), minTwist);

      Quaternion deltaUp = decomposeQuaternion(delta, local_up);
      if (deltaUp.axis().dot(local_up) >= 0) upAngle = Math.max(deltaUp.angle(), upAngle);
      else downAngle = Math.max(deltaUp.angle(), downAngle);

      Quaternion deltaRight = decomposeQuaternion(delta, local_right);
      if (deltaRight.axis().dot(local_right) >= 0) rightAngle = Math.max(deltaRight.angle(), rightAngle);
      else leftAngle = Math.max(deltaRight.angle(), leftAngle);
    }

    //Clamp angles between 5 and 85 degrees
    System.out.println("(*) -" + _joint.get(node.id()).name() + " constraint : ");
    System.out.println("\t\t" + "rest " + rest + " up " + up + " right " + right);
    System.out.println("\t\t" + "up " + Math.toDegrees(upAngle) + "down   " + Math.toDegrees(downAngle));
    System.out.println("\t\t" + "left   " + Math.toDegrees(leftAngle) + "  rigth   " + Math.toDegrees(rightAngle));
    System.out.println("\t\t" + "min tw   " + Math.toDegrees(minTwist) + "  max tw   " + Math.toDegrees(maxTwist));


    upAngle = Math.min(Math.max(upAngle, (float) Math.toRadians(5)), (float) Math.toRadians(85));
    downAngle = Math.min(Math.max(downAngle, (float) Math.toRadians(5)), (float) Math.toRadians(85));
    leftAngle = Math.min(Math.max(leftAngle, (float) Math.toRadians(5)), (float) Math.toRadians(85));
    rightAngle = Math.min(Math.max(rightAngle, (float) Math.toRadians(5)), (float) Math.toRadians(85));
    minTwist = Math.min(Math.max(minTwist, (float) Math.toRadians(5)), (float) Math.toRadians(175));
    maxTwist = Math.min(Math.max(maxTwist, (float) Math.toRadians(5)), (float) Math.toRadians(175));

    System.out.println("(* AFTER) -" + _joint.get(node.id()).name() + " constraint : ");
    System.out.println("\t\t" + "rest " + rest + " up " + up + " right    " + right);
    System.out.println("\t\t" + "up   " + Math.toDegrees(upAngle) + "down   " + Math.toDegrees(downAngle));
    System.out.println("\t\t" + "left   " + Math.toDegrees(leftAngle) + "  rigth   " + Math.toDegrees(rightAngle));
    System.out.println("\t\t" + "min tw   " + Math.toDegrees(minTwist) + "  max tw   " + Math.toDegrees(maxTwist));

    BallAndSocket constraint = new BallAndSocket(downAngle, upAngle, leftAngle, rightAngle);
    constraint.setRestRotation(restRotation, right, rest);
    constraint.setTwistLimits(minTwist, maxTwist);
    node.setConstraint(constraint);
  }

  protected Quaternion decomposeQuaternion(Quaternion quaternion, Vector axis) {
    Vector rotationAxis = new Vector(quaternion._quaternion[0], quaternion._quaternion[1], quaternion._quaternion[2]);
    rotationAxis = Vector.projectVectorOnAxis(rotationAxis, axis); // w.r.t idle
    //Get rotation component on Axis direction
    return new Quaternion(rotationAxis.x(), rotationAxis.y(), rotationAxis.z(), quaternion.w()); //w.r.t rest
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

