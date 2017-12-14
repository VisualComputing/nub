package frame;

import processing.core.PApplet;
import remixlab.input.event.*;
import remixlab.core.Graph;
import remixlab.core.Node;
import remixlab.primitives.Vector;
import remixlab.proscene.Scene;

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

    eye1 = new Node(graph) {
      @Override
      public void interact(MotionEvent event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            //zoomOnRegion(_event);
            rotate(event);
            break;
          case PApplet.CENTER:
            //rotate(_event);
            zoomOnRegion(event);
            break;
          case PApplet.RIGHT:
            translate(event);
            break;
          case processing.event.MouseEvent.WHEEL:
            //_scale(_event);
            translateZ(event);
            break;
        }
      }

      @Override
      public void interact(KeyEvent event) {
        if (event.id() == PApplet.UP)
          translateYPos();
        if (event.id() == PApplet.DOWN)
          translateYNeg();
        if (event.id() == PApplet.LEFT)
          translateXNeg();
        if (event.id() == PApplet.RIGHT)
          translateXPos();
      }
    };

    eye2 = new Node(graph) {
      @Override
      public void interact(MotionEvent event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            translate(event);
            break;
          case PApplet.RIGHT:
            rotate(event);
            break;
          case processing.event.MouseEvent.WHEEL:
            scale(event);
            break;
        }
      }

      @Override
      public void interact(KeyEvent event) {
        if (event.id() == PApplet.UP)
          translateYPos();
        if (event.id() == PApplet.DOWN)
          translateYNeg();
        if (event.id() == PApplet.LEFT)
          translateXNeg();
        if (event.id() == PApplet.RIGHT)
          translateXPos();
      }
    };

    graph.setEye(eye1);
    graph.setFieldOfView((float)Math.PI/3);
    graph.setDefaultNode(eye1);
    graph.fitBall();
    println(graph.inputHandler().hasGrabber(eye1) ? "has eye1" : "has NOT eye1");

    if(graph.is3D())
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
    if(key == 'e') {
      graph.setType(Graph.Type.ORTHOGRAPHIC);
    }
    if(key == 'E') {
      graph.setType(Graph.Type.PERSPECTIVE);
    }
    if(key == ' ') {
      if(eye1 == graph.eye()) {
        graph.setEye(eye2);
        graph.setFieldOfView(1);
        graph.setDefaultNode(eye2);
        //graph.fitBall();
        println("Eye2 set " + graph.fieldOfView());
      }
      else {
        graph.setEye(eye1);
        graph.setFieldOfView((float)Math.PI/4);
        graph.setDefaultNode(eye1);
        //graph.fitBall();
        println("Eye1 set " + graph.fieldOfView());
      }
    }
    if(key == 's')
      graph.fitBallInterpolation();
    if(key == 'S')
      graph.fitBall();
    if(key == 'u')
      graph.shiftDefaultNode((Node)graph.eye(), esfera.iFrame);
    if(key == 't')
      info();
    if(key == 'v') {
      println(Vector.scalarProjection(Vector.subtract(graph.eye().position(), graph.center()), graph.eye().zAxis()));
      Vector.projectVectorOnAxis(Vector.subtract(graph.eye().position(), graph.center()), graph.eye().zAxis()).print();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.CajasOrientadas"});
  }
}
