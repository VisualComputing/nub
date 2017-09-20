/**
 * Frame Interpolation.
 * by Jean Pierre Charalambos.
 * 
 * This example (together with Camera Interpolation) illustrates the KeyFrameInterpolator
 * functionality.
 * 
 * KeyFrameInterpolator smoothly interpolate its attached Frame over time on a path
 * defined by Frames. The interpolation can be started/stopped/reset, played in loop,
 * played at a different speed, etc...
 * 
 * In this example, the path is defined by four InteractivedFrames which can be moved
 * with the mouse. The interpolating path is updated accordingly. The path and the
 * interpolating axis are drawn using scene.drawPath().
 * 
 * The Camera holds 5 KeyFrameInterpolators, binded to [1..5] keys. Pressing
 * CONTROL + [1..5] adds key frames to the specific path. Pressing ALT + [1..5]
 * deletes the specific path. Press 'r' to display all the key frame camera paths
 * (if any). The displayed paths are editable.
 * 
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;

Scene scene;
InteractiveFrame keyFrame[];
KeyFrameInterpolator kfi;
int nbKeyFrames;

public void setup() {
  size(640, 360, P3D);
  nbKeyFrames = 4;
  scene = new Scene(this);
  //unsets grid and axis altogether
  scene.setVisualHints( Scene.PICKING );
  scene.setRadius(70);
  scene.showAll();
  kfi = new KeyFrameInterpolator(scene);
  kfi.setLoopInterpolation();

  // An array of interactive (key) frames.
  keyFrame = new InteractiveFrame[nbKeyFrames];
  // Create an initial path
  for (int i=0; i<nbKeyFrames; i++) {
    keyFrame[i] = new InteractiveFrame(scene);
    keyFrame[i].setPosition(-100 + 200*i/(nbKeyFrames-1), 0, 0);
    keyFrame[i].setScaling(random(0.25f, 4.0f));
    kfi.addKeyFrame(keyFrame[i]);
  }

  kfi.startInterpolation();
}

public void draw() {
  background(0);
  pushMatrix();
  scene.applyTransformation(kfi.frame());
  scene.drawAxes(30);
  popMatrix();

  pushStyle();
  stroke(255);
  scene.drawPath(kfi, 5, 10);
  popStyle();

  for (int i=0; i<nbKeyFrames; ++i) {
    pushMatrix();
    kfi.keyFrame(i).applyTransformation(scene);

    if ( keyFrame[i].grabsInput() )
      scene.drawAxes(40);
    else
      scene.drawAxes(20);

    popMatrix();
  }
}

public void keyPressed() {
  if ((key == ENTER) || (key == RETURN))
    kfi.toggleInterpolation();
  if ( key == 'u')
    kfi.setInterpolationSpeed(kfi.interpolationSpeed()-0.25f);
  if ( key == 'v')
    kfi.setInterpolationSpeed(kfi.interpolationSpeed()+0.25f);
}