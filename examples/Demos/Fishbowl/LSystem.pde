//L-Systems
public class Animation{
  ArrayList<PShape> p = new ArrayList<PShape>();
  int cur = 0;
  int cont = 0;
  int delay = 300;
  void next(){
    if(cont == delay){
      if(cur < p.size() -1) cur++;
      else cur = 0;
      cont = 0;
    }
    cont++;
  }
  void draw(){
    shape(p.get(cur));
    next();
  }
}

public class Position{
  float[] pos;
  float[] orientation; 
  float[] dir;
  float size;  
  int[] col;
  public Position(float[] poss, float[] or, float[] dirr, float sz, int[] c){
    pos = new float[3];
    pos[0] = poss[0];
    pos[1] = poss[1];
    pos[2] = poss[2];
    dir = new float[3];
    dir[0] = dirr[0];
    dir[1] = dirr[1];
    dir[2] = dirr[2];    
    orientation = new float[3];
    orientation[0] = or[0];
    orientation[1] = or[1];
    orientation[2] = or[2];
    size = sz;    
    col = c;
  }
}

public class rule{
  char id;
  String resul; 
  float p = 1.0;
  
  public rule(char iid, String res, float prob){
    id = iid;
    resul = res;
    p = prob;            
  }
  
  public rule(char iid, String res){
    id = iid;
    resul = res;
    p = 1.0;    
  }
}

public class RuleSystem{
  float angle = 0;
  float angle_range = 5;
  int iterations = 10;
  float size = 10;
  float size_range = 2;
  public String axiom = "";
  public ArrayList<rule> rules = new ArrayList<rule>();
  
  public RuleSystem(float ang, float ang_range, 
                    String a, int iters, float s, float sr){
    rules = new ArrayList<rule>();
    angle = ang;
    angle_range = ang_range;
    axiom = a;
    iterations = iters;
    size = s;
    size_range = sr;
  } 
  
  void addRule(rule r){
    rules.add(r);
  }

  void applyRules(){
    //traverse each character
    for(int k = 0; k < iterations; k++){
      String new_axiom = "";

      for(int i = 0; i < axiom.length(); i++){
        char cur = axiom.charAt(i);
        new_axiom = new_axiom + applyRule(cur);    
      }
      axiom = new_axiom;
    }
  }
  
  String applyRule(char c){
    ArrayList<rule> possibles = new ArrayList<rule>(); 
    float rand_f = random(1);
    for(rule r : rules){
      if(r.id == c){
        possibles.add(r);
      }
    }
    for(rule pos : possibles){
      float value = pos.p;
      rand_f -= value;
      if(rand_f <= 0 ) return pos.resul;
    }    
    return "" + c;
  }

  PShape getShape(){
    PShape tree = createShape(GROUP);
    ArrayList<Position> stack_pos = new ArrayList<Position>(); 
    float sz = this.size;
    float[] orientation = {0, 0, 0};
    float[] dir = {0, -1, 0};    
    float var  = random(1)*size_range;
    float cur_size = random(1) > 0.5 ? sz + var : sz - var; 
    float[] pos = {0,0,0};
    int[] col; int[] col_1 = {245,137,5}; int[] col_2 = {242,250,20};
    col = col_1;
    for(int i = 0; i < axiom.length(); i++){
      char c = axiom.charAt(i);
      if(c == 'F'){
        PShape line;
        var  = random(1)*size_range;
        cur_size = random(1) > 0.5 ? sz + var : sz - var; 
        float[] n_pos = {dir[0], dir[1], dir[2]};
        //rot with x
        n_pos = rotX(n_pos, orientation[0]);
        //rot with y
        n_pos = rotY(n_pos, orientation[1]);
        //rot with z
        n_pos = rotZ(n_pos, orientation[2]);        
        
        dir[0] = n_pos[0];
        dir[1] = n_pos[1];
        dir[2] = n_pos[2];
        
        n_pos[0] = pos[0] + dir[0]*cur_size;
        n_pos[1] = pos[1] + dir[1]*cur_size;
        n_pos[2] = pos[2] + dir[2]*cur_size;
        
        line = createShape();
        line.beginShape(LINES);
        line.vertex(pos[0],pos[1],pos[2]);
        line.vertex(n_pos[0],n_pos[1],n_pos[2]);
        line.endShape();        
        line.setFill(color(col[0],col[1],col[2]));        
        line.setStroke(color(col[0],col[1],col[2]));                
        pos[0] = n_pos[0];
        pos[1] = n_pos[1];
        pos[2] = n_pos[2];
        orientation = new float[3];        
        tree.addChild(line);
      }
      if(c == 'S'){
        sz--;
      }
      if(c == '+'){
        var  = random(1)*angle_range;
        orientation[0] += angle;  
        orientation[0] = random(1) > 0.5 ? orientation[0] + var : orientation[0] - var; 
      }
      if(c == '-'){
        var  = random(1)*angle_range;
        orientation[0] += 360 - angle;  
        orientation[0] = random(1) > 0.5 ? orientation[0] + var : orientation[0] - var; 
      }
      if(c == '*'){
        var  = random(1)*angle_range;
        orientation[1] += angle;  
        orientation[1] = random(1) > 0.5 ? orientation[1] + var : orientation[1] - var; 
      }
      if(c == '/'){
        var  = random(1)*angle_range;
        orientation[1] += 360 - angle;  
        orientation[1] = random(1) > 0.5 ? orientation[1] + var : orientation[1] - var; 
      }
      if(c == '&'){
        var  = random(1)*angle_range;
        orientation[2] += angle;  
        orientation[2] = random(1) > 0.5 ? orientation[2] + var : orientation[2] - var; 
      }
      if(c == '|'){
        var  = random(1)*angle_range;
        orientation[2] += 360 - angle;  
        orientation[2] = random(1) > 0.5 ? orientation[2] + var : orientation[2] - var; 
      }
     
      if(c == '['){
        col = col == col_1 ? col_2 : col_1;
        stack_pos.add(new Position(pos,orientation,dir,sz,col));
      }
      if(c == ']'){
        Position p = stack_pos.remove(stack_pos.size()-1);
        pos = p.pos;
        orientation = p.orientation;
        dir = p.dir;
        sz = p.size;
        col = p.col;
      }
    }
    return tree;
  }
}
//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
float[] rotX(float[] x, float ang){
  float[] n_x = new float[3];
  n_x[0] = 1*x[0];
  n_x[1] = x[1] * cos(radians(ang)) - sin(radians(ang))*x[2];
  n_x[2] = x[1] * sin(radians(ang)) + cos(radians(ang))*x[2];
  return n_x;
}

float[] rotY(float[] x, float ang){
  float[] n_x = new float[3];
  n_x[0] = x[0] * cos(radians(ang)) + sin(radians(ang))*x[2];
  n_x[1] = x[1];
  n_x[2] = -1*x[0] * sin(radians(ang)) + cos(radians(ang))*x[2];
  return n_x;
}

float[] rotZ(float[] x, float ang){
  float[] n_x = new float[3];
  n_x[0] = x[0] * cos(radians(ang)) - sin(radians(ang))*x[1];
  n_x[1] = x[0] * sin(radians(ang)) + cos(radians(ang))*x[1];
  n_x[2] = x[2];
  return n_x;
}