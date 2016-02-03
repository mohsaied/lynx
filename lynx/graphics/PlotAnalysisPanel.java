package lynx.graphics;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import lynx.analysis.Analysis;

public class PlotAnalysisPanel extends JScrollPane {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(PlotPanel.class.getName());

    private Analysis analysis;

    private JPanel panel;

    /**
     * this constructor is for SA cost
     * 
     * @param design
     */
    public PlotAnalysisPanel(Analysis analysis) {
        this.panel = new JPanel();
        BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bl);

        log.setLevel(Level.ALL);
        this.analysis = analysis;

        initPane();

        this.setViewportView(panel);
    }

    private void initPane() {

        if (analysis != null) {
            Map<String, List<Integer>> debugYThroughput = analysis.getDebugYThroughput();
            Map<String, List<Integer>> debugXThroughput = analysis.getDebugXThroughput();
            addChart(debugYThroughput, debugXThroughput, "Throughput");
            Map<String, List<Integer>> debugYLatency = analysis.getDebugYLatency();
            Map<String, List<Integer>> debugXLatency = analysis.getDebugXLatency();
            addChart(debugYLatency, debugXLatency, "Latency");
        }
    }

    private void addChart(Map<String, List<Integer>> debugY, Map<String, List<Integer>> debugX, String name) {
        final XYSeriesCollection dataset = new XYSeriesCollection();
        for (String legendString : debugY.keySet()) {
            List<Integer> yList = debugY.get(legendString);
            List<Integer> xList = debugX.get(legendString);
            // create a dataset...
            XYSeries data = new XYSeries(legendString);

            for (int i = 0; i < yList.size(); i++)
                data.add(xList.get(i), yList.get(i));

            dataset.addSeries(data);
        }

        // create a chart...
        JFreeChart chart = ChartFactory.createXYLineChart("", "Time (cycles)", name, dataset, PlotOrientation.VERTICAL,
                true, // legend?
                true, // tooltips?
                false // URLs?
        );

        // create and display a frame...
        ChartPanel chartPanel = new ChartPanel(chart);
        this.panel.add(chartPanel);
    }
}
