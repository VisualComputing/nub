/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a raster or ray-tracing renderer. Released under the terms of the GNU
 * Public License v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.timing;

/**
 * Interface used to define a timer callback method.
 */
public interface Taskable {
  /**
   * Timer callback method
   */
  public void execute();
}
