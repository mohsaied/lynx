package lynx.clustering;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Parameter;
import lynx.data.Port;
import lynx.main.DesignData;
import lynx.main.ReportData;

/**
 * Algorithms to cluster an application into coarse-grained blocks
 *
 * @author Mohamed
 * 
 */
public class NocClustering {

    private static final Logger log = Logger.getLogger(NocClustering.class.getName());

    public static void clusterDesign() {
        long startTime = System.nanoTime();

        ReportData.getInstance().writeToRpt("Started Clustering...");

        List<Set<String>> sccs = Tarjan.clusterDesign();
        constructClusteredDesign(sccs);

        long endTime = System.nanoTime();
        DecimalFormat secondsFormat = new DecimalFormat("#.00");
        log.info("Elapsed Time = " + secondsFormat.format((endTime - startTime) / 1e9) + " seconds");

        ReportData.getInstance().writeToRpt(
                "Finished Clustering -- took " + secondsFormat.format((endTime - startTime) / 1e9) + " seconds");

    }

    /**
     * Create a new 'Design' object after clustering
     * 
     * @param stronglyConnectedComponents
     */
    private static void constructClusteredDesign(List<Set<String>> stronglyConnectedComponents) {

        Design design = DesignData.getInstance().getDesign();

        Design clusteredDesign = new Design(design.getName() + "_clustered");

        int index = 0;

        // loop over the SCCs and create a module for each one
        for (Set<String> scc : stronglyConnectedComponents) {

            // create a new module for each scc
            DesignModule cluster = new DesignModule("cluster", "cluster_" + index, index);

            // loop over all modules in this cluster
            for (String modName : scc) {

                DesignModule mod = (DesignModule) design.getModuleByName(modName);

                // export all parameters
                for (Parameter param : mod.getParameters()) {
                    Parameter newParam = param.clone();
                    cluster.addParameter(newParam);
                }

                // export all unbundled ports
                for (Port por : mod.getPorts().values()) {
                    if (!por.isBundled()) {
                        Port newPor = por.clone();
                        cluster.addPort(newPor);
                    }
                }

                // export bundles (and ports) that connect outside cluster only
                for (Bundle bun : mod.getBundles().values()) {
                    if (connectsOutsideCluster(bun, scc)) {
                        Bundle newBun = bun.clone(cluster, scc);
                        cluster.addBundle(newBun);
                    }
                }
            }
        }

        // go over the new bundles and make the right connections
        

        clusteredDesign.update();

        // set the clustered design
        DesignData.getInstance().setClusteredDesign(clusteredDesign);
    }

    private static boolean connectsOutsideCluster(Bundle bun, Set<String> scc) {

        // go into bundle and loop over connections
        for (Bundle conn : bun.getConnections()) {
            if (!scc.contains(conn.getParentModule().getName()))
                return true;
        }

        return false;
    }
}
