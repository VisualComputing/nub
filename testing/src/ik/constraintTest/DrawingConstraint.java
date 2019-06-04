package ik.constraintTest;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PGraphics3D;

public class DrawingConstraint  extends PApplet {
    Scene sceneConstraint, sceneLR, sceneUD, focus;
    int w = 900;
    int h = 500;

    public void settings() {
        size(w, h, P3D);
    }

    public void setup() {
        sceneConstraint = new Scene(this, P3D, w/3, h);
        sceneConstraint.setType(Graph.Type.ORTHOGRAPHIC);
        sceneConstraint.fit(1);
        sceneLR = new Scene(this, P2D, w/3, h, w/3, 0);
        sceneLR.fit(1);
        sceneUD = new Scene(this, P2D, w/3, h, 2*w/3, 0);
        sceneUD.fit(1);

        createBaseControl(sceneUD, color(100,203,30));
    }

    public void draw() {
        handleMouse();
        drawScene(sceneConstraint);
        drawScene(sceneLR);
        drawScene(sceneUD);
    }

    public void drawScene(Scene scene){
        scene.beginDraw();
        scene.context().background(0);
        scene.drawAxes();
        scene.render();
        scene.beginHUD();
        scene.context().pushStyle();
        scene.context().noFill();
        scene.context().stroke(255);
        scene.context().strokeWeight(3);
        scene.context().rect(0,0,sceneConstraint.context().width, sceneConstraint.context().height);
        scene.context().popStyle();
        scene.endHUD();
        scene.endDraw();
        scene.display();
    }

    public void createBaseControl(final Scene scene, final int color){
        Node ellipse = new Node(scene){
            int _color = color;
            float _left = 80, _right = 80, _up = 80, _down = 80;
            float _pleft = 80, _pright = 80, _pup = 80, _pdown = 80;

            Vector _initial, _end;

            @Override
            public void graphics(PGraphics pg) {
                pg.pushStyle();
                //Draw base according to each radius
                pg.fill(_color, scene.trackedNode() == this ? 255 : 100);
                pg.noStroke();
                drawCone(pg, 64, 0,0,0, _left, _up, _right, _down, false);
                //draw semi-axes
                pg.fill(255,0,0);
                pg.stroke(255,0,0);
                pg.strokeWeight(3);
                pg.line(0,0, -_left, 0);
                pg.line(0,0, _right, 0);
                pg.ellipse(-_left,0, 3,3);
                pg.ellipse(_right,0, 3,3);

                pg.fill(0,255,0);
                pg.stroke(0,255,0);
                pg.line(0,0, 0, _up);
                pg.line(0,0, 0, -_down);
                pg.ellipse(0, _up, 3,3);
                pg.ellipse(0,-_down, 3,3);

                pg.fill(255);
                pg.stroke(255);
                pg.ellipse(0,0, 3,3);

                if(_initial != null && _end != null){
                    pg.stroke(pg.color(255));
                    pg.line(_initial.x(), _initial.y(), _end.x(), _end.y());
                    pg.fill(pg.color(255,0,0));
                    pg.noStroke();
                    pg.ellipse(_initial.x(), _initial.y(), 5,5);
                    pg.ellipse(_end.x(), _end.y(), 5,5);
                    pg.fill(pg.color(255));
                }

                scene.beginHUD(pg);
                Vector l = scene.screenLocation(this.worldLocation(new Vector(-_left,0)));
                Vector r = scene.screenLocation(this.worldLocation(new Vector(_right,0)));
                Vector u = scene.screenLocation(this.worldLocation(new Vector(0, _up)));
                Vector d = scene.screenLocation(this.worldLocation(new Vector(0,-_down)));

                pg.fill(255);
                pg.textAlign(CENTER);
                pg.text("Left", l.x(), l.y() - 10);
                pg.text("Right", r.x(), r.y() - 10);
                pg.textAlign(LEFT);
                pg.text("Up", u.x() + 10, u.y());
                pg.text("Down", d.x() + 10, d.y());

                scene.endHUD(pg);
                pg.popStyle();
            }

            @Override
            public void interact(Object... gesture) {
                String command = (String) gesture[0];
                if(command.matches("Scale")){
                    if(_initial != null && _end != null) {
                        //scale
                        scale();
                    }
                    _initial = null;
                    _end = null;
                } else if(command.matches("OnScaling")){
                    if(_initial == null){
                        //Get initial point
                        _initial = scene.location((Vector) gesture[1], this);
                        _pleft = _left;
                        _pright = _right;
                        _pdown = _down;
                        _pup = _up;
                    }else{
                        //Get final point
                        _end = scene.location((Vector) gesture[1], this);
                        scale();
                    }
                } else if(command.matches("Clear")){
                    _initial = null;
                    _end = null;
                }
            }

            public void scale(){
                float horizontal = _end.x() - _initial.x();
                float vertical = _end.y() - _initial.y();
                //determine Which radius to scale
                if(_initial.x() > 0){
                    //Scale right radius
                    _right = _pright + horizontal;
                    //Clamp
                    _right = max(min(scene.radius(), _right), 5);
                }else{
                    _left = _pleft - horizontal;
                    //Clamp
                    _left = max(min(scene.radius(), _left), 5);
                }
                if(_initial.y() > 0){
                    //Scale right radius
                    _up = _pup + vertical;
                    //Clamp
                    _up = max(min(scene.radius(), _up), 5);
                }else{
                    _down = _pdown - vertical;
                    //Clamp
                    _down = max(min(scene.radius(), _down), 5);
                }
            }
        };
        ellipse.setPickingThreshold(0);
        ellipse.setHighlighting(0);
    }


    public void drawCone(PGraphics pGraphics, int detail, float x, float y, float height, float left_radius, float up_radius, float right_radius, float down_radius, boolean is3D) {
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
        if(is3D) pGraphics.vertex(0, 0, 0);
        else pGraphics.vertex(0, 0);
        for (int i = 0; i <= detail; i++) {
            if(is3D) pGraphics.vertex(unitConeX[i], unitConeY[i], height);
            else pGraphics.vertex(unitConeX[i], unitConeY[i]);
        }
        pGraphics.endShape();
        pGraphics.popMatrix();
        pGraphics.popStyle();
    }


    public void handleMouse() {
        Scene prev = focus;
        focus = mouseX < w / 3 ? sceneConstraint : mouseX < 2 * w / 3 ? sceneLR : sceneUD;
        if(prev != focus && prev != null){
            prev.defaultNode().interact("Clear");
            focus.defaultNode().interact("Clear");
        }

    }

    public void mouseMoved() {
        focus.cast();
    }

    public void mouseDragged() {
        if(focus == sceneUD) {
            focus.defaultNode().interact("OnScaling", new Vector(focus.mouse().x(), focus.mouse().y()));
            return;
        }
        if (mouseButton == LEFT)
            focus.spin();
        else if (mouseButton == RIGHT){
                focus.translate();
        }
        else
            focus.moveForward(mouseX - pmouseX);
    }

    public void mouseReleased(){
        if(focus == sceneUD) {
            focus.defaultNode().interact("Scale");
            return;
        }
    }

    public void mouseWheel(MouseEvent event) {
        focus.scale(event.getCount() * 20);
        //focus.zoom(event.getCount() * 50);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 2)
            if (event.getButton() == LEFT)
                focus.focus();
            else
                focus.align();
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.constraintTest.DrawingConstraint"});
    }

}


