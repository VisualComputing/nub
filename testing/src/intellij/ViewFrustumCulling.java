package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class ViewFrustumCulling extends PApplet {
  OctreeNode root;
  Scene mainScene, secondaryScene, focus;
  boolean bypass;

  int w = 1000;
  int h = 800;
  //octree
  float a = 220, b = 100, c = 280;
  int levels = 4;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    // main scene
    mainScene = new Scene(this, P3D, w, h / 2);
    mainScene.togglePerspective();
    mainScene.enableBoundaryEquations();
    mainScene.fit(1);

    // declare and build the octree hierarchy
    root = new OctreeNode();
    buildOctree(root);

    // secondary scene
    secondaryScene = new Scene(this, P3D, w, h / 2);
    secondaryScene.togglePerspective();
    secondaryScene.setRadius(200);
    secondaryScene.fit();
  }

  public void buildOctree(OctreeNode parent) {
    if (parent.level() < levels)
      for (int i = 0; i < 8; ++i)
        buildOctree(new OctreeNode(parent, new Vector((i & 4) == 0 ? a : -a, (i & 2) == 0 ? b : -b, (i & 1) == 0 ? c : -c)));
  }

  public void draw() {
    handleMouse();
    background(255);
    mainScene.beginDraw();
    mainScene.context().background(255);
    // culling condition should be retested every frame
    root.cull(false);
    bypass = false;
    mainScene.render();
    mainScene.endDraw();
    mainScene.display();
    bypass = true;
    secondaryScene.beginDraw();
    secondaryScene.context().background(185);
    secondaryScene.render();
    secondaryScene.context().pushStyle();
    secondaryScene.context().strokeWeight(2);
    secondaryScene.context().stroke(255, 0, 255);
    secondaryScene.context().fill(255, 0, 255, 160);
    secondaryScene.drawFrustum(mainScene);
    secondaryScene.context().popStyle();
    secondaryScene.endDraw();
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
      mainScene.flip();
      secondaryScene.flip();
    }
  }

  class OctreeNode extends Node {
    OctreeNode() {
      disableTagging();
    }

    OctreeNode(OctreeNode node, Vector vector) {
      super(node);
      scale(0.5f);
      translate(Vector.multiply(vector, scaling() / 2));
      disableTagging();
    }

    float level() {
      return 1 - log(magnitude()) / log(2);
    }

    @Override
    public void graphics(PGraphics pg) {
      float level = level();
      pg.stroke(color(0.3f * level * 255, 0.2f * 255, (1 - 0.3f * level) * 255));
      pg.strokeWeight(pow(2, levels - 1));
      pg.noFill();
      pg.box(a, b, c);
    }

    // The visit() method is called just before the graphics(PGraphics) method
    @Override
    public void visit() {
      // cull only against main scene
      if (bypass)
        return;
      switch (mainScene.boxVisibility(worldLocation(new Vector(-a / 2, -b / 2, -c / 2)),
          worldLocation(new Vector(a / 2, b / 2, c / 2)))) {
        case VISIBLE:
          for (Node node : children())
            node.cull();
          break;
        case SEMIVISIBLE:
          if (!children().isEmpty()) {
            // don't render the node...
            bypass();
            // ... but don't cull its children either
            for (Node node : children())
              node.cull(false);
          }
          break;
        case INVISIBLE:
          cull();
          break;
      }
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ViewFrustumCulling"});
  }
}
