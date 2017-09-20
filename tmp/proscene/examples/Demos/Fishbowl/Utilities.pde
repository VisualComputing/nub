//BOUNDING BOX & TEXTURE METHODS-------------------------------
//Get a bounding box
//get the faces of the bounding box
Vec[][] original_box;
Vec[][] deformed_box;

//draw a rect bound
public void drawCube(InteractiveFrame m, PShape p){
  //set translation, rotation
  Vec[][] cub = getFaces(m, p);
  pushStyle();
  stroke(color(255,0,0));
  fill(color(255,0,0,5));
  for(int i = 0; i < cub.length; i++){
    beginShape();
    vertex(cub[i][0].x(), cub[i][0].y(), cub[i][0].z());
    vertex(cub[i][1].x(), cub[i][1].y(), cub[i][1].z());
    vertex(cub[i][3].x(), cub[i][3].y(), cub[i][3].z());
    vertex(cub[i][2].x(), cub[i][2].y(), cub[i][2].z());
    endShape(CLOSE);
  }
  popStyle();
}

//---------------------------------
//Used for bounding box collisions
public Vec[] getCube(PShape shape) {
  Vec v[] = new Vec[2];
  float minx = 999;
  float miny = 999;
  float maxx = -999;
  float maxy = -999;
  float minz = 999;
  float maxz = -999;  
  for(int j = 0; j < shape.getChildCount(); j++){
    PShape aux = shape.getChild(j);
    for(int i = 0; i < aux.getVertexCount(); i++){
      float x = aux.getVertex(i).x;
      float y = aux.getVertex(i).y;
      float z = aux.getVertex(i).z;
      minx = minx > x ? x : minx;
      miny = miny > y ? y : miny;
      minz = minz > z ? z : minz;
      maxx = maxx < x ? x : maxx;
      maxy = maxy < y ? y : maxy;
      maxz = maxz < z ? z : maxz;
    }
  }
  
  v[0] = new Vec(minx,miny, minz);
  v[1] = new Vec(maxx,maxy, maxz);
  return v;
}

public Vec[][] getFaces(InteractiveFrame m, PShape p){
  Vec[] cub = getCube(p);
  Vec[][] faces = new Vec[6][4];
  faces[0][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[0].z()));
  faces[0][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[0].z()));
  faces[0][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[0].z()));
  faces[0][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[0].z()));

  faces[1][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[1].z()));
  faces[1][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[1].z()));
  faces[1][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[1].z()));
  faces[1][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[1].z()));

  faces[2][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[0].z()));
  faces[2][1] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[0].z()));
  faces[2][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[1].z()));
  faces[2][3] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[1].z()));

  faces[3][0] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[0].z()));
  faces[3][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[0].z()));
  faces[3][2] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[1].z()));
  faces[3][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[1].z()));

  faces[4][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[0].z()));
  faces[4][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[0].z()));
  faces[4][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[1].z()));
  faces[4][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[1].z()));

  faces[5][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[0].z()));
  faces[5][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[0].z()));
  faces[5][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[1].z()));
  faces[5][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[1].z()));
  
  return faces;
}

//a rect is definedby (x,y,z) = r + l*t, where r and l are vectors (l is the direction and r the cut point) 
public Vec getIntersectionPlaneRect(Vec r, Vec l, Vec p, Vec p1, Vec p2){
  //get the plane eq:
  Vec u = Vec.subtract(p1,p);
  Vec v = Vec.subtract(p2,p);
  float[][] A = {{-l.x(),u.x(),v.x()},{-l.y(),u.y(),v.y()},{-l.z(),u.z(),v.z()}};
  Vec r_minus_p = Vec.subtract(r,p);
  float[][] B = {{r_minus_p.x()},{r_minus_p.y()},{r_minus_p.z()}};
  //println("solution : ", B);
  Vec intersection = Vec.add(r,Vec.multiply(l,B[0][0]));
  return intersection;
}

//just check if the point is iner the bounds of a rect
public boolean getIntersectionSubPlane(Vec v, Vec min, Vec max){
  if(v.x() >=min.x() && v.x() <=max.x()){
    if(v.y() >=min.y() && v.y() <=max.y()){
      return true;
    }
  }
  return false;
}

