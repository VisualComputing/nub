class Interpolation {
  InteractiveFrame keyFrame[];
  KeyFrameInterpolator kfi;
  int nbKeyFrames;  
  boolean enmarche;

  Interpolation( PVector but, Quat qbut) {
    kfi = new KeyFrameInterpolator(scene);
    nbKeyFrames = 3;
    keyFrame=new  InteractiveFrame[nbKeyFrames];
    enmarche=false;

    for (int i=0; i<nbKeyFrames; i++) {
      keyFrame[i] = new InteractiveFrame(scene);
      kfi.addKeyFrame(keyFrame[i]);
    }
    keyFrame[0].setPosition(new Vec(random(500, 1500), random(2000, 5000), -500));
    keyFrame[0].setOrientation(new Quat(new Vec(random(-3, 3), random(-3, 3), random(-3, 3)), random(-2, 3.2))); 
    keyFrame[1].setPosition(new Vec(random(-500, 500), 300+random(100, 100), random(1800, 2400))); 
    keyFrame[1].setOrientation(new Quat(new Vec(random(-3, 3), random(-3, 3), random(-3, 3)), random(2, 6.2))); 
    keyFrame[2].setPosition(new Vec(but.x,but.y,but.z));
    keyFrame[2].setOrientation(qbut);
    kfi.setLoopInterpolation(false);
    kfi.setInterpolationSpeed(0.5);
  }
  
  void dessin(float dx, float dy, float dz, int typ) {
    if (!kfi.interpolationStarted()&& !enmarche) { 
      kfi.startInterpolation();
      enmarche=true;
    }
    fill(255); 
    pushMatrix();
    scene.applyTransformation(kfi.frame());
    switch(typ) {
    case 0:// les boites
      box(dx, dy, dz);
      break;
    case 1: //les cylindres
      noStroke();
      scene.drawCylinder(dx, dy);
      break;
    }
    popMatrix();
  }
}