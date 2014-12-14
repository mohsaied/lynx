package lynx.graphics;

import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import lynx.data.Design;

public class MainPanel extends JPanel {

    private static final long serialVersionUID = 2L;

    public MainPanel(Design design) {
        super(new GridLayout(1, 1));

        //panel has a tabbed pane on it
        JTabbedPane tabbedPane = new JTabbedPane();

        //first tab is the graph of given application
        GraphPanel panel1 = new GraphPanel(design);
        tabbedPane.addTab("Graph", null, panel1, "Visualize the provided connectivity graph");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        // Add the tabbed pane to this panel.
        this.add(tabbedPane);

        // The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

}
