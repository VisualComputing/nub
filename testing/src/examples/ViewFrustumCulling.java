package examples;

import nub.core.Scene;
import nub.core.Node;
import nub.primitives.Vector;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

  public class ViewFrustumCulling extends PApplet {
    OctreeNode root;
    Scene mainScene, secondaryScene, focus;
    Node mainEye, secondaryEye;

    int w = 1200;
    int h = 1600;
    //octree
    float a = 220, b = 100, c = 280;
    int levels = 4;

    public void settings() {
      size(w, h, P3D);
    }

    public void setup() {
      // main scene
      mainScene = new Scene(createGraphics(w, h / 2, P3D));
      mainEye = new Node();
      mainEye.setWorldPosition(0,0,300);
      mainScene.setEye(mainEye);
      mainEye.enableHint(Node.BOUNDS);
      // secondary scene
      secondaryScene = new Scene(createGraphics(w, h / 2, P3D));
      secondaryScene.radius = 800;
      secondaryEye = new Node();
      secondaryEye.setWorldPosition(0,0,1800);
      secondaryScene.setEye(secondaryEye);
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
      focus = mainScene.hasFocus() ? mainScene : secondaryScene;
      // culling condition should be retested every frame
      root.cull = false;
      //PGraphics pg1 = mainScene.context();
      //pg1.ortho(-400, 400, -200, 200, 10, 500);
      /*
      float fov = PI/3.0f;
      float cameraZ = (height/2.0f) / tan(fov/2.0f);
      pg1.perspective(fov, (float)width/(float)height, cameraZ/10.0f, cameraZ*2.0f);
      // */
      mainScene.openContext();
      mainScene.context().background(255);
      mainScene.render();
      mainScene.closeContext();
      mainScene.image();
      //PGraphics pg2 = secondaryScene.context();
      //pg2.ortho(-400, 400, -200, 200, 10,2000);
      secondaryScene.openContext();
      secondaryScene.context().background(185);
      secondaryScene.render();
      secondaryScene.closeContext();
      secondaryScene.image(0, h / 2);
    }

    void handleMouse() {
      focus = mouseY < h / 2 ? mainScene : secondaryScene;
    }

    public void mouseDragged() {
      if (mouseButton == LEFT)
        focus.spin();
      else if (mouseButton == RIGHT)
        focus.shift();
      else
        focus.zoom(mouseX - pmouseX);
    }

    public void mouseClicked(MouseEvent event) {
      if (event.getCount() == 2)
        if (event.getButton() == LEFT)
          focus.focus();
        else
          focus.align();
    }

    public void mouseWheel(MouseEvent event) {
      focus.zoom(event.getCount() * 5);
    }

    class OctreeNode extends Node {
      OctreeNode() {
        tagging = false;
        setShape(this::draw);
        setBehavior(mainScene, this::culling);
      }

      OctreeNode(OctreeNode node, Vector vector) {
        super(node);
        scale(0.5f);
        translate(Vector.multiply(vector, magnitude() / 2));
        tagging = false;
        setShape(this::draw);
        setBehavior(mainScene, this::culling);
      }

      float level() {
        return 1 - log(worldMagnitude()) / log(2);
      }

      public void draw(PGraphics pg) {
        float level = level();
        pg.stroke(color(0.3f * level * 255, 0.2f * 255, (1 - 0.3f * level) * 255));
        pg.strokeWeight(pow(2, levels - 1));
        pg.noFill();
        pg.box(a, b, c);
      }

      // The culling method is called just before the graphics(PGraphics) method
      public void culling(Scene graph, Node node) {
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

  public static void main(String[] args) {
    PApplet.main(new String[]{"male.ViewFrustumCulling"});
  }
}
