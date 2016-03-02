package lynx.graphics;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import com.mxgraph.util.mxConstants;

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

	// tabs corresponding to each panel in gui
	private GraphPanel graphPanel;
	private ClusteredGraphPanel clusteredGraphPanel;
	private NocPanel nocPanel;
	private PlotPanel chartPanel1;
	private PerfPanel perfPanel;
	private PlotAnalysisPanel chartPanel2;

	// text boxes for displaying properties in nocPanel and graphPanel
	protected static JTextArea bundleInfo;
	private static JTextArea bundleInfoTitle;
	protected static JTextArea nocInfo;
	private static JTextArea nocInfoTitle;

	// drop down menu for nocPanel when selecting edges
	protected static JComboBox<String> mappingIndex;

	@SuppressWarnings("unused")
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

		// adding JTextArea for displaying bundle properties
		bundleInfo = new JTextArea();
		bundleInfo.setLineWrap(true);

		// adding JTextArea for displaying a title for bundle properties
		bundleInfoTitle = new JTextArea();
		Font font = new Font("Verdana", Font.BOLD, 20);
		bundleInfoTitle.setFont(font);
		bundleInfoTitle.setText("Selected Component Properties");
		bundleInfoTitle.setLineWrap(true);

		// adding the two JTextAreas to the final graphPanel
		JScrollPane titlePane = new JScrollPane(bundleInfoTitle);
		JScrollPane infoPane = new JScrollPane(bundleInfo);
		JSplitPane infoSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, titlePane, infoPane);

		// sets the default proportion of the panel given to the properties and
		// the title
		infoSplitPane.setResizeWeight(.05d);
		JSplitPane graphPanelFinal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphPanel, infoSplitPane);

		// sets the default proportion of the panel given to the textboxes and
		// the actual noc
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

		// adding JTextArea for displaying bundle properties
		nocInfo = new JTextArea();
		nocInfo.setLineWrap(true);

		// adding JTextArea for displaying a title for bundle properties
		nocInfoTitle = new JTextArea();
		Font font = new Font("Verdana", Font.BOLD, 20);
		nocInfoTitle.setFont(font);
		nocInfoTitle.setText("Selected Component Properties");
		nocInfoTitle.setLineWrap(true);

		// adding a split pane to show both the noc + information about specific
		// bundles
		JPanel propertiesHeaderPane = new JPanel(new GridLayout(1, 1));

		// adding listener to drop down menu in nocPanel to change color of
		// edges when items are selected
		mappingIndex = new JComboBox<String>();
		mappingIndex.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				if (event.getSource() == mappingIndex) {
					for (Object c : NocPanel.connLinkMap.values()) {
						// resets all edge colors to black
						NocPanel.graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#000000", new Object[] { c });
					}
					String selectedItem = (String) mappingIndex.getSelectedItem();
					Object selectedEdge = NocPanel.connLinkMap.get(selectedItem);
					// sets edge corresponding to selected edge to red
					NocPanel.graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FF0000",
							new Object[] { selectedEdge });
				}
			}
		});
		propertiesHeaderPane.add(mappingIndex);

		JScrollPane propertiesPane = new JScrollPane(nocInfo);
		JSplitPane sidebarPropertiesPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, propertiesHeaderPane,
				propertiesPane);

		// sets the default proportion of the panel given to the properties and
		// the title
		sidebarPropertiesPane.setResizeWeight(.05d);
		JSplitPane nocPanelFinal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nocPanel, sidebarPropertiesPane);

		// sets the default proportion of the panel given to the textboxes and
		// the actual noc
		nocPanelFinal.setResizeWeight(.95d);

		tabbedPane.addTab("NoC", null, nocPanelFinal, "The NoC topology and module placement thereon");
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
