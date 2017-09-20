class Faisceau {
  InteractiveFrame[] points;
  PVector[] dirs, inter1, inter2, delta1, delta2;
  int nbpoints;

  Faisceau() {
    nbpoints=4;
    points=new InteractiveFrame[nbpoints];
    dirs=new PVector[nbpoints];
    inter1=new PVector[nbpoints];  
    inter2=new PVector[nbpoints];
    delta1=new PVector[nbpoints];
    delta2=new PVector[nbpoints];        
    for (int i=0;i<nbpoints;i++) {
      points[i]=new InteractiveFrame(scene);
      points[i].setConstraint(libreT);
    }
    points[0].setTranslation(39, 209, 123);
    points[1].setTranslation(0, -200, 65);
    points[2].setTranslation(-133, -227, -2);
    points[3].setTranslation(-72, -146, 114);
    directions();
  }

  void draw() {
    PVector v;
    for (int i=0;i<nbpoints;i++) {
      pushMatrix();
      points[i].applyTransformation();
      v=Scene.toPVector(points[i].position());
      balle(i);
      popMatrix();
    }
    directions();
    calculer();
    pointsSurOX();
    quadrilatere(Scene.toPVector(points[1].position()), Scene.toPVector(points[2].position()), inter2[2], inter2[1], color(250, 250, 0, 100));
    quadrilatere(Scene.toPVector(points[1].position()), Scene.toPVector(points[3].position()), inter2[3], inter2[1], color(250, 100, 0, 100));
    quadrilatere(Scene.toPVector(points[2].position()), Scene.toPVector(points[3].position()), inter2[3], inter2[2], color(100, 250, 0, 100));
    triangle3d(Scene.toPVector(points[0].position()), inter1[2], inter1[1], color(250, 250, 0, 100));
    triangle3d(Scene.toPVector(points[0].position()), inter1[2], inter1[3], color(250, 100, 0, 100)); 
    triangle3d(Scene.toPVector(points[0].position()), inter1[3], inter1[1], color(100, 250, 0, 100));

    dessin2D();
  }

  void directions() {
    for (int i=1;i<nbpoints;i++) {
      dirs[i]=comb(1.0, Scene.toPVector(points[i].position()), -1.0, Scene.toPVector(points[0].position()));
    }
  }

  void calculer() {
    for (int i=1;i<nbpoints;i++) {
      inter1[i]=calculInter1(i);
      inter2[i]=calculInter2(i);
    }
    triangle3d(inter2[1], inter2[2], inter2[3], color(0, 0, 255));
    triangle3d(inter1[1], inter1[2], inter1[3], color(0, 0, 255));
  } 

  PVector calculInter1(int n) {
    PVector z1=comb(1, Scene.toPVector(plan1.repere.inverseCoordinatesOf(new Vec(0, 0, 1))), -1, Scene.toPVector(plan1.repere.position()));
    float lambda=z1.dot(comb(-1, Scene.toPVector(points[0].position()), 1, Scene.toPVector(plan1.repere.position())))/z1.dot(dirs[n]);
    return comb(1, Scene.toPVector(points[0].position()), lambda, dirs[n]);
  }
  
  PVector calculInter2(int n) {
    PVector z1=comb(1, Scene.toPVector(plan2.repere.inverseCoordinatesOf(new Vec(0, 0, 1))), -1, Scene.toPVector(plan2.repere.position()));
    float lambda=z1.dot(comb(-1, Scene.toPVector(points[0].position()), 1, Scene.toPVector(plan2.repere.position())))/z1.dot(dirs[n]);
    return comb(1, Scene.toPVector(points[0].position()), lambda, dirs[n]);
  }

  PVector couperLaxe(PVector a, PVector b) {
    PVector dirAB=comb(1, b, -1, a);
    float lambda=(dirAB.y!=0)? -a.y/dirAB.y: -a.z/dirAB.z;
    return comb(1, a, lambda, dirAB);
  }

  void pointsSurOX() {
    stroke(0);
    delta1[1]=couperLaxe(inter1[1], inter1[2]);
    delta1[2]=couperLaxe(inter1[2], inter1[3]);
    delta1[3]=couperLaxe(inter1[1], inter1[3]);
    delta2[1]=couperLaxe(inter2[1], inter2[2]);
    delta2[2]=couperLaxe(inter2[2], inter2[3]);
    delta2[3]=couperLaxe(inter2[1], inter2[3]);

    pushStyle();
    stroke(0, 255, 0);
    strokeWeight(2);
    ligne(inter1[1], delta1[1]);
    ligne(inter1[2], delta1[2]);
    ligne(inter1[3], delta1[3]);

    ligne(inter2[1], delta2[1]);
    ligne(inter2[2], delta2[2]);
    ligne(inter2[3], delta2[3]);
    popStyle();
    color c=color(0, 0, 255, 10);
    triangle3d(inter1[1], delta1[1], delta1[3], c);
    triangle3d(inter1[2], delta1[2], delta1[1], c);
    triangle3d(inter1[3], delta1[3], delta1[2], c);
    c=color(255, 0, 0, 10);  
    triangle3d(inter2[1], delta2[1], delta2[3], c);
    triangle3d(inter2[2], delta2[2], delta2[1], c);
    triangle3d(inter2[3], delta2[3], delta2[2], c);
  }

  void quadrilatere(PVector u1, PVector u2, PVector v1, PVector v2, color c) {
    fill(c);
    stroke(0, 0, 255);
    beginShape();
    vertex(u1.x, u1.y, u1.z);
    vertex(u2.x, u2.y, u2.z);
    vertex(v1.x, v1.y, v1.z);
    vertex(v2.x, v2.y, v2.z);
    endShape(CLOSE);
  }

  void dessin2D() {
    stroke(0, 0, 100);

    PVector pt0=projection(Scene.toPVector(points[0].position()));
    PVector pt1=projection(Scene.toPVector(points[1].position()));       
    PVector pt2=projection(Scene.toPVector(points[2].position())); 
    PVector pt3=projection(Scene.toPVector(points[3].position())); 

    PVector u12=projection(delta1[1]);
    PVector u23=projection(delta1[2]);
    PVector u13=projection(delta1[3]);

    PVector d12=projection(delta2[1]);
    PVector d23=projection(delta2[2]);
    PVector d13=projection(delta2[3]); 
    PVector pi12=projection(inter1[1]);
    PVector pi23=projection(inter1[2]);       
    PVector pi13=projection(inter1[3]);  
    PVector pj12=projection(inter2[1]); 
    PVector pj23=projection(inter2[2]); 
    PVector pj13=projection(inter2[3]); 

    strokeWeight(2);
    stroke(255);
    ligne(pt0, pt1); 
    ligne(pt0, pt2);    
    ligne(pt0, pt3); 

    ligne(pi12, u12);
    ligne(pi23, u23);
    ligne(pi13, u13);

    ligne(pj12, d12);
    ligne(pj23, d23);
    ligne(pj13, d13);
    stroke(255, 0, 0);  
    strokeWeight(4);
    ligne(u12, u13);
    ligne(u12, u23); 
    ligne(u23, u13);
    strokeWeight(1);  
    triangle3d(pi12, pi13, pi23, color(0, 0, 255, 254));
    triangle3d(pj12, pj13, pj23, color(0, 0, 255, 254));
  }
}
