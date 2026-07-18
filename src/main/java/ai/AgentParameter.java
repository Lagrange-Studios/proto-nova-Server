package ai;

import protonova.protobuf.EntityProto.Entity;

public interface AgentParameter {
	String getName();
	boolean canTarget(Entity entity);
	boolean canFindNewTarget();
	boolean canLooseTarget();
	double getRange();
	boolean closeOnPathEnd();
	boolean mustBeAlive();
	boolean canDamage(Entity entity);
}
