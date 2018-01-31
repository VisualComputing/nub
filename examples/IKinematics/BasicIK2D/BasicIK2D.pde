import proscene.input.*;
import proscene.input.event.*;
import proscene.dandelion.constraint.*;
import proscene.dandelion.geom.*;
import proscene.dandelion.primitives.*;
import proscene.dandelion.ik.*;
import proscene.timing.*;
import proscene.proscene.*;
import proscene.util.*;
import proscene.dandelion.ik.Solver.*;
import java.util.Map;

/**
 * Author: Sebastian Chaparro
 * A simple Chain of GenericFrames that is continuosly updated
 * according to a given target. 
 * The green Chain has no constraints whereas the purple one 
 * Has a Hinge constraint.
 * 
 * Press 'z' to enable/disable automatic following of Target 
 * Press 'v' to move the Target
 * If automatic following of Target is disabled you could perform a
 * single iteration of FABRIK algorithm Pressing 'c' to solve for 
 * unconstrained chain (Green) or 'd' to solve for constrained one (Purple)
 * 
 * Press 'j' and 'k' to show the configuration of the constrained chain
 * after Foward and Backward Step respectively.
 *
 * Press 'n' to increment in one the number of times the FABRIK algorithm is 
 * executed per frame
 * 
 * Renderer could take either P2D or P3D as values
 **/

Scene scene;
PFont myFont;
ArrayList<GenericFrame>  joints = new ArrayList<GenericFrame>();
ArrayList<GenericFrame>  jointsConstrained = new ArrayList<GenericFrame>();
String renderer = P2D;

int num_joints = 10;
float constraint_factor = 50; 
float lenght = 10;
float max = 0;
float min = 10;

ChainSolver solverUnconstrained;
ChainSolver solverConstrained;
boolean auto = true;


public void setup() {
  size(640, 360, renderer);
  scene = new Scene(this);
  scene.setAxesVisualHint(true);
  Vec v = new Vec(lenght,lenght);
  if(renderer == P3D) v = new Vec(lenght,lenght,lenght); 
  InteractiveFrame prev = null;
  //Unconstrained Chain
  for(int i = 0; i < num_joints; i++){
    InteractiveFrame j;
    j = new InteractiveFrame(scene);
    if(prev != null){   j.setReferenceFrame(prev);
    j.setTranslation(v.get());}
    joints.add(j);
    prev = j;
  }
  //Fix hierarchy
  joints.get(0).setupHierarchy();  
  prev = null;
  //Constrained Chain
  for(int i = 0; i < num_joints; i++){
    InteractiveFrame j;
    j = new InteractiveFrame(scene);
    if(prev != null){   j.setReferenceFrame(prev);
    j.setTranslation(v.get());}
    jointsConstrained.add(j);
    prev = j;
  }
  //Fix hierarchy
  jointsConstrained.get(0).setupHierarchy();  
  //Add constraints
    
  for(int i = 0; i < jointsConstrained.size(); i++){
    Hinge hinge = new Hinge();
    hinge.setRestRotation(jointsConstrained.get(i).rotation());
    hinge.setMax(radians(max));
    hinge.setMin(radians(min));
    hinge.setAxis(jointsConstrained.get(i).transformOf(new Vec(1,-1,0)));
    jointsConstrained.get(i).setConstraint(hinge);
  }  
  
  
  target = new InteractiveFrame(scene);
  target.translate(new Vec(50, 50*noise(0)));

  solverConstrained = new ChainSolver(jointsConstrained, target);
  solverConstrained.setTIMESPERFRAME(1);
  solverUnconstrained = new ChainSolver(joints, target);
  solverUnconstrained.setTIMESPERFRAME(1);
}


