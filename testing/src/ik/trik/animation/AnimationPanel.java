package ik.trik.animation;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.animation.Skeleton;
import nub.ik.animation.SkeletonAnimation;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PFont;

public class AnimationPanel extends Node {
    protected int _gray1, _gray2, _gray3, _gray4, _red, _blue1, _blue2, _green1, _green2, _yellow, _white;
    protected float _width, _height;
    protected int _colorSlider, _colorEmpty, _colorSliderHighlight;
    protected int _colorText;
    protected PFont _font36;
    protected Scene _scene;
    protected TimeLine _timeLine;
    protected SkeletonAnimation _skeletonAnimation;


    public AnimationPanel(Scene scene, Skeleton skeleton){
        super();
        enableTagging(false);
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

        //set panel dimension
        float rh = scene.radius(), rw = rh*scene.aspectRatio();
        translate(-rw, -rh,0);

        _width = 2 * rw;
        _height = 2 * rh;

        _scene = scene;
        _skeletonAnimation = new SkeletonAnimation(skeleton);

        _timeLine = new TimeLine(this, 200, 1000, 28);

        this.setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                //restriction on vertical translation
                return new Vector();
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                //Rotation is not allowed
                return new Quaternion();
            }
        });
    }

    public void savePosture(){
        _timeLine._current.savePosture();
    }

    public void play(){
        _skeletonAnimation.play();
    }

    public void toggleCurrentKeyPoint(){
        KeyPoint p = _timeLine._current;
        if(p._status.equals(KeyPoint.Status.ENABLED)) p.disable();
        else if(p._status.equals(KeyPoint.Status.DISABLED)) p.enable();
    }

    public void deletePostureAtKeyPoint(){
        KeyPoint p = _timeLine._current;
        p.deletePosture();
    }
}
