package ui.view.color;

import org.jzy3d.colors.Color;
import org.jzy3d.colors.colormaps.AbstractColorMap;
import org.jzy3d.colors.colormaps.IColorMap;

public class ColorMapGreenYellowRed extends AbstractColorMap implements IColorMap {
    public ColorMapGreenYellowRed() {
        super();
    }

    /**
     * @inheritDoc
     */
    @Override
    public Color getColor(double x, double y, double z, double zMin, double zMax) {

        double value = 1-(z-zMin)/(zMax - zMin);

        double H = value * 0.4; // Hue (note 0.4 = Green, see huge chart below)
        double S = 0.9; // Saturation
        double B = 0.9; // Brightness

        java.awt.Color color = java.awt.Color.getHSBColor((float)H, (float)S, (float)B);

        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }
}