void setVertices(PShape figure, ArrayList<PVector> new_positions){
  for(int i = 0; i < new_positions.size(); i++){
    figure.setVertex(i, new_positions.get(i));
  }
}

//apply a texture
public PShape applyTexture(PShape p){
  Vec[] r_bounds = getCube(p);
  PImage text = last_texture;
  PShape p_group = createShape(GROUP);
  
  //step btwn vertices
  float dif = 0; 
  float g_dif = 0;
  float second_dif = 0;
  if(r_bounds[1].x() - r_bounds[0].x() > dif){
    g_dif = 0;
  }
  if(r_bounds[1].y() - r_bounds[0].y() > dif){
    g_dif = 1;
  }
  if(r_bounds[1].z() - r_bounds[0].z() > dif){
    g_dif = 2;
  }

  dif = 0;
  if(r_bounds[1].x() - r_bounds[0].x() > dif && g_dif != 0){
    second_dif = 0;
  }
  if(r_bounds[1].y() - r_bounds[0].y() > dif && g_dif != 1){
    second_dif = 1;
  }
  if(r_bounds[1].z() - r_bounds[0].z() > dif && g_dif != 2){
    second_dif = 2;
  }
  
  float s_w = g_dif == 0 ? r_bounds[1].x() - r_bounds[0].x() : g_dif == 1 ? r_bounds[1].y() - r_bounds[0].y() : r_bounds[1].z() - r_bounds[0].z();
  float s_h = second_dif == 0 ? r_bounds[1].x() - r_bounds[0].x() : second_dif == 1 ? r_bounds[1].y() - r_bounds[0].y() : r_bounds[1].z() - r_bounds[0].z();
  float i_w = text.width;
  float i_h = text.height;
  float rep = 0;
  for(int j = 0; j < p.getChildCount(); j++){
    PShape pc = p.getChild(j);
    PShape p_clone = createShape();
    p_clone.beginShape(POLYGON);
    p_clone.texture(text);
    p_clone.noFill();
    p_clone.noStroke();
    p_clone.textureMode(IMAGE);  
    for(int i = 0; i < pc.getVertexCount(); i++){
      PVector idx = pc.getVertex(i);
      PVector n = pc.getNormal(i);
      float u = g_dif == 0 ? idx.x - r_bounds[0].x(): g_dif == 1 ? idx.y - r_bounds[0].y(): idx.z - r_bounds[0].z(); 
      float v = second_dif == 0 ? idx.x - r_bounds[0].x() : second_dif == 1 ? idx.y - r_bounds[0].y() : idx.z - r_bounds[0].z();
      u = i_w*u*1./s_w;
      v = i_h*v*1./s_h;    
      p_clone.normal(n.x,n.y,n.z);
      p_clone.vertex(idx.x,idx.y,idx.z, 0.5*u,0.5*v);
    }
    p_clone.endShape();
    p_group.addChild(p_clone);
  }
  return p_group;  
}

//fill with a color
public void fillWithColor(InteractiveFrame f, PShape p, color c){
  PShape p_clone = createShape();
  p_clone.beginShape(TRIANGLE);
  p_clone.fill(c);
  p_clone.noStroke();
  for(int i = 0; i < p.getVertexCount(); i++){
    PVector v = p.getVertex(i);
    PVector n = p.getNormal(i);
    p_clone.vertex(v.x,v.y,v.z);
    p_clone.normal(n.x,n.y,n.z);
  }
  p_clone.endShape();
  p = p_clone;
  f.setShape(p);
}
//END BOUNDIG & TEXTURE METHODS---------------------------
//UTIL ALGORITHMS--------------------------------
ArrayList<PVector> quickSort(ArrayList<PVector> list, PVector comp, int size){
  if(size < 2) return list;
  int pivot = int(random(size));
  int p1 = 0,p2 = 0;
  ArrayList<PVector>list1 = new ArrayList<PVector>();
  ArrayList<PVector>list2 = new ArrayList<PVector>();  
  //reorganize list
  for(int k = 0; k < size; k++){
    if(list.get(k).dist(comp) < list.get(pivot).dist(comp)){
      list1.add(list.get(k));
      p1++;
    }else{
      if(k != pivot){
        list2.add(list.get(k));
        p2++;
      }
    }
  }
  //recursion
  list1 = quickSort(list1, comp, p1);
  list2 = quickSort(list2, comp, p2);
  PVector num_pivot = list.get(pivot);
  //return the list in the right order
  for(int k = 0; k < p1; k++){
    list.set(k,list1.get(k));
  }
  list.set(p1, num_pivot);
  for(int k = 0; k < p2; k++){
    list.set(p1 + k + 1, list2.get(k));
  }
  return list;
}
//-----------------------------------------------
// return phi(x) = standard Gaussian pdf
public static float phi(float x) {
    return (float) (Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI));
}

