package ik.interactive;

import frames.core.Frame;
import frames.core.constraint.Constraint;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PConstants;
import processing.core.PGraphics;

public class Slider {
    protected Vector _position;
    protected float _width, _height;
    protected float _detail;
    protected Scene _scene;
    protected String _name;
    protected Frame _bar;
    protected Shape _pointer;
    protected float _min = 0, _max = 100;
    protected float _offset, _bar_width, _text_width;

    public Slider(Scene scene, String name, Vector position, float width, float height){
        _scene = scene;
        _name = name;
        _position = position;
        _width = width;
        _height = height;
        _text_width = (float) Math.ceil(_scene.pApplet().textWidth(_name + " ")*2*_scene.boundaryWidthHeight()[0]/_scene.width());
        _bar_width = _width - _text_width;
        _detail = 20;
        createSlider();
    }

    public void setMin(float min){
        _min = min;
    }

    public void setMax(float max){
        _max = max;
    }

    public void setRange(float min, float max){
        _min = min;
        _max = max;
    }


    public void setValue(float value){
        System.out.println("val : " + value);
        float posX = ((value - _min)/(_max - _min) * _bar_width) + _offset;
        System.out.println("posX : " + posX);
        System.out.println("bw : " + _bar_width);
        _pointer.setTranslation(posX,_pointer.translation().y());
    }

    public float value(){
        return ((_pointer.translation().x() - _offset)/_bar_width )* (_max - _min) + _min;
    }

    protected void createSlider(){
        float detail = _bar_width/_detail;
        _bar = new Frame(_scene){
            @Override
            public void visit() {
                PGraphics pGraphics = _scene.frontBuffer();
                pGraphics.text(_name + " ", 0, 0);
                pGraphics.rectMode(PConstants.CENTER);
                pGraphics.stroke(100);
                pGraphics.fill(100);
                pGraphics.pushStyle();
                pGraphics.rect(_text_width + _bar_width/2.f, -0.8f*_height/2.f, _bar_width , _height*0.8f);
                pGraphics.popStyle();
            }
        };

        _bar.setPosition(_position);
        _bar.setPrecisionThreshold(0);
        _pointer = new Shape(_bar){
            @Override
            public void setGraphics(PGraphics pGraphics) {
                pGraphics.pushStyle();
                pGraphics.rectMode(PConstants.CENTER);
                pGraphics.stroke(255,0,0);
                pGraphics.fill(255,0,0);
                pGraphics.rect(0,0, detail, _height*0.8f);
                pGraphics.popStyle();
            }
            @Override
            public void interact(Object... objects){
                _scene.translate();
                pointerInteract(objects);
            }
        };
        Vector v = new Vector(_text_width + detail/2.f,-0.8f*_height/2.f);
        _pointer.setTranslation(v);
        _pointer.setPrecision(Frame.Precision.ADAPTIVE);
        _pointer.setPrecisionThreshold(detail);

        _offset = _text_width + detail/2.f;
        //Pointer is constrained to X-Axis
        _pointer.setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Frame frame) {
                float delta = translation.x();

                if(frame.translation().x() + delta > _bar_width + _offset){
                    delta = _bar_width - frame.translation().x() + _offset;
                }
                if(frame.translation().x() + delta < _offset){
                    delta = -frame.translation().x() + _offset;
                }
                return new Vector(delta, 0,0);
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
                return new Quaternion();
            }
        });
    }

    public void pointerInteract(Object... object){
    }
}
