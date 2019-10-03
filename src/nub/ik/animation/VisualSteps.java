package nub.ik.animation;


import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;

/**
 * Here you could find some common Visual Steps used in visualization of IK algorithms
 * */
public class VisualSteps {
    public static class RotateNode extends VisualStep{
        protected enum Rotate {HIGHLIGHT, NONE}
        protected Rotate _mode = Rotate.HIGHLIGHT;
        protected float _radius;
        protected Node _node;
        protected int _color;
        protected Quaternion _initial, _final, _deltaPerFrame, _delta;

        public RotateNode(Scene scene, Node node, float radius, long period, long duration, long renderingDuration) {
            super(scene, period, duration, renderingDuration);
            _color = _scene.pApplet().color(0, 0, 255); //use some default colors
            _radius = radius;
            _node = node;
        }

        public void setRotation(Quaternion delta) {
            _completed = false;
            _delta = delta;
            initialize();
        }

        protected void _calculateSpeedPerIteration() {
            float inc = ((float) _period) / (_duration - _duration % _period);
            _deltaPerFrame = new Quaternion(_delta.axis(), _delta.angle() * inc);
        }

        @Override
        public void initialize() {
            _initial = _node.rotation().get();
            _delta = _node.constraint() != null ? _node.constraint().constrainRotation(_delta, _node) : _delta;
            _final = Quaternion.compose(_initial, _delta);
            _completed = false;
            _times = 0;
            _calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil(_duration / _period) + 1;
            //how many times to render?
            _totalRenderingTimes = (int) Math.ceil( _renderingDuration / _period) + 1;
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if(_completed && _times >= _totalRenderingTimes){
                _keepDrawing = false;
            } else if (_times >= _totalTimes) {
                //set rotation to fit exactly final rotation
                _node.setRotation(_final.get());
                _completed = true;
            }else if (1 - Math.pow(Quaternion.dot(_node.rotation(), _final), 2) >= 0.000001) {
                _node.rotate(_deltaPerFrame);
            }
            _times++;
        }

        @Override
        public void render() {
            if(_mode == RotateNode.Rotate.NONE) return;
            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            pg.pushMatrix();
            _scene.applyWorldTransformation(_node);
            pg.noStroke();
            if(!_completed) pg.fill(_color);
            else pg.fill(_color, 150);
            pg.sphere(_radius);
            pg.popMatrix();
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
            pg.popStyle();
        }
    }


    public static class MessageStep extends VisualStep {
        protected enum Location {TOP, BOTTOM, CUSTOM};
        protected float _textSize = 28;
        protected int _color;
        protected String _message;
        protected Vector _start_location, _end_location;
        protected Location _mode = Location.BOTTOM;

        public MessageStep(Scene scene, String message, long period, long duration, long renderingDuration) {
            super(scene, period, duration, renderingDuration);
            _color = _scene.pApplet().color(255);
            _message = message;
            initialize();
        }

        public void setLocation(Location location){
            _mode = location;
            switch (_mode){
                case TOP:{
                    _start_location = new Vector(1.f *_scene.width() / 16.f, 1.f * _scene.height() / 8.f);
                    _end_location = new Vector(14.f *_scene.width() / 16.f,  3.f * _scene.height() / 8.f);
                    break;
                }
                case BOTTOM:{
                    _start_location = new Vector(1.f *_scene.width() / 16.f, 6.f * _scene.height() / 8.f);
                    _end_location = new Vector(14.f *_scene.width() / 16.f,  _scene.height());
                    break;
                }
            }
        }

        public void setLocation(Vector start, Vector end){
            _mode = Location.CUSTOM;
            _start_location = start; //in terms of screen
            _end_location = end; //in terms of screen
            initialize();
        }

        public void setSize(float size){
            _textSize = size;
        }

        @Override
        public void initialize() {
            _completed = false;
            _times = 0;
            _totalTimes = (int) Math.ceil(_duration / _period) + 1;
            setLocation(_mode);
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if(_completed && _times >= _totalRenderingTimes){
                _keepDrawing = false;
            } else if(_times++ >= _totalTimes) {
                //set rotation to fit exactly final rotation
                _completed = true;
            }
            _times++;
        }

