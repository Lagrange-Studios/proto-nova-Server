package entity;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

public class EntityManagerConcurrencyTest {

	@Test
	public void parallelEntityIdReservationsStayUnique() throws Exception {
		EntityManager entityManager = new EntityManager(Collections.emptyMap());
		int threadCount = 8;
		int reservationsPerThread = 200;
		Set<Integer> reservedIds = ConcurrentHashMap.newKeySet();
		CountDownLatch startTogether = new CountDownLatch(1);
		ExecutorService threads = Executors.newFixedThreadPool(threadCount);
		Future<?>[] reservations = new Future<?>[threadCount];

		for (int threadNumber = 0; threadNumber < threadCount; threadNumber++) {
			reservations[threadNumber] = threads.submit(() -> {
				startTogether.await();
				for (int reservationNumber = 0;
						reservationNumber < reservationsPerThread;
						reservationNumber++) {
					reservedIds.add(entityManager.reserveNewEntityId());
				}
				return null;
			});
		}

		startTogether.countDown();
		for (Future<?> reservation : reservations) {
			reservation.get();
		}
		threads.shutdownNow();

		int expectedReservationCount = threadCount * reservationsPerThread;
		assertEquals(expectedReservationCount, reservedIds.size());
		assertEquals(expectedReservationCount, entityManager.getAllEntities().size());
	}
}
