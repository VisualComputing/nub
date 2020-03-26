package ik.animation.eventVisualizer;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

public class Board extends Node {
    protected int _gray1, _gray2, _gray3, _gray4, _red, _blue1, _blue2, _green1, _green2, _yellow, _white;
    protected int MAX_ROWS = 50, MAX_COLS = 500;
    protected int _rows;
    protected int _cols;
    protected float _width, _height;
    protected float _cellHeight, _cellWidth, _slotHeight, _pointDiameter, _draggingRadius, _offsetHeight;
    protected int _colorRenderingSlot, _colorExecutionSlot, _colorRenderingEmptySlot, _colorExecutionEmptySlot;
    protected int _colorEmptyCell, _colorBoardBackground, _colorEdgeBoard, _colorCell, _colorDraggingPoint, _colorEmptyDraggingPoint;
    protected int _colorText;
    protected PFont _font36;

    protected Scene _scene;
    protected Label _label;
    protected EventCell[][] _eventMatrix = new EventCell[MAX_ROWS][MAX_COLS];
    protected List<EventCell> _events = new ArrayList<>();

    public Board(Scene scene, int rows, int cols){
        super();
        enableTagging(false);
        _scene = scene;
        _rows = rows;
        _cols = cols;

        _gray1 = scene.pApplet().color(82,82,82); _gray2 = scene.pApplet().color(65,65,65); _gray3 = scene.pApplet().color(49,49,49); _gray4 = scene.pApplet().color(179,179,179);
        _red = scene.pApplet().color(202,62,71); _blue1 = scene.pApplet().color(23,34,59); _blue2 = scene.pApplet().color(38,56,89); _green1 = scene.pApplet().color(0,129,138);
        _yellow = scene.pApplet().color(249,210,118);
        _white = scene.pApplet().color(240,236,226);
        _green2 = scene.pApplet().color(33,152,151);

        _colorRenderingSlot = _red;
        _colorExecutionSlot = _green1;
        _colorRenderingEmptySlot = scene.pApplet().color(_colorRenderingSlot, 100 );
        _colorExecutionEmptySlot = scene.pApplet().color(_colorExecutionSlot, 100 );
        _colorBoardBackground = _gray2;
        _colorEdgeBoard = _gray3;
        _colorEmptyCell = _gray2;
        _colorCell = _gray1;
        _colorDraggingPoint = _yellow;
        _colorEmptyDraggingPoint = scene.pApplet().color(_yellow, 60);
        _colorText = _white;
        _font36 = scene.pApplet().createFont("Arial", 48, true);//loadFont("FreeSans-36.vlw");

        this.setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                //No restriction on translation
                return translation;
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                //Rotation is not allowed
                return new Quaternion();
            }
        });
    }

    public int rows(){
        return _rows;
    }

    public int cols(){
        return _cols;
    }

    public float width(){
        return _width;
    }

    public float height(){
        return _height;
    }

    public void setDimension(float x, float y, float width, float height){
        this.translate(x,y, 0);
        _width = width;
        _height = height;
        _setCellDimension();
    }

    @Override
    public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(_colorBoardBackground);
        pg.stroke(_colorEdgeBoard);
        pg.rect(0,0,_width,_height);

        drawGrid(pg, _cellWidth, _slotHeight);
        drawHeader(pg, _cellWidth, _slotHeight);
        pg.pushMatrix();
        pg.translate(0, _slotHeight + _offsetHeight);
        for(int r = 0; r < _rows; r++){
            for(int c = 0; c < _cols; c++){
                drawCell(pg, r, c, _cellWidth, _cellHeight);
            }
        }
        pg.popMatrix();
        pg.popStyle();
    }

    protected void drawHeader(PGraphics pg, float width, float height){
        pg.pushStyle();
        pg.textFont(_font36);
        pg.textSize(14);
        pg.textAlign(PConstants.CENTER, PConstants.CENTER);
        for(int c = 0; c < _cols; c++){
            pg.fill(_colorText);
            pg.text(" " + c, (c + 0.5f)*width, height * 0.5f);
        }
        pg.popStyle();
    }

    protected void drawGrid(PGraphics pg, float width, float height){
        pg.pushStyle();
        pg.fill(_colorEdgeBoard);
        float y = 0;
        for(int r = 0; r < _rows; r++){
            pg.rect(0, r*(_offsetHeight + _cellHeight) + _slotHeight, _width, _offsetHeight);
       }

        for(int r = 0; r <= 4*_rows + 1; r++){
            pg.line(0, y , _width, y);
            if(r % 4 == 1) y += _offsetHeight;
            else y += _slotHeight;
        }

        for(int c = 0; c <= _cols; c++){
            pg.line(c*width, 0, c*width, _height);
        }
        pg.popStyle();
    }

    protected void drawCell(PGraphics pg, int row, int col, float width, float height){
        pg.pushStyle();
        pg.noStroke();
        pg.fill(_colorEmptyDraggingPoint);
        float r = _draggingRadius;
        float xc = width * col , yc = height * row + _offsetHeight * row;
        pg.quad(xc - r,yc,xc, yc + r, xc + r,yc, xc, yc - r);
        pg.fill(_colorExecutionEmptySlot);
        xc += width * 0.5f;
        pg.ellipse(xc,yc + _slotHeight*1.5f, _pointDiameter, _pointDiameter);
        pg.fill(_colorRenderingEmptySlot);
        pg.ellipse(xc,yc + _slotHeight*2.5f, _pointDiameter, _pointDiameter);
        pg.popStyle();
    }

    public void addRows(int n, boolean keepHeight){
        if(!keepHeight) _height += 1.f * _height/_rows * n;
        _rows += n;
        _setCellDimension();
    }

    public void addCols(int n, boolean keepWidth){
        if(!keepWidth) _width += 1.f * _width/_cols * n;
        _cols += n;
        _setCellDimension();
    }

    public void setHeight(float height){
        _height = height;
        _setCellDimension();
    }

    public void setWidth(float width){
        _width = width;
        _setCellDimension();
    }

    protected void _setCellDimension(){
        _offsetHeight = _height/(7f * _rows + 2);
        _slotHeight = _offsetHeight * 2;
        _cellWidth = _width / _cols;
        _cellHeight = _slotHeight*3;
        _pointDiameter = Math.min(_cellWidth, _slotHeight) * 0.5f;
        _draggingRadius = _pointDiameter * 0.25f;
        for(EventCell e : _events) e.updateTranslation();
    }

    protected void moveCell(int r1, int c1, int r2, int c2){
        EventCell aux = _eventMatrix[r1][c1];
        _eventMatrix[r1][c1] = _eventMatrix[r2][c2];
        _eventMatrix[r2][c2] = aux;
        aux.move(r2, c2);
    }

    //Fixes row overlapping due of duration change of event
    protected void _fixRowOverlapping(int row){
        for(int col = _cols - 1; col >= 0; col--){
            if(_eventMatrix[row][col] != null){
                _moveToFirstAvailableRow(_eventMatrix[row][col]);
            }
        }
    }

    protected void _moveToFirstAvailableRow(EventCell cell){
        int r = _firstAvailableRow(cell, false);
        if(r >= _rows)
            addRows(r - _rows + 1, false);
        moveCell(cell._row, cell._col, r, cell._col);
    }

    protected int _nextRow(EventCell cell){
        int next = 0;
        for(int r = 0; r < _rows; r++){
            if(_isPreviousOverlapping(r, cell)){
                next = r + 1;
            }
        }
        return next;
    }

    protected int _firstAvailableRow(EventCell cell, boolean next){
        int left_counter = cell._row, right_counter = cell._row + 1;
        if(next){
            left_counter = -1;
            right_counter = _nextRow(cell);
        }

        while(left_counter >= 0 || right_counter < _rows){
            if(left_counter >= 0){ //above
                if(!_isPreviousOverlapping(left_counter, cell) && !_isNextOverlapping(left_counter, cell)) return left_counter;
                left_counter--;
            }
            if(right_counter < _rows){ //below
                if(!_isPreviousOverlapping(right_counter, cell) && !_isNextOverlapping(right_counter, cell)) return right_counter;
                right_counter++;
            }
        }
        return _rows;
    }

    protected boolean _isPreviousOverlapping(int row, EventCell cell){
        EventCell prev = _previousOccupiedCellOnRow(row, cell._col);
        int last_c_occupied = prev == null ? -1 : prev._col + Math.max(prev._renderingDurationSlot._duration, prev._executionDurationSlot._duration);
        if((_eventMatrix[row][cell._col] == null || _eventMatrix[row][cell._col] == cell) && last_c_occupied < cell._col){
            return false;
        }
        return true;
    }

    protected boolean _isNextOverlapping(int row, EventCell cell){
        EventCell next = _nextOccupiedCellOnRow(row, cell._col);
        int last_c_occupied = next == null ? _cols : next._col;

        if((_eventMatrix[row][cell._col] == null || _eventMatrix[row][cell._col] == cell) &&  cell._col + Math.max(cell._renderingDurationSlot._duration, cell._executionDurationSlot._duration) < last_c_occupied){
            return false;
        }
        return true;
    }

    protected EventCell _nextOccupiedCellOnRow(int row, int col){
        for(int c = col +1; c < _cols; c++){
            if(_eventMatrix[row][c] != null) return _eventMatrix[row][c];
        }
        return null;
    }


    //TODO: THIS METHODS ASSUME THAT THERE'S ONLY AN EVENT PER ROW
    protected EventCell _previousOccupiedCellOnRow(int row, int col){
        for(int c = col -1; c >= 0; c--){
            if(_eventMatrix[row][c] != null) return _eventMatrix[row][c];
        }
        return null;
    }

    public EventCell previousCell(EventCell cell){
        int row = cell.row() - 1;
        if(row < 0) return null;
        for(int c = cell.col(); c >= 0; c--){
            if(_eventMatrix[row][c] != null) return _eventMatrix[row][c];
        }
        return null;
    }

    public EventCell nextCell(EventCell cell){
        int row = cell.row() - 1;
        if(row < 0) return null;
        for(int c = cell.col(); c >= 0; c--){
            if(_eventMatrix[row][c] != null) return _eventMatrix[row][c];
        }
        return null;
    }


    public EventCell cellAt(int row){
        for(int c = 0; c < _cols; c++){
            if(_eventMatrix[row][c] != null){
                return _eventMatrix[row][c];
            }
        }
        return null;
    }


    protected boolean isOccupied(int row, int col){
        return _eventMatrix[row][col] != null;
    }

    public void setLabel(String text){
        //Add a label right before the cell
        if(_label == null){
            _label = new Label(this);
        }
        _label.setText(text);
    }

}
