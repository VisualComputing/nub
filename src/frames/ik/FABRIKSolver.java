/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.ik;

import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.primitives.constraint.BallAndSocket;
import frames.primitives.constraint.DistanceFieldConstraint;
import frames.primitives.constraint.Hinge;
import frames.primitives.constraint.PlanarPolygon;
import processing.core.PGraphics;

import java.lang.reflect.Array;
import java.util.ArrayList;

public abstract class FABRIKSolver extends Solver {
  //TODO: It will be useful that any Joint in the chain could have a Target ?
  //TODO: Enable Translation of Head (Skip Backward Step)

  /*Store Joint's desired position*/
  protected ArrayList<Vector> _positions = new ArrayList<Vector>();
  protected ArrayList<Quaternion> _orientations = new ArrayList<Quaternion>();
  protected ArrayList<Float> _distances = new ArrayList<Float>();

  protected ArrayList<Vector> _positions() {
    return _positions;
  }

  public int opt = 1;

  /*
   * Performs First Stage of FABRIK Algorithm, receives a chain of Frames, being the Frame at i
   * the reference frame of the Frame at i + 1
   * */
  protected void _forwardReaching(ArrayList<? extends Frame> chain) {
    for (int i = chain.size() - 2; i >= 0; i--) {
      Vector pos_i = _positions.get(i);
      Vector pos_i1 = _positions.get(i + 1);
      float dist_i = _distances.get(i + 1);
      if (dist_i == 0) {
        _positions.set(i, pos_i1.get());
        continue;
      }
      /*Check constraints (for Ball & Socket) it is not applied in First iteration
       * Look at paper FABRIK: A fast, iterative _solver for the Inverse Kinematics problem For more information*/
      pos_i = _constrainForwardReaching(chain, i);
      float r_i = Vector.distance(pos_i, pos_i1);

      float lambda_i = dist_i / r_i;
      Vector new_pos = Vector.multiply(pos_i1, 1.f - lambda_i);
      new_pos.add(Vector.multiply(pos_i, lambda_i));
      _positions.set(i, new_pos);
    }
    cop = copy_p(_positions);
    if(pg!= null)draw_pos(_positions, pg.color(0,255,0), 3);
  }

  protected float _backwardReaching(ArrayList<? extends Frame> chain) {
    float change = 0;
    Quaternion orientation;
    orientation = chain.get(0).reference() != null ? chain.get(0).reference().orientation() : new Quaternion();
    //orientation.compose(chain.get(0).rotation());
    //if(opt == 2)
    //  if(chain.size() > 1) _positions.set(1, _constrainBackwardReaching(chain, 0));
    for (int i = 0; i < chain.size() - 1; i++) {
      if (_distances.get(i + 1) == 0) {
        _positions.set(i + 1, _positions.get(i));
        continue;
      }
      //Find delta rotation
      //_positions.set(i + 1, _constrainBackwardReaching(chain, i));
      Vector newTranslation = Quaternion.compose(orientation, chain.get(i).rotation()).inverse().rotate(Vector.subtract(_positions.get(i + 1), _positions.get(i)));
      Quaternion deltaRotation = new Quaternion(chain.get(i + 1).translation(), newTranslation);
      //Apply delta rotation
      chain.get(i).rotate(deltaRotation);
      orientation.compose(chain.get(i).rotation());
      _orientations.set(i, orientation.get());
      //Vector constrained_pos = chain.get(i+1).position().get();
      Vector constrained_pos = orientation.rotate(chain.get(i + 1).translation().get());
      constrained_pos.add(_positions.get(i));
      change += Vector.distance(_positions.get(i + 1), constrained_pos);
      _positions.set(i + 1, constrained_pos);
    }
    return change;
  }




  /*
   * Check the type of the constraint related to the Frame Parent (at the i-th position),
   * Frame J is the frame used to verify if the orientation of Parent is appropriate,
   * Vector o is a Vector where Parent is located, whereas p is express the position of J
   * Vector q is the position of Child of J.
   * */

