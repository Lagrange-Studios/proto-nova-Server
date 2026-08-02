package tag;

import health.PhysiologySystem;
import protonova.protobuf.EntityProto.Entity;

public class Physiology extends TagClass {

	private final PhysiologySystem physiologySystem = new PhysiologySystem();

	public String getTag() {
		return "physiology";
	}

	public boolean hasSecondTick() {
		return true;
	}

	public void secondTick(TagHandler tagHandler, Entity entity) {
		if (entity == null || !entity.getAlive()) return;

		Entity updated = physiologySystem.update(entity, tagHandler.getEntityManager());
		tagHandler.updateEntity(updated);
		tagHandler.getHealthManager().entityCheck(updated);
	}
}
