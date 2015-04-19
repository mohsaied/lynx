package lynx.graphics;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.MyEnums.Direction;
import lynx.main.DesignData;

public class ClusteredGraphPanel extends JPanel {

    private static final long serialVersionUID = 2L;

    Design design;

    public ClusteredGraphPanel() {
        super(new GridLayout(1, 1));
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

        // first create the clusters
        List<Object> clusters = new ArrayList<Object>();
        for (int x = 0; x < design.getClusters().size(); x++) {
            Object vertex = graph.insertVertex(parent, null, "cluster" + x, 0, 0, 200, 200, "fillColor=#A9C9A4;");
            clusters.add(vertex);
        }

        // populate vertices
        Map<String, Object> vertices = new HashMap<String, Object>();
        int[] iArr = new int[design.getClusters().size()];
        int[] jArr = new int[design.getClusters().size()];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = 0;
            jArr[i] = 0;
        }

        for (DesignModule mod : design.getDesignModules().values()) {

            // look for this module in the clusters list and set the parent
            Object currParent = graph.getDefaultParent();
            int currClusterIndex = 0;
            for (int x = 0; x < design.getClusters().size(); x++) {
                Set<String> clusterSet = design.getClusters().get(x);
                for (String currModName : clusterSet) {
                    if (mod.getName().equals(currModName)) {
                        currParent = clusters.get(x);
                        currClusterIndex = x;
                        break;
                    }
                }
            }

            // Object vertex = graph.insertVertex(currParent, null,
            // mod.getName(), 100*i++, 75*j++, 75, 50);
            Object vertex = graph.insertVertex(currParent, null, mod.getName(), 10 + 100 * iArr[currClusterIndex]++,
                    10 + 100 * jArr[currClusterIndex], 75, 50);
            vertices.put(mod.getName(), vertex);

            if (iArr[currClusterIndex] % 2 == 0) {
                jArr[currClusterIndex]++;
                iArr[currClusterIndex] = 0;
            }
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
        new mxOrganicLayout(graph).execute(graph.getDefaultParent());
        this.add(graphComponent);
    }
}
