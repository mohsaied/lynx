package lynx.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * This Singleton class holds reporting information for the program For e.g.:
 * design path
 * 
 * @author Mohamed
 *
 */
public class ReportData {

    /**
     * the currently opened design file
     */
    private File designFile;

    /**
     * The output report
     */
    private PrintWriter report;

    /**
     * Singleton of program data
     */
    private static ReportData instance = null;

    private ReportData() {
        this.designFile = null;
        this.report = null;
    }

    public static ReportData getInstance() {
        if (instance == null)
            instance = new ReportData();
        return instance;
    }

    public File getDesignFile() {
        return designFile;
    }

    public void setDesignFile(File designFile) throws FileNotFoundException {
        this.designFile = designFile;
        this.report = new PrintWriter(designFile.getPath() + ".rpt");
    }

    public void writeToRpt(String line) {
        this.report.println(line);
    }

    public void closeRpt() {
        this.report.close();
    }

}
