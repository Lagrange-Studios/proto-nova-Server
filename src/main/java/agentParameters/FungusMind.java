package agentParameters;

import ai.AgentParameter;
import protonova.protobuf.EntityProto.Entity;

public class FungusMind implements AgentParameter {

	@Override
	public String getName() {
		return "fungusMind";
	}
	
	@Override
	public boolean canTarget(Entity target) {
		return target.getName().equals("human");
	}

	@Override
	public boolean canFindNewTarget() {
		return false;
	}

	@Override
	public boolean canLooseTarget() {
		return false;
	}
	
	@Override
	public double getRange() {
		return 15;
	}

	@Override
	public boolean closeOnPathEnd() {
		return false;
	}

	@Override
	public boolean mustBeAlive() {
		return true;
	}

	@Override
	public boolean canDamage(Entity entity) {
		return entity.getName().equals("human") || entity.getTagsList().contains("plant");
	}

}
