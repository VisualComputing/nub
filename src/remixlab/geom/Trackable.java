/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.geom;

/**
 * Interface for objects that are to be trackedGrabber by a proscene Eye.
 * <p>
 * <h3>How does it work ?</h3>
 * <p>
 * All objects that are to be trackedGrabber by the
 * {@link AbstractScene#eye()} (known as avatars) should implement
 * this interface. To setup an avatar you should call
 * {@link AbstractScene#setAvatar(Trackable)}. The avatar will
 * then be trackedGrabber by the {@link AbstractScene#eye()}.
 */

public interface Trackable {
  /**
   * Returns the eye frame that will track the object. This frame (position and
   * orientation) will represent the {@link AbstractScene#eye()} once
   * {@link AbstractScene#setAvatar(Trackable)} is called.
   *
   * @return Frame representing the Eye Frame.
   */
  public InteractiveFrame trackingEyeFrame();
}
