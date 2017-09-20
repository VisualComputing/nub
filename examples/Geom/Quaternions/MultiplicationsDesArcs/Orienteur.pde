class Orienteur {
  PVector vorient;
  InteractiveFrame repere;
  boolean estChange;
  color col1=color(0, 255, 255);
  color col2=color(255, 0, 255);
  int facteur;
  
  Orienteur() {
    vorient=new PVector();
    repere=new InteractiveFrame(scene);
    estChange=false;
    facteur=1;
  }
  
  Orienteur(PVector vo, PVector po) {
    vorient=vo;
    repere=new InteractiveFrame(scene);
    repere.setPosition(Scene.toVec(po));
    estChange=false;
    facteur=1;
  }
  
  void oppose() {
    facteur*=-1;
    vorient.mult(facteur);
  }
  
  void place(PVector p1, PVector p2, PVector v) {
    repere.setPosition(Scene.toVec(comb(0.5, p1, 0.5, p2)));
    vorient=PVector.mult(v, facteur);
  }

  void draw() {
    pushMatrix();
    if (repere.grabsInput()) {
      if (!estChange) {
        color trans=col1;
        col1=col2;
        col2=trans;
        oppose();
        estChange=true;
      }
    }
    else {
      estChange=false;
    }
    repere.applyTransformation();

    fill(col1);
    noStroke();
    sphere(10);
    popMatrix();
    stroke(205);
    strokeWeight(6);
    ligne(Scene.toPVector(repere.position()), comb(1, Scene.toPVector(repere.position()), 70, vorient));
    strokeWeight(1);
  }
}