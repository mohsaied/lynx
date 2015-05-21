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

    protected static final int CURRMOD_POS = 0;
    protected static final int TIME_POS = 1;
    protected static final int SRCROUTER_POS = 2;
    protected static final int DSTROUTER_POS = 3;
    protected static final int CURRROUTER_POS = 4;
    protected static final int DATA_POS = 5;
    protected static final int SRCMOD_POS = 6;

    protected static final int CLK_PERIOD = 10000;

    /**
     * Struct to hold the latencies of transfers
     * 
     * @author Mohamed
     *
     */
    private static class LatencyStruct {

        int avgLatency = 1;
        int minLatency = 1;
        int maxLatency = 1;

        private LatencyStruct(int avgLatency, int minLatency, int maxLatency) {
            this.avgLatency = avgLatency;
            this.minLatency = minLatency;
            this.maxLatency = maxLatency;
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
        Map<String, SimEntry> srcEntryMap = new HashMap<String, SimEntry>();

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
                srcEntryMap.put(hash, simEntry);
            } else if (findModType(line) == SimModType.SINK) {
                SimEntry origSimEntry = srcEntryMap.get(hash);
                origSimEntry.update(simEntry);
            }
        }

        int srcMod = 1;
        int dstMod = 0;

        LatencyStruct latencies = findLatency(srcMod, dstMod, srcEntryMap);
        log.info("Connection(" + srcMod + "->" + dstMod + "): avg latency=" + latencies.avgLatency + ", min/max latency="
                + latencies.minLatency + "/" + latencies.maxLatency);

        double throughput = findSrcThroughput(srcMod, srcEntryMap);
        log.info("Module(" + srcMod + "): throughput(cycles between operations)=" + throughput);

        br.close();
    }

    /**
     * For a module, find it's output throughput in number of operations per
     * cycle. This is measured by finding the number of cycles between
     * successive operations, the target is one, and the higher the worse (in
     * some cases)
     * 
     * @param srcMod
     * @param entryMap
     * @return
     */
    private static double findSrcThroughput(int mod, Map<String, SimEntry> srcEntryMap) {
        double sumThroughput = 0.0;
        int num = 0;
        int prevSendTime = -1;
        SimEntry simEntry = srcEntryMap.get(SimEntry.hash(mod, ++num));
        while (simEntry != null) {
            int currSendTime = simEntry.time;
            if (prevSendTime != -1) {
                double currThroughput = (currSendTime - prevSendTime) / CLK_PERIOD;
                sumThroughput += currThroughput;
            }
            prevSendTime = currSendTime;
            simEntry = srcEntryMap.get(SimEntry.hash(mod, ++num));
        }
        return sumThroughput / (num - 2);
    }

    /**
     * Find the latency for a specific connection between a specified src/dst
     * 
     * @param srcMod
     * @param dstMod
     * @param entryMap
     * @return
     */
    private static LatencyStruct findLatency(int srcMod, int dstMod, Map<String, SimEntry> srcEntryMap) {

        int sumLatency = 0;
        int minLatency = 999999999;
        int maxLatency = -1;
        int num = 0;

        for (SimEntry simEntry : srcEntryMap.values()) {
            if (simEntry.complete) {
                int currSrcMod = simEntry.srcMod;
                int currDstMod = simEntry.dstMod;

                if (currSrcMod == srcMod && currDstMod == dstMod) {
                    num++;
                    int latency = simEntry.getLatency();
                    if (latency > maxLatency)
                        maxLatency = latency;
                    if (latency < minLatency)
                        minLatency = latency;
                    sumLatency += latency;
                }
            }
        }

        int avgLatency = sumLatency / num;
        return new LatencyStruct(avgLatency, minLatency, maxLatency);
    }

    protected static SimModType findModType(String line) {
        String partList[] = line.split(";");
        String typeField = partList[CURRMOD_POS];
        String type = typeField.split("=")[0].trim();
        return SimModType.valueOf(type);
    }
}
