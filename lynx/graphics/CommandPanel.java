package lynx.graphics;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import lynx.clustering.NocClustering;
import lynx.interconnect.NocInterconnect;
import lynx.main.Main;
import lynx.main.ReportData;
import lynx.nocmapping.NocMapping;
import lynx.xml.XmlDesign;

public class CommandPanel extends JPanel {

    private static final long serialVersionUID = 2390916431716936366L;

    private static final Logger log = Logger.getLogger(CommandPanel.class.getName());

    // panels
    private JPanel openPanel;
    private JPanel clusterPanel;
    private JPanel mapPanel;
    private JPanel logoPanel;

    // logo image
    private BufferedImage logo;

    // buttons
    private JButton openButton;
    private JButton clusterButton;
    private JButton mapButton;

    // opened file
    private File openedFile;

    // text field for opened file
    private JLabel openSecLabel;
    private JLabel clusterSecLabel;
    private JLabel mapSecLabel;

    // progress bars for algorithms
    private JProgressBar fileProgress;
    private JProgressBar clusterProgress;
    private JProgressBar mapProgress;

    private MainPanel mainPanel;

    /**
     * Constructor for the CommandPanel creates a simple gridlayout and
     * initializes everything
     */
    public CommandPanel(MainPanel mainPanel) {
        super(new GridLayout(4, 1));
        this.mainPanel = mainPanel;

        // create a panel for each group of buttons
        createLogoPanel();

        // first panel is for open file
        createOpenPanel();

        // second panel is for clustering
        createClusterPanel();

        // third panel is for mapping
        createMapPanel();
    }

    private void createOpenPanel() {
        openPanel = new JPanel(new GridLayout(4, 1));

        // first create a label for this section
        openSecLabel = new JLabel("1. Open Design");
        openSecLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // then create the text field to show name of opened file
        fileProgress = new JProgressBar();
        fileProgress.setStringPainted(true);
        fileProgress.setString("No File Opened");

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
                                    fileProgress.setIndeterminate(true);
                                    XmlDesign.readXMLDesign(designPath);
                                    ReportData.getInstance().setDesignFile(openedFile);
                                    NocInterconnect.addNoc("nocs/w150_n16_v2_d16.xml");
                                    fileProgress.setString(openedFile.getName() + " (valid)");
                                    log.info("Valid design opened successfully");
                                    mainPanel.clearTabs();
                                    clusterProgress.setString("");
                                    mapProgress.setString("");
                                    mainPanel.addGraphTab();
                                    fileProgress.setIndeterminate(false);

                                    clusterProgress.setIndeterminate(true);
                                    clusterProgress.setStringPainted(true);
                                    clusterProgress.setString("working...");
                                    log.info("Clustering " + openedFile.getName() + " started");
                                    NocClustering.clusterDesign();
                                    mainPanel.addClusterTab();
                                    clusterProgress.setIndeterminate(false);
                                    clusterProgress.setString("done.");

                                    mapProgress.setIndeterminate(true);
                                    mapProgress.setStringPainted(true);
                                    mapProgress.setString("working...");
                                    log.info("Mapping " + openedFile.getName() + " started");
                                    NocMapping.findMappings();
                                    mainPanel.addNoCTabs();
                                    mapProgress.setIndeterminate(false);
                                    mapProgress.setString("done.");

                                } catch (Exception e1) {
                                    fileProgress.setString("invalid file specified!");
                                    log.severe("Error! Most probable cause is an invalid file");
                                }
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
                g.drawImage(logo, 10, 10, this.getWidth() - 20, (logo.getHeight() - 20) * this.getWidth()
                        / (logo.getWidth() - 20), null);
            }
        };
        this.add(logoPanel);
    }

}
