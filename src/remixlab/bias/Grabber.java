/**************************************************************************************
 * bias_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.bias;

/**
 * Grabbers are means to attach a user-space object to all the
 * {@link Agent}s (see
 * {@link Agent#addGrabber(Grabber)}) through which it's going to be
 * handled. For details, refer to the {@link Agent} documentation.
 */
public interface Grabber {
  /**
   * Defines the rules to set the grabber as an agent input-grabber.
   *
   * @see Agent#updateTrackedGrabber(BogusEvent)
   * @see Agent#inputGrabber()
   */
  boolean checkIfGrabsInput(BogusEvent event);

  /**
   * Defines how the grabber should react according to the given bogus-event.
   *
   * @see Agent#handle(BogusEvent)
   */
  void performInteraction(BogusEvent event);
}
