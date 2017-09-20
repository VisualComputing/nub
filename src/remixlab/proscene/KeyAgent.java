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
import remixlab.bias.BogusEvent;
import remixlab.bias.Grabber;
import remixlab.bias.Profile;
import remixlab.bias.event.KeyboardEvent;
import remixlab.bias.event.KeyboardShortcut;

import java.awt.event.KeyEvent;

/**
 * Proscene key-agent. A Processing fully fledged key {@link Agent}.
 *
 * @see Agent
 * @see remixlab.proscene.MouseAgent
 * @see remixlab.proscene.DroidKeyAgent
 * @see remixlab.proscene.DroidTouchAgent
 */
public class KeyAgent extends Agent {
  public static final int LEFT_KEY = 37, RIGHT_KEY = 39, UP_KEY = 38, DOWN_KEY = 40;
  // public static int LEFT_KEY = PApplet.LEFT, RIGHT_KEY = PApplet.RIGHT,
  // UP_KEY = PApplet.UP, DOWN_KEY = PApplet.DOWN;
  protected Scene scene;
  protected boolean press, release, type;
  protected KeyboardEvent currentEvent;

  /**
   * Calls super on (scn,n) and sets default keyboard shortcuts.
   */
  public KeyAgent(Scene scn) {
    super(scn.inputHandler());
    Profile.registerVKeys(KeyboardShortcut.class, KeyEvent.class);
    scene = scn;
    addGrabber(scene);
  }

  /**
   * Returns the scene this object belongs to.
   */
  public Scene scene() {
    return scene;
  }

  protected boolean bypass;

  /**
   * Processing keyEvent method to be registered at the PApplet's instance.
   * <p>
   * Current implementation requires grabber objects to have a
   * {@link Profile} and to implement
   * {@link Grabber#checkIfGrabsInput(BogusEvent)} on a
   * {@code KeyboardEvent} as follows:
   * <p>
   * <pre>
   * {@code
   * public boolean checkIfGrabsInput(KeyboardEvent event) {
   *   return profile.hasBinding(event.shortcut());
   * }
   * }
   * </pre>
   * <p>
   * in this way an agent grabber will grab input as long as it defines a binding for a
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
    // we need to bypass TYPE events when a press event generates an action on the tracked
    // grabber
    press = e.getAction() == processing.event.KeyEvent.PRESS;
    release = e.getAction() == processing.event.KeyEvent.RELEASE;
    type = e.getAction() == processing.event.KeyEvent.TYPE;
    currentEvent = type ?
        (new KeyboardEvent(e.getKey())).fire() :
        press ?
            (new KeyboardEvent(e.getModifiers(), e.getKeyCode())).fire() :
            (new KeyboardEvent(e.getModifiers(), e.getKeyCode())).flush();
    if (press) {
      bypass = updateTrackedGrabber(currentEvent) != null;
      if (bypass)
        handle(currentEvent);
    }
    if (type && !bypass) {
      // if(updateTrackedGrabber(currentEvent) != null)
      // handle(currentEvent);
      bypass = updateTrackedGrabber(currentEvent) != null;
      if (bypass)
        handle(currentEvent);
    }
    if (release)
      resetTrackedGrabber();
    // debug
    // System.out.println(press ? "pressed: " + printEvent(currentEvent) :
    // type ? "typed: " + printTypedEvent(currentEvent) :
    // release ? "released: " + printEvent(currentEvent) : "ooops! ");
  }

  // debug

  // protected String printEvent(KeyboardEvent event) {
  // return " mod: " + KeyboardEvent.modifiersText(event.modifiers()) + " vkey: " +
  // event.id() + " description: " + KeyboardShortcut.description(event.id());
  // }
  //
  // protected String printTypedEvent(KeyboardEvent event) {
  // return " char: " + event.key();
  // }

  /**
   * Same as {@code return java.awt.event.KeyEvent.getExtendedKeyCodeForChar(key)}.
   */
  public static int keyCode(char key) {
    return java.awt.event.KeyEvent.getExtendedKeyCodeForChar(key);
  }
}
