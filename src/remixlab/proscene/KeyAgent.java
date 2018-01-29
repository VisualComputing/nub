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

import remixlab.core.Graph;
import remixlab.input.Agent;
import remixlab.input.Event;
import remixlab.input.Grabber;
import remixlab.input.event.KeyEvent;

/**
 * Proscene _key-agent. A Processing fully fledged _key {@link Agent}.
 *
 * @see Agent
 * @see remixlab.proscene.MouseAgent
 */
public class KeyAgent extends Agent {
  public static int LEFT_KEY = 37, RIGHT_KEY = 39, UP_KEY = 38, DOWN_KEY = 40;
  // public static int LEFT_KEY = PApplet.LEFT, RIGHT_KEY = PApplet.RIGHT,
  // UP_KEY = PApplet.UP, DOWN_KEY = PApplet.DOWN;
  protected Graph _graph;
  protected boolean _press, _release, _type;
  protected KeyEvent _currentEvent;
  protected boolean _bypass;

  /**
   * Calls super on (scn,n) and sets default keyboard shortcuts.
   */
  public KeyAgent(Graph graph) {
    super(graph.inputHandler());
    _graph = graph;
  }

  /**
   * Returns the graph this object belongs to.
   */
  public Graph graph() {
    return _graph;
  }

  /**
   * Processing keyEvent method to be registered at the PApplet's instance.
   * <p>
   * Current implementation requires _grabber objects to implement
   * {@link Grabber#track(Event)} on a
   * {@code KeyEvent} as follows:
   * <p>
   * <pre>
   * {@code
   * public boolean track(KeyEvent _event) {
   *   return profile.hasBinding(_event.shortcut());
   * }
   * }
   * </pre>
   * <p>
   * in this way an agent _grabber will grab inputGrabber as long as it defines a binding for a
   * given triggered _key shortcut. The default _grabber will just have the highest
   * precedence among all agent grabbers, provided that more than one _grabber defines a
   * binding for the same _key shortcut.
   */
  public void keyEvent(processing.event.KeyEvent keyEvent) {
    // According to Processing _key _event flow .e.g.,
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
    // we need to bypass TYPE events when a press _event generates an action on the _trackedGrabber
    // _grabber
    _press = keyEvent.getAction() == processing.event.KeyEvent.PRESS;
    _release = keyEvent.getAction() == processing.event.KeyEvent.RELEASE;
    _type = keyEvent.getAction() == processing.event.KeyEvent.TYPE;
    _currentEvent = _type ?
        (new KeyEvent(keyEvent.getKey())).fire() :
        _press ?
            (new KeyEvent(keyEvent.getModifiers(), keyEvent.getKeyCode())).fire() :
            (new KeyEvent(keyEvent.getModifiers(), keyEvent.getKeyCode())).flush();
    if (_press)
      _bypass = _update(_currentEvent);
    if (_type && !_bypass)
      _update(_currentEvent);
    /*
    if (press) {
      bypass = poll(currentEvent) != null;
      if (bypass)
        handle(currentEvent);
      else if(_defaultGrabber() != null)
        handle(currentEvent);
    }
    if (type && !bypass) {
      bypass = poll(currentEvent) != null;
      if (bypass)
        handle(currentEvent);
      else if(_defaultGrabber() != null)
        handle(currentEvent);
    }
    */
    if (_release)
      resetTrackedGrabber();
    // debug
    // System.out.println(press ? "pressed: " + printEvent(currentEvent) :
    // type ? "typed: " + printTypedEvent(currentEvent) :
    // release ? "released: " + printEvent(currentEvent) : "ooops! ");
  }

  //TODO experimental
  protected boolean _update(Event event) {
    if (poll(event) != null)
      return handle(event);
    //if(_defaultGrabber() != null)
    //return handle(_event);
    if (defaultGrabber() != null)
      handle(event);
    return false;
  }

  // debug

  // protected String printEvent(KeyEvent _event) {
  // return " mod: " + KeyEvent.modifiersText(_event._modifiers()) + " vkey: " +
  // _event._id() + " description: " + KeyShortcut.description(_event._id());
  // }
  //
  // protected String printTypedEvent(KeyEvent _event) {
  // return " char: " + _event._key();
  // }
}
