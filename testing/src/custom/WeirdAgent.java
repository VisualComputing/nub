package custom;

import remixlab.bias.Agent;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/24/16.
 */
public class WeirdAgent extends Agent {
  public static final int WEIRD_ID = 101;//TODO experimental

  protected WeirdEvent currentEvent;
  protected boolean move, press, drag, release;
  Scene scene;

  public WeirdAgent(Scene scn) {
    super(scn.inputHandler());
    scene = scn;
    register();
    //TODO restore
    //addGrabber(scene.eye());
    //setDefaultGrabber(scene.eye());
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
    currentEvent = new WeirdEvent(e.getX() - scene.pApplet().width / 2, e.getY() - scene.pApplet().height / 2,
        e.getModifiers(), /*e.getButton()*/ WEIRD_ID);
    if (press || drag || release)
      handle(currentEvent);
    if (move)
      poll(currentEvent);
  }
}
