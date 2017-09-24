/**************************************************************************************
 * bias_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 ********************************************************************************/

package remixlab.bias.event;

/**
 * A {@link remixlab.bias.event.MotionEvent} with six degrees-of-freedom ( {@link #x()},
 * {@link #y()}, {@link #z()} , {@link #rx()}, {@link #ry()} and {@link #rz()}).
 */
public class Event6 extends MotionEvent {
  protected float x, dx;
  protected float y, dy;
  protected float z, dz;

  protected float rx, drx;
  protected float ry, dry;
  protected float rz, drz;

  /**
   * Construct an absolute event from the given dof's and modifiers.
   *
   * @param dx
   * @param dy
   * @param dz
   * @param drx
   * @param dry
   * @param drz
   * @param modifiers
   * @param id
   */
  public Event6(float dx, float dy, float dz, float drx, float dry, float drz, int modifiers, int id) {
    super(modifiers, id);
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.drx = drx;
    this.dry = dry;
    this.drz = drz;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof Event6 ? (Event6) prevEvent : null, x, y, z, rx, ry, rz, modifiers, id)}.
   *
   * @see #Event6(Event6, float, float, float, float, float, float, int, int)
   */
  public Event6(MotionEvent prevEvent, float x, float y, float z, float rx, float ry, float rz, int modifiers,
                int id) {
    this(prevEvent instanceof Event6 ? (Event6) prevEvent : null, x, y, z, rx, ry, rz, modifiers, id);
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
   * @param rx
   * @param ry
   * @param rz
   * @param modifiers
   * @param id
   */
  public Event6(Event6 prevEvent, float x, float y, float z, float rx, float ry, float rz, int modifiers, int id) {
    super(modifiers, id);
    this.x = x;
    this.y = y;
    this.z = z;
    this.rx = rx;
    this.ry = ry;
    this.rz = rz;
    setPrevious(prevEvent);
  }

  /**
   * Construct an absolute event from the given dof's and modifiers.
   *
   * @param dx
   * @param dy
   * @param dz
   * @param drx
   * @param dry
   * @param drz
   */
  public Event6(float dx, float dy, float dz, float drx, float dry, float drz) {
    super();
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.drx = drx;
    this.dry = dry;
    this.drz = drz;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof Event6 ? (Event6) prevEvent : null, x, y, z, rx, ry, rz)}.
   *
   * @see #Event6(Event6, float, float, float, float, float, float)
   */
  public Event6(MotionEvent prevEvent, float x, float y, float z, float rx, float ry, float rz) {
    this(prevEvent instanceof Event6 ? (Event6) prevEvent : null, x, y, z, rx, ry, rz);
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
   * @param rx
   * @param ry
   * @param rz
   */
  public Event6(Event6 prevEvent, float x, float y, float z, float rx, float ry, float rz) {
    super();
    this.x = x;
    this.y = y;
    this.z = z;
    this.rx = rx;
    this.ry = ry;
    this.rz = rz;
    setPrevious(prevEvent);
  }

  protected Event6(Event6 other) {
    super(other);
    this.x = other.x;
    this.dx = other.dx;
    this.y = other.y;
    this.dy = other.dy;
    this.z = other.z;
    this.dz = other.z;
    this.rx = other.rx;
    this.drx = other.drx;
    this.ry = other.ry;
    this.dry = other.dry;
    this.rz = other.rz;
    this.drz = other.drz;
  }

  @Override
  public Event6 get() {
    return new Event6(this);
  }

  @Override
  public Event6 flush() {
    return (Event6) super.flush();
  }

  @Override
  public Event6 fire() {
    return (Event6) super.fire();
  }

  @Override
  protected void setPrevious(MotionEvent prevEvent) {
    rel = true;
    if (prevEvent != null)
      if (prevEvent instanceof Event6 && prevEvent.id() == this.id()) {
        this.dx = this.x() - ((Event6) prevEvent).x();
        this.dy = this.y() - ((Event6) prevEvent).y();
        this.dz = this.z() - ((Event6) prevEvent).z();
        this.drx = this.rx() - ((Event6) prevEvent).rx();
        this.dry = this.ry() - ((Event6) prevEvent).ry();
        this.drz = this.rz() - ((Event6) prevEvent).rz();
        distance = MotionEvent.distance(x, y, z, rx, ry, rz, ((Event6) prevEvent).x(), ((Event6) prevEvent).y(),
            ((Event6) prevEvent).z(), ((Event6) prevEvent).rx(), ((Event6) prevEvent).ry(),
            ((Event6) prevEvent).rz());
        delay = this.timestamp() - prevEvent.timestamp();
        if (delay == 0)
          speed = distance;
        else
          speed = distance / (float) delay;
      }
  }

  /**
   * @return dof1, only meaningful if the event {@link #isRelative()}
   */
  public float x() {
    return x;
  }

  /**
   * @return dof1 delta
   */
  public float dx() {
    return dx;
  }

  /**
   * @return previous dof1, only meaningful if the event {@link #isRelative()}
   */
  public float prevX() {
    return x() - dx();
  }

  /**
   * @return dof2, only meaningful if the event {@link #isRelative()}
   */
  public float y() {
    return y;
  }

  /**
   * @return dof2 delta
   */
  public float dy() {
    return dy;
  }

  /**
   * @return previous dof2, only meaningful if the event {@link #isRelative()}
   */
  public float prevY() {
    return y() - dy();
  }

  /**
   * @return dof3, only meaningful if the event {@link #isRelative()}
   */
  public float z() {
    return z;
  }

  /**
   * @return dof3 delta
   */
  public float dz() {
    return dz;
  }

  /**
   * @return previous dof3, only meaningful if the event {@link #isRelative()}
   */
  public float prevZ() {
    return z() - dz();
  }

  /**
   * Alias for {@link #rx()}, only meaningful if the event {@link #isRelative()}
   */
  public float roll() {
    return rx();
  }

  /**
   * @return dof4, only meaningful if the event {@link #isRelative()}
   */
  public float rx() {
    return rx;
  }

  /**
   * Alias for {@link #ry()}, only meaningful if the event {@link #isRelative()}
   */
  public float pitch() {
    return ry();
  }

  /**
   * @return dof5, only meaningful if the event {@link #isRelative()}
   */
  public float ry() {
    return ry;
  }

  /**
   * alias for {@link #rz()}, only meaningful if the event {@link #isRelative()}
   */
  public float yaw() {
    return rz();
  }

  /**
   * @return dof6, only meaningful if the event {@link #isRelative()}
   */
  public float rz() {
    return rz;
  }

  /**
   * @return dof4 delta
   */
  public float drx() {
    return drx;
  }

  /**
   * @return dof5 delta
   */
  public float dry() {
    return dry;
  }

  /**
   * @return dof6 delta
   */
  public float drz() {
    return drz;
  }

  /**
   * @return previous dof4, only meaningful if the event {@link #isRelative()}
   */
  public float prevRX() {
    return rx() - drx();
  }

  /**
   * @return previous dof5, only meaningful if the event {@link #isRelative()}
   */
  public float prevRY() {
    return ry() - dry();
  }

  /**
   * @return previous dof6, only meaningful if the event {@link #isRelative()}
   */
  public float prevRZ() {
    return rz() - drz();
  }

  @Override
  public boolean isNull() {
    if (dx()==0 && dy()==0 && dz()==0 && drx()==0 && dry()==0 && drz()==0)
      return true;
    return false;
  }

  /**
   * Convenience function that simply returns {@code return event3(true)}
   *
   * @see #event3(boolean)
   */
  public Event3 event3() {
    return event3(true);
  }

  /**
   * Reduces the event to a {@link Event3} (lossy reduction).
   *
   * @param fromTranslation if true keeps dof1, dof2 and dof3; otherwise keeps dof4, dof4 and dof6.
   */
  public Event3 event3(boolean fromTranslation) {
    Event3 pe3;
    Event3 e3;
    if (isRelative()) {
      if (fromTranslation) {
        pe3 = new Event3(null, prevX(), prevY(), prevZ(), modifiers(), id());
        e3 = new Event3(pe3, x(), y(), z(), modifiers(), id());
      } else {
        pe3 = new Event3(null, prevRX(), prevRY(), prevRZ(), modifiers(), id());
        e3 = new Event3(pe3, rx(), ry(), rz(), modifiers(), id());
      }
    } else {
      if (fromTranslation) {
        e3 = new Event3(dx(), dy(), dz(), modifiers(), id());
      } else {
        e3 = new Event3(drx(), dry(), drz(), modifiers(), id());
      }
    }
    e3.delay = this.delay();
    e3.speed = this.speed();
    e3.distance = this.distance();
    if (fired())
      return e3.fire();
    else if (flushed())
      return e3.flush();
    return e3;
  }
}
