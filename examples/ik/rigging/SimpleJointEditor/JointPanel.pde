class JointPanel{
  class Slider{
    String _name;
    GSlider _slider;
    public Slider(GGroup group, GPanel panel, String name, float x, float y, float min, float max, float cur){
      _name = name;
      _createSlider(group, panel, name, x, y);
      _slider.setLimits(cur, min, max);
    }
    
    void _createSlider(GGroup group, GPanel panel, String name, float x, float y){
      GLabel label = new GLabel(_pApplet, x, y + y_offset, label_width, 2 * y_offset);
      label.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
      label.setText(name);
      label.setOpaque(false);
      _slider = new GSlider(_pApplet, label.getWidth() + x_offset, y + y_offset, 180, 2 * y_offset, 10.0);
      _slider.setShowValue(true);
      _slider.setNumberFormat(G4P.DECIMAL, 2);
      _slider.setOpaque(false);
      group.addControls(label, _slider);
      panel.addControl(label);
      panel.addControl(_slider);
    }
  }
  
  
  PApplet _pApplet;
  float _x, _y, _w, _h;
  boolean _enabled  = false;
  boolean _updateFlag = false; 
  
  //Logic variables
  Node _twist, _up;
  float _twistMag, _upMag;
  boolean _updateTwistControl, _updateUpControl;
  
  float _minTheta, _maxTheta;
  float _upTheta, _downTheta, _leftTheta, _rightTheta, _twistMaxTheta, _twistMinTheta;
  
  Joint _joint;
  
  
  public JointPanel(PApplet pApplet, float x, float y, float w, float h){
    _pApplet = pApplet;
    _x = x;
    _y = y;
    _w = w;
    _h = h;
    _createPanel();
    _createVectorPoints();
    setEnabled(false);
  }
  
  
  void _createVectorPoints(){
    _twistMag = scene.radius() * 0.1f;
    _upMag = scene.radius() * 0.1f;
        
    _twist = new Node(){
      @Override
      public void graphics(PGraphics pg){
        pg.pushStyle();
        pg.fill(0,0,255);
        pg.noStroke();
        pg.sphere(scene.radius() * 0.01f);
        pg.popStyle();
      }
    };
    _twist.setPickingThreshold(0);

    _up = new Node(){
      @Override
      public void graphics(PGraphics pg){
        pg.pushStyle();
        pg.fill(0,255,0);
        pg.noStroke();
        pg.sphere(scene.radius() * 0.01f);
        pg.popStyle();
      }
    };
    _up.setPickingThreshold(0);

    _twist.setTranslation(new Vector(0,0,_twistMag));
    _up.setTranslation(new Vector(0,_upMag,0));

    
    _twist.setConstraint(
        new Constraint() {
          @Override
          public Vector constrainTranslation(Vector translation, Node node) {
            //Keep current magnitude
            Vector newTranslation = Vector.add(node.translation(), translation);
            newTranslation.normalize();
            newTranslation.multiply(_twistMag);
            _up.setTranslation(_up.translation());
            return Vector.subtract(newTranslation, node.translation());
          }

          @Override
          public Quaternion constrainRotation(Quaternion rotation, Node node) {
            return new Quaternion(); // rotation is not allowed
          }
    });      
    
    _up.setConstraint(
        new Constraint() {
          @Override
          public Vector constrainTranslation(Vector translation, Node node) {
            //Keep current magnitude
            Vector newTranslation = Vector.add(node.translation(), translation);
            newTranslation = Vector.projectVectorOnPlane(newTranslation, _twist.translation());
            newTranslation.normalize();
            newTranslation.multiply(_upMag);
            if(!_up.isCulled()){
              updateConstraintControls();
              if(_joint.constraint() instanceof Hinge) setHingeFromInformation();
              if(_joint.constraint() instanceof BallAndSocket) setBallFromInformation();
            }
            //Keep on Plane defined by Twist vector
            return Vector.subtract(newTranslation, node.translation());
          }

          @Override
          public Quaternion constrainRotation(Quaternion rotation, Node node) {
            return new Quaternion(); // rotation is not allowed
          }
    });
    _enableVectorPoints(false);
  }
  
  void _enableVectorPoints(boolean enable){
      _twist.cull(!enable);
      _up.cull(!enable);
  }
  
  boolean isOver(float x, float y){
    return _enabled && 
           (x >= _gPanel.getX() && x <= _gPanel.getX() + _gPanel.getWidth()) &&
           (y >= _gPanel.getY() && y <= _gPanel.getY() + _gPanel.getHeight());
  }
  
  void setEnabled(boolean enable){
    _enabled = enable;
    _gPanel.setVisible(enable);
    _gPanel.setEnabled(enable);
  }
  
  void setJointName(Skeleton skeleton){
    if(_joint == null) return;
    String newName = _nameField.getText();
    println("name : " + skeleton.jointName(_joint) + " new " + newName);
    skeleton.setName(skeleton.jointName(_joint),newName);
  }
  
  
  void getInformationFromJoint(Skeleton skeleton, Node node){
    if(!(node instanceof Joint)){
      setEnabled(false);
      return;
    }
    setEnabled(true);
    _joint = (Joint) node;
    //1. Get name and set textField
    String name = skeleton.jointName(_joint);
    _nameField.setText(name);
    //2. Get constraint information
    Constraint constraint = _joint.constraint();
    if(constraint == null){
      resetValues();
      _optionNone.setSelected(true);
      showNone();
    } else if(constraint instanceof Hinge){
      resetValues();
      Hinge hinge = (Hinge) constraint;
      getHingeInformation(_joint, hinge);
      updateControls();
      _optionHinge.setSelected(true);
      showHinge();
      setHingeFromInformation();
    } else if(constraint instanceof BallAndSocket){
      resetValues();
      BallAndSocket ball = (BallAndSocket) constraint;
      getBallInformation(_joint, ball);
      updateControls();
      _optionBall.setSelected(true);
      showBall();
      setBallFromInformation();
    }
  }
  
  void getHingeInformation(Joint joint, Hinge hinge){
    Vector tw = hinge.orientation().rotate(new Vector(0,0,1));
    Vector up = hinge.orientation().rotate(new Vector(0,1,0));
    _twist.setTranslation(joint.displacement(tw, joint.reference()));
    _up.setTranslation(joint.displacement(up, joint.reference()));
    _minTheta = hinge.minAngle();
    _maxTheta = hinge.maxAngle();
  }
  
  void getBallInformation(Joint joint, BallAndSocket ball){
    Vector tw = ball.orientation().rotate(new Vector(0,0,1));
    Vector up = ball.orientation().rotate(new Vector(0,1,0));
    _twist.setTranslation(joint.displacement(tw, joint.reference()));
    _up.setTranslation(joint.displacement(up, joint.reference()));
    _upTheta = ball.up();
    _downTheta = ball.down();
    _leftTheta = ball.left();
    _rightTheta = ball.right();
    _twistMaxTheta = ball.maxTwistAngle();
    _twistMinTheta = ball.minTwistAngle();
  }  
  
  void setNoneConstraint(){
    _joint.setConstraint(null);
  }
  
  
  void setHingeFromInformation(){
    Hinge h = new Hinge(_minTheta, _maxTheta, _joint.rotation(), _up.translation().get(), _twist.translation().get());
    _joint.setConstraint(h);
  }
  
  void setBallFromInformation(){
    BallAndSocket b = new BallAndSocket(_downTheta, _upTheta, _leftTheta, _rightTheta);
    b.setRestRotation(_joint.rotation(), _up.translation().get(), _twist.translation().get());
    b.setTwistLimits(_twistMinTheta, _twistMaxTheta);
    _joint.setConstraint(b);
  }
  
  void updateConstraintControls(){
    Vector t = _twist.translation().get();
    t.normalize();
    _twistXSlider._slider.setValue(t.x());
    _twistYSlider._slider.setValue(t.y());
    _twistZSlider._slider.setValue(t.z());
    Vector u = _up.translation().get();
    u.normalize();
    _upXSlider._slider.setValue(u.x());
    _upYSlider._slider.setValue(u.y());
    _upZSlider._slider.setValue(u.z());
  }
  
  void updateHingeControls(){
    _maxHingeSlider._slider.setValue(degrees(_maxTheta));
    _minHingeSlider._slider.setValue(degrees(_minTheta));
  }

  void updateBallControls(){
    _upBallSlider._slider.setValue(degrees(_upTheta));
    _downBallSlider._slider.setValue(degrees(_downTheta));
    _leftBallSlider._slider.setValue(degrees(_leftTheta));
    _rightBallSlider._slider.setValue(degrees(_rightTheta));
  }
  
  
  void resetValues(){
    resetConstraintValues();
    resetHingeValues();
    resetBallValues();
  }
  
  void updateControls(){
    updateConstraintControls();
    updateHingeControls();
    updateBallControls();
  }
  
  void resetConstraintValues(){
    _twist.setTranslation(new Vector(0,0,1));
    _up.setTranslation(new Vector(0,1,0));
    if(!_joint.children().isEmpty()){
      calculateInitialVectorPoints();
    }
    _twist.setReference(_joint);
    _up.setReference(_joint);
    _enableVectorPoints(false);    
  }

  void calculateInitialVectorPoints(){
    Vector centroid = new Vector(); 
    for(Node child : _joint.children()){
      if(child == _twist || child == _up) continue;
      centroid.add(child.translation());
    }    
    if(centroid.magnitude() > scene.radius() * 0.01f){
      centroid.divide(_joint.children().size());
      if(_optionHinge.isSelected()){
        _twist.setTranslation(centroid.orthogonalVector());
        _up.setTranslation(centroid.get());
      } else{
        _twist.setTranslation(centroid.get());
        _up.setTranslation(_twist.translation().orthogonalVector());
      }
    }
  }
  

  void resetHingeValues(){
    _minTheta = radians(60);
    _maxTheta = radians(60);
  }
  void resetBallValues(){
    _upTheta = _downTheta = _leftTheta = _rightTheta = _twistMaxTheta = _twistMinTheta = radians(30);
  }
  
  void showNone(){
    _enableVectorPoints(false);
    _constraintGroup.setVisible(false);
    _hingeGroup.setVisible(false);
    _ballGroup.setVisible(false);
  }
  
  void showHinge(){
    _enableVectorPoints(true);
    _constraintGroup.setVisible(true);
    _hingeGroup.setVisible(true);
    _ballGroup.setVisible(false);
  }
  
  void showBall(){
      _enableVectorPoints(true);    
    _constraintGroup.setVisible(true);
    _hingeGroup.setVisible(false);
    _ballGroup.setVisible(true);
  }
  
  void drawInformation(){
    if(!_enabled || _joint == null) return;
    scene.context().pushStyle();
    if(_joint.constraint() != null){
      //Draw twist and up vector
      scene.context().pushMatrix();
      scene.context().noStroke();
      scene.applyWorldTransformation(_joint);
      scene.context().fill(0,255,0);
      scene.drawArrow(_up.translation());
      scene.context().fill(0,0,255);    
      scene.drawArrow(_twist.translation());
      scene.context().popMatrix();
    }
    
    scene.beginHUD();
    Vector pix = scene.screenLocation(_joint);
    scene.context().fill(255);
    scene.context().stroke(255);
    scene.context().textAlign(CENTER, CENTER);
    scene.context().textSize(16);
    scene.context().text(_nameField.getText(),pix.x(), pix.y() - 20);
    scene.endHUD();
    scene.context().popStyle();
    
  }
  
  //G4P logic
  
  //G4p variables
  GPanel _gPanel; 
  GTextField _nameField; 
  GToggleGroup _togGroup; 
  GOption _optionNone; 
  GOption _optionHinge; 
  GOption _optionBall; 
  GGroup _constraintGroup, _hingeGroup, _ballGroup;
  Slider _twistXSlider, _twistYSlider, _twistZSlider;
  Slider _upXSlider, _upYSlider, _upZSlider;
  Slider _maxHingeSlider, _minHingeSlider;
  Slider _upBallSlider, _downBallSlider, _leftBallSlider, _rightBallSlider, _twistMaxSlider, _twistMinSlider;
  float x_offset = 5;
  float y_offset = 20;
  float label_width = 50;
  //G4p end of variables
  
  void _createPanel(){
    G4P.setMouseOverEnabled(false);
    G4P.setGlobalColorScheme(GCScheme.CYAN_SCHEME);
    _gPanel = new GPanel(_pApplet, _x, _y, _w, _h - 10, "Joint settings");
    _gPanel.setText("Joint settings");
    //create controls
    _gPanel.setLocalColorScheme(GCScheme.CYAN_SCHEME);
    _gPanel.setOpaque(true);
    GLabel label1 = new GLabel(_pApplet, x_offset, y_offset + 10, label_width, y_offset);
    label1.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
    label1.setText("Name");
    label1.setOpaque(false);
    _nameField = new GTextField(_pApplet, label1.getWidth() + x_offset, label1.getY(), 120, y_offset, G4P.SCROLLBARS_NONE);
    _nameField.setText("");
    _nameField.setOpaque(true);
    _gPanel.addControl(label1);
    _gPanel.addControl(_nameField);
    _nameField.addEventHandler(_pApplet, "_nameFieldChanged");

    _togGroup = new GToggleGroup();
    _optionNone = new GOption(_pApplet, x_offset, label1.getY() + label1.getHeight() + 20, 180, y_offset);
    _optionNone.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
    _optionNone.setText("Unconstrained");
    _optionNone.setOpaque(false);
    _optionNone.addEventHandler(_pApplet, "_optionNoneClicked");
    
    _optionHinge = new GOption(_pApplet, x_offset, _optionNone.getY() + _optionNone.getHeight() + 20, 180, y_offset);
    _optionHinge.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
    _optionHinge.setText("Use hinge constraint");
    _optionHinge.setOpaque(false);
    _optionHinge.addEventHandler(_pApplet, "_optionHingeClicked");
    _optionBall = new GOption(_pApplet, x_offset, _optionHinge.getY() + _optionHinge.getHeight() + 20, 180, y_offset); 
    _optionBall.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
    _optionBall.setText("Use ball and socket constraint");
    _optionBall.setOpaque(false);
    _optionBall.addEventHandler(_pApplet, "_optionBallClicked");
    _togGroup.addControl(_optionNone);
    _optionNone.setSelected(true);
    _gPanel.addControl(_optionNone);
    _togGroup.addControl(_optionHinge);
    _gPanel.addControl(_optionHinge);
    _togGroup.addControl(_optionBall);
    _gPanel.addControl(_optionBall);

    GAbstractControl prev = _optionBall;
    //_constraintPanel = new GPanel(_pApplet, 0, prev.getY() + prev.getHeight() + 2 * y_offset, _w, y_offset * 15, "Constraint settings");
    _constraintGroup = new GGroup(_pApplet);
    GLabel label2 = new GLabel(_pApplet, x_offset, prev.getY() + prev.getHeight() + y_offset, 180, y_offset);
    label2.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
    label2.setText("Constraint Settings");
    label2.setOpaque(false);
    _constraintGroup.addControl(label2);
    _gPanel.addControl(label2);
    prev = label2;
    G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);    
    _twistXSlider = new Slider(_constraintGroup, _gPanel, "Twist x", x_offset, prev.getY() + 10, -1, 1, 0);
    prev = _twistXSlider._slider;
    _twistYSlider = new Slider(_constraintGroup, _gPanel, "Twist y", x_offset, prev.getY() + prev.getHeight(), -1, 1, 0);
    prev = _twistYSlider._slider;
    _twistZSlider = new Slider(_constraintGroup, _gPanel, "Twist z", x_offset, prev.getY() + prev.getHeight(), -1, 1, 1);
    prev = _twistZSlider._slider;
    G4P.setGlobalColorScheme(GCScheme.GREEN_SCHEME);    
    _upXSlider = new Slider(_constraintGroup, _gPanel, "Up x", x_offset, prev.getY() + prev.getHeight(), -1, 1, 1);
    prev = _upXSlider._slider;
    _upYSlider = new Slider(_constraintGroup, _gPanel, "Up y", x_offset, prev.getY() + prev.getHeight(), -1, 1, 0);
    prev = _upYSlider._slider;
    _upZSlider = new Slider(_constraintGroup, _gPanel, "Up z", x_offset, prev.getY() + prev.getHeight(), -1, 1, 0);
    
    //_gPanel.addControl(_constraintPanel);
    _twistXSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _twistYSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _twistZSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _upXSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _upYSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _upZSlider._slider.addEventHandler(_pApplet, "_constraintChanged");

    
    
    G4P.setGlobalColorScheme(GCScheme.CYAN_SCHEME);

    prev = _upZSlider._slider;
    _hingeGroup = new GGroup(_pApplet);
    GLabel label3 = new GLabel(_pApplet, x_offset, prev.getY() + prev.getHeight() + y_offset, 180, y_offset);
    label3.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
    label3.setText("Hinge Settings");
    label3.setOpaque(false);
    _hingeGroup.addControl(label3);
    _gPanel.addControl(label3);
    //_hingePanel = new GPanel(_pApplet, 0, _constraintPanel.getY() + prev.getY() + prev.getHeight() + 2 * y_offset, _w, y_offset * 6, "Hinge settings");
    _minHingeSlider = new Slider(_hingeGroup, _gPanel, "Min angle", x_offset, prev.getY() + prev.getHeight() + y_offset + 10, 5, 180, 60);
    _minHingeSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    
    prev = _minHingeSlider._slider;
    _maxHingeSlider = new Slider(_hingeGroup, _gPanel, "Max angle", x_offset, prev.getY() + prev.getHeight(), 5, 180, 60);
    _maxHingeSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    //_gPanel.addControl(_hingePanel);

    prev = _upZSlider._slider;
    _ballGroup = new GGroup(_pApplet);
    GLabel label4 = new GLabel(_pApplet, x_offset, prev.getY() + prev.getHeight() + y_offset, 180, y_offset);
    label4.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
    label4.setText("Ball and Socket Settings");
    label4.setOpaque(false);
    _ballGroup.addControl(label4);
    _gPanel.addControl(label4);
    //_ballPanel = new GPanel(_pApplet, 0, _constraintPanel.getY() + prev.getY() + prev.getHeight() + 2 * y_offset, _w, y_offset * 15, "Ball and socket settings");
    _upBallSlider = new Slider(_ballGroup, _gPanel, "Up angle", x_offset, prev.getY() + prev.getHeight() + y_offset + 10, 5, 85, 30);
    prev = _upBallSlider._slider;
    _downBallSlider = new Slider(_ballGroup, _gPanel, "Down angle", x_offset, prev.getY() + prev.getHeight(), 5, 85, 30);
    prev = _downBallSlider._slider;
    _leftBallSlider = new Slider(_ballGroup, _gPanel, "Left angle", x_offset, prev.getY() + prev.getHeight(), 5, 85, 30);
    prev = _leftBallSlider._slider;
    _rightBallSlider = new Slider(_ballGroup, _gPanel, "Right angle", x_offset, prev.getY() + prev.getHeight(), 5, 85, 30);
    prev = _rightBallSlider._slider;
    _twistMaxSlider = new Slider(_ballGroup, _gPanel, "Twist max", x_offset, prev.getY() + prev.getHeight(), 0, 180, 30);
    prev = _twistMaxSlider._slider;
    _twistMinSlider = new Slider(_ballGroup, _gPanel, "Twist min", x_offset, prev.getY() + prev.getHeight(), 0, 180, 30);
    
    //_gPanel.addControl(_ballPanel);
    _upBallSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _downBallSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _leftBallSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _rightBallSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _twistMaxSlider._slider.addEventHandler(_pApplet, "_constraintChanged");
    _twistMinSlider._slider.addEventHandler(_pApplet, "_constraintChanged");

    
    
    _constraintGroup.setVisible(false);
    _hingeGroup.setVisible(false);
    _ballGroup.setVisible(false);
    _gPanel.setDraggable(false);
    _gPanel.setCollapsible(false);

  }
  //End of G4P logic
}

