//Based on [Schaefer] : Image Deformation Using Moving Least Squares
//First Approach: Affine Transformations
int step_per_point = 10;
float mod_factor = 8.;

double[][] A;
double[][] w;
void getA(ArrayList<PVector> img, ArrayList<PVector> control){
  A = new double[img.size()][control.size()];
  w = new double[img.size()][control.size()];
  int counter = 0;
  for(PVector v : img){
    double sum_weights = 0;
    PVector sum_weights_per_p = new PVector(0,0,0);
    PVector p_star;
    for(int k = 0; k < control.size(); k++){
        PVector pk = control.get(k); 
        double den = (PVector.dist(v, pk)*PVector.dist(v, pk));
        den = den < 0.00000000001 ? 0.00000000001 : den;
        w[counter][k] = 1/den;
        sum_weights += w[counter][k]; 
        sum_weights_per_p.x = sum_weights_per_p.x  + (float)w[counter][k]*pk.x;  
        sum_weights_per_p.y = sum_weights_per_p.y  + (float)w[counter][k]*pk.y;  
        sum_weights_per_p.z = sum_weights_per_p.z  + (float)w[counter][k]*pk.z;  
    }
    p_star = PVector.mult(sum_weights_per_p, 1.0/(float)sum_weights);
    PVector v_minus_p_s = PVector.sub(v,p_star);    
    for(int i = 0; i < control.size(); i++){
      double[][] pt_per_wp = new double[3][3]; 
      for(int j = 0; j < control.size(); j++){
        PVector pj = control.get(j); 
        PVector p_hat_j = PVector.sub(pj,p_star);
        pt_per_wp[0][0] += w[counter][j]*p_hat_j.x*p_hat_j.x;            
        pt_per_wp[0][1] += w[counter][j]*p_hat_j.y*p_hat_j.x;            
        pt_per_wp[0][2] += w[counter][j]*p_hat_j.z*p_hat_j.x;            
        pt_per_wp[1][0] += w[counter][j]*p_hat_j.x*p_hat_j.y;            
        pt_per_wp[1][1] += w[counter][j]*p_hat_j.y*p_hat_j.y;            
        pt_per_wp[1][2] += w[counter][j]*p_hat_j.z*p_hat_j.y;            
        pt_per_wp[2][0] += w[counter][j]*p_hat_j.x*p_hat_j.z;            
        pt_per_wp[2][1] += w[counter][j]*p_hat_j.y*p_hat_j.z;            
        pt_per_wp[2][2] += w[counter][j]*p_hat_j.z*p_hat_j.z;            
      }   
      PVector pi = control.get(i); 
      PVector p_hat_i = PVector.sub(pi,p_star);
      //inverse
      // computes the inverse of a matrix m
      double det = pt_per_wp[0][0] * (pt_per_wp[1][1] * pt_per_wp[2][2] - pt_per_wp[2][1] * pt_per_wp[1][2]) -
                   pt_per_wp[0][1] * (pt_per_wp[1][0] * pt_per_wp[2][2] - pt_per_wp[1][2] * pt_per_wp[2][0]) +
                   pt_per_wp[0][2] * (pt_per_wp[1][0] * pt_per_wp[2][1] - pt_per_wp[1][1] * pt_per_wp[2][0]);
      
      double invdet = 1 / det;
      double[][] inv_pt_per_wp = new double[3][3];      

      inv_pt_per_wp[0][0] = (pt_per_wp[1][1] * pt_per_wp[2][2] - pt_per_wp[2][1] * pt_per_wp[1][2]) * invdet;
      inv_pt_per_wp[0][1] = (pt_per_wp[0][2] * pt_per_wp[2][1] - pt_per_wp[0][1] * pt_per_wp[2][2]) * invdet;
      inv_pt_per_wp[0][2] = (pt_per_wp[0][1] * pt_per_wp[1][2] - pt_per_wp[0][2] * pt_per_wp[1][1]) * invdet;
      inv_pt_per_wp[1][0] = (pt_per_wp[1][2] * pt_per_wp[2][0] - pt_per_wp[1][0] * pt_per_wp[2][2]) * invdet;
      inv_pt_per_wp[1][1] = (pt_per_wp[0][0] * pt_per_wp[2][2] - pt_per_wp[0][2] * pt_per_wp[2][0]) * invdet;
      inv_pt_per_wp[1][2] = (pt_per_wp[1][0] * pt_per_wp[0][2] - pt_per_wp[0][0] * pt_per_wp[1][2]) * invdet;
      inv_pt_per_wp[2][0] = (pt_per_wp[1][0] * pt_per_wp[2][1] - pt_per_wp[2][0] * pt_per_wp[1][1]) * invdet;
      inv_pt_per_wp[2][1] = (pt_per_wp[2][0] * pt_per_wp[0][1] - pt_per_wp[0][0] * pt_per_wp[2][1]) * invdet;
      inv_pt_per_wp[2][2] = (pt_per_wp[0][0] * pt_per_wp[1][1] - pt_per_wp[1][0] * pt_per_wp[0][1]) * invdet;
      double[] Ai_1 = new double[3];
      Ai_1[0]= (v_minus_p_s.x * inv_pt_per_wp[0][0]) + (v_minus_p_s.y * inv_pt_per_wp[0][1]) + (v_minus_p_s.z * inv_pt_per_wp[0][2]); 
      Ai_1[1]= (v_minus_p_s.x * inv_pt_per_wp[1][0]) + (v_minus_p_s.y * inv_pt_per_wp[1][1]) + (v_minus_p_s.z * inv_pt_per_wp[1][2]); 
      Ai_1[2]= (v_minus_p_s.x * inv_pt_per_wp[2][0]) + (v_minus_p_s.y * inv_pt_per_wp[2][1]) + (v_minus_p_s.z * inv_pt_per_wp[2][2]); 

      A[counter][i] = Ai_1[0] * p_hat_i.x * w[counter][i] + Ai_1[1] * p_hat_i.y * w[counter][i] + Ai_1[2] * p_hat_i.z * w[counter][i];    
    }
    counter++;
  }
}

