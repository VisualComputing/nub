public class Figure {
  int npts=8;
  InteractiveFrame[] pts;
  Vec[] projetes;
  Vec pointA, pointA0, pointB, pointB0;
  AxisPlaneConstraint  axial13, axial15, planaireY, axialA, axialB;
  float rap13, rap15, rapA, rapB;
  Cycle cykle0, cykle1, cykle2;

  Figure() {
    rap13=0.7;
    rap15=1.4;
    rapA=0.5;
    rapB=0.5;

    pts=new InteractiveFrame[npts];
    projetes=new Vec[npts];
    for (int i=0; i<npts; i++) {
      pts[i]=new InteractiveFrame(scene);
    }

    planaireY=new WorldConstraint();
    planaireY.setRotationConstraintType(AxisPlaneConstraint.Type.FORBIDDEN);
    planaireY.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vec( 0.0f, 1.0f, 0.0f));

    axial13=new WorldConstraint();
    axial13.setRotationConstraintType(AxisPlaneConstraint.Type.FORBIDDEN);
    axial13.setTranslationConstraintType(AxisPlaneConstraint.Type.AXIS);
    axial15=new WorldConstraint();
    axial15.setRotationConstraintType(AxisPlaneConstraint.Type.FORBIDDEN);
    axial15.setTranslationConstraintType(AxisPlaneConstraint.Type.AXIS);
    axialA=new WorldConstraint();
    axialA.setRotationConstraintType(AxisPlaneConstraint.Type.FORBIDDEN);
    axialA.setTranslationConstraintType(AxisPlaneConstraint.Type.AXIS);
    axialB=new WorldConstraint();
    axialB.setRotationConstraintType(AxisPlaneConstraint.Type.FORBIDDEN);
    axialB.setTranslationConstraintType(AxisPlaneConstraint.Type.AXIS);  

    pts[0].setPosition(0, 0, 400);
    pts[1].setPosition(0, -400, 400);
    pts[2].setPosition(250, 0, 100);
    pts[3].setPosition(comb(1.0, pts[1].position(), rap13, Vec.subtract(pts[2].position(), pts[0].position())));
    pts[4].setPosition(-230, 0, 250);
    pts[5].setPosition(comb(1.0, pts[1].position(), rap15, Vec.subtract(pts[4].position(), pts[0].position())));
    pts[6].setPosition(barycentre(rapA, pts[2].position(), pts[3].position()));
    pts[7].setPosition(barycentre(rapB, pts[4].position(), pts[5].position()));

    pts[0].setConstraint(planaireY);
    pts[2].setConstraint(planaireY);
    pts[4].setConstraint(planaireY);
    pts[1].setConstraint(planaireY);
    pts[3].setConstraint(axial13);
    pts[5].setConstraint(axial15); 
    pts[6].setConstraint(axialA); 
    pts[7].setConstraint(axialB);

    cykle0=new Cycle(0.6);
    cykle1=new Cycle(0.65);
    cykle2=new Cycle(0.4);
  }
  //fin du constructeur

  void draw() {
    axial13.setTranslationConstraintDirection(soustraire(0, 2));
    axial15.setTranslationConstraintDirection(soustraire(0, 4)); 
    axialA.setTranslationConstraintDirection(soustraire(2, 3)); 
    axialB.setTranslationConstraintDirection(soustraire(4, 5)); 

    replace3();
    replace5(); 
    replaceA();
    replaceB();
    fromTo(pointA0, pointA, color(105, 100, 255));
    fromTo(pointB0, pointB, color(105, 100, 255));

    directionalLight(255, 255, 255, -1, 0, -1);
    directionalLight(255, 255, 0, 0, -1, -0.5);
    for (int i=0; i<npts; i++) {
      pushMatrix();
      pts[i].applyTransformation();
      fill(#ff0000);
      noStroke();
      sphere(5);
      popMatrix();
    }

    for (int i=0; i<npts; i++) {
      projetes[i]=new Vec(pts[i].position().x(), pts[i].position().y(), 0);
    }

    stroke(255); 
    strokeWeight(2.8);
    cykle0.actualiser(0.6*abs(cos(temps))+0.37);
    cykle1.actualiser(0.65*abs(sin(temps*0.5))-0.32);
    cykle2.actualiser(0.8*abs(cos(-temps)));
    strokeWeight(1.0);
    noStroke();
    fromTo(projetes[0], projetes[1], color(255, 0, 0));
    fromTo(projetes[2], projetes[3], color(255, 0, 0));
    fromTo(projetes[4], projetes[5], color(255, 0, 0));
    quad4(new Vec(-350, 200, 0), new Vec(-350, -700, 0), new Vec(350, -700, 0), new Vec(350, 200, 0), color(55, 0, 255, 200), color(100, 100, 255, 200));
    quad4(pts[0].position(), pts[1].position(), pts[3].position(), pts[2].position(), color(255, 0, 0, 100), color(255, 200, 0, 100));
    quad4(pts[0].position(), pts[1].position(), pts[5].position(), pts[4].position(), color(155, 200, 5, 100), color(255, 250, 55, 100)); 
    quad4(projetes[0], pts[0].position(), pts[1].position(), projetes[1], color(180, 255, 255, 100), color(80, 0, 255, 100));
    quad4(projetes[2], pts[2].position(), pts[3].position(), projetes[3], color(180, 255, 255, 100), color(80, 0, 255, 100));
    quad4(projetes[4], pts[4].position(), pts[5].position(), projetes[5], color(180, 255, 255, 100), color(80, 0, 255, 100));
  }

  void replace3() {
    if (parallele(0, 2, 1, 3))  rap13= calculRap(0, 2, 1, 3);
      pts[3].setPosition(comb(1, pts[1].position(), rap13, soustraire(0, 2)));
  } 
  
   void replace5() {
    if (parallele(0, 4, 1, 5)) rap15= calculRap(0, 4, 1, 5);
     pts[5].setPosition(comb(1, pts[1].position(), rap15, soustraire(0, 4)));
  } 
  
  void replaceA() {
    if (aligne(2, 6, 3)) { 
      rapA=calculRap3(2, 6, 3);
    }
    pts[6].setPosition(barycentre(rapA, pts[2].position(), pts[3].position()));
    pointA0=new Vec(pts[6].position().x(), pts[6].position().y(), 0);
    pointA=interPlan(0, 1, 4, pointA0);
  } 
  
  void replaceB() {
    if (aligne(4, 7, 5)) { 
      rapB= calculRap3(4, 7, 5);
    }
    pts[7].setPosition(barycentre(rapB, pts[4].position(), pts[5].position()));
    pointB0=new Vec(pts[7].position().x(), pts[7].position().y(), 0);
    pointB=interPlan(0, 1, 2, pointB0);
  }
  
  Vec inter2Droites(int n0, int n1, int m0, int m1) {
    Vec ad=soustraire(n0, n1);
    Vec bd=soustraire(m0, m1);
    Vec ap=pts[n0].position();
    Vec bp=pts[m0].position();
    Vec ab=Vec.subtract(bp, ap);
    float lambda = determinant(ab, ad)/determinant(bd, ad);
    return comb(1, bp, -lambda, bd);
  } 
  
  float  calculRap3(int a, int m, int b) {  
    Vec ab=soustraire( a, b);
    Vec  am=soustraire( a, m);

    return (am.dot(ab))/(ab.dot(ab));
  }

  float  calculRap(int a, int b, int m, int r) {  
    Vec ab=soustraire( b, a);
    Vec  mr=soustraire( r, m);
    return (mr.dot(ab))/(ab.dot(ab));
  }

  Vec soustraire(int n, int m) {
    return Vec.subtract(pts[m].position(), pts[n].position()) ;
  }

  boolean aligne(int n0, int n1, int n2) {
    float d=determinant(soustraire( n1, n0), soustraire(n0, n2));
    return (abs(d)<1.01);
  } 

  boolean parallele(int n0, int n1, int n2, int n3) {
    float d=determinant(soustraire( n1, n0), soustraire(n3, n2));
    return (abs(d)<1.01);
  } 

  void quad4(Vec a, Vec b, Vec c, Vec d, color c1, color c2) {
    beginShape();
    fill(c1);
    vertex( a.x(), a.y(), a.z());        
    vertex( b.x(), b.y(), b.z());
    fill(c2);
    vertex( c.x(), c.y(), c.z());
    vertex( d.x(), d.y(), d.z());
    endShape(CLOSE);
  }

  Vec interPlan(int n1, int n2, int n3, Vec vA) {
    Vec no=soustraire(n3, n1).cross(soustraire(n2, n1));
    Vec uv=Vec.subtract(pts[0].position(), vA);
    float lambda=(uv.dot(no))/no.z();
    return new Vec(vA.x(), vA.y(), lambda);
  }

  void fromTo(Vec u, Vec v, color col) {
    Vec m=Vec.add(Vec.multiply(u, 0.5), Vec.multiply(v, 0.5));
    Vec w=Vec.subtract(v, u);
    float lon=w.magnitude();
    Vec n=new Vec(0, -w.z(), w.y());
    n.normalize();
    w.normalize();
    pushMatrix();
    translate(m.x(), m.y(), m.z());
    rotate(acos(w.x()), n.x(), n.y(), n.z());
    fill(col);
    box(lon, 6, 6);
    popMatrix();
  }
}
