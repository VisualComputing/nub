//Used for Fish textures
//Adapted from http://www.cgjennings.ca/toybox/turingmorph/TuringMorph.java
double Da = random(3);//Diffusion of activator
double Db = Da + Da/10 + Da*random(2,6)*randomGaussian();//Diffusion of inhibitor
double pa;//decay rate of a
double pb;//decay rate of b

int iterations = 700;
int counter = 0;
int dim = 32;
double[][] a;
double[][] b;
double[][] a_next;
double[][] b_next;
color inhibitor;
color activator;
PImage last_texture = null;
double h = -1;
double l = 99999999;
ArrayList<float[]> params;
ArrayList<PImage> preload_textures;

void setupTuring(boolean predefined, Fish agent){
  setupTuring(predefined, false, agent);
}

void setupTuring(boolean predefined, boolean defined, Fish agent){
  a = new double[dim][dim];
  b = new double[dim][dim];
  a_next = new double[dim][dim];
  b_next = new double[dim][dim];
  if(!predefined){
    Da = random(0.7, 2);//Diffusion of activator
    Db = Da*random(2,10);//Diffusion of inhibitor
    pa = random(11,14);//decay rate of a
    pb = pa*random(1.5,4);//decay rate of b
  }else if(!defined){
    float[] par = params.get((int)random(params.size()));
    Da = par[0];//Diffusion of activator
    Db = par[1];//Diffusion of inhibitor
    pa = par[2];//decay rate of a
    pb = par[3];//decay rate of b  
  }else{
    Da = agent.Da;//Diffusion of activator
    Db = agent.Db;//Diffusion of inhibitor
    pa = agent.pa;//decay rate of a
    pb = agent.pb;//decay rate of b    
  }
  counter = 0;
  if(!defined) getColors();
  for(int i = 0; i < dim; i++){
    for(int j = 0; j < dim; j++){
      a[i][j] = random((float) pa);  
      b[i][j] = random((float) pa);  
    }
  }  
  //REQUIRED FOR AGENT HAPPINESS FUNCTION
  max_Da = max_Da > Da ? max_Da : (float)Da;
  max_Db = max_Db > Db ? max_Db : (float)Db;
  max_pa = max_pa > pa ? max_pa : (float)pa;
  max_pb = max_pb > pb ? max_pb : (float)pb;
  agent.Da = (float)Da; agent.Db = (float)Db; agent.pa = (float)pa; agent.pb = (float)pb;
  if(defined){
      inhibitor = agent.c1; 
      activator = agent.c2;
  } 
  agent.c1 = inhibitor; agent.c2 = activator;
  //_________________________________________
  //_________________________________________  
}
PImage execute(boolean predefined, Fish agent){
  return execute(predefined, false, agent);
}

PImage execute(boolean predefined, boolean defined, Fish agent){
  setupTuring(predefined, defined, agent);
  calculateTuringMorph(agent);
  PImage img = createImage(200, 200, RGB);
  for(int y = 0; y < 200; y++){
    for(int x = 0; x < 200; x++){
      img.pixels[y*img.width + x] = linearInterpolation(a[x%dim][y%dim]);       
    }
  }
  return img;
}

void next_value(){
  //consider the grid as a toroid
  for(int i = 0; i < dim; i++){
    for(int j = 0; j < dim; j++){
      double diff_a = 0;
      double diff_b = 0;
      //for each cell calculates da/dt and db/dt
      int prev_i = i == 0 ? dim -1 : i-1;  
      int prev_j = j == 0 ? dim -1 : j-1;  
      int next_i = i == dim-1 ? 0 : i+1;  
      int next_j = j == dim-1 ? 0 : j+1;  
      //using central diff: f''(x) = f(x-1) - 2*f(x) + f(x+1) 
      double diff_2_a = a[prev_i][j] - 2*a[i][j] + a[next_i][j];
      diff_2_a += a[i][prev_j] - 2*a[i][j] + a[i][next_j];
      //diff_a = s*((a[i][j]*a[i][j]/b[i][j]) + ba) - ra*a[i][j] + Da*diff_2_a;
      diff_a = 0.01*(a[i][j]*b[i][j] - a[i][j] - pa + Da*diff_2_a);
      //using central diff: f''(x) = f(x-1) - 2*f(x) + f(x+1) 
      double diff_2_b = b[prev_i][j] - 2*b[i][j] + b[next_i][j];
      diff_2_b += b[i][prev_j] - 2*b[i][j] + b[i][next_j];
      diff_b = 0.01*(pb - a[i][j]*b[i][j] + Db*diff_2_b);
      //diff_b = s*(a[i][j]*a[i][j]) - rb*b[i][j] + Db*diff_2_b + bb;
      a_next[i][j] = a[i][j] + diff_a;
      if( a_next[i][j] < 0.0 ) a_next[i][j] = 0.0;
      b_next[i][j] = b[i][j] + diff_b;
      if( b_next[i][j] < 0.0 ) b_next[i][j] = 0.0;
      if(a_next[i][j] > h) h = a_next[i][j];
      if(a_next[i][j] < l) l = a_next[i][j];
    }
  }
}


