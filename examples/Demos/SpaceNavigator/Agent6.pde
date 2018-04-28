/**
 * Agent6.
 * by Jean Pierre Charalambos.
 * 
 * This class parses space navigator input data into
 * MotionEvent6 events.
 *
 * Data is allocated into 'control sliders' (sliderY* vars)
 * by gamecontrolplus every frame.
 *
 * Feel free to copy paste it.
 */

public class Agent6 extends Agent {
  // array of sensitivities that will multiply the sliders input
  // found pretty much as trial an error
  float [] s = {10, 10, 10, 10, 10, 10};
  
  public Agent6(Scene scn) {
    super(scn.inputHandler());
  }
  
  // polling is done by overriding the feed agent method
  // Note 1: The handleFeed returns a MotionEvent6, i.e.,
  //         a 6 DOF motion event
  // Note 2: that we pass the space navigator gesture id: SN_ID
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
