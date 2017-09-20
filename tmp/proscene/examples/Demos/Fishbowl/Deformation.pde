//PARAMS AND VARS-------------------------------
PShape figure;
PShape deformed_figure;
ArrayList<PVector> vertices = new ArrayList<PVector>();
ArrayList<PVector> deformed_vertices = new ArrayList<PVector>();
Vec r_center;
Vec[] r_bounds;
float scale_factor = 0.15;

//---------------------------------------------

//CONTROL POINTS-------------------------------
ArrayList<PVector> control_points     = new ArrayList<PVector>();
ArrayList<PVector> control_points_out = new ArrayList<PVector>();
//---------------------------------------------

PShape updateShape(Fish agent){
  PShape sh = agent.boid.s;
  //get bounding rect center
  r_bounds = getCube(sh);
  r_center = new Vec((r_bounds[0].x() + r_bounds[1].x())/2., (r_bounds[0].y() + r_bounds[1].y())/2., (r_bounds[0].z() + r_bounds[1].z())/2.);

  //apply texture
  sh = applyTexture(sh);
  sh.rotateX(PI);
  sh.rotateY(3*PI/2);  
  //aux_fish.s.rotateZ(-PI/2);
  //scale the shape to occupy as much the 10% of the width
  float new_sc = agent.boid.frame.scaling()*agent.l/(r_bounds[1].z() - r_bounds[0].z());
  agent.boid.frame.setScaling(new_sc);
  return sh;
}

PShape applyDeformation(Fish agent){
  //get a random model from the existed ones
  int NUM_SHAPES = 15;
  //int RESIZE = 150;
  String path = "shapes/TropicalFish";
  int r = (int)random(NUM_SHAPES - 1) + 1;
  String selected = r < 10 ? "0" + r : "" + r;
  figure = loadShape(path + selected +".obj");
  deformed_figure = loadShape(path + selected +".obj");
  vertices.clear();
  deformed_vertices.clear();  
  for(int j = 0; j < figure.getChildCount(); j++){
    PShape aux = figure.getChild(j);
    for(int i = 0; i < aux.getVertexCount(); i++){
      vertices.add(aux.getVertex(i));  
    }
  }
  for(int j = 0; j < deformed_figure.getChildCount(); j++){
    PShape aux = deformed_figure.getChild(j);
    for(int i = 0; i < aux.getVertexCount(); i++){
      deformed_vertices.add(aux.getVertex(i));  
    }
  }
  //get bounding rect center
  r_bounds = getCube(figure);
  r_center = new Vec((r_bounds[0].x() + r_bounds[1].x())/2., (r_bounds[0].y() + r_bounds[1].y())/2., (r_bounds[0].z() + r_bounds[1].z())/2.);

  //morphTransformationAction();
  //apply texture
  deformed_figure = applyTexture(deformed_figure);
  agent.base_model = r;
  deformed_figure.rotateX(PI);
  deformed_figure.rotateY(3*PI/2.f);
  return deformed_figure;
}

Boid generateBoid(int x, int y, int z, PShape p, Fish agent){
  InteractiveFrame f = new InteractiveFrame(scene);
  f.setPosition(x,y,z);
  f.setTrackingEyeAzimuth(-PApplet.HALF_PI);
  f.setTrackingEyeInclination(PApplet.PI*(4/5));
  f.setTrackingEyeDistance(scene.radius()*10);

  //USED FOR AGENT PURPOSES
  float h = (r_bounds[1].y() - r_bounds[0].y());
  float w = (r_bounds[1].x() - r_bounds[0].x());
  float l = (r_bounds[1].z() - r_bounds[0].z());

  float bounding_rad = h > l ? h : l;
  bounding_rad = w > bounding_rad ? w : bounding_rad;    
  bounding_rad = bounding_rad*1.f/2.f;

  Boid aux_fish = new Boid( new PVector(x,y,z), bounding_rad);
  aux_fish.frame = f;
  aux_fish.s = p;
  /*Scale*/
  float sw = max(r_bounds[1].x() - r_bounds[0].x(), r_bounds[1].y() - r_bounds[0].y());
  sw = max(sw, r_bounds[1].z() - r_bounds[0].z());
  float sw_percent = sw*1./r_world.x();
  if(sw_percent > scale_factor){
    float new_w = scale_factor / sw_percent;
    f.scale(1.*new_w);
  }
  
  agent.h = h;
  agent.w = w;
  agent.l = l;
  min_h = min_h < h ? min_h: h;
  min_l = min_l < l ? min_l: l;
  min_w = min_w < w ? min_w: w;
  max_h = max_h > h ? max_h: h;
  max_l = max_l > l ? max_l: l;
  max_w = max_w > w ? max_w: w;
  
  return aux_fish;
}

void morphTransformationAction(){
  combination();
}