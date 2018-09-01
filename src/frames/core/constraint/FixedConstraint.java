package frames.core.constraint;

import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.core.Frame;

//TODO : Update

/**
 * Created by sebchaparr on 23/06/18.
 */
public class FixedConstraint extends Constraint {

    @Override
    public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
        return new Quaternion();
    }

    @Override
    public Vector constrainTranslation(Vector translation, Frame frame) {
        return new Vector();
    }
}

