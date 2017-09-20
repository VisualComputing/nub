package custom;

import remixlab.bias.BogusEvent;

/**
 * Created by pierre on 11/24/16.
 */
public class WeirdEvent extends BogusEvent {
  protected float x, y;

  public WeirdEvent(float dx, float dy, int modifiers, int id) {
    super(modifiers, id);
    this.x = dx;
    this.y = dy;
  }

  @Override
  public WeirdShortcut shortcut() {
    return new WeirdShortcut(modifiers(), id());
  }
}