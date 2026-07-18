package assetmaker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.DamageMultiplier;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;

/**
 * Reads and writes entity assets used by the server asset maker.
 *
 * <p>Adding a new asset format or asset folder should start here. Keep file
 * system code in this class and keep Swing code in the GUI/controller classes.
 * The GUI only needs to call {@link #loadAsset(String)}, {@link #saveEntity(String, Entity)},
 * and the list/delete methods below.</p>
 */
public class AssetMaker {

    /** Default folder for editable entity assets. Paths are relative to the server. */
    public static final String ASSET_DIR = "assets/entities/";
    /** File suffix used by protobuf entity assets. */
    public static final String ASSET_EXTENSION = ".data";

    public static void main(String[] args) {
        AssetMaker assetMaker = new AssetMaker();
        Entity entity = assetMaker.createAsset();
        if (entity != null) {
            assetMaker.saveAsset(entity.getName());
        }
    }

    public Entity createAsset() {
        return createAsset("new entity");
    }

    /**
     * Creates a new entity with safe defaults for the editor.
     *
     * <p>When a new protobuf field is added, give it an editor-friendly default
     * here and add the matching control in {@link AssetMakerGUIPanels} and
     * serialization code in {@link AssetMakerGUIController}.</p>
     */
    public Entity createAsset(String name) {
        DamageMultiplier damageMult = DamageMultiplier.newBuilder()
                .setBrute(1)
                .setAsphyxiation(1)
                .setBurn(1)
                .setToxin(1)
                .setGenetic(1)
                .setStructural(1)
                .setBleeding(1)
                .build();
        HitDamage hitDamageStruc = HitDamage.newBuilder()
                .setBruteDamage(0)
                .setAsphyxiationDamage(0)
                .setBurnDamage(0)
                .setToxinDamage(0)
                .setGeneticDamage(0)
                .setStructuralDamage(10)
                .setBleedingPerTick(0)
                .setHitCooldown(0)
                .build();
        Damage damage = Damage.newBuilder()
                .setBruteDamage(0)
                .setAsphyxiationDamage(0)
                .setBurnDamage(0)
                .setToxinDamage(0)
                .setGeneticDamage(0)
                .setStructuralDamage(0)
                .setBleedingPerTick(0)
                .setDamageMultiplier(damageMult)
                .build();
        Vector zero = Vector.newBuilder()
                .setX(0)
                .setY(0)
                .build();
        Vector one = Vector.newBuilder()
                .setX(1)
                .setY(1)
                .build();

        return Entity.newBuilder()
                .setDirection(Direction.Down)
                .setName(name)
                .setPosition(zero)
                .setSize(one)
                .setVelocity(zero)
                .setCanCollide(true)
                .setAnchored(true)
                .setCastShadow(false)
                .setAlive(true)
                .setSpeed(0)
                .setMaxSpeed(0)
                .setMaxHealth(100)
                .setCritHealth(50)
                .setReach(1.5)
                .setSelectedSlot("leftHand")
                .setDamage(damage)
                .setHitDamage(hitDamageStruc)
                .build();
    }

    public Entity loadAsset(String assetName) {
        return loadAsset(assetName, ASSET_DIR);
    }

    public Entity loadAsset(String assetName, String directory) {
        Path path = assetPath(directory, assetName);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return Entity.parseFrom(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean saveAsset(String assetName) {
        return saveAsset(assetName, ASSET_DIR);
    }

    public boolean saveAsset(String assetName, String directory) {
        try {
            File dir = new File(directory);
            if (!dir.exists() && !dir.mkdirs()) {
                return false;
            }
            Entity entity = loadAsset(assetName, directory);
            if (entity == null) {
                entity = createAsset(assetName);
            }
            Path path = assetPath(directory, assetName);
            Files.write(path, entity.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveEntity(String assetName, Entity entity) {
        return saveEntity(assetName, entity, ASSET_DIR);
    }

    public boolean saveEntity(String assetName, Entity entity, String directory) {
        if (assetName == null || assetName.trim().isEmpty() || entity == null) {
            return false;
        }
        
        if (!entity.getName().equals(assetName)) {
            entity = entity.toBuilder().setName(assetName).build();
        }
        try {
            File dir = new File(directory);
            if (!dir.exists() && !dir.mkdirs()) {
                return false;
            }
            Path path = assetPath(directory, assetName);
            Files.write(path, entity.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteAsset(String assetName) {
        return deleteAsset(assetName, ASSET_DIR);
    }

    public boolean deleteAsset(String assetName, String directory) {
        if (assetName == null || assetName.trim().isEmpty()) {
            return false;
        }
        Path path = assetPath(directory, assetName);
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> listAssetNames() {
        return listAssetNames(ASSET_DIR);
    }

    public List<String> listAssetNames(String directory) {
        List<String> names = new ArrayList<>();
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return names;
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(ASSET_EXTENSION));
        if (files == null) {
            return names;
        }
        for (File f : files) {
            String n = f.getName();
            names.add(n.substring(0, n.length() - ASSET_EXTENSION.length()));
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public boolean renameAsset(String oldName, String newName) {
        return renameAsset(oldName, newName, ASSET_DIR);
    }

    public boolean renameAsset(String oldName, String newName, String directory) {
        if (oldName == null || newName == null) {
            return false;
        }
        if (oldName.equals(newName)) {
            return true;
        }
        Path from = assetPath(directory, oldName);
        Path to = assetPath(directory, newName);
        if (!Files.exists(from) || Files.exists(to)) {
            return false;
        }
        try {
            Files.move(from, to);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Builds an asset path in one place so all file operations use the same suffix. */
    private static Path assetPath(String directory, String assetName) {
        return Paths.get(directory, assetName + ASSET_EXTENSION);
    }
}
