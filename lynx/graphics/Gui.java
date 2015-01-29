package lynx.graphics;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import lynx.data.Design;
import lynx.data.MyEnums;

public class Gui extends JFrame {

    private static final long serialVersionUID = 5661237900482388080L;

    private final int xSize = 800;
    private final int ySize = 700;
    private final int commandDivLoc = xSize / 4;
    private final int consoleDivLoc = ySize * 3 / 4;

    private MainPanel mainPanel;
    private CommandPanel commandPanel;

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

        JScrollPane console = new JScrollPane(ta);
        console.setAutoscrolls(true);

        // create the command panel
        commandPanel = new CommandPanel();

        // split the mainpanel and commanpanel
        JSplitPane commandSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, commandPanel, mainPanel);
        commandSplit.setOneTouchExpandable(true);
        commandSplit.setDividerLocation(commandDivLoc);

        // Create a split pane with the two scroll panes in it.
        JSplitPane consoleSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, commandSplit, console);
        consoleSplit.setOneTouchExpandable(true);
        consoleSplit.setDividerLocation(consoleDivLoc);

        this.getContentPane().add(consoleSplit, BorderLayout.CENTER);

        this.pack();
        this.setVisible(true);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setBounds(0, 0, xSize, ySize);
        this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
        this.setVisible(true);
    }

    public void setDesign(Design design) {
        mainPanel.setDesign(design);
    }
}