void calculateTuringMorph(Fish agent){
  while(counter < iterations){
    next_value();
    double[][] tmp = a;
    a = a_next;
    a_next = a;
    tmp = b;
    b = b_next;
    b_next = tmp;
    counter++;  
  }
  h = -1;
  l = 99999999;  
  for(int i = 0; i < dim; i++){
    for(int j = 0; j < dim; j++){
      if(a[i][j] > h) h = a[i][j];
      if(a[i][j] < l) l = a[i][j];
    }
    if(h - l < 0.00001) execute(true, agent);
  }
}

void getColors(){
  float hue = (float) Math.random();
  float sat = (float) Math.random();
  float bri = (float) Math.random() * 0.667f;
  color min = hsvToRgb( hue, sat, bri );
  hue += (float) Math.random() * 26f / 180f;
  bri = 0.75f + (float) Math.random() / 4f;
  color max = hsvToRgb( hue, sat, bri );
  if( Math.random() >= 0.5f ) {
    color temp = min;
    min = max;
    max = temp;
  }
  inhibitor = min;
  activator = max;
}

color linearInterpolation(double number){
  number = (number - l)*255/(h-l);
  float min = min(red(inhibitor),red(activator));
  float max = max(red(inhibitor),red(activator));
  float r = min + (max - min)*(float)number/255;
  min = min(green(inhibitor),green(activator));
  max = max(green(inhibitor),green(activator));
  float g = min + (max - min)*(float)number/255;
  min = min(blue(inhibitor),blue(activator));
  max = max(blue(inhibitor),blue(activator));
  float b = min + (max - min)*(float)number/255;
  return color(r,g,b);
}

