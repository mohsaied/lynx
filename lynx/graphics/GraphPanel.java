package lynx.graphics;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.MyEnums.Direction;

public class GraphPanel extends JPanel {

    private static final long serialVersionUID = 2L;

    Design design;

    public GraphPanel(Design design) {
        super(new GridLayout(1, 1));
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
        Map<String, Object> vertices = new HashMap<String, Object>();
        int i = 0;
        for (DesignModule mod : design.getDesignModules().values()) {
            Object vertex = graph.insertVertex(parent, null, mod.getName(), 100 + 250 * i++, 100, 100, 75);
            vertices.put(mod.getName(), vertex);
            /*
            for(Bundle bun:mod.getBundles().values()){
                Object bundle = graph.insertVertex(vertex, null, bun.getName(), 0, 0, 50, 25);
            }
            */
        }

        for (DesignModule mod : design.getDesignModules().values()) {
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
        this.add(graphComponent);
    }
}
