package lynx.graphics;

import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import lynx.analysis.Analysis;
import lynx.data.Design;
import lynx.data.Noc;

public class MainPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final int GRAPHTABID = 0;
    public static final int CLUSTERTABID = 1;
    public static final int MAPTABID = 2;
    public static final int ANNEALTABID = 3;
    public static final int PERFTABID = 4;
    public static final int CHARTTABID = 5;

    // tabbed panel
    JTabbedPane tabbedPane;

    // tabs
    private GraphPanel graphPanel;
    private ClusteredGraphPanel clusteredGraphPanel;
    private NocPanel nocPanel;
    private PlotPanel chartPanel1;
    private PerfPanel perfPanel;
    private PlotAnalysisPanel chartPanel2;

    public MainPanel(Design design) {
        super(new GridLayout(1, 1));

        // panel has a tabbed pane on it
        tabbedPane = new JTabbedPane();
        this.add(tabbedPane);

        // The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    public void addGraphTab() {
        // graph of given application
        graphPanel = new GraphPanel();
        tabbedPane.addTab("Graph", null, graphPanel, "Visualize the provided connectivity graph");
        tabbedPane.setMnemonicAt(GRAPHTABID, KeyEvent.VK_1);
    }

    public void addClusterTab() {
        // graph after clustering
        clusteredGraphPanel = new ClusteredGraphPanel();
        tabbedPane.addTab("Clustered Graph", null, clusteredGraphPanel, "Coarse application graph after clustering");
        tabbedPane.setMnemonicAt(CLUSTERTABID, KeyEvent.VK_1);
    }

    public void addNoCTabs(Design design, Noc noc) {
        // NoC
        nocPanel = new NocPanel(design, noc);
        tabbedPane.addTab("NoC", null, nocPanel, "The NoC topology and module placement thereon");
        tabbedPane.setMnemonicAt(MAPTABID, KeyEvent.VK_1);

        // charts
        chartPanel1 = new PlotPanel(design);
        tabbedPane.addTab("Charts 1", null, chartPanel1, "Charts visualizing the simulated annealing");
        tabbedPane.setMnemonicAt(ANNEALTABID, KeyEvent.VK_1);
    }

    public void addPerfTab(Analysis analysis) {
        // NoC
        perfPanel = new PerfPanel(analysis);
        tabbedPane.addTab("Performance", null, perfPanel, "Performance analysis summary");
        tabbedPane.setMnemonicAt(PERFTABID, KeyEvent.VK_1);

        // charts
        chartPanel2 = new PlotAnalysisPanel(analysis);
        tabbedPane.addTab("Charts 2", null, chartPanel2, "Charts of throughput and latency from simulation");
        tabbedPane.setMnemonicAt(CHARTTABID, KeyEvent.VK_1);
    }

    public boolean switchTab(int tabID) {
        if (tabID < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(tabID);
            return true;
        }
        return false;
    }

    public void clearTabs() {
        tabbedPane.removeAll();
    }

    public void removePerfTab() {
        tabbedPane.remove(chartPanel2);
        tabbedPane.remove(perfPanel);
    }

}
