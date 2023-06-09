import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
int nb = 4;
Node[] reperes;
PImage abeille;
Homothetie h12, h31, h23;
Vector intersection12, intersection23, intersection13;
float temps = 0;

void setup() {
  size(640, 640, P3D);
  scene = new Scene(this, 800);
  scene.eye().setPosition(new Vector(0, 0, 800));
  reperes = new Node[nb];
  reperes[0] = new Node(pg -> {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(0, 255, 0);
    pg.sphere(15);
    pg.popStyle();
  }
  );
  reperes[1] = new Node(pg -> {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(color(0, 0, 255));
    pg.sphere(15);
    pg.popStyle();
  }
  );
  reperes[2] = reperes[1].copy();
  reperes[3] = reperes[1].copy();
  reperes[0].setPosition(-10, 40, 0);
  reperes[1].setPosition(-80, 150, 0);
  reperes[2].setPosition(100, 90, 0);
  reperes[3].setPosition(-20, -100, 0);
  for (int i=0; i<nb; i++) {
    Vector translationAxis = reperes[i].displacement(Vector.plusK);
    reperes[i].setTranslationPlaneFilter(translationAxis);
  }
  abeille = loadImage("sonia.gif");
}

void draw() {
  scene.display(color(255, 200, 0), this::worldBehavior);
}

