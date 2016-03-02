package lynx.graphics;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

import lynx.data.Bundle;
import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.nocmapping.Mapping;

public class NocPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	final int PORT_DIAMETER = 40;
	final int PORT_RADIUS = 11;

	private static final Logger log = Logger.getLogger(NocPanel.class.getName());

	private static final int routerSpacing = 150;
	private static final int xOffset = 80;
	private static final int yOffset = 50;

	private Design design;
	private Noc noc;
	private int selectedMapping = 0;
	private int selectedVersion = 0;

	JComboBox<Integer> mappingIndex;
	JComboBox<Integer> versionIndex;

	// additional variables added
	protected static mxGraph graph;
	// maps to each link its total usage (both forwards and backwards)
	Map<String, Integer> linkUsageMap;
	// maps to each router index the router mxCell object
	Map<Integer, Object> routerMap;
	// maps to each router index a map of bundles (String bundle name -> Bundle
	// bun)
	Map<Integer, HashSet<Bundle>> routerBunMap;
	// maps to each router index a list modules that correspond to it
	Map<Integer, List<DesignModule>> routerModMap;
	// maps to each edge of the noc a list of connections that use it
	Map<String, List<Connection>> linkConnMap;
	// maps to each connection the mxCell edge object that corresponds to the
	// used noc link
	protected static Map<String, Object> connLinkMap;
	// list of possible geometries for the modules at each router
	List<mxGeometry> geoList;

	public NocPanel(Design design, Noc noc) {
		super(new GridLayout(1, 1));
		log.setLevel(Level.ALL);
		this.design = design;
		this.noc = noc;

		// disabling drag and drop for all cells
		graph = new mxGraph() {
			public boolean isCellSelectable(Object cell) {
				return false;
			}
		};

		// sets the 7 possible configurations of the bundles at the routers
		geoList = new ArrayList<mxGeometry>();
		mxGeometry geo1 = new mxGeometry(1, 0.25, PORT_DIAMETER, PORT_RADIUS);
		geo1.setRelative(true);
		geoList.add(geo1);
		geo1 = new mxGeometry(1, 0.50, PORT_DIAMETER, PORT_RADIUS);
		geo1.setRelative(true);
		geoList.add(geo1);
		geo1 = new mxGeometry(1, 0.75, PORT_DIAMETER, PORT_RADIUS);
		geo1.setRelative(true);
		geoList.add(geo1);
		geo1 = new mxGeometry(1, 1, PORT_DIAMETER, PORT_RADIUS);
		geo1.setRelative(true);
		geoList.add(geo1);
		geo1 = new mxGeometry(-0.6, 1, PORT_DIAMETER, PORT_RADIUS);
		geo1.setRelative(true);
		geoList.add(geo1);
		geo1 = new mxGeometry(-0.6, 0.75, PORT_DIAMETER, PORT_RADIUS);
		geo1.setRelative(true);
		geoList.add(geo1);
		geo1 = new mxGeometry(-0.6, 0.50, PORT_DIAMETER, PORT_RADIUS);
		geo1.setRelative(true);
		geoList.add(geo1);
	}

	public void setDesign(Design design) {
		this.design = design;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (noc != null)
			drawNoc(g, noc);
		if (design != null && design.getMappings() != null)
			drawDesign(g);
	}

	private void drawDesign(Graphics g) {
		Mapping currMapping = design.getMappings().get(selectedMapping).get(selectedVersion);
		routerBunMap = new HashMap<Integer, HashSet<Bundle>>();
		routerModMap = new HashMap<Integer, List<DesignModule>>();
		int i = 0;
		for (HashSet<Bundle> bunSet : currMapping.getBundlesAtRouters()) {
			routerBunMap.put(i, bunSet);
			drawModules(g, bunSet, i++);
		}
	}

	// draw all the modules the correspond to each router
	private void drawModules(Graphics g, HashSet<Bundle> bunSet, int router) {
		mxGeometry geo = new mxGeometry(1, 1, PORT_DIAMETER, PORT_DIAMETER);
		geo.setRelative(true);
		graph.getModel().beginUpdate();
		int counter = 2;
		List<DesignModule> moduleList = new ArrayList<DesignModule>();
		// iterating through each bundle and finding their parent module
		// if their parent module has already been added to a router
		// do not draw the module again
		for (Bundle bun : bunSet) {
			DesignModule parentMod = bun.getParentModule();
			if (!moduleList.contains(parentMod)) {
				moduleList.add(parentMod);
				mxCell mod = new mxCell(parentMod.getName(), geoList.get(counter), "shape=rectangle;");
				mod.setVertex(true);
				graph.addCell(mod, routerMap.get(router));
				counter++;
			}
		}
		routerModMap.put(router, moduleList);
		graph.getModel().endUpdate();
	}

	// helper function to generate labels for each edge to show link usage
	private String generateLabel(int index1, int index2, Map<String, Integer> linkUsageMap) {
		String label = null;
		if (linkUsageMap.get(String.valueOf(index1) + " " + String.valueOf(index2)) != null
				&& linkUsageMap.get(String.valueOf(index2) + " " + String.valueOf(index1)) != null) {
			label = "Link Use " + String.valueOf(index1) + "->" + String.valueOf(index2) + ": "
					+ linkUsageMap.get(String.valueOf(index1) + " " + String.valueOf(index2)) + "\n" + "Link Use "
					+ String.valueOf(index2) + "->" + String.valueOf(index1) + ": "
					+ linkUsageMap.get(String.valueOf(index2) + " " + String.valueOf(index1)) + "\n";
		} else if (linkUsageMap.get(String.valueOf(index1) + " " + String.valueOf(index2)) != null) {
			label = "Link Use " + String.valueOf(index1) + "->" + String.valueOf(index2) + ": "
					+ linkUsageMap.get(String.valueOf(index1) + " " + String.valueOf(index2));
		} else if (linkUsageMap.get(String.valueOf(index2) + " " + String.valueOf(index1)) != null) {
			label = "Link Use " + String.valueOf(index2) + "->" + String.valueOf(index1) + ": "
					+ linkUsageMap.get(String.valueOf(index2) + " " + String.valueOf(index1));
		}
		return label;
	}

	private void drawNoc(Graphics g, Noc noc) {
		int numRoutersPerDimension = noc.getNumRoutersPerDimension();
		int rIndex = 0;
		routerMap = new HashMap<Integer, Object>();
		Object parent = graph.getDefaultParent();
		Mapping currMapping = design.getMappings().get(selectedMapping).get(selectedVersion);
		linkUsageMap = new HashMap<String, Integer>();
		linkConnMap = new HashMap<String, List<Connection>>();
		connLinkMap = new HashMap<String, Object>();

		graph.getModel().beginUpdate();
		// drawing the routers
		for (int j = 0; j < numRoutersPerDimension; j++) {
			for (int i = 0; i < numRoutersPerDimension; i++) {
				int x = xOffset + i * routerSpacing;
				int y = yOffset + j * routerSpacing;
				Object router = graph.insertVertex(parent, null, rIndex, x, y, 60, 60, "shape=ellipse");
				// setting the collapsed size to be the same as its uncollapsed
				// size
				((mxCell) router).getGeometry().setAlternateBounds(new mxRectangle(0.5, 0.5, 60, 60));
				routerMap.put(rIndex, router);
				rIndex++;
			}
		}

		// determining for each link, the number of connections
		for (Connection con : design.getConnections()) {
			List<Integer> path = design.getMappings().get(selectedMapping).get(selectedVersion).getConnectionPath(con);
			for (int i = 0; i < path.size() - 1; i++) {
				int fromRouter = path.get(i);
				int toRouter = path.get(i + 1);
				String linkConnMapKey = String.valueOf(fromRouter < toRouter ? fromRouter : toRouter) + " "
						+ String.valueOf(fromRouter > toRouter ? fromRouter : toRouter);
				if (linkUsageMap.get(String.valueOf(fromRouter) + " " + String.valueOf(toRouter)) != null) {
					int pathUsage = linkUsageMap.get(String.valueOf(fromRouter) + " " + String.valueOf(toRouter));
					pathUsage += 1;
					linkUsageMap.put(String.valueOf(fromRouter) + " " + String.valueOf(toRouter), pathUsage);
				} else {
					linkUsageMap.put(String.valueOf(fromRouter) + " " + String.valueOf(toRouter), 1);
				}
				if (linkConnMap.get(linkConnMapKey) != null) {
					linkConnMap.get(linkConnMapKey).add(con);
				} else {
					List<Connection> connList = new ArrayList<Connection>();
					connList.add(con);
					linkConnMap.put(linkConnMapKey, connList);
				}
			}
		}

		// drawing the links
		for (int i = 0; i < Math.pow(numRoutersPerDimension, 2); i++) {
			// drawing horizontal links
			String label = null;
			String id = null;
			if (i % numRoutersPerDimension != numRoutersPerDimension - 1) {
				label = generateLabel(i, i + 1, linkUsageMap);
				id = String.valueOf(i) + " " + String.valueOf(i + 1);
				mxCell cell = (mxCell) graph.insertEdge(parent, id, label, routerMap.get(i), routerMap.get(i + 1),
						"endArrow=none;");
				// makes the edge black
				graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#000000", new Object[] { cell });
			}
			// drawing vertical links
			if (i <= numRoutersPerDimension * (numRoutersPerDimension - 1)) {
				label = generateLabel(i, numRoutersPerDimension + i, linkUsageMap);
				id = String.valueOf(i) + " " + String.valueOf(numRoutersPerDimension + i);
				mxCell cell = (mxCell) graph.insertEdge(parent, id, label, routerMap.get(i),
						routerMap.get(numRoutersPerDimension + i), "endArrow=none;");
				graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#000000", new Object[] { cell });
			}
		}

		// determining the mapping for connection to the mxCell edge object that
		// corresponds to the link being used
		for (Connection con : design.getConnections()) {
			List<Integer> path = design.getMappings().get(selectedMapping).get(selectedVersion).getConnectionPath(con);
			for (int i = 0; i < path.size() - 1; i++) {
				System.out.println(con.toString());
				int fromRouter = path.get(i);
				int toRouter = path.get(i + 1);
				String linkConnMapKey = String.valueOf(fromRouter < toRouter ? fromRouter : toRouter) + " "
						+ String.valueOf(fromRouter > toRouter ? fromRouter : toRouter);
				connLinkMap.put(con.toString(), ((mxGraphModel) graph.getModel()).getCell(linkConnMapKey));
			}
		}

		// adding items into drop down menu for each link
		for (String connString : connLinkMap.keySet()) {
			MainPanel.mappingIndex.addItem(connString);
		}
		graph.getModel().endUpdate();

		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		// disabling drag and drop edge creation
		graphComponent.setConnectable(false);
		this.add(graphComponent);

		// mouse listener to obtain additional information about module
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				Object cell = graphComponent.getCellAt(e.getX(), e.getY());
				if (cell != null) {
					// clearing all text first
					MainPanel.nocInfo.setText("");
					try {
						// checking if a router has been clicked
						int routerNum = Integer.parseInt(graph.getLabel(cell));
						Object router = routerMap.get(routerNum);
						if (router != null) {
							MainPanel.nocInfo.append("Router Name: " + graph.getLabel(cell));
							MainPanel.nocInfo.append("\nTDM Factor: " + noc.getTdmFactor());
							MainPanel.nocInfo.append("\nIn Width: " + noc.getNocBundleInWidth());
							MainPanel.nocInfo.append("\nOut Width: " + noc.getNocBundleOutWidth());
							MainPanel.nocInfo.append("\nThese are the modules contained in the selected router: ");
							for (DesignModule mod : routerModMap.get(Integer.parseInt(graph.getLabel(cell)))) {
								MainPanel.nocInfo.append("\n" + "Module Name: " + mod.getName());
							}
						}
					} catch (Exception e2) {
						// checking if a module has been clicked
						DesignModule clickedMod = design.getDesignModules().get(graph.getLabel(cell));
						if (clickedMod != null) {
							MainPanel.nocInfo.append("Module Name: " + clickedMod.getName());
							MainPanel.nocInfo.append("\nThese are the bundles contained in the selected module: ");
							for (Bundle bun : clickedMod.getBundles().values()) {
								MainPanel.nocInfo.append("\n" + "Bundle Name: " + bun.getFullName());
								MainPanel.nocInfo.append("\n" + "FPSlots: ");
								for (NocBundle nocBun : currMapping.getBundleMap().get(bun)) {
									MainPanel.nocInfo.append("\n" + nocBun.toString());
								}
							}
						}
					}
					// checking if a link has been clicked
					for (String key : linkConnMap.keySet()) {
						if (key.equals(((mxCell) cell).getId())) {
							List<Connection> connList = linkConnMap.get(key);
							MainPanel.nocInfo.append("List of connections using this link");
							for (Connection con : connList) {
								MainPanel.nocInfo.append("\n" + con.toString());
							}
						}
					}
				}
			}
		});
	}
}
