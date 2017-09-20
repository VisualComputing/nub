PVector comb(float t1, PVector v1, float t2, PVector v2) {
  PVector res=PVector.add(PVector.mult(v1, t1), PVector.mult(v2, t2));
  return res;
}

PVector comb(float t1, PVector v1, float t2, PVector v2, float t3, PVector v3) {
  PVector res=PVector.add(PVector.mult(v1, t1), PVector.mult(v2, t2));
  res=PVector.add(res, PVector.mult(v3, t3));
  return res;
}

PVector centreGravite(PVector u, PVector v, PVector r) {
  PVector gr= comb(0.5f, u, 0.5f, v);
  gr= comb(1.0f/3.0f, r, 2.0f/3.0f, gr);
  return gr;
} 
PVector barycentre(float lamb, PVector u, PVector v) {
  return comb(lamb, u, 1-lamb, v);
}

float barycentre(float lamb, float u, float v) {
  return lamb*u+(1-lamb)*v;
}

void ligne(PVector a, PVector b) {
  line(a.x, a.y, a.z, b.x, b.y, b.z);
}

void afficher(PVector u) {
  println("vecteur = "+u.x+"    "+u.y+"   "+u.z);
}

void afficher(Quat q) {
  println("quaternion = x  "+q.x()+"  y  "+q.y()+" z  "+q.z()+"... w  "+q.z());
}

void rectangle(color c, float dx, float dy, float ax, float ay) {
  stroke(150);
  fill(c);
  beginShape();
  vertex(dx, dy, 0);
  vertex(ax, dy, 0);
  fill(color(red(c)*2, green(c)*2, blue(c)*2));
  vertex(ax, ay, 0);
  vertex(dx, ay, 0);
  endShape(CLOSE);
}

void triangle3D(PVector a, PVector b, PVector c) {
  beginShape();
  fill(255, 200, 0, 200);
  vertex( a.x, a.y, a.z);
  fill(255, 255, 0, 200);        
  vertex( b.x, b.y, b.z);
  fill(155, 50, 250, 200);
  vertex( c.x, c.y, c.z);
  endShape();
}  

void triangle3D(PVector a, PVector b, PVector c, float k, float l, float m) {
  stroke(0, 100, 255);
  beginShape();
  fill(k*0.9, l, m, 254);
  vertex( a.x, a.y, a.z);
  fill(k, l*0.5, m, 254);
  vertex( b.x, b.y, b.z);
  fill(k*0.9, l, 0.8*m, 254);
  vertex( c.x, c.y, c.z);
  endShape();
}  

void triangles3D(PVector a, PVector b, PVector c) {
  PVector or=new PVector(0, 0, 0);
  triangle3D(a, b, or);
  triangle3D(b, c, or);
  triangle3D(a, c, or);
}

PVector symetriePlan(PVector m, PVector u, PVector v) {
  PVector normale=u.cross(v);
  normale.normalize();
  PVector pm=PVector.mult(normale, m.dot(normale));
  return comb(1, m, -2.0, pm);
}

PVector projectionSurDroite(PVector v, PVector droite) {
  PVector u=droite.get();
  u.normalize();
  return PVector.mult(u, u.dot(v));
}

float angleQuaternion(Quat q) {	
  q.normalize();
  return (float) Math.acos(q.w())* 2.0f;
}  

PVector normaliser(PVector v, float f) {
  v.normalize();
  return PVector.mult(v, f);
}

PVector calculSym(PVector m, PVector n) {
  n.normalize();
  PVector p=PVector.mult(n, n.dot(m));
  PVector res=comb(1.0, m, -2.0, p);
  return res;
}

void afficherL(String L, PVector po) {
  pushMatrix();
  translate(po.x, po.y, po.z);
  text(L, 10, 10);
  popMatrix();
}

void unText1(String tex, PVector v) {
  float leX = screenX(v.x, v.y, v.z);
  float leY = screenY(v.x, v.y, v.z);
  pushMatrix();
  scene.beginScreenDrawing();
  fill(255);
  text(tex, leX+10, leY, 10);
  scene.endScreenDrawing(); 
  popMatrix();
} 

void unText1(String tex, float xx, float yy, float zz) {
  float leX = screenX(xx, yy, zz);
  float leY = screenY(xx, yy, zz);
  pushMatrix();
  scene.beginScreenDrawing();
  fill(255);
  text(tex, leX+10, leY, 10);
  scene.endScreenDrawing(); 
  popMatrix();
}   
