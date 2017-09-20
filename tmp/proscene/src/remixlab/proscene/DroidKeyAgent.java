/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Victor Manuel Forero, Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import remixlab.bias.Agent;
import remixlab.bias.Profile;
import remixlab.bias.event.KeyboardEvent;
import remixlab.bias.event.KeyboardShortcut;

/**
 * Proscene Android key-agent. A Processing fully fledged Android-key
 * {@link Agent}.
 */
public class DroidKeyAgent extends Agent {
  protected Scene scene;
  protected KeyboardEvent currentEvent;

  public DroidKeyAgent(Scene scn) {
    super(scn.inputHandler());
    Profile.registerVKeys(KeyboardShortcut.class, android.view.KeyEvent.class);
    scene = scn;
    addGrabber(scene);
  }

  /**
   * Processing keyEvent method to be registered at the PApplet's instance.
   */
  public void keyEvent(processing.event.KeyEvent e) {
    if (e.getAction() == processing.event.KeyEvent.PRESS) {
      if (e.getKeyCode() == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
        Object context = scene.pApplet().getSurface();
        InputMethodManager imm = (InputMethodManager) ((Context) context).getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, 0);
      } else {
        currentEvent = new KeyboardEvent(e.getKey());
        updateTrackedGrabber(currentEvent);
        handle(currentEvent);
      }
    }
  }

  // TODO pending
  public static int keyCode(char key) {
    return key;
  }
}
