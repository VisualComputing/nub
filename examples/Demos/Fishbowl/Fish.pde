/*Global max and min info*/
float max_c1_diff = abs(color(255,255,255) - color(0,0,0));
float max_c2_diff = max_c1_diff;
float max_c = -9999;   float min_c =  9999;
float max_Da = -9999;  float max_Db = -9999;
float max_pa = -9999;  float max_pb = -9999;
float min_h =  9999;  float min_l =  9999;
float min_w =  9999;  float max_h = -9999;
float max_l = -9999;  float max_w = -9999;
boolean DISABLE_VISION = true;
//-----------------------------------------------
//-----------------------------------------------
public class Fish{
  public Fish(int id){
    this.id = id;
  }
  int id;
  color c1, c2;
  float Da, Db, pa, pb;
  float h, w, l;
  int base_model;
  ArrayList<PVector> control_points = new ArrayList<PVector>();
  float happiness;
  float rate_rad = 3000000;//if we're gonna evolve ommite rate_rad else 200
  PVector vision_rad;
  Boid boid;
  PVector center;
  Fish most_similar = null;
  boolean finish = false;
  float getHappiness(){
      //just check for 10 neighbors, if its not too slow check for all
      float most_sim = -9999;
      for(int i = 0; i < agents.size(); i++){
        Fish f = agents.get(i);
        //if(num_neighbors == 10) break;
        if(f == this) continue;
        //calculate Weight 1        
        if(isInMyVision(f)){
          //Color difference: Weight 15%
          //distance = 3 * |dR| + 4 * |dG| + 3 * |dB|
          /*float d_r = (abs(red(c1) - red(f.c1))/255.);
          float d_g = (abs(green(c1) - green(f.c1))/255.);
          float d_b = (abs(blue(c1) - blue(f.c1))/255.);
          float dist = (3.*d_r + 4.*d_g + 3.*d_b) / 10.;
          float feats_1 = 1-dist;          
          feats_1 *= feats_1*feats_1*feats_1;*/
          float feats_1 = getColorDifference(f.c1, c1)*1./ 375.59 + getColorDifference(f.c2, c2)*1./ 375.59;                    
          feats_1 = 1-feats_1;feats_1 *= feats_1;
          //Params difference: Weight 25%          
          float feats_2 = (1 - abs(Da - f.Da)*1./max_Da) + (1 - abs(Db - f.Db)*1./max_Db);
          feats_2 += (1 - abs(pa - f.pa)*1./max_pa) + (1 - abs(pb - f.pb)*1./max_pb);
          feats_2 = feats_2*1./4;
          //Params size: Weight 60%
          float feats_h = 1- abs(f.h-h)*1./(max_h);// - min_h);  // h >= f.h ? 1/(h*1./f.h) : h*1./f.h; 
          float feats_w = 1- abs(f.w-w)*1./(max_w);// - min_w);  //w >= f.w ? 1/(w*1./f.w) : w*1./f.w; 
          float feats_l = 1- abs(f.l-l)*1./(max_l);//l - min_l);  //l >= f.l ? 1/(l*1./f.l) : l*1./f.l; 
          
          float feat_mod = base_model == f.base_model ? 1 : 0;
          float feats_3 = (feats_h*feats_h + feats_w*feats_w + feats_l*feats_l)*1./3.;
          float sim = .25*feats_1 + .10*feats_2 + .20*feats_3 + .45*feat_mod;
          if(sim > most_sim){
            most_sim = sim;
            most_similar = f;
          }
        }
      }
      return most_sim;      
  }  

