class ThetaControl extends Node{
    int colour;
    float min = 20, max = 20;
    float pmin = 20, pmax = 20;
    boolean modified = false;

    Vector initial, end;
    String min_name, max_name;

    ThetaControl(Graph graph, int colour){
        super(graph);
        this.colour = colour;
        setPickingThreshold(0);
        setHighlighting(0);
    }

    float maxAngle(){
        return max;
    }

    float minAngle(){
        return min;
    }

    void setNames(String min, String max){
        this.min_name = min;
        this.max_name = max;
    }

    void update(float min, float max){
        this.min = this.pmin = min;
        this.max = this.pmax = max;
    }

    boolean modified(){
        return modified;
    }

    void setModified(boolean modified){
        this.modified = modified;
    }

    @Override
    void graphics(PGraphics pg) {
        pg.pushStyle();
        //Draw base according to each radius
        pg.fill(colour, graph().trackedNode() == this ? 255 : 100);
        pg.noStroke();
        //Draw arc
        ((Scene) graph()).drawArc(pg, graph().radius()*0.7f, -min , max, 30);
        //draw semi-axe
        pg.fill(255);
        pg.stroke(255);
        pg.strokeWeight(3);
        pg.line(0,0, graph().radius()*0.7f, 0);
        pg.ellipse(graph().radius()*0.7f,0, 3,3);

        pg.fill(255);
        pg.stroke(255);
        pg.ellipse(0,0, 3,3);

        if(initial != null && end != null){
            pg.stroke(255);
            pg.line(initial.x(), initial.y(), end.x(), end.y());
            pg.fill(255,0,0);
            pg.noStroke();
            pg.ellipse(initial.x(), initial.y(), 5,5);
            pg.ellipse(end.x(), end.y(), 5,5);
            pg.fill(255);
        }

        ((Scene) graph()).beginHUD(pg);
        Vector min_position = graph().screenLocation(new Vector(graph().radius()*0.7f * (float) Math.cos(-min), graph().radius()*0.7f * (float) Math.sin(-min)), this);
        Vector max_position = graph().screenLocation(new Vector(graph().radius()*0.7f * (float) Math.cos(max), graph().radius()*0.7f * (float) Math.sin(max)), this);
        pg.fill(255);
        pg.textAlign(LEFT, CENTER);
        pg.textFont(font, 16);
        pg.text("\u03B8 " + min_name, min_position.x() + 5, min_position.y() );
        pg.text("\u03B8 " + max_name, max_position.x() + 5, max_position.y() );
        ((Scene) graph()).endHUD(pg);
        pg.popStyle();
    }

    @Override
    void interact(Object... gesture) {
        String command = (String) gesture[0];
        if(command.matches("Scale")){
            if(initial != null && end != null) {
                //scale
                scale();
                modified = true;
            }
            initial = null;
            end = null;
        } else if(command.matches("OnScaling")){
            if(initial == null){
                //Get initial point
                initial = graph().location((Vector) gesture[1], this);
                pmin = min;
                pmax = max;
            }else{
                //Get final point
                end = graph().location((Vector) gesture[1], this);
                scale();
            }
        } else if(command.matches("Clear")){
            initial = null;
            end = null;
        }
    }

    void scale(){
        float angle = Vector.angleBetween(initial, end);
        angle *= Vector.cross(initial, end, null).dot(new Vector(0,0,1)) > 0 ? 1 : -1;

        //determine Which radius to scale
        if(initial.y() > 0){
            //Scale right radius
            max = pmax + angle;
            //Clamp
            max = max(min(radians(80), max), radians(5));
        }else{
            min = pmin - angle;
            //Clamp
            min = max(min(radians(80), min), radians(5));
        }
    }
}
