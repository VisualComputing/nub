package intellij;

import nub.core.Graph;
import nub.core.MatrixHandler;
import nub.core.Node;
import nub.primitives.Point;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class PassiveTransformations extends PApplet {
  Graph graph;
  Node[] nodes;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    graph = new Graph(g, width, height);
    graph.setMatrixHandler(new GLSLMatrixHandler());
    graph.setFOV(PI / 3);
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
      nodes[i].setPickingThreshold(20);
    }
    //discard Processing matrices
    resetMatrix();
  }

  public void draw() {
    background(0);
    graph.preDraw();
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

  public class GLSLMatrixHandler extends MatrixHandler {
    PShader _shader;

    public GLSLMatrixHandler() {
      _shader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/matrix_handler/fragment.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/matrix_handler/vertex.glsl");
    }

    @Override
    protected void _setUniforms() {
      shader(_shader);
      Scene.setUniform(_shader, "nub_transform", transform());
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.PassiveTransformations"});
  }
}
