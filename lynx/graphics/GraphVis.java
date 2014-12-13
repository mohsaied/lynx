package lynx.graphics;

import java.awt.Graphics;

import javax.swing.JPanel;

import lynx.data.Design;
import lynx.data.DesignModule;

public class GraphVis extends JPanel {

    private static final long serialVersionUID = 1L;

    Design design;

    public GraphVis(Design design) {
        super();
        this.design = design;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawConnectivityGraph(g);
    }

    private void drawConnectivityGraph(Graphics g) {

        assert design != null : "Design passed to graphics is null, cannot draw!";

        int i = 0;
        for (DesignModule mod : design.getDesignModules().values()) {
            g.drawString(mod.getName(), 100, 100 + 20 * i++);
        }

    }
}
