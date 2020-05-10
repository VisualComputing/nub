package nub.ik.skinning;

import nub.core.Node;
import nub.ik.animation.Skeleton;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.*;
import processing.opengl.PShader;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sebchaparr on 11/03/18.
 */
public class GPULinearBlendSkinning implements Skinning {
  //Skeleton & Geometry information
  protected List<PShape> _shapes;
  protected List<Node> _skeleton;
  protected PGraphics _pg;
  //Shader information
  protected PShader _shader;
  protected Quaternion[] _initialOrientations;
  protected Vector[] _initialPositions;
  protected float[] _initialPositionsArray;
  protected float[] _positionsArray;
  protected float[] _orientationsArray;
  protected Map<Node, Integer> _ids;
  protected final String _fragmentPath = "frag.glsl";
  protected final String _vertexPath = "skinning.glsl";
  protected Node _reference;

  public GPULinearBlendSkinning(List<Node> skeleton, PGraphics pg, PShape shape) {
    this._shapes = new ArrayList<>();
    _ids = new HashMap<>();
    _skeleton = skeleton;
    int joints = skeleton.size();
    for (int i = 0; i < joints; i++) {
      _ids.put(_skeleton.get(i), i);
            /*if(_skeleton.get(i) instanceof Joint) {
                int c = Color.HSBtoRGB((i + 1.0f) / skeleton.size(), 1f, 1f);
                ((Joint) _skeleton.get(i)).setColor(c);
            }*/
    }

    _initialOrientations = new Quaternion[joints];
    _initialPositions = new Vector[joints];
    _initialPositionsArray = new float[joints * 3];
    _positionsArray = new float[joints * 3];
    _orientationsArray = new float[joints * 4];
    PApplet pApplet = pg.parent;
    _shader = pApplet.loadShader(_fragmentPath, _vertexPath);
    _shapes.add(shape);
    _pg = pg;
    initParams();
  }

  public GPULinearBlendSkinning(Skeleton skeleton, PShape shape) {
    this(skeleton.BFS(), skeleton.scene().context(), shape);
    _reference = skeleton.reference();
  }

  public GPULinearBlendSkinning(List<Node> skeleton, PGraphics pg, String shape, String texture, float factor) {
    this(skeleton, pg, shape, texture, factor, false);
  }

  public GPULinearBlendSkinning(Skeleton skeleton, String shape, String texture, float factor) {
    this(skeleton, shape, texture, factor, false);
    _reference = skeleton.reference();
  }

  public GPULinearBlendSkinning(Skeleton skeleton, String shape, String texture, float factor, boolean quad) {
    this(skeleton.BFS(), skeleton.scene().context(), shape, texture, factor, quad);
    _reference = skeleton.reference();
  }

  public GPULinearBlendSkinning(List<Node> skeleton, PGraphics pg, String shape, String texture, float factor, boolean quad) {
    this._shapes = new ArrayList<>();
    _ids = new HashMap<>();
    _skeleton = skeleton;
    int joints = skeleton.size();
    for (int i = 0; i < joints; i++) {
      _ids.put(_skeleton.get(i), i);
      if (_skeleton.get(i) instanceof Joint) {
        int c = Color.HSBtoRGB((i + 1.0f) / skeleton.size(), 1f, 1f);
        ((Joint) _skeleton.get(i)).setColor((int) pg.red(c), (int) pg.blue(c), (int) pg.green(c));
      }
    }
    _initialOrientations = new Quaternion[joints];
    _initialPositions = new Vector[joints];
    _initialPositionsArray = new float[joints * 3];
    _positionsArray = new float[joints * 3];
    _orientationsArray = new float[joints * 4];
    PApplet pApplet = pg.parent;
    _shader = pApplet.loadShader(_fragmentPath, _vertexPath);
    _shapes.add(createShape(pg, pg.loadShape(shape), texture, factor, quad));
    initParams();
  }


  public PShader shader() {
    return _shader;
  }

  public List<PShape> shapes() {
    return _shapes;
  }

  public PShape shape() {
    return _shapes.get(0);
  }

  @Override
  public List<Node> skeleton() {
    return _skeleton;
  }

  public Map<Node, Integer> ids() {
    return _ids;
  }

  public void setReference(Node reference) {
    _reference = reference;
  }

  @Override
  public void initParams() {
    for (int i = 0; i < _skeleton.size(); i++) {
      Vector v = _skeleton.get(i).position();
      Quaternion q = _skeleton.get(i).orientation();
      _initialOrientations[i] = q;
      _initialPositions[i] = v.get();
      _initialPositionsArray[i * 3 + 0] = v.x();
      _initialPositionsArray[i * 3 + 1] = v.y();
      _initialPositionsArray[i * 3 + 2] = v.z();
    }
    _shader.set("bonePositionOrig", _initialPositionsArray);
    _shader.set("boneLength", _skeleton.size());
    _shader.set("paintMode", -1);
  }

