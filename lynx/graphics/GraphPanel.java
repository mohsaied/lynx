package lynx.graphics;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxPoint;

//disabled edge editing
import lynx.graphics.mxGraphEdited;

import lynx.data.Bundle;
import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.MyEnums.Direction;
import lynx.main.DesignData;

public class GraphPanel extends JPanel {

    private static final long serialVersionUID = 2L;
    final int PORT_DIAMETER = 20;
    final int PORT_RADIUS = PORT_DIAMETER / 2;

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

        mxGraphEdited graph = new mxGraphEdited();
        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();

        // populate vertices
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

        mxGeometry geo1 = new mxGeometry(0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
        geo1.setRelative(true);
        Map<String, Object> modBunMap = new HashMap<String, Object>();
        for (DesignModule mod : design.getDesignModules().values()) {
            String fromMod = mod.getName();
            for (Bundle Bun : mod.getBundles().values()) {
                mxCell port = new mxCell(Bun.getName(), geo1, "shape=ellipse;perimter=ellipsePerimeter");
                port.setVertex(true);
                modBunMap.put(Bun.getFullName(), port);
            }
        }
        for (DesignModule mod : design.getDesignModules().values()) {
            String fromMod = mod.getName();
            for (Bundle fromBun : mod.getBundles().values()) {
                graph.addCell(modBunMap.get(fromBun.getFullName()), vertices.get(fromMod));
                if (fromBun.getDirection() == Direction.OUTPUT) {
                    for (Bundle toBun : fromBun.getConnections()) {
                        graph.insertEdge(parent, null, null, modBunMap.get(fromBun.getFullName()),
                                modBunMap.get(toBun.getFullName()));
                    }
                }
            }
        }
        // create invisible connections between the modules so that organic
        // layout works
        for (DesignModule mod : design.getDesignModules().values()) {
            String fromMod = mod.getName();
            for (Bundle fromBun : mod.getBundles().values()) {
                if (fromBun.getDirection() == Direction.OUTPUT) {
                    for (Bundle toBun : fromBun.getConnections()) {
                        String toMod = toBun.getParentModule().getName();
                        graph.insertEdge(parent, null, null, vertices.get(fromMod), vertices.get(toMod),
                                "strokeColor=none");
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

        // mouse listener to obtain additional information about module
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                if (cell != null) {

                    DesignModule mod1 = design.getDesignModules().get(graph.getLabel(cell));
                    if (mod1 != null) {
                        if (mod1.getParameters().size() > 0) {
                            for (lynx.data.Parameter params : mod1.getParameters()) {
                                System.out.println("These are the parameters of the module: " + params.getName() + ", "
                                        + params.getValue());
                            }
                        }
                        System.out.println("These are the ports: ");
                        for (String portName : mod1.getPorts().keySet()) {
                            System.out.println(portName);
                        }

                        for (String name : mod1.getBundles().keySet()) {
                            System.out.println(graph.getLabel(cell) + " " + name);
                            Bundle bun = mod1.getBundles().get(name);
                            System.out.println("This is the width of " + name + ":" + " " + bun.getWidth() + ".");
                            if (bun.getDstPort() != null) {
                                System.out.println(
                                        "This is the dst port of " + name + ":" + " " + bun.getDstPort() + ".");
                            }
                            if (bun.getVcPort() != null) {
                                System.out
                                        .println("This is the VC port of " + name + ":" + " " + bun.getVcPort() + ".");
                            }
                        }
                    }
                }
            }
        });
    }

}
