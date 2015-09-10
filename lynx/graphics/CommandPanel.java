package lynx.graphics;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import lynx.analysis.Analysis;
import lynx.analysis.PerfAnalysis;
import lynx.clustering.NocClustering;
import lynx.data.Design;
import lynx.data.Noc;
import lynx.elaboration.ConnectionGroup;
import lynx.elaboration.Elaboration;
import lynx.interconnect.NocInterconnect;
import lynx.main.DesignData;
import lynx.main.Main;
import lynx.main.ReportData;
import lynx.nocmapping.NocMapping;
import lynx.xml.XmlDesign;
import lynx.hdlgen.Simulation;

public class CommandPanel extends JPanel {

    private static final long serialVersionUID = 2390916431716936366L;

    private static final Logger log = Logger.getLogger(CommandPanel.class.getName());

    // panels
    private JPanel openPanel;
    private JPanel clusterPanel;
    private JPanel mapPanel;
    private JPanel fileOutPanel;
    private JPanel perfPanel;
    private JPanel logoPanel;

    // logo image
    private BufferedImage logo;

    // buttons
    private JButton openButton;
    private JButton clusterButton;
    private JButton mapButton;
    private JButton fileOutButton;
    private JButton perfButton;

    // opened file
    private File openedFile;

    // text field for opened file
    private JLabel openSecLabel;
    private JLabel clusterSecLabel;
    private JLabel mapSecLabel;
    private JLabel fileOutSecLabel;
    private JLabel perfSecLabel;

    // progress bars for algorithms
    private JProgressBar fileProgress;
    private JProgressBar clusterProgress;
    private JProgressBar mapProgress;
    private JProgressBar fileOutProgress;
    private JProgressBar perfProgress;

    private MainPanel mainPanel;

    /**
     * Constructor for the CommandPanel creates a simple gridlayout and
     * initializes everything
     */
    public CommandPanel(MainPanel mainPanel) {
        super(new GridLayout(6, 1));
        this.mainPanel = mainPanel;

        // create a panel for each group of buttons
        createLogoPanel();

        // first panel is for open file
        createOpenPanel();

        // second panel is for clustering
        createClusterPanel();

        // third panel is for mapping
        createMapPanel();

        // fourth panel is for file generation
        createFilePanel();

        // fifth panel is for performance evaluation
        createPerfPanel();
    }

    private void createOpenPanel() {
        openPanel = new JPanel(new GridLayout(4, 1));

        // first create a label for this section
        openSecLabel = new JLabel("1. Open Design");
        openSecLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // then create the text field to show name of opened file
        fileProgress = new JProgressBar();
        fileProgress.setString("No File Opened");
        fileProgress.setStringPainted(true);

        // now create the button itself
        openButton = new JButton("Open File") {
            private static final long serialVersionUID = -3169712968203420370L;
        };
        openButton.addActionListener(new ActionListener() {
            private final JFileChooser fc = new JFileChooser();

            @Override
            public void actionPerformed(ActionEvent e) {
                // Handle open button action.
                if (e.getSource() == openButton) {
                    fc.setCurrentDirectory(new File("D:\\Dropbox\\PhD\\Software\\noclynx\\designs"));
                    int returnVal = fc.showOpenDialog(CommandPanel.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        openedFile = fc.getSelectedFile();
                        String designPath = openedFile.getPath();
                        log.info("Opening: " + designPath);
                        new Thread() {
                            public void run() {
                                try {
                                    fileProgress.setString("Opening file...");
                                    fileProgress.setStringPainted(true);
                                    fileProgress.setIndeterminate(true);
                                    DesignData.resetSingleton();
                                    ReportData.resetSingleton();
                                    ReportData.getInstance().setDesignFile(openedFile);
                                    XmlDesign.readXMLDesign(designPath);
                                    NocInterconnect.addNoc("nocs/w150_n16_v2_d16.xml");
                                    fileProgress.setString(openedFile.getName() + " (valid)");
                                    log.info("Valid design " + openedFile.getName() + " opened successfully");
                                    mainPanel.clearTabs();
                                    clusterProgress.setString("");
                                    mapProgress.setString("");
                                    mainPanel.addGraphTab();
                                    fileProgress.setIndeterminate(false);
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                    fileProgress.setString("error!");
                                    log.severe("Error! Most probable cause is an invalid file");
                                }

                                clusterProgress.setIndeterminate(true);
                                clusterProgress.setStringPainted(true);
                                clusterProgress.setString("working...");
                                log.info("Clustering " + openedFile.getName() + " started");
                                NocClustering.clusterDesign();
                                mainPanel.addClusterTab();
                                clusterProgress.setIndeterminate(false);
                                clusterProgress.setString("done.");

                                log.info("Elaborating design");
                                Design clusteredDesign = DesignData.getInstance().getClusteredDesign();
                                List<ConnectionGroup> cgList = Elaboration.identifyConnectionGroups(clusteredDesign);
                                DesignData.getInstance().setConnectionGroups(cgList);

                                mapProgress.setIndeterminate(true);
                                mapProgress.setStringPainted(true);
                                mapProgress.setString("working...");
                                log.info("Mapping " + openedFile.getName() + " started");
                                Noc noc = DesignData.getInstance().getNoc();
                                NocMapping.findMappings(clusteredDesign, noc);
                                mainPanel.addNoCTabs(clusteredDesign, noc);
                                mapProgress.setIndeterminate(false);
                                mapProgress.setString("done.");

                                fileOutProgress.setIndeterminate(true);
                                fileOutProgress.setStringPainted(true);
                                fileOutProgress.setString("working...");
                                NocInterconnect.connectDesignToNoc(clusteredDesign, noc, cgList);
                                log.info("Generating output files");
                                try {
                                    Design simulationDesign = DesignData.getInstance().getSimulationDesign();
                                    Simulation.generateSimDir(simulationDesign, noc);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    fileOutProgress.setString("error!");
                                    log.severe("Error! Something wrong with verilog generation");
                                }
                                fileOutProgress.setIndeterminate(false);
                                fileOutProgress.setString("done.");

                                perfProgress.setIndeterminate(true);
                                perfProgress.setStringPainted(true);
                                perfProgress.setString("working...");
                                File simRepFile = ReportData.getInstance().getSimRepFile();
                                try {
                                    Analysis analysis = PerfAnalysis.parseSimFile(simRepFile);
                                    DesignData.getInstance().setAnalysis(analysis);
                                } catch (IOException e1) {
                                    log.warning("Performance analysis failed - stack trace to follow");
                                    e1.printStackTrace();
                                }
                                Analysis analysis = DesignData.getInstance().getAnalysis();
                                if (analysis != null)
                                    mainPanel.addPerfTab(analysis);
                                perfProgress.setIndeterminate(false);
                                perfProgress.setString("done.");
                            }
                        }.start();
                    } else {
                        log.warning("Open command cancelled by user");
                    }
                }
            }
        });
        openPanel.add(openSecLabel);
        openPanel.add(openButton);
        openPanel.add(fileProgress);
        this.add(openPanel);
    }

