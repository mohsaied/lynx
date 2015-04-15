package lynx.graphics;

import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import lynx.data.Design;

public class MainPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // tabbed panel
    JTabbedPane tabbedPane;

    // tabs
    private GraphPanel graphPanel;
    private ClusteredGraphPanel clusteredGraphPanel;
    private NocPanel nocPanel;
    private PlotPanel chartPanel;

    public MainPanel(Design design) {
        super(new GridLayout(1, 1));

        // panel has a tabbed pane on it
        tabbedPane = new JTabbedPane();
        this.add(tabbedPane);

        // The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    public void addGraphTab(Design design) {
        // graph of given application
        graphPanel = new GraphPanel(design);
        tabbedPane.addTab("Graph", null, graphPanel, "Visualize the provided connectivity graph");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
    }

    public void addClusterTab(Design design) {
        // graph after clustering
        clusteredGraphPanel = new ClusteredGraphPanel(design);
        tabbedPane.addTab("Clustered Graph", null, clusteredGraphPanel, "Coarse application graph after clustering");
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_1);
        tabbedPane.setSelectedIndex(1);
    }

    public void addNoCTabs(Design design) {
        // NoC
        nocPanel = new NocPanel(design);
        tabbedPane.addTab("NoC", null, nocPanel, "The NoC topology and module placement thereon");
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_1);

        // charts
        chartPanel = new PlotPanel(design);
        tabbedPane.addTab("Charts", null, chartPanel, "Charts visualizing the simulated annealing");
        tabbedPane.setMnemonicAt(3, KeyEvent.VK_1);
        tabbedPane.setSelectedIndex(2);
    }

    public void clearTabs() {
        tabbedPane.removeAll();
    }

}
