package assetmaker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.DamageMultiplier;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.LootTableItemProto.lootTableItem;
import protonova.protobuf.OrgansProto.CardiovascularSystem;
import protonova.protobuf.OrgansProto.OrganAssetSlots;
import protonova.protobuf.OrgansProto.OrganComponent;
import protonova.protobuf.OrgansProto.OrganStatus;
import protonova.protobuf.OrgansProto.OrganType;
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.OrgansProto.Stomach;
import protonova.protobuf.VectorProto.Vector;

/**
 * Coordinates user actions and converts between Swing controls and protobufs.
 *
 * <p>The controller is intentionally organized in the same order as the GUI:
 * browser actions first, loading/resetting next, then protobuf read/write
 * helpers. When adding a field, update both the read path ({@link #populateForm(Entity)})
 * and the write path ({@link #buildEntityFromForm()}).</p>
 */
class AssetMakerGUIController {

    private final AssetMakerGUI gui;

    AssetMakerGUIController(AssetMakerGUI gui) {
        this.gui = gui;
    }

    
    
    void refreshAssetList() {
        String selected = gui.entityList.getSelectedValue();
        String previous = gui.currentAssetName;
        gui.entityListModel.clear();
        List<String> all = gui.assetMaker.listAssetNames();
        for (String name : all) {
            gui.entityListModel.addElement(name);
        }
        
        applyFilter();
        
        if (selected != null && all.contains(selected)) {
            gui.entityList.setSelectedValue(selected, true);
        } else if (previous != null && all.contains(previous)) {
            gui.entityList.setSelectedValue(previous, true);
        }
        gui.refreshOtherAssetsPanel();
        setStatus(all.size() + " entity asset(s) found in " + AssetMaker.ASSET_DIR);
    }

    void applyFilter() {
        String filter = gui.searchField.getText().trim().toLowerCase();
        String selected = gui.entityList.getSelectedValue();
        gui.entityListModel.clear();
        for (String name : gui.assetMaker.listAssetNames()) {
            if (filter.isEmpty() || name.toLowerCase().contains(filter)) {
                gui.entityListModel.addElement(name);
            }
        }
        if (selected != null && gui.entityListModel.contains(selected)) {
            gui.entityList.setSelectedValue(selected, true);
        }
    }

    
    