ArrayList<PVector> calculateNewImage(ArrayList<PVector> img, ArrayList<PVector> out_control){
  if(out_control.size() < 4) return img;
  
  ArrayList<PVector> dest = new ArrayList<PVector>();
  int counter = 0;
  for(PVector v : img){
    double sum_weights = 0;
    PVector sum_weights_per_q = new PVector(0,0,0);
    PVector q_star;
    for(int k = 0; k < out_control.size(); k++){
        PVector qk = out_control.get(k); 
        sum_weights += w[counter][k]; 
        sum_weights_per_q.x = sum_weights_per_q.x  + (float)w[counter][k]*qk.x;  
        sum_weights_per_q.y = sum_weights_per_q.y  + (float)w[counter][k]*qk.y;  
        sum_weights_per_q.z = sum_weights_per_q.z  + (float)w[counter][k]*qk.z;  
    }
    q_star = PVector.mult(sum_weights_per_q, 1.0/(float)sum_weights);
    PVector sum_A_q_j = new PVector (0,0,0);
    for(int j = 0; j < out_control.size(); j++){
        PVector qj = out_control.get(j); 
        PVector q_hat_j = PVector.sub(qj,q_star);
        sum_A_q_j.x += A[counter][j]*q_hat_j.x;  
        sum_A_q_j.y += A[counter][j]*q_hat_j.y;  
        sum_A_q_j.z += A[counter][j]*q_hat_j.z;  
    }
    PVector f_a_v = PVector.add(sum_A_q_j, q_star);
    dest.add(f_a_v);
    counter++;
  }
  return dest;
}

