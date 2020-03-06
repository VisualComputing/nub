package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import org.gamecontrolplus.ControlButton;
import org.gamecontrolplus.ControlDevice;
import org.gamecontrolplus.ControlIO;
import org.gamecontrolplus.ControlSlider;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;

public class GoogleSpaceNavigator extends PApplet {
  ControlIO control;
  ControlDevice device; // my SpaceNavigator
  ControlSlider snXPos; // Positions
  ControlSlider snYPos;
  ControlSlider snZPos;
  ControlSlider snXRot; // Rotations
  ControlSlider snYRot;
  ControlSlider snZRot;
  ControlButton button1; // Buttons
  ControlButton button2;

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
  boolean defaultMode = true;

  public void settings() {
    size(1240, 840, P3D);
  }

  public void setup() {
    openSpaceNavigator();
    texmap = loadImage("/home/pierre/IdeaProjects/nub/testing/data/globe/world32k.jpg");
    initializeSphere(sDetail);
    scene = new Scene(this);
    scene.setRadius(globeRadius * 1.2f);
    scene.fit(1);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    //
    //lights();
    fill(200);
    noStroke();
    textureMode(IMAGE);
    texturedSphere(globeRadius, texmap);
    //
    if (defaultMode)
      spaceNavigatorInteraction();
    else
      googleEarth();
  }

  void spaceNavigatorInteraction() {
    scene.translateEye(10 * snXPos.getValue(), 10 * snYPos.getValue(), 10 * snZPos.getValue());
    scene.rotateEye(-snXRot.getValue() * 20 * PI / width, snYRot.getValue() * 20 * PI / width, snZRot.getValue() * 20 * PI / width);
  }

  float computeAngle(float dx) {
    return dx * (float) Math.PI / scene.width();
  }

  // aka google earth navigation
  void googleEarth() {
    // 1. Relate the eye reference frame:
    Vector pos = scene.eye().position();
    Quaternion o = scene.eye().orientation();
    Node oldRef = scene.eye().reference();
    Node rFrame = new Node();
    rFrame.setPosition(scene.anchor());
    rFrame.setZAxis(Vector.subtract(pos, scene.anchor()));
    rFrame.setXAxis(scene.eye().xAxis());
    scene.eye().setReference(rFrame);
    scene.eye().setPosition(pos);
    scene.eye().setOrientation(o);
    // 2. Translate the refFrame along its Z-axis:
    scene.eye().translate(0, 0, -10 * snZPos.getValue());
    // 3. Rotate the refFrame around its X-axis -> translate forward-backward
    // the frame on the sphere surface
    float deltaY = computeAngle(10 * snYPos.getValue());
    rFrame.rotate(new Quaternion(new Vector(1, 0, 0), scene.isRightHanded() ? deltaY : -deltaY));
    // 4. Rotate the refFrame around its Y-axis -> translate left-right the
    // frame on the sphere surface
    float deltaX = computeAngle(10 * snXPos.getValue());
    rFrame.rotate(new Quaternion(new Vector(0, 1, 0), deltaX));
    // 5. Rotate the refFrame around its Z-axis -> look around
    float rZ = computeAngle(snZRot.getValue() * 20);
    rFrame.rotate(new Quaternion(new Vector(0, 0, 1), scene.isRightHanded() ? -rZ : rZ));
    // 6. Rotate the frame around x-axis -> move head up and down :P
    float rX = computeAngle(snXRot.getValue() * 20);
    Quaternion q = new Quaternion(new Vector(1, 0, 0), scene.isRightHanded() ? rX : -rX);
    scene.eye().rotate(q);
    // 7. Unrelate the frame and restore state:
    pos = scene.eye().position();
    o = scene.eye().orientation();
    scene.eye().setReference(oldRef);
    scene.prune(rFrame);
    scene.eye().setPosition(pos);
    scene.eye().setOrientation(o);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpinEye();
    else if (mouseButton == RIGHT)
      scene.mouseTranslateEye();
    else
      scene.scaleEye(scene.mouseDX());
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ') {
      defaultMode = !defaultMode;
      if (defaultMode) {
        scene.lookAtCenter();
        scene.fit(1);
      } else {
        Vector t = new Vector(0, 0, 0.7f * globeRadius);
        float a = TWO_PI - 2;
        scene.eye().setPosition(t);
        //We need to line up the eye up vector along the anchor and the camera position:
        scene.setUpVector(Vector.subtract(scene.eye().position(), scene.anchor()));
        //The rest is just to make the scene appear in front of us. We could have just used
        //the space navigator itself to make that happen too.
        scene.eye().rotate(new Quaternion(a, 0, 0));
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

  void openSpaceNavigator() {
    println(System.getProperty("os.name"));
    control = ControlIO.getInstance(this);
    String os = System.getProperty("os.name").toLowerCase();
    if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0)
      device = control.getDevice("3Dconnexion SpaceNavigator");// magic name for linux
    else
      device = control.getDevice("SpaceNavigator");//magic name, for windows
    if (device == null) {
      println("No suitable device configured");
      System.exit(-1); // End the program NOW!
    }
    //device.setTolerance(5);
    snXPos = device.getSlider(0);
    snYPos = device.getSlider(1);
    snZPos = device.getSlider(2);
    snXRot = device.getSlider(3);
    snYRot = device.getSlider(4);
    snZRot = device.getSlider(5);
    //button1 = device.getButton(0);
    //button2 = device.getButton(1);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.GoogleSpaceNavigator"});
  }
}
