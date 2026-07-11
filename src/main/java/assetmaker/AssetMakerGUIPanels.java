package assetmaker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 * Builds the editor tabs. Each method below represents one logical asset
 * section. To add a new UI section, create a buildXTab method, add its controls
 * to AssetMakerGUI, add the tab in AssetMakerGUI.buildEditorTabs() using
 * {@code wrapEditorTab(...)} so it remains reachable on small windows, and
 * update AssetMakerGUIController for loading and saving.
 */
class AssetMakerGUIPanels {

    static final String[] DAMAGE_KEYS = {
            "Brute", "Asphyxiation", "Burn", "Toxin", "Genetic", "Structural", "Bleeding"
    };

    private final AssetMakerGUI gui;

    AssetMakerGUIPanels(AssetMakerGUI gui) {
        this.gui = gui;
    }

    static void addLabel(JPanel p, GridBagConstraints c, String text, int col, int row) {
        c.gridx = col; c.gridy = row;
        JLabel lbl = new JLabel(text);
        lbl.setPreferredSize(new Dimension(110, lbl.getPreferredSize().height));
        p.add(lbl, c);
    }

    /** Identity, IDs, direction, and state flags. */
    JPanel buildIdentityTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        c.weightx = 0; addLabel(p, c, "Name:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.nameField, c); row++;
        c.weightx = 0; addLabel(p, c, "Entity ID:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.idField, c); row++;
        c.weightx = 0; addLabel(p, c, "Map:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.mapField, c); row++;
        c.weightx = 0; addLabel(p, c, "Direction:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.directionCombo, c); row++;
        c.weightx = 0; addLabel(p, c, "Selected Slot:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.selectedSlotField, c); row++;

        row++;
        JLabel flagsHeader = new JLabel("State flags");
        flagsHeader.setFont(flagsHeader.getFont().deriveFont(Font.BOLD));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        p.add(flagsHeader, c); c.gridwidth = 1; row++;

        JPanel flagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        flagRow.add(gui.aliveBox);
        flagRow.add(gui.canCollideBox);
        flagRow.add(gui.castShadowBox);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        p.add(flagRow, c); c.gridwidth = 1; row++;

        row++;
        gui.loadedEntityLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        gui.loadedEntityLabel.setVerticalAlignment(SwingConstants.TOP);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        p.add(new JScrollPane(gui.loadedEntityLabel), c);
        return p;
    }

    /** Position, velocity, size, speed, and anchoring. */
    JPanel buildMovementTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        c.weightx = 0; addLabel(p, c, "Position X:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.posXField, c); row++;
        c.weightx = 0; addLabel(p, c, "Position Y:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.posYField, c); row++;
        c.weightx = 0; addLabel(p, c, "Velocity X:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.velXField, c); row++;
        c.weightx = 0; addLabel(p, c, "Velocity Y:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.velYField, c); row++;

        row++;
        c.weightx = 0; addLabel(p, c, "Size X:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.sizeXField, c); row++;
        c.weightx = 0; addLabel(p, c, "Size Y:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.sizeYField, c); row++;

        row++;
        c.weightx = 0; addLabel(p, c, "Speed:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.speedField, c); row++;
        c.weightx = 0; addLabel(p, c, "Max Speed:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.maxSpeedField, c); row++;
        c.weightx = 0; addLabel(p, c, "Reach:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.reachField, c); row++;

        row++;
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        p.add(gui.anchoredBox, c); c.gridwidth = 1; row++;
        c.weighty = 1; p.add(Box.createVerticalGlue(), c);
        return p;
    }

    /** Health, light emission, damage, multipliers, and hit cooldown. */
    JPanel buildCombatTab() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel health = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        health.add(new JLabel("Max HP:"));
        health.add(gui.maxHealthSpinner);
        health.add(Box.createHorizontalStrut(12));
        health.add(new JLabel("Crit HP:"));
        health.add(gui.critHealthSpinner);
        root.add(health);

        JPanel light = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        light.add(new JLabel("Light Range:"));
        light.add(gui.lightRangeField);
        light.add(new JLabel("(emitted by this entity, optional)"));
        root.add(light);

        root.add(Box.createVerticalStrut(8));
        root.add(makeDamageTable("Damage dealt (per tick)", gui.dmgValues));
        root.add(Box.createVerticalStrut(8));
        root.add(makeDamageTable("Damage multipliers", gui.dmgMultValues));
        root.add(Box.createVerticalStrut(8));
        root.add(makeDamageTable("Hit damage (on contact)", gui.hitDmgValues));
        root.add(Box.createVerticalStrut(8));
        JPanel cooldownRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cooldownRow.add(new JLabel("Hit Cooldown (ms):"));
        cooldownRow.add(gui.hitCooldownSpinner);
        cooldownRow.add(new JLabel("(milliseconds before can attack again)"));
        root.add(cooldownRow);
        root.add(Box.createVerticalGlue());
        return root;
    }

    /** Item flags, stack amount, and inventory slot references. */
    JPanel buildItemTab() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        flags.add(gui.isItemBox);
        flags.add(gui.stackableBox);
        root.add(flags);

        JPanel amount = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        amount.add(new JLabel("Amount:"));
        amount.add(gui.amountSpinner);
        root.add(amount);

        JPanel inv = new JPanel();
        inv.setLayout(new BoxLayout(inv, BoxLayout.Y_AXIS));
        inv.setBorder(BorderFactory.createTitledBorder("Inventory slots (slot=itemName)"));
        JLabel invHelp = new JLabel("<html>One entry per line, e.g. <i>leftHand=flint axe head</i>. " +
                "Leave blank if this entity has no inventory.</html>");
        invHelp.setFont(invHelp.getFont().deriveFont(Font.PLAIN, 11f));
        inv.add(invHelp);
        gui.inventorySlotsField.setLineWrap(true);
        gui.inventorySlotsField.setWrapStyleWord(true);
        JScrollPane invScroll = new JScrollPane(gui.inventorySlotsField);
        invScroll.setPreferredSize(new Dimension(400, 200));
        inv.add(invScroll);
        root.add(inv);

        root.add(Box.createVerticalGlue());
        return root;
    }

    /** Tags and visual presentation fields used by the client. */
    JPanel buildTagsTab() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel tagsHelp = new JLabel("<html>Comma-separated tags. Used by the tag system to find entities " +
                "(e.g. <i>fungus</i>, <i>flammable</i>).</html>");
        tagsHelp.setFont(tagsHelp.getFont().deriveFont(Font.PLAIN, 11f));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1;
        root.add(tagsHelp, c);

        c.gridwidth = 1; c.gridy = 1; c.weightx = 0; addLabel(root, c, "Tags:", 0, 1);
        c.weightx = 1; c.gridx = 1; root.add(gui.tagsField, c);

        c.weightx = 0; addLabel(root, c, "Display Texture:", 0, 2);
        c.weightx = 1; c.gridx = 1; root.add(gui.displayTextureField, c);

        c.weightx = 0; addLabel(root, c, "Hex Color:", 0, 3);
        c.weightx = 1; c.gridx = 1; root.add(gui.hexColorField, c);

        JLabel colorHelp = new JLabel("<html>Optional tint color in <code>#RRGGBB</code> or <code>#AARRGGBB</code> " +
                "format. Used to recolor the sprite.</html>");
        colorHelp.setFont(colorHelp.getFont().deriveFont(Font.PLAIN, 11f));
        colorHelp.setForeground(new Color(110, 110, 110));
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; c.weightx = 1;
        root.add(colorHelp, c);

        c.weighty = 1;
        root.add(Box.createVerticalGlue(), c);
        return root;
    }

    /** Drop entries created when an entity is defeated. */
    JPanel buildLootTableTab() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel help = new JLabel("<html>Define what items this entity drops when defeated. " +
                "Probability is out of 100% (e.g. 25 = 25% chance). " +
                "Amount is optional; leave 0 or 1 for single drop.</html>");
        help.setFont(help.getFont().deriveFont(Font.PLAIN, 11f));
        help.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        root.add(help, BorderLayout.NORTH);

        gui.lootTable.setFillsViewportHeight(true);
        gui.lootTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        gui.lootTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        gui.lootTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        JScrollPane scroll = new JScrollPane(gui.lootTable);
        scroll.setPreferredSize(new Dimension(600, 250));
        root.add(scroll, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnAdd = new JButton("Add Entry");
        JButton btnRemove = new JButton("Remove Selected");
        JButton btnClear = new JButton("Clear All");
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnRemove);
        buttonPanel.add(btnClear);
        root.add(buttonPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            gui.lootTableModel.addRow(new Object[]{"new_item", 100.0, 1});
        });
        btnRemove.addActionListener(e -> {
            int row = gui.lootTable.getSelectedRow();
            if (row >= 0) {
                gui.lootTableModel.removeRow(row);
            }
        });
        btnClear.addActionListener(e -> {
            gui.lootTableModel.setRowCount(0);
        });

        return root;
    }

    /**
     * Less frequently edited fields from Entity.proto. Internal values use
     * {@code name=id}; chemical lines use {@code id=amount}.
     */
    JPanel buildAdvancedTab() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        body.add(gui.dropsABodyBox);
        body.add(new JLabel("Internal space:"));
        body.add(gui.internalSpaceSpinner);
        root.add(body);

        JPanel internal = new JPanel(new BorderLayout());
        internal.setBorder(BorderFactory.createTitledBorder("Internal values (name=id)"));
        internal.add(new JLabel("Runtime/custom values. Leave blank unless another system expects them."), BorderLayout.NORTH);
        gui.internalValuesField.setLineWrap(true);
        gui.internalValuesField.setWrapStyleWord(true);
        internal.add(new JScrollPane(gui.internalValuesField), BorderLayout.CENTER);
        internal.setPreferredSize(new Dimension(600, 120));
        root.add(internal);

        JPanel organs = new JPanel(new GridBagLayout());
        organs.setBorder(BorderFactory.createTitledBorder("Organs"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        int row = 0;
        c.gridx = 0; c.gridy = row; organs.add(gui.heartBox, c);
        c.gridx = 1; organs.add(new JLabel("Blood:"), c);
        c.gridx = 2; c.weightx = 1; organs.add(gui.heartBloodField, c);
        c.gridx = 3; c.weightx = 0; organs.add(new JLabel("Max blood:"), c);
        c.gridx = 4; c.weightx = 1; organs.add(gui.heartMaxBloodField, c); row++;
        c.gridx = 0; c.gridy = row; c.weightx = 0; organs.add(gui.lungsBox, c);
        c.gridx = 1; organs.add(new JLabel("Oxygen:"), c);
        c.gridx = 2; c.weightx = 1; organs.add(gui.lungsOxygenSpinner, c); row++;
        c.gridx = 0; c.gridy = row; c.weightx = 0; organs.add(gui.liverBox, c);
        c.gridx = 1; organs.add(new JLabel("Detoxification:"), c);
        c.gridx = 2; c.weightx = 1; organs.add(gui.liverDetoxificationSpinner, c); row++;
        c.gridx = 0; c.gridy = row; c.gridwidth = 5; organs.add(gui.brainBox, c); row++;
        addChemicalArea(organs, gui.stomachChemicalsField, "Stomach chemical IDs (one per line):", row++);
        addChemicalArea(organs, gui.cardiovascularChemicalsField, "Cardiovascular chemicals (id=amount):", row);
        root.add(organs);
        root.add(Box.createVerticalGlue());
        return root;
    }

    private static void addChemicalArea(JPanel panel, JTextArea area, String label, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        panel.add(new JLabel(label), c);
        area.setRows(2);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        c.gridx = 2; c.gridwidth = 3; c.weightx = 1;
        panel.add(new JScrollPane(area), c);
    }

    private JPanel makeDamageTable(String title, JSpinner[] spinners) {
        JPanel table = new JPanel(new GridBagLayout());
        table.setBorder(BorderFactory.createTitledBorder(title));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < DAMAGE_KEYS.length; i++) {
            c.gridx = 0; c.gridy = i; c.weightx = 0;
            table.add(new JLabel(DAMAGE_KEYS[i] + ":"), c);
            c.gridx = 1; c.weightx = 1;
            table.add(spinners[i], c);
        }
        return table;
    }
}
