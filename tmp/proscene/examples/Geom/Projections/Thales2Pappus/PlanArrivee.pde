class PlanArrivee {
  InteractiveFrame repere;

  float posix, angle, largeur, longueur;
  PVector normale, direc;
  color col;
  float lambda2, lambda3;

  PlanArrivee(float posx, float an) {
    posix=posx;
    angle=an;
    longueur=200;
    largeur=320;
    normale=new PVector(0, 0, 1);
    col=color(130, 100, 120, 254);
    repere=new InteractiveFrame(scene);
    repere.setTranslation(posx, 0, 0);
    repere.setRotation(new Quat(new Vec(1, 0, 0), angle));
    repere.setConstraint(contrainteX);
  }

  void draw() {
    pushMatrix();
    repere.applyTransformation();  
    rectangle(col, -longueur, 0, longueur, largeur );
    balle(2);
    text("PAPPUS", 70, 70, 4);   
    popMatrix();
  }

  void getNormaleDansWorld() {
    normale=  Scene.toPVector(repere.inverseTransformOf(new Vec(0, 0, 1)));
  }
}