//G4P Event handling
public void _optionNoneClicked(GOption source, GEvent event) {
  //Hide constraint panels
  panel.showNone();
  panel.setNoneConstraint();
} 

public void _optionHingeClicked(GOption source, GEvent event) { 
  //Hide constraint panels
  panel.showHinge();
  panel.calculateInitialVectorPoints();
  panel.setHingeFromInformation();
} 

public void _optionBallClicked(GOption source, GEvent event) { 
  //Hide constraint panels
  panel.showBall();
  panel.calculateInitialVectorPoints();
  panel.setBallFromInformation();
} 

public void _constraintChanged(GSlider source, GEvent event){
  if(event.getType().equals("PRESSED")){
    panel._updateFlag = true;
  }
  if(event.getType().equals("RELEASED")){
    panel._updateFlag = false;
  }
  
  if(!panel._updateFlag) return;
  
  if(source == panel._twistXSlider._slider || source == panel._twistYSlider._slider || source == panel._twistZSlider._slider){
    Vector t = new Vector();
    t.setX(panel._twistXSlider._slider.getValueF());
    t.setY(panel._twistYSlider._slider.getValueF());
    t.setZ(panel._twistZSlider._slider.getValueF());
    panel._twist.setTranslation(t);
    Vector u = Vector.projectVectorOnPlane(panel._up.translation(), panel._twist.translation());
    panel._up.setTranslation(u);
  } else if(source == panel._upXSlider._slider || source == panel._upYSlider._slider || source == panel._upZSlider._slider){
    Vector up = new Vector();
    up.setX(panel._upXSlider._slider.getValueF());
    up.setY(panel._upYSlider._slider.getValueF());
    up.setZ(panel._upZSlider._slider.getValueF());
    //make up vector
    panel._up.setTranslation(up);
  }
  if(panel._joint.constraint() instanceof Hinge){
    _hingeChanged(source, event);
  } else if(panel._joint.constraint() instanceof BallAndSocket){
    _ballChanged(source, event);
  }
}

