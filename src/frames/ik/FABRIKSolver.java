/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/


package frames.ik;

import frames.primitives.Frame;
import frames.primitives.Vector;
import frames.primitives.Quaternion;
import frames.primitives.constraint.*;

import java.util.ArrayList;

public abstract class FABRIKSolver extends Solver {
    //TODO: It will be useful that any Joint in the chain could have a Target ?
    //TODO: Enable Translation of Head (Skip Backward Step)

    /*Store Joint's desired position*/
    protected ArrayList<Vector> positions = new ArrayList<Vector>();
    protected ArrayList<Quaternion> orientations = new ArrayList<Quaternion>();
    protected ArrayList<Float> distances = new ArrayList<Float>();

    public ArrayList<Vector> getPositions() {
        return positions;
    }

    /*
    * Performs First Stage of FABRIK Algorithm, receives a chain of Frames, being the Frame at i
    * the reference frame of the Frame at i + 1
    * */
    public void executeForwardReaching(ArrayList<? extends Frame> chain) {
        for (int i = chain.size() - 2; i >= 0; i--) {
            Vector pos_i = positions.get(i);
            Vector pos_i1 = positions.get(i + 1);
            float r_i = Vector.distance(pos_i, pos_i1);
            float dist_i = distances.get(i + 1);
            if (dist_i == 0) {
                positions.set(i, pos_i1.get());
                continue;
            }
                /*Check constraints (for Ball & Socket) it is not applied in First iteration
                * Look at paper FABRIK: A fast, iterative solver for the Inverse Kinematics problem For more information*/
            Vector pos_i1_constrained = applyConstraintsForwardStage(chain, i);
            Vector diff = Vector.subtract(pos_i1, pos_i1_constrained);
            pos_i.add(diff);
            float lambda_i = dist_i / r_i;
            Vector new_pos = Vector.multiply(pos_i1, 1.f - lambda_i);
            new_pos.add(Vector.multiply(pos_i, lambda_i));
            positions.set(i, new_pos);
        }
    }

    public float executeBackwardReaching(ArrayList<? extends Frame> chain) {
        float change = 0;
        Quaternion orientation;
        orientation = chain.get(0).reference() != null ? chain.get(0).reference().orientation() : new Quaternion();
        //orientation.compose(chain.get(0).rotation());
        for (int i = 0; i < chain.size() - 1; i++) {
            if (distances.get(i + 1) == 0) {
                positions.set(i + 1, positions.get(i));
                continue;
            }
            //Find delta rotation
            Vector newTranslation = Quaternion.compose(orientation, chain.get(i).rotation()).inverse().rotate(Vector.subtract(positions.get(i + 1), positions.get(i)));
            Quaternion deltaRotation = new Quaternion(chain.get(i + 1).translation(), newTranslation);
            //Apply delta rotation
            chain.get(i).rotate(deltaRotation);
            orientation.compose(chain.get(i).rotation());
            orientations.set(i, orientation.get());
            //Vector constrained_pos = chain.get(i+1).position().get();
            Vector constrained_pos = orientation.rotate(chain.get(i + 1).translation().get());
            constrained_pos.add(positions.get(i));
            change += Vector.distance(positions.get(i + 1), constrained_pos);
            positions.set(i + 1, constrained_pos);
        }
        return change;
    }


        /*
        * Check the type of the constraint related to the Frame Parent (at the i-th position),
        * Frame J is the frame used to verify if the orientation of Parent is appropriate,
        * Vector o is a Vector where Parent is located, whereas p is express the position of J
        * Vector q is the position of Child of J.
        * */

    public Vector applyConstraintsForwardStage(ArrayList<? extends Frame> chain, int i) {
        Frame j = chain.get(i + 1);
        Frame parent = chain.get(i + 1).reference();
        Vector o = positions.get(i);
        Vector p = positions.get(i + 1);
        Vector q = i + 2 >= chain.size() ? null : positions.get(i + 2);
        if (parent.constraint() instanceof BallAndSocket) {
            if (q == null) return p.get();
            //Find the orientation of restRotation
            BallAndSocket constraint = (BallAndSocket) parent.constraint();
            Quaternion reference = Quaternion.compose(orientations.get(i), parent.rotation().inverse());
            Quaternion restOrientation = Quaternion.compose(reference, constraint.getRestRotation());

            //Align axis
            Vector translation = orientations.get(i).rotate(j.translation().get());
            Vector newTranslation = Vector.subtract(q, p);
            restOrientation = Quaternion.compose(new Quaternion(translation, newTranslation), restOrientation);

            //Find constraint
            Vector target = constraint.getConstraint(Vector.subtract(p, o), restOrientation);
            return Vector.add(o, target);
        } else if (parent.constraint() instanceof PlanarPolygon) {
            if (q == null) return p.get();
            //Find the orientation of restRotation
            PlanarPolygon constraint = (PlanarPolygon) parent.constraint();
            Quaternion reference = Quaternion.compose(orientations.get(i), parent.rotation().inverse());
            Quaternion restOrientation = Quaternion.compose(reference, constraint.getRestRotation());

            //Align axis
            Vector translation = orientations.get(i).rotate(j.translation().get());
            Vector newTranslation = Vector.subtract(q, p);
            restOrientation = Quaternion.compose(new Quaternion(translation, newTranslation), restOrientation);

            //Find constraint
            Vector target = constraint.getConstraint(Vector.subtract(p, o), restOrientation);
            return Vector.add(o, target);
        } else if (parent.constraint() instanceof Hinge) {
            /*if (parent.is2D()) {
                    //Get new translation in Local Coordinate System
                Hinge constraint = (Hinge) parent.constraint();
                Vector newTranslation = Vector.subtract(p, o);
                newTranslation = orientations.get(i).inverse().rotate(newTranslation);
                Rot desired = new Rot(j.translation(), newTranslation);
                constraint.constrainRotation(desired, parent);
            }*/
        }
        return p;
    }

    public float change(ArrayList<? extends Frame> chain) {
        float change = 0.f;
        for (int i = 0; i < chain.size(); i++) {
            change += Vector.distance(chain.get(i).position(), positions.get(i));
        }
        return change;
    }

    public FABRIKSolver() {
        super();
    }
}