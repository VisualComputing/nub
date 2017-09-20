class Ceva {
  int nb=4;
  InteractiveFrame[] reperes ;
  color c;
  AxisPlaneConstraint  contraintePlane;
  PVector intersection12, intersection23, intersection13;
  Homothetie h12, h31, h23;
  float temps=0;

  Ceva() {
    reperes=new InteractiveFrame[nb];

    contraintePlane=new WorldConstraint();
    contraintePlane.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vec(0.0f, 0.0f, 1.0f));

    for (int i=0;i<nb;i++) {
      reperes[i]=new InteractiveFrame(scene);
      reperes[i].setConstraint(contraintePlane);
    }

    reperes[0].setTranslation(-10, 40, 0); 
    reperes[1].setTranslation(-80, 150, 0); 
    reperes[2].setTranslation(100, 90, 0); 
    reperes[3].setTranslation(-20, -100, 0);
  }//fin constructeur

  void cevaDraw() {
    for ( int i=0;i<nb;i++) {         
      pushMatrix();
      reperes[i].applyTransformation();
      c=(i==0)? color(0, 255, 0): color( 0, 0, 255);

      noStroke();
      fill(c);
      sphere(7);
      popMatrix();
    }
    stroke(0);
    for ( int i=1;i<nb;i++) {   
      for ( int j=i+1;j<nb;j++) {
        line(reperes[i].position().x(), reperes[i].position().y(), reperes[i].position().z(), 
        reperes[j].position().x(), reperes[j].position().y(), reperes[j].position().z());
      }
    }
    calculerIntersections();
  }//fin de ceva draw
  
  void rectangle( float dx, float dy, float ax, float ay) {
    stroke(150);

    beginShape();
    fill(255, 255, 0, 70);
    vertex(dx, dy, 0);
    vertex(ax, dy, 0);
    fill(255, 50, 250, 70);
    vertex(ax, ay, 0);
    vertex(dx, ay, 0);
    endShape(CLOSE);
  }
  
  float det(float a, float b, float ap, float bp) {
    return a*bp-ap*b;
  }
  
  PVector cramer(float a, float b, float c, float ap, float bp, float cp) {
    float d=det(a, ap, b, bp);
    float dx=det(c, cp, b, bp);
    float dy=det(a, ap, c, cp);
    return new PVector(dx/d, dy/d, 0);
  }

  PVector intersection(int i, int j, int k) {
    PVector f1=Scene.toPVector(reperes[i].position());
    PVector f2=Scene.toPVector(reperes[j].position());
    PVector f3=Scene.toPVector(reperes[k].position());
    PVector f1f2=PVector.sub(f2, f1);
    PVector f0f3=PVector.sub(f3, Scene.toPVector(reperes[0].position()));      
    f1f2.normalize();
    f0f3.normalize();

    PVector res=cramer(-f1f2.y, f1f2.x, -f1f2.y*f1.x+f1f2.x*f1.y, 
    -f0f3.y, f0f3.x, -f0f3.y*f3.x+f0f3.x*f3.y);
    return res;
  }
  
  void  calculerIntersections() {
    intersection12=intersection(1, 2, 3);
    intersection13=intersection(1, 3, 2);
    intersection23=intersection(2, 3, 1);

    pushMatrix();     
    translate(intersection12.x, intersection12.y, intersection12.z);
    fill(255, 255, 0);
    noStroke();
    sphere(2); 
    popMatrix();
    pushMatrix();     
    translate(intersection13.x, intersection13.y, intersection13.z);
    fill(255, 255, 0);
    noStroke();
    sphere(2); 
    popMatrix();
    pushMatrix();     
    translate(intersection23.x, intersection23.y, intersection23.z);
    fill(255, 255, 0);
    noStroke();
    sphere(2); 
    popMatrix();
    PVector f0=Scene.toPVector(reperes[0].position());
    PVector f1=Scene.toPVector(reperes[1].position());
    PVector f2=Scene.toPVector(reperes[2].position());
    PVector f3=Scene.toPVector(reperes[3].position());
    triangleCeva(f1, f2, f3);
    stroke(0, 0, 255);
    line(intersection12.x, intersection12.y, intersection12.z, f3.x, f3.y, f3.z);
    line(intersection13.x, intersection13.y, intersection13.z, f2.x, f2.y, f2.z);          
    line(intersection23.x, intersection23.y, intersection23.z, f1.x, f1.y, f1.z);

    h12=new Homothetie(intersection12, f1, f2);
    h23=new Homothetie(intersection23, f2, f3);
    h31=new Homothetie(intersection13, f3, f1);
    PVector haut=PVector.mult(new PVector(0, 0, 1), (PVector.sub(f1, f0)).mag());
    PVector hf0=PVector.add(haut, f0);
    PVector hf1=PVector.add(haut, f1);
    photos(f0, f1, hf1, hf0);
  }      

  void homothese() {
    PVector v0=h12.imagePoint(Scene.toPVector(reperes[0].position()));
    PVector v4=h12.imagePoint(Scene.toPVector(reperes[4].position()));
    PVector v1=h12.imagePoint(Scene.toPVector(reperes[1].position()));
    PVector v2=h12.imagePoint(Scene.toPVector(reperes[2].position()));
  }
  
  void photos(PVector av, PVector bv, PVector bhv, PVector ahv) {
    temps=temps+0.005;
    if ((temps<0)||temps>4)temps=0;

    placePhoto(av, bv, bhv, ahv);

    PVector v0=h12.imagePoint(av);
    PVector v1=h12.imagePoint(bv);
    PVector v2=h12.imagePoint(bhv);
    PVector v3=h12.imagePoint(ahv);

    placePhoto(v0, v1, v2, v3);

    PVector w0=h23.imagePoint(v0);
    PVector w1=h23.imagePoint(v1);
    PVector w2=h23.imagePoint(v2);
    PVector w3=h23.imagePoint(v3);
    placePhoto(w0, w1, w2, w3);
    
    PVector u0=h31.imagePoint(w0);
    PVector u1=h31.imagePoint(w1);
    PVector u2=h31.imagePoint(w2);
    PVector u3=h31.imagePoint(w3);
    placePhoto(u0, u1, u2, u3);

    traceCercle(av, bv);
    int tt=int(temps);
    switch(tt) {
    case 0: 
      mobile(av, v0, bv, v1, bhv, v2, ahv, v3, temps);
      pyramide(intersection12, av, bv, bhv, ahv);
      pyramide(intersection12, v0, v1, v2, v3);
      break;
    case 1: 
      mobile(v0, w0, v1, w1, v2, w2, v3, w3, temps-1);
      pyramide(intersection23, v0, v1, v2, v3);
      pyramide(intersection23, w0, w1, w2, w3);             

      break;
    case 2: 
      mobile(w0, u0, w1, u1, w2, u2, w3, u3, temps-2);
      pyramide(intersection13, u0, u1, u2, u3);
      pyramide(intersection13, w0, w1, w2, w3);
      break;
    case 3: 
      mobile(u0, av, u1, bv, u2, bhv, u3, ahv, temps-3);
      pyramide(bv, u0, u1, u2, u3);
      pyramide(bv, av, bv, bhv, ahv);
      break;
    default:           
      temps=0.0;  
      break;
    }
  }

  void triangleCeva(PVector av, PVector bv, PVector cv) {
    beginShape();
    fill(255, 0, 0, 199);
    vertex( av.x, av.y, av.z);
    fill(255, 155, 0, 199);              
    vertex(bv.x, bv.y, bv.z);
    fill(0, 0, 255, 199);    
    vertex(cv.x, cv.y, cv.z);
    endShape();
  }

  void placePhoto(PVector aw, PVector bw, PVector bhw, PVector ahw) {
    noFill();
    beginShape();
    texture(abeille);
    vertex( aw.x, aw.y, aw.z, 0, 0);        
    vertex(bw.x, bw.y, bw.z, 0, 300);
    vertex(bhw.x, bhw.y, bhw.z, 300, 300);
    vertex(ahw.x, ahw.y, ahw.z, 300, 0);
    endShape();
  }
  
  void traceCercle(PVector a, PVector b) {
    PVector d=PVector.sub(b, a);
    float diam=d.mag()*2;
    d.normalize();
    float angl=(d.y<0)? -acos(d.x) : acos(d.x);

    pushMatrix();
    translate(b.x, b.y, b.z);

    rotateZ(angl);
    rotateX(PI/2);
    stroke(255, 0, 255); 
    strokeWeight(3);
    noFill();
    ellipse(0, 0, diam, diam);

    diam*=sqrt(2);
    ellipse(0, 0, diam, diam);       
    popMatrix();
    strokeWeight(1);
  }
  
  void pyramide(PVector s, PVector a, PVector b, PVector c, PVector d) {
    stroke(50, 155, 155, 99);
    strokeWeight(3);
    beginShape(TRIANGLE_FAN);
    fill(255, 255, 0, 99);
    vertex(s.x, s.y, s.z);

    vertex(a.x, a.y, a.z);
    fill(255, 0, 0, 99);
    vertex(b.x, b.y, b.z);
    vertex(c.x, c.y, c.z);
    fill(0, 255, 255, 99);
    vertex(d.x, d.y, d.z);
    vertex(a.x, a.y, a.z);
    endShape();
    strokeWeight(1);
  }

  void mobile(PVector a, PVector aa, PVector b, PVector bb, PVector c, PVector cc, PVector d, PVector dd, float t) {
    PVector as=comb(1-t, a, t, aa);
    PVector bs=comb(1-t, b, t, bb);
    PVector cs=comb(1-t, c, t, cc);
    PVector ds=comb(1-t, d, t, dd);
    placePhoto(as, bs, cs, ds);
  } 

  PVector comb(float t1, PVector v1, float t2, PVector v2) {
    PVector res=PVector.add(PVector.mult(v1, t1), PVector.mult(v2, t2));
    return res;
  }
}