void updateControlPoints(){
  if(control_points.size() < 4) return;
  getA(vertices,control_points);
}

void updateControlPoints(ArrayList<PVector> img){
  if(control_points.size() < 4) return;  
  getA(img,control_points);
}

//SOME PREDEFINED DEFORMATIONS
void addControlPointsAuto(boolean rand){
  //clear
  control_points.clear();
  control_points_out.clear();
  for(int i = 0; i < vertices.size(); i+=step_per_point){
    //get coordinates in local frame
    //control_points.add(edges.get(i));
    if(!rand){
      control_points.add(vertices.get(i));
      control_points_out.add(vertices.get(i));
    }else{
      PVector v = vertices.get(i);
      PVector new_v = new PVector(v.x - r_center.x(), v.y - r_center.y(), v.z - r_center.z());                                          
      new_v.mult(random(1,2));
      new_v.add(v);
      control_points.add(new_v);
      float r_out_x = (int)(random(0,100)) % 2 == 0 ? random(1,1.5) : -1*random(1,1.5);
      float r_out_y = (int)(random(0,100)) % 2 == 0 ? random(1,1.5) : -1*random(1,1.5);
      float r_out_z = (int)(random(0,100)) % 2 == 0 ? random(1,1.5) : -1*random(1,1.5);
      control_points_out.add(new PVector(new_v.x*r_out_x, new_v.y*r_out_y, new_v.z*r_out_z));
    }
  }  
}

void scaleX(boolean clear){
  //clear
  if(clear){
    control_points.clear();
    control_points_out.clear();
  }  
  float r_width = r_bounds[1].x() - r_bounds[0].x();
  if(r_width < 0) r_width = -1*r_width;
  //two parallel faces surrounding the width of the shape
  PVector[] f1 = new PVector[4];
  //top left
  f1[0] = new PVector(r_bounds[0].x(), r_bounds[1].y(), r_bounds[1].z()); 
  //bottom left
  f1[1] = new PVector(r_bounds[0].x(), r_bounds[0].y(), r_bounds[1].z());
  //top right
  f1[2] = new PVector(r_bounds[0].x(), r_bounds[1].y(), r_bounds[0].z());
  //bottom right
  f1[3] = new PVector(r_bounds[0].x(), r_bounds[0].y(), r_bounds[0].z());
  PVector f1_c  = new PVector(r_bounds[0].x(),(r_bounds[0].y()+r_bounds[1].y())/2., (r_bounds[0].z()+r_bounds[1].z())/2.);  

  PVector[] f2 = new PVector[4];
  //top left
  f2[0] = new PVector(r_bounds[1].x(), r_bounds[1].y(), r_bounds[1].z()); 
  //bottom left
  f2[1] = new PVector(r_bounds[1].x(), r_bounds[0].y(), r_bounds[1].z());
  //top right
  f2[2] = new PVector(r_bounds[1].x(), r_bounds[1].y(), r_bounds[0].z());
  //bottom right
  f2[3] = new PVector(r_bounds[1].x(), r_bounds[0].y(), r_bounds[0].z());
  PVector f2_c  = new PVector(r_bounds[1].x(),(r_bounds[0].y()+r_bounds[1].y())/2., (r_bounds[0].z()+r_bounds[1].z())/2.);  
  PVector movement = new PVector((r_width/8)*randomGaussian(), 0,0);
  PVector new_f1_c = PVector.add(f1_c, movement);
  PVector new_f2_c = PVector.sub(f2_c, movement);
  
  for(int i = 0; i < 4; i++){
    PVector new_f1 = PVector.add(f1[i], movement);
    PVector new_f2 = PVector.sub(f2[i], movement);
    control_points.add(f1[i]);
    control_points_out.add(new_f1);  
    control_points.add(f2[i]);
    control_points_out.add(new_f2);  
  }
  control_points.add(f1_c);
  control_points_out.add(new_f1_c);  
  control_points.add(f2_c);
  control_points_out.add(new_f2_c);  
}

