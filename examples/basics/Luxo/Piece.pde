class Piece extends Node {
  int mode;

  void drawCone(PGraphics pg, float zMin, float zMax, float r1, float r2, int nbSub) {
    pg.translate(0, 0, zMin);
    Scene.drawCone(pg, nbSub, 0, 0, r1, r2, zMax - zMin);
    pg.translate(0, 0, -zMin);
  }

  @Override
  public void graphics(PGraphics pGraphics) {
    switch (mode) {
    case 1:
      pGraphics.fill(isTagged(scene) ? 255 : 0, 0, 255);
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
      pGraphics.fill(0, 255, isTagged(scene) ? 0 : 255);
      drawCone(pGraphics, -2, 6, 4, 4, 30);
      drawCone(pGraphics, 6, 15, 4, 17, 30);
      drawCone(pGraphics, 15, 17, 17, 17, 30);
      pGraphics.spotLight(155, 255, 255, 0, 0, 0, 0, 0, 1, THIRD_PI, 1);
      break;
    }
  }
}
