package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Matrix;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.event.MouseEvent;
import processing.opengl.PShader;

/**
 * Created by pierre on 11/15/16.
 */
public class Graph3 extends PApplet {
  Graph graph;
  PShader framesShader;
  Matrix projection, view, pmv;
  PMatrix3D pmatrix = new PMatrix3D();
  Node[] nodes;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    graph = new Graph(g, width, height);
    graph.fit();
    framesShader = loadShader("/home/pierre/IdeaProjects/nodes/testing/data/matrix_handler/fragment.glsl", "/home/pierre/IdeaProjects/nodes/testing/data/matrix_handler/vertex.glsl");
    nodes = new Node[50];
    for (int i = 0; i < nodes.length; i++) {
      nodes[i] = new Node(graph) {
        @Override
        public void visit() {
          shader(framesShader);
          //pmv = Matrix.multiply(graph.projection(), graph.modelView());
          //pmv = Matrix.multiply(graph.projection(), this.worldMatrix());

          Matrix mv = Matrix.multiply(view, worldMatrix());
          pmv = Matrix.multiply(projection, mv);
          pmatrix.set(pmv.get(new float[16]));
          framesShader.set("nub_transform", pmatrix);

          pushStyle();
          fill(isTagged(graph) ? 0 : 255, 0, 255);
          box(5);
          popStyle();
        }
      };
      nodes[i].randomize();
    }
    //discard Processing matrices
    resetMatrix();
  }

  void updateMatrices() {
    projection = graph.eye().projection(graph.type(), graph.width(), graph.height(), graph.zNear(), graph.zFar(), graph.isLeftHanded());
    view = graph.eye().view();

    shader(framesShader);
    pmv = Matrix.multiply(projection, view);
    pmatrix.set(pmv.get(new float[16]));
    framesShader.set("nub_transform", pmatrix);
  }

  public void draw() {
    //graph.preDraw();
    // can't pick because the matrixHandler projectionView is not updated
    updateMatrices();
    background(125);
    graph.render();
  }

  public void mouseMoved() {
    graph.track(mouseX, mouseY, nodes);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      graph.spin(pmouseX, pmouseY, mouseX, mouseY);
    else if (mouseButton == RIGHT)
      graph.translate(mouseX - pmouseX, mouseY - pmouseY);
    else
      graph.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    graph.scale(event.getCount() * 20);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.Graph3"});
  }
}