void scaleY(boolean clear){
  //clear
  if(clear){
    control_points.clear();
    control_points_out.clear();
  }  
  float r_width = r_bounds[1].y() - r_bounds[0].y();
  if(r_width < 0) r_width = -1*r_width;
  //two parallel faces surrounding the height of the shape
  PVector[] f1 = new PVector[4];
  //top left
  f1[0] = new PVector(r_bounds[0].x(), r_bounds[0].y(), r_bounds[1].z()); 
  //bottom left
  f1[1] = new PVector(r_bounds[0].x(), r_bounds[0].y(), r_bounds[0].z());
  //top right
  f1[2] = new PVector(r_bounds[1].x(), r_bounds[0].y(), r_bounds[1].z());
  //bottom right
  f1[3] = new PVector(r_bounds[1].x(), r_bounds[0].y(), r_bounds[0].z());
  PVector f1_c  = new PVector((r_bounds[0].x()+r_bounds[1].x())/2., r_bounds[0].y(), (r_bounds[0].z()+r_bounds[1].z())/2.);  
  PVector[] f2 = new PVector[4];
  //top left
  f2[0] = new PVector(r_bounds[0].x(), r_bounds[1].y(), r_bounds[1].z()); 
  //bottom left
  f2[1] = new PVector(r_bounds[0].x(), r_bounds[1].y(), r_bounds[0].z());
  //top right
  f2[2] = new PVector(r_bounds[1].x(), r_bounds[1].y(), r_bounds[1].z());
  //bottom right
  f2[3] = new PVector(r_bounds[1].x(), r_bounds[1].y(), r_bounds[0].z());
  PVector f2_c  = new PVector((r_bounds[0].x()+r_bounds[1].x())/2., r_bounds[1].y(), (r_bounds[0].z()+r_bounds[1].z())/2.);  
  PVector movement = new PVector(0, (r_width/8)*randomGaussian(), 0);
  PVector new_f1_c = PVector.add(f1_c, movement);
  PVector new_f2_c = PVector.sub(f2_c, movement);
  
  for(int i = 0; i < 4; i++){
    PVector new_f1 = PVector.add(f1[i], movement);
    PVector new_f2 = PVector.sub(f2[i], movement);
    control_points.add(f1[i]);
    control_points_out.add(new_f1);  
    control_points.add(f2[i]);
    control_points_out.add(new_f2);  
  }
  control_points.add(f1_c);
  control_points_out.add(new_f1_c);  
  control_points.add(f2_c);
  control_points_out.add(new_f2_c);  
}

void scaleZ(boolean clear){
  //clear
  if(clear){
    control_points.clear();
    control_points_out.clear();
  }  
  float r_width = r_bounds[1].z() - r_bounds[0].z();
  if(r_width < 0) r_width = -1*r_width;
  //two parallel faces surrounding the height of the shape
  PVector[] f1 = new PVector[4];
  //top left
  f1[0] = new PVector(r_bounds[0].x(), r_bounds[1].y(), r_bounds[0].z()); 
  //bottom left
  f1[1] = new PVector(r_bounds[0].x(), r_bounds[0].y(), r_bounds[0].z());
  //top right
  f1[2] = new PVector(r_bounds[1].x(), r_bounds[1].y(), r_bounds[0].z());
  //bottom right
  f1[3] = new PVector(r_bounds[1].x(), r_bounds[0].y(), r_bounds[0].z());
  PVector f1_c  = new PVector((r_bounds[0].x()+r_bounds[1].x())/2., (r_bounds[0].y()+r_bounds[1].y())/2., r_bounds[0].z());  
  PVector[] f2 = new PVector[4];
  //top left
  f2[0] = new PVector(r_bounds[0].x(), r_bounds[1].y(), r_bounds[1].z()); 
  //bottom left
  f2[1] = new PVector(r_bounds[0].x(), r_bounds[0].y(), r_bounds[1].z());
  //top right
  f2[2] = new PVector(r_bounds[1].x(), r_bounds[1].y(), r_bounds[1].z());
  //bottom right
  f2[3] = new PVector(r_bounds[1].x(), r_bounds[0].y(), r_bounds[1].z());
  PVector f2_c  = new PVector((r_bounds[0].x()+r_bounds[1].x())/2., (r_bounds[0].y()+r_bounds[1].y())/2., r_bounds[1].z());  
  PVector movement = new PVector(0,0, (r_width/8)*randomGaussian());
  PVector new_f1_c = PVector.add(f1_c, movement);
  PVector new_f2_c = PVector.sub(f2_c, movement);
  
  for(int i = 0; i < 4; i++){
    PVector new_f1 = PVector.add(f1[i], movement);
    PVector new_f2 = PVector.sub(f2[i], movement);
    control_points.add(f1[i]);
    control_points_out.add(new_f1);  
    control_points.add(f2[i]);
    control_points_out.add(new_f2);  
  }
  control_points.add(f1_c);
  control_points_out.add(new_f1_c);  
  control_points.add(f2_c);
  control_points_out.add(new_f2_c);  
}

