package frame;

import frames.core.Graph;
import frames.processing.GLMatrixHandler;
import frames.processing.Mouse;
import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;

/**
 * This example just to show that a Graph is instantiable.
 * More to come...
 */
public class FrameInteraction extends PApplet {
  Graph graph;

  public void settings() {
    size(640, 360, P3D);
  }

  public void setup() {
    graph = new Graph(width, height);
    //TODO wanna go like this:
    //refer to the MatrixShader frames experiment
    //graph.setMatrixHandler(new MatrixHandler(graph));
    graph.setMatrixHandler(new GLMatrixHandler(graph, (PGraphicsOpenGL) g));

    Mouse mouse = new Mouse(graph);
    graph.inputHandler().registerAgent(mouse);
    registerMethod("mouseEvent", mouse);

    // 3. Create agents and register P5 methods

    registerMethod("pre", this);
    //registerMethod("draw", this);
    //registerMethod("post", this);
    //registerMethod("dispose", this);

    graph.setRadius(200);
    graph.fitBallInterpolation();

    // 5. Handed
    graph.setLeftHanded();
  }

  public void proscenium() {
    background(100);
    box(50);
  }

  public void pre() {
    //println("pre");
    graph.preDraw();
    graph.pushModelView();
  }

  public void draw() {
    proscenium();
    graph.popModelView();
    //println("post");
    graph.postDraw();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.FrameInteraction"});
  }
}
