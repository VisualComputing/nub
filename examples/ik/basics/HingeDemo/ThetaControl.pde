class ThetaControl extends Node{
    int _color;
    float _min = 20, _max = 20;
    float _pmin = 20, _pmax = 20;
    boolean _modified = false;
    Scene _scene;


    Vector _initial, _end;
    String _min_name, _max_name;

    public ThetaControl(Scene scene, int col){
        super();
        _scene = scene;
        _color = col;
        setPickingThreshold(0);
        setHighlighting(0);
    }

    public float maxAngle(){
        return _max;
    }

    public float minAngle(){
        return _min;
    }

    public void setNames(String min, String max){
        _min_name = min;
        _max_name = max;
    }

    public void update(float min, float max){
        _min = _pmin = min;
        _max = _pmax = max;
    }

    public boolean modified(){
        return _modified;
    }

    public void setModified(boolean modified){
        _modified = modified;
    }

    @Override
    public void graphics(PGraphics pg) {
        pg.pushStyle();
        //Draw base according to each radius
        pg.fill(_color, _scene.node() == this ? 255 : 100);
        pg.noStroke();
        drawArc(pg, _scene.radius()*0.7f, -_min , _max, 30);
        //draw semi-axe
        pg.fill(255);
        pg.stroke(255);
        pg.strokeWeight(3);
        pg.line(0,0, _scene.radius()*0.7f, 0);
        pg.ellipse(_scene.radius()*0.7f,0, 3,3);

        pg.fill(255);
        pg.stroke(255);
        pg.ellipse(0,0, 3,3);

        if(_initial != null && _end != null){
            pg.stroke(255);
            pg.line(_initial.x(), _initial.y(), _end.x(), _end.y());
            pg.fill(255,0,0);
            pg.noStroke();
            pg.ellipse(_initial.x(), _initial.y(), 5,5);
            pg.ellipse(_end.x(), _end.y(), 5,5);
            pg.fill(255);
        }

        if(pg == _scene.context()) {
            _scene.beginHUD(pg);
            Vector min_position = _scene.screenLocation(new Vector(_scene.radius() * 0.7f * (float) Math.cos(-_min), _scene.radius() * 0.7f * (float) Math.sin(-_min)), this);
            Vector max_position = _scene.screenLocation(new Vector(_scene.radius() * 0.7f * (float) Math.cos(_max), _scene.radius() * 0.7f * (float) Math.sin(_max)), this);
            pg.fill(255);
            pg.textAlign(LEFT, CENTER);
            pg.textFont(font, 16);
            pg.text("\u03B8 " + _min_name, min_position.x() + 5, min_position.y());
            pg.text("\u03B8 " + _max_name, max_position.x() + 5, max_position.y());
            _scene.endHUD(pg);
            pg.popStyle();
        }
    }

    @Override
    public void interact(Object... gesture) {
        String command = (String) gesture[0];
        if(command.matches("Scale")){
            if(_initial != null && _end != null) {
                //scale
                scale();
                _modified = true;
            }
            _initial = null;
            _end = null;
        } else if(command.matches("OnScaling")){
            if(_initial == null){
                //Get initial point
                _initial = _scene.location((Vector) gesture[1], this);
                _pmin = _min;
                _pmax = _max;
            }else{
                //Get final point
                _end = _scene.location((Vector) gesture[1], this);
                scale();
            }
        } else if(command.matches("Clear")){
            _initial = null;
            _end = null;
        }
    }

    public void scale(){
        float angle = Vector.angleBetween(_initial, _end);
        angle *= Vector.cross(_initial, _end, null).dot(new Vector(0,0,1)) > 0 ? 1 : -1;

        //determine Which radius to scale
        if(_initial.y() > 0){
            //Scale right radius
            _max = _pmax + angle;
            //Clamp
            _max = max(min(radians(180), _max), radians(5));
        }else{
            _min = _pmin - angle;
            //Clamp
            _min = max(min(radians(180), _min), radians(5));
        }
    }
}


public static void drawArc(PGraphics pGraphics, float radius, float minAngle, float maxAngle, int detail) {
    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    if(pGraphics.is3D()) {
        pGraphics.vertex(0, 0, 0);
    }
    else{
        pGraphics.vertex(0, 0);
    }
    float step = (maxAngle - minAngle) / detail;
    for (float theta = minAngle; theta < maxAngle; theta += step)
        pGraphics.vertex(radius * (float) Math.cos(theta), radius * (float) Math.sin(theta));
    pGraphics.vertex(radius * (float) Math.cos(maxAngle), radius * (float) Math.sin(maxAngle));
    pGraphics.endShape(PApplet.CLOSE);
}
