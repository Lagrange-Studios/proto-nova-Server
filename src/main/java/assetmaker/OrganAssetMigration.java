package assetmaker;

import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.Brain;
import protonova.protobuf.OrgansProto.OrganAssetSlots;
import protonova.protobuf.OrgansProto.OrganComponent;
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.OrgansProto.Stomach;

/** One-time migration from embedded human organs to organ assets. Later runs are harmless. */
public class OrganAssetMigration {
	private static final float ORGAN_SIZE = 1.0f / 3.0f;

	public static void main(String[] args) {
		AssetMaker assets = new AssetMaker();
		Entity human = assets.loadAsset("human");
		if (human == null) throw new IllegalStateException("Missing human asset");
		if (!human.getTagsList().contains("physiology")) {
			human = human.toBuilder().addTags("physiology").build();
		}
		human = human.toBuilder()
				.setHitDamage(human.getHitDamage().toBuilder().setCanAttack(true))
				.build();

		Organs organs = human.getOrgans();
		if (!organs.hasHeart() && human.hasOrganAssetSlots()) {
			refresh(assets, "human heart");
			refresh(assets, "human lungs");
			refresh(assets, "human liver");
			refresh(assets, "human brain");
			refresh(assets, "human stomach");
			if (!assets.saveEntity("human", human)) throw new IllegalStateException("Could not save human asset");
			return;
		}
		migrate(assets, "human heart", OrganComponent.newBuilder().setHeart(organs.getHeart()).build());
		migrate(assets, "human lungs", OrganComponent.newBuilder().setLungs(organs.getLungs()).build());
		migrate(assets, "human liver", OrganComponent.newBuilder().setLiver(organs.getLiver()).build());
		migrate(assets, "human brain", OrganComponent.newBuilder()
				.setBrain(organs.hasBrain() ? organs.getBrain() : Brain.newBuilder().build()).build());
		migrate(assets, "human stomach", OrganComponent.newBuilder()
				.setStomach(organs.hasStomach() ? organs.getStomach() : Stomach.newBuilder()
						.setChemicalCapacity(50).setAbsorptionPerSecond(1).setOxygenUsePerSecond(0.5f).build())
				.build());

		Organs.Builder bodyState = organs.toBuilder()
				.clearHeart().clearLungs().clearLiver().clearBrain().clearStomach();
		Entity migratedHuman = human.toBuilder()
				.setOrgans(bodyState)
				.setOrganAssetSlots(OrganAssetSlots.newBuilder()
						.setHeartAsset("human heart")
						.setLungsAsset("human lungs")
						.setLiverAsset("human liver")
						.setBrainAsset("human brain")
						.setStomachAsset("human stomach"))
				.build();
		if (!assets.saveEntity("human", migratedHuman)) throw new IllegalStateException("Could not save human asset");
	}

	private static void refresh(AssetMaker assets, String name) {
		Entity organ = assets.loadAsset(name);
		if (organ == null || !organ.hasOrganComponent()) {
			throw new IllegalStateException("Missing organ asset: " + name);
		}
		migrate(assets, name, organ.getOrganComponent());
	}

	private static void migrate(AssetMaker assets, String name, OrganComponent component) {
		Entity organ = assets.loadAsset(name);
		if (organ == null) organ = assets.createAsset(name);
		Entity.Builder builder = organ.toBuilder()
				.setName(name)
				.setDisplayTexture(name)
				.setSize(organ.getSize().toBuilder().setX(ORGAN_SIZE).setY(ORGAN_SIZE))
				.setIsItem(true)
				.setCanDestroy(true)
				.setStackable(false)
				.setAmount(1)
				.setOrganComponent(component)
				.clearOrganSlots()
				.clearOrganAssetSlots()
				.clearOrgans();
		if (!assets.saveEntity(name, builder.build())) throw new IllegalStateException("Could not save " + name);
	}
}
