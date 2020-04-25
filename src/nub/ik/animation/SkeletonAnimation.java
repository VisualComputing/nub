package nub.ik.animation;

import nub.core.Graph;
import nub.core.Interpolator;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.TimingTask;
import nub.timing.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkeletonAnimation {
    public enum InterpolationMode{
        LINEAR, CATMULL_ROM;
    }

    public static class TimePosture{
        protected Posture _posture;
        protected float _time; //expressed in milliseconds

        public TimePosture(Posture posture, float time){
            _posture = posture;
            _time = time;
        }

        public Posture posture(){
            return _posture;
        }

        public float time(){
            return _time;
        }

        public void setTime(float t){
            _time = t;
        }

        public void setPosture(Posture posture){
            _posture = posture;
        }

    }

    protected List<TimePosture> _postures;
    protected Skeleton _skeleton;
    protected InterpolationMode _mode = InterpolationMode.CATMULL_ROM;

    protected int _currentPosture;

    protected boolean _repeat = false;
    protected float _time = 0;
    protected Task _task;

    public SkeletonAnimation(Skeleton skeleton){
        _skeleton = skeleton;
        _postures = new ArrayList<TimePosture>();
        _task = new TimingTask() {
            @Override
            public void execute() {
                SkeletonAnimation.this.execute();
            }
        };
    }

    public Skeleton skeleton(){
        return _skeleton;
    }

    public void setInterpolationMode(InterpolationMode mode){
        _mode = mode;
    }

    public InterpolationMode mode(){
        return _mode;
    }

    public void loadPosture(int i){
        _postures.get(i).posture().loadValues(_skeleton);
    }

    public void savePosture(float time){
        _postures.add(new TimePosture(new Posture(_skeleton), time));
    }

    public List<TimePosture> postures(){
        return _postures;
    }

    public void execute(){
        if(_postures.isEmpty()){
            _task.stop();
            return;
        }
        moveToNextPosture();
        if(_mode == InterpolationMode.LINEAR) {
            _linearInterpolation(_time);
        } else if(_mode == InterpolationMode.CATMULL_ROM){
            _catmullRomInterpolation(_time);
        }
        _time += _task.period(); //3. update time
    }

    public float time(){
        return _time;
    }

    public void setTime(float time){
        _time = time;
    }

    public void moveToNextPosture(){
        int next = _currentPosture;
        while(next < _postures.size() && _time >= _postures.get(next).time()){
            next++;
        }

        if(next == _postures.size()){
            if(_time >= _postures.get(_currentPosture).time()) {
                if (_repeat) {
                    next = 0;
                    setTime(0);
                } else{
                    next = _postures.size() - 1;
                    stop();
                }
            }
        } else {
            next--;
        }

        if(next != _currentPosture){
            _currentPosture = next;
            loadPosture(_currentPosture);
            _skeleton.restoreTargetsState();
        }
    }

    public void _catmullRomInterpolation(float time){
        if(_currentPosture + 1 >= _postures.size()) return;
        int i1 = _currentPosture;
        int i0 = i1 - 1 >= 0 ? i1 - 1 : i1;
        int i2 = i1 + 1;
        int i3 = i2 + 1 <= _postures.size() - 1 ? i2 + 1 : i2;

        float prev = _postures.get(i1)._time;
        float next = _postures.get(i2)._time;

        for(Node node : _skeleton.BFS()) {
            if (node == _skeleton.reference() || !_skeleton._names.containsKey(node)) continue;
            String name = _skeleton.jointName(node);
            Node n0 = _postures.get(i0)._posture.jointState(name);
            Node n1 = _postures.get(i1)._posture.jointState(name);
            Node n2 = _postures.get(i2)._posture.jointState(name);
            Node n3 = _postures.get(i3)._posture.jointState(name);
            _catmullInterpolation(node, n0, n1, n2, n3, time, prev, next);
        }
        _skeleton.restoreTargetsState();
    }

    public void _catmullInterpolation(Node node, Node n0, Node n1, Node n2, Node n3, float time, float prevTime, float nextTime){
        //Calculate tangents
        Vector tangentVector1 = Vector.multiply(Vector.subtract(n2.translation(), n0.translation()), 0.5f);
        Vector tangentVector2 = Vector.multiply(Vector.subtract(n3.translation(), n1.translation()), 0.5f);
        Quaternion tangentQuaternion1 = Quaternion.squadTangent(n0.rotation(), n1.rotation(), n2.rotation());
        Quaternion tangentQuaternion2 = Quaternion.squadTangent(n1.rotation(), n2.rotation(), n3.rotation());
        //Calculate v1, v2
        Vector deltaP = Vector.subtract(n2.translation(), n1.translation());
        Vector vector1 = Vector.add(Vector.multiply(deltaP, 3.0f), Vector.multiply(tangentVector1, (-2.0f)));
        vector1 = Vector.subtract(vector1, tangentVector2);
        Vector vector2 = Vector.add(Vector.multiply(deltaP, (-2.0f)), tangentVector1);
        vector2 = Vector.add(vector2, tangentVector2);

        float alpha = 0;
        if((nextTime - prevTime) > Float.MIN_VALUE){
            alpha = (time - prevTime) / (nextTime - prevTime);
        }

        Vector v = Vector.add(n1.translation(), Vector.multiply(Vector.add(tangentVector1,
                        Vector.multiply(Vector.add(vector1, Vector.multiply(vector2, alpha)), alpha)), alpha));

        float mag = Vector.lerp(n1.magnitude(), n2.magnitude(), alpha);

        Quaternion q = Quaternion.squad(n1.rotation(),
                tangentQuaternion1,
                tangentQuaternion2,
                n2.rotation(), alpha);

        Constraint constraint = node.constraint();
        node.setConstraint(null);
        node.setTranslation(v.get());
        node.setRotation(q.get());
        node.setMagnitude(mag);
        node.setConstraint(constraint);
    }


    public void _linearInterpolation(float time){
        if(_currentPosture + 1 >= _postures.size()) return;
        int nextPosture = _currentPosture + 1;
        float t1 = _postures.get(_currentPosture)._time;
        float t2 = _postures.get(nextPosture)._time;
        float w = (time - t1) / (t2 - t1);
        _linearInterpolation(_postures.get(_currentPosture).posture(), _postures.get(nextPosture).posture(), w);
    }

    public void _linearInterpolation(Posture p1, Posture p2, float w){
        for(Node node : _skeleton.BFS()){
            if(node == _skeleton.reference() || !_skeleton._names.containsKey(node)) continue;
            String name = _skeleton.jointName(node);

            Vector translation = new Vector();
            Quaternion rotation = new Quaternion();

            Node n1 = p1.jointState(name);
            Node n2 = p2.jointState(name);

            translation = Vector.lerp(n1.translation(), n2.translation(), w);
            rotation = Quaternion.slerp(n1.rotation(), n2.rotation(), w);

            Constraint constraint = node.constraint();
            node.setConstraint(null);
            node.setTranslation(translation.get());
            node.setRotation(rotation.get());
            node.setConstraint(constraint);
        }
        _skeleton.restoreTargetsState();
    }

    public void play(){
        if(_postures.isEmpty()){
            return;
        }
        _time = 0;
        _currentPosture = 0;
        loadPosture(_currentPosture);
        _skeleton.restoreTargetsState();
        _task.run();
    }

    public void stop(){
        _task.stop();
    }

}