    private void createClusterPanel() {

        clusterPanel = new JPanel(new GridLayout(4, 1));

        // first create a label for this section
        clusterSecLabel = new JLabel("2. Design Clustering");
        clusterSecLabel.setHorizontalAlignment(SwingConstants.CENTER);

        clusterProgress = new JProgressBar();
        clusterButton = new JButton("Clustering");
        clusterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!mainPanel.switchTab(MainPanel.CLUSTERTABID))
                    log.warning("Clustering has not been run, or hasn't completed running. You must open a design first.");
            }
        });
        clusterPanel.add(clusterSecLabel);
        clusterPanel.add(clusterButton);
        clusterPanel.add(clusterProgress);
        this.add(clusterPanel);
    }

    private void createMapPanel() {
        mapPanel = new JPanel(new GridLayout(4, 1));

        // first create a label for this section
        mapSecLabel = new JLabel("3. NoC Mapping");
        mapSecLabel.setHorizontalAlignment(SwingConstants.CENTER);

        mapProgress = new JProgressBar();
        mapButton = new JButton("Mapping");
        mapButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!mainPanel.switchTab(MainPanel.MAPTABID))
                    log.warning("Mapping has not been run, or hasn't completed running. You must open a design first.");
            }
        });
        mapPanel.add(mapSecLabel);
        mapPanel.add(mapButton);
        mapPanel.add(mapProgress);
        this.add(mapPanel);
    }

    private void createFilePanel() {
        fileOutPanel = new JPanel(new GridLayout(4, 1));

        // first create a label for this section
        fileOutSecLabel = new JLabel("4. Output Generation");
        fileOutSecLabel.setHorizontalAlignment(SwingConstants.CENTER);

        fileOutProgress = new JProgressBar();
        fileOutButton = new JButton("Generate Verilog Simulation");
        fileOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Output XML and Verilog files have been generated in project directory");
            }
        });
        fileOutPanel.add(fileOutSecLabel);
        fileOutPanel.add(fileOutButton);
        fileOutPanel.add(fileOutProgress);
        this.add(fileOutPanel);
    }

    private void createPerfPanel() {
        perfPanel = new JPanel(new GridLayout(4, 1));

        // first create a label for this section
        perfSecLabel = new JLabel("5. Performance Evaluation");
        perfSecLabel.setHorizontalAlignment(SwingConstants.CENTER);

        perfProgress = new JProgressBar();
        perfButton = new JButton("Analyze Simulation Trace");
        perfButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                perfProgress.setIndeterminate(true);
                perfProgress.setStringPainted(true);
                perfProgress.setString("working...");
                File simRepFile = ReportData.getInstance().getSimRepFile();
                new Thread() {
                    public void run() {
                        try {
                            Analysis analysis = PerfAnalysis.parseSimFile(simRepFile);
                            DesignData.getInstance().setAnalysis(analysis);
                        } catch (IOException e) {
                            log.warning("Performance analysis failed - stack trace to follow");
                            e.printStackTrace();
                        }
                    }
                }.start();
                Analysis analysis = DesignData.getInstance().getAnalysis();
                if (analysis != null) {
                    mainPanel.removePerfTab();
                    mainPanel.addPerfTab(analysis);
                }
                perfProgress.setIndeterminate(false);
                perfProgress.setString("done.");
                if (!mainPanel.switchTab(MainPanel.PERFTABID))
                    log.warning("Performance analysis did not complete, make sure you ran a simulation until completion.");
            }
        });
        perfPanel.add(perfSecLabel);
        perfPanel.add(perfButton);
        perfPanel.add(perfProgress);
        this.add(perfPanel);
    }

    private void createLogoPanel() {
        try {
            logo = ImageIO.read(Main.class.getResource("/logo/logo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // panel for logo
        logoPanel = new JPanel(new GridLayout(1, 1)) {
            private static final long serialVersionUID = -1092410231325842147L;

            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(logo, 10, 0, this.getWidth() - 20, (logo.getHeight() - 20) * this.getWidth()
                        / (logo.getWidth() - 20), null);
            }
        };
        this.add(logoPanel);
    }

}
