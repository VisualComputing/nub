package ik.interactiveSkeleton;

import common.InteractiveNode;
import common.InteractiveShape;
import frames.core.Graph;
import frames.core.Node;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;

/**
 * Created by sebchaparr on 24/02/18.
 */

public class InteractiveSkeleton extends PApplet {
  Scene scene;
  Node eye;
  InteractiveShape shape;
  Joint root;
  String shapePath = "/testing/data/objs/Female_low_poly.obj";


  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    //hint(DISABLE_OPTIMIZED_STROKE);
    hint(DISABLE_DEPTH_TEST);

    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    eye = new InteractiveNode(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.setDefaultNode(eye);
    scene.fitBallInterpolation();

    //Create an initial Joint at the center of the Shape
    root = new InteractiveJoint(scene);
    root.setRoot(true);
    //Create and load an InteractiveShape
    PShape model = loadShape(sketchPath() + shapePath);
    model.setFill(color(255, 0, 0, 50));
    Vector[] box = getBoundingBox(model);
    //Scale model
    float max = max(abs(box[0].x() - box[1].x()), abs(box[0].y() - box[1].y()), abs(box[0].z() - box[1].z()));
    model.scale(200.f * 1.f / max);

    shape = new InteractiveShape(scene, model);
    shape.setPrecision(Node.Precision.FIXED);
    shape.setPrecisionThreshold(1);
    shape.rotate(new Quaternion(new Vector(0, 0, 1), PI));

  }

  public void draw() {
    background(0);
    lights();
    //Draw Constraints
    scene.drawAxes();
    for (Node frame : scene.nodes()) {
      if (frame instanceof Shape) {
        ((Shape) frame).draw();
        pushMatrix();
        frame.applyWorldTransformation();
        scene.drawAxes(10);
        popMatrix();
      }
    }
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
    PApplet.main(new String[]{"ik.interactiveSkeleton.InteractiveSkeleton"});
  }
}