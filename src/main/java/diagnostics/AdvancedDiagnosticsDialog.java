package diagnostics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import diagnostics.ResourceDiagnostics.EntitySnapshot;
import diagnostics.ResourceDiagnostics.ProcessSnapshot;
import diagnostics.ResourceDiagnostics.ThreadSnapshot;

/** Live, expandable diagnostics UI for process, thread, and entity resources. */
@SuppressWarnings("serial")
public class AdvancedDiagnosticsDialog extends JDialog {
    private static final Color BACKGROUND = new Color(30, 30, 30);
    private static final Color PANEL = new Color(42, 42, 42);
    private static final Color TEXT = new Color(225, 225, 225);
    private final ResourceDiagnostics diagnostics;
    private final JLabel processSummary = new JLabel();
    private final JTree threadTree = createTree("Loading threads...");
    private final JTree entityTree = createTree("Enter a name or ID, then press Search.");
    private final JTextField entitySearch = new JTextField(24);
    private final JLabel entityCount = new JLabel("0 matches");
    private final Timer refreshTimer;

    public AdvancedDiagnosticsDialog(java.awt.Frame owner, ResourceDiagnostics diagnostics) {
        super(owner, "Advanced Diagnostics", false);
        this.diagnostics = diagnostics;
        setSize(940, 680);
        setMinimumSize(new Dimension(720, 500));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(BACKGROUND);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Resources", createProcessPanel());
        tabs.addTab("Threads", createThreadPanel());
        tabs.addTab("Entity Finder", createEntityPanel());
        add(tabs, BorderLayout.CENTER);

        refreshTimer = new Timer(2000, event -> refreshLiveData());
        refreshTimer.start();
        refreshLiveData();
    }