void applyHorizontalZXSpline(boolean clear){
  //clear
  if(clear){
    control_points.clear();
    control_points_out.clear();
  }
  ArrayList<PVector> spline_control = new ArrayList<PVector>();

  int quantity = 12;
  float e = 0.5; //get a new point for each 2 control points
  float t = 0;
  float min_x = r_bounds[0].x();
  float min_y = r_bounds[0].y();
  float min_z = r_bounds[0].z();
  float max_x = r_bounds[1].x();
  float max_y = r_bounds[1].y();
  float max_z = r_bounds[1].z();
  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

  for(int i = 0; i < quantity; i++){
      float x_pos = min_x + i*(r_w/(quantity-1));
      float y_mode = min_y; 
      float y_pos = y_mode + random(-r_h*1./mod_factor, r_h*1./mod_factor); 
      spline_control.add(new PVector(x_pos, y_pos, (max_z + min_z)/2.));
  }
  spline_control = drawCurve(spline_control, t, e, false);
  //apply the same transformation to all the points
  float y_mode = min_y; 
  float y_pos = y_mode + random(-r_h*1./mod_factor, r_h*1./mod_factor);
  for(PVector point : spline_control){
    control_points.add(new PVector(point.x, y_pos, point.z));
  }
  control_points_out.addAll(spline_control);  
  //put the same calculated points in the oposite place
  //apply the same transformation to all the points
  float inv_y_mode = min_y + r_h;
  y_pos = -(y_pos - y_mode) + inv_y_mode; 
  for(int i = 0; i < spline_control.size(); i++){
    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
    control_points.add(new PVector(point.x, y_pos, point.z));
    point.y = -(point.y - y_mode) + inv_y_mode;
    control_points_out.add(point);
  }
  //add 2 anchor points
  control_points.add(new PVector((min_x + max_x)/2., (min_y + max_y)/2., min_z));
  control_points.add(new PVector((min_x + max_x)/2., (min_y + max_y)/2., max_z));
  control_points_out.add(new PVector((min_x + max_x)/2., (min_y + max_y)/2., min_z));
  control_points_out.add(new PVector((min_x + max_x)/2., (min_y + max_y)/2., max_z));
}

