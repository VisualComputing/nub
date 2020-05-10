package ik.animation.eventVisualizer;

import nub.core.Node;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;

public class Label extends Node {

  protected String _text;
  protected int _colorLabel, _colorText;
  protected PFont _font36;
  protected Board _board;

  public Label(Board board) {
    super(board);
    _board = board;
    setTranslation(0, -_board._offsetHeight);
    _colorLabel = _board._colorBoardBackground;
    _colorText = _board._colorText;
    _font36 = board._font36;
  }

  public Label(EventCell cell) {
    super(cell);
    _board = cell._board;
    setTranslation(0, -_board._offsetHeight);
    _colorLabel = cell._colorCell;
    _colorText = cell._board._colorText;
    _font36 = _board._font36;
  }

  public void setText(String text) {
    _text = text;
  }

  public void setColorLabel(int col) {
    _colorLabel = col;
  }

  @Override
  public void graphics(PGraphics pg) {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(_colorLabel, 120);
    pg.rect(0, 0, pg.textWidth(_text), _board._offsetHeight);
    pg.noStroke();
    pg.fill(_colorText);
    pg.textFont(_font36);
    pg.textSize(12);
    pg.textAlign(PConstants.LEFT, PConstants.TOP);
    pg.text(_text, 0, 0);
    pg.popStyle();
  }
}
