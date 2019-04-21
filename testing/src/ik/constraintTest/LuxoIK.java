package ik.constraintTest;

import frames.core.Frame;
import frames.core.constraint.*;
import frames.ik.ChainSolver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.TimingTask;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

public class LuxoIK extends PApplet {
    class Piece extends Frame {
        int mode;

        Piece(Scene scene) {
            super(scene);
            // set picking precision to the pixels of the frame projection
            setPickingThreshold(0);
        }

        void drawCone(PGraphics pg, float zMin, float zMax, float r1, float r2, int nbSub) {
            pg.translate(0.0f, 0.0f, zMin);
            Scene.drawCone(pg, nbSub, 0, 0, r1, r2, zMax - zMin);
            pg.translate(0.0f, 0.0f, -zMin);
        }

        @Override
        public boolean graphics(PGraphics pGraphics) {
            switch (mode) {
                case 1:
                    pGraphics.fill(isTracked() ? 255 : 0, 0, 255);
                    drawCone(pGraphics, 0, 3, 15, 15, 30);
                    drawCone(pGraphics, 3, 5, 15, 13, 30);
                    drawCone(pGraphics, 5, 7, 13, 1, 30);
                    drawCone(pGraphics, 7, 9, 1, 1, 10);
                    break;
                case 2:
                    pGraphics.pushMatrix();
                    pGraphics.rotate(HALF_PI, 0, 1, 0);
                    drawCone(pGraphics, -5, 5, 2, 2, 20);
                    pGraphics.popMatrix();

                    pGraphics.translate(2, 0, 0);
                    drawCone(pGraphics, 0, 50, 1, 1, 10);
                    pGraphics.translate(-4, 0, 0);
                    drawCone(pGraphics, 0, 50, 1, 1, 10);
                    pGraphics.translate(2, 0, 0);
                    break;
                case 3:
                    pGraphics.fill(0, 255, isTracked() ? 0 : 255);
                    drawCone(pGraphics, -2, 6, 4, 4, 30);
                    drawCone(pGraphics, 6, 15, 4, 17, 30);
                    drawCone(pGraphics, 15, 17, 17, 17, 30);
                    pGraphics.spotLight(155, 255, 255, 0, 0, 0, 0, 0, 1, THIRD_PI, 1);
                    break;
            }
            return true;
        }
    }


    class Lamp implements PConstants {
        Scene scene;
        Piece[] pieces;

        Lamp(Scene s) {
            scene = s;
            pieces = new Piece[4];

            for (int i = 0; i < 4; ++i) {
                pieces[i] = new Piece(scene);
                frame(i).setReference(i > 0 ? pieces[i - 1] : null);
                frame(i).setHighlighting(Frame.Highlighting.FRONT);
            }

            // Initialize frames
            frame(1).setTranslation(0f, 0f, 8f); // Base height
            frame(2).setTranslation(0, 0, 50);  // Arm length
            frame(3).setTranslation(0, 0, 50);  // Arm length

            frame(1).setRotation(new Quaternion(new Vector(1.0f, 0.0f, 0.0f), 0.6f));
            frame(2).setRotation(new Quaternion(new Vector(1.0f, 0.0f, 0.0f), -2.0f));
            frame(3).setRotation(new Quaternion(new Vector(1.0f, -0.3f, 0.0f), -1.7f));

            // Set frame graphics modes
            frame(0).mode = 1;
            frame(1).mode = 2;
            frame(2).mode = 2;
            frame(3).mode = 3;

            // Set frame constraints
            Hinge base = new Hinge(radians(180), radians(180), frame(0).rotation().get(), frame(1).translation(), new Vector(0,0,1));
            frame(0).setConstraint(base);

            Hinge h1 = new Hinge(radians(100), radians(100), frame(1).rotation().get(), frame(2).translation(), new Vector(1,0,0));
            Hinge h2 = new Hinge(radians(100), radians(100), frame(2).rotation().get(), frame(3).translation(), new Vector(1,0,0));

            frame(1).setConstraint(h1);
            frame(2).setConstraint(h2);

            LocalConstraint headConstraint = new LocalConstraint();
            headConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vector(0.0f, 0.0f, 0.0f));
            frame(3).setConstraint(new FixedConstraint());
        }

        Piece frame(int i) {
            return pieces[i];
        }
    }

    Scene scene;
    Lamp lamp;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setRadius(100);
        scene.fit(1);

        PShape redBall = createShape(SPHERE, 10);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        Frame target = new Frame(scene, redBall);
        target.setPickingThreshold(0);

        lamp = new Lamp(scene);
        target.setPosition(lamp.frame(3).position());

        ChainSolver solver = new ChainSolver( (ArrayList<? extends Frame>) scene.branch(lamp.frame(0)));
        solver.setTarget(target);
        solver.setKeepDirection(true);
        solver.setFixTwisting(true);
        solver.error = 3;

        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                solver.solve();
            }
        };
        scene.registerTask(task);
        task.run(40);

        //scene.registerTreeSolver(lamp.frame(0));

        //scene.addIKTarget(lamp.frame(3), target);
    }

    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        //draw the lamp
        scene.render();

        //draw the ground
        noStroke();
        fill(120, 120, 120);
        float nbPatches = 100;
        normal(0.0f, 0.0f, 1.0f);
        for (int j = 0; j < nbPatches; ++j) {
            beginShape(QUAD_STRIP);
            for (int i = 0; i <= nbPatches; ++i) {
                vertex((200 * (float) i / nbPatches - 100), (200 * j / nbPatches - 100));
                vertex((200 * (float) i / nbPatches - 100), (200 * (float) (j + 1) / nbPatches - 100));
            }
            endShape();
        }
    }

    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT)
            scene.spin();
        else if (mouseButton == RIGHT)
            scene.translate();
        else
            scene.scale(mouseX - pmouseX);
    }

    public void mouseWheel(MouseEvent event) {
        scene.moveForward(event.getCount() * 20);
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.constraintTest.LuxoIK"});
    }
}
