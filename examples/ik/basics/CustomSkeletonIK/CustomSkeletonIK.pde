/**
 * CustomSkeleton IK
 * by Sebastian Chaparro.
 *
 * This example illustrates how to create a custom skeleton structure using Skeleton class.
 * It is based on the Luxo example. 
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;
import nub.core.constraint.*;

//this packages are required for ik behavior
import nub.ik.animation.*;
import nub.ik.solver.*;

int w = 1200;
int h = 1200;

Scene scene;
float length = 50;
//Skeleton structure defined above
Skeleton skeleton;

void settings() {
    size(w, h, P3D);
}

void setup() {
    //Setting the scene
    scene = new Scene(this);
    scene.setRadius(200);
    scene.fit(1);
    //1. Create the Skeleton (Luxo Lamp)
    skeleton = new Skeleton(scene);
    
    Piece base = new Piece();
    base.mode = 1;
    skeleton.addJoint("Base", base);
    
    Piece arm1 = new Piece();
    skeleton.addJoint("Arm 1", "Base", arm1);
    arm1.mode = 2;
    arm1.setTranslation(0, 0, 8); // Base height
    arm1.setRotation(new Quaternion(new Vector(1, 0, 0), 0.6));
    
    Piece arm2 = new Piece();
    skeleton.addJoint("Arm 2", "Arm 1", arm2);
    arm2.mode = 2;
    arm2.setTranslation(0, 0, 50); // Arm length
    arm2.setRotation(new Quaternion(new Vector(1, 0, 0), -2));

    Piece end = new Piece();
    skeleton.addJoint("End", "Arm 2", end);
    end.mode = 3;
    end.setTranslation(0, 0, 50); // Arm length
    end.setRotation(new Quaternion(new Vector(1, -0.3, 0), -1.7));
    
    //Set constraints
    Hinge h0 = new Hinge(radians(180), radians(180), base.rotation().get(), arm1.translation(), new Vector(0,0,1));
    h0.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vector(0, 0, 1));
    base.setConstraint(h0);
    Hinge h1 = new Hinge(radians(140), radians(5), arm1.rotation().get(), arm2.translation(), new Vector(1,0,0));
    Hinge h2 = new Hinge(radians(30), radians(30), arm2.rotation().get(), end.translation(), new Vector(1,0,0));
    arm1.setConstraint(h1);
    arm2.setConstraint(h2);

    LocalConstraint headConstraint = new LocalConstraint();
    headConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vector(0.0f, 0.0f, 0.0f));
    end.setConstraint(headConstraint);

    //2. Enable IK functionallity
    skeleton.enableIK();
    //3. Lets create a Targets related with the end of the lamp. 
    skeleton.addTarget("End");
    //4. if desired you could set the target position and orientation to be the same as the leaves of the structure 
    skeleton.restoreTargetsState();
}

void draw() {
  background(0);
  lights();

  //draw the lamp
  scene.render();

  //draw the ground
  noStroke();
  fill(120, 120, 120);
  float nbPatches = 100;
  normal(0, 0, 1);
  for (int j = 0; j < nbPatches; ++j) {
    beginShape(QUAD_STRIP);
    for (int i = 0; i <= nbPatches; ++i) {
      vertex((200 * (float) i / nbPatches - 100), (200 * j / nbPatches - 100));
      vertex((200 * (float) i / nbPatches - 100), (200 * (float) (j + 1) / nbPatches - 100));
    }
    endShape();
  }
}

void mouseMoved() {
    scene.mouseTag();
}

void mouseDragged() {
    if (mouseButton == LEFT){
        scene.mouseSpin();
    } else if (mouseButton == RIGHT) {
        scene.mouseTranslate();
    } else {
        scene.scale(mouseX - pmouseX);
    }
}

void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
        if (event.getButton() == LEFT)
            scene.focus();
        else
            scene.align();
}
