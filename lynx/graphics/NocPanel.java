package lynx.graphics;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.interconnect.Mapping;

public class NocPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private Design design;
    private int selectedMapping;
    private int selectedVersion;

    private JPanel controlPanel;
    JComboBox<Integer> mappingIndex;
    JComboBox<Integer> versionIndex;

    public NocPanel(Design design) {
        super(new FlowLayout());
        this.design = design;
        selectedMapping = 0;
        selectedVersion = 0;

        controlPanel = new JPanel(new GridLayout(1, 1));
        controlPanel.setBounds(0, 0, 15, 10);
        this.add(controlPanel);

        int numMappings = design.getMappings().size();
        int numVersions = design.getMappings().get(selectedMapping).size();

        mappingIndex = new JComboBox<Integer>();
        for (int i = 0; i < numMappings; i++)
            mappingIndex.addItem(i);
        controlPanel.add(mappingIndex);

        versionIndex = new JComboBox<Integer>();
        for (int i = 0; i < numVersions; i++)
            versionIndex.addItem(i);
        controlPanel.add(versionIndex);

        configureDropDowns();
    }

    private void configureDropDowns() {
        mappingIndex.setSelectedItem(selectedMapping);
        versionIndex.setSelectedItem(selectedVersion);

        mappingIndex.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getSource() == mappingIndex) {
                    selectedMapping = mappingIndex.getSelectedIndex();
                    repaint();
                }
            }
        });
        
        versionIndex.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getSource() == versionIndex) {
                    selectedVersion = versionIndex.getSelectedIndex();
                    repaint();
                }
            }
        });

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

        Mapping currMapping = design.getMappings().get(selectedMapping).get(selectedVersion);

        for (DesignModule mod : design.getDesignModules().values()) {
            int modIndex = design.getModuleIndex(mod.getName());
            drawMod(g, mod, currMapping.getModuleRouterIndex(modIndex));
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
