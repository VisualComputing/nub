/**
 *
 * Add a graphics handler to an InteractiveFrame to automatically pick
 * an object. The object is described in the graphics handler procedure.
 *
 * Press 'h' to toggle the mouse and keyboard navigation help.
 */

public class Arbre {
  Scene scene;
  InteractiveFrame [] reperes;
  Vec depart, arrivee;
  Quat quatQ;
  Arbre(Scene s) {
    scene =  s;
    reperes = new InteractiveFrame[5];
    // arborescence des framesr
    reperes[0] = new InteractiveFrame(scene);
    reperes[1] = new InteractiveFrame(scene, reperes[0]);
    reperes[2] = new InteractiveFrame(scene, reperes[0]);
    reperes[3] = new InteractiveFrame(scene, reperes[0]);
    reperes[4] = new InteractiveFrame(scene, reperes[3]);
    // Initialize frames

    for(int i = 0; i<5; i++)
      reperes[i].setHighlightingMode(InteractiveFrame.HighlightingMode.NONE);

    reperes[1].setTranslation(0, 0, 0); // cone depart
    reperes[2].setTranslation(0, 0, 0);  // cone arrivee
    reperes[3].setTranslation(0, 0, 0);  // les arcs du tore
    reperes[4].setTranslation(85, -35, 25);  // bras sur le depart
    reperes[0].setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f),0.0f));
    reperes[1].setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f), 0.6f));
    reperes[2].setRotation(new Quat(new Vec(1.0f, 1.0f, 0.0f), -1.0f));
    reperes[3].setRotation(new Quat(new Vec(1.0f, -0.3f, 0.0f), -1.7f));
    reperes[4].setRotation(new Quat(new Vec(0.0f, 0.0f, 1.0f), -0.0f));

    //graphics handers

    reperes[1].setShape(this, "drawConeDepart");
    reperes[2].setShape(this, "drawConeArrivee");
    reperes[3].setShape(this, "drawRepere3");
    reperes[4].setShape(this, "drawBras");

    // Set frame constraints
    WorldConstraint contrainteDeLaBase = new WorldConstraint();
    contrainteDeLaBase.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vec(0.0f, 0.0f, 1.0f));
    contrainteDeLaBase.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vec(0.0f, 0.0f, 1.0f));
    reperes[0].setConstraint(contrainteDeLaBase);

    LocalConstraint localTrans = new LocalConstraint();
    localTrans.setTranslationConstraint(AxisPlaneConstraint.Type.FREE, new Vec(0.0f, 0.0f, 0.0f));
    localTrans.setRotationConstraint   (AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
    reperes[4].setConstraint(localTrans);

    LocalConstraint rotule = new LocalConstraint();
    rotule.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
    reperes[3].setConstraint(rotule);
    reperes[1].setConstraint(rotule);
    reperes[2].setConstraint(rotule);
  }

  public void drawRepere3(PGraphics pg) {

    pg.fill(0, 0, 255);
    pg.stroke(0);
    drawCone(pg, -100, 100, 4, 4, 32);
    Vec tr=reperes[4].translation();

    pg.pushMatrix();
    pg.translate(tr.x(), -tr.y(), -tr.z());
    pg.box(10);
    pg.popMatrix();
    pg. pushMatrix();
    pg.rotateZ(quatQ.angle());
    pg.translate( tr.x(), tr.y(), tr.z());
    pg.box(10);
    pg.popMatrix();
    pg.pushMatrix();
    pg.rotateZ(quatQ.angle());
    pg.translate( tr.x(), -tr.y(), -tr.z());
    pg.box(10);
    pg.stroke(0);
    pg.strokeWeight(10);
    pg.line(0, 2.0*tr.y(), 2.0*tr.z(), 0, 0, 0 );
     pg.strokeWeight(1);
    pg.popMatrix();

    pg.fill(0, 255, 0, 50);
    pg.arc(0, 0, 200, 200, 0, quatQ.angle());
    float diag=sqrt(sq(tr.x())+sq(tr.y()));
    float alpha=atan2(tr.y(), tr.x());
    pg.pushMatrix();
    pg.translate(0, 0, tr.z());
    pg.arc(0, 0, diag*2.0, diag*2.0, alpha, alpha+ quatQ.angle());
    pg.popMatrix();
    pg.pushMatrix();
    pg.translate(0, 0, -tr.z());
    pg.arc(0, 0, diag*2.0, diag*2.0, -alpha, -alpha+ quatQ.angle());
    pg.popMatrix();
  }

  public void drawBras(PGraphics pg) {
    if ( scene.mouseAgent().inputGrabber() != reperes[4] ) fill(255, 0, 255);
    else fill(255, 255, 0);
    pg.noStroke();
    pg.sphere(10);
    Vec tr=reperes[4].translation();
    pg.stroke(0);
    pg.strokeWeight(10);
    pg.line(0, 0, 0, 0, -2.0* tr.y(), -2.0* tr.z());
    pg.strokeWeight(1);
  }
  public void drawConeArrivee(PGraphics pg) {
    if ( scene.mouseAgent().inputGrabber() != reperes[2] ) fill(255, 0, 0, 75);
    else fill(255, 255, 0);
    Vec tr=reperes[4].translation();
    float ray=sqrt(sq(tr.z())+sq(tr.y()));
    drawCone(pg, tr.x(), tr.x()+10, ray, ray, 32);
    drawCone(pg, 0, 100, 2, 2, 10);
  }

  public void drawConeDepart(PGraphics pg) {
    if ( scene.mouseAgent().inputGrabber() != reperes[1] ) fill(0, 0, 255, 75);
    else fill(255, 255, 0);
    // drawCone(pg, 0, 50, 0, 30, 32);
    Vec tr=reperes[4].translation();
    float ray=sqrt(sq(tr.z())+sq(tr.y()));
    drawCone(pg, tr.x(), tr.x()+10, ray, ray, 32);
    drawCone(pg, 0, 100, 2, 2, 10);
  }

  public void drawCylinder(PGraphics pg) {
    pg.pushMatrix();
    pg.rotate(HALF_PI, 0, 1, 0);
    drawCone(pg, -5, 5, 2, 2, 20);
    pg.popMatrix();
  }

  public void drawPivotArm(PGraphics pg) {
    drawCylinder(pg);
    Vec tr=reperes[4].translation();
  }

  public void drawCone(PGraphics pg, float zMin, float zMax, float r1, float r2, int nbSub) {
    pg.noStroke();
    pg.translate(0.0f, 0.0f, zMin);
    Scene.drawCone(pg, nbSub, 0, 0, r1, r2, zMax-zMin);
    pg.translate(0.0f, 0.0f, -zMin);
  }


  void actualiser() {
    depart= reperes[1].localInverseCoordinatesOf(new Vec(0, 0, 1));
    arrivee= reperes[2].localInverseCoordinatesOf(new Vec(0, 0, 1));
    quatQ=new Quat(depart.cross(arrivee), acos(depart.dot(arrivee)));
    //quatQ est le quaternion de la rotation qui transforme depart dans arrivee
    reperes[3].setZAxis(quatQ.axis());
    reperes[3].setXAxis(depart);
  }
}
