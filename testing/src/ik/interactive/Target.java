package ik.interactive;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.Interpolator;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.TimingTask;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;

import java.util.ArrayList;
import java.util.List;

public class Target extends Frame {
    //TODO : Reduce code Duplicaion by means of Abstract Class
    //TODO : Improve multiple translation
    protected static List<Target> _selectedTargets = new ArrayList<Target>();
    protected Interpolator _interpolator;
    protected List<KeyFrame> _path = new ArrayList<KeyFrame>();
    protected PShape _redBall;
    protected Vector _desiredTranslation;
    protected ArrayList<Vector> _last = new ArrayList<>();


    public Target(Scene scene, Frame frame) {
        super(scene);
        _redBall = scene.is3D() ? scene.frontBuffer().createShape(PConstants.SPHERE, ((Joint) scene.trackedFrame()).radius() * 2f) :
                scene.frontBuffer().createShape(PConstants.ELLIPSE, 0,0, ((Joint) scene.trackedFrame()).radius() * 4f, ((Joint) scene.trackedFrame()).radius() * 4f);
        _redBall.setStroke(false);
        _redBall.setFill(scene.pApplet().color(255, 0, 0));

        _interpolator = new Interpolator(this);
        shape(_redBall);
        setReference(scene.trackedFrame().reference());
        setPosition(frame.position());
        setOrientation(frame.orientation());
        setPickingThreshold(0);

        Target t = this;
        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                _last.add(t.position());
                while(_last.size() > 50) _last.remove(0);
            }
        };
        scene.registerTask(task);
        task.run(150);
    }

    public ArrayList<Vector> last(){ return _last; }
    public void drawPath(){
        ((Scene) _graph).drawPath(_interpolator, 5);
    }

    @Override
    public void interact(Object... gesture) {
        String command = (String) gesture[0];
        if (command.matches("KeepSelected")) {
            if(!_selectedTargets.contains(this)){
                _redBall.setFill(((Scene)graph()).pApplet().color(0,255,0));
                _selectedTargets.add(this);
            }
            else{
                _redBall.setFill(((Scene)graph()).pApplet().color(255,0,0));
                _selectedTargets.remove(this);
            }
        } else if(command.matches("Add")){
            if(_desiredTranslation != null) {
                if(!_path.isEmpty())removeKeyFrame(_path.get(0));
                KeyFrame frame = new KeyFrame(this);
                frame.translate(this.worldLocation(this.translateDesired((Vector) gesture[1])));
                addKeyFrame(frame, 0);
            }
            _desiredTranslation = null;
        } else if(command.matches("OnAdding")){
            _desiredTranslation = translateDesired((Vector) gesture[1]);
        } else if(command.matches("Reset")){
            _desiredTranslation = null;
        }
    }

    public static List<Target> selectedTargets(){
        return _selectedTargets;
    }

    public static void multipleTranslate(){
        for(Target target : _selectedTargets){
            if(target.graph().defaultFrame() != target)
                ((Scene)target.graph()).translate(target);
        }
    }

    public static void clearSelectedTargets(){
        for(int i = _selectedTargets.size()-1; i >= 0; i--){
            _selectedTargets.get(i).interact("KeepSelected");
        }
    }

    public void updateInterpolator(){
        _interpolator.clear();
        for(KeyFrame frame : _path){
            _interpolator.addKeyFrame(frame);
            //_interpolator.addKeyFrame(frame, frame.time);
        }
    }

    public void addKeyFrame(KeyFrame frame, int index){
        _path.add(index, frame);
        updateInterpolator();
    }

    public void addKeyFrame(KeyFrame frame, KeyFrame adjacent, boolean left){
        int index =  _path.indexOf(adjacent);
        if(!left) index = index + 1;
        addKeyFrame(frame, index);
    }

    public void removeKeyFrame(KeyFrame frame){
        _path.remove(frame);
        updateInterpolator();
    }

    //------------------------------------
    //Interactive actions - same method found in Graph Class (duplicated cause of visibility)
    protected Vector _translateDesired(float dx, float dy, float dz, int zMax, Frame frame) {
        Scene scene = (Scene) _graph;
        if (scene.is2D() && dz != 0) {
            System.out.println("Warning: graph is 2D. Z-translation reset");
            dz = 0;
        }
        dx = scene.isEye(frame) ? -dx : dx;
        dy = scene.isRightHanded() ^ scene.isEye(frame) ? -dy : dy;
        dz = scene.isEye(frame) ? dz : -dz;
        // Scale to fit the screen relative vector displacement
        if (scene.type() == Graph.Type.PERSPECTIVE) {
            float k = (float) Math.tan(scene.fov() / 2.0f) * Math.abs(
                    scene.eye().location(scene.isEye(frame) ? scene.anchor() : frame.position())._vector[2] * scene.eye().magnitude());
            //TODO check me weird to find height instead of width working (may it has to do with fov?)
            dx *= 2.0 * k / (scene.height() * scene.eye().magnitude());
            dy *= 2.0 * k / (scene.height() *scene. eye().magnitude());
        }
        // this expresses the dz coordinate in world units:
        //Vector eyeVector = new Vector(dx, dy, dz / eye().magnitude());
        Vector eyeVector = new Vector(dx, dy, dz * 2 * scene.radius() / zMax);
        return frame.reference() == null ? scene.eye().worldDisplacement(eyeVector) : frame.reference().displacement(eyeVector, scene.eye());
    }


    public Vector translateDesired(Vector mouse){
        Scene scene = (Scene) _graph;
        float dx = mouse.x() - scene.screenLocation(position()).x();
        float dy = mouse.y() - scene.screenLocation(position()).y();
        return _translateDesired(dx, dy, 0, Math.min(scene.width(), scene.height()), this);
    }
}
