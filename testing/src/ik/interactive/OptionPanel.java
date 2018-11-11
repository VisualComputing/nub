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
                //Get rotation around X- Axis
                _setOrientation(new Vector(1,0,0), this.value());
            }
        };
        s.setRange(-PApplet.PI, PApplet.PI);
        _frameSliders.add(s);
        s = new Slider(_scene, "Pitch ", new Vector(x,y + height*2), width, height){
            @Override
            public void pointerInteract(Object... objects){
                if(_frame == null) return;
                //Get rotation around Y - Axis
                _setOrientation(new Vector(0,1,0), this.value());
            }
        };
        s.setRange(-PApplet.PI, PApplet.PI);
        _frameSliders.add(s);
        s = new Slider(_scene, "Yaw  ", new Vector(x,y + height*4), width, height){
            @Override
            public void pointerInteract(Object... objects){
                if(_frame == null) return;
                //Get rotation around Z - Axis
                _setOrientation(new Vector(0,0,1), this.value());
            }
        };
        s.setRange(-PApplet.PI, PApplet.PI);
        _frameSliders.add(s);
    }

    public void updateFrameOptions(){
        if(_frame == null){
            for(Slider s : _frameSliders) s._bar.cull(true);
        } else {
            Vector[] axes = new Vector[]{_frame.displacement(new Vector(1,0,0)),
                     _frame.displacement(new Vector(0,1,0)),
                    _frame.displacement(new Vector(0,0,1))};
            for(int i = 0; i < 3; i++){
                _frameSliders.get(i)._bar.cull(false);
                _frameSliders.get(i).setValue(_twist(_frame.orientation(), axes[i]).angle());
            }
        }
    }


    protected Quaternion _twist(Quaternion quaternion, Vector axis){
        Vector quat = new Vector(quaternion._quaternion[0],quaternion._quaternion[1], quaternion._quaternion[2]);
        quat = Vector.projectVectorOnAxis(quat, axis );
        return new Quaternion(quat.x(), quat.y(), quat.z(), quaternion._quaternion[3]);
    }

    protected void _setOrientation(Vector axis, float value){
        Vector local = _frame.displacement(axis);
        Quaternion orientation = _frame.orientation();
        Quaternion twist = _twist(orientation, local);
        Quaternion newOrientation = Quaternion.multiply(orientation, twist.inverse());
        newOrientation.multiply(new Quaternion(local , value));
        _frame.setOrientation(newOrientation);
    }

}