  @Override
  public void updateParams() {
    //TODO: IT COULD BE DONE WITH LESS OPERATIONS
    for (int i = 0; i < _skeleton.size(); i++) {
      Vector v = Vector.subtract(_skeleton.get(i).position(), _initialPositions[i]);
      Quaternion q = Quaternion.compose(_skeleton.get(i).orientation(), _initialOrientations[i].inverse());
      _positionsArray[i * 3 + 0] = v.x();
      _positionsArray[i * 3 + 1] = v.y();
      _positionsArray[i * 3 + 2] = v.z();
      _orientationsArray[i * 4 + 0] = q.x();
      _orientationsArray[i * 4 + 1] = q.y();
      _orientationsArray[i * 4 + 2] = q.z();
      _orientationsArray[i * 4 + 3] = q.w();
    }
    _shader.set("bonePosition", _positionsArray);
    _shader.set("boneRotation", _orientationsArray);
  }

  public void paintAllJoints() {
    _shader.set("paintMode", 0);
  }

  public void paintJoint(int id) {
    _shader.set("paintMode", id);
  }

  public void disablePaintMode() {
    _shader.set("paintMode", -1);
  }

  public float[] addWeights(List<Node> branch, PVector vector) {
    Vector position = new Vector(vector.x, vector.y, vector.z);
    float total_dist = 0.f;
    int[] joints = new int[]{-1, -1, -1};
    float[] w = new float[]{0, 0, 0};
    float[] d = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
    //Find the nearest 3 joints
    //TODO : Perhaps enable more joints - use QuickSort
    for (Node joint : branch) {
      if (_ids.get(joint.reference()) == null) continue;
      if (joint.translation().magnitude() <= Float.MIN_VALUE) continue;
      float dist = (float) Math.pow(getDistance(position, joint), 10);
      if (dist <= d[0] || dist <= d[1] || dist <= d[2]) {
        int start = dist <= d[0] ? 0 : dist <= d[1] ? 1 : 2;
        for (int l = joints.length - 1; l > start; l--) {
          joints[l] = joints[l - 1];
          d[l] = d[l - 1];
        }
        joints[start] = _ids.get(joint.reference());
        d[start] = dist;
      }
    }

    for (int k = 0; k < joints.length; k++) {
      total_dist += 1.f / d[k];
    }
    for (int k = 0; k < joints.length; k++) {
      w[k] += 1.f / d[k] / total_dist;
    }

    return new float[]{joints[0], joints[1], joints[2], w[0], w[1], w[2]};
  }

  /*
   * Get the distance from vertex to line formed by frame and the reference frame of frame
   * Distance will be measure according to root coordinates.
   * If the reference frame is the root, it will return distance from vertex to frame
   * */
  public static float getDistance(Vector vertex, Node node) {
    if (node == null) return Float.MAX_VALUE;
    Vector position = node.position();
    if (node.reference() == null) return Vector.distance(position, vertex);
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
      distance = Vector.subtract(parentPosition, vertex);
    }
    if (u > 1) {
      distance = Vector.subtract(position, vertex);
    }
    return distance.magnitude();
  }

  @Override
  public void render(PGraphics pg) {
    updateParams();
    pg.shader(_shader);
    for (PShape shape : _shapes) {
      pg.shape(shape);
    }
    pg.resetShader();
  }


  @Override
  public void render(Scene scene) {
    render(scene, _reference);
  }

  @Override
  public void render() {
    render(_pg);
  }

  @Override
  public void render(Scene scene, Node reference) {
    if (reference != null) scene.applyWorldTransformation(reference);
    render(scene.context());
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
    if (r.getChildCount() == 0) {
      for (int j = 0; j < r.getVertexCount(); j++) {
        PVector p = r.getVertex(j).mult(scaleFactor);
        System.out.println("scale " + scaleFactor);
        System.out.println("v " + p);
        PVector n = r.getNormal(j);
        float u = r.getTextureU(j);
        float v = r.getTextureV(j);
        s.normal(n.x, n.y, n.z);
        float[] params = addWeights(_skeleton, p);
        s.attrib("joints", params[0] * 1.f, params[1] * 1.f, params[2] * 1.f);
        s.attrib("weights", params[3], params[4], params[5]);
        s.vertex(p.x, p.y, p.z, u, v);
      }
    } else {
      for (int i = 0; i < r.getChildCount(); i++) {
        for (int j = 0; j < r.getChild(i).getVertexCount(); j++) {
          PVector p = r.getChild(i).getVertex(j).mult(scaleFactor);
          PVector n = r.getChild(i).getNormal(j);
          float u = r.getChild(i).getTextureU(j);
          float v = r.getChild(i).getTextureV(j);
          s.normal(n.x, n.y, n.z);
          float[] params = addWeights(_skeleton, p);
          s.attrib("joints", params[0] * 1.f, params[1] * 1.f, params[2] * 1.f);
          s.attrib("weights", params[3], params[4], params[5]);

          s.vertex(p.x, p.y, p.z, u, v);
        }
      }
    }
    s.endShape();
    return s;
  }
}