public void _hingeChanged(GSlider source, GEvent event){
  if(source == panel._minHingeSlider._slider){
    panel._minTheta = radians(panel._minHingeSlider._slider.getValueF());
  } else if(source == panel._maxHingeSlider._slider){
    panel._maxTheta = radians(panel._maxHingeSlider._slider.getValueF());
  }
  panel.setHingeFromInformation();
}

public void _ballChanged(GSlider source, GEvent event){
  if(source == panel._upBallSlider._slider){
    panel._upTheta = radians(panel._upBallSlider._slider.getValueF());
  } else if(source == panel._downBallSlider._slider){
    panel._downTheta = radians(panel._downBallSlider._slider.getValueF());
  } else if(source == panel._leftBallSlider._slider){
    panel._leftTheta = radians(panel._leftBallSlider._slider.getValueF());
  } else if(source == panel._rightBallSlider._slider){
    panel._rightTheta = radians(panel._rightBallSlider._slider.getValueF());
  } else if(source == panel._twistMinSlider._slider){
    println("Entraaa");
    panel._twistMinTheta = radians(panel._twistMinSlider._slider.getValueF());
  } else if(source == panel._twistMaxSlider._slider){
    panel._twistMaxTheta = radians(panel._twistMaxSlider._slider.getValueF());
  } 
  panel.setBallFromInformation();
}
