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

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.Noc;
import lynx.data.Bundle;
import lynx.nocmapping.Mapping;

public class NocPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	final int PORT_DIAMETER = 40;
    final int PORT_RADIUS = 11;

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
	
	mxGraph graph;
	Map<Integer, Object> routerMap;
	List<mxGeometry> geoList;

	public NocPanel(Design design, Noc noc) {
		super(new GridLayout(1, 1));
		log.setLevel(Level.ALL);
		this.design = design;
		this.noc = noc;
		graph = new mxGraph() {
            public boolean isCellSelectable(Object cell) {
                return false;
            }
        };
     // sets the 7 possible configurations of the bundles at the routers
        geoList = new ArrayList<mxGeometry>();
        mxGeometry geo1 = new mxGeometry(1, 1, PORT_DIAMETER, PORT_RADIUS);
        geo1.setRelative(true);
        geoList.add(geo1);
        geo1 = new mxGeometry(1, 0.75, PORT_DIAMETER, PORT_RADIUS);
        geo1.setRelative(true);
        geoList.add(geo1);
        geo1 = new mxGeometry(1, 0.50, PORT_DIAMETER, PORT_RADIUS);
        geo1.setRelative(true);
        geoList.add(geo1);
        geo1 = new mxGeometry(1, 0.25, PORT_DIAMETER, PORT_RADIUS);
        geo1.setRelative(true);
        geoList.add(geo1);
        geo1 = new mxGeometry(0, 1, PORT_DIAMETER, PORT_RADIUS);
        geo1.setRelative(true);
        geoList.add(geo1);
        geo1 = new mxGeometry(0, 0.7, PORT_DIAMETER, PORT_RADIUS);
        geo1.setRelative(true);
        geoList.add(geo1);
        geo1 = new mxGeometry(0, 0.4, PORT_DIAMETER, PORT_RADIUS);
        geo1.setRelative(true);
        geoList.add(geo1);
        
		//initPane();
	}

	public void setDesign(Design design) {
		this.design = design;
		//initPane();
	}
	
	/*
	private void initPane() {
		if (design != null && design.getMappings() != null) {
			controlPanel = new JPanel(new GridLayout(1, 1));
			controlPanel.setBounds(0, 0, 15, 10);
			this.add(controlPanel);
		}
	}
	*/

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (noc != null)
			drawNoc(g, noc);

		if (design != null && design.getMappings() != null)
			drawDesign(g);
	}

	
	private void drawDesign(Graphics g) {

		Mapping currMapping = design.getMappings().get(selectedMapping).get(selectedVersion);

		int i = 0;
		for (HashSet<Bundle> bunSet : currMapping.getBundlesAtRouters()) {
			drawBundles(g, bunSet, i++);
		}

		// draw the connections
		//drawConnections(g, currMapping);
	}
	

	
	private void drawBundles(Graphics g, HashSet<Bundle> bunSet, int router) {

		int i = router % noc.getNumRoutersPerDimension();
		int j = router / noc.getNumRoutersPerDimension();
		int x = xOffset + 15 + i * routerSpacing;
		int y = yOffset + 15 + j * routerSpacing;

		int maxPosibleModules = noc.getTdmFactor()
		        + (noc.getNumVcs() < noc.getTdmFactor() ? noc.getNumVcs() : noc.getTdmFactor());
		
		mxGeometry geo = new mxGeometry(1, 1, PORT_DIAMETER, PORT_DIAMETER);
		geo.setRelative(true);
		graph.getModel().beginUpdate();
        /*
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
		*/
		
		
		int counter = 0;
        for (Bundle bun: bunSet) {
            System.out.println("here");
            mxCell bundle = new mxCell(bun.getName(), geoList.get(counter), "shape=rectangle;");
            bundle.setVertex(true);
            graph.addCell(bundle, routerMap.get(router));
            counter++;
        }
        graph.getModel().endUpdate();
	}
	

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
		int rIndex = 0;
		routerMap = new HashMap<Integer, Object>(); 
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();
		// drawing the routers
		for (int j = 0; j < numRoutersPerDimension; j++) {
			for (int i = 0; i < numRoutersPerDimension; i++) {
				int x = xOffset + i * routerSpacing;
				int y = yOffset + j * routerSpacing;
				Object router = graph.insertVertex(parent, null, rIndex, x, y, 60, 60, "shape=ellipse");
				((mxCell) router).getGeometry().setAlternateBounds(new mxRectangle(0.5,0.5,60,60));
				routerMap.put(rIndex, router);
				rIndex++;
			}
		}
		
		// drawing the links
		for (int i = 0; i < Math.pow(numRoutersPerDimension, 2); i++) {
			// drawing horizontal links
			if(i % numRoutersPerDimension != numRoutersPerDimension - 1) {
				graph.insertEdge(parent, null, null, routerMap.get(i), routerMap.get(i + 1), "endArrow=none;");
			}
			// drawing vertical links
			if(i <= numRoutersPerDimension * (numRoutersPerDimension - 1)) {
				graph.insertEdge(parent, null, null, routerMap.get(i), routerMap.get(numRoutersPerDimension + i), "endArrow=none;");
			}
		}
		graph.getModel().endUpdate();
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		// disabling drag and drop edge creation
		graphComponent.setConnectable(false);
		this.add(graphComponent);
	}
}
