/**
 * Scramble.
 * by Alejandro Duarte (alejandro.d.a@gmail.com) and Jean Pierre Charalambos
 *
 * This example implements an advanced puzzle game. The rules are straightforward
 * and can easily be understood once one begins to play it. The code was commented
 * thoroughly in the hope to make its implementation easy to understand as well.
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.geom.*;
import remixlab.bias.event.*;

Scene scene;
Board board;

PFont font1, font2;

void setup() {
  size(800, 500, P3D); // window size
  scene = new Scene(this); // create a Scene instance
  scene.setAxesVisualHint(false); // hide axis
  scene.setGridVisualHint(false); // hide grid
  board = new Board(3, null); // create a new 3x3 board
  scene.camera().setPosition(new Vec(-20, 100, 230)); // move the camera
  scene.camera().lookAt(new Vec(0, 0, 0)); // make the camera look at the center of the board
  //scene.mouseAgent().setClickBinding(Target.FRAME, LEFT, 1, ClickAction.CUSTOM);
  font1 = loadFont("FreeSans-16.vlw");
  font2 = loadFont("FreeSans-36.vlw");
}

void draw() {
  background(0);
  lights(); // lights on
  // create some lights to make the board look cool
  directionalLight(50, 50, 50, ((Quat)scene.camera().orientation()).x() - scene.camera().position().x(), ((Quat)scene.camera().orientation()).y() - scene.camera().position().y(), ((Quat)scene.camera().orientation()).z() - scene.camera().position().z());
  spotLight(150, 150, 150, scene.camera().position().x(), scene.camera().position().y(), scene.camera().position().z(), 0, 0, -1, 1, 20);
  spotLight(100, 100, 100, scene.camera().position().x(), scene.camera().position().y(), scene.camera().position().z(), 0, 0, 1, 1, 20);
  board.update();
  scene.drawFrames();
  scene.beginScreenDrawing();
  drawText();
  scene.endScreenDrawing();
}

public void drawText() {
  fill(#BBBBBB);
  textFont(font1);
  text("" + board.getMoves() + " moves.", 5, height - 20);
  text("Press 'i' to scramble, 'o' to order, 'p' to change mode, 'q' to increase size, 'w' to decrease size.", 5, height - 5);
  fill(#EEEEEE);
  text(board.isOrdered() && board.getMoves() > 0 ? "COMPLETED!" : "", 5, 28);
}

void keyPressed() {
  if (key == 'i' || key == 'I') {
    board.scramble();
  } else if (key == 'o' || key == 'O') {
    board.order();
  } else if (key == 'p' || key == 'P') {
    if (board.getImage() == null) {
      board.setImage(loadImage("image.png"));
    } else {
      board.setImage(null);
    }
    board.order();
  } else if (key == 'q') {
    if (board.getSize() < 5) {
      board.setSize(board.getSize() + 1);
    }
  } else if (key == 'w') {
    if (board.getSize() > 3) {
      board.setSize(board.getSize() - 1);
    }
  }
}