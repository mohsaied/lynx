package lynx.graphics;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import lynx.data.Design;
import lynx.data.MyEnums;

public class Utils {

    public static void initWindow(Design design) {

        JFrame gui = new JFrame(MyEnums.NOCLYNX);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MainPanel graphLabel = new MainPanel(design);
        gui.setLocationRelativeTo(null);
        gui.getContentPane().add(graphLabel, BorderLayout.CENTER);
        gui.setBounds(100, 100, 400, 400);
        gui.setVisible(true);
    }

}