// return phi(x, mu, signma) = Gaussian pdf with mean mu and stddev sigma
public static float phi(float x, float mu, float sigma) {
    return phi((x - mu) / sigma) / sigma;
}

//color difference
public static int[] rgb2lab(int R, int G, int B) {
    //http://www.brucelindbloom.com
    float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
    float Ls, as, bs;
    float eps = 216.f / 24389.f;
    float k = 24389.f / 27.f;

    float Xr = 0.964221f;  // reference white D50
    float Yr = 1.0f;
    float Zr = 0.825211f;

    // RGB to XYZ
    r = R / 255.f; //R 0..1
    g = G / 255.f; //G 0..1
    b = B / 255.f; //B 0..1

    // assuming sRGB (D65)
    if (r <= 0.04045)
        r = r / 12;
    else
        r = (float) pow((r + 0.055) / 1.055, 2.4);

    if (g <= 0.04045)
        g = g / 12;
    else
        g = (float) pow((g + 0.055) / 1.055, 2.4);

    if (b <= 0.04045)
        b = b / 12;
    else
        b = (float) pow((b + 0.055) / 1.055, 2.4);


    X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
    Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
    Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

    // XYZ to Lab
    xr = X / Xr;
    yr = Y / Yr;
    zr = Z / Zr;

    if (xr > eps)
        fx = (float) pow(xr, 1 / 3.);
    else
        fx = (float) ((k * xr + 16.) / 116.);

    if (yr > eps)
        fy = (float) pow(yr, 1 / 3.);
    else
        fy = (float) ((k * yr + 16.) / 116.);

    if (zr > eps)
        fz = (float) pow(zr, 1 / 3.);
    else
        fz = (float) ((k * zr + 16.) / 116);

    Ls = (116 * fy) - 16;
    as = 500 * (fx - fy);
    bs = 200 * (fy - fz);

    int[] lab = new int[3];
    lab[0] = (int) (2.55 * Ls + .5);
    lab[1] = (int) (as + .5);
    lab[2] = (int) (bs + .5);
    return lab;
}

/**
 * Computes the difference between two RGB colors by converting them to the L*a*b scale and
 * comparing them using the CIE76 algorithm { http://en.wikipedia.org/wiki/Color_difference#CIE76}
 */
public float getColorDifference(int a, int b) {
    int r1, g1, b1, r2, g2, b2;
    r1 = (int)red(a);
    g1 = (int)green(a);
    b1 = (int)blue(a);
    r2 = (int)red(b);
    g2 = (int)green(b);
    b2 = (int)blue(b);
    int[] lab1 = rgb2lab(r1, g1, b1);
    int[] lab2 = rgb2lab(r2, g2, b2);
    return (float) sqrt(pow(lab2[0] - lab1[0], 2) + pow(lab2[1] - lab1[1], 2) + pow(lab2[2] - lab1[2], 2));
}

/*Convert from HSB to RGB*/
public color hsvToRgb(float hue, float saturation, float value) {

    int h = (int)(hue * 6);
    float f = hue * 6 - h;
    float p = value * (1 - saturation);
    float q = value * (1 - f * saturation);
    float t = value * (1 - (1 - f) * saturation);

    switch (h) {
      case 0: return color(value*255  , t*255      , p*255);
      case 1: return color(q*255      , value*255  , p*255);
      case 2: return color(p*255      , value*255  , t*255);
      case 3: return color(p*255      , q*255      , value*255);
      case 4: return color(t*255      , p*255      , value*255);
      case 5: return color(value*255  , p*255      , q*255);
      case 6: return color(value*255  , p*255      , q*255);
      default: throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
    }
}