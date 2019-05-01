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
  PShader _shader;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    graph = new Graph(g, width, height);
    graph.setMatrixHandler(new MatrixHandler() {
      @Override
      protected void _setUniforms() {
        // TODO How to deal with this command. Seems related to: Scene._drawBackBuffer(Node node)
        shader(_shader);
        Scene.setUniform(_shader, "nub_transform", transform());
      }
    });
    graph.setFOV(PI / 3);
    graph.fit(1);
    nodes = new Node[50];
    for (int i = 0; i < nodes.length; i++) {
      nodes[i] = new Node(graph) {
        @Override
        public void visit() {
          pushStyle();
          fill(isTracked() ? 0 : 255, 0, 255);
          box(5);
          popStyle();
        }
      };
      nodes[i].randomize();
      nodes[i].setPickingThreshold(20);
    }
    //discard Processing matrices
    resetMatrix();
    _shader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/matrix_handler/fragment.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/matrix_handler/vertex.glsl");
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

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.PassiveTransformations"});
  }
}
