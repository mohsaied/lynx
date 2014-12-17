package lynx.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.JPanel;

import lynx.data.Design;
import lynx.data.DesignModule;
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

        drawDesign(g);
    }

    private void drawDesign(Graphics g) {
        for (DesignModule mod : design.getDesignModules().values()) {
            if (mod.getRouter() != -1) {
                drawMod(g, mod, mod.getRouter());
            }
        }
    }

    private void drawMod(Graphics g, DesignModule mod, int router) {
        int i = router % design.getNoc().getNumRoutersPerDimension();
        int j = router / design.getNoc().getNumRoutersPerDimension();
        int x = 115 + i * 100;
        int y = 115 + j * 100;
        g.setColor(Color.ORANGE);
        g.fillRect(x, y, 70, 70);
        g.setColor(Color.BLACK);

        String name = mod.getName();
        if (name.length() > 10) {
            name = name.substring(0, 8);
            name += "..";
        }
        g.drawString(name, x + 5, y + 35);
    }

    private void drawNoc(Graphics g, Noc noc) {

        int numRoutersPerDimension = noc.getNumRoutersPerDimension();
        int ymin = 110;
        int ymax = 100 + 10 + 100 * (numRoutersPerDimension - 1);
        int xmin = 110;
        int xmax = 100 + 10 + 100 * (numRoutersPerDimension - 1);
        int rIndex = 0;
        for (int j = 0; j < numRoutersPerDimension; j++) {
            for (int i = 0; i < numRoutersPerDimension; i++) {
                int x = 100 + i * 100;
                int y = 100 + j * 100;
                g.setColor(Color.BLACK);
                g.fillOval(x, y, 20, 20);
                if (j == 0) {
                    g.drawLine(x + 10, ymin, x + 10, ymax);
                }
                if (i == 0) {
                    g.drawLine(xmin, y + 10, xmax, y + 10);
                }
                g.setColor(Color.WHITE);
                g.drawString(Integer.toString(rIndex), x + 4, y + 15);
                rIndex++;
            }
        }
    }

}
