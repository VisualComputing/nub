package ik.interactive;

import frames.core.Frame;
import frames.primitives.Point;
import frames.processing.Scene;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

public class MultipleViews {
    List<AuxiliaryView> _auxiliaryViews;
    AuxiliaryView _defaultView;
    AuxiliaryView _currentView;
    Scene _scene;

    public MultipleViews(Scene scene) {
        _scene = scene;
        _defaultView = new AuxiliaryView(_scene, new Frame(_scene.eye().position(), _scene.eye().orientation(), _scene.eye().magnitude()), 0,0, scene.width(), scene.height());
        _currentView = _defaultView;
        _auxiliaryViews = new ArrayList<>();
    }

    public void addAuxiliaryView(AuxiliaryView view){
        _auxiliaryViews.add(view);
    }

    protected void update(float x, float y){
        AuxiliaryView view = setCurrentView(x, y);
        if (view == _currentView) return;
        _currentView = view;
        _scene.setEye(_currentView.eye());
        _scene.setCenter(_currentView.center());
        _scene.setRadius(_currentView.radius());
    }

    public AuxiliaryView setCurrentView(float x, float y){
        for(AuxiliaryView view : _auxiliaryViews) {
            if (view.focus(x, y)) return view;
        }
        return _defaultView;
    }

    public Point cursorLocation(float x , float y){
        return  _currentView.cursorLocation(x,y);
    }

    protected void setPGraphics(PGraphics pg){

    }

    public void setBackBuffer(){
        _scene.backBuffer().beginDraw();
        _scene.backBuffer().background(0);
        _scene.render(_currentView.scene().backBuffer(), _currentView.type(), _currentView.eye(), _currentView.zNear(), _currentView.zFar());
        _scene.backBuffer().endDraw();
        _scene.backBuffer().loadPixels();
    }

    protected void draw(){
        setBackBuffer();
        //InteractiveJoint.setPGraphics(_defaultView._pGraphics);
        //_defaultView.draw();
        for(AuxiliaryView view : _auxiliaryViews) {
            InteractiveJoint.setPGraphics(view._pGraphics);
            view.draw();
            view._pGraphics.beginDraw();
            setPGraphics(view._pGraphics);
            view._pGraphics.endDraw();
            view.display();

        }
        update(_scene.pApplet().mouseX, _scene.pApplet().mouseY);
    }
}
