/*
 * Eagle.
 * by Sebastian Chaparro Cuevas.
 * 
 * This example shows how to use ik module in order to interact with an .obj model using the Node class. 
 * First we define a Skeleton and relate it with a .obj (via Skinning class)  
 * Then we register a solver on the Scene and add a Target that the Wings must follow. 
 * We use an Interpolator per Wing in order to define a motion.
 * 
 * Press 's' to enable/disable skeleton visualization.
 * Press 'c' to change between GPU/CPU mode.
 * Press 'i' to show/hide info. 
 * Press 'a' to visualize which joints influence the most a region of the mesh. 
 * Press 'd' to disable Paint mode. 
 */

import nub.core.*;
import nub.core.constraint.*;
import nub.primitives.*;
import nub.processing.*;
import nub.ik.solver.*;
import nub.ik.skinning.*;
import nub.ik.visual.Joint; //Joint provides default way to visualize the skeleton
import java.util.List;

boolean showSkeleton = true;
boolean showInfo = true;
boolean constraintSkeleton = true;
Scene scene;
Node reference;

List<Node> skeleton;
Skinning skinning;

Interpolator[] targetInterpolator = new Interpolator[2];

String shapePath = "EAGLE_2.OBJ";
String texturePath = "EAGLE2.jpg";

//Flag used to visualize which joint influences the most a region of the mesh
int activeRegion = 0;  
boolean gpu = true;

void settings() {
    size(700, 700, P3D);
}

void setup() {
    //1. Create and set the scene
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRightHanded();
    scene.fit(1);
    //2. Define the structure
    //2.1 Define a reference node to the skeleton and the mesh
    reference = new Node();
    reference.enableTagging(false); //disable interaction
    //2.2 Use SimpleBuilder example (or a Modelling Sw if desired) and locate each Joint accordingly to mesh
    //2.3 Create the Joints based on 2.2.
    skeleton = loadSkeleton(reference);
    //2.4 define constraints (if any)
    //Find axis of rotation via cross product
    if(constraintSkeleton) setConstraints();
    //3. Relate the shape with a skinning method (CPU or GPU)
    resetSkinning(gpu);
    //4. Adding IK behavior
    //4.1 Identify root and end effector(s)
    Node root = skeleton.get(0); //root is the fist joint of the structure
    List<Node> endEffectors = new ArrayList<Node>(); //End Effectors are leaf nodes (with no children)
    for(Node node : skeleton) {
        if (node.children().size() == 0) {
            endEffectors.add(node);
        }
    }

    //4.2 relate a skeleton with an IK Solver
    Solver solver = scene.registerTreeSolver(root);
    //Update params
    solver.setMaxError(1f);
    solver.setMaxIterations(15);

    for(Node endEffector : endEffectors){
        //4.3 Create target(s) to relate with End Effector(s)
        PShape redBall = createShape(SPHERE, scene.radius() * 0.02f);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0, 220));
        Node target = new Node(redBall);
        target.setPickingThreshold(0);
        target.setReference(reference); //Target also depends on reference
        target.setPosition(endEffector.position().get());
        //4.4 Relate target(s) with end effector(s)
        scene.addIKTarget(endEffector, target);
        //disable enf effector tracking
        endEffector.enableTagging(false);
        //If desired generates a default Path that target must follow
        if(endEffector == skeleton.get(14)){
          targetInterpolator[0] = setupTargetInterpolator(target, new Vector[]{new Vector(-48,0,0), new Vector(-40,-13,0), new Vector(-32,0,0) , new Vector(-40,20,0), new Vector(-48,0,0)});
        }

        if(endEffector == skeleton.get(18)){
          targetInterpolator[1] = setupTargetInterpolator(target, new Vector[]{new Vector(44,0,0), new Vector(38,-16,0), new Vector(28.5,0,0) , new Vector(38,19,0), new Vector(44,0,0)});
        } 
    }
    //use this method to visualize which node influences the most on a region of the mesh.
    if(skinning instanceof GPULinearBlendSkinning)
        ((GPULinearBlendSkinning) skinning).paintAllJoints();
}

void draw(){
    background(0);
    lights();
    scene.drawAxes();
    //Render mesh with respect to the node
    skinning.render(scene, reference);
    if(showSkeleton){
      scene.render();
      pushStyle();
      fill(255);
      stroke(255);
      scene.drawCatmullRom(targetInterpolator[0],1);
      scene.drawCatmullRom(targetInterpolator[1],1);
      popStyle();
    }
    //Optionally print some info:
    scene.beginHUD();
    text("Mode: " + (gpu ? "GPU " : "CPU") + " Frame rate: " + nf(frameRate, 0, 1), width/2, 50);    
    if(showInfo){
      for(int i = 0; i < skinning.skeleton().size(); i++){
          if(skinning.skeleton().get(i).translation().magnitude() == 0){
              continue;
          }
          fill(255);
          Vector p = scene.screenLocation(skinning.skeleton().get(i).position());
          text("" + i, p.x(), p.y());
          Vector pos = skinning.skeleton().get(i).position();
      }

      for(Interpolator interpolator : targetInterpolator){
        for(Node node : interpolator.keyFrames().values()){
          pushStyle();
          Vector p = scene.screenLocation(node.position());
          text(round(node.translation().x()) + "," + round(node.translation().y()) + "," + round(node.translation().z()), p.x() - 5, p.y());
          popStyle();
        }
      }

      String msg = activeRegion == 0 ? "Painting all Joints" : activeRegion == -1 ? "" : "Painting Joint : " + activeRegion;
      text(msg,width/2, height - 50);
    }
    scene.endHUD();
    
}

