package frame;

import common.InteractiveNode;
import processing.core.PApplet;
import proscene.core.Graph;
import proscene.core.Node;
import proscene.input.Shortcut;
import proscene.input.event.MotionEvent;
import proscene.primitives.Vector;
import proscene.processing.Scene;

/**
 * Created by pierre on 11/15/16.
 */
public class CajasOrientadas extends PApplet {
  Scene graph;
  Box[] cajas;
  Sphere esfera;
  Node eye1, eye2;

  public void settings() {
    size(640, 360, P3D);
  }

  public void info() {
    println(graph.radius());
    graph.center().print();
    println(graph.fieldOfView());
    println(graph.zNearCoefficient());
    println(graph.zClippingCoefficient());
    println(graph.zNear());
    println(graph.zFar());
    graph.matrixHandler().projection().print();
    graph.matrixHandler().view().print();
    graph.matrixHandler().modelView().print();
  }

  public void setup() {
    graph = new Scene(this);
    graph.setRadius(200);
    graph.fitBall();
    graph.setType(Graph.Type.ORTHOGRAPHIC);
    esfera = new Sphere(graph);
    esfera.setPosition(new Vector(0.0f, 1.4f, 0.0f));
    esfera.setColor(color(0, 0, 255));

    cajas = new Box[30];
    for (int i = 0; i < cajas.length; i++)
      cajas[i] = new Box(graph);

    eye1 = new InteractiveNode(graph);

    eye2 = new Node(graph) {
      Shortcut left = new Shortcut(PApplet.LEFT);
      Shortcut right = new Shortcut(PApplet.RIGHT);
      Shortcut wheel = new Shortcut(processing.event.MouseEvent.WHEEL);

      @Override
      public void interact(MotionEvent event) {
        if (event.shortcut().matches(left))
          translate(event);
        else if (event.shortcut().matches(right))
          rotate(event);
        else if (event.shortcut().matches(wheel))
          if (isEye() && graph().is3D())
            translateZ(event);
          else
            scale(event);
      }
    };

    graph.setEye(eye1);
    graph.setFieldOfView((float) Math.PI / 3);
    graph.setDefaultNode(eye1);
    graph.fitBall();
    println(graph.inputHandler().hasGrabber(eye1) ? "has eye1" : "has NOT eye1");

    if (graph.is3D())
      println("Scene is 3D");
    else
      println("Scene is 2D");
    //graph.lookAt(new Vector());
    //info();
  }

  public void draw() {
    background(0);

    esfera.draw(false);
    for (int i = 0; i < cajas.length; i++) {
      cajas[i].setOrientation(esfera.getPosition());
      cajas[i].draw(true);
    }
  }

  public void keyPressed() {
    if (key == 'e') {
      graph.setType(Graph.Type.ORTHOGRAPHIC);
    }
    if (key == 'E') {
      graph.setType(Graph.Type.PERSPECTIVE);
    }
    if (key == ' ') {
      if (eye1 == graph.eye()) {
        graph.setEye(eye2);
        graph.setFieldOfView(1);
        graph.setDefaultNode(eye2);
        //graph.fitBall();
        println("Eye2 set " + graph.fieldOfView());
      } else {
        graph.setEye(eye1);
        graph.setFieldOfView((float) Math.PI / 4);
        graph.setDefaultNode(eye1);
        //graph.fitBall();
        println("Eye1 set " + graph.fieldOfView());
      }
    }
    if (key == 's')
      graph.fitBallInterpolation();
    if (key == 'S')
      graph.fitBall();
    if (key == 'u')
      graph.shiftDefaultNode((Node) graph.eye(), esfera.iFrame);
    if (key == 't')
      info();
    if (key == 'v') {
      println(Vector.scalarProjection(Vector.subtract(graph.eye().position(), graph.center()), graph.eye().zAxis()));
      Vector.projectVectorOnAxis(Vector.subtract(graph.eye().position(), graph.center()), graph.eye().zAxis()).print();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.CajasOrientadas"});
  }
}
