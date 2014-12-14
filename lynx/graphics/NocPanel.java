package lynx.graphics;

import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.JPanel;

import lynx.data.Design;
import lynx.data.Noc;

public class NocPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    Design design;

    public NocPanel(Design design) {
        super(new GridLayout(1, 1));
        this.design = design;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Noc noc = null;
        if (design != null)
            noc = design.getNoc();
        if (noc != null)
            drawNoc(g, noc);
    }

    private void drawNoc(Graphics g, Noc noc) {

        int numRoutersPerDimension = noc.getNumRoutersPerDimension();
        int ymin = 110;
        int ymax = 100 + 10 + 100 * (numRoutersPerDimension - 1);
        int xmin = 110;
        int xmax = 100 + 10 + 100 * (numRoutersPerDimension - 1);
        for (int i = 0; i < numRoutersPerDimension; i++) {
            for (int j = 0; j < numRoutersPerDimension; j++) {
                int x = 100 + i * 100;
                int y = 100 + j * 100;
                g.fillOval(x, y, 20, 20);

                if (j == 0) {
                    g.drawLine(x + 10, ymin, x + 10, ymax);
                }
                if (i == 0) {
                    g.drawLine(xmin, y + 10, xmax, y + 10);
                }
            }
        }
    }

}
