package ik.trik.expressive;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.solver.trik.heuristic.FinalHeuristic;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PFont;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

public class DelegationPanel extends Node {
    protected int _gray1, _gray2, _gray3, _gray4, _red, _blue1, _blue2, _green1, _green2, _yellow, _white;
    protected float _width, _height, _sliderHeight, _sliderWidth;
    protected int _colorSlider, _colorEmpty, _colorSliderHighlight;
    protected int _colorText;
    protected PFont _font36;

    protected SimpleTRIK _solver;
    protected List<Slider> _sliders = new ArrayList<Slider>();
    protected Scene _scene;


    public DelegationPanel(Scene scene, SimpleTRIK solver){
        super();
        enableTagging(false);
        _scene = scene;
        _gray1 = scene.pApplet().color(82,82,82); _gray2 = scene.pApplet().color(65,65,65); _gray3 = scene.pApplet().color(49,49,49); _gray4 = scene.pApplet().color(179,179,179);
        _red = scene.pApplet().color(202,62,71); _blue1 = scene.pApplet().color(23,34,59); _blue2 = scene.pApplet().color(38,56,89); _green1 = scene.pApplet().color(0,129,138);
        _yellow = scene.pApplet().color(249,210,118);
        _white = scene.pApplet().color(240,236,226);
        _green2 = scene.pApplet().color(33,152,151);

        _colorSlider  = _blue1;
        _colorEmpty = _gray1;
        _colorSliderHighlight = _green1;
        _colorText = _white;
        _font36 = scene.pApplet().createFont("Arial", 48, true);//loadFont("FreeSans-36.vlw");


        _solver = solver;

        //set panel dimension
        float rh = scene.radius(), rw = rh*scene.aspectRatio();
        translate(-rw, -rh,0);

        _width = 2 * rw;
        _height = 2 * rh;

        int n = solver.context().chain().size() - 1;

        _sliderWidth = _width / n;
        _sliderHeight = _height * 0.9f;

        _addSliders(n);

        this.setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                //No restriction on translation
                return translation;
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                //Rotation is not allowed
                return new Quaternion();
            }
        });
    }


    protected void _addSliders(int num){
        for(int i = 0; i < num; i++) {
            final int idx = i;
            Vector t = new Vector(_sliderWidth * idx, _sliderHeight);
            float value = _solver.context().delegationAtJoint(idx);
            _sliders.add(new Slider(this, i, t, value) {
                @Override
                public void onValueChanged() {
                    updateJoint(idx);
                }
            });
        }
    }

    protected void updateJoint(int idx){
        _solver.context().setDelegationAtJoint(idx, _sliders.get(idx)._value);
    }

    protected void updateSliders(){
        for(int i = 0; i < _sliders.size(); i++){
            _sliders.get(i).setValue(_solver.context().delegationAtJoint(i));
        }
    }

    @Override
    public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(_gray1);
        pg.stroke(_gray1);
        pg.rect(0,0,_width,_height);
        pg.popStyle();
    }


}
