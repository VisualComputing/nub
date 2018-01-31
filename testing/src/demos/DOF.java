package demos;

import common.InteractiveNode;
import common.InteractiveShape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.opengl.PShader;
import remixlab.proscene.Scene;

public class DOF extends PApplet {
  PShader depthShader, dofShader;
  PGraphics srcPGraphics, depthPGraphics, dofPGraphics;
  Scene scene;
  float posns[];
  InteractiveShape[] models;
  int mode = 2;

  public void settings() {
    size(1000, 800, P3D);
  }

  public void setup() {
    colorMode(HSB, 255);
    posns = new float[300];
    for (int i = 0; i < 100; i++) {
      posns[3 * i] = random(-1000, 1000);
      posns[3 * i + 1] = random(-1000, 1000);
      posns[3 * i + 2] = random(-1000, 1000);
    }

    srcPGraphics = createGraphics(width, height, P3D);
    scene = new Scene(this, srcPGraphics);
    InteractiveNode eye = new InteractiveNode(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    //interactivity defaults to the eye
    scene.setDefaultNode(eye);
    scene.setRadius(1000);
    scene.fitBallInterpolation();

    models = new InteractiveShape[100];

    for (int i = 0; i < models.length; i++) {
      models[i] = new InteractiveShape(scene, boxShape());
      models[i].translate(posns[3 * i], posns[3 * i + 1], posns[3 * i + 2]);
    }

    depthShader = loadShader("/home/pierre/IdeaProjects/proscene.js/testing/data/dof/depth.glsl");
    depthShader.set("maxDepth", scene.radius() * 2);
    depthPGraphics = createGraphics(width, height, P3D);
    depthPGraphics.shader(depthShader);

    dofShader = loadShader("/home/pierre/IdeaProjects/proscene.js/testing/data/dof/dof.glsl");
    dofShader.set("aspect", width / (float) height);
    dofShader.set("maxBlur", (float) 0.015);
    dofShader.set("aperture", (float) 0.02);
    dofPGraphics = createGraphics(width, height, P3D);
    dofPGraphics.shader(dofShader);

    frameRate(1000);
  }

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

  public void keyPressed() {
    if (key == '0') mode = 0;
    if (key == '1') mode = 1;
    if (key == '2') mode = 2;
    if (key == 's') scene.saveConfig("/home/pierre/config.json");
    if (key == 'l') scene.loadConfig("/home/pierre/config.json");
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"demos.DOF"});
  }
}