void applyVerticalZXSpline(boolean clear){
  //clear
  if(clear){
    control_points.clear();
    control_points_out.clear();
  }
  ArrayList<PVector> spline_control = new ArrayList<PVector>();

  int quantity = 12;
  float e = 0.5; //get a new point for each 2 control points
  float t = 0;
  float min_x = r_bounds[0].x();
  float min_y = r_bounds[0].y();
  float min_z = r_bounds[0].z();
  float max_x = r_bounds[1].x();
  float max_y = r_bounds[1].y();
  float max_z = r_bounds[1].z();
  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

  for(int i = 0; i < quantity; i++){
      float z_pos = min_z + i*(r_l/(quantity-1));
      float y_mode = min_y; 
      float y_pos = y_mode + random(-r_h*1./mod_factor, r_h*1./mod_factor); 
      spline_control.add(new PVector((max_x + min_x)/2., y_pos, z_pos));
  }
  spline_control = drawCurve(spline_control, t, e, false);
  //apply the same transformation to all the points
  float y_mode = min_y; 
  float y_pos = y_mode + random(-r_h*1./mod_factor, r_h*1./mod_factor);
  for(PVector point : spline_control){
    control_points.add(new PVector(point.x, y_pos, point.z));
  }
  control_points_out.addAll(spline_control);  
  //put the same calculated points in the oposite place
  //apply the same transformation to all the points
  float inv_y_mode = min_y + r_h;
  y_pos = -(y_pos - y_mode) + inv_y_mode; 
  for(int i = 0; i < spline_control.size(); i++){
    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
    control_points.add(new PVector(point.x, y_pos, point.z));
    point.y = -(point.y - y_mode) + inv_y_mode;
    control_points_out.add(point);
  }
  //add 2 anchor points
  control_points.add(new PVector(min_x, (min_y + max_y)/2., (min_z + max_z)/2.));
  control_points.add(new PVector(max_x, (min_y + max_y)/2., (min_z + max_z)/2.));
  control_points_out.add(new PVector(min_x, (min_y + max_y)/2., (min_z + max_z)/2.));
  control_points_out.add(new PVector(max_x, (min_y + max_y)/2., (min_z + max_z)/2.));
}



void applyHorizontalYZSpline(boolean clear){
  //clear
  if(clear){
    control_points.clear();
    control_points_out.clear();
  }
  ArrayList<PVector> spline_control = new ArrayList<PVector>();

  int quantity = 12;
  float e = 0.5; //get a new point for each 2 control points
  float t = 0;
  float min_x = r_bounds[0].x();
  float min_y = r_bounds[0].y();
  float min_z = r_bounds[0].z();
  float max_x = r_bounds[1].x();
  float max_y = r_bounds[1].y();
  float max_z = r_bounds[1].z();
  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

  for(int i = 0; i < quantity; i++){
      float z_pos = min_z + i*(r_l/(quantity-1));
      float x_mode = min_x; 
      float x_pos = x_mode + random(-r_w*1./mod_factor, r_w*1./mod_factor); 
      spline_control.add(new PVector(x_pos, (max_y + min_y)/2., z_pos));
  }
  spline_control = drawCurve(spline_control, t, e, false);
  //apply the same transformation to all the points
  float x_mode = min_x; 
  float x_pos = x_mode + random(-r_w*1./mod_factor, r_w*1./mod_factor);
  for(PVector point : spline_control){
    control_points.add(new PVector(x_pos, point.y, point.z));
  }
  control_points_out.addAll(spline_control);  
  //put the same calculated points in the oposite place
  //apply the same transformation to all the points
  float inv_x_mode = min_x + r_w;
  x_pos = -(x_pos - x_mode) + inv_x_mode; 
  for(int i = 0; i < spline_control.size(); i++){
    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
    control_points.add(new PVector(x_pos, point.y, point.z));
    point.x = -(point.x - x_mode) + inv_x_mode;
    control_points_out.add(point);
  }
  //add 2 anchor points
  control_points.add(new PVector((min_x + max_x)/2., min_y, (min_z + max_z)/2.));
  control_points.add(new PVector((min_x + max_x)/2., max_y, (min_z + max_z)/2.));
  control_points_out.add(new PVector((min_x + max_x)/2., min_y, (min_z + max_z)/2.));
  control_points_out.add(new PVector((min_x + max_x)/2., max_y, (min_z + max_z)/2.));
}

