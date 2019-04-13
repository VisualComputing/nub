package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Point;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.event.MouseEvent;
import processing.opengl.PShader;

/**
 * Created by pierre on 11/15/16.
 */
public class Graph4 extends PApplet {
  Graph graph;
  PShader framesShader;
  Matrix pmv;
  PMatrix3D pmatrix = new PMatrix3D();
  Node[] nodes;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    graph = new Graph(width, height) {
      // Note that within visit() geometry is defined
      // at the node local coordinate system.
      @Override
      public void applyTransformation(Node node) {
        super.applyTransformation(node);
        shader(framesShader);
        pmv = Matrix.multiply(matrixHandler().projection(), matrixHandler().matrix());
        pmatrix.set(pmv.get(new float[16]));
        framesShader.set("nub_transform", pmatrix);
      }
    };
    graph.fit(1);
    framesShader = loadShader("/home/pierre/IdeaProjects/nodes/testing/data/matrix_handler/fragment.glsl", "/home/pierre/IdeaProjects/nodes/testing/data/matrix_handler/vertex.glsl");
    nodes = new Node[50];
    for (int i = 0; i < nodes.length; i++) {
      nodes[i] = new Node(graph) {
        @Override
        public void visit() {
          pushStyle();
          fill(isTracked(graph) ? 0 : 255, 0, 255);
          box(5);
          popStyle();
        }
      };
      nodes[i].randomize();
    }
    //discard Processing matrices
    resetMatrix();
  }

  public void draw() {
    graph.preDraw();
    background(0);
    graph.render();
  }

  public void mouseMoved() {
    graph.track(mouseX, mouseY, nodes);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      graph.spin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY));
    else if (mouseButton == RIGHT)
      graph.translate(mouseX - pmouseX, mouseY - pmouseY);
    else
      graph.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    graph.scale(event.getCount() * 20);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.Graph4"});
  }
}
