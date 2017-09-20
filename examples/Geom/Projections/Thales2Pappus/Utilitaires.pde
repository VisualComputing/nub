void balle(int i) {
  color  c=color(255, 255, 255);   
  pushStyle();
  switch(i) {
  case 0 :
  case 1:
  case 4: 
    c=color(255, 0, 0);  
    break;
  case 2 :
  case 3 :
    c=color(255, 255, 0); 
    break;
  case 5 :
  case 6:
    c=color(255, 255, 255); 
    break;
  }  
  fill(c);
  noStroke();
  sphere(2);  
  popStyle();
}

PVector comb(float t1, PVector v1, float t2, PVector v2) {
  PVector res=PVector.add(PVector.mult(v1, t1), PVector.mult(v2, t2));
  return res;
}

PVector comb(float t1, PVector v1, float t2, PVector v2, float t3, PVector v3) {
  PVector res=PVector.add(PVector.mult(v1, t1), PVector.mult(v2, t2));
  res=PVector.add(res, PVector.mult(v3, t3));
  return res;
}

float angleEntre(PVector u, PVector v) {
  u.normalize();
  v.normalize();
  float sinus=u.y*v.z-u.z*v.y;

  return asin(sinus);
}

PVector centreGravite(PVector u, PVector v, PVector r) {
  PVector gr= comb(0.5f, u, 0.5f, v);
  gr= comb(1.0f/3.0f, r, 2.0f/3.0f, gr);
  return gr;
}

PVector barycentre(float lamb, PVector u, PVector v) {
  return comb(lamb, u, 1-lamb, v);
}

float  barycentre(float lamb, float u, float v) {
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

PVector projection(PVector M) {
  return  projection( M, true);
}

PVector projection(PVector M, boolean dess) {
  PVector SM=comb(1, M, -1, Scene.toPVector(soleil.position() ));
  PVector P=Scene.toPVector(planProjection.repere.position());
  PVector SP=comb(1, P, -1, Scene.toPVector(soleil.position()) );
  planProjection.getNormaleDansWorld();
  float lambda=(SP.dot( planProjection.normale))/(SM.dot( planProjection.normale));
  PVector rep= comb(1, Scene.toPVector(soleil.position()), lambda, SM);
  strokeWeight(1); 
  stroke(0, 0, 200, 150);
  if (dess) ligne(Scene.toPVector(soleil.position()), rep);
  strokeWeight(1);
  return rep;
}

PVector intersectionDroite(PVector u1, PVector v1, PVector u2, PVector v2) {
  PVector uv1=comb(1, v1, -1, u1);
  PVector uv2=comb(1, v2, -1, u2);
  PVector nor=uv2.cross(uv2.cross(uv1));
  nor.normalize();
  PVector u1u2=comb(1, u2, -1, u1);
  float lambda=(u1u2.dot(nor))/(uv1.dot(nor));
  return comb(1, u1, lambda, uv1);
}

PVector intersectionDroiteDir2(PVector u1, PVector v1, PVector u2, PVector dir2) {
  PVector uv1=comb(1, v1, -1, u1);
  PVector uv2=dir2;
  PVector nor=uv2.cross(uv2.cross(uv1));
  nor.normalize();
  PVector u1u2=comb(1, u2, -1, u1);
  float lambda=(u1u2.dot(nor))/(uv1.dot(nor));
  return comb(1, u1, lambda, uv1);
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

void triangle3d(PVector a, PVector b, PVector c) {
  beginShape();
  fill(255, 200, 0, 200);
  vertex( a.x, a.y, a.z);
  fill(255, 255, 0, 200);        
  vertex( b.x, b.y, b.z);
  fill(155, 50, 250, 200);
  vertex( c.x, c.y, c.z);
  endShape();
}     

void triangle3d(PVector a, PVector b, PVector c, color couleur) {
  stroke(0, 100, 255);
  beginShape();
  fill(couleur);
  vertex( a.x, a.y, a.z);
  vertex( b.x, b.y, b.z);
  vertex( c.x, c.y, c.z);
  endShape();
}     
