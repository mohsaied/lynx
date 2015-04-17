package lynx.nocmapping;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import lynx.data.Design;
import lynx.main.ProgramData;

/**
 * Algorithms to map a design onto an NoC
 *
 * @author Mohamed
 * 
 */
public class NocMapping {

    private static final Logger log = Logger.getLogger(NocMapping.class.getName());

    public static void findMappings(Design design) {
        long startTime = System.nanoTime();

        ProgramData.getInstance().writeToRpt("Started Mapping...");
        // Ullman.findMappings(design);

        SimulatedAnnealing.findMappings(design);

        long endTime = System.nanoTime();
        DecimalFormat secondsFormat = new DecimalFormat("#.00");
        log.info("Elapsed Time = " + secondsFormat.format((endTime - startTime) / 1e9) + " seconds");

        ProgramData.getInstance().writeToRpt(
                "Finished Mapping -- took " + secondsFormat.format((endTime - startTime) / 1e9) + " seconds");

        ProgramData.getInstance().closeRpt();
    }

}
