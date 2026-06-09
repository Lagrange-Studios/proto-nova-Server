package assetmaker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.LootTableItemProto.lootTableItem;

public class AssetMakerGUI {

    final AssetMaker assetMaker = new AssetMaker();

    final JFrame frame = new JFrame("Proto Nova - Asset Maker");

    final DefaultListModel<String> entityListModel = new DefaultListModel<>();
    final JList<String> entityList = new JList<>(entityListModel);
    final JTextField searchField = new JTextField(18);
    final JTextArea otherAssetsArea = new JTextArea();

    final JTextField nameField = new JTextField();
    final JTextField idField = new JTextField();
    final JTextField mapField = new JTextField();
    final JTextField selectedSlotField = new JTextField();
    final JComboBox<Direction> directionCombo = new JComboBox<>(new Direction[]{
            Direction.Up, Direction.Down, Direction.Left, Direction.Right
    });
    final JTextField tagsField = new JTextField();
    final JTextField displayTextureField = new JTextField();
    final JTextField hexColorField = new JTextField();

    final JTextField posXField = new JTextField();
    final JTextField posYField = new JTextField();
    final JTextField velXField = new JTextField();
    final JTextField velYField = new JTextField();
    final JTextField sizeXField = new JTextField();
    final JTextField sizeYField = new JTextField();
    final JTextField speedField = new JTextField();
    final JTextField maxSpeedField = new JTextField();
    final JTextField reachField = new JTextField();
    final JCheckBox anchoredBox = new JCheckBox("Anchored");
    final JCheckBox canCollideBox = new JCheckBox("Can Collide");
    final JCheckBox castShadowBox = new JCheckBox("Cast Shadow");
    final JCheckBox aliveBox = new JCheckBox("Alive");