void applyVerticalYZSpline(boolean clear){
  //clear
  if(clear){
    control_points.clear();
    control_points_out.clear();
  }
  ArrayList<PVector> spline_control = new ArrayList<PVector>();

  int quantity = 12;
  float e = 0.5; //get a new point for each 2 control points
  float t = 0;
  float min_x = r_bounds[0].x();
  float min_y = r_bounds[0].y();
  float min_z = r_bounds[0].z();
  float max_x = r_bounds[1].x();
  float max_y = r_bounds[1].y();
  float max_z = r_bounds[1].z();
  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

  for(int i = 0; i < quantity; i++){
      float y_pos = min_y + i*(r_h/(quantity-1));
      float x_mode = min_x; 
      float x_pos = x_mode + random(-r_w*1./mod_factor, r_w*1./mod_factor); 
      spline_control.add(new PVector(x_pos, y_pos, (max_z + min_z)/2.));
  }
  spline_control = drawCurve(spline_control, t, e, false);
  //apply the same transformation to all the points
  float x_mode = min_x; 
  float x_pos = x_mode + random(-r_w*1./mod_factor, r_w*1./mod_factor);
  for(PVector point : spline_control){
    control_points.add(new PVector(x_pos, point.y, point.z));
  }
  control_points_out.addAll(spline_control);  
  //put the same calculated points in the oposite place
  //apply the same transformation to all the points
  float inv_x_mode = min_x + r_w;
  x_pos = -(x_pos - x_mode) + inv_x_mode; 
  for(int i = 0; i < spline_control.size(); i++){
    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
    control_points.add(new PVector(x_pos, point.y, point.z));
    point.x = -(point.x - x_mode) + inv_x_mode;
    control_points_out.add(point);
  }
  //add 2 anchor points
  control_points.add(new PVector((min_x + max_x)/2., (min_y + max_y)/2., min_z));
  control_points.add(new PVector((min_x + max_x)/2., (min_y + max_y)/2., max_z));
  control_points_out.add(new PVector((min_x + max_x)/2., (min_y + max_y)/2., min_z));
  control_points_out.add(new PVector((min_x + max_x)/2., (min_y + max_y)/2., max_z));
}

void applyVerticalXYSpline(boolean clear){
  //clear
  if(clear){
    control_points.clear();
    control_points_out.clear();
  }
  ArrayList<PVector> spline_control = new ArrayList<PVector>();

  int quantity = 12;
  float e = 0.5; //get a new point for each 2 control points
  float t = 0;
  float min_x = r_bounds[0].x();
  float min_y = r_bounds[0].y();
  float min_z = r_bounds[0].z();
  float max_x = r_bounds[1].x();
  float max_y = r_bounds[1].y();
  float max_z = r_bounds[1].z();
  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

  for(int i = 0; i < quantity; i++){
      float y_pos = min_y + i*(r_h/(quantity-1));
      float z_mode = min_z; 
      float z_pos = z_mode + random(-r_l*1./mod_factor, r_l*1./mod_factor); 
      spline_control.add(new PVector((max_x + min_x)/2., y_pos, z_pos));
  }
  spline_control = drawCurve(spline_control, t, e, false);
  //apply the same transformation to all the points
  float z_mode = min_z; 
  float z_pos = z_mode + random(-r_l*1./mod_factor, r_l*1./mod_factor);
  for(PVector point : spline_control){
    control_points.add(new PVector(point.x, point.y, z_pos));
  }
  control_points_out.addAll(spline_control);  
  //put the same calculated points in the oposite place
  //apply the same transformation to all the points
  float inv_z_mode = min_z + r_l;
  z_pos = -(z_pos - z_mode) + inv_z_mode; 
  for(int i = 0; i < spline_control.size(); i++){
    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
    control_points.add(new PVector(point.x, point.y, z_pos));
    point.z = -(point.z - z_mode) + inv_z_mode;
    control_points_out.add(point);
  }
  //add 2 anchor points
  control_points.add(new PVector(min_x, (min_y + max_y)/2., (min_z + max_z)/2.));
  control_points.add(new PVector(max_x, (min_y + max_y)/2., (min_z + max_z)/2.));
  control_points_out.add(new PVector(min_x, (min_y + max_y)/2., (min_z + max_z)/2.));
  control_points_out.add(new PVector(max_x, (min_y + max_y)/2., (min_z + max_z)/2.));
}

