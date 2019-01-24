package intellij;

import frames.core.Frame;
import frames.core.Graph;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class DOF extends PApplet {
  PShader depthShader, dofShader;
  PGraphics depthPGraphics, dofPGraphics;
  Scene scene;
  Frame[] models;
  int mode = 2;
  boolean exact = true;

  @Override
  public void settings() {
    size(1000, 800, P3D);
  }

  @Override
  public void setup() {
    colorMode(HSB, 255);
    scene = new Scene(this, P3D, width, height);
    scene.setRadius(1000);
    scene.fit(1);

    models = new Frame[100];

    for (int i = 0; i < models.length; i++) {
      //models[i] = new Frame(scene, boxShape());
      models[i] = new Frame(scene, boxShape());
      scene.randomize(models[i]);
    }

    //depthShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/dof/depth_linear.glsl");
    //depthShader.set("maxDepth", scene.radius() * 2);
    //depthShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/depth/depth_nonlinear.glsl");
    depthShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/depth/depth_linear.glsl");
    depthPGraphics = createGraphics(width, height, P3D);
    depthPGraphics.shader(depthShader);

    dofShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/dof/dof.glsl");
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
    for (int i = 0; i < models.length; i++)
      scene.drawShooterTarget(models[i]);
    scene.frontBuffer().background(0);
    scene.render();
    scene.endDraw();

    // 2. Draw into depth buffer
    depthPGraphics.beginDraw();
    depthPGraphics.background(0);
    depthShader.set("near", scene.zNear());
    depthShader.set("far", scene.zFar());
    scene.render(depthPGraphics);
    depthPGraphics.endDraw();

    // 3. Draw destination buffer
    dofPGraphics.beginDraw();
    dofShader.set("focus", map(mouseX, 0, width, -0.5f, 1.5f));
    dofShader.set("tDepth", depthPGraphics);
    dofPGraphics.image(scene.frontBuffer(), 0, 0);
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
    if (key == 's') scene.saveConfig("/home/pierre/config.json");
    if (key == 'l') scene.loadConfig("/home/pierre/config.json");
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
      if (scene.backBuffer() == null)
        println("backBuffer disabled");
      else
        println("backBuffer enabled");
    }
  }

  @Override
  public void mouseMoved() {
    scene.cast();
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
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
        scene.align();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.DOF"});
  }
}