    final JSpinner[] dmgValues = new JSpinner[AssetMakerGUIPanels.DAMAGE_KEYS.length];
    final JSpinner[] dmgMultValues = new JSpinner[AssetMakerGUIPanels.DAMAGE_KEYS.length];
    final JSpinner[] hitDmgValues = new JSpinner[AssetMakerGUIPanels.DAMAGE_KEYS.length];
    final JSpinner maxHealthSpinner = new JSpinner(new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 1));
    final JSpinner critHealthSpinner = new JSpinner(new SpinnerNumberModel(50, 0, Integer.MAX_VALUE, 1));
    final JTextField lightRangeField = new JTextField();

    final JCheckBox isItemBox = new JCheckBox("Is Item");
    final JCheckBox stackableBox = new JCheckBox("Stackable");
    final JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
    final JTextArea inventorySlotsField = new JTextArea();

    // Loot table fields
    final DefaultTableModel lootTableModel = new DefaultTableModel(new Object[]{"Item Name", "Probability (%)", "Amount"}, 0) {
        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
                case 0: return String.class;
                case 1: return Double.class;
                case 2: return Integer.class;
                default: return Object.class;
            }
        }
    };
    final javax.swing.JTable lootTable = new javax.swing.JTable(lootTableModel);

    final JLabel statusLabel = new JLabel(" ");
    final JLabel loadedEntityLabel = new JLabel(" ");

    String currentAssetName = null;
    Entity currentEntity = null;
    boolean dirty = false;

    private AssetMakerGUIController controller;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            AssetMakerGUI window = new AssetMakerGUI();
            window.frame.setVisible(true);
        });
    }

    public AssetMakerGUI() {
        initialize();
        controller = new AssetMakerGUIController(this);
        controller.refreshAssetList();
    }

    private void initialize() {

        for (int i = 0; i < AssetMakerGUIPanels.DAMAGE_KEYS.length; i++) {
            dmgValues[i] = new JSpinner(new SpinnerNumberModel(0.0f, -10000.0f, 10000.0f, 0.1));
            dmgMultValues[i] = new JSpinner(new SpinnerNumberModel(1.0f, 0.0f, 1000.0f, 0.1));
            hitDmgValues[i] = new JSpinner(new SpinnerNumberModel(0.0f, 0.0f, 10000.0f, 0.1));
        }

        frame.setSize(1200, 780);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(buildToolbar(), BorderLayout.NORTH);
        frame.getContentPane().add(buildMainSplit(), BorderLayout.CENTER);
        frame.getContentPane().add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnNew = new JButton("+ New Asset");
        JButton btnSave = new JButton("Save");
        JButton btnSaveAs = new JButton("Save As...");
        JButton btnDelete = new JButton("Delete");
        JButton btnReload = new JButton("Reload");
        JButton btnRefresh = new JButton("Refresh List");
        JButton btnExportRaw = new JButton("Show Raw");
        left.add(btnNew); left.add(btnSave); left.add(btnSaveAs);
        left.add(btnDelete); left.add(btnReload); left.add(btnRefresh);
        left.add(Box.createHorizontalStrut(12));
        left.add(btnExportRaw);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.add(new JLabel("Filter:"));
        right.add(searchField);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        btnNew.addActionListener(e -> controller.onNewAsset());
        btnSave.addActionListener(e -> controller.onSave(false));
        btnSaveAs.addActionListener(e -> controller.onSave(true));
        btnDelete.addActionListener(e -> controller.onDelete());
        btnReload.addActionListener(e -> controller.onReload());
        btnRefresh.addActionListener(e -> controller.refreshAssetList());
        btnExportRaw.addActionListener(e -> controller.onShowRaw());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { controller.applyFilter(); }
            public void removeUpdate(DocumentEvent e) { controller.applyFilter(); }
            public void changedUpdate(DocumentEvent e) { controller.applyFilter(); }
        });

        return bar;
    }

    private JPanel buildMainSplit() {
        JPanel split = new JPanel(new BorderLayout());
        split.add(buildLeftPanel(), BorderLayout.WEST);
        split.add(buildEditorTabs(), BorderLayout.CENTER);
        return split;
    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setPreferredSize(new Dimension(320, 0));
        left.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel header = new JLabel("Entity Assets  (assets/entities/)");
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        left.add(header, BorderLayout.NORTH);

        entityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entityList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                if (value != null && value.toString().equals(currentAssetName)) {
                    lbl.setText("* " + value);
                }
                return lbl;
            }
        });
        entityList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            String selected = entityList.getSelectedValue();
            if (selected == null) return;
            if (dirty && !selected.equals(currentAssetName)) {
                int r = JOptionPane.showConfirmDialog(frame,
                        "You have unsaved changes. Discard them?",
                        "Unsaved changes",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (r != JOptionPane.YES_OPTION) {
                    entityList.setSelectedValue(currentAssetName, true);
                    return;
                }
            }
            controller.loadAssetIntoEditor(selected);
        });

        JScrollPane scroll = new JScrollPane(entityList);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        left.add(scroll, BorderLayout.CENTER);

        JPanel other = new JPanel(new BorderLayout());
        other.setBorder(BorderFactory.createTitledBorder("Other asset folders (read only)"));
        otherAssetsArea.setEditable(false);
        otherAssetsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        otherAssetsArea.setBackground(new Color(245, 245, 245));
        otherAssetsArea.setRows(8);
        other.add(new JScrollPane(otherAssetsArea), BorderLayout.CENTER);
        left.add(other, BorderLayout.SOUTH);

        return left;
    }

    private JTabbedPane buildEditorTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        AssetMakerGUIPanels p = new AssetMakerGUIPanels(this);
        tabs.addTab("Identity", p.buildIdentityTab());
        tabs.addTab("Movement", p.buildMovementTab());
        tabs.addTab("Combat", p.buildCombatTab());
        tabs.addTab("Item / Stack", p.buildItemTab());
        tabs.addTab("Tags & Display", p.buildTagsTab());
        tabs.addTab("Loot Table", p.buildLootTableTab());
        return tabs;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    void refreshOtherAssetsPanel() {
        StringBuilder sb = new StringBuilder();
        File root = new File("assets");
        appendFolderListing(sb, root, 0);
        otherAssetsArea.setText(sb.length() == 0 ? "(no assets/ folder found)" : sb.toString());
        otherAssetsArea.setCaretPosition(0);
    }

    private static void appendFolderListing(StringBuilder sb, File dir, int depth) {
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(dir.getName()).append("/\n");
        for (File f : files) {
            if (f.isDirectory()) {
                appendFolderListing(sb, f, depth + 1);
            } else {
                for (int i = 0; i <= depth; i++) sb.append("  ");
                sb.append(f.getName()).append('\n');
            }
        }
    }
}
