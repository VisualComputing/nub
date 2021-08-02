/**
 * Post Effects.
 * by Ivan Castellanos and Jean Pierre Charalambos.
 *
 * This example is an adaption of Neil Mendoza great openFrameworks
 * ofxPostProcessing addon (http://www.neilmendoza.com/ofxpostprocessing/)
 * which illustrates how to concatenate shaders to accumulate their effects
 * by drawing shapes into arbitrary PGraphics canvases.
 *
 * Press '1' to '7' to (de)activate effect.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

PShader noiseShader, kaleidoShader, raysShader, pixelShader, edgeShader, depthShader, horizontalShader;
PGraphics drawGraphics, noiseGraphics, kaleidoGraphics, raysGraphics, pixelGraphics, edgeGraphics, horizontalGraphics;
Scene scene, depthScene;
boolean depth, rays, pixel, edge, kaleido, noise, horizontal;
Node[] models;
PFont font;

public void setup() {
  size(1920, 1080, P3D);
  font = loadFont("FreeSans-13.vlw");
  textFont(font);
  colorMode(HSB, 255);
  // both the main and depth scenes share the same eye
  scene = new Scene(createGraphics(width, height, P3D), 1000);
  scene.enableHint(Scene.BACKGROUND, color(0));
  depthScene = new Scene(createGraphics(width, height, P3D), scene.eye(), 1000);
  depthShader = loadShader("depth.glsl");
  depthScene.context().shader(depthShader);
  depthScene.picking = false;
  depthScene.enableHint(Scene.BACKGROUND, color(0));
  // nodes
  models = new Node[100];
  for (int i = 0; i < models.length; i++) {
    models[i] = new Node(shape());
    scene.randomize(models[i]);
  }
  scene.fit(1);
  // edge
  edgeShader = loadShader("edge.glsl");
  edgeGraphics = createGraphics(width, height, P3D);
  edgeGraphics.shader(edgeShader);
  edgeShader.set("aspect", 1.0/width, 1.0/height);
  // pixel
  pixelShader = loadShader("pixelate.glsl");
  pixelGraphics = createGraphics(width, height, P3D);
  pixelGraphics.shader(pixelShader);
  pixelShader.set("xPixels", 100.0);
  pixelShader.set("yPixels", 100.0);
  // rays
  raysShader = loadShader("raysfrag.glsl");
  raysGraphics = createGraphics(width, height, P3D);
  raysGraphics.shader(raysShader);
  raysShader.set("lightPositionOnScreen", 0.5, 0.5);
  raysShader.set("lightDirDOTviewDir", 0.7);
  // kaleido
  kaleidoShader = loadShader("kaleido.glsl");
  kaleidoGraphics = createGraphics(width, height, P3D);
  kaleidoGraphics.shader(kaleidoShader);
  kaleidoShader.set("segments", 2.0);
  // noise
  noiseShader = loadShader("noise.glsl");
  noiseGraphics = createGraphics(width, height, P3D);
  noiseGraphics.shader(noiseShader);
  noiseShader.set("frequency", 4.0);
  noiseShader.set("amplitude", 0.1);
  noiseShader.set("speed", 0.1);
  // horizontal
  horizontalShader = loadShader("horizontal.glsl");
  horizontalGraphics = createGraphics(width, height, P3D);
  horizontalGraphics.shader(horizontalShader);
  horizontalShader.set("h", 0.005);
  horizontalShader.set("r", 0.5);
  frameRate(100);
}

public void draw() {
  PGraphics graphics = drawGraphics = scene.context();
  // 1. Render into main buffer
  scene.render();
  // 2. Draw into depth buffer
  if (depth) {
    depthScene.openContext();
    depthShader.set("near", depthScene.zNear());
    depthShader.set("far", depthScene.zFar());
    depthScene.render();
    depthScene.closeContext();
    drawGraphics = depthScene.context();
  }
  if (kaleido) {
    kaleidoGraphics.beginDraw();
    kaleidoShader.set("tex", drawGraphics);
    kaleidoGraphics.image(graphics, 0, 0);
    kaleidoGraphics.endDraw();
    drawGraphics = kaleidoGraphics;
  }
  if (noise) {
    noiseGraphics.beginDraw();
    noiseShader.set("time", millis() / 1000.0);
    noiseShader.set("tex", drawGraphics);
    noiseGraphics.image(graphics, 0, 0);
    noiseGraphics.endDraw();
    drawGraphics = noiseGraphics;
  }
  if (pixel) {
    pixelGraphics.beginDraw();
    pixelShader.set("tex", drawGraphics);
    pixelGraphics.image(graphics, 0, 0);
    pixelGraphics.endDraw();
    drawGraphics = pixelGraphics;
  }
  if (edge) {
    edgeGraphics.beginDraw();
    edgeShader.set("tex", drawGraphics);
    edgeGraphics.image(graphics, 0, 0);
    edgeGraphics.endDraw();
    drawGraphics = edgeGraphics;
  }
  if (horizontal) {
    horizontalGraphics.beginDraw();
    horizontalShader.set("tDiffuse", drawGraphics);
    horizontalGraphics.image(graphics, 0, 0);
    horizontalGraphics.endDraw();
    drawGraphics = horizontalGraphics;
  }
  if (rays) {
    raysGraphics.beginDraw();
    raysShader.set("otex", drawGraphics);
    raysShader.set("rtex", drawGraphics);
    raysGraphics.image(graphics, 0, 0);
    raysGraphics.endDraw();
    drawGraphics = raysGraphics;
  }
  image(drawGraphics, 0, 0);
  drawText();
}

void drawText() {
  scene.beginHUD();
  text(depth ? "1. Depth (*)" : "1. Depth", 5, 20);
  text(kaleido ? "2. Kaleidoscope (*)" : "2. Kaleidoscope", 5, 35);
  text(noise ? "3. Noise (*)" : "3. Noise", 5, 50);
  text(pixel ? "4. Pixelate (*)" : "4. Pixelate", 5, 65);
  text(edge ? "5. Edge (*)" : "5. Edge", 5, 80);
  text(horizontal ? "6. Horizontal (*)" : "6. Horizontal", 5, 95);
  text(rays ? "7. Rays (*)" : "7. Rays", 5, 110);
  scene.endHUD();
}

PShape shape() {
  PShape fig;
  if (int(random(2))%2 == 0)
    fig = createShape(BOX, 60);
  else
    fig = createShape(SPHERE, 30);
  fig.setStroke(255);
  fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return fig;
}

void keyPressed() {
  if (key=='1')
    depth = !depth;
  if (key=='2')
    kaleido = !kaleido;
  if (key=='3')
    noise = !noise;
  if (key=='4')
    pixel = !pixel;
  if (key=='5')
    edge = !edge;
  if (key=='6')
    horizontal = !horizontal;
  if (key=='7')
    rays = !rays;
}

void mouseMoved() {
  scene.mouseTag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpin();
  else if (mouseButton == RIGHT)
    scene.mouseShift();
  else
    scene.moveForward(scene.mouseDX());
}

void mouseWheel(MouseEvent event) {
  scene.zoom(event.getCount() * 20);
}
