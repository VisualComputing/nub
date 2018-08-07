/**
 * Lamp by Jean Pierre Charalambos.
 * 
 * This class is part of the Luxo example.
 * 
 * Add a graphics handler to an InteractiveFrame to automatically pick
 * an object. The object is described in the graphics handler procedure.
 */

class Lamp implements PConstants {
  Scene scene;
  Piece[] pieces;

  Lamp(Scene s) {
    scene = s;
    pieces = new Piece[4];

    for (int i = 0; i < 4; ++i) {
      pieces[i] = new Piece(scene);
      frame(i).setReference(i > 0 ? pieces[i - 1] : null);
      frame(i).setHighlighting(Shape.Highlighting.FRONT);
    }

    // Initialize frames
    frame(1).setTranslation(0f, 0f, 8f); // Base height
    frame(2).setTranslation(0, 0, 50);  // Arm length
    frame(3).setTranslation(0, 0, 50);  // Arm length

    frame(1).setRotation(new Quaternion(new Vector(1.0f, 0.0f, 0.0f), 0.6f));
    frame(2).setRotation(new Quaternion(new Vector(1.0f, 0.0f, 0.0f), -2.0f));
    frame(3).setRotation(new Quaternion(new Vector(1.0f, -0.3f, 0.0f), -1.7f));

    // Set frame graphics modes
    frame(0).mode = 1;
    frame(1).mode = 2;
    frame(2).mode = 2;
    frame(3).mode = 3;

    // Set frame constraints
    WorldConstraint baseConstraint = new WorldConstraint();
    baseConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vector(0.0f, 0.0f, 1.0f));
    baseConstraint.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vector(0.0f, 0.0f, 1.0f));
    frame(0).setConstraint(baseConstraint);

    LocalConstraint XAxis = new LocalConstraint();
    XAxis.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vector(0.0f, 0.0f, 0.0f));
    XAxis.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vector(1.0f, 0.0f, 0.0f));
    frame(1).setConstraint(XAxis);
    frame(2).setConstraint(XAxis);

    LocalConstraint headConstraint = new LocalConstraint();
    headConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vector(0.0f, 0.0f, 0.0f));
    frame(3).setConstraint(headConstraint);
  }

  Piece frame(int i) {
    return pieces[i];
  }
}
