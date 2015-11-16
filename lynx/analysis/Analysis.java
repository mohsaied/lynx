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
    List<LatencyStruct> queueTime;
    List<ThroughputStruct> throughput;

    Map<String, List<Integer>> debugYThroughput;
    Map<String, List<Integer>> debugXThroughput;
    Map<String, List<Integer>> debugYLatency;
    Map<String, List<Integer>> debugXLatency;
    Map<String, List<Integer>> debugYQueueTime;
    Map<String, List<Integer>> debugXQueueTime;

    public Analysis(int numMods, int numConns) {
        this.latency = new ArrayList<PerfAnalysis.LatencyStruct>();
        this.queueTime = new ArrayList<PerfAnalysis.LatencyStruct>();
        this.throughput = new ArrayList<PerfAnalysis.ThroughputStruct>();

        this.debugYThroughput = new HashMap<String, List<Integer>>();
        this.debugXThroughput = new HashMap<String, List<Integer>>();
        this.debugYLatency = new HashMap<String, List<Integer>>();
        this.debugXLatency = new HashMap<String, List<Integer>>();
        this.debugYQueueTime = new HashMap<String, List<Integer>>();
        this.debugXQueueTime = new HashMap<String, List<Integer>>();
    }

    public void addLatencyEntry(LatencyStruct entry) {
        this.latency.add(entry);
    }

    public void addqueueTimeEntry(LatencyStruct entry) {
        this.queueTime.add(entry);
    }

    public void addThroughputEntry(ThroughputStruct entry, int i) {
        this.throughput.add(entry);
    }

    public List<LatencyStruct> getLatencyList() {
        return this.latency;
    }

    public List<LatencyStruct> getQueueTimeList() {
        return this.queueTime;
    }

    public List<ThroughputStruct> getThroughputList() {
        return this.throughput;
    }

    public void addDebugThroughput(String mod, int throughput, int time) {
        List<Integer> yList = new ArrayList<Integer>();
        List<Integer> xList = new ArrayList<Integer>();
        if (debugYThroughput.get(mod) != null) {
            yList = debugYThroughput.get(mod);
            xList = debugXThroughput.get(mod);
        }
        yList.add(throughput);
        xList.add(time);
        debugYThroughput.put(mod, yList);
        debugXThroughput.put(mod, xList);
    }

    public Map<String, List<Integer>> getDebugYThroughput() {
        return debugYThroughput;
    }

    public Map<String, List<Integer>> getDebugXThroughput() {
        return debugXThroughput;
    }

    public void addDebugLatency(String conn, int latency, int time) {
        List<Integer> yList = new ArrayList<Integer>();
        List<Integer> xList = new ArrayList<Integer>();
        if (debugYLatency.get(conn) != null) {
            yList = debugYLatency.get(conn);
            xList = debugXLatency.get(conn);
        }
        yList.add(latency);
        xList.add(time);
        debugYLatency.put(conn, yList);
        debugXLatency.put(conn, xList);
    }

    public Map<String, List<Integer>> getDebugYLatency() {
        return debugYLatency;
    }

    public Map<String, List<Integer>> getDebugXLatency() {
        return debugXLatency;
    }

    public void addDebugQueueTime(String conn, int latency, int time) {
        List<Integer> yList = new ArrayList<Integer>();
        List<Integer> xList = new ArrayList<Integer>();
        if (debugYQueueTime.get(conn) != null) {
            yList = debugYQueueTime.get(conn);
            xList = debugXQueueTime.get(conn);
        }
        yList.add(latency);
        xList.add(time);
        debugYQueueTime.put(conn, yList);
        debugXQueueTime.put(conn, xList);
    }

    public Map<String, List<Integer>> getDebugYQueueTime() {
        return debugYQueueTime;
    }

    public Map<String, List<Integer>> getDebugXQueueTime() {
        return debugXQueueTime;
    }
}
