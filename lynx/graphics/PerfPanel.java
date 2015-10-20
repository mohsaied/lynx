package lynx.graphics;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

import lynx.analysis.Analysis;
import lynx.analysis.PerfAnalysis.LatencyStruct;
import lynx.analysis.PerfAnalysis.ThroughputStruct;

public class PerfPanel extends JScrollPane {

    private static final long serialVersionUID = 23184113128666842L;

    public PerfPanel(Analysis analysis) {
        super();

        JTable throughputTable = createThroughputTable(analysis);
        JTable latencyTable = createLatencyTable(analysis);
        JTable queueTimeTable = createQueueTimeTable(analysis);

        JPanel panel = new JPanel();
        BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bl);

        panel.add(new JTextArea("Throughput Analysis"));
        panel.add(throughputTable.getTableHeader(), BorderLayout.NORTH);
        panel.add(throughputTable, BorderLayout.CENTER);

        panel.add(new JTextArea("Latency Analysis"));
        panel.add(latencyTable.getTableHeader(), BorderLayout.NORTH);
        panel.add(latencyTable, BorderLayout.CENTER);

        panel.add(new JTextArea("QueueTime Analysis"));
        panel.add(queueTimeTable.getTableHeader(), BorderLayout.NORTH);
        panel.add(queueTimeTable, BorderLayout.CENTER);

        this.setViewportView(panel);
    }

    private JTable createThroughputTable(Analysis analysis) {
        String[] columns = { "mod", "avg", "min", "max", "num" };
        Object rows[][] = new Object[analysis.getThroughputList().size()][columns.length];
        int num = 0;
        for (ThroughputStruct entry : analysis.getThroughputList()) {
            rows[num][0] = entry.module;
            rows[num][1] = (double) Math.round(entry.avgThroughput * 100) / 100.0;
            rows[num][2] = entry.minThroughput;
            rows[num][3] = entry.maxThroughput;
            rows[num][4] = entry.numberOfSamples;
            num++;
        }
        JTable table = new JTable(rows, columns);
        table.setAutoCreateRowSorter(true);
        return table;
    }

    private JTable createLatencyTable(Analysis analysis) {
        String[] columns = { "conn", "avg", "min", "max", "num" };
        Object rows[][] = new Object[analysis.getLatencyList().size()][columns.length];
        int num = 0;
        for (LatencyStruct entry : analysis.getLatencyList()) {
            rows[num][0] = entry.name;
            rows[num][1] = (double) Math.round(entry.avgLatency * 100) / 100.0;
            rows[num][2] = entry.minLatency;
            rows[num][3] = entry.maxLatency;
            rows[num][4] = entry.numberOfSamples;
            num++;
        }
        JTable table = new JTable(rows, columns);
        table.setAutoCreateRowSorter(true);
        return table;
    }

    private JTable createQueueTimeTable(Analysis analysis) {
        String[] columns = { "conn", "avg", "min", "max", "num" };
        Object rows[][] = new Object[analysis.getQueueTimeList().size()][columns.length];
        int num = 0;
        for (LatencyStruct entry : analysis.getQueueTimeList()) {
            rows[num][0] = entry.name;
            rows[num][1] = (double) Math.round(entry.avgLatency * 100) / 100.0;
            rows[num][2] = entry.minLatency;
            rows[num][3] = entry.maxLatency;
            rows[num][4] = entry.numberOfSamples;
            num++;
        }
        JTable table = new JTable(rows, columns);
        table.setAutoCreateRowSorter(true);
        return table;
    }

}
