/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.dandelion.geom;

import remixlab.util.Copyable;

/**
 * Interface for objects represented from array-data.
 */
public interface Linkable extends Copyable {
  /**
   * Share the src array data.
   */
  public void link(float[] src);

  /**
   * Stop sharing data with the source
   */
  public void unLink();

  @Override
  public Linkable get();

  /**
   * Gets data as an array
   */
  public float[] get(float[] target);

  /**
   * Links array data representation with the source
   */
  public void set(Linkable source);

  /**
   * Sets representation from the array data
   */
  public void set(float[] source);

  /**
   * Sets data to default value.
   */
  public void reset();
}
