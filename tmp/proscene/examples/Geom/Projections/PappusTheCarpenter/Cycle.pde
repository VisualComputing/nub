class Cycle {
  int npk=9;
  float  rap;
  Vec[] punks;

  Cycle(float r) {
    rap=r;
    punks=new Vec[npk];
  }

  void actualiser(float r) {
    rap=r;
    punks[0]=figure.pointA;
    punks[1]=barycentre(rap, figure.pts[0].position(), figure.pts[1].position());
    punks[2]=inter2Droites(4, 5, punks[0], punks[1]);
    punks[3]=new Vec(punks[2].x(), punks[2].y(), 0);
    punks[4]=figure.pointB;
    punks[5]=punks[1];
    punks[6]=inter2Droites(2, 3, punks[4], punks[5]);  
    punks[7]=new Vec(punks[6].x(), punks[6].y(), 0); 
    punks[8]=new Vec(punks[1].x(), punks[1].y(), 0); 

    ligne(punks[0], punks[1]);
    ligne(punks[0], punks[2]);    
    ligne(figure.pointB, punks[5]); 
    ligne(punks[4], punks[6]);

    pushStyle();  
    noStroke();
    fill(255, 200, 200);
    fromTo(punks[2], punks[3]);
    fromTo(figure.pointA0, punks[3]);  
    fromTo(punks[6], punks[7]);
    fromTo(punks[7], figure.pointB0);   
    fromTo(punks[1], punks[8]);
    popStyle();
  }

  Vec inter2Droites(int n0, int n1, Vec p0, Vec p1) {
    Vec adir=figure.soustraire(n0, n1);
    Vec bdir=Vec.subtract(p0, p1);
    Vec ab=Vec.subtract(figure.pts[n0].position(), punks[1]);
    float lambda = determinant(ab, adir)/determinant(bdir, adir);
    return comb(1, punks[1], lambda, bdir);
  } 

  void ligne(Vec a, Vec b) {
    // stroke(0);
    line(a.x(), a.y(), a.z(), b.x(), b.y(), b.z());
  }
  
  void fromTo(Vec u, Vec v) {
    Vec m=Vec.add(Vec.multiply(u, 0.5), Vec.multiply(v, 0.5));
    Vec w=Vec.subtract(v, u);
    float lon=w.magnitude();
    Vec n=new Vec(0, -w.z(), w.y());
    n.normalize();
    w.normalize();
    pushMatrix();
    translate(m.x(), m.y(), m.z());
    rotate(acos(w.x()), n.x(), n.y(), n.z());
    box(lon, 8, 8);
    popMatrix();
  }
}
