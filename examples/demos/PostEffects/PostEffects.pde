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
PGraphics drawGraphics, noiseGraphics, kaleidoGraphics, raysGraphics, pixelGraphics, edgeGraphics, depthPGraphics, horizontalGraphics;
Scene scene;
boolean bdepth, brays, bpixel, bedge, bkaleido, bnoise, bhorizontal;
Node[] models;
PFont font;

public void setup() {
  size(900, 900, P3D);
  font = loadFont("FreeSans-13.vlw");
  textFont(font);
  colorMode(HSB, 255);
  scene = new Scene(this, P3D);
  scene.setRadius(1000);
  models = new Node[100];
  for (int i = 0; i < models.length; i++) {
    models[i] = new Node(shape());
    scene.randomize(models[i]);
  }
  scene.fit(1);

  depthShader = loadShader("depth.glsl");
  depthPGraphics = createGraphics(width, height, P3D);
  depthPGraphics.shader(depthShader);

  edgeShader = loadShader("edge.glsl");
  edgeGraphics = createGraphics(width, height, P3D);
  edgeGraphics.shader(edgeShader);
  edgeShader.set("aspect", 1.0/width, 1.0/height);

  pixelShader = loadShader("pixelate.glsl");
  pixelGraphics = createGraphics(width, height, P3D);
  pixelGraphics.shader(pixelShader);
  pixelShader.set("xPixels", 100.0);
  pixelShader.set("yPixels", 100.0);

  raysShader = loadShader("raysfrag.glsl");
  raysGraphics = createGraphics(width, height, P3D);
  raysGraphics.shader(raysShader);
  raysShader.set("lightPositionOnScreen", 0.5, 0.5);
  raysShader.set("lightDirDOTviewDir", 0.7);

  kaleidoShader = loadShader("kaleido.glsl");
  kaleidoGraphics = createGraphics(width, height, P3D);
  kaleidoGraphics.shader(kaleidoShader);
  kaleidoShader.set("segments", 2.0);

  noiseShader = loadShader("noise.glsl");
  noiseGraphics = createGraphics(width, height, P3D);
  noiseGraphics.shader(noiseShader);
  noiseShader.set("frequency", 4.0);
  noiseShader.set("amplitude", 0.1);
  noiseShader.set("speed", 0.1);

  horizontalShader = loadShader("horizontal.glsl");
  horizontalGraphics = createGraphics(width, height, P3D);
  horizontalGraphics.shader(horizontalShader);
  horizontalShader.set("h", 0.005);
  horizontalShader.set("r", 0.5);
  frameRate(100);
}

public void draw() {
  PGraphics graphics = drawGraphics = scene.context();

  // 1. Draw into main buffer
  scene.beginDraw();
  graphics.background(0);
  scene.render();
  scene.endDraw();

  if (bdepth){
    depthPGraphics.beginDraw();
    depthPGraphics.background(0);
    depthShader.set("near", scene.zNear());
    depthShader.set("far", scene.zFar());
    //Note that when drawing the shapes into an arbitrary PGraphics
    //the eye position of the main PGraphics is used
    scene.render(depthPGraphics);
    depthPGraphics.endDraw();
    drawGraphics = depthPGraphics;
  }
  if (bkaleido) {
    kaleidoGraphics.beginDraw();
    kaleidoShader.set("tex", drawGraphics);
    kaleidoGraphics.image(graphics, 0, 0);
    kaleidoGraphics.endDraw();
    drawGraphics = kaleidoGraphics;
  }
  if (bnoise) {
    noiseGraphics.beginDraw();
    noiseShader.set("time", millis() / 1000.0);
    noiseShader.set("tex", drawGraphics);
    noiseGraphics.image(graphics, 0, 0);
    noiseGraphics.endDraw();
    drawGraphics = noiseGraphics;
  }
  if (bpixel) {
    pixelGraphics.beginDraw();
    pixelShader.set("tex", drawGraphics);
    pixelGraphics.image(graphics, 0, 0);
    pixelGraphics.endDraw();
    drawGraphics = pixelGraphics;
  }
  if (bedge) {
    edgeGraphics.beginDraw();
    edgeShader.set("tex", drawGraphics);
    edgeGraphics.image(graphics, 0, 0);
    edgeGraphics.endDraw();
    drawGraphics = edgeGraphics;
  }
  if (bhorizontal) {
    horizontalGraphics.beginDraw();
    horizontalShader.set("tDiffuse", drawGraphics);
    horizontalGraphics.image(graphics, 0, 0);
    horizontalGraphics.endDraw();
    drawGraphics = horizontalGraphics;
  }
  if (brays) {
    raysGraphics.beginDraw();
    raysShader.set("otex", drawGraphics);
    raysShader.set("rtex", drawGraphics);
    raysGraphics.image(graphics, 0, 0);
    raysGraphics.endDraw();
    drawGraphics = raysGraphics;
  }
  scene.display(drawGraphics);
  drawText();
}

void drawText() {
  scene.beginHUD();
  text(bdepth ? "1. Depth (*)" : "1. Depth", 5, 20);
  text(bkaleido ? "2. Kaleidoscope (*)" : "2. Kaleidoscope", 5, 35);
  text(bnoise ? "3. Noise (*)" : "3. Noise", 5, 50);
  text(bpixel ? "4. Pixelate (*)" : "4. Pixelate", 5, 65);
  text(bedge ? "5. Edge (*)" : "5. Edge", 5, 80);
  text(bhorizontal ? "6. Horizontal (*)" : "6. Horizontal", 5, 95);
  text(brays ? "7. Rays (*)" : "7. Rays", 5, 110);
  scene.endHUD();
}

PShape shape() {
  PShape fig;
  if (int(random(2))%2 == 0)
    fig = createShape(BOX, 60);
  else
    fig = createShape(SPHERE, 30);
  fig.setStroke(255);
  fig.setFill(color(random(0,255), random(0,255), random(0,255)));
  return fig;
}

void keyPressed() {
  if(key=='1')
    bdepth = !bdepth;
  if(key=='2')
    bkaleido = !bkaleido;
  if(key=='3')
    bnoise = !bnoise;
  if(key=='4')
    bpixel = !bpixel;
  if(key=='5')
    bedge = !bedge;
  if(key=='6')
    bhorizontal = !bhorizontal;
  if(key=='7')
    brays = !brays;
}

void mouseMoved() {
  scene.mouseTag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpin();
  else if (mouseButton == RIGHT)
    scene.mouseTranslate();
  else
    scene.moveForward(scene.mouseDX());
}

void mouseWheel(MouseEvent event) {
  scene.scale(event.getCount() * 20);
}
