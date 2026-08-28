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

import health.OrganEnergy;

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
        lbl.setPreferredSize(new Dimension(185, lbl.getPreferredSize().height));
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
        JPanel starter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        starter.setBorder(BorderFactory.createTitledBorder("Easy setup"));
        starter.add(new JLabel("I am making a:"));
        starter.add(gui.starterTypeCombo);
        JButton applyStarter = new JButton("Fill in safe starting values");
        starter.add(applyStarter);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        p.add(starter, c); c.gridwidth = 1; row++;

        JLabel startHelp = new JLabel("Choose the closest kind of thing first. You can change any answer afterward.");
        startHelp.setForeground(new Color(90, 90, 90));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; p.add(startHelp, c); c.gridwidth = 1; row++;

        row++;
        c.weightx = 0; addLabel(p, c, "What is it called?", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.nameField, c); row++;
        c.weightx = 0; addLabel(p, c, "Saved ID (automatic):", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.idField, c); row++;
        c.weightx = 0; addLabel(p, c, "Starting map number:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.mapField, c); row++;
        c.weightx = 0; addLabel(p, c, "Which way does it face?", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.directionCombo, c); row++;
        c.weightx = 0; addLabel(p, c, "Starting hand slot:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.selectedSlotField, c); row++;

        row++;
        JLabel flagsHeader = new JLabel("Simple yes-or-no choices");
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
        applyStarter.addActionListener(e -> gui.applyStarterPreset(
                String.valueOf(gui.starterTypeCombo.getSelectedItem())));
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
        JLabel help = new JLabel("Tip: 1 means one map tile. Most saved assets can start at X = 0 and Y = 0.");
        help.setForeground(new Color(90, 90, 90));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1; p.add(help, c);
        c.gridwidth = 1; row++;
        c.weightx = 0; addLabel(p, c, "Start left/right (X):", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.posXField, c); row++;
        c.weightx = 0; addLabel(p, c, "Start up/down (Y):", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.posYField, c); row++;
        c.weightx = 0; addLabel(p, c, "Moving left/right now:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.velXField, c); row++;
        c.weightx = 0; addLabel(p, c, "Moving up/down now:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.velYField, c); row++;

        row++;
        c.weightx = 0; addLabel(p, c, "Width (tiles):", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.sizeXField, c); row++;
        c.weightx = 0; addLabel(p, c, "Height (tiles):", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.sizeYField, c); row++;

        row++;
        c.weightx = 0; addLabel(p, c, "Speed right now:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.speedField, c); row++;
        c.weightx = 0; addLabel(p, c, "Fastest speed:", 0, row);
        c.weightx = 1; c.gridx = 1; p.add(gui.maxSpeedField, c); row++;
        c.weightx = 0; addLabel(p, c, "Touching reach (tiles):", 0, row);
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
        health.add(new JLabel("Max health (damage needed to die):"));
        health.add(gui.maxHealthSpinner);
        health.add(Box.createHorizontalStrut(12));
        health.add(new JLabel("Critical health (damage needed to collapse):"));
        health.add(gui.critHealthSpinner);
        root.add(health);

        JPanel light = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        light.add(new JLabel("How far its light shines:"));
        light.add(gui.lightRangeField);
        light.add(new JLabel("(emitted by this entity, optional)"));
        root.add(light);

        root.add(Box.createVerticalStrut(8));
        root.add(makeDamageTable("Damage (current damage, usually all zero)", gui.dmgValues));
        root.add(Box.createVerticalStrut(8));
        root.add(makeDamageTable("Damage multiplier (1 = normal, 2 = double)", gui.dmgMultValues));
        root.add(Box.createVerticalStrut(8));
        root.add(makeDamageTable("Hit damage (damage dealt when it hits)", gui.hitDmgValues));
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
        flags.add(gui.canDestroyBox);
        flags.add(gui.consumableBox);
        root.add(flags);

        JLabel durabilityHelp = new JLabel("<html>Items are indestructible unless <b>Can be damaged and destroyed</b> is enabled. " +
                "This setting does not affect creatures or other non-item entities.</html>");
        durabilityHelp.setFont(durabilityHelp.getFont().deriveFont(Font.PLAIN, 11f));
        root.add(durabilityHelp);

        JPanel amount = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        amount.add(new JLabel("How many are in this stack?"));
        amount.add(gui.amountSpinner);
        root.add(amount);

        JPanel inv = new JPanel();
        inv.setLayout(new BoxLayout(inv, BoxLayout.Y_AXIS));
        inv.setBorder(BorderFactory.createTitledBorder("Things it starts out holding (optional)"));
        JLabel invHelp = new JLabel("<html>Put one on each line. Example: <i>leftHand = #123</i>. " +
                "The number is the saved item's ID. Leave this empty if you are unsure.</html>");
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

    /** Entity temperature, carried chemicals, and schema-defined custom values. */
    JPanel buildChemistryTab() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel temperature = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        temperature.add(new JLabel("Temperature:"));
        temperature.add(gui.temperatureSpinner);
        temperature.add(new JLabel("Used when checking chemical reaction temperature ranges."));
        root.add(temperature);

        JPanel chemicals = new JPanel(new BorderLayout());
        chemicals.setBorder(BorderFactory.createTitledBorder("Chemicals carried by this entity"));
        chemicals.add(new JLabel("Enter one per line as chemicalName=amountU."), BorderLayout.NORTH);
        gui.entityChemicalsField.setRows(5);
        gui.entityChemicalsField.setLineWrap(true);
        gui.entityChemicalsField.setWrapStyleWord(true);
        chemicals.add(new JScrollPane(gui.entityChemicalsField), BorderLayout.CENTER);
        root.add(chemicals);

        JPanel customData = new JPanel(new BorderLayout());
        customData.setBorder(BorderFactory.createTitledBorder("Custom data"));
        JLabel customHelp = new JLabel("Each key stores all five protobuf value types; unused values can stay at their defaults.");
        customData.add(customHelp, BorderLayout.NORTH);

        gui.customDataTable.setFillsViewportHeight(true);
        gui.customDataTable.getColumnModel().getColumn(0).setPreferredWidth(170);
        gui.customDataTable.getColumnModel().getColumn(5).setPreferredWidth(220);
        JScrollPane customScroll = new JScrollPane(gui.customDataTable);
        customScroll.setPreferredSize(new Dimension(700, 220));
        customData.add(customScroll, BorderLayout.CENTER);

        JPanel customButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton add = new JButton("Add Value");
        JButton remove = new JButton("Remove Selected");
        JButton clear = new JButton("Clear All");
        customButtons.add(add);
        customButtons.add(remove);
        customButtons.add(clear);
        customData.add(customButtons, BorderLayout.SOUTH);

        add.addActionListener(e -> gui.customDataModel.addRow(
                new Object[]{"newKey", 0, 0.0f, 0.0d, false, ""}));
        remove.addActionListener(e -> {
            int row = gui.customDataTable.getSelectedRow();
            if (row >= 0) gui.customDataModel.removeRow(row);
        });
        clear.addActionListener(e -> gui.customDataModel.setRowCount(0));

        root.add(customData);
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

        JLabel tagsHelp = new JLabel("<html>Behavior words tell the game what this thing can do. Separate them with commas. " +
                "Example: <i>plant, harvestable</i>. Leave blank if it needs no special behavior.</html>");
        tagsHelp.setFont(tagsHelp.getFont().deriveFont(Font.PLAIN, 11f));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1;
        root.add(tagsHelp, c);

        c.gridwidth = 1; c.gridy = 1; c.weightx = 0; addLabel(root, c, "Behavior words:", 0, 1);
        c.weightx = 1; c.gridx = 1; root.add(gui.tagsField, c);

        c.weightx = 0; addLabel(root, c, "Picture name:", 0, 2);
        c.weightx = 1; c.gridx = 1; root.add(gui.displayTextureField, c);

        c.weightx = 0; addLabel(root, c, "Optional color:", 0, 3);
        c.weightx = 1; c.gridx = 1; root.add(gui.hexColorField, c);

        c.weightx = 0; addLabel(root, c, "Render priority:", 0, 4);
        c.weightx = 1; c.gridx = 1; root.add(gui.renderPrioritySpinner, c);

        JLabel colorHelp = new JLabel("<html>Optional tint color in <code>#RRGGBB</code> or <code>#AARRGGBB</code> " +
                "format. Used to recolor the sprite. Higher render priority values draw on top.</html>");
        colorHelp.setFont(colorHelp.getFont().deriveFont(Font.PLAIN, 11f));
        colorHelp.setForeground(new Color(110, 110, 110));
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2; c.weightx = 1;
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

    /** Organ state, resource rates, chemical storage, and cybernetic power. */
    JPanel buildPhysiologyTab() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel help = new JLabel("Easy choice: click 'Use a Normal Human Body' and leave the detailed numbers alone.");
        help.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        root.add(help);

        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton standardBody = new JButton("Use a Normal Human Body");
        JButton clearBody = new JButton("This Has No Organs");
        presets.add(standardBody);
        presets.add(clearBody);
        root.add(presets);

        JPanel templates = new JPanel(new GridBagLayout());
        templates.setBorder(BorderFactory.createTitledBorder("Which saved organs should this body receive?"));
        addOrganField(templates, "Heart name:", gui.heartOrganAssetField, 0);
        addOrganField(templates, "Lungs name:", gui.lungsOrganAssetField, 1);
        addOrganField(templates, "Liver name:", gui.liverOrganAssetField, 2);
        addOrganField(templates, "Brain name:", gui.brainOrganAssetField, 3);
        addOrganField(templates, "Stomach name:", gui.stomachOrganAssetField, 4);
        root.add(templates);

        JLabel detailHelp = new JLabel("Detailed organ numbers are below. The normal-human button fills them in for you.");
        detailHelp.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        detailHelp.setForeground(new Color(90, 90, 90));
        root.add(detailHelp);

        root.add(buildHeartPanel());
        root.add(buildLungsPanel());
        root.add(buildLiverPanel());
        root.add(buildBrainPanel());
        root.add(buildStomachPanel());
        root.add(buildCirculationPanel());
        root.add(Box.createVerticalGlue());

        standardBody.addActionListener(e -> applyStandardBodyDefaults());
        clearBody.addActionListener(e -> {
            gui.heartBox.setSelected(false);
            gui.lungsBox.setSelected(false);
            gui.liverBox.setSelected(false);
            gui.brainBox.setSelected(false);
            gui.stomachBox.setSelected(false);
            gui.cardiovascularBox.setSelected(false);
        });
        return root;
    }

    private JPanel buildHeartPanel() {
        JPanel panel = organPanel("Heart", gui.heartBox, gui.heartStatus);
        addOrganField(panel, "Current blood volume (u):", gui.heartBloodSpinner, 4);
        addOrganField(panel, "Maximum blood volume (u):", gui.heartMaxBloodSpinner, 5);
        addOrganField(panel, "Circulation capacity (u/s):", gui.heartCirculationSpinner, 6);
        addOrganField(panel, "Oxygen demand (u/s):", gui.heartOxygenUseSpinner, 7);
        return panel;
    }

    private JPanel buildLungsPanel() {
        JPanel panel = organPanel("Lungs", gui.lungsBox, gui.lungsStatus);
        addOrganField(panel, "Oxygen transfer capacity (u/s):", gui.lungsOxygenSpinner, 4);
        addOrganField(panel, "Oxygen demand (u/s):", gui.lungsOxygenUseSpinner, 5);
        return panel;
    }

    private JPanel buildLiverPanel() {
        JPanel panel = organPanel("Liver", gui.liverBox, gui.liverStatus);
        addOrganField(panel, "Detoxification capacity (u/s):", gui.liverDetoxificationSpinner, 4);
        addOrganField(panel, "Oxygen demand (u/s):", gui.liverOxygenUseSpinner, 5);
        return panel;
    }

    private JPanel buildBrainPanel() {
        JPanel panel = organPanel("Brain", gui.brainBox, gui.brainStatus);
        addOrganField(panel, "Oxygen demand (u/s):", gui.brainOxygenUseSpinner, 4);
        return panel;
    }

    private JPanel buildStomachPanel() {
        JPanel panel = organPanel("Stomach", gui.stomachBox, gui.stomachStatus);
        addOrganField(panel, "Chemical capacity (u):", gui.stomachCapacitySpinner, 4);
        addOrganField(panel, "Absorption capacity (u/s):", gui.stomachAbsorptionSpinner, 5);
        addOrganField(panel, "Oxygen demand (u/s):", gui.stomachOxygenUseSpinner, 6);
        addChemicalArea(panel, gui.stomachChemicalsField,
                "Contents (chemicalName=amountU, one per line):", 7);
        return panel;
    }

    private JPanel buildCirculationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Circulatory System"));
        GridBagConstraints c = organConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        panel.add(gui.cardiovascularBox, c);
        addOrganField(panel, "Stored blood oxygen (u):", gui.cardiovascularOxygenSpinner, 1);
        addOrganField(panel, "Maximum blood oxygen (u):", gui.cardiovascularMaxOxygenSpinner, 2);
        addOrganField(panel, "Stored electrical power (u):", gui.cardiovascularPowerSpinner, 3);
        addOrganField(panel, "Maximum electrical power (u):", gui.cardiovascularMaxPowerSpinner, 4);
        addOrganField(panel, "Stored nutrition (u):", gui.cardiovascularNutritionSpinner, 5);
        addOrganField(panel, "Maximum nutrition (u):", gui.cardiovascularMaxNutritionSpinner, 6);
        addOrganField(panel, "Total fluid capacity (blood + chemicals, u):",
                gui.cardiovascularFluidCapacitySpinner, 7);
        addChemicalArea(panel, gui.cardiovascularChemicalsField,
                "Bloodstream chemicals (chemicalName=amountU):", 8);
        return panel;
    }

    private JPanel organPanel(String title, javax.swing.JCheckBox present,
                              AssetMakerGUI.OrganStatusControls status) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        GridBagConstraints c = organConstraints();
        c.gridx = 0; c.gridy = 0; panel.add(present, c);
        c.gridx = 1; panel.add(new JLabel("Type:"), c);
        c.gridx = 2; c.gridwidth = 2; c.weightx = 1; panel.add(status.type, c);
        c.gridwidth = 1; c.weightx = 0;
        c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Health (%):"), c);
        c.gridx = 1; panel.add(status.healthPercent, c);
        c.gridx = 2; panel.add(new JLabel("Efficiency (%):"), c);
        c.gridx = 3; c.weightx = 1; panel.add(status.efficiencyPercent, c);
        c.weightx = 0;
        c.gridx = 0; c.gridy = 2; panel.add(new JLabel("Cybernetic power demand (u/s):"), c);
        c.gridx = 1; c.gridwidth = 3; c.weightx = 1; panel.add(status.powerUse, c);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 1; c.weightx = 0;
        panel.add(new JLabel("Biological nutrition demand (u/s):"), c);
        c.gridx = 1; c.gridwidth = 3; c.weightx = 1; panel.add(status.nutritionUse, c);
        return panel;
    }

    private static GridBagConstraints organConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private static void addOrganField(JPanel panel, String label, java.awt.Component field, int row) {
        GridBagConstraints c = organConstraints();
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.gridwidth = 3; c.weightx = 1;
        panel.add(field, c);
    }

    private void applyStandardBodyDefaults() {
        gui.heartBox.setSelected(true);
        gui.lungsBox.setSelected(true);
        gui.liverBox.setSelected(true);
        gui.brainBox.setSelected(true);
        gui.stomachBox.setSelected(true);
        gui.cardiovascularBox.setSelected(true);
        setBiological(gui.heartStatus, OrganEnergy.DEFAULT_HEART_NUTRITION_USE);
        setBiological(gui.lungsStatus, OrganEnergy.DEFAULT_LUNG_NUTRITION_USE);
        setBiological(gui.liverStatus, OrganEnergy.DEFAULT_LIVER_NUTRITION_USE);
        setBiological(gui.brainStatus, OrganEnergy.DEFAULT_BRAIN_NUTRITION_USE);
        setBiological(gui.stomachStatus, OrganEnergy.DEFAULT_STOMACH_NUTRITION_USE);
        gui.heartBloodSpinner.setValue(100.0);
        gui.heartMaxBloodSpinner.setValue(100.0);
        gui.heartCirculationSpinner.setValue(10.0);
        gui.heartOxygenUseSpinner.setValue(1.0);
        gui.lungsOxygenSpinner.setValue(10);
        gui.lungsOxygenUseSpinner.setValue(0.5);
        gui.liverDetoxificationSpinner.setValue(1);
        gui.liverOxygenUseSpinner.setValue(1.0);
        gui.brainOxygenUseSpinner.setValue(3.0);
        gui.stomachCapacitySpinner.setValue(50.0);
        gui.stomachAbsorptionSpinner.setValue(1.0);
        gui.stomachOxygenUseSpinner.setValue(0.5);
        gui.cardiovascularOxygenSpinner.setValue(100.0);
        gui.cardiovascularMaxOxygenSpinner.setValue(100.0);
        gui.cardiovascularPowerSpinner.setValue(0.0);
        gui.cardiovascularMaxPowerSpinner.setValue(0.0);
        gui.cardiovascularNutritionSpinner.setValue(50.0);
        gui.cardiovascularMaxNutritionSpinner.setValue(100.0);
        gui.cardiovascularFluidCapacitySpinner.setValue(150.0);
        gui.heartOrganAssetField.setText("human heart");
        gui.lungsOrganAssetField.setText("human lungs");
        gui.liverOrganAssetField.setText("human liver");
        gui.brainOrganAssetField.setText("human brain");
        gui.stomachOrganAssetField.setText("human stomach");
        if (!gui.tagsField.getText().contains("physiology")) {
            String tags = gui.tagsField.getText().trim();
            gui.tagsField.setText(tags.isEmpty() ? "physiology" : tags + ", physiology");
        }
    }

    private static void setBiological(
            AssetMakerGUI.OrganStatusControls status,
            double nutritionUsePerSecond) {
        status.type.setSelectedItem("Biological");
        status.healthPercent.setValue(100.0);
        status.efficiencyPercent.setValue(100.0);
        status.powerUse.setValue(0.0);
        status.nutritionUse.setValue(nutritionUsePerSecond);
    }

    /** Less frequently edited fields from Entity.proto. */
    JPanel buildAdvancedTab() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        body.add(gui.dropsABodyBox);
        body.add(new JLabel("Internal storage capacity:"));
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

        root.add(Box.createVerticalGlue());
        return root;
    }

    private static void addChemicalArea(JPanel panel, JTextArea area, String label, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        panel.add(new JLabel(label), c);
        area.setRows(2);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        c.gridx = 1; c.gridwidth = 3; c.weightx = 1;
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
