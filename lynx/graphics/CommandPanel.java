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
import javax.swing.JPanel;

public class CommandPanel extends JPanel {

    private static final long serialVersionUID = 2390916431716936366L;

    private static final Logger log = Logger.getLogger(CommandPanel.class.getName());

    private BufferedImage logo;

    private JButton openButton;
    private JButton clusterButton;
    private JButton mapButton;

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

        // panel to put all the buttons we need
        JPanel buttonsPanel = new JPanel(new GridLayout(3, 1));
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
                        File file = fc.getSelectedFile();
                        log.info("Opening: " + file.getName() + ".");
                    } else {
                        log.info("Open command cancelled by user.");
                    }
                }
            }
        });
        clusterButton = new JButton("Cluster Design");
        mapButton = new JButton("Map Design to NoC");
        buttonsPanel.add(openButton);
        buttonsPanel.add(clusterButton);
        buttonsPanel.add(mapButton);
        this.add(buttonsPanel);
    }

}
