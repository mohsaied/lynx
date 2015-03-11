package lynx.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.interconnect.mapping.Mapping;

public class NocPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(NocPanel.class.getName());

    private Design design;
    private int selectedMapping;
    private int selectedVersion;

    private JPanel controlPanel;
    JComboBox<Integer> mappingIndex;
    JComboBox<Integer> versionIndex;

    public NocPanel(Design design) {
        super(new FlowLayout());
        log.setLevel(Level.ALL);
        this.design = design;

        initPane();

    }

    public void setDesign(Design design) {
        this.design = design;
        initPane();
    }

    private void initPane() {

        if (design != null && design.getMappings() != null) {
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
    }

    private void configureDropDowns() {
        mappingIndex.setSelectedItem(selectedMapping);

        mappingIndex.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getSource() == mappingIndex && event.getStateChange() == ItemEvent.SELECTED) {
                    selectedMapping = mappingIndex.getSelectedIndex();

                    int numVersions = design.getMappings().get(selectedMapping).size();
                    versionIndex.removeAllItems();
                    for (int i = 0; i < numVersions; i++)
                        versionIndex.addItem(i);
                    versionIndex.setSelectedItem(0);

                    repaint();

                    compareToBestMappingWithConsolePrint(design, selectedMapping, selectedVersion);
                }
            }
        });

        versionIndex.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getSource() == versionIndex) {
                    selectedVersion = versionIndex.getSelectedIndex();
                    repaint();
                }
            }
        });

    }

    protected void compareToBestMappingWithConsolePrint(Design design, int selectedMapping, int selectedVersion) {
        // TODO compare traffic

        Mapping bestMapping = design.getMappings().get(0).get(0);
        Mapping currMapping = design.getMappings().get(selectedMapping).get(selectedVersion);

        List<Connection> connections = design.getConnections();

        for (Connection con : connections) {
            int bestLat = bestMapping.getConnectionPath(con).size() - 1;
            int currLat = currMapping.getConnectionPath(con).size() - 1;

            if (bestLat < currLat)
                log.warning("Selected Mapping (" + selectedMapping + ") has increased latency " + currLat + ", instead of "
                        + bestLat + " on connection between " + con.getFromModule().getName() + "-->"
                        + con.getToModule().getName());
            else if (bestLat > currLat)
                log.warning("Selected Mapping (" + selectedMapping + ") has decreased latency " + currLat + ", instead of "
                        + bestLat + " on connection between " + con.getFromModule().getName() + "-->"
                        + con.getToModule().getName());
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Noc noc = null;
        if (design != null)
            noc = design.getNoc();
        if (noc != null)
            drawNoc(g, noc);

        if (design != null && design.getMappings() != null)
            drawDesign(g);
    }

    private void drawDesign(Graphics g) {

        Mapping currMapping = design.getMappings().get(selectedMapping).get(selectedVersion);

        for (DesignModule mod : design.getDesignModules().values()) {
            int modIndex = design.getModuleIndex(mod.getName());
            drawMod(g, mod, currMapping.getModuleRouterIndex(modIndex));
        }

        // draw the connections
        drawConnections(g, currMapping);

    }

    private void drawConnections(Graphics g, Mapping currMapping) {

        Map<String, List<Integer>> linkIndices = new HashMap<String, List<Integer>>();
        double[][] nocLinks = design.getNoc().getAdjacencyMatrix();

        // initialize used link indices (none are used at the start)
        for (int i = 0; i < design.getNoc().getNumRouters(); i++) {
            for (int j = 0; j < design.getNoc().getNumRouters(); j++) {
                if (nocLinks[i][j] == 1.0) {
                    List<Integer> emptyList = new ArrayList<Integer>();
                    linkIndices.put(Mapping.linkString(i, j), emptyList);
                }
            }
        }

        // loop over all connection paths
        int x = 0;
        for (Connection con : design.getConnections()) {
            List<Integer> path = design.getMappings().get(selectedMapping).get(selectedVersion).getConnectionPath(con);

            // determine connection drawIndex
            int drawIndex = 0;
            for (int i = 0; i < path.size() - 1; i++) {
                int fromRouter = path.get(i);
                int toRouter = path.get(i + 1);
                String linkStr = Mapping.linkString(fromRouter, toRouter);
                while (linkIndices.get(linkStr).contains(drawIndex)) {
                    drawIndex++;
                }
                String oppLinkStr = Mapping.linkString(toRouter, fromRouter);
                linkIndices.get(linkStr).add(drawIndex);
                linkIndices.get(oppLinkStr).add(drawIndex);
            }
            g.drawString("" + drawIndex, 10 * (++x), 10);

            // draw the connection on the links
            for (int i = 0; i < path.size() - 1; i++) {
                int fromRouter = path.get(i);
                int toRouter = path.get(i + 1);
                int fromX = 100 + (fromRouter % design.getNoc().getNumRoutersPerDimension()) * 100;
                int fromY = 100 + (fromRouter / design.getNoc().getNumRoutersPerDimension()) * 100;
                int toX = 100 + (toRouter % design.getNoc().getNumRoutersPerDimension()) * 100;
                int toY = 100 + (toRouter / design.getNoc().getNumRoutersPerDimension()) * 100;
                if (drawIndex % 2 == 0)
                    g.setColor(Color.RED);
                else
                    g.setColor(Color.GREEN);

                ((Graphics2D) g).setStroke(new BasicStroke(2));
                g.drawLine(fromX + drawIndex * 5, fromY + drawIndex * 5, toX + drawIndex * 5, toY + drawIndex * 5);
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
