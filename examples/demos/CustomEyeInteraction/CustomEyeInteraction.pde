/**
 * Custom Eye Interaction.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates how to customize the eye behavior.
 *
 * The implemented key actions control the eye using the node api:
 * while the node.rotate* methods locally rotate the eye respect
 * to its position, the node.orbit methods emulate translations on
 * the globe by rotating the eye around a world axis that should
 * passed through the world origin.
 *
 * Press ' ' to toggle the eye mode from mouse to key.
 *
 * The eye key mode has the following bindings:
 *
 * UP and DOWN arrows: translate the eye forward-backward
 * LEFT and RIGHT arrows: translate the eye left-right
 * UP and DOWN + SHIFT: rotate the eye up and down
 * LEFT and RIGHT arrows + SHIFT: to look around
 */

import nub.primitives.*;
import nub.processing.*;

PImage texmap;
float globeRadius = 400;
int sDetail = 35;  // Sphere detail setting
float[] sphereX, sphereY, sphereZ;
float sinLUT[];
float cosLUT[];
float SINCOS_PRECISION = 0.5f;
int SINCOS_LENGTH = (int) (360 / SINCOS_PRECISION);

// nub stuff:
Scene scene;
boolean keyMode;

// size
int w = 1200;
int h = 700;
float step = 5 * TWO_PI / w;

void settings() {
  size(w, h, P3D);
}

void setup() {
  texmap = loadImage("world32k.jpg");
  initializeSphere(sDetail);
  scene = new Scene(this, globeRadius * 1.2f);
}

void draw() {
  scene.openContext();
  background(0);
  scene.closeContext();
  scene.render();
  fill(200);
  noStroke();
  textureMode(IMAGE);
  texturedSphere(globeRadius, texmap);
}

void mouseDragged() {
  if (keyMode)
    return;
  if (mouseButton == LEFT)
    scene.mouseSpinEye();
  else if (mouseButton == RIGHT)
    scene.mouseTranslateEye();
  else
    scene.scaleEye(scene.mouseDX());
}

void mouseWheel(MouseEvent event) {
  if (keyMode)
    scene.moveForward(event.getCount() * 20);
}

void keyPressed(KeyEvent event) {
  if (key == ' ') {
    keyMode = !keyMode;
    if (keyMode) {
      Vector t = new Vector(0, 0, 0.7f * globeRadius);
      float a = TWO_PI - 2;
      scene.eye().setPosition(t);
      // We need to line up the eye up vector along the anchor and the camera position:
      scene.setUpVector(Vector.subtract(scene.eye().position(), scene.center()));
      // The rest is just to make the scene appear in front of us.
      scene.eye().rotate(Quaternion.from(a, 0, 0));
    } else {
      scene.lookAt(Vector.zero);
      scene.fit(1);
    }
  }
  if (keyMode) {
    if (key == CODED) {
      // note that the last parameter in the below
      // methods is the inertia which should be in [0..1]
      switch (keyCode) {
      case UP:
        if (event.isShiftDown())
          scene.eye().rotate(Vector.plusI, -step, 0.85);
        else
          scene.eye().orbit(xAxis(), step, 0.85);
        break;
      case DOWN:
        if (event.isShiftDown())
          scene.eye().rotate(Vector.plusI, step, 0.85);
        else
          scene.eye().orbit(xAxis(), -step, 0.85);
        break;
      case LEFT:
        if (event.isShiftDown())
          scene.eye().orbit(zAxis(), -step, 0.85);
        else
          scene.eye().orbit(yAxis(), -step, 0.85);
        break;
      case RIGHT:
        if (event.isShiftDown())
          scene.eye().orbit(zAxis(), step, 0.85);
        else
          scene.eye().orbit(yAxis(), step, 0.85);
        break;
      }
    }
  }
}

Vector xAxis() {
  return scene.eye().worldDisplacement(Vector.plusI);
}

