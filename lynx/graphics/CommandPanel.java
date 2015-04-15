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

public class CommandPanel extends JPanel {

    private static final long serialVersionUID = 2390916431716936366L;

    private static final Logger log = Logger.getLogger(CommandPanel.class.getName());

    //logo image
    private BufferedImage logo;

    //buttons
    private JButton openButton;
    private JButton clusterButton;
    private JButton mapButton;

    //opened file
    private File openedFile;
    
    //text field for opened file
    private JLabel fileNameLabel;
    
    //progress bars for algorithms
    private JProgressBar clusterProgress;
    private JProgressBar mapProgress;
    
    
    public CommandPanel() {
        super(new GridLayout(2, 1));

        init();
    }

    public void init() {

        try {
            logo = ImageIO.read(new File("logo/logo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // panel for logo
        JPanel logoPanel = new JPanel(new GridLayout(1, 1)) {
            private static final long serialVersionUID = -1092410231325842147L;

            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(logo, 0, 0, this.getWidth(), logo.getHeight() * this.getWidth() / logo.getWidth(), null);
            }
        };
        this.add(logoPanel);

        // panel to put all the buttons (and progress bars) we need
        JPanel buttonsPanel = new JPanel(new GridLayout(7, 1));
        
        //text field to show name of opened file
        fileNameLabel = new JLabel("No File Opened");
        fileNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        //button to open file
        openButton = new JButton("Open Design") {
            private static final long serialVersionUID = -3169712968203420370L;
        };
        openButton.addActionListener(new ActionListener() {
            private final JFileChooser fc = new JFileChooser();

            @Override
            public void actionPerformed(ActionEvent e) {
                // Handle open button action.
                if (e.getSource() == openButton) {
                    int returnVal = fc.showOpenDialog(CommandPanel.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        openedFile = fc.getSelectedFile();
                        log.info("Opening: " + openedFile.getName() + ".");
                        fileNameLabel.setText(openedFile.getName());
                    } else {
                        log.info("Open command cancelled by user.");
                    }
                }
            }
        });
        
        //progress bars
        clusterProgress = new JProgressBar();
        clusterProgress.setIndeterminate(true);
        mapProgress = new JProgressBar();
        mapProgress.setIndeterminate(true);
        
        //Tarjan clustering button
        clusterButton = new JButton("Cluster Design");
        
        //NoC mapping button
        mapButton = new JButton("Map Design to NoC");
        
        //add all buttons to panel in the right order
        buttonsPanel.add(openButton);
        buttonsPanel.add(fileNameLabel);
        buttonsPanel.add(clusterButton);
        buttonsPanel.add(clusterProgress);
        buttonsPanel.add(mapButton);
        buttonsPanel.add(mapProgress);
        this.add(buttonsPanel);
    }

}
