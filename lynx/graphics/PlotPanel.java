package lynx.graphics;

import java.awt.FlowLayout;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import lynx.data.Design;

public class PlotPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(PlotPanel.class.getName());

    private Design design;

    public PlotPanel(Design design) {
        super(new FlowLayout());
        log.setLevel(Level.ALL);
        this.design = design;

        initPane();
    }

    public void setDesign(Design design) {
        this.design = design;
        initPane();
    }

    private void initPane() {

        if (design != null) {

            //create anneal cost chart
            addChart(design.getDebugAnnealCost(),"SA Cost");
            
            //create anneal cost chart
            addChart(design.getDebugAnnealTemp(),"SA Temp");
        }
    }

    private void addChart(List<Double> dataSet, String name) {
     // create a dataset...
        XYSeries data = new XYSeries(name);

        for (int i = 0; i < dataSet.size(); i++)
            data.add(i, dataSet.get(i));

        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(data);
        // create a chart...
        JFreeChart chart = ChartFactory.createXYLineChart("", "Time", name, dataset, PlotOrientation.VERTICAL, false, // legend?
                true, // tooltips?
                false // URLs?
                );

        // create and display a frame...
        ChartPanel panel = new ChartPanel(chart);
        this.add(panel);
    }
}
