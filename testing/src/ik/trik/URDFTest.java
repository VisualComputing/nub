package ik.trik;

import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.ik.loader.collada.data.Model;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.animation.Joint;
import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class URDFTest extends PApplet {
  Scene scene;
  String path = "C:\\Users\\olgaa\\Desktop\\Sebas\\Thesis\\robot_models\\collada-robots-collection-master\\collada\\";
  Model model;

  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    File folder = new File(path);
    File[] listOfFiles = folder.listFiles();
    int file = 0;
    System.out.println(file + " Robot Name: " + listOfFiles[file].getName());


    Joint.axes = true;
    //Joint.markers = true;
    randomSeed(14);
    this.g.textureMode(NORMAL);
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);

    readChainFromString(panda(), scene, 5);

    scene.eye().rotate(new Quaternion(new Vector(1, 0, 0), PI / 2));
    scene.eye().rotate(new Quaternion(new Vector(0, 0, 1), PI));
    scene.fit();

    //model.printNames();
    if (file < -1) {
      Target target = new Target(scene, ((Joint) model.root()).radius());
      /*Chain solver*/

      List<Node> branch = Node.path(model.skeleton().get(0), model.skeleton().get(file == 0 ? "node10" : "node8"));

      ChainSolver solver = new ChainSolver(branch);
      solver.setKeepDirection(true);
      solver.setFixTwisting(true);

      solver.setTimesPerFrame(5);
      solver.setMaxIterations(50);
      solver.setMaxError(scene.radius() * 0.001f);
      solver.setMinDistance(scene.radius() * 0.001f);
      solver.setTarget(branch.get(branch.size() - 1), target);
      target.setPosition(branch.get(branch.size() - 1).position().get());
      TimingTask task = new TimingTask() {
        @Override
        public void execute() {
          solver.solve();
        }
      };
      task.run(40);
    }
  }

  public void draw() {
    background(0);
    lights();
    scene.drawAxes();
    scene.render();
    scene.beginHUD();
        /*for (String s : model.skeleton().keySet()) {
            Node n = model.skeleton().get(s);
            Vector sc = scene.screenLocation(new Vector(), n);
            text(s, sc.x(), sc.y());
        }*/
    scene.endHUD();
  }

  @Override
  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      scene.mouseSpin();
    } else if (mouseButton == RIGHT) {
      scene.mouseTranslate();
    } else {
      scene.scale(scene.mouseDX());
    }
  }


  /**
   * This method is useful to transform a DH parameter into a Node
   */
  public Joint DHToNode(Scene scene, Node reference, float radius, float theta, float r, float d, float alpha) {
    float ct = cos(theta);
    float st = sin(theta);
    float ca = cos(alpha);
    float sa = sin(alpha);

    Matrix mat = new Matrix(ct, -st * ca, st * sa, r * ct,
        st, ct * ca, -ct * sa, r * st,
        0, sa, ca, d,
        0, 0, 0, 1, false);

    Joint child = new Joint(radius);
    child.setReference(reference);
    child.fromMatrix(mat);
    return child;
  }

  public void readChainFromString(String info, Scene scene, float radius) {
    List<Joint> skeleton = new ArrayList<Joint>();
    Node prev = null;
    String[] lines = info.split("\n");

    for (String line : lines) {
      String[] data = line.split(";");
      float theta = Float.valueOf(data[0]);
      float r = Float.valueOf(data[1]);
      float d = Float.valueOf(data[2]);
      float alpha = Float.valueOf(data[3]);
      float min_x = -Float.valueOf(data[4]);
      float max_x = Float.valueOf(data[5]);
      Joint j = DHToNode(scene, prev, radius, theta, r, d, alpha);
      Hinge hinge = new Hinge(min_x, max_x, j.rotation().get(), new Vector(1, 0, 0), new Vector(0, 0, 1));
      j.setConstraint(hinge);
      skeleton.add(j);
      prev = j;
    }
    skeleton.get(0).setRoot(true);
  }

  public String panda() {
    String s =
        "0;0;33.3;0;-2.8973;2.8973\n" +
            "0;0;0;-1.570796327;-1.7628;1.7628\n" +
            "0;0;31.6;1.570796327;-2.8973;2.8973\n" +
            "0;8.25;0;1.570796327;-3.0718;0\n" +
            "0;-8.25;38.4;-1.570796327;-2.8973;2.8973\n" +
            "0;0;0;1.570796327;0;3.7525\n" +
            "0;8.8;0;1.570796327;-2.8973;2.8973\n" +
            "0;0;10.7;0;0;0";
    return s;
  }


  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.trik.URDFTest"});
  }
}
