package lynx.analysis;

import java.util.ArrayList;
import java.util.List;

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

    public Analysis(int numMods, int numConns) {
        this.latency = new ArrayList<PerfAnalysis.LatencyStruct>();
        this.throughput = new ArrayList<PerfAnalysis.ThroughputStruct>();
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
}
