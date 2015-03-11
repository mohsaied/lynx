package lynx.graphics;

import java.awt.FlowLayout;
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
            // create a dataset...
            XYSeries data = new XYSeries("Temperature");
            data.add(1, 50);
            data.add(2, 100);
            data.add(3, 150);
            data.add(4, 200);

            final XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(data);
            // create a chart...
            JFreeChart chart = ChartFactory.createXYLineChart("", "Time", "Temperature", dataset,
                    PlotOrientation.VERTICAL, //default = vertical 
                    false, // legend?
                    true, // tooltips?
                    false // URLs?
                    );

            // create and display a frame...
            ChartPanel panel = new ChartPanel(chart);
            this.add(panel);
        }
    }
}
