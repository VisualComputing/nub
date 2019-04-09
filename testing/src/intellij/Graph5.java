package intellij;

import nub.core.Graph;
import nub.core.MatrixHandler;
import nub.core.Node;
import nub.primitives.Point;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.event.MouseEvent;
import processing.opengl.PShader;

/**
 * Created by pierre on 11/15/16.
 */
public class Graph5 extends PApplet {
  Graph graph;
  Node[] nodes;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    graph = new Graph(width, height);
    GLSLMatrixHandler mh = new GLSLMatrixHandler(graph);
    graph.setMatrixHandler(mh);
    graph.fit(1);
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
    background(0);
    graph.preDraw();
    pushStyle();
    fill(0, 255, 0);
    Scene.drawTorusSolenoid(g);
    popStyle();
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
    PApplet.main(new String[]{"intellij.Graph5"});
  }

  public class GLSLMatrixHandler extends MatrixHandler {
    PShader framesShader;
    PMatrix3D pmatrix = new PMatrix3D();

    public GLSLMatrixHandler(Graph graph) {
      super(graph);
      framesShader = loadShader("/home/pierre/IdeaProjects/nodes/testing/data/matrix_handler/fragment.glsl", "/home/pierre/IdeaProjects/nodes/testing/data/matrix_handler/vertex.glsl");
    }

    @Override
    protected void _setUniforms() {
      shader(framesShader);
      // same as:
      //pmatrix.set(Scene.toPMatrix(projectionModelView()));
      //pmatrix.transpose();
      pmatrix.set(projectionModelView().get(new float[16]));
      framesShader.set("nub_transform", pmatrix);
    }
  }
}
