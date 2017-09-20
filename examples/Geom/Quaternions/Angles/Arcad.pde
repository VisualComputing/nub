class Arcad {
  float angle;
  PVector axe, vec2Phi, sym;
  Quat quat;
  InteractiveFrame repere, dragueur, rotateur;
  Arcad (PVector ax) {
    axe=ax;
    // angle=a;
    vec2Phi=new PVector();
    repere=new InteractiveFrame(scene);
    dragueur=new InteractiveFrame(scene);
    rotateur=new InteractiveFrame(scene, repere);
    dragueur.setConstraint(drag);
    rotateur.setConstraint(planaire);

    //placements
    repere.setOrientation(new Quat(new Vec(1, 0, 0), -PI/4));
    repere.setPosition(Scene.toVec(axe));
    dragueur.setPosition(new Vec(1.2*axe.x, 1.2*axe.y, 1.2*axe.z));
    rotateur.setTranslation(new Vec(200*cos(QUARTER_PI), 200*sin(QUARTER_PI), 0));
  }

  void draw() {
    stroke(0);
    scene.drawAxes(200);
    ligne(Scene.toPVector(dragueur.position()), or);
    ligne(Scene.toPVector(repere.position()), Scene.toPVector(rotateur.position()));

    noStroke();
    pushMatrix();
    repere.applyTransformation();
    stroke(0);
    line(0, 0, 0, 200, 0, 0);
    box(8);
    fill(50, 150, 255, 200);
    translate(0, 0, -3);
    ellipse(0, 0, 400, 400);
    translate(0, 0, 3);

    stroke(255, 0, 0);
    ligne(or, vec2Phi);

    rotateur.setTranslation(Scene.toVec(normaliser(Scene.toPVector(rotateur.translation()), 200)));
    sym=PVector.mult(Scene.toPVector(rotateur.translation()), -2);
    rotateur.applyTransformation();
    ligne(sym, or);
    fill(255, 255, 0);
    noStroke();
    sphere(15);
    scene.drawAxes(100);
    translate(sym.x, sym.y, sym.z);
    fill(255);
    sphere(15);
    scene.drawAxes(100);
    sym=Scene.toPVector(rotateur.inverseCoordinatesOf(Scene.toVec(sym)));
    popMatrix();

    pushMatrix();
    dragueur.applyTransformation();
    fill(255, 0, 0);
    sphere(7);
    popMatrix();

    PVector w= Scene.toPVector(dragueur.translation().get());
    w.normalize();
    w.mult(lon);
    repere.setPosition(Scene.toVec(w));
    w.get().normalize();
    repere.setZAxis(Scene.toVec(w));

    float co=rotateur.translation().x()/200.0;
    float si=rotateur.translation().y()/200.0;
    float angleQuat=acos(co);
    if (si<0) angleQuat=TWO_PI-angleQuat;
    //println(angleQuat);
    vec2Phi=new PVector(200.0*(sq(co)-sq(si)), 400.0*co*si, 0);
    dessinerArc(angleQuat, color(250, 60, 170));
  }

  void dessinerArc(float angleQnion, color c) {
    float anglArc=2*angleQnion;
    if (angleQnion>PI) anglArc=2*(angleQnion-2.0*PI);
    if (angleQnion<-PI) anglArc=2*(angleQnion+2*PI);
    pushMatrix();
    repere.applyTransformation();
    float r=200;

    noStroke();
    fill(0, 0, 255);
    pushMatrix();
    translate(r, 0, 0);
    box(18);
    popMatrix();
    beginShape(QUAD_STRIP);

    for (int a=0;a<=100;a++) {
      float aa=anglArc/100*a;
      vertex(r*cos(aa), r*sin(aa), -10);
      vertex(r*cos(aa), r*sin(aa), 10);
    }
    endShape();

    beginShape(TRIANGLE_FAN);
    fill(150, 220, 0, 254);
    vertex(0, 0, 0);
    fill(100, 255, 250, 254);
    for (int a=0;a<100;a+=4) {
      float aa=anglArc*a/100.0;
      vertex(r*cos(aa), r*sin(aa), 0);
    }
    vertex(r*cos(anglArc), r*sin(anglArc), 0);
    endShape();
    pushMatrix();
    rotateZ(anglArc);
    translate(r, 0, 0);
    fill(0);
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
    popMatrix();
    float angleQnion1=(-TWO_PI+ anglArc)/2.0;
    unText1("  angle du quaternion 2: "+ String.format("%.0f", degrees(angleQnion1))+"°", sym);
    unText1("  angle du quaternion 2: "+ String.format("%.0f", degrees(angleQnion))+"°", Scene.toPVector(rotateur.position()));
    PVector extr= Scene.toPVector(repere.inverseCoordinatesOf(new Vec(r*cos(angleRot), r*sin(angleRot), 0)));
    unText1("point  B = rot( A ) ", extr);
    unText1("  angle de la rotation : "+ String.format("%.0f", degrees(angleRot))+"°", extr.x+20, extr.y+20, extr.z+50);
    unText1("mesure de l'arc = "+String.format("%.0f", degrees(anglArc))+"°", extr.x+40, extr.y+40, extr.z+100);
    extr= Scene.toPVector(repere.inverseCoordinatesOf(new Vec(r, 0, 0)));
    unText1(" point A ", extr);
  }
}
