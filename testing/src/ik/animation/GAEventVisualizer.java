package ik.animation;

import ik.animation.eventVisualizer.Board;
import ik.animation.eventVisualizer.EventCell;
import ik.animation.eventVisualizer.Slot;
import ik.basic.Util;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.GASolver;
import nub.ik.visualization.*;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class GAEventVisualizer extends PApplet {
  class GAVisualizer extends Visualizer {
    public GAVisualizer(Scene scene, float radius, long period, long stepDuration) {
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
      if (event.type() == "HighlightStructure" || event.type() == "HideStructure") {
        attribs = new HashMap<String, Object>();
        if (event.name().toLowerCase().contains("best")) {
          attribs.put("highlight", color(0, 255, 0));
        } else if (event.name().toLowerCase().contains("parent")) {
          attribs.put("highlight", color(0, 0, 255));
        } else if (event.name().toLowerCase().contains("child")) {
          attribs.put("highlight", color(255, 0, 0));
        }
      }
      return attribs;
    }
  }


  int rows = 10, cols = 50;
  Scene eventScene, animScene, ikScene;
  Board board;

  //Define the Solver attributes
  int numJoints = 7;
  float targetRadius = 7;
  float boneLength = 50;
  GASolver solver; //IK Algorithm that uses Solver template
  GAVisualizer visualizer; //A Visualizer manages a scene in which the IK algorithm will be animated
  VisualizerMediator mediator; //Since the interaction between a solver and a Visualizer is bidirectional a mediator is required to handle the events
  ArrayList<Node> structure = new ArrayList<>(); //Keep Structures


  public void settings() {
    size(1200, 800, P2D);
  }

  public void setup() {
    //Set Kinematic scene
    ikScene = new Scene(this, P3D, width, height, 0, 0);
    //create target
    Node target = new Node();
    structure = Util.generateAttachedChain(numJoints, targetRadius * 0.8f, boneLength, new Vector(), 255, 255, 255);
    solver = new GASolver(structure, 10);
    solver.enableMediator(true);
    //Do only on iteration as we care just about plain events
    solver.setTimesPerFrame(1);
    solver.setMaxIterations(1);
    solver.setTarget(structure.get(numJoints - 1), target);
    //Move the target to any position
    target.setPosition(Vector.multiply(Vector.random(), random(10, numJoints * boneLength)));

    //set anim scene
    animScene = new Scene(this, P3D, width, height, 0, 0);
    //Create the visualizer
    visualizer = new GAVisualizer(animScene, targetRadius * 0.8f, 40, 480);
    //Create Mediator that relates a Solver with at least one visualizer
    mediator = new VisualizerMediator(solver);

    //Set the event scene (only one that cares visually)
    eventScene = new Scene(this);
    eventScene.setRadius(height / 2.f);
    eventScene.fit();
    //Setting the board
    board = new Board(eventScene, rows, cols);
    float rh = eventScene.radius(), rw = rh * eventScene.aspectRatio();
    board.setDimension(-rw, -rh, 10 * rw, 2 * rh);

    //set eye constraint
    eventScene.eye().setConstraint(new Constraint() {
      @Override
      public Vector constrainTranslation(Vector translation, Node node) {
        Vector v = Vector.add(node.translation(), translation);
        if (v.x() < -5) v.setX(-5);
        if (v.y() < -5) v.setY(-5);

        return Vector.subtract(v, node.translation());
      }

      @Override
      public Quaternion constrainRotation(Quaternion rotation, Node node) {
        return new Quaternion(); //no rotation is allowed
      }
    });

    //Adding some cells
    addEventsToBoard(solver, mediator, board);
  }

  public void addEventsToBoard(Solver solver, VisualizerMediator mediator, Board board) {
    solver.solve(); //solve once
    //find and use events
    Set<String> names = new HashSet<String>();
    for (InterestingEvent event : mediator.eventQueue()) {
      if (!names.contains(event.name())) {
        new EventCell(board, event.name(), event.startingTime(), event.executionDuration(), event.renderingDuration());
        names.add(event.name());
      }
    }
  }


  public void draw() {
    background(0);
    eventScene.render();
  }

  public void keyPressed() {
    if (key == 'r') {
      board.addRows(1, true);
    } else if (key == 'c') {
      board.addCols(1, true);
    }

  }


  public void mouseMoved() {
    eventScene.mouseTag();
  }

  public void mouseDragged() {
    if (eventScene.node() instanceof Slot) {
      eventScene.node().interact("OnMovement", new Vector(eventScene.mouseX(), eventScene.mouseY()));
    } else {
      eventScene.mouseTranslate();
    }
  }

  public void mouseReleased() {
    if (eventScene.node() instanceof EventCell) {
      //we must align the event to the closest row / col
      ((EventCell) eventScene.node()).applyMovement();
    } else if (eventScene.node() instanceof Slot) {
      eventScene.node().interact("OnFinishedMovement", new Vector(eventScene.mouseX(), eventScene.mouseY()));
    }
  }

  public void mouseWheel(MouseEvent event) {
    if (eventScene.node() == null) eventScene.scale(event.getCount() * 50);
  }


  public static void main(String[] args) {
    PApplet.main(new String[]{"ik.animation.GAEventVisualizer"});
  }
}
