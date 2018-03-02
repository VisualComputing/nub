package ik.interactiveSkeleton;

import frames.input.Event;
import frames.input.Shortcut;
import frames.input.event.MotionEvent;
import frames.processing.Scene;
import ik.common.Joint;
import processing.core.PApplet;

/**
 * Created by sebchaparr on 24/02/18.
 */
public class InteractiveJoint extends Joint {
  Shortcut ctrlRight = new Shortcut(Event.CTRL, PApplet.RIGHT);
  InteractiveJoint child;

  public InteractiveJoint(Scene scene, boolean root) {
    super(scene, root);
  }

  public InteractiveJoint(Scene scene, boolean root, int color) {
    super(scene, root, color);
  }

  @Override
  public void interact(MotionEvent event) {
    super.interact(event);
    //Add mode
    if (event.shortcut().matches(ctrlRight)) {
      if (event.fired()) {
        //create a new InteractiveJoint
        child = new InteractiveJoint(graph(), false);
        child.setReference(this);
      } else {
        child.translate(event);
      }
    }
  }
}
