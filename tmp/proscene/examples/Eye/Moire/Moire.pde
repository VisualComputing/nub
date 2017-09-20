/**
 * Moire.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates the camera and world coordinate systems relationship.
 * Three sets of lines (red, green, blue) are drawn. They have common end points,
 * located on a circle in the XY plane. However, they have different starting points
 * defined as follows: 1. The red lines start at the camera position and will hence
 * always be aligned with the viewing direction, making them invisible, unless you
 * change the camera projection to ORTHOGRAPHIC (press 'e' to do it); 2. The green
 * lines starting points are determined from the camera coordinate system and will
 * hence always be fixed on the screen; and, 3. The blue lines starting points are
 * defined in the world coordinate system, and will hence move in camera motion.
 * 
 * A nice Moire pattern can be obtained when rotating the camera.
 * 
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.geom.*;

Scene scene;

void setup() {
  size(640, 360, P3D);
  scene = new Scene(this);
  scene.setGridVisualHint(false);
  scene.setRadius(150);
  scene.showAll();
}

void draw() {
  background(0);
  final float nbLines = 50.0f;
  beginShape(LINES);
  for (float i=0; i<nbLines; ++i) {
    float angle = 2.0f*PI*i/nbLines;
    
    stroke(204, 51, 51);
    // These lines will never be seen as they are always aligned with the viewing direction.
    vertex(scene.camera().position().x(), scene.camera().position().y(), scene.camera().position().z());
    vertex(100*cos(angle), 100*sin(angle), 0);
    
    stroke(55, 204, 55);
    // World Coordinates are infered from the camera, and seem to be immobile in the screen.
    Vec tmp = scene.camera().worldCoordinatesOf(new Vec(30*cos(angle), 30*sin(angle), -200));
    vertex(tmp.x(), tmp.y(), tmp.z());
    vertex(100*cos(angle), 100*sin(angle), 0);
    
    stroke(55, 55, 204);
    // These lines are defined in the world coordinate system and will move with the camera.
    vertex(150*cos(angle), 150*sin(angle), -100);
    vertex(100*cos(angle), 100*sin(angle), 0);
  }
  endShape();
}