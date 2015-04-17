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

    /**
     * The output report
     */
    private PrintWriter report;

    /**
     * Singleton of program data
     */
    private static ProgramData instance = null;

    private ProgramData() {
        this.designFile = null;
        this.report = null;
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
        this.report = new PrintWriter(designFile.getPath() + ".rpt");
    }

    public void writeToRpt(String line) {
        this.report.println(line);
    }

    public void closeRpt() {
        this.report.close();
    }

}
