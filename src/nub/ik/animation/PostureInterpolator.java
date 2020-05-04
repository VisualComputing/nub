package nub.ik.animation;

import nub.core.Interpolator;
import nub.core.Node;
import nub.processing.TimingTask;
import nub.timing.Task;

import java.util.*;

public class PostureInterpolator {
    protected class KeyPosture{
        protected Posture _posture;
        protected float _time;

        public KeyPosture(Posture posture, float time){
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

    protected List<KeyPosture> _postures;
    protected Skeleton _skeleton;
    protected HashMap<Node, Interpolator> _interpolators;

    // Beat
    protected Task _task;
    protected float _t;
    protected float _speed;

    // Misc
    protected boolean _recurrent;


    public PostureInterpolator(Skeleton skeleton){
        _skeleton = skeleton;
        _postures = new ArrayList<KeyPosture>();
        _interpolators = new HashMap<Node, Interpolator>();
        _recurrent = false;
        _initInterpolators();
        _t = 0.0f;
        _speed = 1.0f;
        _task = new TimingTask() {
            @Override
            public void execute() {
                PostureInterpolator.this._execute();
            }
        };
    }

    public Skeleton skeleton(){
        return _skeleton;
    }

    public int size(){
        return _postures.size();
    }

    public Task task(){
        return _task;
    }

    public void _execute(){
        if ((_postures.isEmpty()) || (skeleton() == null))
            return;
        if ((_speed > 0.0) && (time() >= _postures.get(_postures.size() - 1)._time))
            setTime(_postures.get(0)._time);
        if ((_speed < 0.0) && (time() <= _postures.get(0)._time))
            setTime(_postures.get(_postures.size() - 1)._time);
        interpolate(time());
        _t += _speed * _task.period() / 1000.0f;
        if (time() > _postures.get(_postures.size() - 1)._time) {
            if (isRecurrent())
                setTime(_postures.get(0)._time + _t - _postures.get(_postures.size() - 1)._time);
            else {
                // Make sure last KeyFrame is reached and displayed
                interpolate(_postures.get(_postures.size() - 1)._time);
                _task.stop();
            }
        } else if (time() < _postures.get(0)._time) {
            if (isRecurrent())
                setTime(_postures.get(_postures.size() - 1)._time - _postures.get(0)._time + _t);
            else {
                // Make sure first KeyFrame is reached and displayed
                interpolate(_postures.get(0)._time);
                _task.stop();
            }
        }
    }

    public void toggle() {
        _task.toggle();
    }

    public void run() {
        _task.run();
    }


    public void run(float speed) {
        setSpeed(speed);
        _task.run();
    }

    public void run(int period, float speed) {
        setSpeed(speed);
        _task.run(period);
    }

    public void reset() {
        _task.stop();
        setTime(firstTime());
    }

    public void setTime(float time) {
        _t = time;
    }

    public float time() {
        return _t;
    }

    public float duration() {
        return lastTime() - firstTime();
    }

    public float firstTime() {
        return _postures.isEmpty() ? 0.0f : _postures.get(0)._time;
    }

    public float lastTime() {
        return _postures.isEmpty() ? 0.0f : _postures.get(_postures.size() - 1)._time;
    }

    public void disableRecurrence() {
        enableRecurrence(false);
    }

    public void enableRecurrence() {
        enableRecurrence(true);
    }

    public void enableRecurrence(boolean enable) {
        _recurrent = enable;
    }

    public boolean isRecurrent() {
        return _recurrent;
    }

    public float speed() {
        return _speed;
    }

    public void increaseSpeed(float delta) {
        setSpeed(speed() + delta);
    }

    public void setSpeed(float speed) {
        _speed = speed;
    }

    public void loadPosture(int i){
        _postures.get(i).posture().loadValues(_skeleton);
    }

    public void savePosture(float time){
        _postures.add(new KeyPosture(new Posture(_skeleton), time));
    }

    public HashMap<Float, Node> keyFrames() {
        HashMap map = new HashMap<Float, Posture>();
        for (KeyPosture keyPosture : _postures)
            map.put(keyPosture._time, keyPosture._posture);
        return map;
    }

    //Define a list of interpolators per joint
    protected void _initInterpolators(){
        for(Node joint : _skeleton.BFS()){
            Interpolator interpolator = new Interpolator(Node.detach(joint.translation().get(), joint.rotation().get(), joint.magnitude()));
            _interpolators.put(joint, interpolator);
        }
    }

    public void addKeyPosture(Posture posture, float time) {
        if (_postures.size() == 0) {
            if (time < 0)
                return;
        } else if (time <= 0)
            return;
        if (skeleton() == null)
            return;
        _postures.add(new KeyPosture(posture, _postures.isEmpty() ? time : _postures.get(_postures.size() - 1)._time + time));
        //add key frames
        for(Map.Entry<String, Node> entry : posture._nodeInformation.entrySet()){
            Node joint = _skeleton.joint(entry.getKey());
            Interpolator interpolator = _interpolators.get(joint);
            Node info = entry.getValue();
            //create detached node with local information
            if(joint.rotation().w() < 0 && info.rotation().w() > 0){
                //change sign
                info.rotation().setX(-info.rotation().x());
                info.rotation().setY(-info.rotation().y());
                info.rotation().setZ(-info.rotation().z());
                info.rotation().setW(-info.rotation().w());
            }

            Node detach = Node.detach(info.translation().get(), info.rotation().get(), info.magnitude());
            interpolator.addKeyFrame(detach, time);
        }
        reset();
    }


    public boolean removeKeyPosture(float time) {
        boolean result = false;
        ListIterator<KeyPosture> listIterator = _postures.listIterator();
        while (listIterator.hasNext()) {
            KeyPosture keyPosture = listIterator.next();
            if (keyPosture._time == time) {
                if (_task.isActive())
                    _task.stop();
                setTime(firstTime());
                listIterator.remove();
                result = true;
            }
        }

        for(Interpolator interpolator: _interpolators.values()){
            interpolator.removeKeyFrame(time);
        }
        return result;
    }

    public void clear() {
        _task.stop();
        _postures.clear();
        for(Interpolator interpolator: _interpolators.values()){
            interpolator.clear();
        }
    }


    public void interpolate(float time){
        for(Node joint : skeleton().BFS()){
            Interpolator interpolator = _interpolators.get(joint);
            interpolator.interpolate(time);
            //update skeleton joint
            Node node = interpolator.node();
            //System.out.println("joint " + joint);
            joint.setTranslation(node.translation().get());
            joint.setRotation(node.rotation().get());
            joint.setMagnitude(node.magnitude());
            //System.out.println("joint " + joint);
        }
        _skeleton.restoreTargetsState();

    }
}
