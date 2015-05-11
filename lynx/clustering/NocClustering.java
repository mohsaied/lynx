package lynx.clustering;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Clustering;
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

    /**
     * Struct to cache the values of a connection during cluster formation
     */
    static class ClusterConnection {
        int srcCluster;
        DesignModule srcModule;
        Bundle srcBundle;

        int dstCluster;
        DesignModule dstModule;
        Bundle dstBundle;
    }

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
        ReportData.getInstance().writeToRpt("cluster_time = " + secondsFormat.format((endTime - startTime) / 1e9));

    }

    /**
     * Create a new 'Design' object after clustering
     * 
     * @param stronglyConnectedComponents
     */
    private static void constructClusteredDesign(List<Set<String>> stronglyConnectedComponents) {

        Design design = DesignData.getInstance().getDesign();

        Design clusteredDesign = new Design(design.getName() + "_clustered");

        Clustering clustering = new Clustering();

        int index = 0;
        ArrayList<ClusterConnection> connections = new ArrayList<ClusterConnection>();

        // loop over the SCCs and create a module for each one
        for (Set<String> scc : stronglyConnectedComponents) {

            // create a new module for each scc
            DesignModule cluster = new DesignModule("cluster", "cluster_" + index, index);

            // create a list of modules in this cluster
            List<DesignModule> mods = new ArrayList<DesignModule>();

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

                        // hash the connections in this bundle
                        // then add it back to clusters after formation
                        // format is "cluster_index.module_name.bundle_name"
                        for (Bundle con : bun.getConnections()) {
                            ClusterConnection currentClusterConnection = formatConnectionString(index, bun, con,
                                    stronglyConnectedComponents);
                            if (currentClusterConnection != null)
                                connections.add(currentClusterConnection);
                        }
                    }
                }

                // add this module to the list of modules in this cluster
                mods.add(mod);
            }

            // now add cluster + its list of modules to our Clustering object
            clustering.addCluster(cluster, mods, design);

            // add cluster to clusteredDesign
            clusteredDesign.addModule(cluster);

            // next cluster index is incremented
            index++;
        }

        // go over hashed connections and connect the clusters to each other
        for (ClusterConnection con : connections) {
            // find the new clustered bundle
            DesignModule srcMod = (DesignModule) clusteredDesign.getModuleByName("cluster_" + con.srcCluster);
            Bundle srcBun = srcMod.getBundleByName(con.srcModule.getName() + "^" + con.srcBundle.getName());

            DesignModule dstMod = (DesignModule) clusteredDesign.getModuleByName("cluster_" + con.dstCluster);
            Bundle dstBun = dstMod.getBundleByName(con.dstModule.getName() + "^" + con.dstBundle.getName());

            // make the connection
            srcBun.addConnection(dstBun);
        }

        // finally, run an update to populate the data structures
        clusteredDesign.update();

        // set the clustered design
        // TODO currently just check if the # of clusters are equal to the
        // number of design modules, we assume that no clustering happened and
        // we duplicate design into clustered design to avoid long undescriptive
        // names - might want to put a safer check here
        if (design.getNumDesignModules() == clusteredDesign.getNumDesignModules())
            DesignData.getInstance().setClusteredDesign(design);
        else
            DesignData.getInstance().setClusteredDesign(clusteredDesign);
    }

    /**
     * Takes the current bundle info and finds the bundle info for the
     * connections of that bundle. If both modules are in the same cluster a
     * null object is returned
     * 
     * @param srcClusterIndex
     * @param srcBundle
     * @param dstBundle
     * @param stronglyConnectedComponents
     * @return
     */
    private static ClusterConnection formatConnectionString(int srcClusterIndex, Bundle srcBundle, Bundle dstBundle,
            List<Set<String>> stronglyConnectedComponents) {

        ClusterConnection currClusterConnection = new ClusterConnection();

        // src info
        currClusterConnection.srcCluster = srcClusterIndex;
        currClusterConnection.srcModule = srcBundle.getParentModule();
        currClusterConnection.srcBundle = srcBundle;

        // third find the cluster index
        int clusterIndex = 0;
        int dstClusterIndex = -1;
        // look for module
        for (Set<String> scc : stronglyConnectedComponents) {

            for (String destModName : scc) {
                if (destModName.equals(dstBundle.getParentModule().getName())) {
                    // found the module in this cluster
                    dstClusterIndex = clusterIndex;
                }
            }

            clusterIndex++;
        }

        assert dstClusterIndex != -1 : "Didn't find module " + dstBundle.getParentModule().getName()
                + " in any cluster, something must be wrong!";

        // dst info
        currClusterConnection.dstCluster = dstClusterIndex;
        currClusterConnection.dstModule = dstBundle.getParentModule();
        currClusterConnection.dstBundle = dstBundle;

        if (dstClusterIndex != srcClusterIndex)
            return currClusterConnection;
        else
            return null;
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