void applyHorizontalXYSpline(boolean clear){
  //clear
  if(clear){
    control_points.clear();
    control_points_out.clear();
  }
  ArrayList<PVector> spline_control = new ArrayList<PVector>();

  int quantity = 12;
  float e = 0.5; //get a new point for each 2 control points
  float t = 0;
  float min_x = r_bounds[0].x();
  float min_y = r_bounds[0].y();
  float min_z = r_bounds[0].z();
  float max_x = r_bounds[1].x();
  float max_y = r_bounds[1].y();
  float max_z = r_bounds[1].z();
  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

  for(int i = 0; i < quantity; i++){
      float x_pos = min_x + i*(r_w/(quantity-1));
      float z_mode = min_z; 
      float z_pos = z_mode + random(-r_l*1./mod_factor, r_l*1./mod_factor); 
      spline_control.add(new PVector(x_pos, (max_y + min_y)/2., z_pos));
  }
  spline_control = drawCurve(spline_control, t, e, false);
  //apply the same transformation to all the points
  float z_mode = min_z; 
  float z_pos = z_mode + random(-r_l*1./mod_factor, r_l*1./mod_factor);
  for(PVector point : spline_control){
    control_points.add(new PVector(point.x, point.y, z_pos));
  }
  control_points_out.addAll(spline_control);  
  //put the same calculated points in the oposite place
  //apply the same transformation to all the points
  float inv_z_mode = min_z + r_l;
  z_pos = -(z_pos - z_mode) + inv_z_mode; 
  for(int i = 0; i < spline_control.size(); i++){
    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
    control_points.add(new PVector(point.x, point.y, z_pos));
    point.z = -(point.z - z_mode) + inv_z_mode;
    control_points_out.add(point);
  }
  //add 2 anchor points
  control_points.add(new PVector((min_x + max_x)/2., min_y, (min_z + max_z)/2.));
  control_points.add(new PVector((min_x + max_x)/2., max_y, (min_z + max_z)/2.));
  control_points_out.add(new PVector((min_x + max_x)/2., min_y, (min_z + max_z)/2.));
  control_points_out.add(new PVector((min_x + max_x)/2., max_y, (min_z + max_z)/2.));
}

void combination(){
  ArrayList<PVector> new_img = new ArrayList<PVector>();
  new_img.addAll(vertices);
  //splineht
  applyHorizontalYZSpline(true);
  updateControlPoints(new_img);
  new_img = calculateNewImage(new_img,control_points_out);
  //splines
  applyVerticalZXSpline(true);
  updateControlPoints(new_img);
  new_img = calculateNewImage(new_img,control_points_out);
  applyVerticalXYSpline(true);
  updateControlPoints(new_img);
  new_img = calculateNewImage(new_img,control_points_out);
  //scalew
  scaleX(true);
  updateControlPoints(new_img);
  new_img = calculateNewImage(new_img,control_points_out);
  //scaleh
  scaleY(true);
  updateControlPoints(new_img);
  new_img = calculateNewImage(new_img,control_points_out);
  //scalel
  scaleZ(true);
  updateControlPoints(new_img);
  new_img = calculateNewImage(new_img,control_points_out);
  //modify the shape
  deformed_vertices = new_img;
  setVertices(deformed_figure, deformed_vertices);
}