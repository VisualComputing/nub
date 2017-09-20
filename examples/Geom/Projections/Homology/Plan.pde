class Plan {
  InteractiveFrame repere;    
  float posix, angle, largeur, longueur;
  PVector normale;
  color col;

  Plan(float posx, float an) {
    posix=posx;
    angle=an;
    longueur=-2.0f*posix;
    largeur=200;
    normale=new PVector(0, 0, 1);
    col=color(55, 55, 100, 254);
    repere=new InteractiveFrame(scene);
    repere.setTranslation(posx, 0, 0);
    repere.setRotation(new Quat(new Vec(1, 0, 0), angle));
    repere.setConstraint(contrainteX);
  }

  void draw() {
    pushMatrix();
    repere.applyTransformation();
    normale=Scene.toPVector(repere.inverseTransformOf(new Vec(0, 0, 1)));
    noStroke();
    fill(255, 05, 05);
    sphere(7);
    translate(0, 0, -3);
    rectangle(col, 0, 0, longueur, largeur );
    popMatrix();
  }
  
  void setLargeur(float lar) {
    largeur=lar;
  }
  
  void setCouleur(color c) { 
    col=c;
  }
  
  void getNormaleDansWorld() {
    normale=Scene.toPVector(repere.inverseTransformOf(new Vec(0, 0, 1)));
    println("normale au plan =  "+ normale.x+"   "+ normale.y+"   "+ normale.z);
  }
}
