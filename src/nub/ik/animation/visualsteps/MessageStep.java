package nub.ik.animation.visualsteps;

import nub.ik.animation.VisualStep;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;

public class MessageStep extends VisualStep {
    protected String _message;
    protected Vector _start_location, _end_location;

    public MessageStep(Scene scene, String message, long period, long duration, long renderingDuration) {
        super(scene, period, duration, renderingDuration);
        _message = message;
        initialize();
    }

    public void setLocation(){
        switch ((String) attributes().get("location")){
            case "TOP":{
                _start_location = new Vector(1.f *_scene.width() / 16.f, 1.f * _scene.height() / 8.f);
                _end_location = new Vector(14.f *_scene.width() / 16.f,  3.f * _scene.height() / 8.f);
                break;
            }
            case "BOTTOM":{
                _start_location = new Vector(1.f *_scene.width() / 16.f, 6.f * _scene.height() / 8.f);
                _end_location = new Vector(14.f *_scene.width() / 16.f,  _scene.height());
                break;
            }
            case "CUSTOM":{
                _start_location = (Vector)_attributes.get("startLocation"); //in terms of screen
                _end_location = (Vector) _attributes.get("endLocation");
                break;
            }
            default:{
                throw new RuntimeException("Location attribute must have either of the following values:" +
                        "TOP, BOTTOM or CUSTOM ");
            }
        }
    }

    @Override
    public void _onInit() {
        setLocation();
    }

    @Override
    public void reverse() {

    }

    @Override
    protected void _onComplete(){
        //Do nothing...
    }

    @Override
    protected void _onRunning(){
        //Do nothing...
    }

    @Override
    public void render() {
        PGraphics pg = _scene.context();
        pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.pushStyle();
        if (_message != null) {
            _scene.beginHUD();
            pg.stroke((int)_attributes.get("color"));
            pg.fill((int)_attributes.get("color"));
            pg.textSize((int)_attributes.get("textSize"));
            pg.textAlign(PConstants.CENTER);
            pg.text(_message, _start_location.x(),_start_location.y(), _end_location.x(), _end_location.y());
            _scene.endHUD();
        }
        pg.popStyle();
        pg.hint(PConstants.ENABLE_DEPTH_TEST);
    }

    @Override
    protected void _defineAttributes(){
        _attributes.put("location", "BOTTOM"); //Choose among the following modes: TOP, BOTTOM, CUSTOM
        _attributes.put("textSize", 24);
        _attributes.put("color", _scene.pApplet().color(255));
    }
}
