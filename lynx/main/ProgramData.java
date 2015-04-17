package lynx.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * This class holds any global data that has to be available for all subroutines
 * For e.g.: design path
 * 
 * @author Mohamed
 *
 */
public class ProgramData {

    /**
     * the currently opened design file
     */
    private File designFile;

    private PrintWriter clusterReport;
    private PrintWriter mapReport;

    /**
     * Singleton of programdata
     */
    private static ProgramData instance = null;

    private ProgramData() {
        this.designFile = null;
        this.clusterReport = null;
        this.mapReport = null;
    }

    public static ProgramData getInstance() {
        if (instance == null)
            instance = new ProgramData();
        return instance;
    }

    public File getDesignFile() {
        return designFile;
    }

    public void setDesignFile(File designFile) throws FileNotFoundException {
        this.designFile = designFile;
        this.clusterReport = new PrintWriter(designFile.getPath() + ".cluster.rpt");
        this.mapReport = new PrintWriter(designFile.getPath() + ".map.rpt");
    }

    public void writeClusterRpt(String line) {
        this.clusterReport.println(line);
    }

    public void writeMapRpt(String line) {
        this.mapReport.println(line);
    }

    public void closeClusterRpt() {
        this.clusterReport.close();
    }

    public void closeMapRpt() {
        this.mapReport.close();
    }

}
