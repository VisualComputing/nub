package frame;

import processing.core.PApplet;
import remixlab.bias.event.*;
import remixlab.geom.Graph;
import remixlab.geom.Node;
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

  /*
  public boolean matches(Shortcut shortcut, List<Shortcut> list) {
    for(Shortcut s : list)
      if(s.matches(shortcut))
        return true;
    return false;
  }
  */

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
    graph.setGridVisualHint(true);
    //graph.setCameraType(Camera.Type.ORTHOGRAPHIC);
    graph.setRadius(200);
    //graph.camera().setPosition(new PVector(10,0,0));
    //graph.camera().lookAt( graph.center() );
    graph.fitBall();
    //graph.disableBackgroundHanddling();
    esfera = new Sphere(graph);
    esfera.setPosition(new Vector(0.0f, 1.4f, 0.0f));
    esfera.setColor(color(0, 0, 255));

    cajas = new Box[30];
    for (int i = 0; i < cajas.length; i++)
      cajas[i] = new Box(graph);

    //graph.keyAgent().setDefaultGrabber(null);

    //if(graph.keyAgent().defaultGrabber() == graph.frame())
      //println("is eyeFrame!");
    //frameRate(500);

    eye1 = new Node(graph) {
      @Override
      public void interact(MotionEvent event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            rotate(event);
            break;
          case PApplet.RIGHT:
            translate(event);
            break;
          case processing.event.MouseEvent.WHEEL:
            //scale(event);
            translateZ(event);
            break;
        }
      }

      @Override
      public void interact(KeyEvent event) {
        if (event.id() == PApplet.UP)
          translateY(true);
        if (event.id() == PApplet.DOWN)
          translateY(false);
        if (event.id() == PApplet.LEFT)
          translateX(false);
        if (event.id() == PApplet.RIGHT)
          translateX(true);
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
          translateY(true);
        if (event.id() == PApplet.DOWN)
          translateY(false);
        if (event.id() == PApplet.LEFT)
          translateX(false);
        if (event.id() == PApplet.RIGHT)
          translateX(true);
      }
    };

    graph.setEye(eye1);
    graph.setFieldOfView((float)Math.PI/3);
    graph.setDefaultNode(eye1);
    graph.fitBall();

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
      graph.fitBall();
    //TODO restore
    //if(key == ' ')
      //graph.keyAgent().shiftDefaultGrabber(graph.frame(), esfera.iFrame);
      //graph.keyAgent().shiftDefaultGrabber(graph.eyeFrame(), graph);
    //if(key ==' ')
      //info();
    if(key == 'a')
      graph.toggleAxesVisualHint();
    if(key == 'g')
      graph.toggleGridVisualHint();
    if(key == 'f')
      graph.togglePickingVisualhint();
    if(key == 'v') {
      println(Vector.scalarProjection(Vector.subtract(graph.eye().position(), graph.center()), graph.eye().zAxis()));
      Vector.projectVectorOnAxis(Vector.subtract(graph.eye().position(), graph.center()), graph.eye().zAxis()).print();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.CajasOrientadas"});
  }
}
