package ik.interactiveSkeleton;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.Interpolator;
import frames.core.constraint.AxisPlaneConstraint;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.FixedConstraint;
import frames.core.constraint.LocalConstraint;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 8/09/18.
 */
public class InteractiveSpider extends PApplet {

    /*
    * Ideas taken from:
    * - https://www.youtube.com/watch?v=GtHzpX0FCFY
    * - https://codepen.io/DonKarlssonSan/post/particles-in-simplex-noise-flow-field
    * */

    // It seems slow, probably cause target interpolation

    public static class Spider{
        float[] velocity = new float[]{0.02f, 0.2f}; //save direction and angle

        Frame shape;
        PShape pshape;
        Interpolator[] interpolator;

        public Spider(Scene scene){
            this(scene, 7, 5, 14, 6);
        }

        public Spider(Scene scene, float bodyWidth, float bodyHeight, float bodyLength, int legs){
            pshape = scene.frontBuffer().createShape(BOX, bodyWidth, bodyHeight, bodyLength);
            float r = scene.pApplet().random(0, 255);
            float g = scene.pApplet().random(0, 255);
            float b = scene.pApplet().random(0, 255);
            pshape.setFill(scene.pApplet().color(r,g,b));
            pshape.setStroke(false);
            pshape.setTexture(scene.pApplet().loadImage(scene.pApplet().sketchPath() + "/testing/data/textures/spider.jpg"));
            pshape.setShininess(10.0f);

            float targetRadius = bodyWidth/10.f;

            shape = new Frame(scene, pshape);
            shape.setPickingThreshold(0);
            //create targets
            Frame[] targets = new Frame[legs];
            interpolator = new Interpolator[legs];

            for(int i = 0; i < legs; i++){
                targets[i] = new Frame(scene);
            }
            spiderSkeleton(shape, legs,bodyWidth,bodyHeight,bodyLength, targets, targetRadius);

            ArrayList<Frame> branch = (ArrayList) scene.branch(shape);
            Vector p = branch.get(branch.size()-1).position();
            Vector newPos = new Vector(shape.position().x(), -p.y(), shape.position().z());
            shape.setPosition(newPos);

            shape.rotate(new Quaternion(new Vector(0,1,0), PI/2));
            float x = scene.pApplet().random(-scene.radius(), scene.radius());
            float z = scene.pApplet().random(-scene.radius(), scene.radius());

            shape.translation().setX(x);
            shape.translation().setZ(z);

            shape.rotate(new Quaternion(new Vector(0,1,0), scene.pApplet().random(-PI, PI)));

            LocalConstraint constraint = new LocalConstraint();
            constraint.setTranslationConstraintType(LocalConstraint.Type.PLANE);
            constraint.setTranslationConstraintDirection(new Vector(0,1,0));
            shape.setConstraint(constraint);
        }

        //Motion Stuff
        public void move(float[] velocity){
            this.velocity[0] = velocity[0];
            this.velocity[1] = velocity[1];
            Quaternion rotY = new Quaternion(new Vector(0,1,0), this.velocity[1]);
            Vector desired = shape.displacement(rotY.rotate(new Vector(1,0,0)));
            Quaternion rot = new Quaternion(new Vector(0,0,1), desired);
            shape.rotate(rot);
            Vector translation = shape.worldDisplacement(new Vector(0,0,1));
            translation.normalize();
            translation.multiply(velocity[0]);
            shape.translate(translation);
        }

        public void keepInside(){
            if(shape.translation().x() > shape.graph().radius()) {
                shape.translation().setX(-shape.graph().radius() + shape.graph().radius()*0.1f);
            } else if(shape.translation().x() < -shape.graph().radius()) {
                shape.translation().setX(shape.graph().radius() - shape.graph().radius()*0.1f);
            } else if(shape.translation().z() > shape.graph().radius()) {
                shape.translation().setZ(-shape.graph().radius() + shape.graph().radius()*0.1f);
            } else if(shape.translation().z() < -shape.graph().radius()) {
                shape.translation().setZ(shape.graph().radius() - shape.graph().radius()*0.1f);
            }
        }

