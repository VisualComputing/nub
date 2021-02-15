/**
 * View Frustum Culling.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates a basic view frustum culling implementation which
 * is performed by analytically solving the frustum plane equations.
 *
 * A customized traversal rendering algorithm is implemented by overriding the
 * node visit() method to clip an octree against the camera's viewing frustum.
 * A second viewer displays an external view of the main frustum scene (using
 * the drawFrustum(Scene otherScene) method) and the clipped octree.
 *
 * Press the space-bar to change the scene type: PERSPECTIVE or ORTHOGRAPHIC.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Node root;
Scene mainScene, secondaryScene, focus;

int w = 1400;
int h = 1400;
//octree
float a = 220, b = 100, c = 280;
int levels = 4;

void settings() {
  size(w, h, P3D);
}

void setup() {
  // main scene
  mainScene = new Scene(createGraphics(w, h / 2, P3D));
  mainScene.enableHint(Scene.BACKGROUND, color(255));
  mainScene.eye().enableHint(Node.BOUNDS);
  mainScene.togglePerspective();
  mainScene.fit(1);
  // secondary scene
  secondaryScene = new Scene(createGraphics(w, h / 2, P3D), 200);
  secondaryScene.enableHint(Scene.BACKGROUND, color(185));
  secondaryScene.togglePerspective();
  secondaryScene.fit();
  // declare and build the octree hierarchy
  root = node(null, new Vector());
  buildOctree(root);
}

void buildOctree(Node parent) {
  if (level(parent) < levels)
    for (int i = 0; i < 8; ++i)
      buildOctree(node(parent, new Vector((i & 4) == 0 ? a : -a, (i & 2) == 0 ? b : -b, (i & 1) == 0 ? c : -c)));
}

float level(Node node) {
  return 1 - log(node.magnitude()) / log(2);
}

Node node(Node parent, Vector vector) {
  Node node = new Node(parent);
  node.scale(parent == null ? 1 : 0.5);
  node.translate(Vector.multiply(vector, node.scaling() / 2));
  node.tagging = false;
  node.setShape((PGraphics pg) -> {
    float level = level(node);
    pg.stroke(color(0.3 * level * 255, 0.2 * 255, (1 - 0.3 * level) * 255));
    pg.strokeWeight(pow(2, levels - 1));
    pg.noFill();
    pg.box(a, b, c);
  });
  // register the visit method only at the main scene
  node.setVisit(mainScene, this::cull);
  return node;
}


// The culling method is called just before the graphics(PGraphics) method
public void cull(Graph graph, Node node) {
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

void draw() {
  focus = mainScene.hasMouseFocus() ? mainScene : secondaryScene;
  // culling condition should be retested every frame
  root.cull = false;
  mainScene.display();
  secondaryScene.display(0, h / 2);
}

void mouseDragged() {
  if (mouseButton == LEFT)
    focus.mouseSpinEye();
  else if (mouseButton == RIGHT)
    focus.mouseTranslateEye();
  else
    focus.scaleEye(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  focus.moveForward(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 2)
    if (event.getButton() == LEFT)
      focus.focusEye();
    else
      focus.alignEye();
}

void keyPressed() {
  if (key == ' ')
    focus.togglePerspective();
  if (key == 'f') {
    Scene.leftHanded = !Scene.leftHanded;
  }
}
