package ui.view.closedloopfueling.kfkhfm.colormap;

import org.jzy3d.colors.Color;
import org.jzy3d.colors.colormaps.AbstractColorMap;

public class ColorMapTransparent extends AbstractColorMap {

    @Override
    public Color getColor(double v, double v1, double v2, double v3, double v4) {
        return Color.color(java.awt.Color.TRANSLUCENT);
    }
}