        //Skeleton and IK Stuff
        public Joint leg(int i, Frame reference, Vector upper, Vector middle, Vector lower, Frame target, boolean invert, float radius){
            Scene scene = (Scene) (shape.graph());
            Joint j1 = new Joint(scene, radius);
            j1.setDrawConstraint(false);
            j1.setReference(reference);
            j1.setPosition(reference.worldLocation(upper));
            Joint j2 = new Joint(scene, radius);
            j2.setDrawConstraint(false);
            j2.setReference(j1);
            j2.setPosition(reference.worldLocation(middle));
            Joint j21 = new Joint(scene, radius);
            j21.setDrawConstraint(false);
            j21.setReference(j2);
            Vector v = Vector.add(middle, Vector.multiply(Vector.subtract(lower, middle),0.5f));
            j21.setPosition(reference.worldLocation(v));
            Joint j3 = new Joint(scene, radius);
            j3.setDrawConstraint(false);
            j3.setReference(j21);
            j3.setPosition(reference.worldLocation(lower));
            j1.setRoot(true);
            addIk(i, j1, j3, target, invert);

            BallAndSocket ballAndSocket = new BallAndSocket(radians(20), radians(20));
            ballAndSocket.setRestRotation(j1.rotation().get(), Vector.orthogonalVector(j2.translation()), j2.translation());
            j1.setConstraint(ballAndSocket);

            ballAndSocket = new BallAndSocket(radians(20), radians(20));
            ballAndSocket.setRestRotation(j2.rotation().get(), Vector.orthogonalVector(j21.translation()), j21.translation());
            j2.setConstraint(ballAndSocket);

            ballAndSocket = new BallAndSocket(radians(20), radians(20));
            ballAndSocket.setRestRotation(j21.rotation().get(), Vector.orthogonalVector(j3.translation()), j3.translation());
            j21.setConstraint(ballAndSocket);

            j21.setConstraint(new FixedConstraint());
            return j1;
        }

        public void spiderSkeleton(Frame reference, int legs, float bodyWidth, float bodyHeigth, float bodyLength, Frame[] targets, float radius){
            if(legs < 4 ) legs = 4;
            legs = legs% 2 != 0 ? legs + 1 : legs;
            boolean invert = false;
            float factor = (float)(Math.random() + 0.5);

            for(int i = 0; i < legs/2; i++){
                //offset w.r.t length
                float z = i*(bodyLength*0.8f/(legs/2)) - bodyLength*0.3f;
                float upper_x = bodyWidth/2.f;
                float middle_x = upper_x + bodyWidth/2.f;
                float lower_x = middle_x + bodyWidth/2.f;

                float upper_y = 0;
                float middle_y = -bodyWidth * factor;
                float lower_y = -2*middle_y;

                leg(2*i, reference, new Vector(-upper_x, upper_y, z), new Vector(-middle_x, middle_y, z), new Vector(-lower_x, lower_y, z), targets[2*i], invert, radius);
                leg(2*i + 1, reference, new Vector(upper_x, upper_y, z), new Vector(middle_x, middle_y, z), new Vector(lower_x, lower_y, z), targets[2*i +1], !invert, radius);
                invert = !invert;
            }
        }

        public void addIk(int i, Frame root, Frame endEffector, Frame target, boolean invert){
            Scene scene = (Scene) (shape.graph());
            target.setReference(root.reference());
            target.setPosition(endEffector.position());
            interpolator[i] = legPath(target, Vector.distance(root.position(), endEffector.position())*0.1f, invert);
            Solver solver = scene.registerTreeSolver(root);
            solver.error = 0.01f;
            solver.timesPerFrame = 1f;
            scene.addIKTarget(endEffector, target);
        }

