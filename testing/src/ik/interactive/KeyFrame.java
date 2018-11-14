package ik.interactive;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;


public class KeyFrame extends Frame {
    Target _target;
    protected float time; //TODO Use time attribute
    protected Vector _desiredTranslation;

    public KeyFrame(Target target){
        super(target.graph());
        _target = target;
    }

    public KeyFrame(Target target, Vector translation, Quaternion rotation){
        super(target.graph(), null, translation, rotation, 1);
        _target = target;
    }

    @Override
    public void interact(Object... gesture){
        String command = (String) gesture[0];
        if(command.matches("Add")){
            if(_desiredTranslation != null) {
                add((boolean) gesture[2]);
            }
            _desiredTranslation = null;
        } else if(command.matches("OnAdding")){
            _desiredTranslation = translateDesired();
        } else if(command.matches("Reset")){
            _desiredTranslation = null;
        } else if(command.matches("Remove")){
            remove();
        }
    }

    public void add(boolean left){
        KeyFrame frame = new KeyFrame(this._target);
        frame.setTranslation(frame.translateDesired());
        _target.addKeyFrame(frame, this, left);
    }

    public void remove(){
        _target.removeKeyFrame(this);
        _graph.pruneBranch(this);
    }

    //------------------------------------
    //Interactive actions - same method found in Graph Class
    public Vector translateDesired(){
        Scene scene = (Scene) _graph;
        PApplet pApplet = scene.pApplet();
        float dx = pApplet.mouseX - scene.screenLocation(position()).x();
        float dy = pApplet.mouseY - scene.screenLocation(position()).y();

        dy = scene.isRightHanded() ? -dy : dy;
        if(scene.type() == Graph.Type.PERSPECTIVE){
            float k = (float) Math.tan(scene.fieldOfView() / 2.0f) * Math.abs(
                    scene.eye().location(scene.isEye(this) ? scene.anchor() : this.position())._vector[2] * scene.eye().magnitude());
            dx *= 2.0 * k / scene.height();
            dy *= 2.0 * k / scene.height();
        }
        else {
            float[] wh = scene.boundaryWidthHeight();
            dx *= 2.0 * wh[0] / scene.width();
            dy *= 2.0 * wh[1] / scene.height();
        }
        Vector eyeVector = new Vector(dx / scene.eye().magnitude(), dy / scene.eye().magnitude(), 0);
        return this.reference() == null ? scene.eye().worldDisplacement(eyeVector) : this.reference().displacement(eyeVector, scene.eye());
    }
}
