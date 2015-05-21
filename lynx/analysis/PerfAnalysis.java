package lynx.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import lynx.data.MyEnums.SimModType;

public class PerfAnalysis {

    private static final Logger log = Logger.getLogger(PerfAnalysis.class.getName());

    private static final int CURRMOD_POS = 0;
    private static final int TIME_POS = 1;
    private static final int SRCROUTER_POS = 2;
    private static final int DSTROUTER_POS = 3;
    private static final int CURRROUTER_POS = 4;
    private static final int DATA_POS = 5;
    private static final int SRCMOD_POS = 6;

    /**
     * Struct to hold the data parsed from the trace file
     * 
     * @author Mohamed
     *
     */
    public static class SimEntry {

        int currMod;
        int time;
        int endTime;
        int srcRouter;
        int dstRouter;
        int currRouter;
        int data;
        int srcMod;

        String line;

        public SimEntry(String line) {

            this.line = line;
            currMod = findFieldValue(line, CURRMOD_POS);
            time = findFieldValue(line, TIME_POS);
            srcRouter = findFieldValue(line, SRCROUTER_POS);
            dstRouter = findFieldValue(line, DSTROUTER_POS);
            currRouter = findFieldValue(line, CURRROUTER_POS);
            data = findFieldValue(line, DATA_POS);
            // sinks have the sourcemod id as well
            if (findModType(line) == SimModType.SINK) {
                srcMod = findFieldValue(line, SRCMOD_POS);
            } else if (findModType(line) == SimModType.SRC) {
                srcMod = currMod;
            }
            endTime = 0;
        }

        public String getHash() {
            return "" + srcMod + "_" + data;
        }

        public void updateTime(int time) {
            this.endTime = time;
        }
    }

    /**
     * Parse a simulation trace file and find performance metrics
     * 
     * @param simRepFile
     * @throws IOException
     */
    public static void parseSimFile(File simRepFile) throws IOException {

        log.info("Starting performance analysis of trace file: " + simRepFile.getPath());

        if (!simRepFile.isFile()) {
            log.warning("No lynx_trace.txt file was found in " + simRepFile.getParent()
                    + ". Please run a performance simulation first.");
            return;
        }

        // create a map that keeps track of all sent flits
        Map<String, SimEntry> entryMap = new HashMap<String, SimEntry>();

        // go over the file line by line
        BufferedReader br = new BufferedReader(new FileReader(simRepFile));
        String line = null;
        while ((line = br.readLine()) != null) {
            String partList[] = line.split(";");
            assert partList.length == 6 || partList.length == 7 : "This line:\n" + line
                    + "\n in the trace file has an unsupported format";

            SimEntry simEntry = new SimEntry(line);
            String hash = simEntry.getHash();
            if (findModType(line) == SimModType.SRC) {
                entryMap.put(hash, simEntry);
            } else if (findModType(line) == SimModType.SINK) {
                SimEntry origSimEntry = entryMap.get(hash);
                origSimEntry.updateTime(simEntry.time);
            }
        }

        // go over the entryMap and print all latencies
        for (SimEntry simEntry : entryMap.values()) {
            log.info(simEntry.getHash() + ": startTime=" + simEntry.time + ", endTime=" + simEntry.endTime);
        }

        br.close();

    }

    private static int findFieldValue(String line, int fieldPos) {
        String partList[] = line.split(";");
        String field = partList[fieldPos];
        int value = Integer.parseInt(field.split("=")[1].trim());
        return value;
    }

    private static SimModType findModType(String line) {
        String partList[] = line.split(";");
        String typeField = partList[CURRMOD_POS];
        String type = typeField.split("=")[0].trim();
        return SimModType.valueOf(type);
    }
}
