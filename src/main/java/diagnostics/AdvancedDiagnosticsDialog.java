package diagnostics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import diagnostics.ResourceDiagnostics.EntitySnapshot;
import diagnostics.ResourceDiagnostics.ProcessSnapshot;
import diagnostics.ResourceDiagnostics.ThreadSnapshot;

/** Live diagnostics that consume no monitoring resources while the window is closed. */
@SuppressWarnings("serial")
public class AdvancedDiagnosticsDialog extends JDialog {
    private static final Color BACKGROUND = new Color(30, 30, 30);
    private static final Color PANEL = new Color(42, 42, 42);
    private static final Color TEXT = new Color(225, 225, 225);
    private static final int REFRESH_MILLIS = 2_500;
    private static final int MAX_ENTITY_RESULTS = 200;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ResourceDiagnostics diagnostics;
    private final JLabel processSummary = new JLabel("Open the Resources tab to sample process usage.");
    private final JTree threadTree = createTree("Open the Threads tab to sample threads.");
    private final JTree entityTree = createTree("Enter a name or ID, then press Search.");
    private final JTextField entitySearch = new JTextField(24);
    private final JLabel entityCount = new JLabel("No search running");
    private final JLabel sampleStatus = new JLabel("Diagnostics are idle");
    private final JTabbedPane tabs = new JTabbedPane();
    private final Timer refreshTimer;
    private final AtomicBoolean sampling = new AtomicBoolean();
    private final AtomicLong entitySearchGeneration = new AtomicLong();
    private final Set<Integer> loadingEntityDetails = new HashSet<>();

    private ExecutorService sampler;
    private boolean sessionActive;