  public Vector _constrainForwardReaching(ArrayList<? extends Frame> chain, int i) {
    Frame j = chain.get(i + 1);
    Frame parent = chain.get(i + 1).reference();
    Vector o = _positions.get(i);
    Vector p = _positions.get(i + 1);
    Vector q = i + 2 >= chain.size() ? null : _positions.get(i + 2);

    if(q == null){
      if (parent.constraint() instanceof DistanceFieldConstraint) {
          //Apply other approach
          //if(opt == 1) return o.get();
          Vector translation = chain.get(i+1).translation().get();

          Vector new_translation = Vector.subtract(p,o);
          new_translation = chain.get(i).transformOf(new_translation);

          Quaternion delta = new Quaternion(translation, new_translation);
          DistanceFieldConstraint constraint = (DistanceFieldConstraint) parent.constraint();

          Quaternion desired = Quaternion.compose(parent.rotation().get(),delta);
          Quaternion constrained = constraint.apply(desired);
          constrained = Quaternion.compose(parent.rotation().inverse(), constrained);

          Vector target = constrained.rotate(translation);

          target.normalize();
          target.multiply(new_translation.magnitude());
          target.multiply(-1);
          target = chain.get(i).inverseTransformOf(target);

          target.add(p);
          return target;
      } else if(parent.constraint() instanceof PlanarPolygon){
        if(opt > 2) return o.get();
        Vector translation = chain.get(i+1).translation().get();

        Vector new_translation = Vector.subtract(p,o);
        new_translation = chain.get(i).transformOf(new_translation);

        Quaternion delta = new Quaternion(translation, new_translation);
        PlanarPolygon constraint = (PlanarPolygon) parent.constraint();

        Quaternion desired = delta;
        desired = Quaternion.compose(chain.get(i).rotation(), desired);
        //twist to frame
        Vector twist = constraint.restRotation().rotate(new Vector(0, 0, 1));
        Vector new_pos = Quaternion.multiply(desired, twist);
        //Quaternion constrained = Quaternion.compose(chain.get(i+1).rotation().get(),desired);
        Quaternion constrained = new Quaternion(twist, Quaternion.multiply(chain.get(i).rotation().inverse(), constraint.apply(new_pos)));


        Vector target = constrained.rotate(translation);

        target.normalize();
        target.multiply(new_translation.magnitude());
        target.multiply(-1);
        target = chain.get(i).inverseTransformOf(target);

        target.add(p);
        return target;

      }
    }


    if (parent.constraint() instanceof BallAndSocket) {
      if (q == null) return o.get();
      //Find the orientation of restRotation
      BallAndSocket constraint = (BallAndSocket) parent.constraint();
      Quaternion reference = Quaternion.compose(_orientations.get(i), parent.rotation().inverse());
      Quaternion restOrientation = Quaternion.compose(reference, constraint.restRotation());

      //Align axis
      Vector translation = _orientations.get(i).rotate(j.translation().get());
      Vector newTranslation = Vector.subtract(q, p);
      restOrientation = Quaternion.compose(new Quaternion(translation, newTranslation), restOrientation);

      //Vector pos_i1_constrained = _constrainForwardReaching(chain, i);
      //Vector diff = Vector.subtract(pos_i1, pos_i1_constrained);
      //pos_i.add(diff);

      //Find constraint
      Vector target = constraint.apply(Vector.subtract(p, o), restOrientation);
      target.add(o);
      Vector diff = Vector.subtract(p, target);
      return Vector.add(o, diff);
    } else if (parent.constraint() instanceof PlanarPolygon && opt == 0) {
        if (q == null) return o.get();
        //Find the orientation of restRotation
        PlanarPolygon constraint = (PlanarPolygon) parent.constraint();
        Quaternion reference = Quaternion.compose(_orientations.get(i), parent.rotation().inverse());
        Quaternion restOrientation = Quaternion.compose(reference, constraint.restRotation());

        //Align axis
        Vector translation = _orientations.get(i).rotate(j.translation().get());
        Vector newTranslation = Vector.subtract(q, p);
        restOrientation = Quaternion.compose(new Quaternion(translation, newTranslation), restOrientation);

        //Find constraint
        Vector target = constraint.apply(Vector.subtract(p, o), restOrientation);
        target.add(o);
        Vector diff = Vector.subtract(p, target);
        return Vector.add(o, diff);
    } else if(j.constraint() instanceof PlanarPolygon && opt >= 1){
      if(q == null) return o.get();
      Vector x = chain.get(i+1).transformOf(Vector.subtract(chain.get(i).position(), chain.get(i+1).position()));
      Vector y = chain.get(i+1).transformOf(Vector.subtract(chain.get(i+2).position(), chain.get(i+1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i+1).transformOf(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i+1).transformOf(w);
      Quaternion delta = new Quaternion(z,y);

      w = delta.rotate(w);
      Quaternion desired = new Quaternion(w,x);
      //System.out.println("ON JOINT " + i);

      //System.out.println("DEsired : " + desired.axis() + " angle: " + desired.angle());

      PlanarPolygon constraint = (PlanarPolygon) chain.get(i+1).constraint();
      desired = Quaternion.compose(chain.get(i+1).rotation(), desired);
      //twist to frame
      Vector twist = constraint.restRotation().rotate(new Vector(0, 0, 1));
      Vector new_pos = Quaternion.multiply(desired, twist);
      //Quaternion constrained = Quaternion.compose(chain.get(i+1).rotation().get(),desired);
      Quaternion constrained = new Quaternion(twist, Quaternion.multiply(chain.get(i+1).rotation().inverse(), constraint.apply(new_pos)));
      //System.out.println("Cons : " + constrained.axis() + " angle: " + constrained.angle());

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i+1).inverseTransformOf(target);
      target.add(p);
      return target;
    } else if (parent.constraint() instanceof Hinge) {
      if (q == null) return o.get();
      Hinge constraint = (Hinge) parent.constraint();
      Vector axis = constraint.restRotation().rotate(constraint.axis());
      //Get in terms of j_(i+1)
      axis = j.localTransformOf(axis);
      Vector x = chain.get(i+1).transformOf(Vector.subtract(p,o));
      Vector y = chain.get(i+2).translation();
      Vector z = chain.get(i+1).transformOf(Vector.subtract(q,p));
      Quaternion delta = new Quaternion(y,z);
      axis = delta.rotate(axis);
      //Project vector PO to new Plane
      Vector target = Vector.projectVectorOnPlane(x, axis);
      target = chain.get(i+1).inverseTransformOf(target);
      target.add(p);



            /*if (parent.is2D()) {
                    //Get new translation in Local Coordinate System
                Hinge constraint = (Hinge) parent.constraint();
                Vector newTranslation = Vector.subtract(p, o);
                newTranslation = orientations.get(i).inverse().rotate(newTranslation);
                Rot desired = new Rot(j.translation(), newTranslation);
                constraint.constrainRotation(desired, parent);
            }*/
    } else if (j.constraint() instanceof DistanceFieldConstraint) {
      if(q == null) return o.get();
      Vector x = chain.get(i+1).transformOf(Vector.subtract(chain.get(i).position(), chain.get(i+1).position()));
      Vector y = chain.get(i+1).transformOf(Vector.subtract(chain.get(i+2).position(), chain.get(i+1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i+1).transformOf(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i+1).transformOf(w);
      Quaternion delta = new Quaternion(z,y);

      w = delta.rotate(w);
      Quaternion desired = new Quaternion(w,x);
      DistanceFieldConstraint constraint = (DistanceFieldConstraint) chain.get(i+1).constraint();
      Quaternion constrained = Quaternion.compose(chain.get(i+1).rotation().get(),desired);
      constrained = constraint.apply(constrained);
      constrained = Quaternion.compose(chain.get(i+1).rotation().inverse(), constrained);

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i+1).inverseTransformOf(target);
      target.add(p);


      /*

      w = delta.rotate(w);
      w = offset.rotate(w);

      DistanceFieldConstraint constraint = (DistanceFieldConstraint) chain.get(i+1).constraint();

      Quaternion desired = new Quaternion(y,w);
      Quaternion constrained = Quaternion.compose(j.rotation().get(),desired);
      constrained = constraint.apply(constrained);
      constrained = Quaternion.compose(j.rotation().inverse(), constrained);
      constrained = Quaternion.compose(desired.inverse(), constrained);


      Vector target = w.get();
      target = constrained.rotate(target);
      target = offset.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i+1).inverseTransformOf(target);
      target.add(p);
      */
      return target;



      //if(true )return o.get();
      //Get axis
      //Vector translation = chain.get(i).inverseTransformOf(chain.get(i+1).translation().get());

      /*Vector ch_tr = chain.get(i+1).inverseTransformOf(chain.get(i+2).translation());
      System.out.println("TRanslation : " + ch_tr);
      Quaternion quaternion = new Quaternion(ch_tr, Vector.subtract(q, p));
      Vector tr = chain.get(i).inverseTransformOf(chain.get(i+1).translation());
      Vector translation = quaternion.rotate(tr);
      System.out.println("TRanslation : " + tr);

      DistanceFieldConstraint constraint = (DistanceFieldConstraint) j.constraint();
      Vector axis = translation;

      Quaternion desired = new Quaternion(Vector.subtract(p, o), axis);
      Quaternion constrained = constraint.apply(desired);
      Vector target = constrained.rotate(Vector.subtract(p, o));
      target.add(o);
      System.out.println("Axis : " + axis);
      System.out.println("Desired : " + desired.rotate(axis));
      System.out.println("Real : " + Vector.subtract(o,p));
      System.out.println("constrained : " + constrained.rotate(axis));

      //Find the orientation of restRotation
      //Quaternion reference = _orientations.get(i).get();
      //Quaternion restOrientation = Quaternion.compose(reference, constraint.restRotation());
      //Vector translation = j.translation().get();
      //Vector axis = reference.inverseRotate(Vector.subtract(q, p));
      //reference.compose(new Quaternion(translation, axis));

      //Vector newTranslation = reference.inverseRotate(Vector.subtract(p, o));
      //Quaternion desired = Quaternion.compose(parent.rotation(), new Quaternion(translation, newTranslation));
      //Find constraint
      //Quaternion constrained = constraint.apply(desired);
      //reference.compose(parent.rotation().inverse());
      //Vector target = Quaternion.compose(reference, constrained).rotate(translation);
      //return Vector.add(o, target);
      return target;*/
    }
    return o.get();
  }

  public Vector _constrainBackwardReaching(ArrayList<? extends Frame> chain, int i) {
    Frame j = chain.get(i + 1);
    Vector o = _positions.get(i);
    Vector p = _positions.get(i + 1);
    Vector q = i + 2 >= chain.size() ? null : _positions.get(i + 2);

    if (j.constraint() instanceof PlanarPolygon) {
      if (q == null) return p.get();
      Vector translation = chain.get(i + 1).localTransformOf(chain.get(i + 1).translation().get());
      Vector new_translation = Vector.subtract(p, o);
      new_translation = chain.get(i + 1).transformOf(new_translation);
      Quaternion q1 = new Quaternion(translation, new_translation);

      System.out.println("ON JOINT " + i);

      System.out.println("q1 : " + q1.axis() + " angle: " + q1.angle());

      Vector a = q1.rotate(chain.get(i + 2).translation().get());
      Vector b = chain.get(i + 1).transformOf(Vector.subtract(q, p));

      Quaternion delta = new Quaternion(a, b);

      System.out.println("delta : " + delta.axis() + " angle: " + delta.angle());

      PlanarPolygon constraint = (PlanarPolygon) j.constraint();
      //delta = Quaternion.compose(j.rotation(), delta);

      Vector twist = constraint.restRotation().rotate(new Vector(0, 0, 1));
      Vector new_pos = Quaternion.multiply(delta, twist);

      Quaternion constrained = new Quaternion(twist, Quaternion.multiply(chain.get(i+1).rotation().inverse(), constraint.apply(new_pos)));
      constrained = new Quaternion();

      Quaternion desired = delta.angle() > 0.001 ? Quaternion.compose(Quaternion.compose(j.rotation(), delta), constrained.inverse()) : new Quaternion();

      Vector target = new_translation;
      target = desired.rotate(target);
      System.out.println("delta : " + delta.axis() + " angle: " + delta.angle());
      System.out.println("Cons : " + constrained.axis() + " ang : " + constrained.angle());
      System.out.println("desired : " + desired.axis() + " ang : " + desired.angle());

      Quaternion proof = new Quaternion(new Vector(1,0,0),0);
      Quaternion proof2 = new Quaternion(new Vector(1,0,0),0);
      Quaternion proof3 = Quaternion.compose(proof, proof2.inverse());
      System.out.println("proof : " + proof3.axis() + " ang : " + proof3.angle());
      proof = new Quaternion(new Vector(-1,0,0),0);
      proof2 = new Quaternion(new Vector(-1,0,0),0);
      proof3 = Quaternion.compose(proof, proof2.inverse());
      System.out.println("proof : " + proof3.axis() + " ang : " + proof3.angle());


      target = chain.get(i + 1).inverseTransformOf(target);
      target.add(o);
      return target;
    } else if (j.constraint() instanceof DistanceFieldConstraint) {
      if(q == null) return p.get();
      Vector translation = chain.get(i+1).localTransformOf(chain.get(i+1).translation().get());
      Vector new_translation = Vector.subtract(p,o);
      new_translation  = chain.get(i+1).transformOf(new_translation);
      Quaternion q1 = new Quaternion(translation,new_translation);

      Vector a = q1.rotate(chain.get(i+2).translation().get());
      Vector b = chain.get(i+1).transformOf(Vector.subtract(q,p));

      Quaternion delta = new Quaternion(a,b);

      DistanceFieldConstraint constraint = (DistanceFieldConstraint) j.constraint();
      Quaternion constrained = Quaternion.compose(j.rotation(), delta);
      constrained = constraint.apply(constrained);
      constrained = Quaternion.compose(j.rotation().inverse(), constrained);

      Quaternion desired = Quaternion.compose(constrained.inverse(), delta);

      Vector target = q1.rotate(translation);
      target = desired.rotate(target);
      //System.out.println("Cons : " + constrained.axis() + " ang : " + constrained.angle());
      target = chain.get(i+1).inverseTransformOf(target);
      target.add(o);




      /*Vector c = chain.get(i+1).transformOf(Vector.subtract(q,p));
      Quaternion offset = new Quaternion(c, chain.get(i+2).translation().get());

      Vector a = chain.get(i+1).localTransformOf(chain.get(i+1).translation().get());
      a = offset.rotate(a);
      Vector new_translation = Vector.subtract(p,o);
      Vector b = chain.get(i+1).transformOf(new_translation);
      b = offset.rotate(b);

      Quaternion desired = new Quaternion(a,b).inverse();

      desired.compose(j.rotation(), desired);
      //desired = Quaternion.compose(desired, offset);

      System.out.println("1. Translation - world : " + a);
      System.out.println("2. New Translation - world : " + b);
      System.out.println("3. O - world : " + o);
      System.out.println("4. P - world : " + p);

      System.out.println("4. offset : " + offset.axis() + " ang : " + offset.angle());

      DistanceFieldConstraint constraint = (DistanceFieldConstraint) j.constraint();

      Quaternion constrained = constraint.apply(desired);
      constrained = Quaternion.compose(j.rotation().inverse(), constrained);
      constrained = constrained.inverse();

      //constrained = Quaternion.compose(parent.rotation().inverse(), constrained);
      //constrained = Quaternion.compose(parent.reference().orientation(), constrained);
      //constrained = Quaternion.compose(offset.inverse(), constrained);

      //System.out.println("Cons : " + constrained.axis() + " ang : " + constrained.angle());
      Vector target = a;
      target = constrained.rotate(target);
      target = offset.inverseRotate(target);
      target = chain.get(i+1).inverseTransformOf(target);
      target.normalize();
      target.multiply(new_translation.magnitude());
      target.add(o);
      System.out.println("5. Target - world : " + target);*/
      return target;
    }
    return p.get();
  }

  protected float _distance(ArrayList<? extends Frame> chain) {
    float distance = 0.f;
    for (int i = 0; i < chain.size(); i++) {
      distance += Vector.distance(chain.get(i).position(), _positions.get(i));
    }
    return distance;
  }

  /// TO DEBUG
  public ArrayList<Vector> copy_p(ArrayList<Vector> _positions){
    ArrayList<Vector> copy = new ArrayList<Vector>();
    for(Vector p : _positions){
      copy.add(p.get());
    }
    return copy;
  }

  public static PGraphics pg;
  public ArrayList<Vector> cop;

  public void draw_pos(){
    draw_pos(cop, pg.color(0,255,0), 3);
  }

  public void draw_pos(ArrayList<Vector> pos, int color, float str) {
    if(pos == null) return;
    Vector prev = null;
    for(Vector p : pos){
      pg.pushMatrix();
      pg.pushStyle();
      pg.stroke(color);
      pg.strokeWeight(str);
      if(prev != null) pg.line(prev.x(),prev.y(),prev.z(), p.x(),p.y(),p.z());
      pg.noStroke();
      pg.fill(color, 100);
      pg.translate(p.x(),p.y(),p.z());
      pg.sphere(3);
      pg.popStyle();
      pg.popMatrix();
      prev = p;
    }

  }

  public FABRIKSolver() {
    super();
  }
}