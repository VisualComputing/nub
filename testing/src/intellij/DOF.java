package intellij;

import frames.core.Graph;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class DOF extends PApplet {
  PShader depthShader, dofShader;
  PGraphics srcPGraphics, depthPGraphics, dofPGraphics;
  Scene scene;
  Shape[] models;
  int mode = 2;

  @Override
  public void settings() {
    size(1000, 800, P3D);
  }

  @Override
  public void setup() {
    colorMode(HSB, 255);
    srcPGraphics = createGraphics(width, height, P3D);
    scene = new Scene(this, srcPGraphics);
    //scene.setAperture(Graph.Type.PERSPECTIVE);
    scene.setAperture(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(1000);
    scene.fitBallInterpolation();

    models = new Shape[100];

    for (int i = 0; i < models.length; i++) {
      models[i] = new Shape(scene, boxShape());
      scene.randomize(models[i]);
    }

    depthShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/dof/depth.glsl");
    depthShader.set("maxDepth", scene.radius() * 2);
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
    scene.frontBuffer().background(0);
    scene.traverse();
    scene.endDraw();

    // 2. Draw into depth buffer
    depthPGraphics.beginDraw();
    depthPGraphics.background(0);
    scene.traverse(depthPGraphics);
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
        scene.setAperture(Graph.Type.PERSPECTIVE);
      else
        scene.setAperture(Graph.Type.ORTHOGRAPHIC);
    if (key == 'f')
      scene.fitBallInterpolation();
    if (key == 'F')
      scene.fitBall();
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
      scene.zoom(scene.mouseDX());
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
