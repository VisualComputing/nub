/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.dandelion.core;

/**
 * Interface for objects that are to be tracked by a proscene Eye.
 * <p>
 * <h3>How does it work ?</h3>
 * <p>
 * All objects that are to be tracked by the
 * {@link remixlab.dandelion.core.AbstractScene#eye()} (known as avatars) should implement
 * this interface. To setup an avatar you should call
 * {@link remixlab.dandelion.core.AbstractScene#setAvatar(Trackable)}. The avatar will
 * then be tracked by the {@link remixlab.dandelion.core.AbstractScene#eye()}.
 */

public interface Trackable {
  /**
   * Returns the eye frame that will track the object. This frame (position and
   * orientation) will represent the {@link remixlab.dandelion.core.Eye#frame()} once
   * {@link remixlab.dandelion.core.AbstractScene#setAvatar(Trackable)} is called.
   *
   * @return Frame representing the Eye Frame.
   */
  public GenericFrame trackingEyeFrame();
}
