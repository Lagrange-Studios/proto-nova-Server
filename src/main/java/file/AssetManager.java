package file;

import java.util.HashMap;
import java.util.HashSet;

import entity.EntityManager;
import main.Console;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.OrganAssetSlots;
import protonova.protobuf.OrgansProto.OrganComponent;
import protonova.protobuf.OrgansProto.OrganSlots;
import protonova.protobuf.VectorProto.Vector;

public class AssetManager {
	private HashMap<String, Entity> entityAssets;
	private EntityManager entityManager;
	private HashMap<String, HashSet<String>> typeMap;
	private Console console;
	
	public AssetManager(EntityManager entityManager, HashMap<String, Entity> entityAssets, Console console, HashMap<String, HashSet<String>> typeMap) {
		this.entityAssets = entityAssets;
		this.entityManager = entityManager;
		this.console = console;
		this.typeMap = typeMap;
		
		// just checking the entities so they don't load with improper values
		for (Entity entity : entityAssets.values()) {
			if (entity.getSize().getX() == 0) {
				System.err.println("Asset: "+entity.getName()+" has a size of zero on x axis");
			}
			if (entity.getSize().getY() == 0) {
				System.err.println("Asset: "+entity.getName()+" has a size of zero on y axis");
			}
		}
	}
	
	public Entity getEntity(String name, int mapId, Vector newPosition) {
			
			if (entityAssets.containsKey(name)) {
				Entity entity = entityAssets.get(name).toBuilder()
					.setId(entityManager.reserveNewEntityId())
					.setMap(mapId)
					.setPosition(newPosition).build();
						
				return attachOrgans(entity);
			}
			else console.print("Error: Could not find asset: "+name);
			return null;
		}
	
	public Entity getEntity(String name, int mapId) {
		
		if (entityAssets.containsKey(name)) {
			Entity entity = entityAssets.get(name).toBuilder()
				.setId(entityManager.reserveNewEntityId())
				.setMap(mapId).build();
					
			return attachOrgans(entity);
		}
		else console.print("Error: Could not find asset: "+name);
		return null;
	}

	private Entity attachOrgans(Entity body) {
		if (!body.hasOrganAssetSlots()) return body;

		OrganAssetSlots assets = body.getOrganAssetSlots();
		OrganSlots.Builder slots = OrganSlots.newBuilder();
		setOrganSlot(slots, assets.getHeartAsset(), body, 0);
		setOrganSlot(slots, assets.getLungsAsset(), body, 1);
		setOrganSlot(slots, assets.getLiverAsset(), body, 2);
		setOrganSlot(slots, assets.getBrainAsset(), body, 3);
		setOrganSlot(slots, assets.getStomachAsset(), body, 4);
		return body.toBuilder().setOrganSlots(slots).build();
	}

	private void setOrganSlot(OrganSlots.Builder slots, String assetName, Entity body, int slot) {
		if (assetName == null || assetName.isBlank()) return;
		Entity asset = entityAssets.get(assetName);
		if (asset == null || !asset.hasOrganComponent()) {
			console.print("Error: Invalid organ asset: " + assetName);
			return;
		}

		OrganComponent component = asset.getOrganComponent().toBuilder()
				.setInstalledInEntityId(body.getId())
				.build();
		Entity organ = asset.toBuilder()
				.setId(entityManager.reserveNewEntityId())
				.setMap(-1)
				.setAnchored(true)
				.setOrganComponent(component)
				.clearOrganSlots()
				.clearOrganAssetSlots()
				.build();
		entityManager.updateEntity(organ);

		switch (slot) {
			case 0: slots.setHeartEntityId(organ.getId()); break;
			case 1: slots.setLungsEntityId(organ.getId()); break;
			case 2: slots.setLiverEntityId(organ.getId()); break;
			case 3: slots.setBrainEntityId(organ.getId()); break;
			case 4: slots.setStomachEntityId(organ.getId()); break;
			default: break;
		}
	}
	
	public final Entity getReadOnlyEntity(String name) {
		return entityAssets.get(name);
	}
	
	public boolean containsEntity(String name) {
		return entityAssets.containsKey(name);
	}
	
	public HashSet<String> getTypes(String typeName) {
		return typeMap.get(typeName);
	}
}