Vector yAxis() {
  return scene.eye().worldDisplacement(Vector.plusJ);
}

Vector zAxis() {
  return scene.eye().worldDisplacement(Vector.plusK);
}

void initializeSphere(int res) {
  sinLUT = new float[SINCOS_LENGTH];
  cosLUT = new float[SINCOS_LENGTH];
  for (int i = 0; i < SINCOS_LENGTH; i++) {
    sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
    cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
  }
  float delta = (float) SINCOS_LENGTH / res;
  float[] cx = new float[res];
  float[] cz = new float[res];
  // Calc unit circle in XZ plane
  for (int i = 0; i < res; i++) {
    cx[i] = -cosLUT[(int) (i * delta) % SINCOS_LENGTH];
    cz[i] = sinLUT[(int) (i * delta) % SINCOS_LENGTH];
  }
  // Computing vertexlist vertexlist starts at south pole
  int vertCount = res * (res - 1) + 2;
  int currVert = 0;
  // Re-init arrays to store vertices
  sphereX = new float[vertCount];
  sphereY = new float[vertCount];
  sphereZ = new float[vertCount];
  float angle_step = (SINCOS_LENGTH * 0.5f) / res;
  float angle = angle_step;
  // Step along Y axis
  for (int i = 1; i < res; i++) {
    float curradius = sinLUT[(int) angle % SINCOS_LENGTH];
    float currY = -cosLUT[(int) angle % SINCOS_LENGTH];
    for (int j = 0; j < res; j++) {
      sphereX[currVert] = cx[j] * curradius;
      sphereY[currVert] = currY;
      sphereZ[currVert++] = cz[j] * curradius;
    }
    angle += angle_step;
  }
  sDetail = res;
}

// Generic routine to draw textured sphere
void texturedSphere(float r, PImage t) {
  int v1, v11, v2;
  r = (r + 240) * 0.33f;
  beginShape(TRIANGLE_STRIP);
  texture(t);
  float iu = (float) (t.width - 1) / (sDetail);
  float iv = (float) (t.height - 1) / (sDetail);
  float u = 0, v = iv;
  for (int i = 0; i < sDetail; i++) {
    vertex(0, -r, 0, u, 0);
    vertex(sphereX[i] * r, sphereY[i] * r, sphereZ[i] * r, u, v);
    u += iu;
  }
  vertex(0, -r, 0, u, 0);
  vertex(sphereX[0] * r, sphereY[0] * r, sphereZ[0] * r, u, v);
  endShape();
  // Middle rings
  int voff = 0;
  for (int i = 2; i < sDetail; i++) {
    v1 = v11 = voff;
    voff += sDetail;
    v2 = voff;
    u = 0;
    beginShape(TRIANGLE_STRIP);
    texture(t);
    for (int j = 0; j < sDetail; j++) {
      vertex(sphereX[v1] * r, sphereY[v1] * r, sphereZ[v1++] * r, u, v);
      vertex(sphereX[v2] * r, sphereY[v2] * r, sphereZ[v2++] * r, u, v + iv);
      u += iu;
    }
    // Close each ring
    v1 = v11;
    v2 = voff;
    vertex(sphereX[v1] * r, sphereY[v1] * r, sphereZ[v1] * r, u, v);
    vertex(sphereX[v2] * r, sphereY[v2] * r, sphereZ[v2] * r, u, v + iv);
    endShape();
    v += iv;
  }
  u = 0;
  // Add the northern cap
  beginShape(TRIANGLE_STRIP);
  texture(t);
  for (int i = 0; i < sDetail; i++) {
    v2 = voff + i;
    vertex(sphereX[v2] * r, sphereY[v2] * r, sphereZ[v2] * r, u, v);
    vertex(0, r, 0, u, v + iv);
    u += iu;
  }
  vertex(sphereX[voff] * r, sphereY[voff] * r, sphereZ[voff] * r, u, v);
  endShape();
}
