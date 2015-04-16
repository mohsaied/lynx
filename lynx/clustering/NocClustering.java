package lynx.clustering;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import lynx.data.Design;

/**
 * Algorithms to cluster an application into coarse-grained blocks
 *
 * @author Mohamed
 * 
 */
public class NocClustering {
    
    private static final Logger log = Logger.getLogger(NocClustering.class.getName());

    public static void clusterDesign(Design design) {
        long startTime = System.nanoTime();
        
        Tarjan.clusterDesign(design);
        
        long endTime = System.nanoTime();
        DecimalFormat secondsFormat = new DecimalFormat("#.00");
        log.info("Elapsed Time = " + secondsFormat.format((endTime - startTime) / 1e9) + " seconds");
    }
}
