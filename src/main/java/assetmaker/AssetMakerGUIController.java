package assetmaker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.DamageMultiplier;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.LootTableItemProto.lootTableItem;
import protonova.protobuf.VectorProto.Vector;

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
        String name = JOptionPane.showInputDialog(gui.frame,
                "Name for the new entity asset:",
                "New Asset",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(gui.frame, "Asset name cannot be empty.",
                    "Invalid name", JOptionPane.WARNING_MESSAGE);
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
        setStatus("Created new asset: " + name);
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
                    "Could not parse a numeric field: " + ex.getMessage() + "\n\n" +
                            "Fix the highlighted field and try again.",
                    "Parse error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (saveAs && gui.currentAssetName != null && !targetName.equals(gui.currentAssetName)) {
            
            gui.assetMaker.deleteAsset(gui.currentAssetName);
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
        populateForm(entity);
        updateLoadedEntityLabel(entity);
        setStatus("Loaded " + name);
    }

    private void clearForm() {
        gui.nameField.setText("");
        gui.idField.setText("0");
        gui.mapField.setText("1");
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
        gui.reachField.setText("1.5");
        gui.anchoredBox.setSelected(false);
        gui.canCollideBox.setSelected(false);
        gui.castShadowBox.setSelected(false);
        gui.aliveBox.setSelected(true);

        for (int i = 0; i < AssetMakerGUIPanels.DAMAGE_KEYS.length; i++) {
            gui.dmgValues[i].setValue(0.0f);
            gui.dmgMultValues[i].setValue(1.0f);
            gui.hitDmgValues[i].setValue(0.0f);
        }
        gui.maxHealthSpinner.setValue(100);
        gui.critHealthSpinner.setValue(50);
        gui.lightRangeField.setText("");
        gui.hitCooldownSpinner.setValue(0);

        gui.isItemBox.setSelected(false);
        gui.stackableBox.setSelected(false);
        gui.amountSpinner.setValue(1);
        gui.inventorySlotsField.setText("");

        gui.lootTableModel.setRowCount(0);

        gui.loadedEntityLabel.setText(" ");
    }

    private void populateForm(Entity entity) {
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

        gui.isItemBox.setSelected(entity.getIsItem());
        gui.stackableBox.setSelected(entity.getStackable());
        gui.amountSpinner.setValue(entity.getAmount());

        // Populate loot table
        gui.lootTableModel.setRowCount(0);
        for (lootTableItem item : entity.getLootTableList()) {
            gui.lootTableModel.addRow(new Object[]{
                item.getItemName(),
                item.getProbability(),
                item.hasAmount() ? item.getAmount() : 1
            });
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
        sb.append("</body></html>");
        gui.loadedEntityLabel.setText(sb.toString());
    }

    
    
    private Entity buildEntityFromForm() {
        Entity existing = gui.currentEntity != null ? gui.currentEntity : Entity.getDefaultInstance();

        Vector pos = Vector.newBuilder()
                .setX(parseFloat(gui.posXField.getText(), "Position X"))
                .setY(parseFloat(gui.posYField.getText(), "Position Y"))
                .build();
        Vector vel = Vector.newBuilder()
                .setX(parseFloat(gui.velXField.getText(), "Velocity X"))
                .setY(parseFloat(gui.velYField.getText(), "Velocity Y"))
                .build();
        Vector size = Vector.newBuilder()
                .setX(parseFloat(gui.sizeXField.getText(), "Size X"))
                .setY(parseFloat(gui.sizeYField.getText(), "Size Y"))
                .build();

        Damage dmg = Damage.newBuilder()
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
                        .setBleeding(spinFloat(gui.dmgMultValues[6]))
                        .build())
                .build();

        HitDamage hit = HitDamage.newBuilder()
                .setBruteDamage(spinFloat(gui.hitDmgValues[0]))
                .setAsphyxiationDamage(spinFloat(gui.hitDmgValues[1]))
                .setBurnDamage(spinFloat(gui.hitDmgValues[2]))
                .setToxinDamage(spinFloat(gui.hitDmgValues[3]))
                .setGeneticDamage(spinFloat(gui.hitDmgValues[4]))
                .setStructuralDamage(spinFloat(gui.hitDmgValues[5]))
                .setBleedingPerTick(spinFloat(gui.hitDmgValues[6]))
                .setHitCooldown((Integer) gui.hitCooldownSpinner.getValue())
                .build();

        Entity.Builder b = Entity.newBuilder(existing)
                .setName(gui.nameField.getText().trim())
                .setId(parseInt(gui.idField.getText(), "Entity ID"))
                .setMap(parseInt(gui.mapField.getText(), "Map"))
                .setDirection((Direction) gui.directionCombo.getSelectedItem())
                .setSelectedSlot(gui.selectedSlotField.getText())
                .setPosition(pos)
                .setVelocity(vel)
                .setSize(size)
                .setSpeed(parseDouble(gui.speedField.getText(), "Speed"))
                .setMaxSpeed(parseDouble(gui.maxSpeedField.getText(), "Max Speed"))
                .setReach(parseDouble(gui.reachField.getText(), "Reach"))
                .setAnchored(gui.anchoredBox.isSelected())
                .setCanCollide(gui.canCollideBox.isSelected())
                .setCastShadow(gui.castShadowBox.isSelected())
                .setAlive(gui.aliveBox.isSelected())
                .setMaxHealth((Integer) gui.maxHealthSpinner.getValue())
                .setCritHealth((Integer) gui.critHealthSpinner.getValue())
                .setDamage(dmg)
                .setHitDamage(hit);

        String lightRangeStr = gui.lightRangeField.getText().trim();
        if (!lightRangeStr.isEmpty()) {
            try {
                b.setLightRange(Float.parseFloat(lightRangeStr));
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException("Light Range: " + nfe.getMessage());
            }
        } else {
            b.clearLightRange();
        }
        if (gui.isItemBox.isSelected()) b.setIsItem(true); else b.clearIsItem();
        if (gui.stackableBox.isSelected()) b.setStackable(true); else b.clearStackable();
        b.setAmount((Integer) gui.amountSpinner.getValue());

        Map<String, Integer> existingSlots = new LinkedHashMap<>();
        for (String line : gui.inventorySlotsField.getText().split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String slot = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();
            if (slot.isEmpty()) continue;
            int id;
            try {
                id = Integer.parseInt(val.startsWith("#") ? val.substring(1) : val);
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException("Inventory slot '" + slot + "': " + nfe.getMessage());
            }
            existingSlots.put(slot, id);
        }
        b.putAllInventorySlots(existingSlots);

        List<String> tags = new ArrayList<>();
        for (String tag : gui.tagsField.getText().split(",")) {
            String t = tag.trim();
            if (!t.isEmpty()) tags.add(t);
        }
        b.clearTags();
        b.addAllTags(tags);

        String tex = gui.displayTextureField.getText().trim();
        if (!tex.isEmpty()) b.setDisplayTexture(tex); else b.clearDisplayTexture();
        String hex = gui.hexColorField.getText().trim();
        if (!hex.isEmpty()) b.setHexColor(hex); else b.clearHexColor();

        // Build loot table from table model
        List<lootTableItem> lootItems = new ArrayList<>();
        for (int row = 0; row < gui.lootTableModel.getRowCount(); row++) {
            String itemName = (String) gui.lootTableModel.getValueAt(row, 0);
            if (itemName == null || itemName.trim().isEmpty()) continue;
            double probability = 100.0;
            Object probVal = gui.lootTableModel.getValueAt(row, 1);
            if (probVal instanceof Number) {
                probability = ((Number) probVal).doubleValue();
            }
            int amount = 1;
            Object amtVal = gui.lootTableModel.getValueAt(row, 2);
            if (amtVal instanceof Number) {
                amount = ((Number) amtVal).intValue();
            }
            lootTableItem.Builder itemBuilder = lootTableItem.newBuilder()
                    .setItemName(itemName.trim())
                    .setProbability(probability);
            if (amount > 1) {
                itemBuilder.setAmount(amount);
            }
            lootItems.add(itemBuilder.build());
        }
        b.clearLootTable();
        b.addAllLootTable(lootItems);

        Entity built = b.build();
        gui.dirty = true;
        return built;
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
