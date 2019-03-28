package intellij;

import nub.processing.*;
import processing.core.*;
import processing.event.*;
import processing.opengl.*;

public class SimpleShadows extends PApplet {
  Scene scene;
  PVector lightDir = new PVector();
  PShader depthShader, defaultShader;
  PGraphics shadowMap;
  int landscape = 1;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    size(w, h, P3D);
    scene = new Scene(this);
    scene.togglePerspective();
    scene.setRadius(max(w, h)/3);
    scene.fit(1);
    initShadowPass();
    initDefaultPass();
  }

  public void draw() {
    // Calculate the light direction (actually scaled by negative distance)
    float lightAngle = frameCount * 0.002f;
    lightDir.set(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);

    // Render shadow pass
    shadowMap.beginDraw();
    shadowMap.camera(lightDir.x, lightDir.y, lightDir.z, 0, 0, 0, 0, 1, 0);
    shadowMap.background(0xffffffff); // Will set the depth to 1.0 (maximum depth)
    renderLandscape(shadowMap);
    shadowMap.endDraw();
    shadowMap.updatePixels();

    // Update the shadow transformation matrix and send it, the light
    // direction normal and the shadow map to the default shader.
    updateDefaultShader();

    // Render default pass
    background(0xff222222);
    renderLandscape(g);

    // Render light source
    pushMatrix();
    fill(0xffffffff);
    translate(lightDir.x, lightDir.y, lightDir.z);
    box(5);
    popMatrix();

  }

  public void keyPressed() {
    if(key != CODED) {
      if(key >= '1' && key <= '3')
        landscape = key - '0';
      else if(key == 'd') {
        shadowMap.beginDraw(); shadowMap.ortho(-200, 200, -200, 200, 10, 400); shadowMap.endDraw();
      } else if(key == 's') {
        shadowMap.beginDraw(); shadowMap.perspective(60 * DEG_TO_RAD, 1, 10, 1000); shadowMap.endDraw();
      }
    }
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
    scene.scale(event.getCount() * 20);
  }

  public void initShadowPass() {
    depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth/depth_frag.glsl");
    shadowMap = createGraphics(2048, 2048, P3D);
    shadowMap.noSmooth(); // Antialiasing on the shadowMap leads to weird artifacts
    //shadowMap.loadPixels(); // Will interfere with noSmooth() (probably a bug in Processing)
    shadowMap.beginDraw();
    shadowMap.noStroke();
    shadowMap.shader(depthShader);
    shadowMap.ortho(-200, 200, -200, 200, 10, 400); // Setup orthogonal view matrix for the directional light
    shadowMap.endDraw();
  }

  public void initDefaultPass() {
    defaultShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/shadow/shadow_frag.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/shadow/shadow_vert.glsl");
    shader(defaultShader);
    noStroke();
    perspective(60 * DEG_TO_RAD, (float)width / height, 10, 1000);
  }

  void updateDefaultShader() {

    // Bias matrix to move homogeneous shadowCoords into the UV texture space
    PMatrix3D shadowTransform = new PMatrix3D(
        0.5f, 0.0f, 0.0f, 0.5f,
        0.0f, 0.5f, 0.0f, 0.5f,
        0.0f, 0.0f, 0.5f, 0.5f,
        0.0f, 0.0f, 0.0f, 1.0f
    );

    // Apply project modelview matrix from the shadow pass (light direction)
    shadowTransform.apply(((PGraphicsOpenGL)shadowMap).projmodelview);

    // Apply the inverted modelview matrix from the default pass to get the original vertex
    // positions inside the shader. This is needed because Processing is pre-multiplying
    // the vertices by the modelview matrix (for better performance).
    PMatrix3D modelviewInv = ((PGraphicsOpenGL)g).modelviewInv;
    shadowTransform.apply(modelviewInv);

    // Convert column-minor PMatrix to column-major GLMatrix and send it to the shader.
    // PShader.set(String, PMatrix3D) doesn't convert the matrix for some reason.
    defaultShader.set("shadowTransform", new PMatrix3D(
        shadowTransform.m00, shadowTransform.m10, shadowTransform.m20, shadowTransform.m30,
        shadowTransform.m01, shadowTransform.m11, shadowTransform.m21, shadowTransform.m31,
        shadowTransform.m02, shadowTransform.m12, shadowTransform.m22, shadowTransform.m32,
        shadowTransform.m03, shadowTransform.m13, shadowTransform.m23, shadowTransform.m33
    ));

    // Calculate light direction normal, which is the transpose of the inverse of the
    // modelview matrix and send it to the default shader.
    float lightNormalX = lightDir.x * modelviewInv.m00 + lightDir.y * modelviewInv.m10 + lightDir.z * modelviewInv.m20;
    float lightNormalY = lightDir.x * modelviewInv.m01 + lightDir.y * modelviewInv.m11 + lightDir.z * modelviewInv.m21;
    float lightNormalZ = lightDir.x * modelviewInv.m02 + lightDir.y * modelviewInv.m12 + lightDir.z * modelviewInv.m22;
    float normalLength = sqrt(lightNormalX * lightNormalX + lightNormalY * lightNormalY + lightNormalZ * lightNormalZ);
    defaultShader.set("lightDirection", lightNormalX / -normalLength, lightNormalY / -normalLength, lightNormalZ / -normalLength);

    // Send the shadowmap to the default shader
    defaultShader.set("shadowMap", shadowMap);

  }

  public void renderLandscape(PGraphics canvas) {
    switch(landscape) {
      case 1: {
        float offset = -frameCount * 0.01f;
        canvas.fill(0xffff5500);
        for(int z = -5; z < 6; ++z)
          for(int x = -5; x < 6; ++x) {
            canvas.pushMatrix();
            canvas.translate(x * 12, sin(offset + x) * 20 + cos(offset + z) * 20, z * 12);
            canvas.box(10, 100, 10);
            canvas.popMatrix();
          }
      } break;
      case 2: {
        float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
        canvas.fill(0xffff5500);
        for(int n = 0; n < 20; ++n, angle += rotation) {
          canvas.pushMatrix();
          canvas.translate(sin(angle) * 70, cos(angle * 4) * 10, cos(angle) * 70);
          canvas.box(10, 100, 10);
          canvas.popMatrix();
        }
        canvas.fill(0xff0055ff);
        canvas.sphere(50);
      } break;
      case 3: {
        float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
        canvas.fill(0xffff5500);
        for(int n = 0; n < 20; ++n, angle += rotation) {
          canvas.pushMatrix();
          canvas.translate(sin(angle) * 70, cos(angle) * 70, 0);
          canvas.box(10, 10, 100);
          canvas.popMatrix();
        }
        canvas.fill(0xff00ff55);
        canvas.sphere(50);
      }
    }
    canvas.fill(0xff222222);
    canvas.box(360, 5, 360);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.SimpleShadows"});
  }
}
