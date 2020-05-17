/**
 * Simple Animation Controls
 * by Sebastian Chaparro Cuevas.
 *
 * In this example a mesh is loaded (shapePath) along with a skeleton (jsonPath) and the idea is to define
 * key postures at different times (represented in the timeline at the bottom of the window) that will be 
 * interpolated to produce a smooth movement.
 *
 * To do so you could interact with the scene and the time line as follows:
 * Interact with the main scene and the skeleton as usual either manipulating the skeleton itself or the IK targets.
 * Click on a key frame to set it as the current key frame. Note that the current key frame is highlighted with a green border
 * and any action performed with the keyboard will be applied to it.
 * Press 'S' to save the posture of the skeleton on the current key frame.
 * Press 'E' to enable / disable the current key frame if it contains a posture. If a key frame is enabled (green fill) 
 * then it will be used to generate the animation otherwise it will be ignored (red fill).
 * Press 'D' to delete the posture saved in the current key frame.
 * Press 'R' to play the current animation.
 * Press 'T' to stop the animation.
 * Press 'I' to invert the speed direction (if is negative the animation will run backwards).
 * Press 'L' to make the animation loop.
 * Press any digit to change the sped of the animation.
 * Press the space bar to show / hide the skeleton.
 * Translate the key frames to change their time position.
 */


import nub.core.*;
import nub.core.constraint.*;
import nub.primitives.*;
import nub.processing.*;
import nub.ik.solver.*;
import nub.ik.skinning.*;
import nub.ik.animation.Joint; //Joint provides default way to visualize the skeleton
import java.util.List;

//this packages are required for ik behavior
import nub.ik.animation.*;
import nub.ik.visual.*;
import nub.ik.solver.*;

    
Scene mainScene, controlScene, focus;

boolean showSkeleton = true;
String jsonPath = "data/Hand_constrained.json";
String shapePath = "Rigged_Hand.obj";
String texturePath = "HAND_C.jpg";
AnimationPanel panel;
Skeleton skeleton;
Skinning skinning;

float speed = 1, direction = 1;




public void settings() {
  size(1200, 800, P3D);
}

public void setup() {
  //Kinematic scene
  mainScene = new Scene(this, P3D, width, height);
  mainScene.fit(0);
  mainScene.setRightHanded();
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
}


public void draw() {
  mainScene.beginDraw();
  mainScene.context().lights();
  mainScene.context().background(0);
  mainScene.drawAxes();
  skinning.render(mainScene);
  if(showSkeleton) mainScene.render(skeleton.reference());
  mainScene.endDraw();
  mainScene.display();
  controlScene.beginDraw();
  controlScene.context().background(150);
  controlScene.render(panel);
  controlScene.endDraw();
  controlScene.display();
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
            panel.play(direction * speed);
        }
        
        if(key == 't' || key == 'T'){
          panel.stop();
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

        if(key == 'i' || key == 'I'){
            direction  *= -1;
        }

        if(key == 'l' || key == 'L'){
            panel.enableRecurrence(!panel.isRecurrent());
        }

        if(Character.isDigit(key)){
            speed = Float.valueOf(""+key);
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
