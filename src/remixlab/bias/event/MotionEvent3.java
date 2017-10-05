/**************************************************************************************
 * bias_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.bias.event;

/**
 * A {@link remixlab.bias.event.MotionEvent} with three degrees-of-freedom ( {@link #x()},
 * {@link #y()} and {@link #z()} ).
 */
public class MotionEvent3 extends MotionEvent {
  protected float x, dx;
  protected float y, dy;
  protected float z, dz;

  /**
   * Construct an absolute event from the given dof's and modifiers.
   *
   * @param dx
   * @param dy
   * @param dz
   * @param modifiers
   * @param id
   */
  public MotionEvent3(float dx, float dy, float dz, int modifiers, int id) {
    super(modifiers, id);
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent3 ? (MotionEvent3) prevEvent : null, x, y, z, modifiers, id)}.
   *
   * @see #MotionEvent3(MotionEvent3, float, float, float, int, int)
   */
  public MotionEvent3(MotionEvent prevEvent, float x, float y, float z, int modifiers, int id) {
    this(prevEvent instanceof MotionEvent3 ? (MotionEvent3) prevEvent : null, x, y, z, modifiers, id);
  }

  /**
   * Construct a relative event from the given previous event, dof's and modifiers.
   * <p>
   * If the {@link #id()} of the {@code prevEvent} is different then {@link #id()}, sets
   * the {@link #distance()}, {@link #delay()} and {@link #speed()} all to {@code zero}.
   *
   * @param prevEvent
   * @param x
   * @param y
   * @param z
   * @param modifiers
   * @param id
   */
  public MotionEvent3(MotionEvent3 prevEvent, float x, float y, float z, int modifiers, int id) {
    super(modifiers, id);
    this.x = x;
    this.y = y;
    this.z = z;
    setPrevious(prevEvent);
  }

  /**
   * Construct an absolute event from the given dof's.
   *
   * @param dx
   * @param dy
   * @param dz
   */
  public MotionEvent3(float dx, float dy, float dz) {
    super();
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent3 ? (MotionEvent3) prevEvent : null, x, y, z)}.
   *
   * @see #MotionEvent3(MotionEvent3, float, float, float)
   */
  public MotionEvent3(MotionEvent prevEvent, float x, float y, float z) {
    this(prevEvent instanceof MotionEvent3 ? (MotionEvent3) prevEvent : null, x, y, z);
  }

  /**
   * Construct a relative event from the given previous event, dof's and modifiers.
   * <p>
   * If the {@link #id()} of the {@code prevEvent} is different then {@link #id()}, sets
   * the {@link #distance()}, {@link #delay()} and {@link #speed()} all to {@code zero}.
   *
   * @param prevEvent
   * @param x
   * @param y
   * @param z
   */
  public MotionEvent3(MotionEvent3 prevEvent, float x, float y, float z) {
    super();
    this.x = x;
    this.y = y;
    this.z = z;
    setPrevious(prevEvent);
  }

  protected MotionEvent3(MotionEvent3 other) {
    super(other);
    this.x = other.x;
    this.dx = other.dx;
    this.y = other.y;
    this.dy = other.dy;
    this.z = other.z;
    this.dz = other.z;
  }

  @Override
  public MotionEvent3 get() {
    return new MotionEvent3(this);
  }

  @Override
  public MotionEvent3 flush() {
    return (MotionEvent3) super.flush();
  }

  @Override
  public MotionEvent3 fire() {
    return (MotionEvent3) super.fire();
  }

  @Override
  protected void setPrevious(MotionEvent prevEvent) {
    rel = true;
    if (prevEvent != null)
      if (prevEvent instanceof MotionEvent3 && prevEvent.id() == this.id()) {
        this.dx = this.x() - ((MotionEvent3) prevEvent).x();
        this.dy = this.y() - ((MotionEvent3) prevEvent).y();
        this.dz = this.z() - ((MotionEvent3) prevEvent).z();
        distance = MotionEvent
            .distance(x, y, z, ((MotionEvent3) prevEvent).x(), ((MotionEvent3) prevEvent).y(), ((MotionEvent3) prevEvent).z());
        delay = this.timestamp() - prevEvent.timestamp();
        if (delay == 0)
          speed = distance;
        else
          speed = distance / (float) delay;
      }
  }

  /**
   * @return dof-1, only meaningful if the event {@link #isRelative()}
   */
  public float x() {
    return x;
  }

  /**
   * @return dof-1 delta
   */
  public float dx() {
    return dx;
  }

  /**
   * @return previous dof-1, only meaningful if the event {@link #isRelative()}
   */
  public float prevX() {
    return x() - dx();
  }

  /**
   * @return dof-2, only meaningful if the event {@link #isRelative()}
   */
  public float y() {
    return y;
  }

  /**
   * @return dof-2 delta
   */
  public float dy() {
    return dy;
  }

  /**
   * @return previous dof-2, only meaningful if the event {@link #isRelative()}
   */
  public float prevY() {
    return y() - dy();
  }

  /**
   * @return dof-3, only meaningful if the event {@link #isRelative()}
   */
  public float z() {
    return z;
  }

  /**
   * @return dof-3 delta
   */
  public float dz() {
    return dz;
  }

  /**
   * @return previous dof-3, only meaningful if the event {@link #isRelative()}
   */
  public float prevZ() {
    return z() - dz();
  }

  @Override
  public boolean isNull() {
    if (dx()==0 && dy()==0 && dz()==0)
      return true;
    return false;
  }

  /**
   * Reduces the event to a {@link MotionEvent2} (lossy reduction). Keeps
   * dof-1 and dof-2 and discards dof-3.
   */
  public MotionEvent2 event2() {
    MotionEvent2 pe2;
    MotionEvent2 e2;
    if (isRelative()) {
      pe2 = new MotionEvent2(null, prevX(), prevY(), modifiers(), id());
      e2 = new MotionEvent2(pe2, x(), y(), modifiers(), id());
    } else {
      e2 = new MotionEvent2(dx(), dy(), modifiers(), id());
    }
    e2.delay = this.delay();
    e2.speed = this.speed();
    e2.distance = this.distance();
    if (fired())
      return e2.fire();
    else if (flushed())
      return e2.flush();
    return e2;
  }
}
