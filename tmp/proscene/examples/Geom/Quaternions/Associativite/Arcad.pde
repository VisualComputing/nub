class Arcad {
  float angle, raycercle, zcercle;
  PVector axe, vec2Phi, ocentre, depart, arrivee;
  Quat quat;
  InteractiveFrame repere, dragueur, rotateur;
  String texte;

  Arcad (PVector ax, PVector dep, float ang, String tex) {
    depart=dep;
    axe=ax;
    angle=ang;
    axe.normalize();
    texte=tex;
    ocentre=projectionSurDroite(dep, axe);
    zcercle=sqrt(sq(raySphere)-ocentre.dot(ocentre));
    raycercle=sqrt(sq(raySphere)-sq(zcercle));
    vec2Phi=new PVector();       
    repere=new InteractiveFrame(scene);
    dragueur=new InteractiveFrame(scene);  
    rotateur=new InteractiveFrame(scene, repere);
    dragueur.setConstraint(drag);
    rotateur.setConstraint(planaire);
    repere.setZAxis(Scene.toVec(axe));
    repere.setXAxis(Scene.toVec(comb(1, depart, -1, ocentre)));
    repere.setPosition(Scene.toVec(ocentre));
    dragueur.setPosition(Vec.multiply(repere.position(), 1.2));
    rotateur.setTranslation(new Vec(raycercle*cos(angle/2.0), raycercle*sin(angle/2.0), 0));
    quat=new Quat(Scene.toVec(axe), angle);
  }

  void draw() {
    init();  

    pushMatrix();//--------------------------------------------------
    repere.applyTransformation();
    directionalLight(255, 200, 200, 1, -1, -2);

    if (montretout) {
      fill(255, 250, 255, 50);
      stroke(0);
      ellipse(0, 0, 2*raycercle, 2*raycercle);
    }

    rotateur.setTranslation(Scene.toVec(normaliser(Scene.toPVector(rotateur.translation()), raycercle)));//replacer rotateur sur le Cercle
    float co=rotateur.translation().x()/raycercle;
    float si=rotateur.translation().y()/raycercle;
    float angleQuat=acos(co);
    if (si<0) angleQuat=-angleQuat;
    angle=2.0*angleQuat;
    vec2Phi=new PVector(raycercle*cos(angle), raycercle*sin(angle), 0);

    rotateur.applyTransformation();
    fill(255, 255, 0); 
    noStroke();  
    sphere(15);
    popMatrix();//------------------------------------------------------  
    unText1(texte, Scene.toPVector(rotateur.position()));

    pushMatrix();//+++++++++++++++++++++++++++++++++++++++++++++++++++++
    float lon=dragueur.position().magnitude();
    if (lon<raySphere) dragueur.setPosition(Vec.multiply(dragueur.position(), raySphere/lon));
    dragueur.applyTransformation();
    fill(255, 0, 255);
    sphere(15);
    popMatrix(); //+++++++++++++++++++++++++++++++++++++++++++++++++++++

    stroke(255);
    strokeWeight(6);
    ligne(Scene.toPVector(dragueur.position()), or);
    strokeWeight(1);  
    dessinerArc(angleQuat, color(250, 60, 170));
    arrivee= Scene.toPVector(repere.inverseCoordinatesOf(Scene.toVec(vec2Phi)));
    stroke(255);
    //(or,arrivee);
    quat=new Quat(Scene.toVec(axe), angle);
  }

  void dessinerArc(float angleQnion, color c) {
    float anglArc=2*angleQnion;
    //texte ecran
    PVector posWorld=Scene.toPVector(repere.inverseCoordinatesOf(new Vec(raycercle, 0, 0)));
    unText1("depart de " +texte, posWorld);
    pushMatrix();//--------------------------------------------------
    repere.applyTransformation();
    fill(215, 210, 255, 30);
    float r=raycercle;
    noStroke();
    fill(0, 0, 255);
    pushMatrix();//--------
    translate(r, 0, 0);
    box(30);
    popMatrix();//---------
    beginShape(QUAD_STRIP); 
    fill(0, 255, 0);
    for (int a=0;a<=100;a++) {
      float aa=anglArc/100*a;
      vertex(r*cos(aa), r*sin(aa), 23);
      vertex(r*cos(aa), r*sin(aa), 3);
    }
    endShape();

    fill(255, 0, 155);
    beginShape(TRIANGLE_FAN); 
    vertex(0, 0, 0);
    fill(255, 255, 155, 100);
    for (int a=0;a<50;a++) {
      float aa=anglArc*a/50.0;
      float bb=(-TWO_PI+anglArc)*a/50.0;
      vertex(r*cos(aa), r*sin(aa), 0);
    }
    vertex(r*cos(anglArc), r*sin(anglArc), 0);
    endShape();

    pushMatrix();
    rotateZ(anglArc);
    translate(r, 0, 0);
    fill(255);
    float angleRot=2.0*angleQnion; 
    if (angleRot>TWO_PI) angleRot-=TWO_PI;
    if (anglArc<0) {
      rotateX(HALF_PI);
    }
    else { 
      rotateX(-HALF_PI);
    }
    fill(255, 55, 55);
    scene.drawCone(20, 20);
    popMatrix();
    popMatrix();//--------------
  }

  void init() {    
    axe=Scene.toPVector(dragueur.position());
    axe.normalize();
    ocentre=projectionSurDroite(depart, axe);
    zcercle=ocentre.mag();
    raycercle=sqrt(sq(raySphere)-sq(zcercle));
    repere.setPosition(Scene.toVec(ocentre));
    repere.setZAxis(Scene.toVec(axe));
    repere.setXAxis(Scene.toVec(comb(1, depart, -1, ocentre)));
  }
}
