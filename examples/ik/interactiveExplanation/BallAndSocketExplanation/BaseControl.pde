static class BaseControl extends Node{
    int colour;
    float left = 80, right = 80, up = 80, down = 80;
    float pleft = 80, pright = 80, pup = 80, pdown = 80;
    Vector initial, end;
    boolean modified = false;
    float max;
    float max_tan;
    float height;

    BaseControl(Graph graph, int colour){
        super(graph);
        this.colour = colour;
        setPickingThreshold(0);
        setHighlighting(0);
        max = graph().radius() * 0.8f;
        max_tan = tan(radians(70));
        height = max / max_tan;
    }

    boolean modified(){
        return modified;
    }

    void setModified(boolean modified){
        this.modified = modified;
    }

    void update(float tl, float tr, float tu, float td){
        left = pleft = height * tan(tl);
        right = pright = height * tan(tr);
        up = pup = height * tan(tu);
        down = pdown = height * tan(td);
    }

    float toAngle(float l){
        return atan2(l, height);
    }

    float left(){
        return left;
    }

    float right(){
        return right;
    }

    float up(){
        return up;
    }

    float down(){
        return down;
    }

    @Override
    void graphics(PGraphics pg) {
        Scene scene = (Scene) graph();
        pg.pushStyle();
        //Draw base according to each radius
        pg.fill(colour, graph().trackedNode() == this ? 255 : 100);
        pg.noStroke();
        drawConeBase(pg, 64, 0,0,0, left, up, right, down);
        //draw semi-axes
        pg.fill(255,0,0);
        pg.stroke(255,0,0);
        pg.strokeWeight(3);
        pg.line(0,0, -left, 0);
        pg.line(0,0, right, 0);
        pg.ellipse(-left,0, 3,3);
        pg.ellipse(right,0, 3,3);

        pg.fill(0,255,0);
        pg.stroke(0,255,0);
        pg.line(0,0, 0, up);
        pg.line(0,0, 0, -down);
        pg.ellipse(0, up, 3,3);
        pg.ellipse(0,-down, 3,3);

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

        scene.beginHUD(pg);
        Vector l = scene.screenLocation(this.worldLocation(new Vector(-left,0)));
        Vector r = scene.screenLocation(this.worldLocation(new Vector(right,0)));
        Vector u = scene.screenLocation(this.worldLocation(new Vector(0, up)));
        Vector d = scene.screenLocation(this.worldLocation(new Vector(0,-down)));

        pg.fill(255);
        pg.textFont(font, 16);
        pg.textAlign(RIGHT, CENTER);
        pg.text("Left", l.x() - 5, l.y());
        pg.textAlign(LEFT, CENTER);
        pg.text("Right", r.x() + 5, r.y());
        pg.textAlign(CENTER, TOP);
        pg.text("Up", u.x(), u.y());
        pg.textAlign(CENTER, BOTTOM);
        pg.text("Down", d.x(), d.y());

        scene.endHUD(pg);
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
                pleft = left;
                pright = right;
                pdown = down;
                pup = up;
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
        float horizontal = end.x() - initial.x();
        float vertical = end.y() - initial.y();
        //determine Which radius to scale
        if(initial.x() > 0){
            //Scale right radius
            right = pright + horizontal;
            //Clamp
            right = max(min(graph().radius(), right), 5);
        }else{
            left = pleft - horizontal;
            //Clamp
            left = max(min(graph().radius(), left), 5);
        }
        if(initial.y() > 0){
            //Scale right radius
            up = pup + vertical;
            //Clamp
            up = max(min(graph().radius(), up), 5);
        }else{
            down = pdown - vertical;
            //Clamp
            down = max(min(graph().radius(), down), 5);
        }
    }
    
    void drawConeBase(PGraphics pGraphics, int detail, float x, float y, float height, float left_radius, float up_radius, float right_radius, float down_radius){
        pGraphics.pushStyle();
        detail = detail % 4 != 0 ? detail + ( 4 - detail % 4) : detail;
        detail = Math.min(64, detail);
    
        float unitConeX[] = new float[detail + 1];
        float unitConeY[] = new float[detail + 1];
        int d = detail/4;
    
        for (int i = 0; i <= d; i++) {
            float a1 = (PApplet.PI * i) / (2.f * d);
            unitConeX[i] = right_radius * (float) Math.cos(a1);
            unitConeY[i] = up_radius * (float) Math.sin(a1);
            unitConeX[i + d] = left_radius * (float) Math.cos(a1 + PApplet.HALF_PI);
            unitConeY[i + d] = up_radius * (float) Math.sin(a1 + PApplet.HALF_PI);
            unitConeX[i + 2*d] = left_radius * (float) Math.cos(a1 + PApplet.PI);
            unitConeY[i + 2*d] = down_radius * (float) Math.sin(a1 + PApplet.PI);
            unitConeX[i + 3*d] = right_radius * (float) Math.cos(a1 + 3*PApplet.PI/2);
            unitConeY[i + 3*d] = down_radius * (float) Math.sin(a1 + 3*PApplet.PI/2);
        }
        pGraphics.pushMatrix();
        pGraphics.translate(x, y);
        pGraphics.beginShape(PApplet.TRIANGLE_FAN);
        for (int i = 0; i <= detail; i++) {
            pGraphics.vertex(unitConeX[i], unitConeY[i], height);
            pGraphics.vertex(unitConeX[i], unitConeY[i]);
        }
        pGraphics.endShape();
        pGraphics.popMatrix();
        pGraphics.popStyle();
      
    }
}