  //Not include vision rad
  float getHappinessAvg(){
      float calc = 0;
      float most_sim = -9999;
      int num_neighbors = 0;
      for(Fish f : agents){
        //if(num_neighbors == 10) break;
        if(f == this) continue;
        //calculate Weight 1        
        //Color difference: Weight 15%
        float feats_1 = 1- (abs(red(c1) - red(f.c1))/255.);   
        feats_1 += 1- (abs(blue(c1) - blue(f.c1))/255.);
        feats_1 += 1- (abs(green(c1) - green(f.c1))/255.);
        feats_1 = feats_1*1./3;
        feats_1 *= feats_1*feats_1;
        //Params difference: Weight 25%          
        float feats_2 = (1 - abs(Da - f.Da)*1./max_Da) + (1 - abs(Db - f.Db)*1./max_Db);
        feats_2 += (1 - abs(pa - f.pa)*1./max_pa) + (1 - abs(pb - f.pb)*1./max_pb);
        feats_2 = feats_2*1./4;
        //Params size: Weight 60%
        float feats_h = 1- abs(f.h-h)*1./(max_h - min_h);  // h >= f.h ? 1/(h*1./f.h) : h*1./f.h; 
        float feats_w = 1- abs(f.w-w)*1./(max_w - min_w);  //w >= f.w ? 1/(w*1./f.w) : w*1./f.w; 
        float feats_l = 1- abs(f.l-l)*1./(max_l - min_l);  //l >= f.l ? 1/(l*1./f.l) : l*1./f.l; 
        float feat_mod = base_model == f.base_model ? 1 : 0;
        float feats_3 = (feats_h*feats_h + feats_w*feats_w + feats_l*feats_l)*1./3.;
        float sim = .25*feats_1 + .10*feats_2 + .20*feats_3 + .45*feat_mod;
        if(sim > most_sim){
          most_sim = sim;
          most_similar = f;
        }
        calc += sim; 
        num_neighbors++;
      }
      return calc*1./num_neighbors;
  }  
  
  float fitness(){
      float val = phi(boid.boids.size(), 10, 3);
      return val;
  }

  
  //join to the most appropiate group
  void joinToShoal(){
    if(most_similar == null) return;
    //join to the most similar in the current iteration
    float prop = boid.boids.size()*1./(boid.boids.size() + most_similar.boid.boids.size());
    
    if(finish) return;
    for(int i = 0; i < this.boid.boids.size(); i++){
      most_similar.boid.boids.add(boid.boids.get(i));
    }
    this.boid.boids = most_similar.boid.boids; 
    Fish f = new Fish(id++);
    //calculate avg properties
    f.c1 = (int)((1-prop)*1.*most_similar.c1 + (prop)*1.*c1);
    f.c2 = (int)((1-prop)*1.*most_similar.c2 + (prop)*1.*c2);
    f.Da = (1-prop)*1.*most_similar.Da + (prop)*1.*Da;
    f.Db = (1-prop)*1.*most_similar.Db + (prop)*1.*Db;
    f.pa = (1-prop)*1.*most_similar.pa + (prop)*1.*pa;
    f.pb = (1-prop)*1.*most_similar.pb + (prop)*1.*pb;
    f.w  = (1-prop)*1.*most_similar.w  + (prop)*1.*w;
    f.h  = (1-prop)*1.*most_similar.h  + (prop)*1.*h;
    f.l  = (1-prop)*1.*most_similar.l  + (prop)*1.*l;
    f.base_model = prop >= 0.5 ? base_model : most_similar.base_model;
    f.boid = boid;
    //remove one of this instances
    agents.remove(most_similar);     
    agents.remove(this);    
    this.finish = true;
    most_similar.finish = true;
    agents.add(f);    
    f.updateVision();
  }
  
  boolean isInMyVision(Fish f){
    if(DISABLE_VISION) return true;
    float dx = min(abs(f.center.x + vision_rad.x - center.x), abs(f.center.x - vision_rad.x - center.x));
    float dy = min(abs(f.center.y + vision_rad.y - center.y), abs(f.center.y - vision_rad.y - center.y));
    float dz = min(abs(f.center.z + vision_rad.z - center.z), abs(f.center.z - vision_rad.z - center.z));
    return dx <= vision_rad.x*1 + rate_rad && dy <= vision_rad.y*1 + rate_rad && dz <= vision_rad.z*1 + rate_rad;
  }
  
  void updateVision(){
    boid.updateCoords();
    center =  new PVector((boid.min_coords.x +  boid.max_coords.x)*1./2, (boid.min_coords.y +  boid.max_coords.y)*1./2, (boid.min_coords.z +  boid.max_coords.z)*1./2);
    vision_rad = PVector.sub(boid.max_coords,center);
  }
  
  public void run(){
    float calc = getHappiness();
    if(calc >= 0.9)
      joinToShoal();
  }
}
