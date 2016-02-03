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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.Noc;
import lynx.data.Bundle;
import lynx.nocmapping.Mapping;

public class NocPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(NocPanel.class.getName());

	private static final int routerSpacing = 150;
	private static final int bunSize = 140;
	private static final int xOffset = 80;
	private static final int yOffset = 50;

	private Design design;
	private Noc noc;
	private int selectedMapping = 0;
	private int selectedVersion = 0;

	private JPanel controlPanel;
	JComboBox<Integer> mappingIndex;
	JComboBox<Integer> versionIndex;

	public NocPanel(Design design, Noc noc) {
		super(new FlowLayout());
		log.setLevel(Level.ALL);
		this.design = design;
		this.noc = noc;

		initPane();
	}

	public void setDesign(Design design) {
		this.design = design;
		initPane();
	}

	private void initPane() {

		if (design != null && design.getMappings() != null) {
			controlPanel = new JPanel(new GridLayout(1, 1));
			controlPanel.setBounds(0, 0, 15, 10);
			this.add(controlPanel);

		}
	}

	/*
	protected void compareToBestMappingWithConsolePrint(Design design, int selectedMapping, int selectedVersion) {
		// TODO compare traffic

		Mapping bestMapping = design.getMappings().get(0).get(0);
		Mapping currMapping = design.getMappings().get(selectedMapping).get(selectedVersion);

		List<Connection> connections = design.getConnections();

		for (Connection con : connections) {
			int bestLat = bestMapping.getConnectionPath(con).size() - 1;
			int currLat = currMapping.getConnectionPath(con).size() - 1;

			if (bestLat < currLat)
				log.warning("Selected Mapping (" + selectedMapping + ") has increased latency " + currLat
				        + ", instead of " + bestLat + " on connection between " + con.getFromModule().getName() + "-->"
				        + con.getToModule().getName());
			else if (bestLat > currLat)
				log.warning("Selected Mapping (" + selectedMapping + ") has decreased latency " + currLat
				        + ", instead of " + bestLat + " on connection between " + con.getFromModule().getName() + "-->"
				        + con.getToModule().getName());
		}
	}
	*/

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (noc != null)
			drawNoc(g, noc);

		//if (design != null && design.getMappings() != null)
			//drawDesign(g);
	}

	/*
	private void drawDesign(Graphics g) {

		Mapping currMapping = design.getMappings().get(selectedMapping).get(selectedVersion);

		int i = 0;
		for (HashSet<Bundle> bunSet : currMapping.getBundlesAtRouters()) {
			drawBundles(g, bunSet, i++);
		}

		// draw the connections
		//drawConnections(g, currMapping);
	}
	*/

	/*
	private void drawBundles(Graphics g, HashSet<Bundle> bunSet, int router) {

		int i = router % noc.getNumRoutersPerDimension();
		int j = router / noc.getNumRoutersPerDimension();
		int x = xOffset + 15 + i * routerSpacing;
		int y = yOffset + 15 + j * routerSpacing;

		int maxPosibleModules = noc.getTdmFactor()
		        + (noc.getNumVcs() < noc.getTdmFactor() ? noc.getNumVcs() : noc.getTdmFactor());

		boolean switchColor = true;
		for (Bundle bun : bunSet) {
			if (switchColor) {
				g.setColor(Color.CYAN);
			} else {
				g.setColor(Color.ORANGE);
			}
			switchColor = !switchColor;
			g.fillRect(x, y, (int) ((float) bunSize * 0.9), bunSize / maxPosibleModules);
			g.drawRect(x, y, (int) ((float) bunSize * 0.9), bunSize / maxPosibleModules);
			g.setColor(Color.BLACK);

			String name = bun.getFullName();
			if (name.length() > 15) {
				name = name.substring(0, 8);
				name += "..";
			}
			g.drawString(name, x + 5, y + bunSize / 2 / maxPosibleModules);

			y += bunSize / maxPosibleModules;
		}
	}
	*/

	/*
	private void drawConnections(Graphics g, Mapping currMapping) {

		Map<String, List<Integer>> linkIndices = new HashMap<String, List<Integer>>();
		double[][] nocLinks = noc.getAdjacencyMatrix();

		// initialize used link indices (none are used at the start)
		for (int i = 0; i < noc.getNumRouters(); i++) {
			for (int j = 0; j < noc.getNumRouters(); j++) {
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
				int fromX = xOffset + (fromRouter % noc.getNumRoutersPerDimension()) * routerSpacing;
				int fromY = yOffset + (fromRouter / noc.getNumRoutersPerDimension()) * routerSpacing;
				int toX = xOffset + (toRouter % noc.getNumRoutersPerDimension()) * routerSpacing;
				int toY = yOffset + (toRouter / noc.getNumRoutersPerDimension()) * routerSpacing;
				if (drawIndex % 2 == 0)
					g.setColor(Color.RED);
				else
					g.setColor(Color.GREEN);

				((Graphics2D) g).setStroke(new BasicStroke(2));
				g.drawLine(fromX + drawIndex * 5, fromY + drawIndex * 5, toX + drawIndex * 5, toY + drawIndex * 5);
			}
		}
	}

*/
	private void drawNoc(Graphics g, Noc noc) {

		int numRoutersPerDimension = noc.getNumRoutersPerDimension();
		int ymin = yOffset;
		int ymax = yOffset + routerSpacing * (numRoutersPerDimension - 1);
		int xmin = xOffset;
		int xmax = xOffset + routerSpacing * (numRoutersPerDimension - 1);
		int rIndex = 0;
		mxGraph graph = new mxGraph();
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();
		System.out.println(numRoutersPerDimension);
		for (int j = 0; j < numRoutersPerDimension; j++) {
			for (int i = 0; i < numRoutersPerDimension; i++) {
				int x = xOffset + i * routerSpacing;
				int y = yOffset + j * routerSpacing;
				graph.insertVertex(parent, null, rIndex, x, y, 20, 20);
				
				/*
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
				*/
				rIndex++;
				
			}
		}
		graph.getModel().endUpdate();
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		this.add(graphComponent);
	}

}
