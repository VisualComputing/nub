class Homothetie {
  PVector centre;
  float rapport;

  Homothetie(PVector c, float r) {
    centre=c;
    rapport=r;
  }

  Homothetie(PVector C, PVector D, PVector A) {
    centre=C;
    PVector CD=PVector.sub(D, C);
    PVector CA=PVector.sub(A, C);
    PVector u=CD.get();
    u.normalize();
    rapport=CA.dot(u)/CD.dot(u);
  }

  PVector imageVecteur(PVector v) {
    return PVector.add(centre, PVector.mult(v, rapport));
  }
  
  PVector imagePoint(PVector p) {
    return imageVecteur(PVector.sub(p, centre));
  }

  PVector[] image2Points(PVector p1, PVector p2) {
    PVector[]     res= {
      imagePoint(p1), imagePoint(p2)
    };
    return res;
  }

  PVector[] image3Points(PVector p1, PVector p2, PVector p3) {
    PVector[]     res= {
      imagePoint(p1), imagePoint(p2), imagePoint(p3)
    };
    return res;
  }
}
