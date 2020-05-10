package ik.animation;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Node;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.visualization.*;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class CCDAnim extends PApplet {
  /*
   * This example shows how to visualize an IK  Algorithm step by step
   * */

  /*
  (*) Define a Visualizer to your algorithm.
  Here, you must specify the way that each interesting event annotated on the solver will be mapped to a visual step
   */
  class CCDVisualizer extends Visualizer {
    public CCDVisualizer(Scene scene, float radius, long period, long stepDuration) {
      super(scene, radius, period, stepDuration);
    }

    /*
    Override this method is critical, Although EventToViz contains a default way to map an Interesting event into a
    visual step, there could be times in which you require to generate your own mapping
     */
    @Override
    public VisualStep eventToVizMapping(InterestingEvent event) {
      return EventToViz.generateDefaultViz(this, event, setVisualFeatures(event));
    }

    /*
     * Use this method to customize the aspect of a visual step, take into account the Type of the Events or their ids
     * */
    @Override
    public HashMap<String, Object> setVisualFeatures(InterestingEvent event) {
      //In this basic example we customize the aspect of the rotation step:
      HashMap<String, Object> attribs = null;
      if (event.type() == "NodeRotation") {
        attribs = new HashMap<String, Object>();
        attribs.put("color", color(0, 255, 0));
      }
      return attribs;
    }
  }


  Scene scene, auxiliar, focus;
  boolean displayAuxiliar = false, anim = false;

  int numJoints = 7;
  float targetRadius = 7;
  float boneLength = 50;

  CCDSolver solver; //IK Algorithm that uses Solver template
  CCDVisualizer visualizer; //A Visualizer manages a scene in which the IK algorithm will be animated
  VisualizerMediator mediator; //Since the interaction between a solver and a Visualizer is bidirectional a mediator is required to handle the events

  ArrayList<Node> structure = new ArrayList<>(); //Keep Structures
  Node target; //Keep targets
  boolean solve = false;


  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(numJoints * boneLength * 0.8f);
    scene.fit(1);
    scene.setRightHanded();

    //(*) The auxiliar Scene is where the animation will take place
    auxiliar = new Scene(this, P3D, width, height, 0, 0);
    auxiliar.setType(Graph.Type.ORTHOGRAPHIC);
    auxiliar.setRadius(numJoints * boneLength);
    auxiliar.setRightHanded();
    auxiliar.fit();

    PShape redBall = createShape(SPHERE, targetRadius);
    redBall.setStroke(false);
    redBall.setFill(color(255, 0, 0));

    //create targets
    target = new Node(redBall);
    target.setPickingThreshold(0);

    //create skeleton
    structure = Util.generateAttachedChain(numJoints, targetRadius * 0.8f, boneLength, new Vector(), 212, 0, 255);

    solver = new CCDSolver(structure);
    solver.enableMediator(true);

    //Create the visualizer
    visualizer = new CCDVisualizer(auxiliar, targetRadius * 0.8f, 40, 480);

    //Create Mediator that relates a Solver with at least one visualizer
    mediator = new VisualizerMediator(solver, visualizer);

    solver.setMaxError(0.001f);
    solver.setTimesPerFrame(20);
    solver.setMaxIterations(200);
    //7. Set targets
    solver.setTarget(structure.get(numJoints - 1), target);
    target.setPosition(structure.get(numJoints - 1).position());

    //Defines a task to run the solver each 40 ms
    TimingTask task = new TimingTask() {
      @Override
      public void execute() {
        if (solve) {
          solver.solve();
        }
      }
    };
    task.run(40);

    //Defines a task to run the animation each 40 ms
    TimingTask animTask = new TimingTask() {
      @Override
      public void execute() {
        if (anim) {
          visualizer.execute();
        }
      }
    };
    animTask.run(visualizer.period());
    //Define Text Font
    textFont(createFont("Zapfino", 38));
  }

  public void draw() {
    focus = displayAuxiliar ? auxiliar : scene;
    background(0);
    lights();
    scene.drawAxes();
    scene.render();
    scene.beginHUD();
    Util.printInfo(scene, solver, structure.get(0).position());

    if (displayAuxiliar) {
      auxiliar.beginDraw();
      auxiliar.context().lights();
      auxiliar.context().background(0);
      auxiliar.drawAxes();
      auxiliar.render();
      visualizer.render();
      auxiliar.endDraw();
      auxiliar.display();
    }
    scene.endHUD();

  }


  @Override
  public void mouseMoved() {
    focus.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      focus.mouseSpin();
    } else if (mouseButton == RIGHT) {
      focus.mouseTranslate();
    } else {
      focus.scale(mouseX - pmouseX);
    }
  }

  public void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  public void keyPressed() {
    if (key == 'w' || key == 'W') {
      solve = !solve;
    } else if (key == 'q') {
      displayAuxiliar = true;
      solver.solve();
      anim = true;
    } else if (key == ' ') {
      displayAuxiliar = !displayAuxiliar;
    } else if (key == 's') {
      solver.solve();
    } else if (Character.isDigit(key)) {
      if (visualizer != null) visualizer.setStepDuration(Integer.valueOf("" + key) * 1000);
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.animation.CCDAnim"});
  }
}