    public AdvancedDiagnosticsDialog(java.awt.Frame owner, ResourceDiagnostics diagnostics) {
        super(owner, "Advanced Diagnostics", false);
        this.diagnostics = diagnostics;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(940, 680);
        setMinimumSize(new Dimension(720, 500));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(BACKGROUND);

        tabs.addTab("Resources", createProcessPanel());
        tabs.addTab("Threads", createThreadPanel());
        tabs.addTab("Entity Finder", createEntityPanel());
        tabs.addChangeListener(event -> requestRefresh());
        add(tabs, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        threadTree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            @Override public void treeExpanded(javax.swing.event.TreeExpansionEvent event) { requestRefresh(); }
            @Override public void treeCollapsed(javax.swing.event.TreeExpansionEvent event) { }
        });
        entityTree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            @Override public void treeExpanded(javax.swing.event.TreeExpansionEvent event) {
                requestEntityDetails(event.getPath());
            }
            @Override public void treeCollapsed(javax.swing.event.TreeExpansionEvent event) { }
        });

        refreshTimer = new Timer(REFRESH_MILLIS, event -> requestRefresh());
        refreshTimer.setCoalesce(true);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) startSession();
        super.setVisible(visible);
        if (visible) requestRefresh(); else stopSession();
    }

    private void startSession() {
        if (sessionActive) return;
        diagnostics.beginSession();
        sessionActive = true;
        sampler = Executors.newSingleThreadExecutor(task -> {
            Thread thread = ResourceDiagnostics.threadFactory("Diagnostics-Sampler").newThread(task);
            thread.setDaemon(true);
            return thread;
        });
        refreshTimer.start();
        sampleStatus.setText("Live sampling active only while this window is open");
    }

    private void stopSession() {
        if (!sessionActive) return;
        sessionActive = false;
        refreshTimer.stop();
        entitySearchGeneration.incrementAndGet();
        ExecutorService executor = sampler;
        sampler = null;
        if (executor != null) executor.shutdownNow();
        sampling.set(false);
        diagnostics.endSession();
    }

    private JPanel createProcessPanel() {
        JPanel panel = darkPanel(new BorderLayout(10, 10));
        processSummary.setForeground(TEXT);
        processSummary.setFont(new Font("Consolas", Font.PLAIN, 16));
        processSummary.setVerticalAlignment(JLabel.TOP);
        processSummary.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        panel.add(processSummary, BorderLayout.NORTH);

        JLabel note = new JLabel("<html><b>Low-impact measurement</b><br><br>"
                + "All metric collection begins when this window opens and stops when it closes. "
                + "Only the visible tab is sampled. World disk size is cached for 30 seconds.<br><br>"
                + "CPU is the Java server process load. RAM is live JVM heap usage. Internet counts player-socket "
                + "traffic observed while diagnostics are open.</html>");
        note.setForeground(TEXT);
        note.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        panel.add(note, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createThreadPanel() {
        JPanel panel = darkPanel(new BorderLayout());
        JLabel help = label("Thread summaries are sampled on this tab. Expand a thread to capture only that stack.");
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

    private JPanel createStatusBar() {
        JPanel bar = darkPanel(new BorderLayout(8, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        sampleStatus.setForeground(TEXT);
        bar.add(sampleStatus, BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh now");
        refresh.addActionListener(event -> requestRefresh());
        bar.add(refresh, BorderLayout.EAST);
        return bar;
    }

    private void requestRefresh() {
        if (!sessionActive || sampler == null || !sampling.compareAndSet(false, true)) return;
        int selectedTab = tabs.getSelectedIndex();
        Set<Long> expandedThreads = selectedTab == 1 ? expandedIds(threadTree) : java.util.Collections.emptySet();
        long started = System.nanoTime();
        try {
            sampler.execute(() -> {
                try {
                    if (selectedTab == 0) {
                        ProcessSnapshot process = diagnostics.getProcessSnapshot();
                        SwingUtilities.invokeLater(() -> applyProcessSnapshot(process, started));
                    } else if (selectedTab == 1) {
                        List<ThreadSnapshot> threads = diagnostics.getThreadSnapshots(expandedThreads);
                        SwingUtilities.invokeLater(() -> applyThreadSnapshots(threads, expandedThreads, started));
                    } else {
                        finishSample(started);
                    }
                } catch (RuntimeException exception) {
                    SwingUtilities.invokeLater(() -> {
                        sampleStatus.setText("Sampling failed: " + safeMessage(exception));
                        sampling.set(false);
                    });
                }
            });
        } catch (RejectedExecutionException closed) {
            sampling.set(false);
        }
    }

    private void applyProcessSnapshot(ProcessSnapshot process, long started) {
        if (!sessionActive) return;
        processSummary.setText("<html>CPU: <b>" + percent(process.cpuPercent) + "</b><br>"
                + "RAM: <b>" + bytes(process.heapUsed) + " used</b> / " + bytes(process.heapCommitted)
                + " committed / " + bytes(process.heapMax) + " maximum<br>"
                + "Threads: <b>" + process.threads + "</b><br>"
                + "Disk (world data): <b>" + bytes(process.worldBytes) + "</b><br>"
                + "Internet while open: <b>" + bytes(process.networkRead) + " received</b> / "
                + bytes(process.networkWritten) + " sent</html>");
        finishSample(started);
    }

    private void applyThreadSnapshots(List<ThreadSnapshot> threads, Set<Long> expanded, long started) {
        if (!sessionActive) return;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Server threads (" + threads.size() + ")");
        for (ThreadSnapshot thread : threads) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeLabel(thread.id,
                    thread.name + "  |  CPU " + percent(thread.cpuPercent) + "  |  " + thread.state));
            node.add(new DefaultMutableTreeNode("CPU time: " + duration(thread.cpuNanos)));
            node.add(new DefaultMutableTreeNode("RAM allocations: " + bytes(thread.allocatedBytes)
                    + " total; " + bytes((long) thread.allocationBytesPerSecond) + "/s recently"));
            node.add(new DefaultMutableTreeNode("Internet while open: " + bytes(thread.networkRead)
                    + " received; " + bytes(thread.networkWritten) + " sent"));
            DefaultMutableTreeNode stack = new DefaultMutableTreeNode("Current stack");
            for (String line : thread.stack.split("\\n")) stack.add(new DefaultMutableTreeNode(line));
            node.add(stack);
            root.add(node);
        }
        threadTree.setModel(new DefaultTreeModel(root));
        threadTree.expandRow(0);
        restoreExpanded(threadTree, expanded);
        finishSample(started);
    }

    private void refreshEntities() {
        String query = entitySearch.getText().trim();
        if (query.isEmpty()) {
            entityCount.setText("Enter a name or numeric ID");
            entityTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Search is intentionally blank.")));
            return;
        }
        if (!sessionActive || sampler == null) return;
        long generation = entitySearchGeneration.incrementAndGet();
        loadingEntityDetails.clear();
        entityCount.setText("Searching...");
        try {
            sampler.execute(() -> {
                List<EntitySnapshot> matches = diagnostics.findEntities(query, MAX_ENTITY_RESULTS);
                SwingUtilities.invokeLater(() -> applyEntitySnapshots(matches, generation));
            });
        } catch (RejectedExecutionException closed) {
            entityCount.setText("Diagnostics closed");
        }
    }

    private void applyEntitySnapshots(List<EntitySnapshot> matches, long generation) {
        if (!sessionActive || generation != entitySearchGeneration.get()) return;
        boolean capped = matches.size() == MAX_ENTITY_RESULTS;
        entityCount.setText(matches.size() + (capped ? "+ shown; refine the search" : matches.size() == 1 ? " match" : " matches"));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(matches.size() + (capped ? "+ matching entities" : " matching entities"));
        for (EntitySnapshot entity : matches) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeLabel(entity.id,
                    entity.name + "  (#" + entity.id + ")  |  CPU " + percent(entity.cpuPercent)));
            node.add(new DefaultMutableTreeNode("CPU while open: " + percent(entity.cpuPercent)
                    + " recently; " + duration(entity.cpuNanos) + " measured total"));
            node.add(new DefaultMutableTreeNode("Serialized size estimate: " + bytes(entity.serializedBytes)));
            node.add(new DefaultMutableTreeNode("Attributed network while open: " + bytes(entity.networkBytes)));
            node.add(new DefaultMutableTreeNode("Entity contents load when this result is expanded"));
            root.add(node);
        }
        entityTree.setModel(new DefaultTreeModel(root));
        entityTree.expandRow(0);
    }

    private void requestEntityDetails(TreePath path) {
        if (!sessionActive || sampler == null || path == null) return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object value = node.getUserObject();
        if (!(value instanceof NodeLabel)) return;
        int entityId = (int) ((NodeLabel) value).id;
        if (!loadingEntityDetails.add(entityId)) return;
        long generation = entitySearchGeneration.get();
        try {
            sampler.execute(() -> {
                String details = diagnostics.getEntityDetails(entityId);
                SwingUtilities.invokeLater(() -> applyEntityDetails(node, path, entityId, details, generation));
            });
        } catch (RejectedExecutionException closed) {
            loadingEntityDetails.remove(entityId);
        }
    }

    private void applyEntityDetails(DefaultMutableTreeNode node, TreePath path, int entityId,
            String details, long generation) {
        loadingEntityDetails.remove(entityId);
        if (!sessionActive || generation != entitySearchGeneration.get() || node.getChildCount() == 0) return;
        DefaultMutableTreeNode contents = new DefaultMutableTreeNode("Entity contents");
        for (String line : details.split("\\n")) contents.add(new DefaultMutableTreeNode(line));
        node.remove(node.getChildCount() - 1);
        node.add(contents);
        ((DefaultTreeModel) entityTree.getModel()).reload(node);
        entityTree.expandPath(path);
    }

    private void finishSample(long started) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> finishSample(started));
            return;
        }
        long millis = (System.nanoTime() - started) / 1_000_000;
        if (sessionActive) sampleStatus.setText("Updated " + LocalTime.now().format(CLOCK) + " in " + millis + " ms");
        sampling.set(false);
    }

    @Override
    public void dispose() {
        stopSession();
        ToolTipManager.sharedInstance().unregisterComponent(threadTree);
        ToolTipManager.sharedInstance().unregisterComponent(entityTree);
        super.dispose();
    }

    private static JTree createTree(String rootText) {
        JTree tree = new JTree(new DefaultMutableTreeNode(rootText));
        tree.setBackground(BACKGROUND);
        tree.setForeground(TEXT);
        tree.setFont(new Font("Consolas", Font.PLAIN, 13));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        ToolTipManager.sharedInstance().registerComponent(tree);
        return tree;
    }

    private static JScrollPane darkScroll(JTree tree) {
        JScrollPane scroll = new JScrollPane(tree);
        scroll.getViewport().setBackground(BACKGROUND);
        return scroll;
    }

    private static JPanel darkPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(PANEL);
        return panel;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        return label;
    }

    private static void styleField(JTextField field) {
        field.setBackground(BACKGROUND);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
    }

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

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String percent(double value) {
        return value < 0 ? "unavailable" : String.format("%.2f%%", value);
    }

    private static String duration(long nanos) {
        return nanos < 0 ? "unavailable" : String.format("%.3f s", nanos / 1_000_000_000.0);
    }

    public static String bytes(long value) {
        if (value < 0) return "unavailable";
        if (value < 1024) return value + " B";
        String[] units = { "KB", "MB", "GB", "TB" };
        double amount = value;
        int unit = -1;
        do { amount /= 1024.0; unit++; } while (amount >= 1024 && unit < units.length - 1);
        return String.format("%.2f %s", amount, units[unit]);
    }

    private static final class NodeLabel {
        final long id;
        final String text;
        NodeLabel(long id, String text) { this.id = id; this.text = text; }
        @Override public String toString() { return text; }
    }
}
