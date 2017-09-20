/*
Here the idea is to optimize the fitness function considering as variables: the Size, the shape and the color.
We're gonna take the same happiness function of the Fish class as the fitness function, this will lead to homogenize the population.
Performance is slow due to the high number of executions of TuringMorph algorithm 
*/

public void  executeGA(){
  //1. Evaluate fitness for each agent in th population
  float fitness[] = new float[population.size()];
  Fish[] new_population = new Fish[population.size()];
  
  for(int k = 0; k < 1; k++){
    for(int i = 0; i < agents.size(); i++){
      agents.get(i).run();
    }
    float max_f = -9999, total_f = 0;
    int i = 0;
    for(Fish f : population){
      fitness[i] = f.fitness();
      max_f = fitness[i] > max_f ? fitness[i] : max_f;
      total_f += fitness[i];
      i++;
    }
    //normalize 
    for(i = 0; i < fitness.length; i++){
      fitness[i] = fitness[i]*1./(total_f);
    }
    agents.clear();
    for(i = 0; i < population.size(); i++){   

      //select 2 agents 
      int i1 = roulette(fitness);
      int i2 = roulette(fitness);      

      Fish parent_a = population.get(i1);
      Fish parent_b = population.get(i2);
      //do crossover
      Fish child = crossover(parent_a ,parent_b);
      //do mutation
      child = mutation(child);
      child.boid.boids.clear();
      child.boid.boids.add(child.boid);
      new_population[i] = child;
      agents.add(child);
      child.updateVision();
    }
    population.clear();
    for(Fish p : new_population) population.add(p);
  }
  updateGA();
}


public int roulette(float[] fitness){
  float r = random(1);
  int i = 0;
  while(r > 0){
    if(r <= fitness[i]) return i;
    r -= fitness[i];
    i++;
  }
  return -1;
}


public Fish crossover(Fish f1, Fish f2){
  Fish f = new Fish(id++);
  f.c1 = f1.c1; f.c2 = f1.c2;
  f.Da = f1.Da; f.Db = f1.Db; f.pa = f1.pa; f.pb = f1.pb;
  f.l = f2.l; 
  f.w = f2.w; 
  f.h = f2.h;
  f.base_model = f2.base_model;
  f.boid = f2.boid;
  return f;
}

public Fish mutation(Fish fi){
  float p = 0.05;
  Fish f = new Fish(id++);
  f.c1 = fi.c1;
  f.c2 = fi.c2;  
  f.Da = fi.Da; f.Db = fi.Db; f.pa = fi.pa; f.pb = fi.pb;
  f.l = fi.l; f.w = fi.w; f.h = fi.h;
  f.base_model = fi.base_model;
  f.boid = fi.boid; 
  int r_aux, g_aux, b_aux;
  if(random(1) <= p){
    r_aux = (int) (randomGaussian()*20 + red(fi.c1));
    g_aux = (int) (randomGaussian()*20 + green(fi.c1));
    b_aux = (int) (randomGaussian()*20 + blue(fi.c1));
    r_aux = (int) (random(100)) % 2 == 0 ? r_aux : (int)red(fi.c1); 
    g_aux = (int) (random(100)) % 2 == 0 ? g_aux : (int)green(fi.c1); 
    b_aux = (int) (random(100)) % 2 == 0 ? b_aux : (int)blue(fi.c1); 
    f.c1 = color(r_aux, g_aux, b_aux); 
  }
  if(random(1) <= p){
    r_aux = (int) (randomGaussian()*20 + red(fi.c1));
    g_aux = (int) (randomGaussian()*20 + green(fi.c1));
    b_aux = (int) (randomGaussian()*20 + blue(fi.c1));
    r_aux = (int) random(100) % 2 == 0 ? r_aux : (int)red(fi.c1); 
    g_aux = (int) random(100) % 2 == 0 ? g_aux : (int)green(fi.c1); 
    b_aux = (int) random(100) % 2 == 0 ? b_aux : (int)blue(fi.c1); 
    f.c2 = color(r_aux, g_aux, b_aux); 
  }
  if(random(1) <= p) f.Da = random(0.7, 2);//Diffusion of activator
  if(random(1) <= p) f.Db = f.Da*random(2,10);//Diffusion of inhibitor
  if(random(1) <= p) f.pa = random(11,14);//decay rate of a
  if(random(1) <= p) f.pb = f.pa*random(1.5,4);//decay rate of b
  
  return f;
}

void updateGA(){
  //actualize turing morph and parameters to render the agents
  shoal.clear();
  for(Fish f : agents){    
    last_texture = execute(true, true, f);        
    f.boid.s = updateShape(f);
    float bounding_rad = f.h > f.l ? f.h : f.l;
    bounding_rad = f.w > bounding_rad ? f.w : bounding_rad;    
    bounding_rad = bounding_rad*1.f/2.f;    
    f.boid = generateBoid((int)random(0,r_world.x() - 100), (int)random(0,r_world.y() - 100), (int)random(0,r_world.z() - 100),f.boid.s, f);
    f.boid.boids = new ArrayList<Boid>();
    f.boid.boids.add(f.boid);
    shoal.add(f.boid);
    f.updateVision();
  }
}
