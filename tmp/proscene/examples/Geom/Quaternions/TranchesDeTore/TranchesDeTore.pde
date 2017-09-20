/**
 * TranchesDeTore version 0.1
 * fait par  Jacques Maire le 20/01/2016
 * Ce sketch illustre la formule V'=qVq* qui fait tourner un vecteur grace à un quaternion
 * les vecteurs V et V' sont assimilés aux symétries axiales d'axe V et V'.
 * la composée invrot -> sym(V) -> rot est égale à la symétrie d'axe rot(V)
 * La structure du programme est un décalque de l'exemple LUXO
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.constraint.*;

Scene scene;
Arbre arbre;

public void setup() {
  size(800, 800, P3D);
  scene = new Scene(this);  
  scene.setRadius(120);
  scene.showAll();
  scene.setGridVisualHint(false);
  scene.setAxesVisualHint(false);
  arbre = new Arbre(scene);
}

public void draw() {
  background(255,240,200);
  lights();

  directionalLight(251, 202, 226, 1, 1, 0);
  directionalLight(0, 0, 226, 0, 1, 1);
  directionalLight(0, 0, 226, 0, 1, -1);
  
  arbre.actualiser();
  scene.drawFrames(); 
 
}