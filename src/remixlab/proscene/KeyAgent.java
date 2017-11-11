/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import remixlab.bias.Agent;
import remixlab.bias.Event;
import remixlab.bias.Grabber;
import remixlab.bias.event.KeyEvent;

/**
 * Proscene key-agent. A Processing fully fledged key {@link Agent}.
 *
 * @see Agent
 * @see remixlab.proscene.MouseAgent
 */
public class KeyAgent extends Agent {
  public static int LEFT_KEY = 37, RIGHT_KEY = 39, UP_KEY = 38, DOWN_KEY = 40;
  // public static int LEFT_KEY = PApplet.LEFT, RIGHT_KEY = PApplet.RIGHT,
  // UP_KEY = PApplet.UP, DOWN_KEY = PApplet.DOWN;
  protected Scene scene;
  protected boolean press, release, type;
  protected KeyEvent currentEvent;

  /**
   * Calls super on (scn,n) and sets default keyboard shortcuts.
   */
  public KeyAgent(Scene scn) {
    super(scn.inputHandler());
    scene = scn;
  }

  /**
   * Returns the graph this object belongs to.
   */
  public Scene scene() {
    return scene;
  }

  protected boolean bypass;

  /**
   * Processing keyEvent method to be registered at the PApplet's instance.
   * <p>
   * Current implementation requires grabber objects to implement
   * {@link Grabber#track(Event)} on a
   * {@code KeyEvent} as follows:
   * <p>
   * <pre>
   * {@code
   * public boolean track(KeyEvent event) {
   *   return profile.hasBinding(event.shortcut());
   * }
   * }
   * </pre>
   * <p>
   * in this way an agent grabber will grab inputGrabber as long as it defines a binding for a
   * given triggered key shortcut. The default grabber will just have the highest
   * precedence among all agent grabbers, provided that more than one grabber defines a
   * binding for the same key shortcut.
   */
  public void keyEvent(processing.event.KeyEvent e) {
    // According to Processing key event flow .e.g.,
    // RIGHT_ARROW
    // pressed: mod: vkey: 39 description: VK_RIGHT
    // released: mod: vkey: 39 description: VK_RIGHT
    // pressed: mod: vkey: 27 description: VK_ESCAPE
    // '1'
    // pressed: mod: vkey: 49 description: VK_1
    // typed: char: 1
    // released: mod: vkey: 49 description: VK_1
    // pressed: mod: vkey: 27 description: VK_ESCAPE
    // CTRL + '1'
    // pressed: mod: CTRL vkey: 17 description: VK_CONTROL
    // pressed: mod: CTRL vkey: 49 description: VK_1
    // typed: char: 1
    // released: mod: CTRL vkey: 49 description: VK_1
    // released: mod: CTRL vkey: 17 description: VK_CONTROL
    // pressed: mod: vkey: 27 description: VK_ESCAPE
    // we need to bypass TYPE events when a press event generates an action on the trackedGrabber
    // grabber
    press = e.getAction() == processing.event.KeyEvent.PRESS;
    release = e.getAction() == processing.event.KeyEvent.RELEASE;
    type = e.getAction() == processing.event.KeyEvent.TYPE;
    currentEvent = type ?
        (new KeyEvent(e.getKey())).fire() :
        press ?
            (new KeyEvent(e.getModifiers(), e.getKeyCode())).fire() :
            (new KeyEvent(e.getModifiers(), e.getKeyCode())).flush();
    if (press)
      bypass = update(currentEvent);
    if (type && !bypass)
      update(currentEvent);
    /*
    if (press) {
      bypass = poll(currentEvent) != null;
      if (bypass)
        handle(currentEvent);
      else if(defaultGrabber() != null)
        handle(currentEvent);
    }
    if (type && !bypass) {
      bypass = poll(currentEvent) != null;
      if (bypass)
        handle(currentEvent);
      else if(defaultGrabber() != null)
        handle(currentEvent);
    }
    */
    if (release)
      resetTrackedGrabber();
    // debug
    // System.out.println(press ? "pressed: " + printEvent(currentEvent) :
    // type ? "typed: " + printTypedEvent(currentEvent) :
    // release ? "released: " + printEvent(currentEvent) : "ooops! ");
  }

  //TODO experimental
  protected boolean update(Event event) {
    if(poll(event) != null)
      return handle(event);
    //if(defaultGrabber() != null)
    //return handle(event);
    if(defaultGrabber() != null)
      handle(event);
    return false;
  }

  // debug

  // protected String printEvent(KeyEvent event) {
  // return " mod: " + KeyEvent.modifiersText(event.modifiers()) + " vkey: " +
  // event.id() + " description: " + KeyShortcut.description(event.id());
  // }
  //
  // protected String printTypedEvent(KeyEvent event) {
  // return " char: " + event.key();
  // }

  /**
   * Same as {@code return java.awt.event.KeyEvent.getExtendedKeyCodeForChar(key)}.
   */
  public static int keyCode(char key) {
    return java.awt.event.KeyEvent.getExtendedKeyCodeForChar(key);
  }
}
