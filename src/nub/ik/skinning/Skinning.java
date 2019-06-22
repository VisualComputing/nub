package nub.ik.skinning;

import nub.core.Node;
import processing.core.PGraphics;

public interface Skinning {
    void updateParams();
    void initParams();
    void render();
    void render(PGraphics pg);
    void render(Node node);
}
