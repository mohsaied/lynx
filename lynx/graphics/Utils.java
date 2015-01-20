package lynx.graphics;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import lynx.data.Design;
import lynx.data.MyEnums;

public class Utils {

    public static void initWindow(Design design) {

        // JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame gui = new JFrame(MyEnums.NOCLYNX);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MainPanel mainLabel = new MainPanel(design);
        gui.setLocationRelativeTo(null);
        gui.getContentPane().add(mainLabel, BorderLayout.CENTER);
        gui.setBounds(100, 100, 800, 600);
        gui.setVisible(true);
    }
}
