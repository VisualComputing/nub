class Torus {
  int id = totalShapes++, faces = randomFaces(), colour = randomColor();
  Node node;

  Torus() {
    node = new Node();
    node.enableHint(Node.TORUS, colour, faces);
    node.setInteraction(this::interact);
    node.setHUD(this::hud);
    scene.randomize(node);
  }

  void hud(PGraphics pg) {
    pg.fill(node.isTagged(scene) ? 0 : 255, node.isTagged(scene) ? 255 : 0, node.isTagged(scene) ? 0 : 255);
    pg.textFont(font36);
    pg.text(id, 0, 0);
  }

  void interact(Object[] gesture) {
    if (gesture.length == 0){
      colour = randomColor();
      node.configHint(Node.TORUS, colour, faces);
    }
    if (gesture.length == 1) {
      if (gesture[0] instanceof String) {
        if (((String) gesture[0]).matches("mas"))
          faces++;
        else if (((String) gesture[0]).matches("menos"))
          if (faces > 2)
            faces--;
      } else if (gesture[0] instanceof Integer) {
        int delta = (Integer) gesture[0];
        if (faces +  delta > 1)
          faces = faces + delta;
      }
      node.configHint(Node.TORUS, colour, faces);
    }
  }

  int randomColor() {
    return color(random(255), random(255), random(255), random(125, 255));
  }

  int randomFaces() {
    return (int) random(3, 15);
  }
}
