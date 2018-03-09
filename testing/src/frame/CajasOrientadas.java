package frame;

import common.InteractiveNode;
import frames.core.Graph;
import frames.core.Node;
import frames.input.Shortcut;
import frames.input.event.MotionEvent;
import frames.primitives.Matrix;
import frames.primitives.Vector;
import frames.processing.Mouse;
import frames.processing.Scene;
import processing.core.PApplet;

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
    graph.matrixHandler().cacheView().print();
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
      @Override
      public void interact(MotionEvent event) {
        if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
          translate(event);
        else if (event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
          rotate(event);
        else if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
          if (isEye() && graph().is3D())
            translateZ(event);
          else
            scale(event);
      }
    };

    graph.setEye(eye1);
    graph.setFieldOfView((float) Math.PI / 3);
    graph.setDefaultGrabber(eye1);
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

    String text = "Hello world";
    float w = graph.frontBuffer().textWidth(text);
    float h = graph.frontBuffer().textAscent() + graph.frontBuffer().textDescent();
    graph.beginScreenCoordinates();
    //textFont(font);
    text(text, 20, 20, w + 1, h);
    graph.endScreenCoordinates();
  }

  public void keyPressed() {
    if (key == 'e') {
      graph.setType(Graph.Type.ORTHOGRAPHIC);
    }
    if (key == 'E') {
      graph.setType(Graph.Type.PERSPECTIVE);
    }
    if (key == ' ') {
      /*
      if (eye1 == graph.eye()) {
        graph.setEye(eye2);
        graph.setFieldOfView(1);
        graph.setDefaultGrabber(eye2);
        //graph.fitBall();
        println("Eye2 set " + graph.fieldOfView());
      } else {
        graph.setEye(eye1);
        graph.setFieldOfView((float) Math.PI / 4);
        graph.setDefaultGrabber(eye1);
        //graph.fitBall();
        println("Eye1 set " + graph.fieldOfView());
      }
      */

      /*
      Matrix m1 = graph.eye().worldMatrix();
      m1.print();

      Matrix m2 = graph.eye().matrix();
      m2.print();
      */

      Matrix m3 = graph.eye().matrix();
      m3.invert();
      m3.print();

      /*
      Frame frame = graph.eye().inverse();
      Matrix m4 = frame.matrix();
      m4.print();

      Frame frame2 = graph.eye().worldInverse();
      Matrix m5 = frame2.matrix();
      m5.print();
      */
    }
    if (key == 's')
      graph.fitBallInterpolation();
    if (key == 'S')
      graph.fitBall();
    if (key == 'u')
      graph.shiftDefaultGrabber((Node) graph.eye(), esfera.iFrame);
    if (key == 't')
      info();
    if (key == 'v') {
      println(Vector.scalarProjection(Vector.subtract(graph.eye().position(), graph.center()), graph.eye().zAxis()));
      Vector.projectVectorOnAxis(Vector.subtract(graph.eye().position(), graph.center()), graph.eye().zAxis()).print();
    }
    if (key == 'w')
      graph.fitFieldOfView();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.CajasOrientadas"});
  }
}
