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
import javax.swing.AbstractButton;
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

/**
 * Swing window for editing entity protobuf assets.
 *
 * <p>This class owns controls and layout only. Button behavior belongs in
 * {@link AssetMakerGUIController}; tab layout belongs in
 * {@link AssetMakerGUIPanels}. To add a field, add the control in the matching
 * section below, place it on the matching tab, then read/write it in the
 * controller.</p>
 */
public class AssetMakerGUI {

    // ===== Shared application state =====
    final AssetMaker assetMaker = new AssetMaker();

    final JFrame frame = new JFrame("Proto Nova - Asset Maker");

    // ===== Asset browser controls =====
    final DefaultListModel<String> entityListModel = new DefaultListModel<>();
    final JList<String> entityList = new JList<>(entityListModel);
    final JTextField searchField = new JTextField(18);
    final JTextArea otherAssetsArea = new JTextArea();
    final JComboBox<String> starterTypeCombo = new JComboBox<>(new String[]{
            "Solid world object", "Item you can pick up", "Living creature", "Human body", "Organ"
    });

    // ===== Identity and display controls =====
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
    final JSpinner renderPrioritySpinner = new JSpinner(
            new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));

    // ===== Movement controls =====
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

    // ===== Combat controls =====
    final JSpinner[] dmgValues = new JSpinner[AssetMakerGUIPanels.DAMAGE_KEYS.length];
    final JSpinner[] dmgMultValues = new JSpinner[AssetMakerGUIPanels.DAMAGE_KEYS.length];
    final JSpinner[] hitDmgValues = new JSpinner[AssetMakerGUIPanels.DAMAGE_KEYS.length];
    final JSpinner hitCooldownSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 30000, 50));
    final JSpinner maxHealthSpinner = new JSpinner(new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 1));
    final JSpinner critHealthSpinner = new JSpinner(new SpinnerNumberModel(50, 0, Integer.MAX_VALUE, 1));
    final JTextField lightRangeField = new JTextField();

    // ===== Item and inventory controls =====
    final JCheckBox isItemBox = new JCheckBox("Is Item");
    final JCheckBox stackableBox = new JCheckBox("Stackable");
    final JCheckBox canDestroyBox = new JCheckBox("Can be damaged and destroyed");
    final JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
    final JTextArea inventorySlotsField = new JTextArea();

    // ===== Loot table controls =====
    // Add a column here and update AssetMakerGUIController.buildLootTable() when
    // the protobuf loot entry gains another editable property.
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

    // ===== Advanced Entity.proto fields =====
    final JCheckBox dropsABodyBox = new JCheckBox("Drops a body");
    final JSpinner internalSpaceSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    final JTextArea internalValuesField = new JTextArea();
    final JCheckBox heartBox = new JCheckBox("Heart present");
    final JSpinner heartBloodSpinner = decimalSpinner(100, 0, 100000, 1);
    final JSpinner heartMaxBloodSpinner = decimalSpinner(100, 0, 100000, 1);
    final JSpinner heartCirculationSpinner = decimalSpinner(10, 0, 100000, 0.5);
    final JSpinner heartOxygenUseSpinner = decimalSpinner(1, 0, 100000, 0.1);
    final OrganStatusControls heartStatus = new OrganStatusControls();
    final JCheckBox lungsBox = new JCheckBox("Lungs present");
    final JSpinner lungsOxygenSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    final JSpinner lungsOxygenUseSpinner = decimalSpinner(0.5, 0, 100000, 0.1);
    final OrganStatusControls lungsStatus = new OrganStatusControls();
    final JCheckBox liverBox = new JCheckBox("Liver present");
    final JSpinner liverDetoxificationSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    final JSpinner liverOxygenUseSpinner = decimalSpinner(1, 0, 100000, 0.1);
    final OrganStatusControls liverStatus = new OrganStatusControls();
    final JCheckBox brainBox = new JCheckBox("Brain present");
    final JSpinner brainOxygenUseSpinner = decimalSpinner(3, 0, 100000, 0.1);
    final OrganStatusControls brainStatus = new OrganStatusControls();
    final JCheckBox stomachBox = new JCheckBox("Stomach present");
    final JSpinner stomachCapacitySpinner = decimalSpinner(50, 0, 100000, 1);
    final JSpinner stomachAbsorptionSpinner = decimalSpinner(1, 0, 100000, 0.1);
    final JSpinner stomachOxygenUseSpinner = decimalSpinner(0.5, 0, 100000, 0.1);
    final OrganStatusControls stomachStatus = new OrganStatusControls();
    final JTextArea stomachChemicalsField = new JTextArea();
    final JCheckBox cardiovascularBox = new JCheckBox("Circulatory system present");
    final JSpinner cardiovascularOxygenSpinner = decimalSpinner(0, 0, 100000, 1);
    final JSpinner cardiovascularMaxOxygenSpinner = decimalSpinner(100, 0, 100000, 1);
    final JSpinner cardiovascularPowerSpinner = decimalSpinner(0, 0, 100000, 1);
    final JSpinner cardiovascularMaxPowerSpinner = decimalSpinner(0, 0, 100000, 1);
    final JSpinner cardiovascularNutritionSpinner = decimalSpinner(100, 0, 100000, 1);
    final JSpinner cardiovascularMaxNutritionSpinner = decimalSpinner(100, 0, 100000, 1);
    final JSpinner cardiovascularFluidCapacitySpinner = decimalSpinner(150, 0, 100000, 1);
    final JTextArea cardiovascularChemicalsField = new JTextArea();
    final JTextField heartOrganAssetField = new JTextField();
    final JTextField lungsOrganAssetField = new JTextField();
    final JTextField liverOrganAssetField = new JTextField();
    final JTextField brainOrganAssetField = new JTextField();
    final JTextField stomachOrganAssetField = new JTextField();

    final JLabel statusLabel = new JLabel(" ");
    final JLabel loadedEntityLabel = new JLabel(" ");

    String currentAssetName = null;
    Entity currentEntity = null;
    boolean dirty = false;
    boolean updatingForm = false;

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
        installDirtyTracking();
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
        addSimpleHelp();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnNew = new JButton("+ Make Something New");
        JButton btnSave = new JButton("Save Changes");
        JButton btnSaveAs = new JButton("Make a Copy...");
        JButton btnDelete = new JButton("Delete");
        JButton btnReload = new JButton("Undo Unsaved Changes");
        JButton btnRefresh = new JButton("Refresh");
        JButton btnExportRaw = new JButton("Expert: Show Raw Data");
        left.add(btnNew); left.add(btnSave); left.add(btnSaveAs);
        left.add(btnDelete); left.add(btnReload); left.add(btnRefresh);
        left.add(Box.createHorizontalStrut(12));
        left.add(btnExportRaw);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.add(new JLabel("Find by name:"));
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
        left.setPreferredSize(new Dimension(270, 0));
        left.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel header = new JLabel("Saved Things");
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

        return left;
    }

    private JTabbedPane buildEditorTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        AssetMakerGUIPanels p = new AssetMakerGUIPanels(this);
        // Every editor section is wrapped in its own scroll pane. This keeps
        // lower controls reachable on smaller screens or at higher display scaling.
        tabs.addTab("1. Basics", wrapEditorTab(p.buildIdentityTab()));
        tabs.addTab("2. Place & Move", wrapEditorTab(p.buildMovementTab()));
        tabs.addTab("3. Health & Damage", wrapEditorTab(p.buildCombatTab()));
        tabs.addTab("4. Item & Storage", wrapEditorTab(p.buildItemTab()));
        tabs.addTab("5. Looks & Behaviors", wrapEditorTab(p.buildTagsTab()));
        tabs.addTab("6. Drops", wrapEditorTab(p.buildLootTableTab()));
        tabs.addTab("7. Body & Organs", wrapEditorTab(p.buildPhysiologyTab()));
        tabs.addTab("Expert (Usually Skip)", wrapEditorTab(p.buildAdvancedTab()));
        return tabs;
    }

    void applyStarterPreset(String choice) {
        controller.applyStarterPreset(choice);
    }

    private void addSimpleHelp() {
        idField.setEditable(false);
        idField.setToolTipText("The server gives each spawned thing its own ID. You normally leave this alone.");
        mapField.setToolTipText("Which world map this starts on. Most assets can leave this at 0.");
        nameField.setToolTipText("The name used by the game, such as wooden chair or red apple.");
        displayTextureField.setToolTipText("The picture name. Example: red apple");
        tagsField.setToolTipText("Behavior words separated by commas. Example: plant, harvestable");
        hexColorField.setToolTipText("Optional color. Example: #FF0000 is red.");
        renderPrioritySpinner.setToolTipText("Higher numbers draw on top of entities with lower numbers.");
        posXField.setToolTipText("How far left or right it starts. 0 is the middle.");
        posYField.setToolTipText("How far up or down it starts. 0 is the middle.");
        sizeXField.setToolTipText("Width in tiles. 1 means one tile wide.");
        sizeYField.setToolTipText("Height in tiles. 1 means one tile tall.");
        speedField.setToolTipText("How fast it moves now. Use 0 for things that do not move.");
        maxSpeedField.setToolTipText("The fastest it is allowed to move.");
        reachField.setToolTipText("How many tiles away it can touch something.");
        lightRangeField.setToolTipText("Leave blank for no light, or enter how many tiles it lights up.");
        anchoredBox.setToolTipText("Turn this on if it should stay in one place.");
        canCollideBox.setToolTipText("Turn this on if other things should bump into it instead of passing through.");
        aliveBox.setToolTipText("Turn this on for a living creature.");
        isItemBox.setToolTipText("Turn this on if a player can pick it up.");
        stackableBox.setToolTipText("Turn this on if several copies can share one inventory slot.");
        canDestroyBox.setToolTipText("Turn this on if attacks should be able to break this item.");
    }

    private void installDirtyTracking() {
        DocumentListener documentListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { markDirty(); }
            public void removeUpdate(DocumentEvent e) { markDirty(); }
            public void changedUpdate(DocumentEvent e) { markDirty(); }
        };
        JTextField[] textFields = {
                nameField, idField, mapField, selectedSlotField, tagsField, displayTextureField, hexColorField,
                posXField, posYField, velXField, velYField, sizeXField, sizeYField, speedField, maxSpeedField,
                reachField, lightRangeField, heartOrganAssetField, lungsOrganAssetField, liverOrganAssetField,
                brainOrganAssetField, stomachOrganAssetField
        };
        for (JTextField field : textFields) field.getDocument().addDocumentListener(documentListener);
        JTextArea[] textAreas = {
                inventorySlotsField, internalValuesField, stomachChemicalsField, cardiovascularChemicalsField
        };
        for (JTextArea area : textAreas) area.getDocument().addDocumentListener(documentListener);

        AbstractButton[] buttons = {
                anchoredBox, canCollideBox, castShadowBox, aliveBox, isItemBox, stackableBox, canDestroyBox,
                dropsABodyBox, heartBox, lungsBox, liverBox, brainBox, stomachBox, cardiovascularBox
        };
        for (AbstractButton button : buttons) button.addItemListener(e -> markDirty());
        directionCombo.addItemListener(e -> markDirty());
        starterTypeCombo.addItemListener(e -> { });

        List<JSpinner> spinners = new ArrayList<>();
        for (JSpinner spinner : dmgValues) spinners.add(spinner);
        for (JSpinner spinner : dmgMultValues) spinners.add(spinner);
        for (JSpinner spinner : hitDmgValues) spinners.add(spinner);
        JSpinner[] otherSpinners = {
                renderPrioritySpinner, hitCooldownSpinner, maxHealthSpinner, critHealthSpinner, amountSpinner, internalSpaceSpinner,
                heartBloodSpinner, heartMaxBloodSpinner, heartCirculationSpinner, heartOxygenUseSpinner,
                lungsOxygenSpinner, lungsOxygenUseSpinner, liverDetoxificationSpinner, liverOxygenUseSpinner,
                brainOxygenUseSpinner, stomachCapacitySpinner, stomachAbsorptionSpinner, stomachOxygenUseSpinner,
                cardiovascularOxygenSpinner, cardiovascularMaxOxygenSpinner, cardiovascularPowerSpinner,
                cardiovascularMaxPowerSpinner, cardiovascularNutritionSpinner,
                cardiovascularMaxNutritionSpinner, cardiovascularFluidCapacitySpinner
        };
        for (JSpinner spinner : otherSpinners) spinners.add(spinner);
        OrganStatusControls[] statuses = { heartStatus, lungsStatus, liverStatus, brainStatus, stomachStatus };
        for (OrganStatusControls status : statuses) {
            status.type.addItemListener(e -> markDirty());
            spinners.add(status.healthPercent);
            spinners.add(status.efficiencyPercent);
            spinners.add(status.powerUse);
            spinners.add(status.nutritionUse);
        }
        for (JSpinner spinner : spinners) spinner.addChangeListener(e -> markDirty());
        lootTableModel.addTableModelListener(e -> markDirty());
    }

    private void markDirty() {
        if (updatingForm || currentEntity == null || dirty) return;
        dirty = true;
        statusLabel.setText(" You have unsaved changes. Click Save Changes when you are ready.");
        entityList.repaint();
    }

    private static JSpinner decimalSpinner(double value, double minimum, double maximum, double step) {
        return new JSpinner(new SpinnerNumberModel(value, minimum, maximum, step));
    }

    static final class OrganStatusControls {
        final JComboBox<String> type = new JComboBox<>(new String[]{"Biological", "Cybernetic"});
        final JSpinner healthPercent = decimalSpinner(100, 0, 100, 1);
        final JSpinner efficiencyPercent = decimalSpinner(100, 0, 200, 1);
        final JSpinner powerUse = decimalSpinner(0, 0, 100000, 0.1);
        final JSpinner nutritionUse = decimalSpinner(0, 0, 100000, 0.001);
    }

    /**
     * Makes an editor tab vertically scrollable without allowing horizontal
     * scrolling to hide field labels. Add new tabs through this helper too.
     */
    private JScrollPane wrapEditorTab(JPanel content) {
        JScrollPane scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        return scroll;
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