void createGoodParams(){
  params = new ArrayList<float[]>();
  params.add(new float[]{1.6, 4, 12, 16});
  params.add(new float[]{3.6, 15, 12, 16});
  params.add(new float[]{0.1, 1, 12, 16});
  params.add(new float[]{1, 16, 12, 16});
  params.add(new float[]{2.6, 23, 12, 16});
  params.add(new float[]{ 1.820237159729004,9.098380387170184,11.055387496948242,16.044132232666016});
  params.add(new float[]{ 1.8654859066009521,14.061677772123831,10.92138385772705,17.55817222595215});
  params.add(new float[]{ 0.9323149919509888,12.368269768272825,13.61745834350586,13.073046684265137});
  params.add(new float[]{ 1.3909082412719727,7.96618654015729,14.373779296875,18.37541389465332});
  params.add(new float[]{ 1.244422435760498,9.399146686284993,10.530546188354492,15.1903715133667});
  params.add(new float[]{ 1.361748456954956,5.027707863078449,13.963714599609375,17.08762550354004});
  params.add(new float[]{ 1.9893717765808105,13.073239295018324,12.605398178100586,16.535234451293945});
  params.add(new float[]{ 1.8679652214050293,8.905863212868104,15.289697647094727,20.11795997619629});
  params.add(new float[]{ 1.5169005393981934,5.098050264888362,10.089836120605469,13.600872993469238});
  params.add(new float[]{ 1.637159824371338,8.162986638302456,14.342633247375488,18.266571044921875});
  params.add(new float[]{ 1.1772737503051758,4.309704938711293,13.604137420654297,16.81740379333496});
  params.add(new float[]{ 0.7081674337387085,2.9685837451448593,12.415901184082031,14.767086029052734});
  params.add(new float[]{ 0.9372822046279907,3.7605891400694427,13.344186782836914,16.273183822631836});
  params.add(new float[]{ 0.8895675539970398,3.3944612490281285,14.199894905090332,16.13526725769043});
  params.add(new float[]{ 0.7187371253967285,4.013583867948098,11.979429244995117,16.686275482177734});
  params.add(new float[]{ 0.7163916826248169,3.6655857995617076,11.297784805297852,15.82742691040039});
  params.add(new float[]{ 1.7773635387420654,7.115153845893504,13.795303344726562,17.489063262939453});
  params.add(new float[]{ 1.0512175559997559,3.7808575917248435,14.153043746948242,17.8505802154541});
  params.add(new float[]{ 1.2851462364196777,7.382025844085799,12.14144229888916,13.839949607849121});
  params.add(new float[]{ 1.1260885000228882,5.896098090877233,14.791630744934082,17.139739990234375});
  params.add(new float[]{ 0.7481123805046082,2.2946476573752945,12.221925735473633,13.747495651245117});
  params.add(new float[]{ 1.0741355419158936,3.8345732604093077,13.646682739257812,16.73682975769043});
  params.add(new float[]{ 0.8985697031021118,5.10840581428246,12.641302108764648,15.830381393432617});
  params.add(new float[]{ 1.2111268043518066,4.901229714252973,12.490299224853516,16.04593276977539});
  params.add(new float[]{ 0.9941200017929077,5.971927546770205,11.392909049987793,16.23088836669922});
  params.add(new float[]{ 0.7871498465538025,3.419959644089152,12.577191352844238,15.262157440185547});
  params.add(new float[]{ 1.2260243892669678,3.8457495838218874,14.100003242492676,18.007099151611328});
  params.add(new float[]{ 1.7345709800720215,7.834726193572421,8.940418243408203,12.321981430053711});
  params.add(new float[]{ 1.9681928157806396,13.561329549676705,13.433323860168457,17.268707275390625});
  params.add(new float[]{ 1.6321532726287842,8.240926866989783,13.620535850524902,16.94477653503418});
  params.add(new float[]{ 1.3061139583587646,5.154204651091707,11.874044418334961,15.225461959838867});
  params.add(new float[]{ 1.305968999862671,6.92637209290721,12.902256965637207,16.351919174194336}); 
  params.add(new float[]{ 0.7727353572845459,3.913105440208599,13.07991886138916,14.485102653503418});
  params.add(new float[]{ 0.8312311172485352,4.940607816561422,12.045702934265137,13.002368927001953});
  params.add(new float[]{ 1.4434738159179688,4.340747731514052,10.82282829284668,13.77204704284668}); 
  params.add(new float[]{ 1.7301685810089111,9.719948506313726,14.439325332641602,16.945358276367188});
  params.add(new float[]{ 0.7469828128814697,5.632955413780408,14.957828521728516,14.892322540283203});
  params.add(new float[]{ 0.9217255115509033,3.6945492001050875,12.32547378540039,15.82451057434082}); 
  params.add(new float[]{ 0.7095315456390381,1.9066461102106573,14.762285232543945,18.276065826416016});
  params.add(new float[]{ 1.6311092376708984,4.999516232513476,12.453896522521973,16.065265655517578}); 
  params.add(new float[]{ 1.340782642364502,3.8484096040598796,13.066950798034668,16.785425186157227}); 
  params.add(new float[]{ 0.9871760606765747,6.628287014798836,11.386174201965332,13.518062591552734}); 
}

void preloadTextures(){
  String path = "textures/";
  preload_textures = new ArrayList<PImage>();
  int num = 0;
  while(num < 25){
    preload_textures.add(loadImage(path + (int)(num+1) +".png"));
    num++;
  }
}

void preloadMorphology(){
  preloadTextures();
  createGoodParams();
}

PImage applyTuringMorph(boolean predefined, boolean load, Fish agent){
    if(!load) return execute(predefined, agent);
    return preload_textures.get((int) random(preload_textures.size()));
}