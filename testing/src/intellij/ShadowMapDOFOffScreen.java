package intellij;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class ShadowMapDOFOffScreen extends PApplet {
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  Scene scene;
  Frame[] shapes;
  PGraphics lightPGraphics, depthPGraphics, dofPGraphics;
  PShader depthShader, dofShader;
  int mode = 0;
  float zNear = 50;
  float zFar = 1000;

  public void settings() {
    size(1200, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this, P3D);
    scene.setRadius(max(width, height));
    shapes = new Frame[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Frame(scene) {
        @Override
        public void graphics(PGraphics pg) {
          pg.pushStyle();
          if (scene.trackedFrame("light") == this) {
            Scene.drawAxes(pg, 150);
            pg.fill(0, scene.isTrackedFrame(this) ? 255 : 0, 255, 120);
            //Scene.drawFrustum(pg, depthPGraphics, shadowMapType, this, zNear, zFar);
            Scene.drawFrustum(pg, lightPGraphics, shadowMapType, this, zNear, zFar);
          } else {
            if (pg == depthPGraphics)
              pg.noStroke();
            else {
              pg.strokeWeight(3);
              pg.stroke(0, 255, 255);
            }
            pg.fill(255, 0, 0);
            pg.box(80);
          }
          pg.popStyle();
        }

        // /*
        @Override
        public void interact(Object... gesture) {
          if (gesture.length == 1)
            if (gesture[0] instanceof Integer)
              if (zFar + (Integer) gesture[0] > zNear) {
                zFar += (Integer) gesture[0];
                depthShader.set("far", zFar);
                //depthShader.set("maxDepth", zFar - zNear);
              }
        }
        // */
      };
      shapes[i].randomize();
      //shapes[i].setPrecision(Frame.Precision.FIXED);
      shapes[i].setHighlighting(Frame.Highlighting.NONE);
    }
    //scene._bbEnabled = false;
    scene.setRadius(scene.radius() * 1.2f);
    scene.fit(1);

    lightPGraphics = createGraphics(width / 2, height / 2, P3D);

    depthShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/depth/depth_linear.glsl");
    depthShader.set("near", zNear);
    depthShader.set("far", zFar);
    //depthShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/dof/depth_linear.glsl");
    //depthShader.set("maxDepth", zFar - zNear);
    depthPGraphics = createGraphics(width / 2, height / 2, P3D);
    depthPGraphics.shader(depthShader);

    dofShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/dof/dof.glsl");
    dofShader.set("aspect", width / (float) height);
    dofShader.set("maxBlur", (float) 0.015);
    dofShader.set("aperture", (float) 0.02);
    dofPGraphics = createGraphics(width, height, P3D);
    dofPGraphics.shader(dofShader);

    scene.setTrackedFrame("light", shapes[(int) random(0, shapes.length - 1)]);
    scene.trackedFrame("light").setOrientation(new Quaternion(new Vector(0, 0, 1), scene.trackedFrame("light").position()));
  }

  public void draw() {
    // 1. Draw into main buffer
    scene.beginDraw();
    scene.frontBuffer().background(75, 25, 15);
    scene.render();
    scene.endDraw();
    scene.display();

    // 2. Fill in shadow map using the light point of view
    if (scene.trackedFrame("light") != null) {
      lightPGraphics.beginDraw();
      lightPGraphics.background(140, 160, 125);
      scene.render(lightPGraphics, shadowMapType, scene.trackedFrame("light"), zNear, zFar);
      lightPGraphics.endDraw();
      // calling image here IS fine!
      //if(mode == 0) image(lightPGraphics, width / 2, height / 2);

      depthPGraphics.beginDraw();
      depthPGraphics.background(140, 160, 125);
      scene.render(depthPGraphics, shadowMapType, scene.trackedFrame("light"), zNear, zFar);
      depthPGraphics.endDraw();
      //if(mode == 1) image(depthPGraphics, width / 2, height / 2);

      // /*
      // 3. Draw destination buffer
      dofPGraphics.beginDraw();
      dofShader.set("focus", map(mouseX, 0, width, -0.5f, 1.5f));
      dofShader.set("tDepth", depthPGraphics);
      // merry christmas bug just found here!
      // calling image here is problematic since it messes the lightPGraphics and the frontBuffer
      //dofPGraphics.image(lightPGraphics, 0, 0);
      //dofPGraphics.image(scene.frontBuffer(), 0, 0);
      dofPGraphics.endDraw();
      //*/
      //if(mode == 2) image(dofPGraphics, width / 2, height / 2);

      // display one of the 3 buffers
      // calling image here ISN't fine
      // /*
      if (mode == 0)
        image(lightPGraphics, width / 2, height / 2);
      else if (mode == 1)
        image(depthPGraphics, width / 2, height / 2);
      else if (mode == 2)
        image(dofPGraphics, width / 2, height / 2);
      // */
    }
  }

  public void mouseMoved(MouseEvent event) {
    if (event.isShiftDown())
      scene.cast("light");
    else
      scene.cast();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (event.isShiftDown())
      // application control of the light: set the zfar plan of the light
      // it is implemented as a custom behavior by frame.interact()
      scene.control("light", event.getCount() * 20);
    else
      scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == '0') mode = 0;
    if (key == '1') mode = 1;
    if (key == '2') mode = 2;
    if (key == 'f')
      scene.fitFOV(1);
    if (key == 'o')
      shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
    if (key == 't')
      scene.togglePerspective();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShadowMapDOFOffScreen"});
  }
}
