package lynx.graphics;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import lynx.data.Design;
import lynx.data.MyEnums;

public class Gui extends JFrame {

    private static final long serialVersionUID = 5661237900482388080L;

    private MainPanel mainPanel;
    
    public Gui(Design design) throws InterruptedException {
        super(MyEnums.NOCLYNX);
        
        // JFrame.setDefaultLookAndFeelDecorated(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());
        this.setLocationRelativeTo(null);

        // create the mainpanel
        mainPanel = new MainPanel(design);

        // Console output
        JTextArea ta = new JTextArea();
        TextAreaOutputStream taos = new TextAreaOutputStream(ta);
        PrintStream ps = new PrintStream(taos);
        System.setOut(ps);
        System.setErr(ps);

        JScrollPane sp = new JScrollPane(ta);
        sp.setAutoscrolls(true);

        // Create a split pane with the two scroll panes in it.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, sp);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(600);

        // Provide minimum sizes for the two components in the split pane
        Dimension minimumSize = new Dimension(100, 50);
        mainPanel.setMinimumSize(minimumSize);
        sp.setMinimumSize(minimumSize);
        this.getContentPane().add(splitPane, BorderLayout.CENTER);

        this.pack();
        this.setVisible(true);
        this.setBounds(150, 200, 800, 800);
        this.setVisible(true);
    }
    
    public void setDesign(Design design){
        mainPanel.setDesign(design);
    }
}
