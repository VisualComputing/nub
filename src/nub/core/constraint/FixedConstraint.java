/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.core.constraint;

import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.core.Node;

public class FixedConstraint extends Constraint {

    @Override
    public Quaternion constrainRotation(Quaternion rotation, Node frame) {
        return new Quaternion();
    }

    @Override
    public Vector constrainTranslation(Vector translation, Node frame) {
        return new Vector();
    }
}