        public Interpolator legPath(Frame target, float amplitude, boolean invert) {
            Scene scene = (Scene) (shape.graph());
            Interpolator targetInterpolator = new Interpolator(target);
            targetInterpolator.setLoop();
            targetInterpolator.setSpeed(8.2f);
            // Create an initial path
            int nbKeyFrames = 8;
            float step = 2*PI / (nbKeyFrames - 1);
            int inv = invert ? 1 : -1;
            for (int i = 0; i < nbKeyFrames; i++) {
                float z = target.translation().z() + inv*amplitude*cos(step*i);
                float y = target.translation().y() - abs(amplitude*sin(step*i));
                if(i >= nbKeyFrames/2 - 1 && invert) y = target.translation().y();
                if(i <= nbKeyFrames/2 - 1 && !invert) y = target.translation().y();

                Frame iFrame = new Frame(scene);
                iFrame.setReference(target.reference());
                iFrame.setTranslation(new Vector(target.translation().x(), y, z));
                targetInterpolator.addKeyFrame(iFrame);
            }

            targetInterpolator.start();
            return targetInterpolator;
        }

        public void stop(){
            for(int i = 0; i < interpolator.length; i++){
                interpolator[i].stop();
            }
        }

        public void start(){
            for(int i = 0; i < interpolator.length; i++){
                interpolator[i].start();
            }
        }

    }

    Scene scene;
    Spider[] spiders = new Spider[20];
    Spider userSpider;
    float time = 0;
    float[][] magnitudeField = new float[30][30];
    float[][] angleField = new float[30][30];
    float[] userVelocity = new float[]{0,0};
    boolean left, right;
    float velocityMagnitude;
    float userTime = 0;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setFOV(PI / 3);
        Joint.deph = true;
        for(int i = 0; i < spiders.length; i++) {
            spiders[i] = new Spider(scene,
                    random(6, 14),
                    random(5, 8),
                    random(16,28),
                    2*(int) random(3, 6));
        }
        userSpider = new Spider(scene,
                random(6, 14),
                random(5, 8),
                random(16,28),
                2*(int) random(3, 6));

        userSpider.shape.setPosition(0,userSpider.shape.position().y(),0);
        userSpider.pshape.setFill(color(255,0,0));

        Frame cursor = new Frame(scene);
        cursor.shape(diamond(scene.radius()*0.2f, scene.radius()*0.1f, scene.radius()*0.1f));

        cursor.setReference(userSpider.shape);
        cursor.setTranslation(0, -scene.radius()*0.3f, 0);

