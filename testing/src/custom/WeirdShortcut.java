package custom;

import remixlab.bias.Shortcut;

/**
 * Created by pierre on 11/24/16.
 */
public class WeirdShortcut extends Shortcut {
  public WeirdShortcut(int m, int id) {
    super(m, id);
  }

  @Override
  public Class eventClass() {
    return WeirdEvent.class;
  }

  public static int registerID(int id, String description) {
    return Shortcut.registerID(WeirdShortcut.class, id, description);
  }

  public static int registerID(String description) {
    return Shortcut.registerID(WeirdShortcut.class, description);
  }

  public static boolean hasID(int id) {
    return Shortcut.hasID(WeirdShortcut.class, id);
  }
}