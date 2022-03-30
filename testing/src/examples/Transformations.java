package examples;

import nub.core.Node;
import nub.core.Scene;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;

import java.nio.file.Paths;

public class Transformations extends PApplet {
  Node eye;
  Scene scene;
  boolean auto_rotate = false;
  String mode = "Fill";
  String imgPath;
  PImage img;
  Matrix mat;

  public void settings() {
    size(800, 600, P3D);
  }

  public void setup() {
    imgPath = Paths.get("testing/data/texture/lachoy.jpg").toAbsolutePath().toString();
    //depthPath = Paths.get("testing/data/depth/depth_nonlinear.glsl").toAbsolutePath().toString();
    //depthPath = Paths.get("testing/data/depth/depth_pack.glsl").toAbsolutePath().toString();
    img = loadImage(imgPath);
    scene = new Scene(this);
    eye = new Node();
    eye.setWorldPosition(0,0,700);
    scene.setEye(eye);
    //ortho(-width/2, width/2, -height/2, height/2);

    //frustum([left], [right], [bottom], [top], [near], [far]);
    //frustum(-150.1f, 200.2f, -90.3f,      70.4f, 1.5f, 5000.6f);

    //float eyeZ = ((float)height / 2) / tan(PI / 6);
    //perspective(PI / 3.0f, width / height, eyeZ / 10.0f, eyeZ * 10.0f);
    //perspective(PI / 3, (float)width / (float)height, 2.1f, 50.2f);

//ortho([left], [right], [bottom], [top], [near], [far])
    //ortho(-150.1f, 200.2f, -90.3f,      70.4f, 1.5f, 5000.6f);
    //console.log('fov', this._renderer._fov());
    println("near", scene.near());
    println("far", scene.far());
    println("left", scene.left());
    println("right", scene.right());
    println("bottom", scene.bottom());
    println("top", scene.top());
    println("fov", scene.fov());
    println("hfov", scene.hfov());
  }

  public void draw() {
    background(120);
    if (auto_rotate) {
      rotateZ(frameCount * 0.01f);
      rotateX(frameCount * 0.01f);
      rotateY(frameCount * 0.01f);
    }
    lights();
    scale(10);
    strokeWeight(1);
    switch (mode) {
      case "Fill":
        fill(255, 0, 0);
        break;
      case "Wiredframe":
        noFill();
        stroke(0, 255, 255);
        break;
      default:
        texture(img);
    }
    box(30);
    push();
    translate(0, 0, 20);
    fill(0, 0, 255);
    box(5);
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
    if (key == 's') {
      // /*
      mat = new Matrix(-0.7053518295288086f,
              -0.07230858504772186f,
              -0.7051597833633423f,
              0,
              0.708856999874115f,
              -0.07091210782527924f,
              -0.7017785906791687f,
              0,
              0.0007402516785077751f,
              -0.9948582649230957f,
              0.10127443075180054f,
              0,
              -0.000011515710866660811f,
              0.0000021756632122560404f,
              -764.4110107421875f,
              1);
      // */
      /*
      mat = new Matrix(0.8276702761650085f,
              0.5575584769248962f,
              0.0639565959572792f,
              0.0f,
              -0.5222256779670715f,
              0.723417341709137f,
              0.4516056478023529f,
              0.0f,
              0.20552925765514374f,
              -0.4071803390979767f,
              0.8899223804473877f,
              0.0f,
              -132.32723999023438f,
              51.44283676147461f,
              -772.2044067382812f,
              1.0f);
      // */
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
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"examples.Transformations"});
  }
}
