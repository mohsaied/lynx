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

    private String fileName;

    private PrintWriter report;
    private PrintWriter verilogFile;
    private PrintWriter nocConfigFile;
    private PrintWriter nocWrapperFile;
    private PrintWriter quickScriptFile;

    private File simDir;

    private static ReportData instance = null;

    private ReportData() {
        this.designFile = null;
        this.report = null;
        this.verilogFile = null;
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

        // file name
        this.fileName = designFile.getName().substring(0, designFile.getName().length() - 4);

        // create directories for simulation and synthesis flows
        this.simDir = new File(designFile.getParent() + "\\sim");
        simDir.mkdir();
    }

    public void writeToRpt(String line) {
        this.report.println(line);
    }

    public void closeRpt() {
        this.report.close();
    }

    public PrintWriter getVerilogFile() throws FileNotFoundException {
        if (verilogFile == null) {
            verilogFile = new PrintWriter(simDir + "\\tb_" + fileName + ".sv");
        }
        return verilogFile;
    }

    public void closeVerilogFile() {
        verilogFile.close();
    }

    public PrintWriter getNocConfigFile() throws FileNotFoundException {
        if (nocConfigFile == null) {
            nocConfigFile = new PrintWriter(simDir + "\\noc_config");
        }
        return nocConfigFile;
    }

    public void closeNocConfigFile() {
        nocConfigFile.close();
    }

    public PrintWriter getNocWrapperFile() throws FileNotFoundException {
        if (nocWrapperFile == null) {
            nocWrapperFile = new PrintWriter(simDir + "\\noc_wrapper.sv");
        }
        return nocWrapperFile;
    }

    public void closeNocWrapperFile() {
        nocWrapperFile.close();
    }

    public PrintWriter getQuickScriptFile() throws FileNotFoundException {
        if (quickScriptFile == null) {
            quickScriptFile = new PrintWriter(simDir + "\\quick_script");
        }
        return quickScriptFile;
    }

    public void closeQuickScriptFile() {
        quickScriptFile.close();
    }

}
