package ik.trik.animation;

import nub.core.Node;
import nub.primitives.Vector;
import processing.core.PGraphics;

public class TimeLine extends Node {
    protected AnimationPanel _panel;
    protected KeyPoint[] _points;
    protected KeyPoint _current;
    protected float _timeStep, _spaceStep;
    protected int _stamps;

    public TimeLine(AnimationPanel panel, float spaceStep, float timeStep, int n){
        super(panel);
        _panel = panel;
        _timeStep = timeStep;
        _spaceStep = spaceStep;
        _stamps = n;
        _points = new KeyPoint[_stamps];
        translate(new Vector(_panel._width * 0.1f, _panel._height * 0.5f));
        enableTagging(false);
        generateKeyPoints();
        _current = _points[0];
    }

    @Override
    public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.strokeWeight(5);
        pg.stroke(_panel._gray1);
        Vector end = location(_points[_stamps - 1]);
        pg.line(0, 0, end.x(), end.y());
        pg.popStyle();
    }

    protected void generateKeyPoints(){
        KeyPoint prev = null;
        for(int i = 0; i < _stamps; i++){
            KeyPoint p = new KeyPoint(this, _panel._width * 0.02f);
            p.translate(_spaceStep * i, 0,0);
            p._prev = prev;
            if(prev != null){
                prev._next = p;
            }
            prev  = p;
            _points[i] = p;
        }
    }

    public int getCurrentIdx(){
        int idx = 0;
        for(KeyPoint p : _points){
            if(p == _current){
                return idx;
            }
            idx++;
        }
        return -1;
    }
}
