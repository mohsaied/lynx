package lynx.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lynx.analysis.PerfAnalysis.LatencyStruct;
import lynx.analysis.PerfAnalysis.ThroughputStruct;

/**
 * Holds the performance analysis results
 * 
 * @author Mohamed
 *
 */
public class Analysis {

    List<LatencyStruct> latency;
    List<ThroughputStruct> throughput;

    Map<String, List<Integer>> debugThroughput;
    Map<String, List<Integer>> debugLatency;

    public Analysis(int numMods, int numConns) {
        this.latency = new ArrayList<PerfAnalysis.LatencyStruct>();
        this.throughput = new ArrayList<PerfAnalysis.ThroughputStruct>();

        this.debugThroughput = new HashMap<String, List<Integer>>();
        this.debugLatency = new HashMap<String, List<Integer>>();
    }

    public void addLatencyEntry(LatencyStruct entry) {
        this.latency.add(entry);
    }

    public void addThroughputEntry(ThroughputStruct entry, int i) {
        this.throughput.add(entry);
    }

    public List<LatencyStruct> getLatencyList() {
        return this.latency;
    }

    public List<ThroughputStruct> getThroughputList() {
        return this.throughput;
    }

    public void addDebugThroughput(String mod, int entry) {
        List<Integer> list = new ArrayList<Integer>();
        if (debugThroughput.get(mod) != null)
            list = debugThroughput.get(mod);
        list.add(entry);
        debugThroughput.put(mod, list);
    }

    public Map<String, List<Integer>> getDebugThroughput() {
        return debugThroughput;
    }

    public void addDebugLatency(String conn, int entry) {
        List<Integer> list = new ArrayList<Integer>();
        if (debugLatency.get(conn) != null)
            list = debugLatency.get(conn);
        list.add(entry);
        debugLatency.put(conn, list);
    }

    public Map<String, List<Integer>> getDebugLatency() {
        return debugLatency;
    }
}
