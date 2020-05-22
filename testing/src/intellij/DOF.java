package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class DOF extends PApplet {
  String depthPath;
  PShader depthShader, dofShader;
  PGraphics depthPGraphics, dofPGraphics;
  Scene scene;
  Node[] models;
  int mode = 2;
  boolean exact = true;

  @Override
  public void settings() {
    size(1000, 800, P3D);
  }

  @Override
  public void setup() {
    scene = new Scene(this, P3D, width, height);
    scene.setRadius(1000);
    scene.fit(1);
    models = new Node[100];
    for (int i = 0; i < models.length; i++) {
      models[i] = new Node(boxShape());
      models[i].setPickingThreshold(0);
      scene.randomize(models[i]);
    }

    // Depth shader
    // Test all the different versions
    //depthPath = "depth_frag.glsl";
    depthPath = "depth_linear.glsl";
    //depthPath = "depth_nonlinear.glsl";
    depthShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/depth/" + depthPath);
    depthPGraphics = createGraphics(width, height, P3D);
    depthPGraphics.shader(depthShader);

    // DOF shader
    dofShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/dof/dof.glsl");
    dofShader.set("aspect", width / (float) height);
    dofShader.set("maxBlur", (float) 0.015);
    dofShader.set("aperture", (float) 0.02);
    dofPGraphics = createGraphics(width, height, P3D);
    dofPGraphics.shader(dofShader);

    frameRate(1000);
  }

  @Override
  public void draw() {
    // 1. Draw into main buffer
    scene.beginDraw();
    scene.context().background(0);
    scene.render();
    scene.endDraw();

    // 2. Draw into depth buffer
    depthPGraphics.beginDraw();
    depthPGraphics.background(0);
    // only for depth_linear shader
    // Don't pay attention to the doesn't have a uniform called "far/near" message
    if (depthPath.matches("depth_linear.glsl")) {
      depthShader.set("near", scene.zNear());
      depthShader.set("far", scene.zFar());
    }
    scene.render(depthPGraphics);
    depthPGraphics.endDraw();

    // 3. Draw destination buffer
    dofPGraphics.beginDraw();
    dofShader.set("focus", map(mouseX, 0, width, -0.5f, 1.5f));
    dofShader.set("tDepth", depthPGraphics);
    dofPGraphics.image(scene.context(), 0, 0);
    dofPGraphics.endDraw();

    // display one of the 3 buffers
    if (mode == 0)
      scene.display();
    else if (mode == 1)
      scene.display(depthPGraphics);
    else
      scene.display(dofPGraphics);
  }

  PShape boxShape() {
    PShape box = createShape(BOX, 60);
    box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return box;
  }

  @Override
  public void keyPressed() {
    if (key == '0') mode = 0;
    if (key == '1') mode = 1;
    if (key == '2') mode = 2;
    if (key == 't')
      if (scene.type() == Graph.Type.ORTHOGRAPHIC)
        scene.setType(Graph.Type.PERSPECTIVE);
      else
        scene.setType(Graph.Type.ORTHOGRAPHIC);
    if (key == 'f')
      scene.fit(1);
    if (key == 'F')
      scene.fit();
    if (key == 'p') {
      exact = !exact;
      for (int i = 0; i < models.length; i++)
        models[i].setPickingThreshold(exact ? 0 : 0.7f);
    }
  }

  @Override
  public void mouseMoved() {
    scene.mouseTag();
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.moveForward(scene.mouseDX());
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.alignTag();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.DOF"});
  }
}
