/*Adapted version of Matt Wetmore and Jean Pierre Charalambos
Here a Boid encapsulates all the required behavior to render and modify the
movement of a Fish
*/
class Boid{
  PShape s;
  InteractiveFrame frame;
  Quat quat;
  PVector position;
  PVector velocity;
  PVector acceleration;
  PVector aligment, cohesion, separation;
  float radius; //num of neighbors
  PVector min_coords;
  PVector max_coords;
  float maxSpeed = 5;
  float maxSteerForce = .1f;
  float flap = 0;
  float t = 0;
  ArrayList<Boid> boids; //List of Fishes in the same group 
  ArrayList<Boid> predators; //List of Fishes to Avoid  
  ArrayList<Boid> preys; //List of Fishes to Follow  
  float weight_alignment, weight_cohesion, weight_separation, weight_madness;
  float x_seed, y_seed, z_seed; //used to random walk with perlin noise
  float bounding_rad;
  
  
  public Boid(PVector ipos, float bounding_rad){
    position = new PVector(ipos.x, ipos.y, ipos.z);
    //TO DO ADD FRAME AND SHAPE CONFS
    velocity = new PVector(random(-maxSpeed,maxSpeed),random(-maxSpeed,maxSpeed),random(-maxSpeed,maxSpeed));
    acceleration = new PVector(0,0,0);
    radius = 100;
    weight_cohesion = 3.; 
    weight_separation = 1.4;
    weight_alignment = 2.;
    weight_madness = 0.01;
    x_seed = random(0,1000);
    y_seed = random(3000, 4000);
    z_seed = random(7000, 8000);
    min_coords = position.get();
    max_coords = position.get();
    predators = new ArrayList<Boid>();
    preys = new ArrayList<Boid>();
    maxSpeed = 5 - 2*scale_factor;
    this.bounding_rad = bounding_rad;
}
  
  public Boid(PVector ipos, PVector ivel, float r, float bounding_rad){
    position = new PVector(ipos.x, ipos.y, ipos.z);
    //TO DO ADD FRAME AND SHAPE CONFS
    velocity = new PVector(ivel.x, ivel.y, ivel.z);
    acceleration = new PVector(0,0,0);
    predators = new ArrayList<Boid>();
    preys = new ArrayList<Boid>();    
    radius = r;
    maxSpeed = 5 - 2*scale_factor;
    this.bounding_rad = bounding_rad;    
  }

  Boid getNearest(){
    Boid n = null;
    for(Boid b : boids){
      if(b == this) continue;
      if(n == null) n = b;
      if(b.position.dist(position) < position.dist(n.position)) n = b;
    }
    return n;
  }

  void run(){
    t += .1;
    flap = 10 * sin(t);
    avoidWalls();
    Boid nearest = getNearest();
    avoidPredators();
    followPreys();
    shoal(boids); 
    move(nearest);
  }
  
  void avoidWalls(){
    float w = weight_alignment + weight_cohesion + weight_separation + weight_madness;
    w = w + .9*w;
    acceleration.add(PVector.mult(avoid(new PVector(position.x,        r_world.y(),     position.z),       true), w));
    acceleration.add(PVector.mult(avoid(new PVector(position.x,        0,               position.z),       true), w));
    acceleration.add(PVector.mult(avoid(new PVector(r_world.x(),       position.y,      position.z),       true), w));
    acceleration.add(PVector.mult(avoid(new PVector(0,                 position.y,      position.z),       true), w));
    acceleration.add(PVector.mult(avoid(new PVector(position.x,        position.y,      0),                true), w));
    acceleration.add(PVector.mult(avoid(new PVector(position.x,        position.y,      r_world.z()),      true), w));
  }
  
  void avoidPredators(){
    float w = weight_cohesion + weight_separation + weight_alignment + weight_madness;
    w = w + .6*w;
    Boid n = null;
    for(Boid b : predators){
      if(n == null) n = b;
      if(b.position.dist(position) < position.dist(n.position)) n = b;
    }
    //avoid the nearest
    if(n != null)acceleration.add(PVector.mult(avoid(n.position,       true), w));
  }
  
  void followPreys(){
    float w = weight_cohesion + weight_separation + weight_alignment + weight_madness;
    w = w + .4*w;
    Boid n = null;
    for(Boid b : preys){
      //follow the nearest
      if(n == null) n = b;
      if(b.position.dist(position) < position.dist(n.position)) n = b;
    }  
    if(n != null)acceleration.add(PVector.mult(avoid(n.position,       true), -1*w));
  }
  
  PVector avoid(PVector target, boolean weight){
    PVector steer = PVector.sub(position, target);  
    if(weight){
      steer.mult(1/sq(PVector.dist(position,target)));
    }
    return steer;
  }

  void shoal(ArrayList<Boid> bl){
    aligment   = getAlignment(bl);
    cohesion   = getCohesion(bl);
    separation = getSeparation(bl);
    acceleration.add(PVector.mult(aligment,weight_alignment));    
    acceleration.add(PVector.mult(cohesion,weight_cohesion));    
    acceleration.add(PVector.mult(separation,weight_separation));    
  }
  
