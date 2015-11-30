package lynx.graphics;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.MyEnums.Direction;
import lynx.main.DesignData;

public class GraphPanel extends JPanel {

	private static final long serialVersionUID = 2L;

	Design design;

	public GraphPanel() {
		super(new GridLayout(1, 1));
		this.setSize(2000, 700);
		this.design = DesignData.getInstance().getDesign();
	}

	public void setDesign(Design design) {
		this.design = design;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (design != null)
			drawConnectivityGraph(g);
	}

	private void drawConnectivityGraph(Graphics g) {

		mxGraph graph = new mxGraph();
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();

		// populate vertices
		// is the vertex the vertex of each square?
		Map<String, Object> vertices = new HashMap<String, Object>();
		int i = 0;
		int j = 0;
		for (DesignModule mod : design.getDesignModules().values()) {
			Object vertex = graph.insertVertex(parent, null, mod.getName(), 100 + 150 * i++, 100 + 150 * j, 100, 75);
			vertices.put(mod.getName(), vertex);
			if (i % 3 == 0) {
				j++;
				i = 0;
			}
			/*
			 * for(Bundle bun:mod.getBundles().values()){ Object bundle =
			 * graph.insertVertex(vertex, null, bun.getName(), 0, 0, 50, 25); }
			 */
		}

		for (DesignModule mod : design.getDesignModules().values()) {
			System.out.println("graph modules");
			String fromMod = mod.getName();
			for (Bundle fromBun : mod.getBundles().values()) {
				if (fromBun.getDirection() == Direction.OUTPUT) {
					for (Bundle toBun : fromBun.getConnections()) {
						String toMod = toBun.getParentModule().getName();
						graph.insertEdge(parent, null, null, vertices.get(fromMod), vertices.get(toMod));
					}
				}
			}
		}

		graph.getModel().endUpdate();

		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		mxFastOrganicLayout lo = new mxFastOrganicLayout(graph);
		lo.setUseBoundingBox(true);
		lo.execute(graph.getDefaultParent());
		this.add(graphComponent);
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter()
        {
        
            public void mouseReleased(MouseEvent e)
            {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                
                if (cell != null)
                {
                    System.out.println("cell="+graph.getLabel(cell));
                }
            }
        });
	}
}
