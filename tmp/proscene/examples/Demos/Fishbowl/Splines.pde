//Splines curves used to locate control points
ArrayList<PVector> curve_cardinal;

ArrayList<PVector> drawCardinalSpline( float tension, PVector k0, PVector k1, PVector k2, PVector k3, float e){
  if(tension > 1) tension/=100;
  float s = (1 - tension)/ 2;  
  ArrayList<PVector> curve = new ArrayList<PVector>();
  for(float u = 0; u < 1; u+= e){
    float u3 = u*u*u;
    float u2 = u*u;
    float x = k0.x * (-s*u3 + 2*s*u2 -s*u) +
              k1.x * ((2-s)*u3 +(s-3)*u2 + 1) +
              k2.x * ((s-2)*u3 + (3-2*s)*u2 + s*u) +
              k3.x * (s*u3 - s*u2);
    float y = k0.y * (-s*u3 + 2*s*u2 -s*u) +
              k1.y * ((2-s)*u3 +(s-3)*u2 + 1) +
              k2.y * ((s-2)*u3 + (3-2*s)*u2 + s*u) +
              k3.y * (s*u3 - s*u2);
    float z = k0.z * (-s*u3 + 2*s*u2 -s*u) +
              k1.z * ((2-s)*u3 +(s-3)*u2 + 1) +
              k2.z * ((s-2)*u3 + (3-2*s)*u2 + s*u) +
              k3.z * (s*u3 - s*u2);
   curve.add(new PVector(x,y,z));
  }
  float u = 1, u2 = 1, u3 = 1; 
  float x = k0.x * (-s*u3 + 2*s*u2 -s*u) +
            k1.x * ((2-s)*u3 +(s-3)*u2 + 1) +
            k2.x * ((s-2)*u3 + (3-2*s)*u2 + s*u) +
            k3.x * (s*u3 - s*u2);
  float y = k0.y * (-s*u3 + 2*s*u2 -s*u) +
            k1.y * ((2-s)*u3 +(s-3)*u2 + 1) +
            k2.y * ((s-2)*u3 + (3-2*s)*u2 + s*u) +
            k3.y * (s*u3 - s*u2);
  float z = k0.z * (-s*u3 + 2*s*u2 -s*u) +
            k1.z * ((2-s)*u3 +(s-3)*u2 + 1) +
            k2.z * ((s-2)*u3 + (3-2*s)*u2 + s*u) +
            k3.z * (s*u3 - s*u2);
  curve.add(new PVector(x,y,z));
  return curve;
}

ArrayList<PVector> drawCurve(ArrayList<PVector> points, float tension, float e, boolean closed){
    if(points.size() < 4) return null;
    ArrayList<PVector> curve_card = new ArrayList<PVector>();
    int i = 1;
    for(;i < points.size() - 2; i++){
      curve_card.addAll(drawCardinalSpline(tension, points.get(i-1), points.get(i), points.get(i + 1), points.get(i + 2), e));
    }
    if(closed){
      curve_card.addAll( drawCardinalSpline(tension, points.get(points.size()-3), points.get(points.size()-2), points.get(points.size()-1), points.get(0), e));
      curve_card.addAll( drawCardinalSpline(tension, points.get(points.size()-2), points.get(points.size()-1), points.get(0), points.get(1), e));
      curve_card.addAll( drawCardinalSpline(tension, points.get(points.size()-1), points.get(0), points.get(1), points.get(2), e));
      curve_card.addAll( drawCardinalSpline(tension, points.get(0), points.get(1), points.get(2), points.get(3), e));
    }
    return curve_card;
}