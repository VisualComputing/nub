package intellij;

import nub.core.Graph;
import nub.core.Node;
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
    // TODO pending
    /*
    graph._setMatrixHandler(new MatrixHandler() {
      @Override
      protected void _setUniforms() {
        // TODO How to deal with this command. Seems related to: Scene._drawBackBuffer(Node node)
        shader(_shader);
        Scene.setUniform(_shader, "nub_transform", transform());
      }
    });
     */
    graph.setFOV(PI / 3);
    graph.fit(1);
    nodes = new Node[50];
    for (int i = 0; i < nodes.length; i++) {
      nodes[i] = new Node() {
        //TODO
        /*
        @Override
        public void visit() {
          pushStyle();
          fill(isTagged(graph) ? 0 : 255, 0, 255);
          box(5);
          popStyle();
        }
         */
      };
      graph.randomize(nodes[i]);
      nodes[i].setBullsEyeSize(20);
    }
    //discard Processing matrices
    resetMatrix();
    _shader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/matrix_handler/fragment.glsl", "/home/pierre/IdeaProjects/nub/testing/data/matrix_handler/vertex.glsl");
  }

  public void draw() {
    background(0);
    // TODO needs testing
    //graph.preDraw();
    graph.render();
  }

  public void mouseMoved() {
    graph.updateTag(mouseX, mouseY, nodes);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      graph.spin(pmouseX, pmouseY, mouseX, mouseY);
    else if (mouseButton == RIGHT)
      graph.translate(mouseX - pmouseX, mouseY - pmouseY, 0);
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
