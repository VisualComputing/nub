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

import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;
import remixlab.util.Util;

/**
 * A {@link remixlab.bias.event.MotionEvent} with six degrees-of-freedom ( {@link #x()},
 * {@link #y()}, {@link #z()} , {@link #rx()}, {@link #ry()} and {@link #rz()}).
 */
public class DOF6Event extends MotionEvent {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(x).append(dx).append(y).append(dy).append(z)
        .append(dz).append(rx).append(drx).append(ry).append(dry).append(rz).append(drz).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    DOF6Event other = (DOF6Event) obj;
    return new EqualsBuilder().appendSuper(super.equals(obj)).append(x, other.x).append(dx, other.dx).append(y, other.y)
        .append(dy, other.dy).append(z, other.z).append(dz, other.dz).append(rx, other.rx).append(drx, other.drx)
        .append(ry, other.ry).append(dry, other.dry).append(rz, other.rz).append(drz, other.drz).isEquals();
  }

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
  public DOF6Event(float dx, float dy, float dz, float drx, float dry, float drz, int modifiers, int id) {
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
   * {@code this(prevEvent instanceof DOF6Event ? (DOF6Event) prevEvent : null, x, y, z, rx, ry, rz, modifiers, id)}.
   *
   * @see #DOF6Event(DOF6Event, float, float, float, float, float, float, int, int)
   */
  public DOF6Event(MotionEvent prevEvent, float x, float y, float z, float rx, float ry, float rz, int modifiers,
                   int id) {
    this(prevEvent instanceof DOF6Event ? (DOF6Event) prevEvent : null, x, y, z, rx, ry, rz, modifiers, id);
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
  public DOF6Event(DOF6Event prevEvent, float x, float y, float z, float rx, float ry, float rz, int modifiers, int id) {
    super(modifiers, id);
    this.x = x;
    this.y = y;
    this.z = z;
    this.rx = rx;
    this.ry = ry;
    this.rz = rz;
    setPreviousEvent(prevEvent);
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
  public DOF6Event(float dx, float dy, float dz, float drx, float dry, float drz) {
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
   * {@code this(prevEvent instanceof DOF6Event ? (DOF6Event) prevEvent : null, x, y, z, rx, ry, rz)}.
   *
   * @see #DOF6Event(DOF6Event, float, float, float, float, float, float)
   */
  public DOF6Event(MotionEvent prevEvent, float x, float y, float z, float rx, float ry, float rz) {
    this(prevEvent instanceof DOF6Event ? (DOF6Event) prevEvent : null, x, y, z, rx, ry, rz);
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
  public DOF6Event(DOF6Event prevEvent, float x, float y, float z, float rx, float ry, float rz) {
    super();
    this.x = x;
    this.y = y;
    this.z = z;
    this.rx = rx;
    this.ry = ry;
    this.rz = rz;
    setPreviousEvent(prevEvent);
  }

  protected DOF6Event(DOF6Event other) {
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
  public DOF6Event get() {
    return new DOF6Event(this);
  }

  @Override
  public DOF6Event flush() {
    return (DOF6Event) super.flush();
  }

  @Override
  public DOF6Event fire() {
    return (DOF6Event) super.fire();
  }

  @Override
  protected void setPreviousEvent(MotionEvent prevEvent) {
    rel = true;
    if (prevEvent != null)
      if (prevEvent instanceof DOF6Event && prevEvent.id() == this.id()) {
        this.dx = this.x() - ((DOF6Event) prevEvent).x();
        this.dy = this.y() - ((DOF6Event) prevEvent).y();
        this.dz = this.z() - ((DOF6Event) prevEvent).z();
        this.drx = this.rx() - ((DOF6Event) prevEvent).rx();
        this.dry = this.ry() - ((DOF6Event) prevEvent).ry();
        this.drz = this.rz() - ((DOF6Event) prevEvent).rz();
        distance = Util.distance(x, y, z, rx, ry, rz, ((DOF6Event) prevEvent).x(), ((DOF6Event) prevEvent).y(),
            ((DOF6Event) prevEvent).z(), ((DOF6Event) prevEvent).rx(), ((DOF6Event) prevEvent).ry(),
            ((DOF6Event) prevEvent).rz());
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
  public void modulate(float[] sens) {
    if (sens != null)
      if (sens.length >= 6 && this.isAbsolute()) {
        dx = dx * sens[0];
        dy = dy * sens[1];
        dz = dz * sens[2];
        drx = drx * sens[3];
        dry = dry * sens[4];
        drz = drz * sens[5];
      }
  }

  @Override
  public boolean isNull() {
    if (Util.zero(dx()) && Util.zero(dy()) && Util.zero(dz()) && Util.zero(drx()) && Util.zero(dry()) && Util.zero(drz()))
      return true;
    return false;
  }

  /**
   * Convenience function that simply returns {@code return dof3Event(true)}
   *
   * @see #dof3Event(boolean)
   */
  public DOF3Event dof3Event() {
    return dof3Event(true);
  }

  /**
   * Reduces the event to a {@link remixlab.bias.event.DOF3Event} (lossy reduction).
   *
   * @param fromTranslation if true keeps dof1, dof2 and dof3; otherwise keeps dof4, dof4 and dof6.
   */
  public DOF3Event dof3Event(boolean fromTranslation) {
    DOF3Event pe3;
    DOF3Event e3;
    if (isRelative()) {
      if (fromTranslation) {
        pe3 = new DOF3Event(null, prevX(), prevY(), prevZ(), modifiers(), id());
        e3 = new DOF3Event(pe3, x(), y(), z(), modifiers(), id());
      } else {
        pe3 = new DOF3Event(null, prevRX(), prevRY(), prevRZ(), modifiers(), id());
        e3 = new DOF3Event(pe3, rx(), ry(), rz(), modifiers(), id());
      }
    } else {
      if (fromTranslation) {
        e3 = new DOF3Event(dx(), dy(), dz(), modifiers(), id());
      } else {
        e3 = new DOF3Event(drx(), dry(), drz(), modifiers(), id());
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