    private JPanel createProcessPanel() {
        JPanel panel = darkPanel(new BorderLayout(10, 10));
        processSummary.setForeground(TEXT);
        processSummary.setFont(new Font("Consolas", Font.PLAIN, 16));
        processSummary.setVerticalAlignment(JLabel.TOP);
        processSummary.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        panel.add(processSummary, BorderLayout.NORTH);

        JLabel note = new JLabel("<html><b>Measurement notes</b><br><br>"
                + "CPU is the Java server process load. RAM is live JVM heap usage. Disk is the serialized worldRoot size. "
                + "Internet counts bytes read and written by connected player sockets since this server started.<br><br>"
                + "Java exposes CPU and allocated memory per thread, but not reliable per-thread disk I/O. "
                + "Entity RAM and disk values are serialized-data estimates; entity CPU covers measured entity tick/tag work."
                + "</html>");
        note.setForeground(TEXT);
        note.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        panel.add(note, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createThreadPanel() {
        JPanel panel = darkPanel(new BorderLayout());
        JLabel help = label("Expand a thread to see CPU, allocated RAM, internet traffic, state, and its current Java stack.");
        help.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(help, BorderLayout.NORTH);
        panel.add(darkScroll(threadTree), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEntityPanel() {
        JPanel panel = darkPanel(new BorderLayout(5, 5));
        JPanel searchBar = darkPanel(new FlowLayout(FlowLayout.LEFT));
        searchBar.add(label("Name or ID:"));
        styleField(entitySearch);
        searchBar.add(entitySearch);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(event -> refreshEntities());
        entitySearch.addActionListener(event -> refreshEntities());
        searchBar.add(searchButton);
        entityCount.setForeground(TEXT);
        searchBar.add(entityCount);
        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(darkScroll(entityTree), BorderLayout.CENTER);
        return panel;
    }

    private void refreshLiveData() {
        ProcessSnapshot process = diagnostics.getProcessSnapshot();
        processSummary.setText("<html>CPU: <b>" + percent(process.cpuPercent) + "</b><br>"
                + "RAM: <b>" + bytes(process.heapUsed) + " used</b> / " + bytes(process.heapCommitted)
                + " committed / " + bytes(process.heapMax) + " maximum<br>"
                + "Threads: <b>" + process.threads + "</b><br>"
                + "Disk (world data): <b>" + bytes(process.worldBytes) + "</b><br>"
                + "Internet: <b>" + bytes(process.networkRead) + " received</b> / "
                + bytes(process.networkWritten) + " sent</html>");
        refreshThreads();
    }

    private void refreshThreads() {
        Set<Long> expanded = expandedIds(threadTree);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Server threads");
        for (ThreadSnapshot thread : diagnostics.getThreadSnapshots()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeLabel(thread.id,
                    thread.name + "  |  CPU " + percent(thread.cpuPercent) + "  |  " + thread.state));
            node.add(new DefaultMutableTreeNode("CPU time: " + duration(thread.cpuNanos)));
            node.add(new DefaultMutableTreeNode("RAM allocations: " + bytes(thread.allocatedBytes)
                    + " total; " + bytes((long) thread.allocationBytesPerSecond) + "/s recently"));
            node.add(new DefaultMutableTreeNode("Disk: unavailable per JVM thread"));
            node.add(new DefaultMutableTreeNode("Internet: " + bytes(thread.networkRead) + " received; "
                    + bytes(thread.networkWritten) + " sent"));
            DefaultMutableTreeNode stack = new DefaultMutableTreeNode("Current stack (what it is running)");
            for (String line : thread.stack.split("\\n")) stack.add(new DefaultMutableTreeNode(line));
            node.add(stack);
            root.add(node);
        }
        threadTree.setModel(new DefaultTreeModel(root));
        threadTree.expandRow(0);
        restoreExpanded(threadTree, expanded);
    }

    private void refreshEntities() {
        List<EntitySnapshot> matches = diagnostics.findEntities(entitySearch.getText());
        entityCount.setText(matches.size() + (matches.size() == 1 ? " match" : " matches"));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(matches.size() + " matching entities");
        for (EntitySnapshot entity : matches) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeLabel(entity.id,
                    entity.name + "  (#" + entity.id + ")  |  CPU " + percent(entity.cpuPercent)));
            node.add(new DefaultMutableTreeNode("CPU: " + percent(entity.cpuPercent)
                    + " recently; " + duration(entity.cpuNanos) + " measured total"));
            node.add(new DefaultMutableTreeNode("RAM estimate: " + bytes(entity.serializedBytes)));
            node.add(new DefaultMutableTreeNode("Disk estimate: " + bytes(entity.serializedBytes) + " when serialized"));
            node.add(new DefaultMutableTreeNode("Internet attribution: " + bytes(entity.networkBytes) + " sent in packets"));
            DefaultMutableTreeNode details = new DefaultMutableTreeNode("Everything this entity contains");
            for (String line : entity.details.split("\\n")) details.add(new DefaultMutableTreeNode(line));
            node.add(details);
            root.add(node);
        }
        entityTree.setModel(new DefaultTreeModel(root));
        entityTree.expandRow(0);
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }

    private static JTree createTree(String rootText) {
        JTree tree = new JTree(new DefaultMutableTreeNode(rootText));
        tree.setBackground(BACKGROUND); tree.setForeground(TEXT);
        tree.setFont(new Font("Consolas", Font.PLAIN, 13));
        tree.setRootVisible(true); tree.setShowsRootHandles(true);
        ToolTipManager.sharedInstance().registerComponent(tree);
        return tree;
    }

    private static JScrollPane darkScroll(JTree tree) {
        JScrollPane scroll = new JScrollPane(tree);
        scroll.getViewport().setBackground(BACKGROUND);
        return scroll;
    }

    private static JPanel darkPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setBackground(PANEL); return panel;
    }

    private static JLabel label(String text) { JLabel label = new JLabel(text); label.setForeground(TEXT); return label; }
    private static void styleField(JTextField field) { field.setBackground(BACKGROUND); field.setForeground(TEXT); field.setCaretColor(TEXT); }

    private static Set<Long> expandedIds(JTree tree) {
        Set<Long> result = new HashSet<>();
        for (int row = 0; row < tree.getRowCount(); row++) {
            if (!tree.isExpanded(row)) continue;
            Object value = ((DefaultMutableTreeNode) tree.getPathForRow(row).getLastPathComponent()).getUserObject();
            if (value instanceof NodeLabel) result.add(((NodeLabel) value).id);
        }
        return result;
    }

    private static void restoreExpanded(JTree tree, Set<Long> ids) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            Object value = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (value instanceof NodeLabel && ids.contains(((NodeLabel) value).id)) tree.expandPath(path);
        }
    }

    private static String percent(double value) { return value < 0 ? "unavailable" : String.format("%.2f%%", value); }
    private static String duration(long nanos) { return nanos < 0 ? "unavailable" : String.format("%.3f s", nanos / 1_000_000_000.0); }
    public static String bytes(long value) {
        if (value < 1024) return value + " B";
        String[] units = { "KB", "MB", "GB", "TB" };
        double amount = value;
        int unit = -1;
        do { amount /= 1024.0; unit++; } while (amount >= 1024 && unit < units.length - 1);
        return String.format("%.2f %s", amount, units[unit]);
    }

    private static final class NodeLabel {
        final long id; final String text;
        NodeLabel(long id, String text) { this.id = id; this.text = text; }
        @Override public String toString() { return text; }
    }
}
