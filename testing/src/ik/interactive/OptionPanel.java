package ik.interactive;

import frames.core.Frame;
import frames.core.constraint.Hinge;
import frames.core.constraint.PlanarPolygon;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;

import processing.core.PApplet;
import processing.core.PConstants;

import java.util.ArrayList;
import java.util.List;

public class OptionPanel {

    protected Scene _scene;
    protected Frame _frame;
    protected List<Slider> _frameSliders = new ArrayList<>();

    public OptionPanel(PApplet pApplet, float x, float y, int width, int height){
        _scene = new Scene(pApplet, pApplet.createGraphics(width, height, PConstants.P2D), (int) x, (int) y);
        _scene.pApplet().textSize(_scene.height()/50.f);
        setupPanel();
    }

    public void setFrame(Frame frame){
        _frame = frame;
        updateFrameOptions();
    }

    public void setupPanel(){
        float[] size = _scene.boundaryWidthHeight();
        float textSize = _scene.height()/50.f;
        float textHeight = (textSize*2*size[1]/_scene.height());
        createFrameOptions(-size[0], -size[1] + 2*textHeight, (int)(2*size[0]*0.8f), (int)Math.ceil(textHeight));
        updateFrameOptions();
    }

    public void createFrameOptions(float x, float y, int width, int height){
        Slider s = new Slider(_scene, "Roll  ", new Vector(x,y), width, height){
            @Override
            public void pointerInteract(Object... objects){
                if(_frame == null) return;
                Vector euler = _frame.rotation().eulerAngles();
                _frame.setRotation(new Quaternion(this.value(), euler.y(), euler.z()));
            }
        };
        s.setRange(-PApplet.PI, PApplet.PI);
        _frameSliders.add(s);
        s = new Slider(_scene, "Pitch ", new Vector(x,y + height*2), width, height){
            @Override
            public void pointerInteract(Object... objects){
                if(_frame == null) return;
                Vector euler = _frame.rotation().eulerAngles();
                _frame.setRotation(new Quaternion(euler.x(), this.value(), euler.z()));
            }
        };
        s.setRange(-PApplet.PI, PApplet.PI);
        _frameSliders.add(s);
        s = new Slider(_scene, "Yaw  ", new Vector(x,y + height*4), width, height){
            @Override
            public void pointerInteract(Object... objects){
                if(_frame == null) return;
                Vector euler = _frame.rotation().eulerAngles();
                _frame.setRotation(new Quaternion(euler.x(), euler.y(), this.value()));
            }
        };
        s.setRange(-PApplet.PI, PApplet.PI);
        _frameSliders.add(s);
    }

    public void updateFrameOptions(){
        if(_frame == null){
            for(Slider s : _frameSliders) s._bar.cull(true);
        } else {
            Vector euler = _frame.rotation().eulerAngles();
            for(int i = 0; i < 3; i++){
                _frameSliders.get(i)._bar.cull(false);
                _frameSliders.get(i).setValue(euler._vector[i]);
            }
        }
    }

}
