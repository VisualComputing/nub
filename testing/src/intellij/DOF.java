package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;
import processing.opengl.PShader;

import java.nio.file.Paths;

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
    size(700, 700, P3D);
  }

  @Override
  public void setup() {
    scene = new Scene(this, P3D, width, height);
    scene.setRadius(1000);
    scene.fit(1);
    models = new Node[100];
    for (int i = 0; i < models.length; i++) {
      models[i] = new Node(boxShape());
      models[i].setBullsEyeSize(0.7f);
      scene.randomize(models[i]);
    }

    // Depth shader
    // Test all the different versions
    //depthPath = Paths.get("testing/data/depth/depth_linear.glsl").toAbsolutePath().toString();
    depthPath = Paths.get("testing/data/depth/depth_nonlinear.glsl").toAbsolutePath().toString();
    depthShader = loadShader(depthPath);
    depthPGraphics = createGraphics(width, height, P3D);
    depthPGraphics.shader(depthShader);

    // DOF shader
    dofShader = loadShader(Paths.get("testing/data/dof/dof.glsl").toAbsolutePath().toString());
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
      image(scene.context(), 0, 0);
    else if (mode == 1)
      image(depthPGraphics, 0, 0);
    else
      image(dofPGraphics, 0, 0);
    println("-> frameRate: " + Scene.frameRate() + " (nub) " + frameRate + " (p5)");
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
        if (models[i].pickingPolicy() == Node.SHAPE)
          models[i].setPickingPolicy(Node.BULLSEYE);
        else
          models[i].setPickingPolicy(Node.SHAPE);
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
