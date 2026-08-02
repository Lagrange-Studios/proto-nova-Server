package entity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import plane.PlaneManager;
import protonova.protobuf.ChatProto.ChatMessage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.OrganComponent;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.VectorProto.Vector;

public class ChunkManagerTest {

	@Test
	public void installedOrgansAreNotWorldEntities() {
		Entity installedOrgan = Entity.newBuilder()
				.setMap(-1)
				.setOrganComponent(OrganComponent.newBuilder().setInstalledInEntityId(12))
				.build();

		assertFalse(ChunkManager.isWorldEntity(installedOrgan));
	}

	@Test
	public void inventoryEntitiesAreNotWorldEntities() {
		Entity inventoryItem = Entity.newBuilder().setMap(0).build();

		assertFalse(ChunkManager.isWorldEntity(inventoryItem));
	}

	@Test
	public void entitiesOnAPlaneAreWorldEntities() {
		Entity worldEntity = Entity.newBuilder().setMap(1).build();

		assertTrue(ChunkManager.isWorldEntity(worldEntity));
	}

	@Test
	public void entitiesWithoutAPlaneAreNotGroupedIntoChunks() {
		HashMap<Integer, Entity> entities = new HashMap<>();
		entities.put(3, Entity.newBuilder().setId(3).setMap(1).build());
		ChunkManager chunkManager = new ChunkManager(entities, new PlaneManager(new HashMap<>()));

		chunkManager.groupAllEntites();

		assertFalse(chunkManager.getChunks().containsKey(1));
	}

	@Test
	public void entitiesWithAPlaneAreGroupedIntoChunks() {
		HashMap<Integer, Entity> entities = new HashMap<>();
		HashMap<Integer, Plane> planes = new HashMap<>();
		entities.put(3, Entity.newBuilder().setId(3).setMap(1).build());
		planes.put(1, Plane.newBuilder().setId(1).build());
		ChunkManager chunkManager = new ChunkManager(entities, new PlaneManager(planes));

		chunkManager.groupAllEntites();

		assertTrue(chunkManager.getChunks().containsKey(1));
	}

	@Test
	public void chatCleanupCanRunWhileNewPlaneChunksAreAdded() throws Exception {
		HashMap<Integer, Plane> planes = new HashMap<>();
		for (int planeId = 1; planeId <= 250; planeId++) {
			planes.put(planeId, Plane.newBuilder().setId(planeId).build());
		}
		ChunkManager chunkManager = new ChunkManager(new HashMap<>(), new PlaneManager(planes));
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService threads = Executors.newFixedThreadPool(2);

		Future<?> cleanup = threads.submit(() -> {
			start.await();
			for (int pass = 0; pass < 1000; pass++) {
				chunkManager.removeAllChatMessages();
			}
			return null;
		});
		Future<?> additions = threads.submit(() -> {
			start.await();
			for (int planeId = 1; planeId <= 250; planeId++) {
				chunkManager.addChatMessage(ChatMessage.newBuilder()
						.setMap(planeId)
						.setPosition(Vector.newBuilder().setX(planeId).setY(planeId))
						.build());
			}
			return null;
		});

		start.countDown();
		cleanup.get();
		additions.get();
		threads.shutdown();
	}
}
