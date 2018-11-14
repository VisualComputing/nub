package ik.interactive;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Vector;
import frames.processing.Scene;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PGraphics;

public class InteractiveJoint extends Joint {
    protected Vector _desiredTranslation;
    public InteractiveJoint(Scene scene, int color, float radius) {
        super(scene, color, radius);
    }
    public InteractiveJoint(Scene scene, float radius) {
        super(scene, radius);
    }

    @Override
    public void interact(Object... gesture){
        String command = (String) gesture[0];
        if(command.matches("Add")){
            if(_desiredTranslation != null) {
                addChild();
            }
            _desiredTranslation = null;
        } else if(command.matches("OnAdding")){
            _desiredTranslation = translateDesired();
        } else if(command.matches("Reset")){
            _desiredTranslation = null;
        } else if(command.matches("Remove")){
            removeChild();
        }
    }
    @Override
    public void visit(){
        super.visit();
        //Draw desired position
        Scene scene = (Scene) this._graph;
        PGraphics pg = scene.frontBuffer();
        if(_desiredTranslation != null){
            pg.pushStyle();
            pg.stroke(pg.color(0,255,0));
            pg.strokeWeight(_radius/2);
            pg.line(0,0,0, _desiredTranslation.x()/this.scaling(), _desiredTranslation.y()/this.scaling(), _desiredTranslation.z()/this.scaling());
            pg.popStyle();
        }
    }

    public void addChild(){
        InteractiveJoint joint = new InteractiveJoint((Scene)this._graph, this.radius());
        joint.setReference(this);
        joint.setTranslation(joint.translateDesired());
    }

    public void removeChild(){
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