  void move(Boid nearest){
    acceleration.add(PVector.mult(myMovement(), weight_madness));
    velocity.add(acceleration);
    /*get dist btwn nearest and self*/
    velocity.limit(maxSpeed);
    
    PVector tentative_pos = PVector.add(position,velocity);
    float dist = nearest == null ? 99999 : nearest.position.dist(tentative_pos);
    //dist -= (bounding_rad*frame.scaling() + nearest.bounding_rad);
    /*Possible collision, try to avoid it*/
    if(dist < 2.f*bounding_rad*frame.scaling() && nearest != null){
      PVector diff = PVector.sub(tentative_pos, nearest.position);
      diff.normalize();
      position.add(diff);
    }
    position.add(velocity);
    frame.setPosition(new Vec(position.x, position.y, position.z));
    acceleration.mult(0);
  }
  
  // steering. If arrival==true, the boid slows to meet the target. Credit to
  // Craig Reynolds
  PVector steer(PVector target, boolean arrival) {
    PVector steer = new PVector(); // creates vector for steering
    if (!arrival) {
      steer.set(PVector.sub(target, position)); // steering vector points
      // towards target (switch target and pos for avoiding)
      steer.limit(maxSteerForce); 
      // maxSteerForce
    } 
    else {
      PVector targetOffset = PVector.sub(target, position);
      float distance = targetOffset.mag();
      float rampedSpeed = maxSpeed * (distance / 100);
      float clippedSpeed = min(rampedSpeed, maxSpeed);
      PVector desiredVelocity = PVector.mult(targetOffset,
      (clippedSpeed / distance));
      steer.set(PVector.sub(desiredVelocity, velocity));
    }
    return steer;
  }

  PVector getSeparation(ArrayList boids) {
    PVector posSum = new PVector(0, 0, 0);
    PVector repulse;
    float total_dist = 0;
    for (int i = 0; i < boids.size(); i++) {
      Boid b = (Boid) boids.get(i);
      if(b == this) continue;
      float d = PVector.dist(position, b.position);
      if (d > 0 && d <= radius) {
        repulse = PVector.sub(position, b.position);
        repulse.normalize();
        repulse.mult(d);
        posSum.add(repulse);
        total_dist += d;
      }
    }
    if(total_dist > 0){
      posSum.div((float) total_dist);
      posSum.limit(maxSteerForce);
    }
    return posSum;
  }
  
  PVector getAlignment(ArrayList boids) {
    PVector velSum = new PVector(0, 0, 0);
    int count = 0;
    for (int i = 0; i < boids.size(); i++) {
      Boid b = (Boid) boids.get(i);
      if(b == this) continue;      
      float d = PVector.dist(position, b.position);
      if (d > 0 && d <= radius) {
        velSum.add(b.velocity);
        count++;
      }
    }
    if (count > 0) {
      velSum.div((float) count);
      velSum.limit(maxSteerForce);
    }
    return velSum;
  }

  PVector getCohesion(ArrayList boids) {
    PVector posSum = new PVector(0, 0, 0);
    PVector steer = new PVector(0, 0, 0);
    int count = 0;
    for (int i = 0; i < boids.size(); i++) {
      Boid b = (Boid) boids.get(i);
      if(b == this) continue;      
      float d = dist(position.x, position.y, b.position.x, b.position.y);
      if (d > 0 && d <= radius) {
        posSum.add(b.position);
        count++;
      }
    }
    if (count > 0) {
      posSum.div((float) count);
      steer = PVector.sub(posSum, position);
      steer.limit(maxSteerForce);      
    }
    return steer;
  }
  
  //follow a perlin noise movement
  PVector myMovement(){
    float x = noise(x_seed);
    x = map(x,0,1,-1,1);
    x_seed += 0.01;
    float y = noise(y_seed);
    y_seed += 0.01;
    y = map(y,0,1,-1,1);
    float z = noise(z_seed);
    z_seed += 0.01;
    z = map(z,0,1,-1,1);    
    return new PVector(x,y,z);
  }
  
  public void updateCoords(){
    min_coords = position.get();
    max_coords = position.get();    
    for (int i = 0; i < boids.size(); i++) {
      Boid b = (Boid) boids.get(i);      
      if(b.position.x < min_coords.x) min_coords.x = b.position.x; 
      if(b.position.y < min_coords.y) min_coords.y = b.position.y; 
      if(b.position.z < min_coords.z) min_coords.z = b.position.z;       
      if(b.position.x > max_coords.x) max_coords.x = b.position.x; 
      if(b.position.y > max_coords.y) max_coords.y = b.position.y; 
      if(b.position.z > max_coords.z) max_coords.z = b.position.z;       

    }  
  }
  
  // check if this boid's frame is the avatar
  boolean isAvatar() {
    return scene.avatar() == null ? false : scene.avatar().equals(frame) ? true : false;
  }
  
  
  void render() {
    /*Basically is doing a rotation in 2 steps:
     1 - align to X axis rotating the frame according to axis Y and the angle btwn X & Z
     2 - align to Y axis rotating the frame according to axis Z and the angle btwn Vec & Y     
    */
    quat = Quat.multiply(new Quat( new Vec(0,1,0),  atan2(-velocity.z, velocity.x)), 
                      new Quat( new Vec(0,0,1),  asin(velocity.y / velocity.mag())) );    
    frame.setRotation(quat);

    // Multiply matrix to get in the frame coordinate system.
    pushMatrix();  
    frame.applyTransformation();
    // setAvatar according to scene.motionAgent().inputGrabber()
    if (frame.grabsInput()){       
      if (!isAvatar())
        scene.setAvatar(frame);
    }
    shape(s);    
    popMatrix();
  }
  
}