public void draw() {
  background(0);
  for(GenericFrame j : joints){
    pushMatrix();
      pushStyle();
      j.applyWorldTransformation();
      scene.drawAxes(3);  
      fill(0,255,0,50);
      strokeWeight(5);
      stroke(0,100,100,100);
      if(j.referenceFrame() != null){
        Vec v = j.coordinatesOfFrom(new Vec(), j.referenceFrame());
        line(0,0,0, v.x(), v.y(), v.z());
      }
      popStyle();
    popMatrix();
  }
  for(GenericFrame j : jointsConstrained){
    pushMatrix();
      pushStyle();
      j.applyWorldTransformation();
      scene.drawAxes(3);  
      fill(0,255,0,50);
      strokeWeight(5);
      stroke(100,0,100,100);
      if(j.referenceFrame() != null){
        Vec v = j.coordinatesOfFrom(new Vec(), j.referenceFrame());
        line(0,0,0, v.x(), v.y(), v.z());
      }
      popStyle();
    popMatrix();
  }

  pushMatrix();
    pushStyle();
    noStroke();
    fill(255,0,0,200);    
    if(renderer == P3D){
      translate(target.position().x(),target.position().y(),target.position().z());
      sphere(5); 
    }
    else{
      translate(target.position().x(),target.position().y());
      ellipse(0,0,5,5);
    }
    popStyle();
  popMatrix();
  if(auto){
    solverConstrained.solve();
    solverUnconstrained.solve();
  }
  
  if(forward != null)drawChain(forward, color(0,255,0,30));   
  if(backward != null)drawChain(backward, color(0,0,255,30));   
}

InteractiveFrame target;
float counter = 0;
boolean enableBack = false;
Vec initial = null;
ChainSolver solver = null;
HashMap<Integer, Vec> forward = null;
HashMap<Integer, Vec> backward = null;

boolean inv = false;
public void keyPressed(){
  if(key == 'v'){
    counter+=3;
    float val = inv ? -1 : 1; 
    target.translate(3*val, 3*noise(counter)); 
    if(target.position().x() > 130) inv = true;   
    if(target.position().x() < -130) inv = false; 
  }
  
  if(key == 'c'){
    //create solver
    ChainSolver solver = new ChainSolver(joints, target);
    solver.setTIMESPERFRAME(1);
    solver.solve();
  }  

  if(key == 'd'){
    //create solver
    ChainSolver solver = new ChainSolver(jointsConstrained, target);
    solver.setTIMESPERFRAME(1);
    solver.solve();
  }  
  
  if(key == 'j'){
    backward = null;
    enableBack = false;
    //create solver
    solver = new ChainSolver(jointsConstrained, target);
    solver.setTIMESPERFRAME(1);
    float length = solver.getLength();
    GenericFrame root = jointsConstrained.get(0);
    GenericFrame end   = jointsConstrained.get(jointsConstrained.size()-1);
    Vec target = solver.getTarget().position().get();
    //Get the distance between the Root and the Target
    float dist = Vec.distance(root.position(), target);
    initial = solver.getPositions().get(root.id()).get();
    if(Vec.distance(end.position(), target) <= solver.getERROR()) return;
    enableBack = true;
    solver.getPositions().put(end.id(), target.get());
    //Stage 1: Forward Reaching
    forward = new HashMap<Integer, Vec>();
    solver.executeForwardReaching(solver.getChain());
    for(Map.Entry<Integer, Vec> entry : solver.getPositions().entrySet()){
      forward.put(entry.getKey(), entry.getValue());
    }
  }
  if(key == 'k'){
    if(!enableBack) return;
      solver.getPositions().put(jointsConstrained.get(0).id(), initial);
      solver.executeBackwardReaching(solver.getChain());
      backward = solver.getPositions();
      solver.update();
      enableBack = false;
  }
  if(key == 'z'){
    auto = !auto;
  }
}

/*DEBUG METHODS*/
public void drawChain(HashMap<Integer, Vec> positions, int c){
  PShape p;
  if(renderer == P3D) p = createShape(SPHERE,5); 
  else p = createShape(ELLIPSE,0,0,5,5);
  p.setStroke(false);
  int tr = 30; 
  for(Vec v : positions.values()){
    p.setFill(color(red(c),green(c),blue(c), tr));
    pushMatrix();
    if(renderer == P3D){
      translate(v.x(),v.y(),v.z());
      shape(p);
    }else{
      translate(v.x(),v.y());
      shape(p);    
    }
    popMatrix();
    tr +=20;
  }
}