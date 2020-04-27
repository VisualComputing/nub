import nub.core.*;
import nub.core.constraint.*;
import nub.primitives.*;
import nub.processing.*;
import nub.ik.solver.*;
import nub.ik.skinning.*;
import nub.ik.visual.Joint; //Joint provides default way to visualize the skeleton
import java.util.List;

//this packages are required for ik behavior
import nub.ik.animation.*;
import nub.ik.visual.*;
import nub.ik.solver.*;

    
Scene mainScene, controlScene, focus;
boolean showSkeleton = true;
String jsonPath = "data/rex.json";
String shapePath = "Rex.obj";
String texturePath = "T-Rex.jpg";
AnimationPanel panel;
Skeleton skeleton;
Skinning skinning;


public void settings() {
  size(1200, 800, P3D);
}

public void setup() {
  //Kinematic scene
  mainScene = new Scene(this);
  mainScene.fit(0);
  mainScene.setRightHanded();
  //skeleton = generateSkeleton(mainScene, 5);
  skeleton = new Skeleton(mainScene, jsonPath);
  skeleton.enableIK();
  skeleton.addTargets();
  skeleton.setTargetRadius(0.03f * mainScene.radius());
  //Relate the shape with a skinning method (CPU or GPU)
  skinning = new GPULinearBlendSkinning(skeleton, shapePath, texturePath, mainScene.radius());
  //Set the control scene
  controlScene = new Scene(this, P2D, width, (int)(height * 0.3), 0,(int)(height * 0.7f));
  controlScene.setRadius( height * 0.3f / 2.f);
  controlScene.fit();
  //Setting the panel
  panel = new AnimationPanel(controlScene, skeleton);
  //set eye constraint
  controlScene.eye().enableTagging(false);
  controlScene.eye().setConstraint(new Constraint() {
      @Override
      public Vector constrainTranslation(Vector translation, Node node) {
          return new Vector(translation.x(), 0); //no vertical translation allowed
      }

      @Override
      public Quaternion constrainRotation(Quaternion rotation, Node node) {
          return new Quaternion(); //no rotation is allowed
      }
  });
  //Lock nodes on a given graph
  mainScene.lock(skeleton.reference());
  controlScene.lock(panel);
}


public void draw() {
  lights();
  mainScene.context().background(0);
  mainScene.drawAxes();
  skinning.render(mainScene, skeleton.reference());
  skeleton.cull(!showSkeleton);
  mainScene.render();

  noLights();
  mainScene.beginHUD();
  controlScene.beginDraw();
  controlScene.context().background(150);
  controlScene.render();
  controlScene.endDraw();
  controlScene.display();
  mainScene.endHUD();

}

//Skeleton definition methods
public Skeleton generateSkeleton(Scene scene, int n){
  Skeleton skeleton = new Skeleton(scene);
  //1. create a basic skeleton composed of n Joints
  int idx = 0;
  skeleton.addJoint("J" + idx++);
  Vector t = new Vector(0, scene.radius() / n);
  for( ; idx < n; idx++){
      Joint joint = skeleton.addJoint("J" +idx, "J" + (idx - 1));
      joint.translate(t);
  }
  //2. create solvers
  skeleton.enableIK();
  //3. add targets
  skeleton.addTargets();
  return skeleton;
}


//Interaction methods

public void mouseMoved(){
  focus = mouseY > 0.7 * height ? controlScene : mainScene;
  focus.mouseTag();
}

public void mouseDragged(){
  if (mouseButton == LEFT) {
    focus.mouseSpin();
  } else if (mouseButton == RIGHT)
    focus.mouseTranslate(0);
  else
    focus.moveForward(mouseX - pmouseX);
}

public void mouseWheel(MouseEvent event) {
  if(focus != controlScene && focus.node() == null) focus.scale(event.getCount() * 50);
}

public void keyPressed(){
  if(key == 'r' || key == 'R'){
    panel.play();
  }

  if(key == 's' || key == 'S'){
    //save skeleton posture
    panel.savePosture();
  }

  if(key == 'e' || key == 'E'){
    panel.toggleCurrentKeyPoint();
  }

  if(key == 'd' || key == 'D'){
    panel.deletePostureAtKeyPoint();
  }

  if(key == 't' || key == 'T'){
    skeleton.restoreTargetsState();
  }
  
  if(key == ' '){
    showSkeleton = !showSkeleton;
  }
}

public void mouseClicked(MouseEvent event) {
  if(focus == mainScene) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
          focus.focus();
      else
          focus.align();
  } else if(focus == controlScene){
    if(focus.node() != null) focus.node().interact("onClicked", event.getButton());
  }
}
