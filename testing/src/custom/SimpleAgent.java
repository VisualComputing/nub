package custom;

import remixlab.bias.Agent;
import remixlab.bias.BogusEvent;
import remixlab.bias.Shortcut;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/24/16.
 */
public class SimpleAgent extends Agent {
  public static final int SIMPLE_ID = Shortcut.registerID("SIMPLE");
  //public static final int SIMPLE_ID = Shortcut.registerID(5, "SIMPLE");

  protected BogusEvent currentEvent;
  protected boolean move, press, drag, release;
  Scene scene;

  public SimpleAgent(Scene scn) {
    super(scn.inputHandler());
    scene = scn;
    register();
    addGrabber(scene.eyeFrame());
    setDefaultGrabber(scene.eyeFrame());
  }

  public void register() {
    scene.inputHandler().registerAgent(this);
    scene.pApplet().registerMethod("mouseEvent", this);
  }

  public void unregister() {
    scene.inputHandler().unregisterAgent(this);
    scene.pApplet().unregisterMethod("mouseEvent", this);
  }

  public void mouseEvent(processing.event.MouseEvent e) {
    move = e.getAction() == processing.event.MouseEvent.MOVE;
    press = e.getAction() == processing.event.MouseEvent.PRESS;
    drag = e.getAction() == processing.event.MouseEvent.DRAG;
    release = e.getAction() == processing.event.MouseEvent.RELEASE;
    if (move || press || drag || release) {
      currentEvent = new BogusEvent(BogusEvent.NO_MODIFIER_MASK, SIMPLE_ID);
      handle(currentEvent);
      return;
    }
  }
}
