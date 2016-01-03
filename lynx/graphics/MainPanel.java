package lynx.graphics;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import lynx.analysis.Analysis;
import lynx.data.Design;
import lynx.data.Noc;
import lynx.main.DesignData;

public class MainPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public static final int GRAPHTABID = 0;
	public static final int CLUSTERTABID = 1;
	public static int MAPTABID = 2;
	public static int ANNEALTABID = 3;
	public static int PERFTABID = 4;
	public static int CHARTTABID = 5;

	// tabbed panel
	JTabbedPane tabbedPane;

	// tabs
	private GraphPanel graphPanel;
	private ClusteredGraphPanel clusteredGraphPanel;
	private NocPanel nocPanel;
	private PlotPanel chartPanel1;
	private PerfPanel perfPanel;
	private PlotAnalysisPanel chartPanel2;
	
	//text boxes
	protected static JTextArea bundleInfo;
	private static JTextArea bundleInfoTitle;
	
	private static final Logger log = Logger.getLogger(CommandPanel.class.getName());

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
		
		//adding JTextArea for displaying bundle properties
		bundleInfo = new JTextArea();
		bundleInfo.setLineWrap(true);
		
		//adding JTextArea for displaying a title for bundle properties
		bundleInfoTitle = new JTextArea();
		Font font = new Font("Verdana", Font.BOLD, 20);
		bundleInfoTitle.setFont(font);
		bundleInfoTitle.setText("Selected Bundle Properties");
		bundleInfoTitle.setLineWrap(true);

		JScrollPane titlePane = new JScrollPane(bundleInfoTitle);
		JScrollPane infoPane = new JScrollPane(bundleInfo);
		JSplitPane infoSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, titlePane, infoPane);
		infoSplitPane.setResizeWeight(.05d);
		JSplitPane graphPanelFinal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphPanel, infoSplitPane);
		graphPanelFinal.setResizeWeight(.85d);
		
		tabbedPane.addTab("Graph", null, graphPanelFinal, "Visualize the provided connectivity graph");
		tabbedPane.setMnemonicAt(GRAPHTABID, KeyEvent.VK_1);
	}

	public void addClusterTab() {
		// graph after clustering
		// setting a condition for whether clusters will be shown or not
		if (DesignData.getInstance().getDesign().getNumModules() > DesignData.getInstance().getDesign().getClusters()
				.size()) {
			clusteredGraphPanel = new ClusteredGraphPanel();
			tabbedPane.addTab("Clustered Graph", null, clusteredGraphPanel,
					"Coarse application graph after clustering");
			tabbedPane.setMnemonicAt(CLUSTERTABID, KeyEvent.VK_1);
			// changing the ID numbers in response to a removed cluster panel
		} else {
			MAPTABID = 1;
			ANNEALTABID = 2;
			PERFTABID = 3;
			CHARTTABID = 4;
		}
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