void resetSkinning(boolean gpu){
  //move sekeleton to rest position (in this case is when nodes are all aligned)
  for(Node node : skeleton){
    node.setRotation(new Quaternion());
  }
  
  if(gpu){
    skinning = new GPULinearBlendSkinning(skeleton, this.g, shapePath, texturePath, scene.radius(), false);    
  } else{
    skinning = new CPULinearBlendSkinning(skeleton, this.g, shapePath, texturePath, scene.radius(), false);    
  }
}

void mouseMoved() {
    scene.mouseTag();
}

void mouseDragged() {
    if (mouseButton == LEFT){
        scene.mouseSpin();
    } else if (mouseButton == RIGHT) {
        scene.mouseTranslate();
    } else {
        scene.scale(mouseX - pmouseX);
    }
}

void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
        if (event.getButton() == LEFT)
            scene.focus();
        else
            scene.align();
}

void keyPressed() {
    if(key == 'S' || key == 's'){
        showSkeleton = !showSkeleton;
    }
    if(key == 'C' || key == 'c'){
        gpu = !gpu;
        resetSkinning(gpu);
    }

    if(key == 'A' || key == 'a') {
        activeRegion = (activeRegion + 1) % skinning.skeleton().size();
        if(skinning instanceof GPULinearBlendSkinning)
          ((GPULinearBlendSkinning) skinning).paintJoint(activeRegion);
    }

    if(key == 'd' || key == 'D'){
        if(skinning instanceof GPULinearBlendSkinning)
          ((GPULinearBlendSkinning) skinning).disablePaintMode();
    }
        
    if(key == 'i' || key == 'I'){
        showInfo = !showInfo;
    }
}

//Skeleton is generated by interacting with SimpleBuilder
List<Node> loadSkeleton(Node reference){
  JSONArray skeleton_data = loadJSONArray("skeleton.json");
  HashMap<String, Joint> dict = new HashMap<String, Joint>();
  List<Node> skeleton = new ArrayList<Node>();
  for(int i = 0; i < skeleton_data.size(); i++){
    JSONObject joint_data = skeleton_data.getJSONObject(i);
    Joint joint = new Joint(joint_data.getFloat("radius"));
    joint.setPickingThreshold(joint_data.getFloat("picking"));
    if(i == 0){
      joint.setRoot(true);
      joint.setReference(reference);
    }else{
      joint.setReference(dict.get(joint_data.getString("reference")));
    }
    joint.setTranslation(joint_data.getFloat("x"), joint_data.getFloat("y"), joint_data.getFloat("z"));
    joint.setRotation(joint_data.getFloat("q_x"), joint_data.getFloat("q_y"), joint_data.getFloat("q_z"), joint_data.getFloat("q_w"));
    skeleton.add(joint);
    dict.put(joint_data.getString("name"), joint);
  }  
  return skeleton;
}


void setConstraints(){
    Node j11 = skeleton.get(11);
    Vector up11 = j11.children().get(0).translation();//Same as child translation 
    Vector twist11 = Vector.cross(up11, new Vector(0,1,0), null);//Same as child translation 
    Hinge h11 = new Hinge(radians(40), radians(40), j11.rotation(), up11, twist11);
    j11.setConstraint(h11);
    
    
    Node j12 = skeleton.get(12);
    Vector up12 = j12.children().get(0).translation();//Same as child translation 
    Vector twist12 = Vector.cross(up12, new Vector(0,1,0), null);//Same as child translation 
    Hinge h12 = new Hinge(radians(40), radians(40), j12.rotation(), up12, twist12);
    j12.setConstraint(h12);
    
    Node j13 = skeleton.get(13);
    Vector up13 = j13.children().get(0).translation();//Same as child translation 
    Vector twist13 = Vector.cross(up13, new Vector(0,1,0), null);//Same as child translation 
    Hinge h13 = new Hinge(radians(45), radians(5), skeleton.get(13).rotation(), up13, twist13);
    j13.setConstraint(h13);

    
    Node j15 = skeleton.get(15);
    Vector up15 = j15.children().get(0).translation();//Same as child translation 
    Vector twist15 = Vector.cross(up15, new Vector(0,1,0), null);//Same as child translation 
    Hinge h15 = new Hinge(radians(40), radians(40), j15.rotation(), up15, twist15);
    j15.setConstraint(h15);
    
    
    Node j16 = skeleton.get(16);
    Vector up16 = j16.children().get(0).translation();//Same as child translation 
    Vector twist16 = Vector.cross(up16, new Vector(0,1,0), null);//Same as child translation 
    Hinge h16 = new Hinge(radians(40), radians(40), j16.rotation(), up16, twist16);
    j16.setConstraint(h16);
    
    Node j17 = skeleton.get(17);
    Vector up17 = j17.children().get(0).translation();//Same as child translation 
    Vector twist17 = Vector.cross(up17, new Vector(0,1,0), null);//Same as child translation 
    Hinge h17 = new Hinge(radians(45), radians(5), skeleton.get(17).rotation(), up17, twist17);
    j17.setConstraint(h17);
}

Interpolator setupTargetInterpolator(Node target, Vector[] positions) {
    Interpolator targetInterpolator = new Interpolator(target);
    targetInterpolator.enableRecurrence();
    targetInterpolator.setSpeed(1f);
    // Create a path
    for(int i = 0; i < positions.length; i++){
        Node iFrame = new Node();
        iFrame.setPickingThreshold(5);
        iFrame.setReference(target.reference());
        iFrame.setTranslation(positions[i]);
        targetInterpolator.addKeyFrame(iFrame);
    }
    //targetInterpolator.run();
    return targetInterpolator;
}
