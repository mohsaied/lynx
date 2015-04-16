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
import lynx.data.Design;
import lynx.interconnect.NocInterconnect;
import lynx.nocmapping.NocMapping;
import lynx.xml.XmlDesign;

public class CommandPanel extends JPanel {

    private static final long serialVersionUID = 2390916431716936366L;

    private static final Logger log = Logger.getLogger(CommandPanel.class.getName());

    // panels
    private JPanel buttonsPanel;
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
    private JLabel fileNameLabel;

    // progress bars for algorithms
    private JProgressBar clusterProgress;
    private JProgressBar mapProgress;

    // loaded design
    private Design design;

    private MainPanel mainPanel;

    /**
     * Constructor for the CommandPanel creates a simple gridlayout and
     * initializes everything
     */
    public CommandPanel(MainPanel mainPanel) {
        super(new GridLayout(2, 1));
        this.mainPanel = mainPanel;
        init();
    }

    public void init() {

        createLogoPanel();

        // panel to put all the buttons (and progress bars) we need
        buttonsPanel = new JPanel(new GridLayout(7, 1));

        // create all the buttons and their progress bars too
        openButton = createOpenButton();
        clusterButton = createClusterButton();
        mapButton = createMapButton();

        // add all buttons to panel in the right order
        buttonsPanel.add(openButton);
        buttonsPanel.add(fileNameLabel);
        buttonsPanel.add(clusterButton);
        buttonsPanel.add(clusterProgress);
        buttonsPanel.add(mapButton);
        buttonsPanel.add(mapProgress);
        this.add(buttonsPanel);
    }

    private JButton createOpenButton() {
        // first create the text field to show name of opened file
        fileNameLabel = new JLabel("No File Opened");
        fileNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // now create the button itself
        JButton openButton = new JButton("Open Design") {
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

                        try {
                            design = XmlDesign.readXMLDesign(designPath);
                            design.update();
                            NocInterconnect.addNoc(design, "designs/noc.xml");
                            fileNameLabel.setText(openedFile.getName() + " (valid)");
                            log.info("Valid design opened successfully");
                            mainPanel.clearTabs();
                            clusterProgress.setString("");
                            mapProgress.setString("");
                            mainPanel.addGraphTab(design);
                        } catch (Exception e1) {
                            fileNameLabel.setText("invalid file specified!");
                            log.info("Invalid file not opened");
                        }

                    } else {
                        log.info("Open command cancelled by user");
                    }
                }
            }
        });
        return openButton;
    }

    private JButton createClusterButton() {
        clusterProgress = new JProgressBar();
        clusterButton = new JButton("Cluster Design");
        clusterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (openedFile == null) {
                    log.info("Clustering attempted but no design is loaded. Please open a lynx file first.");
                } else {
                    clusterProgress.setIndeterminate(true);
                    clusterProgress.setStringPainted(true);
                    clusterProgress.setString("working...");
                    log.info("Clustering " + openedFile.getName() + " started");
                    NocClustering.clusterDesign(design);
                    mainPanel.addClusterTab(design);
                    clusterProgress.setIndeterminate(false);
                    clusterProgress.setString("done.");
                }
            }
        });
        return clusterButton;
    }

    private JButton createMapButton() {
        mapProgress = new JProgressBar();
        mapButton = new JButton("Map Design to NoC");
        mapButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (openedFile == null) {
                    log.info("Mapping attempted but no design is loaded. Please open a lynx file first.");
                } else {
                    mapProgress.setIndeterminate(true);
                    mapProgress.setStringPainted(true);
                    mapProgress.setString("working...");
                    log.info("Mapping " + openedFile.getName() + " started");
                    NocMapping.findMappings(design);
                    mainPanel.addNoCTabs(design);
                    mapProgress.setIndeterminate(false);
                    mapProgress.setString("done.");
                }
            }
        });
        return mapButton;
    }

    private void createLogoPanel() {
        try {
            logo = ImageIO.read(new File("logo/logo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // panel for logo
        logoPanel = new JPanel(new GridLayout(1, 1)) {
            private static final long serialVersionUID = -1092410231325842147L;

            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(logo, 0, 0, this.getWidth(), logo.getHeight() * this.getWidth() / logo.getWidth(), null);
            }
        };
        this.add(logoPanel);
    }

}
