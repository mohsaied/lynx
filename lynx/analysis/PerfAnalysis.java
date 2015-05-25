package lynx.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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

    private static final int WARMUP_CYCLES = 100;

    protected static final int CLK_PERIOD = 10000;

    /**
     * Struct to hold the latencies of transfers
     */
    public static class LatencyStruct {

        public String name;
        public double avgLatency;
        public int minLatency;
        public int maxLatency;
        public int numberOfSamples;

        private LatencyStruct(String name, double avgLatency, int minLatency, int maxLatency, int numberOfSamples) {
            this.name = name;
            this.avgLatency = avgLatency;
            this.minLatency = minLatency;
            this.maxLatency = maxLatency;
            this.numberOfSamples = numberOfSamples;
        }

        @Override
        public String toString() {
            return "name: " + name + ", avg:" + avgLatency + ", min/max:" + minLatency + "/" + maxLatency + ", num:"
                    + numberOfSamples;
        }
    }

    /**
     * Struct to hold the latencies of transfers
     */
    public static class ThroughputStruct {

        public int module;
        public double avgThroughput;
        public double minThroughput;
        public double maxThroughput;
        public int numberOfSamples;

        private ThroughputStruct(int module, double avgThroughput, double minThroughput, double maxThroughput, int numberOfSamples) {
            this.module = module;
            this.avgThroughput = avgThroughput;
            this.minThroughput = minThroughput;
            this.maxThroughput = maxThroughput;
            this.numberOfSamples = numberOfSamples;
        }

        @Override
        public String toString() {
            return "module: " + module + ", avg:" + avgThroughput + ", min/max:" + minThroughput + "/" + maxThroughput + ", num:"
                    + numberOfSamples;
        }
    }

    /**
     * Parse a simulation trace file and find performance metrics
     * 
     * @param simRepFile
     * @throws IOException
     */
    public static Analysis parseSimFile(File simRepFile) throws IOException {

        log.info("Starting performance analysis of trace file: " + simRepFile.getPath());

        if (!simRepFile.isFile()) {
            log.warning("No lynx_trace.txt file was found in " + simRepFile.getParent()
                    + ". Please run a performance simulation first.");
            return null;
        }

        // create a map that keeps track of all flits
        Map<String, SimEntry> srcEntryMap = new HashMap<String, SimEntry>();
        Map<Integer, SimEntry> sinkEntryMap = new HashMap<Integer, SimEntry>();

        // sets to keep all the sources and sinks
        Set<Integer> srcs = new HashSet<Integer>();
        Set<Integer> sinks = new HashSet<Integer>();
        Set<String> connections = new HashSet<String>();

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
                srcs.add(simEntry.currMod);
            } else if (findModType(line) == SimModType.SINK) {
                SimEntry origSimEntry = srcEntryMap.get(hash);
                origSimEntry.update(simEntry);
                int sinkHash = simEntry.getSinkHash();
                sinkEntryMap.put(sinkHash, simEntry);
                sinks.add(simEntry.currMod);
                connections.add(connString(simEntry.srcMod, simEntry.currMod));
            }
        }

        Analysis analysis = new Analysis(sinks.size() + srcs.size(), connections.size());

        // what did we find?
        log.info("Found " + srcs.size() + " src(s), " + sinks.size() + " sink(s) and " + connections.size() + " connection(s)");

        // loop over srcs and sinks and find the throughput
        for (int srcMod : srcs) {
            ThroughputStruct throughput = findOutputThroughput(srcMod, srcEntryMap, analysis);
            log.info("Src(" + srcMod + "): throughput(cycles)=" + throughput);
            analysis.addThroughputEntry(throughput, srcMod);
        }
        for (int dstMod : sinks) {
            ThroughputStruct throughput = findInputThroughput(dstMod, sinkEntryMap, analysis);
            log.info("Sink(" + dstMod + "): throughput(cycles)=" + throughput);
            analysis.addThroughputEntry(throughput, dstMod);
        }

        // loop over all connections and find the latency
        for (String connString : connections) {
            int srcMod = connSrc(connString);
            int dstMod = connSink(connString);
            LatencyStruct latency = findLatency(srcMod, dstMod, srcEntryMap, analysis);
            log.info("Connection(" + srcMod + "->" + dstMod + "): latency = " + latency);
            analysis.addLatencyEntry(latency);
        }

        br.close();

        return analysis;
    }

    private static String connString(int srcMod, int sinkMod) {
        return srcMod + "_" + sinkMod;
    }

    private static int connSrc(String connString) {
        return Integer.parseInt(connString.split("_")[0]);
    }

    private static int connSink(String connString) {
        return Integer.parseInt(connString.split("_")[1]);
    }

    /**
     * For a sink module, this function finds its input throughput
     * 
     * @param dstMod
     * @param sinkEntryMap
     * @param analysis
     * @return
     */
    private static ThroughputStruct findInputThroughput(int mod, Map<Integer, SimEntry> entryMap, Analysis analysis) {
        // first, we need to sort that entryMap by key
        SortedSet<Integer> keys = new TreeSet<Integer>(entryMap.keySet());
        int prevSendTime = -1;
        double sumThroughput = 0.0;
        double minThroughput = 999999999.9;
        double maxThroughput = -1.0;
        int num = 0;
        for (Integer key : keys) {
            SimEntry simEntry = entryMap.get(key);
            int currSendTime = simEntry.time;
            if (prevSendTime != -1) {
                double currThroughput = (currSendTime - prevSendTime) / CLK_PERIOD;
                if (currThroughput < minThroughput)
                    minThroughput = currThroughput;
                if (currThroughput > maxThroughput)
                    maxThroughput = currThroughput;

                if (num >= WARMUP_CYCLES)
                    sumThroughput += currThroughput;

                // debugging for plot
                analysis.addDebugThroughput("" + mod, (int) currThroughput, currSendTime/CLK_PERIOD);
            }
            prevSendTime = currSendTime;
            simEntry = entryMap.get(SimEntry.hash(mod, ++num));
        }
        double avgThroughput = sumThroughput / (num - 2 - WARMUP_CYCLES);
        return new ThroughputStruct(mod, avgThroughput, minThroughput, maxThroughput, num - 2);
    }

    /**
     * For a src module, find its output throughput in number of operations per
     * cycle. This is measured by finding the number of cycles between
     * successive operations that are output from a single module, the target is
     * one, and the higher the worse (in some cases)
     * 
     * @param mod
     * @param modType
     * @param srcEntryMap
     * @param analysis
     * @return
     */
    private static ThroughputStruct findOutputThroughput(int mod, Map<String, SimEntry> entryMap, Analysis analysis) {

        double sumThroughput = 0.0;
        double minThroughput = 999999999.9;
        double maxThroughput = -1.0;
        int num = 0;
        int prevSendTime = -1;
        SimEntry simEntry = entryMap.get(SimEntry.hash(mod, ++num));
        while (simEntry != null) {
            int currSendTime = simEntry.time;
            if (prevSendTime != -1) {
                double currThroughput = (currSendTime - prevSendTime) / CLK_PERIOD;
                if (currThroughput < minThroughput)
                    minThroughput = currThroughput;
                if (currThroughput > maxThroughput)
                    maxThroughput = currThroughput;

                if (num >= WARMUP_CYCLES)
                    sumThroughput += currThroughput;

                // debugging for plot
                analysis.addDebugThroughput("" + mod, (int) currThroughput, currSendTime/CLK_PERIOD);
            }
            prevSendTime = currSendTime;
            simEntry = entryMap.get(SimEntry.hash(mod, ++num));
        }
        double avgThroughput = sumThroughput / (num - 2 - WARMUP_CYCLES);
        return new ThroughputStruct(mod, avgThroughput, minThroughput, maxThroughput, num - 2);
    }

    /**
     * Find the latency for a specific connection between a specified src/dst
     * 
     * @param srcMod
     * @param dstMod
     * @param analysis
     * @param entryMap
     * @return
     */
    private static LatencyStruct findLatency(int srcMod, int dstMod, Map<String, SimEntry> srcEntryMap, Analysis analysis) {

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

                    // debugging for plot
                    analysis.addDebugLatency(connString(srcMod, dstMod), (int) latency, simEntry.time/CLK_PERIOD);
                }
            }
        }

        int avgLatency = sumLatency / num;
        return new LatencyStruct(connString(srcMod, dstMod), avgLatency, minLatency, maxLatency, num);
    }

    protected static SimModType findModType(String line) {
        String partList[] = line.split(";");
        String typeField = partList[CURRMOD_POS];
        String type = typeField.split("=")[0].trim();
        return SimModType.valueOf(type);
    }
}
