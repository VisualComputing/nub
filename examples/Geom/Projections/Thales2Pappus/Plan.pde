class Plan {
  InteractiveFrame repere;
  InteractiveFrame[] pointSensible;
  PVector[] pabs; 
  float posix, angle, largeur, longueur;
  PVector normale, direc;
  color col;
  float lambda2, lambda3;

  Plan(float posx, float an) {
    posix=posx;
    angle=an;
    longueur=-2.0f*posix;
    largeur=40;
    normale=new PVector(0, 0, 1);
    col=color(55, 55, 100, 254);
    repere=new InteractiveFrame(scene);
    repere.setTranslation(posx, 0, 12);
    repere.setRotation(new Quat(new Vec(1, 0, 0), angle));
    repere.setConstraint(contrainteX);
    init();
  }

  void draw() {
    actualiser();
    
    pushMatrix();
    repere.applyTransformation();
    normale=  Scene.toPVector(repere.inverseTransformOf(new Vec(0, 0, 1)));
    balle(1);
    fill(255);
    stroke(255);

    scene.drawAxes(20);
    rectangle(col, 0, 0, longueur, largeur );
    rectanglesoleil();
    for (int i=0;i<7;i++) {
      pushMatrix();
      pointSensible[i].applyTransformation();
      balle(i);

      popMatrix();
    }
    figure0();
    pushMatrix();
    rotateY(PI);
    rotateX(PI);
    rotateY(PI);
    fill(255);
    stroke(255);
    text("THALES ", 50, -5, 2);
    popMatrix();
    popMatrix();

    projetantes();
    figure1();
  }

  void figure0() {
    stroke(180, 180, 255);
    strokeWeight(3); 
    ligne(pst(1), pst(4));
    ligne(pst(2), pst(6));
    stroke(255, 255, 0);
    ligne(pst(2), pst(5));
    ligne(pst(3), pst(4));
    stroke(255, 255, 255);
    ligne(pst(1), pst(5));
    ligne(pst(3), pst(6));
    strokeWeight(1); 
    stroke(255, 0, 0);
    ligne(pst(1), pst(0));
    ligne(pst(5), pst(0));
  } 

  void projetantes() {
    PVector pv;    
    for (int i=0;i<7;i++) {
      pv=Scene.toPVector(repere.inverseCoordinatesOf(pointSensible[i].translation()));
      pabs[i]=projection(pv);
    }
    strokeWeight(3);
    stroke(0, 0, 255);   
    ligne(pabs[1], pabs[4]);
    ligne(pabs[2], pabs[6]);
    stroke(255, 255, 0);
    ligne(pabs[2], pabs[5]);
    ligne(pabs[3], pabs[4]);
    stroke(255, 255, 255);
    ligne(pabs[1], pabs[5]);
    ligne(pabs[3], pabs[6]);
    strokeWeight(1);
  }  

  void figure1() {
    PVector a2=intersectionDroite(pabs[2], pabs[5], pabs[3], pabs[4]);
    PVector   b2=intersectionDroite(pabs[1], pabs[5], pabs[3], pabs[6]);
    PVector c2= intersectionDroite(pabs[2], pabs[6], pabs[1], pabs[4]);
    stroke(255, 255, 0); 
    ligne(a2, pabs[2]);
    ligne(a2, pabs[3]);
    stroke(255, 255, 255); 
    ligne(b2, pabs[1]);
    ligne(b2, pabs[3]);
    stroke(0, 0, 255);
    ligne(c2, pabs[1]);
    ligne(c2, pabs[2]);
    strokeWeight(3);
    stroke(255, 0, 0);
    ligne(c2, b2);
    ligne(a2, b2);
    strokeWeight(1);
    stroke(255, 0, 0);
    ligne(pabs[1], pabs[0]);
    ligne(pabs[5], pabs[0]);
  }  
  
  void setLargeur(float lar) {
    largeur=lar;
  }
  
  void setLongueur(float lar) {
    longueur=lar;
  }
  
  void setCouleur(color c) { 
    col=c;
  }
  
  void getNormaleDansWorld() {
    normale=  Scene.toPVector(repere.inverseTransformOf(new Vec(0, 0, 1)));
  }

  void init() { 
    pabs=new PVector[7];   
    pointSensible= new InteractiveFrame[7];
    for (int i=0;i<7;i++)
      pointSensible[i]=new InteractiveFrame(scene, repere);
    pointSensible[0].setConstraint(libreT);
    pointSensible[1].setConstraint(libreT);
    pointSensible[2].setConstraint(axial);
    pointSensible[3].setConstraint(axial);
    pointSensible[4].setConstraint(libreT);
    pointSensible[5].setConstraint(fixe);
    pointSensible[6].setConstraint(fixe);

    pointSensible[0].setTranslation(new Vec(70, 20, 0));//S
    pointSensible[1].setTranslation(new Vec(20, 30, 0));//A
    lambda2=0.85;
    lambda3=0.72;
    pointSensible[2].setTranslation(Scene.toVec(barycentre(lambda2, pst(1), pst(0))));//B
    pointSensible[3].setTranslation(Scene.toVec(barycentre(lambda3, pst(1), pst(0))));//C
    pointSensible[4].setTranslation(new Vec(25, 8, 0));//B1

    replaceC1A1();

    direc=comb(1, pst(1), -1, pst(0));
    axial.setTranslationConstraint(AxisPlaneConstraint.Type.AXIS, Scene.toVec(direc));
  }

  void actualiser() {
    PVector adirec=direc.get();
    direc=comb(1, pst(1), -1, pst(0));
    adirec.normalize();
    direc.normalize();
    PVector diff=PVector.sub(direc, adirec);
    if (diff.mag()>1.0E-6) {
      pointSensible[2].setTranslation(Scene.toVec(comb(lambda2, pst(1), 1.0-lambda2, pst(0)))); 
      pointSensible[3].setTranslation(Scene.toVec(comb(lambda3, pst(1), 1.0-lambda3, pst(0))));  

      axial.setTranslationConstraint(AxisPlaneConstraint.Type.AXIS, Scene.toVec(direc));
      pointSensible[2].setConstraint(axial);
      pointSensible[3].setConstraint(axial);
    }
    PVector f2f0=PVector.sub( pst(2), pst(0));
    PVector  f1f0=PVector.sub( pst(1), pst(0));
    lambda2=(f2f0.dot(f1f0))/(f1f0.dot(f1f0));
    f2f0=PVector.sub( pst(3), pst(0));
    lambda3=(f2f0.dot(f1f0))/(f1f0.dot(f1f0));
    replaceC1A1();
  }

  PVector pst(int i) {
    return Scene.toPVector(pointSensible[i].translation());
  }
  
  void replaceC1A1() {
    PVector dir1=comb(1, pst(4), -1, pst(3));
    PVector ww=intersectionDroiteDir2(pst(4), pst(0), pst(2), dir1);//C1
    pointSensible[5].setTranslation(Scene.toVec(ww));//C1

    dir1=comb(1, pst(4), -1, pst(1));
    ww=intersectionDroiteDir2(pst(4), pst(0), pst(2), dir1);//A1
    pointSensible[6].setTranslation(Scene.toVec(ww));//A1
  }

  void rectanglesoleil() {
    PVector soleilrep = Scene.toPVector(repere.coordinatesOf(soleil.position()));//coordonnées de soleil dans repere
    PVector ptr=  comb(1, soleilrep, 1, new PVector(0, -10, 0));//dans repere un point à projeter 
    PVector sr =Scene.toPVector(repere.inverseCoordinatesOf(Scene.toVec(ptr)));//dans absolu le poinr à projeter
    sr = Scene.toPVector(repere.coordinatesOf(Scene.toVec(projection(sr, false))));
    fill(0, 255, 255, 150);
    beginShape();
    vertex(soleilrep.x+largeur, soleilrep.y, soleilrep.z);
    vertex(soleilrep.x-largeur, soleilrep.y, soleilrep.z);
    vertex(sr.x-largeur, sr.y, sr.z);
    vertex(sr.x+largeur, sr.y, sr.z);
    endShape(CLOSE);
  }
}