    void onNewAsset() {
        JTextField nameField = new JTextField(20);
        JComboBox<String> kindField = new JComboBox<>(new String[]{
                "Solid world object", "Item you can pick up", "Living creature", "Human body", "Organ"
        });
        int answer = JOptionPane.showConfirmDialog(gui.frame,
                new Object[]{
                        "What should your new thing be called?", nameField,
                        "What kind of thing is it?", kindField,
                        "You can change these choices later."
                },
                "Make Something New",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(gui.frame, "Please give it a name, such as 'wooden chair'.",
                    "It needs a name", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (gui.assetMaker.listAssetNames().contains(name)) {
            int r = JOptionPane.showConfirmDialog(gui.frame,
                    "An asset named '" + name + "' already exists. Overwrite?",
                    "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        Entity entity = gui.assetMaker.createAsset(name);
        if (!gui.assetMaker.saveEntity(name, entity)) {
            JOptionPane.showMessageDialog(gui.frame, "Failed to save new asset.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshAssetList();
        gui.entityList.setSelectedValue(name, true);
        loadAssetIntoEditor(name);
        gui.starterTypeCombo.setSelectedItem(kindField.getSelectedItem());
        applyStarterPreset(String.valueOf(kindField.getSelectedItem()));
        onSave(false);
        setStatus("Made " + name + ". Follow the numbered tabs if you want to change more.");
    }

    void applyStarterPreset(String choice) {
        gui.isItemBox.setSelected(false);
        gui.stackableBox.setSelected(false);
        gui.canDestroyBox.setSelected(false);
        gui.amountSpinner.setValue(1);
        gui.anchoredBox.setSelected(true);
        gui.canCollideBox.setSelected(true);
        gui.castShadowBox.setSelected(false);
        gui.aliveBox.setSelected(false);
        gui.speedField.setText("0");
        gui.maxSpeedField.setText("0");
        gui.reachField.setText("1.5");
        gui.renderPrioritySpinner.setValue(0);
        gui.sizeXField.setText("1");
        gui.sizeYField.setText("1");
        gui.velXField.setText("0");
        gui.velYField.setText("0");
        gui.maxHealthSpinner.setValue(100);
        gui.critHealthSpinner.setValue(50);
        gui.dropsABodyBox.setSelected(false);
        gui.heartBox.setSelected(false);
        gui.lungsBox.setSelected(false);
        gui.liverBox.setSelected(false);
        gui.brainBox.setSelected(false);
        gui.stomachBox.setSelected(false);
        gui.cardiovascularBox.setSelected(false);
        gui.heartOrganAssetField.setText("");
        gui.lungsOrganAssetField.setText("");
        gui.liverOrganAssetField.setText("");
        gui.brainOrganAssetField.setText("");
        gui.stomachOrganAssetField.setText("");
        for (int i = 0; i < AssetMakerGUIPanels.DAMAGE_KEYS.length; i++) {
            gui.dmgValues[i].setValue(0.0f);
            gui.dmgMultValues[i].setValue(1.0f);
            gui.hitDmgValues[i].setValue(0.0f);
        }

        if ("Item you can pick up".equals(choice)) {
            gui.isItemBox.setSelected(true);
            gui.anchoredBox.setSelected(false);
            gui.canCollideBox.setSelected(false);
        } else if ("Living creature".equals(choice)) {
            applyLivingDefaults(false);
        } else if ("Human body".equals(choice)) {
            applyLivingDefaults(true);
            applyHumanBodyDefaults();
        } else if ("Organ".equals(choice)) {
            gui.isItemBox.setSelected(true);
            gui.canDestroyBox.setSelected(true);
            gui.anchoredBox.setSelected(false);
            gui.canCollideBox.setSelected(false);
            setStatus("Organ starting values filled in. On '7. Body & Organs', choose exactly one organ.");
            return;
        }
        setStatus("Safe starting values filled in for: " + choice);
    }

    private void applyLivingDefaults(boolean human) {
        gui.anchoredBox.setSelected(false);
        gui.aliveBox.setSelected(true);
        gui.speedField.setText(human ? "7.5" : "5");
        gui.maxSpeedField.setText(human ? "7.5" : "5");
        gui.dropsABodyBox.setSelected(true);
        gui.selectedSlotField.setText("leftHand");
    }

    private void applyHumanBodyDefaults() {
        gui.heartBox.setSelected(true);
        gui.lungsBox.setSelected(true);
        gui.liverBox.setSelected(true);
        gui.brainBox.setSelected(true);
        gui.stomachBox.setSelected(true);
        gui.cardiovascularBox.setSelected(true);
        gui.heartOrganAssetField.setText("human heart");
        gui.lungsOrganAssetField.setText("human lungs");
        gui.liverOrganAssetField.setText("human liver");
        gui.brainOrganAssetField.setText("human brain");
        gui.stomachOrganAssetField.setText("human stomach");
        gui.heartBloodSpinner.setValue(100.0);
        gui.heartMaxBloodSpinner.setValue(100.0);
        gui.heartCirculationSpinner.setValue(10.0);
        gui.lungsOxygenSpinner.setValue(10);
        gui.liverDetoxificationSpinner.setValue(1);
        gui.stomachCapacitySpinner.setValue(50.0);
        gui.cardiovascularOxygenSpinner.setValue(100.0);
        gui.cardiovascularMaxOxygenSpinner.setValue(100.0);
        gui.cardiovascularFluidCapacitySpinner.setValue(150.0);
        if (!gui.tagsField.getText().contains("physiology")) {
            String tags = gui.tagsField.getText().trim();
            gui.tagsField.setText(tags.isEmpty() ? "physiology" : tags + ", physiology");
        }
    }

    void onSave(boolean saveAs) {
        String targetName;
        if (saveAs || gui.currentAssetName == null) {
            String suggested = saveAs && gui.currentAssetName != null ? gui.currentAssetName : "new entity";
            String entered = JOptionPane.showInputDialog(gui.frame,
                    "Save asset as:", suggested);
            if (entered == null) return;
            targetName = entered.trim();
            if (targetName.isEmpty()) {
                JOptionPane.showMessageDialog(gui.frame, "Asset name cannot be empty.",
                        "Invalid name", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            targetName = gui.currentAssetName;
        }

        Entity newEntity;
        try {
            newEntity = buildEntityFromForm();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(gui.frame,
                    "One answer needs to be a number.\n\n" + ex.getMessage() + "\n\n" +
                            "Example numbers: 0, 1, 2.5, or 100.",
                    "Please fix one answer", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!gui.assetMaker.saveEntity(targetName, newEntity)) {
            JOptionPane.showMessageDialog(gui.frame, "Failed to save asset.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshAssetList();
        gui.entityList.setSelectedValue(targetName, true);
        loadAssetIntoEditor(targetName);
        setStatus("Saved " + targetName);
    }

    void onDelete() {
        if (gui.currentAssetName == null) {
            JOptionPane.showMessageDialog(gui.frame, "No asset is currently loaded.",
                    "Nothing to delete", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int r = JOptionPane.showConfirmDialog(gui.frame,
                "Delete asset '" + gui.currentAssetName + "'?\n" +
                        "The file '" + AssetMaker.ASSET_DIR + gui.currentAssetName + ".data' " +
                        "will be removed from disk.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        if (!gui.assetMaker.deleteAsset(gui.currentAssetName)) {
            JOptionPane.showMessageDialog(gui.frame, "Failed to delete asset.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        gui.currentAssetName = null;
        gui.currentEntity = null;
        gui.dirty = false;
        clearForm();
        refreshAssetList();
        setStatus("Deleted asset");
    }

    void onReload() {
        if (gui.currentAssetName == null) return;
        Entity fresh = gui.assetMaker.loadAsset(gui.currentAssetName);
        if (fresh == null) {
            JOptionPane.showMessageDialog(gui.frame,
                    "Could not reload '" + gui.currentAssetName + "'. The file may be missing or corrupted.",
                    "Reload failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        loadAssetIntoEditor(gui.currentAssetName);
        setStatus("Reloaded " + gui.currentAssetName + " from disk");
    }

    void onShowRaw() {
        if (gui.currentEntity == null) {
            JOptionPane.showMessageDialog(gui.frame, "No asset is currently loaded.",
                    "Nothing to show", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String raw = gui.currentEntity.toString();
        JOptionPane.showMessageDialog(gui.frame,
                raw.length() > 4000 ? raw.substring(0, 4000) + "\n... (truncated)" : raw,
                "Raw protobuf: " + gui.currentAssetName,
                JOptionPane.INFORMATION_MESSAGE);
    }

    
    
    void loadAssetIntoEditor(String name) {
        Entity entity = gui.assetMaker.loadAsset(name);
        if (entity == null) {
            JOptionPane.showMessageDialog(gui.frame,
                    "Could not load '" + name + "'. The file may be missing or corrupted.",
                    "Load failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        gui.currentAssetName = name;
        gui.currentEntity = entity;
        gui.dirty = false;
        gui.updatingForm = true;
        try {
            populateForm(entity);
        } finally {
            gui.updatingForm = false;
        }
        updateLoadedEntityLabel(entity);
        setStatus("Loaded " + name);
    }

    /** Resets every editor section to its new-asset default. */
    private void clearForm() {
        gui.nameField.setText("");
        gui.idField.setText("0");
        gui.mapField.setText("0");
        gui.selectedSlotField.setText("");
        gui.directionCombo.setSelectedItem(Direction.Down);
        gui.tagsField.setText("");
        gui.displayTextureField.setText("");
        gui.hexColorField.setText("");

        gui.posXField.setText("0");
        gui.posYField.setText("0");
        gui.velXField.setText("0");
        gui.velYField.setText("0");
        gui.sizeXField.setText("1");
        gui.sizeYField.setText("1");
        gui.speedField.setText("0");
        gui.maxSpeedField.setText("0");
        gui.reachField.setText("0");
        gui.anchoredBox.setSelected(false);
        gui.canCollideBox.setSelected(false);
        gui.castShadowBox.setSelected(false);
        gui.aliveBox.setSelected(false);

        for (int i = 0; i < AssetMakerGUIPanels.DAMAGE_KEYS.length; i++) {
            gui.dmgValues[i].setValue(0.0f);
            gui.dmgMultValues[i].setValue(1.0f);
            gui.hitDmgValues[i].setValue(0.0f);
        }
        gui.maxHealthSpinner.setValue(0);
        gui.critHealthSpinner.setValue(0);
        gui.lightRangeField.setText("");
        gui.hitCooldownSpinner.setValue(0);

        gui.isItemBox.setSelected(false);
        gui.stackableBox.setSelected(false);
        gui.canDestroyBox.setSelected(false);
        gui.amountSpinner.setValue(0);
        gui.inventorySlotsField.setText("");

        gui.lootTableModel.setRowCount(0);

        // ===== Advanced Entity.proto fields =====
        gui.dropsABodyBox.setSelected(false);
        gui.internalSpaceSpinner.setValue(0);
        gui.internalValuesField.setText("");
        gui.heartBox.setSelected(false);
        gui.heartBloodSpinner.setValue(100.0);
        gui.heartMaxBloodSpinner.setValue(100.0);
        gui.heartCirculationSpinner.setValue(10.0);
        gui.heartOxygenUseSpinner.setValue(1.0);
        resetStatus(gui.heartStatus);
        gui.lungsBox.setSelected(false);
        gui.lungsOxygenSpinner.setValue(10);
        gui.lungsOxygenUseSpinner.setValue(0.5);
        resetStatus(gui.lungsStatus);
        gui.liverBox.setSelected(false);
        gui.liverDetoxificationSpinner.setValue(1);
        gui.liverOxygenUseSpinner.setValue(1.0);
        resetStatus(gui.liverStatus);
        gui.brainBox.setSelected(false);
        gui.brainOxygenUseSpinner.setValue(3.0);
        resetStatus(gui.brainStatus);
        gui.stomachBox.setSelected(false);
        gui.stomachCapacitySpinner.setValue(50.0);
        gui.stomachAbsorptionSpinner.setValue(1.0);
        gui.stomachOxygenUseSpinner.setValue(0.5);
        resetStatus(gui.stomachStatus);
        gui.stomachChemicalsField.setText("");
        gui.cardiovascularBox.setSelected(false);
        gui.cardiovascularOxygenSpinner.setValue(0.0);
        gui.cardiovascularMaxOxygenSpinner.setValue(100.0);
        gui.cardiovascularPowerSpinner.setValue(0.0);
        gui.cardiovascularMaxPowerSpinner.setValue(0.0);
        gui.cardiovascularFluidCapacitySpinner.setValue(150.0);
        gui.cardiovascularChemicalsField.setText("");
        gui.heartOrganAssetField.setText("");
        gui.lungsOrganAssetField.setText("");
        gui.liverOrganAssetField.setText("");
        gui.brainOrganAssetField.setText("");
        gui.stomachOrganAssetField.setText("");

        gui.loadedEntityLabel.setText(" ");
        gui.selectedSlotField.setText("");
    }

    /**
     * Reads an entity into the editor. Keep this in the same section order as
     * buildEntityFromForm so a newly added field has an obvious home.
     */
    private void populateForm(Entity entity) {
        // ===== Identity and display =====
        gui.nameField.setText(entity.getName());
        gui.idField.setText(String.valueOf(entity.getId()));
        gui.mapField.setText(String.valueOf(entity.getMap()));
        gui.selectedSlotField.setText(entity.getSelectedSlot());
        gui.directionCombo.setSelectedItem(entity.getDirection());
        gui.tagsField.setText(String.join(", ", entity.getTagsList()));
        if (entity.hasDisplayTexture()) {
            gui.displayTextureField.setText(entity.getDisplayTexture());
        } else {
            gui.displayTextureField.setText("");
        }
        if (entity.hasHexColor()) {
        gui.hexColorField.setText(entity.getHexColor());
        } else {
            gui.hexColorField.setText("");
        }
        gui.renderPrioritySpinner.setValue(entity.getRenderPriority());

        // ===== Movement =====
        gui.posXField.setText(fmt(entity.getPosition().getX()));
        gui.posYField.setText(fmt(entity.getPosition().getY()));
        gui.velXField.setText(fmt(entity.getVelocity().getX()));
        gui.velYField.setText(fmt(entity.getVelocity().getY()));
        gui.sizeXField.setText(fmt(entity.getSize().getX()));
        gui.sizeYField.setText(fmt(entity.getSize().getY()));
        gui.speedField.setText(fmt(entity.getSpeed()));
        gui.maxSpeedField.setText(fmt(entity.getMaxSpeed()));
        gui.reachField.setText(fmt(entity.getReach()));
        gui.anchoredBox.setSelected(entity.getAnchored());
        gui.canCollideBox.setSelected(entity.getCanCollide());
        gui.castShadowBox.setSelected(entity.getCastShadow());
        gui.aliveBox.setSelected(entity.getAlive());

        // ===== Combat =====
        Damage dmg = entity.getDamage();
        DamageMultiplier mult = dmg.getDamageMultiplier();
        float[] dmgArr = {
                dmg.getBruteDamage(), dmg.getAsphyxiationDamage(),
                dmg.getBurnDamage(), dmg.getToxinDamage(),
                dmg.getGeneticDamage(), dmg.getStructuralDamage(),
                dmg.getBleedingPerTick()
        };
        float[] multArr = {
                mult.getBrute(), mult.getAsphyxiation(), mult.getBurn(),
                mult.getToxin(), mult.getGenetic(), mult.getStructural(),
                mult.getBleeding()
        };
        HitDamage hit = entity.getHitDamage();
        float[] hitArr = {
                hit.getBruteDamage(), hit.getAsphyxiationDamage(),
                hit.getBurnDamage(), hit.getToxinDamage(),
                hit.getGeneticDamage(), hit.getStructuralDamage(),
                hit.getBleedingPerTick()
        };
        for (int i = 0; i < dmgArr.length; i++) {
            gui.dmgValues[i].setValue(dmgArr[i]);
            gui.dmgMultValues[i].setValue(multArr[i]);
            gui.hitDmgValues[i].setValue(hitArr[i]);
        }
        gui.hitCooldownSpinner.setValue(entity.getHitDamage().hasHitCooldown()
            ? entity.getHitDamage().getHitCooldown()
            : 0);
        gui.maxHealthSpinner.setValue(entity.getMaxHealth());
        gui.critHealthSpinner.setValue(entity.getCritHealth());
        if (entity.hasLightRange()) {
            gui.lightRangeField.setText(fmt(entity.getLightRange()));
        } else {
            gui.lightRangeField.setText("");
        }

        // ===== Item and inventory =====
        gui.isItemBox.setSelected(entity.getIsItem());
        gui.stackableBox.setSelected(entity.getStackable());
        gui.canDestroyBox.setSelected(entity.getCanDestroy());
        gui.amountSpinner.setValue(entity.getAmount());

        // ===== Loot table =====
        // Populate loot table
        gui.lootTableModel.setRowCount(0);
        for (lootTableItem item : entity.getLootTableList()) {
            gui.lootTableModel.addRow(new Object[]{
                    item.getItemName(),
                    item.getProbability(),
                    item.hasAmount() ? item.getAmount() : 1
            });
        }

        // ===== Advanced Entity.proto fields =====
        gui.dropsABodyBox.setSelected(entity.getDropsABody());
        gui.internalSpaceSpinner.setValue(entity.getInternalSpace());
        gui.internalValuesField.setText(formatMap(entity.getInternalMap()));
        Organs organs = entity.getOrgans();
        OrganAssetSlots organAssets = entity.getOrganAssetSlots();
        gui.heartOrganAssetField.setText(organAssets.getHeartAsset());
        gui.lungsOrganAssetField.setText(organAssets.getLungsAsset());
        gui.liverOrganAssetField.setText(organAssets.getLiverAsset());
        gui.brainOrganAssetField.setText(organAssets.getBrainAsset());
        gui.stomachOrganAssetField.setText(organAssets.getStomachAsset());
		if (entity.hasOrganComponent()) {
			OrganComponent component = entity.getOrganComponent();
			if (component.hasHeart()) organs = organs.toBuilder().setHeart(component.getHeart()).build();
			if (component.hasLungs()) organs = organs.toBuilder().setLungs(component.getLungs()).build();
			if (component.hasLiver()) organs = organs.toBuilder().setLiver(component.getLiver()).build();
			if (component.hasBrain()) organs = organs.toBuilder().setBrain(component.getBrain()).build();
			if (component.hasStomach()) organs = organs.toBuilder().setStomach(component.getStomach()).build();
		}
        gui.heartBox.setSelected(organs.hasHeart());
        if (organs.hasHeart()) {
            gui.heartBloodSpinner.setValue((double) organs.getHeart().getBlood());
            gui.heartMaxBloodSpinner.setValue((double) organs.getHeart().getMaxBlood());
            gui.heartCirculationSpinner.setValue((double) (organs.getHeart().hasCirculationPerSecond()
                    ? organs.getHeart().getCirculationPerSecond() : organs.getHeart().getMaxBlood()));
            gui.heartOxygenUseSpinner.setValue((double) (organs.getHeart().hasOxygenUsePerSecond()
                    ? organs.getHeart().getOxygenUsePerSecond() : 1.0f));
            populateStatus(gui.heartStatus, organs.getHeart().getStatus());
        } else {
            gui.heartBloodSpinner.setValue(100.0);
            gui.heartMaxBloodSpinner.setValue(100.0);
            gui.heartCirculationSpinner.setValue(10.0);
            gui.heartOxygenUseSpinner.setValue(1.0);
            resetStatus(gui.heartStatus);
        }
        gui.lungsBox.setSelected(organs.hasLungs());
        if (organs.hasLungs()) {
            gui.lungsOxygenSpinner.setValue(organs.getLungs().getOxygen());
            gui.lungsOxygenUseSpinner.setValue((double) (organs.getLungs().hasOxygenUsePerSecond()
                    ? organs.getLungs().getOxygenUsePerSecond() : 0.5f));
            populateStatus(gui.lungsStatus, organs.getLungs().getStatus());
        } else {
            gui.lungsOxygenSpinner.setValue(10);
            gui.lungsOxygenUseSpinner.setValue(0.5);
            resetStatus(gui.lungsStatus);
        }
        gui.liverBox.setSelected(organs.hasLiver());
        if (organs.hasLiver()) {
            gui.liverDetoxificationSpinner.setValue(organs.getLiver().getDetoxification());
            gui.liverOxygenUseSpinner.setValue((double) (organs.getLiver().hasOxygenUsePerSecond()
                    ? organs.getLiver().getOxygenUsePerSecond() : 1.0f));
            populateStatus(gui.liverStatus, organs.getLiver().getStatus());
        } else {
            gui.liverDetoxificationSpinner.setValue(1);
            gui.liverOxygenUseSpinner.setValue(1.0);
            resetStatus(gui.liverStatus);
        }
        gui.brainBox.setSelected(organs.hasBrain());
        if (organs.hasBrain()) {
            gui.brainOxygenUseSpinner.setValue((double) (organs.getBrain().hasOxygenUsePerSecond()
                    ? organs.getBrain().getOxygenUsePerSecond() : 3.0f));
            populateStatus(gui.brainStatus, organs.getBrain().getStatus());
        } else {
            gui.brainOxygenUseSpinner.setValue(3.0);
            resetStatus(gui.brainStatus);
        }
        gui.stomachBox.setSelected(organs.hasStomach());
        if (organs.hasStomach()) {
            Stomach stomach = organs.getStomach();
            gui.stomachCapacitySpinner.setValue((double) (stomach.hasChemicalCapacity()
                    ? stomach.getChemicalCapacity() : 50.0f));
            gui.stomachAbsorptionSpinner.setValue((double) (stomach.hasAbsorptionPerSecond()
                    ? stomach.getAbsorptionPerSecond() : 1.0f));
            gui.stomachOxygenUseSpinner.setValue((double) (stomach.hasOxygenUsePerSecond()
                    ? stomach.getOxygenUsePerSecond() : 0.5f));
            populateStatus(gui.stomachStatus, stomach.getStatus());
            gui.stomachChemicalsField.setText(formatStomachChemicals(stomach));
        } else {
            gui.stomachCapacitySpinner.setValue(50.0);
            gui.stomachAbsorptionSpinner.setValue(1.0);
            gui.stomachOxygenUseSpinner.setValue(0.5);
            resetStatus(gui.stomachStatus);
            gui.stomachChemicalsField.setText("");
        }
        gui.cardiovascularBox.setSelected(organs.hasCardiovascularSystem());
        if (organs.hasCardiovascularSystem()) {
            CardiovascularSystem cardiovascular = organs.getCardiovascularSystem();
            float maximumBlood = organs.hasHeart() ? organs.getHeart().getMaxBlood() : 0;
            gui.cardiovascularOxygenSpinner.setValue((double) cardiovascular.getOxygen());
            gui.cardiovascularMaxOxygenSpinner.setValue((double) (cardiovascular.hasMaxOxygen()
                    ? cardiovascular.getMaxOxygen() : maximumBlood));
            gui.cardiovascularPowerSpinner.setValue((double) cardiovascular.getElectricalPower());
            gui.cardiovascularMaxPowerSpinner.setValue((double) (cardiovascular.hasMaxElectricalPower()
                    ? cardiovascular.getMaxElectricalPower() : cardiovascular.getElectricalPower()));
            gui.cardiovascularFluidCapacitySpinner.setValue((double) (cardiovascular.hasFluidCapacity()
                    ? cardiovascular.getFluidCapacity() : maximumBlood + 50.0f));
            gui.cardiovascularChemicalsField.setText(
                    formatChemicalMessages(cardiovascular.getChemicalsList()));
        } else {
            float maximumBlood = organs.hasHeart() ? organs.getHeart().getMaxBlood() : 100.0f;
            gui.cardiovascularOxygenSpinner.setValue(0.0);
            gui.cardiovascularMaxOxygenSpinner.setValue((double) maximumBlood);
            gui.cardiovascularPowerSpinner.setValue(0.0);
            gui.cardiovascularMaxPowerSpinner.setValue(0.0);
            gui.cardiovascularFluidCapacitySpinner.setValue((double) maximumBlood + 50.0);
            gui.cardiovascularChemicalsField.setText("");
        }

        Map<String, Integer> slots = entity.getInventorySlotsMap();
        StringBuilder inv = new StringBuilder();
        for (Map.Entry<String, Integer> e : slots.entrySet()) {

            inv.append(e.getKey()).append(" = #").append(e.getValue()).append('\n');
        }
        gui.inventorySlotsField.setText(inv.toString());
    }

    private void updateLoadedEntityLabel(Entity entity) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:monospace'>");
        sb.append("name:      ").append(entity.getName()).append("<br>");
        sb.append("id:        ").append(entity.getId()).append("<br>");
        sb.append("map:       ").append(entity.getMap()).append("<br>");
        sb.append("direction: ").append(entity.getDirection()).append("<br>");
        sb.append("position:  (").append(fmt(entity.getPosition().getX()))
                .append(", ").append(fmt(entity.getPosition().getY())).append(")<br>");
        sb.append("size:      (").append(fmt(entity.getSize().getX()))
                .append(", ").append(fmt(entity.getSize().getY())).append(")<br>");
        sb.append("speed:     ").append(fmt(entity.getSpeed()))
                .append(" / max ").append(fmt(entity.getMaxSpeed())).append("<br>");
        sb.append("maxHP:     ").append(entity.getMaxHealth())
                .append("  critHP: ").append(entity.getCritHealth()).append("<br>");
        sb.append("alive:     ").append(entity.getAlive())
                .append("  anchored: ").append(entity.getAnchored())
                .append("  collide: ").append(entity.getCanCollide())
                .append("  shadow: ").append(entity.getCastShadow()).append("<br>");
        sb.append("tags:      ").append(String.join(", ", entity.getTagsList())).append("<br>");
        sb.append("slots:     ").append(entity.getInventorySlotsMap().size()).append("<br>");
        sb.append("loot:      ").append(entity.getLootTableCount()).append(" entries").append("<br>");
        sb.append("body:      ").append(entity.getDropsABody())
                .append("  organs: ").append(entity.getOrgans().getAllFields().size()).append("<br>");
        sb.append("internal:  ").append(entity.getInternalMap().size())
                .append(" values / ").append(entity.getInternalSpace()).append(" space<br>");
        sb.append("</body></html>");
        gui.loadedEntityLabel.setText(sb.toString());
    }

    
    
    /**
     * Converts the visible form into the protobuf saved on disk.
     *
     * <p><strong>Adding a new entity field:</strong> add its Swing control in
     * {@link AssetMakerGUI}, place it in the matching tab in
     * {@link AssetMakerGUIPanels}, then update the matching method below and
     * {@link #populateForm(Entity)}. Keeping read and write logic together by
     * section makes new fields much harder to forget.</p>
     */
    private Entity buildEntityFromForm() {
        Entity existing = gui.currentEntity != null ? gui.currentEntity : Entity.getDefaultInstance();
        Entity.Builder builder = Entity.newBuilder(existing);

        // 1. Identity and movement fields are the fields every entity uses.
        applyIdentityAndMovement(builder);

        // 2. Combat fields: health, light, damage-over-time, and contact damage.
        builder.setMaxHealth((Integer) gui.maxHealthSpinner.getValue())
                .setCritHealth((Integer) gui.critHealthSpinner.getValue())
                .setDamage(buildDamage())
                .setHitDamage(buildHitDamage());
        applyOptionalLightRange(builder);

        // 3. Item and inventory fields. Inventory lines use: slotName=itemId.
        builder.setIsItem(gui.isItemBox.isSelected())
                .setStackable(gui.stackableBox.isSelected())
                .setCanDestroy(gui.canDestroyBox.isSelected())
                .setAmount((Integer) gui.amountSpinner.getValue())
                .putAllInventorySlots(parseInventorySlots());

        // 4. Tags and display fields.
        builder.clearTags().addAllTags(parseTags());
        builder.setRenderPriority((Integer) gui.renderPrioritySpinner.getValue());
        setOptionalText(builder, gui.displayTextureField.getText(), true);
        setOptionalText(builder, gui.hexColorField.getText(), false);

        // 5. Advanced Entity.proto fields.
        builder.setDropsABody(gui.dropsABodyBox.isSelected())
                .setInternalSpace((Integer) gui.internalSpaceSpinner.getValue())
                .putAllInternal(parseInternalValues())
				.setOrganAssetSlots(buildOrganAssetSlots());
		Organs builtOrgans = buildOrgans();
		builder.setOrgans(builtOrgans.toBuilder()
				.clearHeart().clearLungs().clearLiver().clearBrain().clearStomach());
		OrganComponent component = buildOrganComponent(builtOrgans);
		if (component == null) builder.clearOrganComponent();
		else builder.setOrganComponent(component);

        // 6. Loot table rows are converted from the table model at save time.
        builder.clearLootTable().addAllLootTable(buildLootTable());

        gui.dirty = true;
        return builder.build();
    }

    /** Formats the Entity.proto map as one name=id entry per line. */
    private static String formatMap(Map<String, Integer> values) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            result.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return result.toString();
    }

    /** Converts legacy one-unit stomach entries and amount-aware entries to id=amountU. */
    private static String formatStomachChemicals(Stomach stomach) {
        Map<Integer, Float> combined = new LinkedHashMap<>();
        for (Integer id : stomach.getChemicalsList()) combined.merge(id, 1.0f, Float::sum);
        for (Chemical chemical : stomach.getContentsList()) {
            combined.merge(chemical.getId(), chemical.getAmount(), Float::sum);
        }
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, Float> entry : combined.entrySet()) {
            result.append(entry.getKey()).append('=').append(fmt(entry.getValue())).append('\n');
        }
        return result.toString();
    }

    /** Formats full Chemical messages as id=amount entries. */
    private static String formatChemicalMessages(List<Chemical> chemicals) {
        StringBuilder result = new StringBuilder();
        for (Chemical chemical : chemicals) {
            result.append(chemical.getId()).append('=').append(chemical.getAmount()).append('\n');
        }
        return result.toString();
    }

    /** Parses the advanced internal map as name=id entries. */
    private Map<String, Integer> parseInternalValues() {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String line : gui.internalValuesField.getText().split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int equals = line.indexOf('=');
            if (equals < 0) continue;
            String name = line.substring(0, equals).trim();
            if (name.isEmpty()) continue;
            try {
                values.put(name, Integer.parseInt(line.substring(equals + 1).trim()));
            } catch (NumberFormatException ex) {
                throw new NumberFormatException("Internal value '" + name + "': " + ex.getMessage());
            }
        }
        return values;
    }

    /** Builds the nested Organs message. Blank/unchecked organs are omitted. */
    private Organs buildOrgans() {
        Organs.Builder organs = Organs.newBuilder();
        if (gui.heartBox.isSelected()) {
            organs.setHeart(protonova.protobuf.OrgansProto.Heart.newBuilder()
                    .setBlood(spinFloat(gui.heartBloodSpinner))
                    .setMaxBlood(spinFloat(gui.heartMaxBloodSpinner))
                    .setCirculationPerSecond(spinFloat(gui.heartCirculationSpinner))
                    .setOxygenUsePerSecond(spinFloat(gui.heartOxygenUseSpinner))
                    .setStatus(buildStatus(gui.heartStatus)));
        }
        if (gui.lungsBox.isSelected()) {
            organs.setLungs(protonova.protobuf.OrgansProto.Lungs.newBuilder()
                    .setOxygen((Integer) gui.lungsOxygenSpinner.getValue())
                    .setOxygenUsePerSecond(spinFloat(gui.lungsOxygenUseSpinner))
                    .setStatus(buildStatus(gui.lungsStatus)));
        }
        if (gui.liverBox.isSelected()) {
            organs.setLiver(protonova.protobuf.OrgansProto.Liver.newBuilder()
                    .setDetoxification((Integer) gui.liverDetoxificationSpinner.getValue())
                    .setOxygenUsePerSecond(spinFloat(gui.liverOxygenUseSpinner))
                    .setStatus(buildStatus(gui.liverStatus)));
        }
        if (gui.brainBox.isSelected()) {
            organs.setBrain(protonova.protobuf.OrgansProto.Brain.newBuilder()
                    .setOxygenUsePerSecond(spinFloat(gui.brainOxygenUseSpinner))
                    .setStatus(buildStatus(gui.brainStatus)));
        }
        if (gui.stomachBox.isSelected()) {
            organs.setStomach(Stomach.newBuilder()
                    .setChemicalCapacity(spinFloat(gui.stomachCapacitySpinner))
                    .setAbsorptionPerSecond(spinFloat(gui.stomachAbsorptionSpinner))
                    .setOxygenUsePerSecond(spinFloat(gui.stomachOxygenUseSpinner))
                    .setStatus(buildStatus(gui.stomachStatus))
                    .addAllContents(parseChemicals(
                            gui.stomachChemicalsField.getText(), "Stomach content")));
        }
        if (gui.cardiovascularBox.isSelected()) {
            organs.setCardiovascularSystem(CardiovascularSystem.newBuilder()
                    .setOxygen(spinFloat(gui.cardiovascularOxygenSpinner))
                    .setMaxOxygen(spinFloat(gui.cardiovascularMaxOxygenSpinner))
                    .setElectricalPower(spinFloat(gui.cardiovascularPowerSpinner))
                    .setMaxElectricalPower(spinFloat(gui.cardiovascularMaxPowerSpinner))
                    .setFluidCapacity(spinFloat(gui.cardiovascularFluidCapacitySpinner))
                    .addAllChemicals(parseChemicals(
                            gui.cardiovascularChemicalsField.getText(), "Bloodstream chemical")));
        }
        return organs.build();
    }

	private OrganAssetSlots buildOrganAssetSlots() {
		return OrganAssetSlots.newBuilder()
				.setHeartAsset(gui.heartOrganAssetField.getText().trim())
				.setLungsAsset(gui.lungsOrganAssetField.getText().trim())
				.setLiverAsset(gui.liverOrganAssetField.getText().trim())
				.setBrainAsset(gui.brainOrganAssetField.getText().trim())
				.setStomachAsset(gui.stomachOrganAssetField.getText().trim())
				.build();
	}

	private OrganComponent buildOrganComponent(Organs organs) {
		if (!gui.isItemBox.isSelected()) return null;
		int selected = (gui.heartBox.isSelected() ? 1 : 0)
				+ (gui.lungsBox.isSelected() ? 1 : 0)
				+ (gui.liverBox.isSelected() ? 1 : 0)
				+ (gui.brainBox.isSelected() ? 1 : 0)
				+ (gui.stomachBox.isSelected() ? 1 : 0);
		if (selected != 1) return null;

		OrganComponent.Builder component = OrganComponent.newBuilder();
		if (organs.hasHeart()) component.setHeart(organs.getHeart());
		if (organs.hasLungs()) component.setLungs(organs.getLungs());
		if (organs.hasLiver()) component.setLiver(organs.getLiver());
		if (organs.hasBrain()) component.setBrain(organs.getBrain());
		if (organs.hasStomach()) component.setStomach(organs.getStomach());
		return component.build();
	}

    private List<Chemical> parseChemicals(String text, String label) {
        List<Chemical> values = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int equals = line.indexOf('=');
            if (equals < 0) throw new NumberFormatException(label + " must use chemicalId=amountU");
            try {
                float amount = Float.parseFloat(line.substring(equals + 1).trim());
                if (!Float.isFinite(amount) || amount < 0) {
                    throw new NumberFormatException("amount must be zero or greater");
                }
                values.add(Chemical.newBuilder()
                        .setId(Integer.parseInt(line.substring(0, equals).trim()))
                        .setAmount(amount).build());
            } catch (NumberFormatException ex) {
                throw new NumberFormatException(label + ": " + ex.getMessage());
            }
        }
        return values;
    }

    private static OrganStatus buildStatus(AssetMakerGUI.OrganStatusControls controls) {
        OrganType type = "Cybernetic".equals(controls.type.getSelectedItem())
                ? OrganType.ORGAN_TYPE_CYBERNETIC
                : OrganType.ORGAN_TYPE_BIOLOGICAL;
        return OrganStatus.newBuilder()
                .setType(type)
                .setHealth(spinFloat(controls.healthPercent) / 100.0f)
                .setEfficiency(spinFloat(controls.efficiencyPercent) / 100.0f)
                .setPowerUsePerSecond(spinFloat(controls.powerUse))
                .build();
    }

    private static void populateStatus(AssetMakerGUI.OrganStatusControls controls, OrganStatus status) {
        controls.type.setSelectedItem(status.getType() == OrganType.ORGAN_TYPE_CYBERNETIC
                ? "Cybernetic" : "Biological");
        double health = (status.hasHealth() ? status.getHealth() : 1.0f) * 100.0;
        double efficiency = (status.hasEfficiency() ? status.getEfficiency() : 1.0f) * 100.0;
        controls.healthPercent.setValue(Math.max(0.0, Math.min(100.0, health)));
        controls.efficiencyPercent.setValue(Math.max(0.0, Math.min(200.0, efficiency)));
        controls.powerUse.setValue((double) status.getPowerUsePerSecond());
    }

    private static void resetStatus(AssetMakerGUI.OrganStatusControls controls) {
        controls.type.setSelectedItem("Biological");
        controls.healthPercent.setValue(100.0);
        controls.efficiencyPercent.setValue(100.0);
        controls.powerUse.setValue(0.0);
    }

    /** Writes the identity and movement tab into the protobuf builder. */
    private void applyIdentityAndMovement(Entity.Builder builder) {
        Vector pos = vectorFromFields(gui.posXField, gui.posYField, "Position X", "Position Y");
        Vector vel = vectorFromFields(gui.velXField, gui.velYField, "Velocity X", "Velocity Y");
        Vector size = vectorFromFields(gui.sizeXField, gui.sizeYField, "Size X", "Size Y");
        builder.setName(gui.nameField.getText().trim())
                .setId(parseInt(gui.idField.getText(), "Entity ID"))
                .setMap(parseInt(gui.mapField.getText(), "Map"))
                .setDirection((Direction) gui.directionCombo.getSelectedItem())
                .setSelectedSlot(gui.selectedSlotField.getText())
                .setPosition(pos).setVelocity(vel).setSize(size)
                .setSpeed(parseDouble(gui.speedField.getText(), "Speed"))
                .setMaxSpeed(parseDouble(gui.maxSpeedField.getText(), "Max Speed"))
                .setReach(parseDouble(gui.reachField.getText(), "Reach"))
                .setAnchored(gui.anchoredBox.isSelected())
                .setCanCollide(gui.canCollideBox.isSelected())
                .setCastShadow(gui.castShadowBox.isSelected())
                .setAlive(gui.aliveBox.isSelected());
    }

    /** Builds a vector from a pair of text fields and gives both fields useful error names. */
    private Vector vectorFromFields(javax.swing.JTextField xField, javax.swing.JTextField yField,
                                    String xName, String yName) {
        return Vector.newBuilder().setX(parseFloat(xField.getText(), xName))
                .setY(parseFloat(yField.getText(), yName)).build();
    }

    /** Builds continuous damage and its seven damage-type multipliers. */
    private Damage buildDamage() {
        return Damage.newBuilder()
                .setBruteDamage(spinFloat(gui.dmgValues[0]))
                .setAsphyxiationDamage(spinFloat(gui.dmgValues[1]))
                .setBurnDamage(spinFloat(gui.dmgValues[2]))
                .setToxinDamage(spinFloat(gui.dmgValues[3]))
                .setGeneticDamage(spinFloat(gui.dmgValues[4]))
                .setStructuralDamage(spinFloat(gui.dmgValues[5]))
                .setBleedingPerTick(spinFloat(gui.dmgValues[6]))
                .setDamageMultiplier(DamageMultiplier.newBuilder()
                        .setBrute(spinFloat(gui.dmgMultValues[0]))
                        .setAsphyxiation(spinFloat(gui.dmgMultValues[1]))
                        .setBurn(spinFloat(gui.dmgMultValues[2]))
                        .setToxin(spinFloat(gui.dmgMultValues[3]))
                        .setGenetic(spinFloat(gui.dmgMultValues[4]))
                        .setStructural(spinFloat(gui.dmgMultValues[5]))
                        .setBleeding(spinFloat(gui.dmgMultValues[6])).build()).build();
    }

    /** Builds damage applied when another entity makes contact with this entity. */
    private HitDamage buildHitDamage() {
        return HitDamage.newBuilder()
                .setBruteDamage(spinFloat(gui.hitDmgValues[0]))
                .setAsphyxiationDamage(spinFloat(gui.hitDmgValues[1]))
                .setBurnDamage(spinFloat(gui.hitDmgValues[2]))
                .setToxinDamage(spinFloat(gui.hitDmgValues[3]))
                .setGeneticDamage(spinFloat(gui.hitDmgValues[4]))
                .setStructuralDamage(spinFloat(gui.hitDmgValues[5]))
                .setBleedingPerTick(spinFloat(gui.hitDmgValues[6]))
				.setHitCooldown((Integer) gui.hitCooldownSpinner.getValue())
				.setCanAttack(true)
				.build();
    }

    /** Light range is optional in the protobuf, so blank means clear the field. */
    private void applyOptionalLightRange(Entity.Builder builder) {
        String value = gui.lightRangeField.getText().trim();
        if (value.isEmpty()) {
            builder.clearLightRange();
            return;
        }
        try {
            builder.setLightRange(Float.parseFloat(value));
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Light Range: " + ex.getMessage());
        }
    }

    /** Parses the inventory text area. Invalid lines are ignored; invalid IDs are reported. */
    private Map<String, Integer> parseInventorySlots() {
        Map<String, Integer> slots = new LinkedHashMap<>();
        for (String line : gui.inventorySlotsField.getText().split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int equals = line.indexOf('=');
            if (equals < 0) continue;
            String slot = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if (slot.isEmpty()) continue;
            try {
                slots.put(slot, Integer.parseInt(value.startsWith("#") ? value.substring(1) : value));
            } catch (NumberFormatException ex) {
                throw new NumberFormatException("Inventory slot '" + slot + "': " + ex.getMessage());
            }
        }
        return slots;
    }

    /** Converts the comma-separated Tags field into the repeated protobuf field. */
    private List<String> parseTags() {
        List<String> tags = new ArrayList<>();
        for (String tag : gui.tagsField.getText().split(",")) {
            if (!tag.trim().isEmpty()) tags.add(tag.trim());
        }
        return tags;
    }

    /** Builds loot entries from the editable table. Add new loot columns here when the schema changes. */
    private List<lootTableItem> buildLootTable() {
        List<lootTableItem> items = new ArrayList<>();
        for (int row = 0; row < gui.lootTableModel.getRowCount(); row++) {
            Object nameValue = gui.lootTableModel.getValueAt(row, 0);
            if (nameValue == null || nameValue.toString().trim().isEmpty()) continue;
            Object probabilityValue = gui.lootTableModel.getValueAt(row, 1);
            Object amountValue = gui.lootTableModel.getValueAt(row, 2);
            double probability = probabilityValue instanceof Number
                    ? ((Number) probabilityValue).doubleValue() : 100.0;
            int amount = amountValue instanceof Number ? ((Number) amountValue).intValue() : 1;
            lootTableItem.Builder item = lootTableItem.newBuilder()
                    .setItemName(nameValue.toString().trim()).setProbability(probability);
            if (amount > 1) item.setAmount(amount);
            items.add(item.build());
        }
        return items;
    }

    /** Applies one of the two optional string fields without duplicating blank-check logic. */
    private void setOptionalText(Entity.Builder builder, String text, boolean texture) {
        String value = text.trim();
        if (texture) {
            if (value.isEmpty()) builder.clearDisplayTexture(); else builder.setDisplayTexture(value);
        } else {
            if (value.isEmpty()) builder.clearHexColor(); else builder.setHexColor(value);
        }
    }

    
    
    private void setStatus(String msg) {
        gui.statusLabel.setText(" " + msg);
    }

    private static String fmt(float f) {
        if (f == (long) f) return Long.toString((long) f);
        return Float.toString(f);
    }

    private static String fmt(double d) {
        if (d == (long) d) return Long.toString((long) d);
        return Double.toString(d);
    }

    private static float spinFloat(javax.swing.JSpinner s) {
        Object v = s.getValue();
        if (v instanceof Number) return ((Number) v).floatValue();
        return Float.parseFloat(v.toString());
    }

    private static float parseFloat(String s, String field) {
        if (s == null || s.trim().isEmpty()) return 0f;
        try { return Float.parseFloat(s.trim()); }
        catch (NumberFormatException nfe) {
            throw new NumberFormatException(field + " - " + nfe.getMessage());
        }
    }

    private static double parseDouble(String s, String field) {
        if (s == null || s.trim().isEmpty()) return 0d;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException nfe) {
            throw new NumberFormatException(field + " - " + nfe.getMessage());
        }
    }

    private static int parseInt(String s, String field) {
        if (s == null || s.trim().isEmpty()) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException nfe) {
            throw new NumberFormatException(field + " - " + nfe.getMessage());
        }
    }
}