        @Override
        public void render() {
            PGraphics pg = _scene.context();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            pg.pushStyle();
            if (_message != null) {
                _scene.beginHUD();
                pg.stroke(_color);
                pg.fill(_color);
                pg.textSize(_textSize);
                pg.textAlign(PConstants.CENTER);
                pg.text(_message, _start_location.x(),_start_location.y(), _end_location.x(), _end_location.y());
                _scene.endHUD();
            }
            pg.popStyle();
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
        }
    }

    public static class FollowTrajectoryStep extends VisualStep{
        protected enum Line {V1_TO_V2, V_TO_V2, V1_TO_V};
        protected Node _reference;
        protected float _radius;
        protected Vector[] _trajectory;
        protected Vector _current, _delta;
        protected int _idx = 0;
        protected int _cline, _cv1, _cv2, _cv;
        protected Line _mode = Line.V1_TO_V;

        public FollowTrajectoryStep(Scene scene, float radius, long period, long duration, long renderingDuration) {
            super(scene, period, duration, renderingDuration);
            _radius = radius;
            //use some default colors
            _cv1 = _scene.pApplet().color(0, 0, 255);
            _cv2 = _scene.pApplet().color(0, 0, 255);
            _cv = _scene.pApplet().color(0, 0, 255);
            _cline = _scene.pApplet().color(255, 0, 0);
        }

        public void setTrajectory(Node node, Vector... trajectory) {
            _reference = node;
            _trajectory = trajectory;
            initialize();
        }

        protected void _calculateSpeedPerIteration() {
            Vector v1 = _trajectory[_idx];
            Vector v2 = _trajectory[_idx + 1];
            float inc = ((float) _period) / (_duration - _duration % _period);
            _delta = Vector.subtract(v2, v1);
            _delta.multiply(inc);
        }

        @Override
        public void initialize() {
            _idx = 0;
            _current = _trajectory[_idx].get();
            _completed = false;
            _times = 0;
            _calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil(_duration / _period) + 1;
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if(_completed && _times >= _totalRenderingTimes){
                _keepDrawing = false;
            } else if(_times >= _totalTimes){
                _completed = true;
            } else {
                Vector v2 = _trajectory[_idx + 1];
                if (Vector.distance(_current, v2) < 0.1) {
                    _idx++;
                    if (_idx == _trajectory.length - 1) {
                        _idx--;
                    } else {
                        _current = v2;
                        _calculateSpeedPerIteration();
                    }
                } else {
                    _current.add(_delta);
                }
            }
            _times++;
        }

        @Override
        public void render() {
            Vector v1 = _trajectory[_idx];
            Vector v2 = _trajectory[_idx + 1 < _trajectory.length ? _idx + 1 : _idx];
            switch (_mode) {
                case V1_TO_V: {
                    v2 = _current;
                    break;
                }
                case V_TO_V2: {
                    v1 = _current;
                    break;
                }
            }
            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.pushMatrix();
            if(_reference != null) _scene.applyWorldTransformation(_reference);
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            if (_scene.is3D()) {
                pg.noStroke();
                pg.pushMatrix();
                pg.translate(_current.x(), _current.y(), _current.z());
                if(!_completed) pg.fill(_cv2);
                else pg.fill(_cv2, 150);
                pg.sphere(_radius);
                pg.popMatrix();
                pg.fill(_cline);
                if (Vector.distance(v1, v2) > 1.2f * _radius) _scene.drawArrow(v1, v2, _radius / 4f);
            } else {
                drawSegment2D(pg, v1, v2, _radius, pg.color(_cline, _completed ? 255 : 100), pg.color(_cv1, _completed ? 255 : 100), pg.color(_cv2, _completed ? 255 : 100));
                if(!_completed) pg.fill(_cv);
                else pg.stroke(_cv, 150);
                pg.ellipse(_current.x(), _current.y(), _radius, _radius);
            }
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
            pg.popMatrix();
            pg.popStyle();
        }
    }


    //Some common rendering routines
    public static void drawSegment2D(PGraphics pg, Vector v1, Vector v2, float radius, int cline, int cv1, int cv2) {
        pg.pushStyle();
        pg.strokeWeight(radius / 2.f);
        pg.stroke(cline);
        pg.fill(cline);
        pg.line(v1.x(), v1.y(), v2.x(), v2.y());
        pg.strokeWeight(radius);
        pg.stroke(cv1);
        pg.fill(cv1);
        pg.ellipse(v1.x(), v1.y(), radius, radius);
        pg.stroke(cv2);
        pg.fill(cv2);
        pg.ellipse(v2.x(), v2.y(), radius, radius);
        pg.popStyle();
    }
}
