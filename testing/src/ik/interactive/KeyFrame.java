package ik.interactive;

import nub.core.Node;
import nub.core.Graph;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;


public class KeyFrame extends Node {
    Target _target;
    protected float time; //TODO Use time attribute
    protected Vector _desiredTranslation;

    public KeyFrame(Target target){
        super(target.graph());
        _target = target;
    }

    public KeyFrame(Target target, Vector translation, Quaternion rotation){
        super(target.graph(), translation, rotation, 1);
        _target = target;
    }

    @Override
    public void interact(Object... gesture){
        String command = (String) gesture[0];
        if(command.matches("Add")){
            if(_desiredTranslation != null) {
                add((boolean) gesture[2], (Vector) gesture[1]);
            }
            _desiredTranslation = null;
        } else if(command.matches("OnAdding")){
            _desiredTranslation = translateDesired((Vector) gesture[1]);
        } else if(command.matches("Reset")){
            _desiredTranslation = null;
        } else if(command.matches("Remove")){
            remove();
        }
    }

    public void add(boolean left, Vector mouse){
        KeyFrame frame = new KeyFrame(this._target);
        frame.setTranslation(this.worldLocation(this.translateDesired(mouse)));
        _target.addKeyFrame(frame, this, left);
    }

    public void remove(){
        _target.removeKeyFrame(this);
        _graph.prune(this);
    }

    //------------------------------------
    //Interactive actions - same method found in Graph Class (duplicated cause of visibility)
    protected Vector _translateDesired(float dx, float dy, float dz, int zMax, Node node) {
        Scene scene = (Scene) _graph;
        if (scene.is2D() && dz != 0) {
            System.out.println("Warning: graph is 2D. Z-translation reset");
            dz = 0;
        }
        dx = scene.isEye(node) ? -dx : dx;
        dy = scene.isRightHanded() ^ scene.isEye(node) ? -dy : dy;
        dz = scene.isEye(node) ? dz : -dz;
        // Scale to fit the screen relative vector displacement
        if (scene.type() == Graph.Type.PERSPECTIVE) {
            float k = (float) Math.tan(scene.fov() / 2.0f) * Math.abs(
                    scene.eye().location(scene.isEye(node) ? scene.anchor() : node.position())._vector[2] * scene.eye().magnitude());
            //TODO check me weird to find height instead of width working (may it has to do with fov?)
            dx *= 2.0 * k / (scene.height() * scene.eye().magnitude());
            dy *= 2.0 * k / (scene.height() *scene. eye().magnitude());
        }
        // this expresses the dz coordinate in world units:
        //Vector eyeVector = new Vector(dx, dy, dz / eye().magnitude());
        Vector eyeVector = new Vector(dx, dy, dz * 2 * scene.radius() / zMax);
        return node.reference() == null ? scene.eye().worldDisplacement(eyeVector) : node.reference().displacement(eyeVector, scene.eye());
    }


    public Vector translateDesired(Vector mouse){
        Scene scene = (Scene) _graph;
        float dx = mouse.x() - scene.screenLocation(position()).x();
        float dy = mouse.y() - scene.screenLocation(position()).y();
        return _translateDesired(dx, dy, 0, Math.min(scene.width(), scene.height()), this);
    }
}
