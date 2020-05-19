/*
 * Skinning.
 * by Sebastian Chaparro Cuevas.
 * 
 * This example shows how to use ik module in order to interact with an .obj model. 
 * First we import a Skeleton and enable IK functionallity. 
 * Then we relate the Skeleton with an .obj model using linear blend skinning algorithm (via Skinning class).  
 * 
 * Press 's' to enable/disable skeleton visualization.
 * Press 'c' to change between GPU/CPU mode.
 * Press 'i' to show/hide info. 
 * Press 'a' to visualize which joints influence the most a region of the mesh. 
 * Press 'd' to disable Paint mode. 
 */


//this packages are required for ik behavior
import nub.core.*;
import nub.primitives.*;
import nub.processing.*;
import nub.ik.animation.*;
import nub.ik.skinning.*;


boolean showSkeleton = true;
boolean showInfo = true;
boolean constraintSkeleton = true;
Scene scene;
Node reference;

Skeleton skeleton;
Skinning skinning;

String shapePath = "Kangaroo/Kangaroo.obj";
String texturePath = "Kangaroo/Kangaroo_diff.jpg";
String skeletonPath = "Kangaroo/Kangaroo.json";

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
    //2. Load the Skeleton
    skeleton = new Skeleton(scene, skeletonPath);
    //3. Enable IK and add targets at the leaf nodes
    skeleton.enableIK();
    skeleton.addTargets();
    //Set the radius of the targets (optional)
    skeleton.setTargetRadius(0.03f * scene.radius());
    //4 Define constraints according to the model
    //5. Relate the shape with a skinning method (CPU or GPU)
    resetSkinning(gpu);
    //use this method to visualize which node influences the most on a region of the mesh.
    if(skinning instanceof GPULinearBlendSkinning)
        ((GPULinearBlendSkinning) skinning).paintAllJoints();
}

void draw(){
    background(0);
    lights();
    scene.drawAxes();
    //Render mesh with respect to the node
    skinning.render(scene, skeleton.reference());
    if(showSkeleton){
      scene.render();
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
      String msg = activeRegion == 0 ? "Painting all Joints" : activeRegion == -1 ? "" : "Painting Joint : " + activeRegion;
      text(msg,width/2, height - 50);
    }
    scene.endHUD();
}

void resetSkinning(boolean gpu){
  //move sekeleton to rest position (in this case is when nodes are all aligned)
  for(Node node : skeleton.joints().values()){
    node.setRotation(new Quaternion());
  }
  
  if(gpu){
    skinning = new GPULinearBlendSkinning(skeleton, shapePath, texturePath, scene.radius(), false);    
  } else{
    skinning = new CPULinearBlendSkinning(skeleton, shapePath, texturePath, scene.radius(), false);    
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
