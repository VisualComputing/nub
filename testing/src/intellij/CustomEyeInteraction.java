package intellij;

import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

public class CustomEyeInteraction extends PApplet {
  // scene
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
  int w = 1240;
  int h = 840;
  float step = 5 * TWO_PI / w;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    texmap = loadImage("/home/pierre/IdeaProjects/nub/testing/data/globe/world32k.jpg");
    initializeSphere(sDetail);
    scene = new Scene(this, globeRadius * 1.2f);
    scene.fit(1);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    //lights();
    fill(200);
    noStroke();
    textureMode(IMAGE);
    texturedSphere(globeRadius, texmap);
  }

  // eye().orbit(axis, angle) requires the axis to be defined in the world coordinate system:

  Vector xAxis() {
    return scene.eye().worldDisplacement(new Vector(1, 0, 0));
  }

  Vector yAxis() {
    return scene.eye().worldDisplacement(new Vector(0, 1, 0));
  }

  Vector zAxis() {
    return scene.eye().worldDisplacement(new Vector(0, 0, 1));
  }

  public void mouseDragged() {
    if (keyMode)
      return;
    if (mouseButton == LEFT)
      scene.mouseSpinEye();
    else if (mouseButton == RIGHT)
      scene.mouseTranslateEye();
    else
      scene.scaleEye(scene.mouseDX());
  }

  public void mouseWheel(MouseEvent event) {
    if (keyMode)
      scene.moveForward(event.getCount() * 20);
  }

  public void keyPressed(KeyEvent event) {
    if (key == ' ') {
      keyMode = !keyMode;
      if (keyMode) {
        /*
        //Node cachedEye = scene.eye().get();
        //Node eye = new Node(scene);
        //scene.setEye(eye);
        Node node = new Node();
        Vector t = new Vector(0, 0, 0.7f * globeRadius);
        float a = TWO_PI - 2;
        node.setPosition(t);
        //node.setYAxis(Vector.subtract(node.position(), scene.anchor()));
        //node.rotate(new Quaternion(a, 0, 0));
        scene.fit(node, 1);
        // */
        // /*
        Vector t = new Vector(0, 0, 0.7f * globeRadius);
        float a = TWO_PI - 2;
        scene.eye().setPosition(t);
        //We need to line up the eye up vector along the anchor and the camera position:
        scene.setUpVector(Vector.subtract(scene.eye().position(), scene.center()));
        //The rest is just to make the scene appear in front of us.
        scene.eye().rotate(new Quaternion(a, 0, 0));
        // */
      } else {
        scene.fit(1);
        scene.lookAt(scene.center());
      }
    }
    if (keyMode) {
      // Translate the eye along its reference Z-axis
      if (key == 'u')
        scene.eye().translate(0, 0, 10);
      if (key == 'd')
        scene.eye().translate(0, 0, -10);
      if (key == CODED) {
        switch (keyCode) {
          case UP:
            if (event.isShiftDown())
              // Rotate the eye around its X-axis -> move head up and down
              scene.eye().rotate(new Vector(1, 0, 0), -step);
            else
              // Orbit the eye around its X-axis -> translate forward-backward
              scene.eye().orbit(xAxis(), step);
            break;
          case DOWN:
            if (event.isShiftDown())
              // Rotate the eye around its X-axis -> move head up and down
              scene.eye().rotate(new Vector(1, 0, 0), step);
            else
              // Orbit the eye around its X-axis -> translate forward-backward
              scene.eye().orbit(xAxis(), -step);
            break;
          case LEFT:
            // /*
            if (event.isShiftDown())
              // Orbit the eye around its Z-axis -> look around
              scene.eye().orbit(zAxis(), -step);
            else
              // */
              // Orbit the eye around its Y-axis -> translate left-right
              scene.eye().orbit(yAxis(), -step);
            break;
          case RIGHT:
            // /*
            if (event.isShiftDown())
              // Orbit the eye around its Z-axis -> look around
              scene.eye().orbit(zAxis(), step);
            else
              // */
              // Orbit the eye around its Y-axis -> translate left-right
              scene.eye().orbit(yAxis(), step);
            break;
        }
      }
    }
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

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.CustomEyeInteraction"});
  }
}
