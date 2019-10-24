package ik.animation.eventVisualizer;

import nub.core.Node;
import nub.primitives.Vector;
import processing.core.PConstants;
import processing.core.PGraphics;

public class EventCell extends Node {
    protected Board _board;
    protected String _name;
    protected Slot _executionDurationSlot, _renderingDurationSlot;
    protected int _row, _col = 0;
    protected int _colorCell;
    protected Label _label;

    public  EventCell(Board board, String name, int start, int executionDuration, int renderingDuration){
        super(board);
        _board = board;
        _name = name;
        Vector executionPos = new Vector(_board._cellWidth * 0.5f, _board._slotHeight * 1.5f);
        _executionDurationSlot = new Slot(this, executionPos, _board._colorExecutionSlot, executionDuration);
        Vector renderingPos = new Vector(_board._cellWidth * 0.5f, _board._slotHeight * 2.5f);
        _renderingDurationSlot = new Slot(this, renderingPos, _board._colorRenderingSlot, renderingDuration);
        _board._events.add(this);
        _col = start;
        _row = _board._firstAvailableRow(this, true);
        if(_row >= _board._rows)
            _board.addRows(_row - _board._rows + 1, false);
        setTranslation(_col * _board._cellWidth, _row * (_board._cellHeight + _board._offsetHeight) + _board._slotHeight + _board._offsetHeight);
        _board._eventMatrix[_row][_col] = this;
        _colorCell = _board._colorCell;
    }

    public EventCell(Board board, String name, int row, int col, int executionDuration, int renderingDuration){
        super(board);
        _board = board;
        _name = name;
        _row = row;
        _col = col;
        setTranslation(_col * _board._cellWidth, _row * _board._cellHeight + _board._slotHeight);
        _board._eventMatrix[row][col] = this;
        _board._events.add(this);
        Vector executionPos = new Vector(_board._cellWidth * 0.5f, _board._slotHeight * 1.5f);
        _executionDurationSlot = new Slot(this, executionPos, _board._colorExecutionSlot, executionDuration);
        Vector renderingPos = new Vector(_board._cellWidth * 0.5f, _board._slotHeight * 2.5f);
        _renderingDurationSlot = new Slot(this, renderingPos, _board._colorRenderingSlot, renderingDuration);
        _colorCell = _board._colorCell;
    }

    @Override
    public float pickingThreshold() {
        return -_board._draggingRadius*2;
    }


    @Override
    public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.stroke(_board._colorEdgeBoard);
        pg.fill(_colorCell, 180);
        int w = Math.max(_executionDurationSlot._duration, _renderingDurationSlot._duration) + 1;
        pg.rect(0, 0, _board._cellWidth * w, _board._cellHeight);
        pg.noStroke();
        pg.fill(_board._colorDraggingPoint);
        float r = _board._draggingRadius;
        pg.quad(-r,0,0,r, r,0, 0, -r);
        pg.fill(_board._colorText);
        pg.textFont(_board._font36);
        pg.textSize(12);
        pg.textAlign(PConstants.CENTER, PConstants.CENTER);
        pg.text(_name, 0, 0, _board._cellWidth * w, _board._slotHeight);
        pg.text("" + _executionDurationSlot._duration, 0, _board._slotHeight, _board._cellWidth * 0.5f, _board._slotHeight);
        pg.text("" + _renderingDurationSlot._duration, 0, _board._slotHeight * 2, _board._cellWidth * 0.5f, _board._slotHeight);
        pg.hint(PConstants.ENABLE_DEPTH_TEST);
        pg.popStyle();
    }

    public void applyMovement(){
        int col = Math.round(translation().x() / _board._cellWidth);
        int row = Math.round((translation().y() - _board._slotHeight) / (_board._cellHeight + _board._offsetHeight));
        if(_board.isOccupied(row, col)){
            _board.moveCell(_row, _col, _row, _col);
        }
        //update the board
        _board.moveCell(_row, _col, row, col);
        //fix overlapping
        _board._fixRowOverlapping(_row);
    }

    public void move(int r, int c){
        _col = c; _row = r;
        updateTranslation();
    }

    public void updateTranslation(){
        setTranslation(_col * _board._cellWidth, _row * (_board._cellHeight + _board._offsetHeight) + _board._slotHeight + _board._offsetHeight);
        _executionDurationSlot._updateTranslation(_board._cellWidth * 0.5f, _board._slotHeight * 1.5f);
        _renderingDurationSlot._updateTranslation(_board._cellWidth * 0.5f, _board._slotHeight * 2.5f);
    }

    public void setColorCell(int col){
        _colorCell = col;
    }

    public void setLabel(String text){
        //Add a label right before the cell
        if(_label == null){
            _label = new Label(this);
        }
        _label.setText(text);
    }

    public int duration(){
        return Math.max(_executionDurationSlot._duration, _renderingDurationSlot._duration);
    }
}
