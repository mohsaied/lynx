package lynx.vcmapping;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.Noc;
import lynx.data.MyEnums.Direction;
import lynx.main.ReportData;
import lynx.nocmapping.Mapping;

/**
 * This class contains the methods to assign a VC and combine-data status to
 * each bundle
 * 
 * @author Mohamed
 *
 */
public class VcDesignation {

    private static final Logger log = Logger.getLogger(VcDesignation.class.getName());

    public static VcMap createVcMap(Design design, Noc noc, Mapping mapping) {
        long startTime = System.nanoTime();

        ReportData.getInstance().writeToRpt("Started VC designation...");

        VcMap vcMap = assignVcs(design, noc, mapping);
        log.info(vcMap.toString());

        long endTime = System.nanoTime();
        DecimalFormat secondsFormat = new DecimalFormat("#.00");
        log.info("Elapsed Time = " + secondsFormat.format((endTime - startTime) / 1e9) + " seconds");

        ReportData.getInstance().writeToRpt(
                "Finished VC designation -- took " + secondsFormat.format((endTime - startTime) / 1e9) + " seconds");

        ReportData.getInstance().writeToRpt("map_time = " + secondsFormat.format((endTime - startTime) / 1e9));

        ReportData.getInstance().closeRpt();

        return vcMap;
    }

    private static VcMap assignVcs(Design design, Noc noc, Mapping mapping) {
        VcMap vcMap = new VcMap();

        // loop over all routers, and figure out which routers need combine-data
        // to be enabled, the criteria for combine data are.
        // 1) That more than one input bundle share a router output.
        // In this case, each of those bundles should take its own VC and it's
        // own nocbundle that will become associated with that VC

        // loop over all routers in this mapping
        for (int i = 0; i < noc.getNumRouters(); i++) {
            HashSet<Bundle> bunSet = mapping.getBundlesAtRouters().get(i);
            int numInBuns = 0;
            // loop over all bundles at each router
            for (Bundle bun : bunSet) {
                // How many input bundles do we have?
                if (bun.getDirection() == Direction.INPUT) {
                    numInBuns++;
                }
            }

            // "combine data" is equal to the number of additional streams we
            // merge onto this router input.
            // So it is 0 for normal mode or if we have no bundles.
            // And it is 1 if we have 2 bundles, or n-1 for n bunles
            int combineData = numInBuns == 0 ? 0 : (numInBuns - 1);

            // 1) assign the routers to this combine_data value.
            // 2) associate bundles at this router with the combine_data value.
            // 3) associate bundles at this router with a VC
            // 4) associate source bundles of the same connections with that VC
            // 5) store all that info in a VcMap that can be used later

            // how do we choose the VC? randomly is fine for now.
            // But later (TODO), we should also look at path overlap in the
            // whole NoC and choose the VC such that it minimizes HOL blocking
            // when two connection paths overlap

            // go over the bunset. For each input bundle, find the source bundle
            // then assign each bundle a VC
            if (combineData != 0) {
                int currVC = combineData;
                for (Bundle dstBun : bunSet) {
                    if (dstBun.getDirection() == Direction.INPUT) {
                        List<Bundle> srcBuns = dstBun.getConnections();
                        vcMap.addVcDesignation(dstBun, srcBuns, i, currVC--, combineData);
                    }
                }
            }

            // TODO at the very end, go over all bundles; if they weren't
            // already added then we'll add them with defaults (optional)
            // right now I just assume that unspecified bundles are assigned
            // default values (VC0 and combine_data 0)
        }

        // last thing is to edit the combine_data parameter in the NoC
        editNocCombineDataParameter(noc, vcMap);

        return vcMap;
    }

    private static void editNocCombineDataParameter(Noc noc, VcMap vcMap) {
        String combineDataStr = "'{";

        for (int i = 0; i < noc.getNumRouters(); i++) {

            combineDataStr += vcMap.getRouterToCombineData().containsKey(i) ? vcMap.getRouterToCombineData().get(i) + "," : "0,";
        }
        combineDataStr = combineDataStr.substring(0, combineDataStr.length() - 1) + "}";

        noc.editParameter("COMBINE_DATA", combineDataStr);
    }
}
