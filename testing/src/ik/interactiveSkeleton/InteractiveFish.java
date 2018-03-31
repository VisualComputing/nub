package ik.interactiveSkeleton;

import common.InteractiveNode;
import common.InteractiveShape;
import frames.core.Graph;
import frames.core.Interpolator;
import frames.core.Node;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import ik.common.Joint;
import ik.common.LinearBlendSkinningGPU;
import ik.common.Target;
import processing.core.PApplet;
import processing.core.PShape;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 11/03/18.
 */
public class InteractiveFish extends PApplet {
  Scene scene;
  Node eye;
  InteractiveShape shape;
  Joint root;
  //Uncomment to use Linear Blending Skinning with CPU
  // LinearBlendSkinning skinning;
  LinearBlendSkinningGPU skinning;
  Target target;
  Interpolator targetInterpolator;
  String shapePath = "/testing/data/objs/TropicalFish01.obj";
  String texturePath = "/testing/data/objs/TropicalFish01.jpg";

  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    eye = new InteractiveNode(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.setDefaultGrabber(eye);
    scene.fitBallInterpolation();
    target = new Target(scene);

    PShape model = loadShape(sketchPath() + shapePath);
    model.setTexture(loadImage(sketchPath() + texturePath));

    Vector[] box = getBoundingBox(model);
    //Scale model
    float max = max(abs(box[0].x() - box[1].x()), abs(box[0].y() - box[1].y()), abs(box[0].z() - box[1].z()));
    //model.scale(200.f*1.f/max);
    //Invert Y Axis and set Fill
    shape = new InteractiveShape(scene, model);
    shape.setPrecision(Node.Precision.FIXED);
    shape.setPrecisionThreshold(1);

    shape.rotate(new Quaternion(new Vector(0, 0, 1), PI));
    shape.scale(200.f * 1.f / max);
    root = fishSkeleton(shape);

    ArrayList<Node> skeleton = scene.branch(root);

    //Uncomment to use Linear Blending Skinning with CPU
    // skinning = new LinearBlendSkinning(shape, model);
    // skinning.setup(skeleton);
    skinning = new LinearBlendSkinningGPU(shape, skeleton);
    skinning.setSkinning(this, scene);
    //Adding IK behavior
    target.setPosition(skeleton.get(skeleton.size() - 1).position());
    //Making a default Path that target must follow
    targetInterpolator = setupTargetInterpolator(target);
    scene.registerTreeSolver(root);
    scene.addIKTarget(skeleton.get(skeleton.size() - 1), target);
  }

  public void draw() {
    skinning.updateParams();
    background(0);
    lights();
    //Draw Constraints
    scene.drawAxes();
    //comment this line if you're using Linear Blending Skinning with CPU
    shader(skinning.shader);
    shape.draw();
    resetShader();
    //Uncomment to use Linear Blending Skinning with CPU
    //skinning.applyTransformations();
  }

  public static Vector[] getBoundingBox(PShape shape) {
    Vector v[] = new Vector[2];
    float minx = 999;
    float miny = 999;
    float maxx = -999;
    float maxy = -999;
    float minz = 999;
    float maxz = -999;
    for (int j = 0; j < shape.getChildCount(); j++) {
      PShape aux = shape.getChild(j);
      for (int i = 0; i < aux.getVertexCount(); i++) {
        float x = aux.getVertex(i).x;
        float y = aux.getVertex(i).y;
        float z = aux.getVertex(i).z;
        minx = minx > x ? x : minx;
        miny = miny > y ? y : miny;
        minz = minz > z ? z : minz;
        maxx = maxx < x ? x : maxx;
        maxy = maxy < y ? y : maxy;
        maxz = maxz < z ? z : maxz;
      }
    }

    v[0] = new Vector(minx, miny, minz);
    v[1] = new Vector(maxx, maxy, maxz);
    return v;
  }

  public Joint fishSkeleton(Node reference) {
    Joint j1 = new Joint(scene);
    j1.setReference(reference);
    j1.setPosition(0, 10.8f, 93);
    j1.setScaling(1.f / reference.scaling());
    Joint j2 = new Joint(scene);
    j2.setReference(j1);
    j2.setPosition(0, 2.3f, 54.7f);
    Joint j3 = new Joint(scene);
    j3.setReference(j2);
    j3.setPosition(0, 0.4f, 22);
    Joint j4 = new Joint(scene);
    j4.setReference(j3);
    j4.setPosition(0, 0, -18);
    Joint j5 = new Joint(scene);
    j5.setReference(j4);
    j5.setPosition(0, 1.8f, -54);
    Joint j6 = new Joint(scene);
    j6.setReference(j5);
    j6.setPosition(0, -1.1f, -95);
    j1.setRoot(true);
    return j1;
  }

  public Interpolator setupTargetInterpolator(Node target) {
    Interpolator targetInterpolator = new Interpolator(target);
    targetInterpolator.setLoop();
    targetInterpolator.setSpeed(3.2f);
    // Create an initial path
    int nbKeyFrames = 10;
    float step = 2.0f * PI / (nbKeyFrames - 1);
    for (int i = 0; i < nbKeyFrames; i++) {
      InteractiveNode iFrame = new InteractiveNode(scene);
      iFrame.setPosition(new Vector(50 * sin(step * i), target.position().y(), target.position().z()));
      targetInterpolator.addKeyFrame(iFrame);
    }
    targetInterpolator.start();
    return targetInterpolator;
  }

  public void printSkeleton(Node root) {
    int i = 0;
    for (Node node : scene.branch(root)) {
      System.out.println("Node " + i + " : " + node.position());
      i++;
    }
  }

  public void keyPressed() {
    if (key == ' ') {
      printSkeleton(root);
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.interactiveSkeleton.InteractiveFish"});
  }
}