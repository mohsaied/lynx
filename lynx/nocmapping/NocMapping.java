package lynx.nocmapping;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import lynx.data.Design;
import lynx.data.Noc;
import lynx.main.DesignData;
import lynx.main.ReportData;

/**
 * Algorithms to map a design onto an NoC
 *
 * @author Mohamed
 * 
 */
public class NocMapping {

    private static final Logger log = Logger.getLogger(NocMapping.class.getName());

    public static Mapping findMappings(Design design, Noc noc) {
        long startTime = System.nanoTime();

        ReportData.getInstance().writeToRpt("Started Mapping...");

        // Ullman.findMappings(design, noc);

        // SimulatedAnnealingModule.findMappings(design, noc);

        SimulatedAnnealingBundle.findMappings(design, noc);

        // set mapping in singleton
        Mapping mapping = design.getMappings().get(0).get(0);
        DesignData.getInstance().setNocMapping(mapping);

        long endTime = System.nanoTime();
        DecimalFormat secondsFormat = new DecimalFormat("#.00");
        log.info("Elapsed Time = " + secondsFormat.format((endTime - startTime) / 1e9) + " seconds");

        ReportData.getInstance().writeToRpt(
                "Finished Mapping -- took " + secondsFormat.format((endTime - startTime) / 1e9) + " seconds");

        ReportData.getInstance().writeToRpt("map_time = " + secondsFormat.format((endTime - startTime) / 1e9));

        ReportData.getInstance().closeRpt();

        return mapping;
    }

}
