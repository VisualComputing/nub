class Arcal {
  float angle, raycercle, zcercle, decaler;
  PVector axe, ocentre, depart, arrivee;
  Quat quat;
  InteractiveFrame repere;
  String texte;

  Arcal (Quat q, PVector dep, PVector arr, String tex, float decal) {
    depart=dep;
    arrivee=arr;
    quat=q;
    axe=Scene.toPVector(quat.axis());
    angle=quat.angle();
    texte=tex;
    decaler=decal;
    ocentre=projectionSurDroite(depart, axe);
    zcercle=sqrt(sq(raySphere)-ocentre.dot(ocentre));
    raycercle=sqrt(sq(raySphere)-sq(zcercle));
    repere=new InteractiveFrame(scene);
    repere.setZAxis(Scene.toVec(axe));
    repere.setXAxis(Scene.toVec(comb(1, depart, -1, ocentre)));
    repere.setPosition(Scene.toVec(ocentre));
  }

  void draw() {
    pushMatrix();
    repere.applyTransformation();
    if (montretout) {
      fill(0, 255, 0, 50);
      stroke(255, 0, 0);
      ellipse(0, 0, 2*raycercle, 2*raycercle);
    }
    popMatrix();
    dessinerArc();
  }

  void dessinerArc() {
    stroke(255, 0, 0);
    strokeWeight(6);
    ligne(Scene.toPVector(repere.position()), or);
    strokeWeight(1);  
    pushMatrix();
    repere.applyTransformation();
    fill(215, 210, 255, 30);
    float r=raycercle;
    noStroke();
    fill(255, 0, 0);

    beginShape(QUAD_STRIP);
    for (int a=0;a<=100;a++) {
      float aa=angle/100*a;
      vertex(r*cos(aa), r*sin(aa), 3);
      vertex(r*cos(aa), r*sin(aa), 23);
    }
    endShape();

    fill(255, 250, 100);
    beginShape(TRIANGLE_FAN); 
    vertex(0, 0, 0);
    fill(255, 155, 150);
    for (int a=0;a<50;a++) {
      float aa=angle*a/50.0;
      vertex(r*cos(aa), r*sin(aa), 0);
    }
    vertex(r*cos(angle), r*sin(angle), 0);
    endShape();
    popMatrix();
    PVector postexte= Scene.toPVector(repere.inverseCoordinatesOf(new Vec(r*cos(angle/2.0), r*sin(angle/2.0), 0)));
    postexte=PVector.add(postexte, new PVector(decaler, decaler, 0));
    unText1(texte, postexte);
  }

  void init(Quat q, PVector dep, PVector arr) { 
    quat=q;
    axe=Scene.toPVector(quat.axis());
    angle=quat.angle();
    depart=dep;
    arrivee=arr;
    ocentre=projectionSurDroite(depart, axe);
    zcercle=ocentre.mag();
    raycercle=sqrt(sq(raySphere)-sq(zcercle));
    repere.setPosition(Scene.toVec(ocentre));
    repere.setZAxis(Scene.toVec(axe));
    repere.setXAxis(Scene.toVec(comb(1, depart, -1, ocentre)));
  }
}