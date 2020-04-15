package ik.trik.animation;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.animation.Posture;
import nub.ik.animation.Skeleton;
import nub.ik.animation.SkeletonAnimation;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PConstants;
import processing.core.PGraphics;

public class KeyPoint extends Node {
    public enum Status{
        ENABLED, DISABLED, EMPTY
    }

    protected TimeLine _timeLine;
    protected KeyPoint _prev, _next;
    protected float _radius, _time;
    protected Status _status = Status.EMPTY;
    protected SkeletonAnimation.TimePosture _timePosture;


    public KeyPoint(TimeLine timeLine, float radius){
        super(timeLine);
        _timeLine = timeLine;
        _radius = radius;
        //Add translation constraint
        setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                Vector v1 = node.translation();
                Vector t = new Vector(translation.x(), 0); //no vertical translation allowed
                //2. keep certain distance w.r.t parent
                if(_prev != null) {
                    Vector v2 = _prev.translation();
                    if (t.x() + v1.x() - v2.x() < 2 * _radius) {
                        t.setX(2 * _radius - v1.x() + v2.x());
                    }
                } else{
                    if(t.x() + v1.x() < 0){
                        t.setX(-v1.x());
                    }
                }

                return t;
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                return new Quaternion(); //no rotation is allowed
            }
        });
        //dummy conversion from pix to units
        float pixels = 1.f * _timeLine._panel._scene.height() / _timeLine._panel._height;
        setPickingThreshold(-_radius * pixels); //as pixels and graph units corresponds
    }

    @Override
    public void graphics(PGraphics pg){
        pg.pushStyle();
        pg.stroke(_timeLine._current == this ? _timeLine._panel._green2 : _timeLine._panel._gray1);
        pg.strokeWeight(_timeLine._current == this ? 5 : 3);
        switch (_status){
            case EMPTY:{
                pg.fill(180);
                break;
            }
            case ENABLED:{
                pg.fill(_timeLine._panel._green1);
                break;
            }
            case DISABLED:{
                pg.fill(_timeLine._panel._red);
                break;
            }
        }
        pg.ellipse(0,0, _radius * 2, _radius * 2);

        pg.textSize(10);
        pg.stroke(_timeLine._panel._gray1);
        pg.fill(_timeLine._panel._gray1);

        pg.textAlign(PConstants.CENTER, PConstants.CENTER);
        pg.text("" + String.format("%.2f", _time), 0, _radius + 10f);
        pg.popStyle();
    }

    @Override
    public void translate(Vector vector) {
        translation().add(constraint() != null ? constraint().constrainTranslation(vector, this) : vector);
        _modified();
        //translate next KeyPoint if required
        if(_next != null) {
            Vector v2 = _next.translation();
            if (v2.x() - translation().x() < 2 * _radius) {
                _next.translate(-v2.x() + translation().x() + 2 * _next._radius, 0, 0);
            }
        }
        _timeChanged();
    }

    @Override
    public void interact(Object... gesture){
        if(gesture.length >= 1){
            String s = (String) gesture[0];
            if(s == "onClicked"){
                _timeLine._current = this;
                if (!_status.equals(Status.EMPTY)) loadPosture();
            }
        }
    }

    public void savePosture(){
        //first delete posture
        Status prev = _status;
        deletePosture();
        Skeleton skeleton = _timeLine._panel._skeletonAnimation.skeleton();
        Posture posture = new Posture(skeleton);
        _timePosture = new SkeletonAnimation.TimePosture(posture, _time);
        if(_status.equals(Status.DISABLED)) return;
        _status = Status.DISABLED;
        if(!prev.equals(Status.DISABLED)){
            enable();
        }
    }

    public void loadPosture(){
        if(_status.equals(Status.EMPTY)) return;
        _timePosture.posture().loadValues(_timeLine._panel._skeletonAnimation.skeleton());
        //restore targets to eff position
        _timeLine._panel._skeletonAnimation.skeleton().restoreTargetsState();
    }

    public void deletePosture(){
        if(_status.equals(Status.EMPTY)) return;
        disable(); //disable the posture
        _timePosture = null;
        _status = Status.EMPTY;
    }

    public void disable(){
        if(_status.equals(Status.DISABLED) || _status.equals(Status.EMPTY)) return;
        //Remove the posture from the list
        _timeLine._panel._skeletonAnimation.postures().remove(_timePosture);
        //change status to disabled
        _status = Status.DISABLED;
    }

    public void enable(){
        if(_status.equals(Status.ENABLED) || _status.equals(Status.EMPTY)) return; //no modification is required
        //Add the posture to the animation handler list in the correct position
        SkeletonAnimation skeletonAnimation = _timeLine._panel._skeletonAnimation;
        if(skeletonAnimation.postures().isEmpty() || skeletonAnimation.postures().get(skeletonAnimation.postures().size() - 1).time() <= _time){
            skeletonAnimation.postures().add(_timePosture); //add posture at the end of the list
            _status = Status.ENABLED;
            return;
        }

        for(int i = 0; i < skeletonAnimation.postures().size(); i++){
            SkeletonAnimation.TimePosture p = skeletonAnimation.postures().get(i);
            if(_time < p.time()){
                skeletonAnimation.postures().add(i,_timePosture);
                _status = Status.ENABLED;
                return;
            }
        }
    }

    protected void _timeChanged(){
        _time = Vector.distance(_timeLine.position(), this.position()) / _timeLine._spaceStep * _timeLine._timeStep;
        if(_status.equals(Status.EMPTY)) return;
        _timePosture.setTime(_time);
    }

}
