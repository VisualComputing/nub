package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class ViewFrustumCulling extends PApplet {
  OctreeNode root;
  Scene mainScene, secondaryScene, focus;

  int w = 700;
  int h = 700;
  //octree
  float a = 220, b = 100, c = 280;
  int levels = 4;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    // main scene
    mainScene = new Scene(createGraphics(w, h / 2, P3D));
    mainScene.enableHint(Scene.BACKGROUND, color(255));
    mainScene.eye().enableHint(Node.FRUSTUM);
    mainScene.togglePerspective();
    mainScene.fit(1);
    // secondary scene
    secondaryScene = new Scene(createGraphics(w, h / 2, P3D));
    secondaryScene.enableHint(Scene.BACKGROUND, color(185));
    secondaryScene.togglePerspective();
    secondaryScene.setRadius(200);
    secondaryScene.fit();
    // declare and build the octree hierarchy
    root = new OctreeNode();
    buildOctree(root);
  }

  public void buildOctree(OctreeNode parent) {
    if (parent.level() < levels)
      for (int i = 0; i < 8; ++i)
        buildOctree(new OctreeNode(parent, new Vector((i & 4) == 0 ? a : -a, (i & 2) == 0 ? b : -b, (i & 1) == 0 ? c : -c)));
  }

  public void draw() {
    focus = mainScene.hasMouseFocus() ? mainScene : secondaryScene;
    // culling condition should be retested every frame
    root.cull = false;
    mainScene.display();
    secondaryScene.display(0, h / 2);
  }

  void handleMouse() {
    focus = mouseY < h / 2 ? mainScene : secondaryScene;
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.mouseSpinEye();
    else if (mouseButton == RIGHT)
      focus.mouseTranslateEye();
    else
      focus.scaleEye(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    focus.moveForward(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focusEye();
      else
        focus.alignEye();
  }

  public void keyPressed() {
    if (key == ' ')
      focus.togglePerspective();
    if (key == 'f') {
      Scene.leftHanded = !Scene.leftHanded;
    }
  }

  class OctreeNode extends Node {
    OctreeNode() {
      tagging = false;
      setShape(this::draw);
      setVisit(mainScene, this::culling);
    }

    OctreeNode(OctreeNode node, Vector vector) {
      super(node);
      scale(0.5f);
      translate(Vector.multiply(vector, scaling() / 2));
      tagging = false;
      setShape(this::draw);
      setVisit(mainScene, this::culling);
    }

    float level() {
      return 1 - log(magnitude()) / log(2);
    }

    public void draw(PGraphics pg) {
      float level = level();
      pg.stroke(color(0.3f * level * 255, 0.2f * 255, (1 - 0.3f * level) * 255));
      pg.strokeWeight(pow(2, levels - 1));
      pg.noFill();
      pg.box(a, b, c);
    }

    // The culling method is called just before the graphics(PGraphics) method
    public void culling(Graph graph, Node node) {
      switch (graph.boxVisibility(node.worldLocation(new Vector(-a / 2, -b / 2, -c / 2)),
              node.worldLocation(new Vector(a / 2, b / 2, c / 2)))) {
        case VISIBLE:
          for (Node child : node.children())
            child.cull = true;
          break;
        case SEMIVISIBLE:
          if (!node.children().isEmpty()) {
            // don't render the node...
            node.bypass();
            // ... but don't cull its children either
            for (Node child : node.children())
              child.cull = false;
          }
          break;
        case INVISIBLE:
          node.cull = true;
          break;
      }
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ViewFrustumCulling"});
  }
}
