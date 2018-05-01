/**
 * Agent6.
 * by Jean Pierre Charalambos.
 * 
 * This class parses space navigator input data into MotionEvent6 events.
 *
 * Data is allocated into 'control sliders' (sliderY* vars) by
 * gamecontrolplus every frame.
 *
 * Feel free to copy paste it.
 */

public class Agent6 extends Agent {
  // Sensitivities array that will multiply the sliders input
  // (found pretty much as trial an error)
  float [] s = {10, 10, 10, 10, 10, 10};
  
  public Agent6(Scene scn) {
    super(scn.inputHandler());
  }
  
  // Parsing of the Space Navigator input data which is stored in the
  // slider* variables. The MotionEvent6 output generated is sent to
  // the scene input node (either the default node or the one picked
  // by the agent) to interact with.
  //
  // To set a default node call scene.setDefaultNode(Node).
  //
  // Override pollFeed() to implement Space Navigator node picking.
  @Override
  public MotionEvent6 handleFeed() {
    return new MotionEvent6(s[0]*sliderXpos.getValue(),
                            s[1]*sliderYpos.getValue(),
                            s[2]*sliderZpos.getValue(),
                            s[3]*sliderXrot.getValue(),
                            s[4]*sliderYrot.getValue(),
                            s[5]*sliderZrot.getValue(),
                            frames.input.Event.NO_MODIFIER_MASK, SN_ID);
  }
}