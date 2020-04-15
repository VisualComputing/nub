package nub.ik.animation;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.TimingTask;
import nub.timing.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SkeletonAnimation {
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
        moveToNextPosture(); //1. move to the next posture required
        interpolate(_time); //2. interpolate between two frames
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

    public void interpolate(float time){
        if(_currentPosture + 1 >= _postures.size()) return;
        int nextPosture = _currentPosture + 1;
        float t1 = _postures.get(_currentPosture)._time;
        float t2 = _postures.get(nextPosture)._time;
        float w = (time - t1) / (t2 - t1);
        interpolate(_postures.get(_currentPosture).posture(), _postures.get(nextPosture).posture(), w);
    }

    public void interpolate(Posture p1, Posture p2, float w){
        for(Map.Entry<String, Node> entry : _skeleton._joints.entrySet()){
            Vector translation = new Vector();
            Quaternion rotation = new Quaternion();

            Posture.NodeInformation n1 = p1.jointState(entry.getKey());
            Posture.NodeInformation n2 = p2.jointState(entry.getKey());

            translation = Vector.lerp(n1._translation, n2._translation, w);
            rotation = Quaternion.slerp(n1._rotation, n2._rotation, w);

            Node node = entry.getValue();
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
