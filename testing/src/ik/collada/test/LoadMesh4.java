package ik.collada.test;

import nub.core.Graph;
import nub.ik.loader.collada.BlenderLoader;
import nub.ik.loader.collada.data.Model;
import nub.ik.skinning.GPULinearBlendSkinning;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

/**
 * Created by sebchaparr on 18/05/19.
 */
public class LoadMesh4 extends PApplet {
  //TODO : Update
  Scene scene;
  String path = "/testing/data/dae/";
  String dae = "t_rex.dae";
  String tex = null;
  Model model;
  GPULinearBlendSkinning skinning;

  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    randomSeed(14);
    //Joint.markers = true;
    //1. Create the scene
    textureMode(NORMAL);
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);

    //2. Load the model
    model = BlenderLoader.loadColladaModel(sketchPath() + path, dae, tex, scene, 3);

    //3. Setup scene
    scene.setRadius(model.mesh().getWidth() * 2);
    scene.eye().rotate(new Quaternion(new Vector(1, 0, 0), PI / 2));
    scene.eye().rotate(new Quaternion(new Vector(0, 0, 1), PI));
    scene.fit();

    //4. Relate mesh and skinning
    skinning = new GPULinearBlendSkinning(model.structure(), scene.context(), model.mesh());
  }

  public void draw() {
    background(0);
    lights();
    scene.drawAxes();
    //Render mesh
    skinning.render();
    //Render skeleton
    hint(DISABLE_DEPTH_TEST);
    scene.render();
    hint(ENABLE_DEPTH_TEST);
  }

  @Override
  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      scene.mouseSpin();
    } else if (mouseButton == RIGHT) {
      scene.mouseTranslate();
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
    PApplet.main(new String[]{"ik.collada.test.LoadMesh4"});
  }
}