void mouseMoved() {
  scene.tag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.shift();
  else
    scene.zoom(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  scene.moveForward(event.getCount() * 20);
}

void worldBehavior() {
  stroke(0);
  for (int i = 1; i < nb; i++) {
    for (int j = i + 1; j < nb; j++) {
      line(reperes[i].position().x(), reperes[i].position().y(), reperes[i].position().z(),
           reperes[j].position().x(), reperes[j].position().y(), reperes[j].position().z());
    }
  }
  calculerIntersections();
}//fin de ceva draw

float det(float a, float b, float ap, float bp) {
  return a * bp - ap * b;
}

Vector cramer(float a, float b, float c, float ap, float bp, float cp) {
  float d = det(a, ap, b, bp);
  float dx = det(c, cp, b, bp);
  float dy = det(a, ap, c, cp);
  return new Vector(dx / d, dy / d, 0);
}

Vector intersection(int i, int j, int k) {
  Vector f1 = reperes[i].position();
  Vector f2 = reperes[j].position();
  Vector f3 = reperes[k].position();
  Vector f1f2 = Vector.subtract(f2, f1);
  Vector f0f3 = Vector.subtract(f3, reperes[0].position());
  f1f2.normalize();
  f0f3.normalize();
  Vector res = cramer(-f1f2.y(), f1f2.x(), -f1f2.y() * f1.x() + f1f2.x() * f1.y(),
                      -f0f3.y(), f0f3.x(), -f0f3.y() * f3.x() + f0f3.x() * f3.y());
  return res;
}

void calculerIntersections() {
  intersection12 = intersection(1, 2, 3);
  intersection13 = intersection(1, 3, 2);
  intersection23 = intersection(2, 3, 1);
  pushMatrix();
  translate(intersection12.x(), intersection12.y(), intersection12.z());
  fill(255, 255, 0);
  noStroke();
  sphere(2);
  popMatrix();
  pushMatrix();
  translate(intersection13.x(), intersection13.y(), intersection13.z());
  fill(255, 255, 0);
  noStroke();
  sphere(2);
  popMatrix();
  pushMatrix();
  translate(intersection23.x(), intersection23.y(), intersection23.z());
  fill(255, 255, 0);
  noStroke();
  sphere(2);
  popMatrix();
  Vector f0 = reperes[0].position();
  Vector f1 = reperes[1].position();
  Vector f2 = reperes[2].position();
  Vector f3 = reperes[3].position();
  triangleCeva(f1, f2, f3);
  stroke(0, 0, 255);
  line(intersection12.x(), intersection12.y(), intersection12.z(), f3.x(), f3.y(), f3.z());
  line(intersection13.x(), intersection13.y(), intersection13.z(), f2.x(), f2.y(), f2.z());
  line(intersection23.x(), intersection23.y(), intersection23.z(), f1.x(), f1.y(), f1.z());
  h12 = new Homothetie(intersection12, f1, f2);
  h23 = new Homothetie(intersection23, f2, f3);
  h31 = new Homothetie(intersection13, f3, f1);
  Vector haut = Vector.multiply(new Vector(0, 0, 1), (Vector.subtract(f1, f0)).magnitude());
  Vector hf0 = Vector.add(haut, f0);
  Vector hf1 = Vector.add(haut, f1);
  photos(f0, f1, hf1, hf0);
}

void photos(Vector av, Vector bv, Vector bhv, Vector ahv) {
  temps = temps + 0.005f;
  if ((temps < 0) || temps > 4) temps = 0;
  placePhoto(av, bv, bhv, ahv);
  Vector v0 = h12.imagePoint(av);
  Vector v1 = h12.imagePoint(bv);
  Vector v2 = h12.imagePoint(bhv);
  Vector v3 = h12.imagePoint(ahv);
  placePhoto(v0, v1, v2, v3);
  Vector w0 = h23.imagePoint(v0);
  Vector w1 = h23.imagePoint(v1);
  Vector w2 = h23.imagePoint(v2);
  Vector w3 = h23.imagePoint(v3);
  placePhoto(w0, w1, w2, w3);
  Vector u0 = h31.imagePoint(w0);
  Vector u1 = h31.imagePoint(w1);
  Vector u2 = h31.imagePoint(w2);
  Vector u3 = h31.imagePoint(w3);
  placePhoto(u0, u1, u2, u3);
  traceCercle(av, bv);
  int tt = (int) temps;
  switch (tt) {
  case 0:
    mobile(av, v0, bv, v1, bhv, v2, ahv, v3, temps);
    pyramide(intersection12, av, bv, bhv, ahv);
    pyramide(intersection12, v0, v1, v2, v3);
    break;
  case 1:
    mobile(v0, w0, v1, w1, v2, w2, v3, w3, temps - 1);
    pyramide(intersection23, v0, v1, v2, v3);
    pyramide(intersection23, w0, w1, w2, w3);
    break;
  case 2:
    mobile(w0, u0, w1, u1, w2, u2, w3, u3, temps - 2);
    pyramide(intersection13, u0, u1, u2, u3);
    pyramide(intersection13, w0, w1, w2, w3);
    break;
  case 3:
    mobile(u0, av, u1, bv, u2, bhv, u3, ahv, temps - 3);
    pyramide(bv, u0, u1, u2, u3);
    pyramide(bv, av, bv, bhv, ahv);
    break;
  default:
    temps = 0.0f;
    break;
  }
}

void triangleCeva(Vector av, Vector bv, Vector cv) {
  beginShape();
  fill(255, 0, 0, 199);
  vertex(av.x(), av.y(), av.z());
  fill(255, 155, 0, 199);
  vertex(bv.x(), bv.y(), bv.z());
  fill(0, 0, 255, 199);
  vertex(cv.x(), cv.y(), cv.z());
  endShape();
}

void placePhoto(Vector aw, Vector bw, Vector bhw, Vector ahw) {
  noFill();
  beginShape();
  texture(abeille);
  vertex(aw.x(), aw.y(), aw.z(), 0, 0);
  vertex(bw.x(), bw.y(), bw.z(), 0, 300);
  vertex(bhw.x(), bhw.y(), bhw.z(), 300, 300);
  vertex(ahw.x(), ahw.y(), ahw.z(), 300, 0);
  endShape();
}

void traceCercle(Vector a, Vector b) {
  Vector d = Vector.subtract(b, a);
  float diam = d.magnitude() * 2;
  d.normalize();
  float angl = (d.y() < 0) ? -acos(d.x()) : acos(d.x());
  pushMatrix();
  translate(b.x(), b.y(), b.z());
  rotateZ(angl);
  rotateX(PI / 2);
  stroke(255, 0, 255);
  strokeWeight(3);
  noFill();
  ellipse(0, 0, diam, diam);
  diam *= sqrt(2);
  ellipse(0, 0, diam, diam);
  popMatrix();
  strokeWeight(1);
}

void pyramide(Vector s, Vector a, Vector b, Vector c, Vector d) {
  stroke(50, 155, 155, 99);
  strokeWeight(3);
  beginShape(TRIANGLE_FAN);
  fill(255, 255, 0, 99);
  vertex(s.x(), s.y(), s.z());
  vertex(a.x(), a.y(), a.z());
  fill(255, 0, 0, 99);
  vertex(b.x(), b.y(), b.z());
  vertex(c.x(), c.y(), c.z());
  fill(0, 255, 255, 99);
  vertex(d.x(), d.y(), d.z());
  vertex(a.x(), a.y(), a.z());
  endShape();
  strokeWeight(1);
}

void mobile(Vector a, Vector aa, Vector b, Vector bb, Vector c, Vector cc, Vector d, Vector dd, float t) {
  Vector as = comb(1 - t, a, t, aa);
  Vector bs = comb(1 - t, b, t, bb);
  Vector cs = comb(1 - t, c, t, cc);
  Vector ds = comb(1 - t, d, t, dd);
  placePhoto(as, bs, cs, ds);
}

Vector comb(float t1, Vector v1, float t2, Vector v2) {
  Vector res = Vector.add(Vector.multiply(v1, t1), Vector.multiply(v2, t2));
  return res;
}

class Homothetie {
  Vector centre;
  float rapport;

  Homothetie(Vector C, Vector D, Vector A) {
    centre = C;
    Vector CD = Vector.subtract(D, C);
    Vector CA = Vector.subtract(A, C);
    Vector u = CD.copy();
    u.normalize();
    rapport = CA.dot(u) / CD.dot(u);
  }

  Vector imageVecteur(Vector v) {
    return Vector.add(centre, Vector.multiply(v, rapport));
  }

  Vector imagePoint(Vector p) {
    return imageVecteur(Vector.subtract(p, centre));
  }
}
