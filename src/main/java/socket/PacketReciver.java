package socket;

import entity.ChunkManager;
import entity.EntityFinder;
import entity.EntityManager;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.EntityProto.Entity;

public class PacketReciver {

	private EntityFinder entityFinder;
	private ChunkManager chunkManager;
	private EntityManager entityManager;
	
	public PacketReciver(EntityFinder entityFinder, ChunkManager chunkManager, EntityManager entityManager) {
		this.entityFinder = entityFinder;
		this.chunkManager = chunkManager;
		this.entityManager = entityManager;
	}
	
	public void recivePacket(Player player, ClientToServerPacket packet) {
		
		if (packet.hasUpdatedEntity()) {
			updatePlayerEntity(player, packet.getUpdatedEntity());
		}
		
	}

	private void updatePlayerEntity(Player player, Entity entity) {
		Entity oldEntity = entityManager.getEntity(player.data.getEntityId());
		
		if (!oldEntity.getPosition().equals(entity.getPosition())) {
			chunkManager.updateEntityPosition(oldEntity, entity.getPosition());
			
			entityManager.updateEntity(entity);
		}
	}
}
