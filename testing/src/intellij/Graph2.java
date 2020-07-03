package intellij;

import nub.core.Graph;
import nub.core.MatrixHandler;
import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.event.MouseEvent;
import processing.opengl.PShader;

/**
 * Created by pierre on 11/15/16.
 */
public class Graph2 extends PApplet {
  Graph graph;
  Node[] nodes;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    graph = new Graph(g, width, height);
    GLSLMatrixHandler mh = new GLSLMatrixHandler();
    // TODO how to handle this
    //graph._setMatrixHandler(mh);
    graph.fit(1);
    nodes = new Node[50];
    for (int i = 0; i < nodes.length; i++) {
      nodes[i] = new Node() {
        @Override
        public void visit() {
          pushStyle();
          fill(isTagged(graph) ? 0 : 255, 0, 255);
          box(5);
          popStyle();
        }
      };
      nodes[i].setBullsEyeSize(.05f);
      graph.randomize(nodes[i]);
    }
    //discard Processing matrices
    resetMatrix();
  }

  public void draw() {
    background(0);
    // TODO needs testing
    // graph.preDraw();
    pushStyle();
    fill(0, 255, 0);
    Scene.drawTorusSolenoid(g);
    popStyle();
    graph.render();
  }

  public void mouseMoved() {
    graph.updateTag(mouseX, mouseY, nodes);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      graph.spin(pmouseX, pmouseY, mouseX, mouseY);
    else if (mouseButton == RIGHT)
      graph.translate(mouseX - pmouseX, mouseY - pmouseY, 0, 0);
    else
      graph.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    graph.scale(event.getCount() * 20);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.Graph2"});
  }

  public class GLSLMatrixHandler extends MatrixHandler {
    PShader framesShader;
    PMatrix3D pmatrix = new PMatrix3D();

    public GLSLMatrixHandler() {
      framesShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/matrix_handler/fragment.glsl", "/home/pierre/IdeaProjects/nub/testing/data/matrix_handler/vertex.glsl");
    }

    @Override
    protected void _setUniforms() {
      shader(framesShader);
      // same as:
      //pmatrix.set(Scene.toPMatrix(transform()));
      //pmatrix.transpose();
      pmatrix.set(transform().get(new float[16]));
      framesShader.set("nub_transform", pmatrix);
    }
  }
}
