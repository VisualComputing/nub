package examples;

import nub.core.Node;
import nub.core.Scene;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;

import java.nio.file.Paths;

public class Female extends PApplet {
  Node eye;
  Scene scene;
  Matrix mat;
  boolean persp;

  public void settings() {
    size(600, 450, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    eye = new Node();
    eye.setWorldPosition(0,0,700);
    scene.setEye(eye);
    //ortho(-width/2, width/2, -height/2, height/2);

    //frustum([left], [right], [bottom], [top], [near], [far]);
    //frustum(-150.1f, 200.2f, -90.3f,      70.4f, 1.5f, 5000.6f);

    persp = true;
    float eyeZ = ((float)height / 2) / tan(PI / 6);
    perspective(PI / 3.0f, (float)width / (float)height, eyeZ / 10.0f, eyeZ * 10.0f);
    //perspective(PI / 3, (float)width / (float)height, 2.1f, 50.2f);

//ortho([left], [right], [bottom], [top], [near], [far])
    //ortho(-150.1f, 200.2f, -90.3f,      70.4f, 1.5f, 5000.6f);
    /*
    println("near", scene.near());
    println("far", scene.far());
    println("left", scene.left());
    println("right", scene.right());
    println("bottom", scene.bottom());
    println("top", scene.top());
    println("fov", scene.fov());
    println("hfov", scene.hfov());
    // */
  }

  public void draw() {
    background(120);
    push();
    noFill();
    strokeWeight(3);
    box(300);
    pop();
    push();
    //translate(50, 100, 0);
    fill(0, 0, 255);
    box(150);
    pop();
  }

  public void mouseMoved() {
    scene.tag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT) {
      scene.shift();
    }
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (!scene.isTagValid()) {
      scene.zoom(event.getCount() * 20);
    }
  }

  public void keyPressed() {
    if (key == 'p') {
      persp = !persp;
      if (persp) {
        float eyeZ = ((float)height / 2) / tan(PI / 6);
        perspective(PI / 3.0f, (float)width / (float)height, eyeZ / 10.0f, eyeZ * 10.0f);
      }
      else {
        ortho(-(float)width / 2, (float)width / 2, -(float)height / 2, (float)height / 2, 1, 5000);
      }
      //easycam.setState(state, 2000);
    }
    if (key == 's') {
      mat = new Matrix(-0.9772399663925171f,
              -0.18915009498596191f,
      0.09604335576295853f,
      0f,
      0.20929057896137238f,
              -0.7857396602630615f,
      0.5820744037628174f,
      0f,
              -0.03463435545563698f,
      0.5889273285865784f,
      0.8074434995651245f,
      0f,
              -165.21083068847656f,
              -39.76504135131836f,
              -702.18603515625f,
      1f);
      mat.invert();
      eye.fromWorldMatrix(mat);
    }
    if (key == 'v') {
      println(eye.view().toString());
    }
    if (key == 't') {
      Vector vector = new Vector(250, 200, 0.8f);
      Vector result = scene.screenLocation(vector);
      println(result.toString());
    }
    if (key == 'd') {
      Vector v = new Vector(15, -25, 70);
      Vector pv = new Vector(15, -25, 70);
      Vector r = eye.displacement(v);
      Vector w2e = scene.w2eDisplacement(pv);
      println(r.toString());
      println(w2e.toString());
      println(eye.worldDisplacement(r).toString());
      Vector e2w = scene.e2wDisplacement(w2e);
      println(e2w.toString());
    }
    if (key == 'u') {
      Vector v = new Vector(35, -55, 0.7f);
      Vector d = scene.displacement(v);
      println(d.toString());
      Vector w = scene.screenDisplacement(d);
      println(w.toString());
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"examples.Female"});
  }
}
