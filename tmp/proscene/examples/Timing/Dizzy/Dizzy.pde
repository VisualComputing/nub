/**
 * Dizzy
 * by Jean Pierre Charalambos.
 *
 * This example demonstrates how 2D key frames may be used to perform a
 * Prezi-like presentation.
 *
 * The displayed eye path is defined by some interactive-frames which can
 * be moved with the mouse, making the path editable.
 *
 * The eye interpolating path is played with the shortcut '1'.
 *
 * Press CONTROL + '1' to add (more) key frames to the eye path.
 *
 * Press ALT + '1' to delete the eye path.
 *
 * Note that the eye actually holds 3 paths, bound to the [1..3] keys.
 * Pressing CONTROL + [1..3] adds key frames to the specific path.
 * Pressing ALT + [1..3] deletes the specific path. Press 'r' to display
 * all the key frame eye paths (if any). The displayed paths are editable.
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.geom.*;

Scene scene;
PImage img;
Info toggleInfo;
ArrayList<Info> info;
PFont font;
InteractiveFrame message1;
InteractiveFrame message2;
InteractiveFrame image;
float h;
int fSize = 16;

public void setup() {
  size(640, 360, P2D);

  img = loadImage("dizzy.jpg");
  scene = new Scene(this);
  scene.setGridVisualHint(false);
  scene.setAxesVisualHint(false);

  message1 = new InteractiveFrame(scene);
  message2 = new InteractiveFrame(scene);
  image = new InteractiveFrame(scene);

  message1.setPosition(33.699852f, -62.68051f);
  message1.setOrientation(new Rot(-1.5603539f));
  message1.setMagnitude(0.8502696f);

  message2.setPosition(49.460827f, 74.67359f);
  message2.setOrientation(new Rot(-1.533576f));
  message2.setMagnitude(0.3391391f);

  image.setPosition(-314.30075f, -165.1348f);
  image.setOrientation(new Rot(-0.0136114275f));
  image.setMagnitude(0.07877492f);

  // create a camera path and add some key frames:
  // key frames can be added at runtime with keys [j..n]
  scene.loadConfig();
  scene.loadConfig();
  font = loadFont("FreeSans-24.vlw");
  toggleInfo = new Info(new PVector(10, 7), font);
  info = new ArrayList<Info>();
  for (int i=0; i<3; ++i)
    info.add(new Info(new PVector(10, toggleInfo.height*(i+1) + 7*(i+2)), font, String.valueOf(i+1)));
}

public void draw() {
  background(0);
  fill(204, 102, 0);

  pushMatrix();
  image.applyTransformation();// optimum
  image(img, 0, 0);
  popMatrix();

  pushMatrix();
  message1.applyTransformation();// optimum
  text("I'm useless", 10, 50);
  popMatrix();

  fill(0, 255, 0);

  pushMatrix();
  message2.applyTransformation();// optimum
  text("but I feel dizzy", 10, 50);
  popMatrix();

  info();
}

void info() {
  toggleInfo.setText("Camera paths edition "
                     + ( scene.pathsVisualHint() ? "ON" : "OFF" )
                     + " (press 'r' to toggle it)");
  toggleInfo.display();
  for (int i = 0; i < info.size(); i++)
    // Check if CameraPathPlayer is still valid
    if (scene.eye().keyFrameInterpolator(i+1) != null) {
      info.get(i).setText("Path " + String.valueOf(i+1) + "( press " + String.valueOf(i+1) + " to" +
                           (scene.eye().keyFrameInterpolator(i+1).numberOfKeyFrames() > 1 ?
                           scene.eye().keyFrameInterpolator(i+1).interpolationStarted() ?
                           " stop it)" : " play it)"
                           : " restore init position)"));
      info.get(i).display();
    }
}