        scene.eye().translate(75, -171, 163);
        scene.eye().rotate(new Quaternion(new Vector(0.88f, 0.44f, 0.13f), 0.75f));
    }

    public void draw() {
        background(0);
        ambientLight(102, 102, 102);
        lightSpecular(204, 204, 204);
        directionalLight(102, 102, 102, 0, 0, -1);
        specular(255, 255, 255);

        scene.render();
        for(int i = 0; i < spiders.length; i++) {
            spiders[i].keepInside();
            Vector p = spiders[i].shape.translation();
            //get cell
            int x = floor((p.x() + scene.radius())/ magnitudeField.length);
            int z = floor((p.z() + scene.radius())/ magnitudeField.length);
            x = (int) min(max(0, x), scene.radius());
            z = (int) min(max(0, z), scene.radius());
            spiders[i].move(new float[]{magnitudeField[z][x], angleField[z][x]});
        }
        userSpider.keepInside();
        userSpider.move(userVelocity);
        generateField();
        drawField();
        moveUser();
        time += 0.005f;
    }

    public void generateField(){
        for(int i = 0; i < magnitudeField.length; i++){
            for(int j = 0; j < magnitudeField[0].length; j++){
                magnitudeField[i][j] = 1.f * noise(i, j, time);
                angleField[i][j] = noise(i*0.1f + 4000, j*0.1f + 4000, time*5) * PI - 2 * PI;
            }
        }
    }

    public void drawField(){
        pushStyle();
        float time = this.time*10;
        stroke(255*noise(time),255*noise(time + 1000),255*noise(time + 5000), 100);
        strokeWeight(2);
        for(int i = 0; i < magnitudeField.length; i++){
            for(int j = 0; j < magnitudeField[0].length; j++){
                float x1 = (i * 1.f / (magnitudeField.length - 1)) * 2 * scene.radius() - scene.radius();
                float y1 = (j * 1.f / (magnitudeField[0].length - 1)) * 2 * scene.radius() - scene.radius();
                Vector v = new Quaternion(new Vector(0,1,0), angleField[i][j]).rotate(new Vector(magnitudeField[i][j],0,0));
                line(x1,0,y1, x1 + scene.radius()*0.1f*v.x(),0,y1 + scene.radius()*0.1f*v.z());
            }
        }
        popStyle();
    }

    public PShape diamond(float height, float width, float depth){

        PShape s = createShape();
        s.beginShape(TRIANGLES);
        s.vertex(0, height, 0);
        s.vertex(-width/2.f, 0, depth/2.f);
        s.vertex(width/2.f, 0, depth/2.f);

        s.vertex(0, height, 0);
        s.vertex(-width/2.f, 0, -depth/2.f);
        s.vertex(width/2.f, 0, -depth/2.f);

        s.vertex(0, height, 0);
        s.vertex(width/2.f, 0, -depth/2.f);
        s.vertex(width/2.f, 0, depth/2.f);

        s.vertex(0, height, 0);
        s.vertex(-width/2.f, 0, -depth/2.f);
        s.vertex(-width/2.f, 0, depth/2.f);

        s.vertex(-width/2.f, 0, -depth/2.f);
        s.vertex(-width/2.f, 0, depth/2.f);
        s.vertex(width/2.f, 0, depth/2.f);

        s.vertex(-width/2.f, 0, -depth/2.f);
        s.vertex(width/2.f, 0, depth/2.f);
        s.vertex(width/2.f, 0, -depth/2.f);

        s.endShape();

        s.setStroke(false);
        s.setFill(true);
        s.setFill(color(0,255,0));

        return s;
    }

    public void moveUser(){
        if(velocityMagnitude != 0){
            userTime+=0.1f*velocityMagnitude;
            userSpider.start();
            userVelocity[0] = velocityMagnitude + userTime;
            if(left) userVelocity[1] += velocityMagnitude > 0 ? 0.1f : -0.1f;
            if(right) userVelocity[1] += velocityMagnitude > 0 ? -0.1f : 0.1f;
        }else{
            userTime = 0;
            userVelocity[0] = 0;
            userSpider.stop();
        }
    }

    public void keyReleased(){
        if (key == CODED){
            if(keyCode == UP){
                velocityMagnitude = 0;
                userTime = 0;
            }
            if(keyCode == DOWN){
                velocityMagnitude = 0;
            }
            if(keyCode == LEFT){
                left = false;
            }
            if(keyCode == RIGHT){
                right = false;
            }
        }
    }

    public  void keyPressed(){
        if (key == CODED){
            if(keyCode == UP){
                velocityMagnitude = 1;
            }
            if(keyCode == DOWN){
                velocityMagnitude = -1;
            }
            if(keyCode == LEFT){
                left = true;
            }
            if(keyCode == RIGHT){
                right = true;
            }
        }
    }


    @Override
    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.spin();
        } else if (mouseButton == RIGHT) {
            scene.translate();
        } else {
            scene.scale(scene.mouseDX());
        }
    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 2)
            if (event.getButton() == LEFT)
                scene.focus();
            else
                scene.align();
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactiveSkeleton.InteractiveSpider"});
    }
}
