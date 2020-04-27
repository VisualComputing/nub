class Target extends Node{
    /*
    * A Target will be represented as a red ball
    * Note that you could override graphics or interaction methods 
    * to generate complex behaviors
    */    
    
    Target(Scene scene, float radius){
      super();
      PShape redBall;
      if (scene.is2D()) redBall = createShape(ELLIPSE,0, 0, radius*2, radius*2);
      else  redBall = createShape(SPHERE, radius);
      redBall.setStroke(false);
      redBall.setFill(color(255,0,0));
      //Exact picking precision
      this.setShape(redBall);
      this.setPickingThreshold(0);
    }
}
