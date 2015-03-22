package lynx.clustering;

import lynx.data.Design;

/**
 * Algorithms to cluster an application into coarse-grained blocks
 *
 * @author Mohamed
 * 
 */
public class NocClustering {

    public static void clusterDesign(Design design) {
        Tarjan.clusterDesign(design);
    }
}
