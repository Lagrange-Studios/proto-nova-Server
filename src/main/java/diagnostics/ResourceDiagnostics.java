package diagnostics;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.management.OperatingSystemMXBean;

import entity.EntityManager;
import protonova.protobuf.EntityProto.Entity;

/** Collects honest, low-overhead diagnostics for the server diagnostics window. */
public class ResourceDiagnostics {
    private static volatile ConcurrentHashMap<Long, LongAdder> threadRx;
    private static volatile ConcurrentHashMap<Long, LongAdder> threadTx;
    private static final ConcurrentHashMap<String, AtomicInteger> THREAD_NAME_SEQUENCES = new ConcurrentHashMap<>();
    private static final AtomicInteger ACTIVE_SESSIONS = new AtomicInteger();

    private final EntityManager entityManager;
    private volatile int localSessions;
    private ThreadMXBean threadBean;
    private OperatingSystemMXBean osBean;
    private boolean enabledThreadCpuTime;
    private boolean enabledThreadAllocatedMemory;
    private volatile ConcurrentHashMap<Integer, LongAdder> entityCpuNanos;
    private volatile ConcurrentHashMap<Integer, LongAdder> entityNetworkBytes;
    private Map<Long, Long> previousThreadCpu;
    private Map<Long, Long> previousThreadAllocated;
    private Map<Integer, Long> previousEntityCpu;
    private long previousThreadSampleNanos;
    private long previousEntitySampleNanos;
    private long cachedWorldBytes;
    private long worldSizeCheckedAt;

    public ResourceDiagnostics(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Starts metric collection. Nothing is initialized until the diagnostics window opens. */
    public synchronized void beginSession() {
        localSessions++;
        activateGlobalCapture();
        if (localSessions > 1) return;

        entityCpuNanos = new ConcurrentHashMap<>();
        entityNetworkBytes = new ConcurrentHashMap<>();
        previousThreadCpu = new HashMap<>();
        previousThreadAllocated = new HashMap<>();
        previousEntityCpu = new HashMap<>();
        threadBean = ManagementFactory.getThreadMXBean();
        java.lang.management.OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
        osBean = base instanceof OperatingSystemMXBean ? (OperatingSystemMXBean) base : null;
        if (threadBean.isThreadCpuTimeSupported() && !threadBean.isThreadCpuTimeEnabled()) {
            try {
                threadBean.setThreadCpuTimeEnabled(true);
                enabledThreadCpuTime = true;
            } catch (SecurityException ignored) { }
        }
        if (threadBean instanceof com.sun.management.ThreadMXBean) {
            com.sun.management.ThreadMXBean sunThreadBean = (com.sun.management.ThreadMXBean) threadBean;
            if (sunThreadBean.isThreadAllocatedMemorySupported() && !sunThreadBean.isThreadAllocatedMemoryEnabled()) {
                try {
                    sunThreadBean.setThreadAllocatedMemoryEnabled(true);
                    enabledThreadAllocatedMemory = true;
                } catch (SecurityException ignored) { }
            }
        }
        resetSamples();
    }

    /** Stops metric collection and releases all samples when the diagnostics window closes. */
    public synchronized void endSession() {
        if (localSessions == 0) return;
        localSessions--;
        deactivateGlobalCapture();
        if (localSessions > 0) return;

        if (enabledThreadCpuTime && threadBean != null) {
            try { threadBean.setThreadCpuTimeEnabled(false); } catch (SecurityException ignored) { }
        }
        if (enabledThreadAllocatedMemory && threadBean instanceof com.sun.management.ThreadMXBean) {
            try {
                ((com.sun.management.ThreadMXBean) threadBean).setThreadAllocatedMemoryEnabled(false);
            } catch (SecurityException ignored) { }
        }
        enabledThreadCpuTime = false;
        enabledThreadAllocatedMemory = false;
        threadBean = null;
        osBean = null;
        resetSamples();
        entityCpuNanos = null;
        entityNetworkBytes = null;
        previousThreadCpu = null;
        previousThreadAllocated = null;
        previousEntityCpu = null;
    }

    public boolean isCapturing() {
        return localSessions > 0;
    }

    private void resetSamples() {
        if (previousThreadCpu != null) previousThreadCpu.clear();
        if (previousThreadAllocated != null) previousThreadAllocated.clear();
        if (previousEntityCpu != null) previousEntityCpu.clear();
        if (entityCpuNanos != null) entityCpuNanos.clear();
        if (entityNetworkBytes != null) entityNetworkBytes.clear();
        previousThreadSampleNanos = System.nanoTime();
        previousEntitySampleNanos = previousThreadSampleNanos;
        cachedWorldBytes = 0;
        worldSizeCheckedAt = 0;
    }

    public static void recordNetworkRead(long bytes) {
        ConcurrentHashMap<Long, LongAdder> counters = threadRx;
        if (counters == null || bytes <= 0) return;
        counters.computeIfAbsent(Thread.currentThread().getId(), id -> new LongAdder()).add(bytes);
    }

    public static void recordNetworkWrite(long bytes) {
        ConcurrentHashMap<Long, LongAdder> counters = threadTx;
        if (counters == null || bytes <= 0) return;
        counters.computeIfAbsent(Thread.currentThread().getId(), id -> new LongAdder()).add(bytes);
    }

    private static synchronized void activateGlobalCapture() {
        if (ACTIVE_SESSIONS.getAndIncrement() == 0) {
            threadRx = new ConcurrentHashMap<>();
            threadTx = new ConcurrentHashMap<>();
        }
    }

    private static synchronized void deactivateGlobalCapture() {
        if (ACTIVE_SESSIONS.decrementAndGet() == 0) {
            threadRx = null;
            threadTx = null;
        }
    }

    /** Creates a named thread without attaching diagnostics instrumentation. */
    public static Thread newThread(String name, Runnable task) {
        return new Thread(task, uniqueThreadName(name));
    }

    /** Creates a named executor thread factory without attaching diagnostics instrumentation. */
    public static ThreadFactory threadFactory(String namePrefix) {
        return task -> new Thread(task, uniqueThreadName(namePrefix));
    }

    private static String uniqueThreadName(String prefix) {
        int sequence = THREAD_NAME_SEQUENCES.computeIfAbsent(prefix, ignored -> new AtomicInteger()).incrementAndGet();
        return prefix + "-" + sequence;
    }

    public void recordEntityCpu(int entityId, long nanos) {
        ConcurrentHashMap<Integer, LongAdder> counters = entityCpuNanos;
        if (counters != null && nanos > 0) counters.computeIfAbsent(entityId, id -> new LongAdder()).add(nanos);
    }

    public void recordEntityNetwork(int entityId, long bytes) {
        ConcurrentHashMap<Integer, LongAdder> counters = entityNetworkBytes;
        if (counters != null && bytes > 0) counters.computeIfAbsent(entityId, id -> new LongAdder()).add(bytes);
    }

    public synchronized ProcessSnapshot getProcessSnapshot() {
        Runtime runtime = Runtime.getRuntime();
        if (localSessions == 0 || threadBean == null) {
            return new ProcessSnapshot(-1, runtime.totalMemory() - runtime.freeMemory(), runtime.totalMemory(),
                    runtime.maxMemory(), 0, 0, 0, 0);
        }
        long now = System.currentTimeMillis();
        if (now - worldSizeCheckedAt > 30_000) {
            cachedWorldBytes = directorySize(new File("worldRoot"));
            worldSizeCheckedAt = now;
        }
        ConcurrentHashMap<Long, LongAdder> rxCounters = threadRx;
        ConcurrentHashMap<Long, LongAdder> txCounters = threadTx;
        long rx = rxCounters == null ? 0 : rxCounters.values().stream().mapToLong(LongAdder::sum).sum();
        long tx = txCounters == null ? 0 : txCounters.values().stream().mapToLong(LongAdder::sum).sum();
        return new ProcessSnapshot(
                osBean == null ? -1 : osBean.getProcessCpuLoad() * 100.0,
                runtime.totalMemory() - runtime.freeMemory(), runtime.totalMemory(), runtime.maxMemory(),
                threadBean.getThreadCount(), cachedWorldBytes, rx, tx);
    }

    public synchronized List<ThreadSnapshot> getThreadSnapshots(Set<Long> stackThreadIds) {
        if (localSessions == 0 || threadBean == null) return java.util.Collections.emptyList();
        long now = System.nanoTime();
        long elapsed = Math.max(1, now - previousThreadSampleNanos);
        List<ThreadSnapshot> result = new ArrayList<>();
        Set<Long> liveIds = new HashSet<>();
        ConcurrentHashMap<Long, LongAdder> rxCounters = threadRx;
        ConcurrentHashMap<Long, LongAdder> txCounters = threadTx;
        com.sun.management.ThreadMXBean sunBean = threadBean instanceof com.sun.management.ThreadMXBean
                ? (com.sun.management.ThreadMXBean) threadBean : null;
        boolean cpuAvailable = threadBean.isThreadCpuTimeSupported() && threadBean.isThreadCpuTimeEnabled();
        boolean allocationAvailable = sunBean != null && sunBean.isThreadAllocatedMemorySupported()
                && sunBean.isThreadAllocatedMemoryEnabled();

        long[] ids = threadBean.getAllThreadIds();
        ThreadInfo[] infos = threadBean.getThreadInfo(ids, 0);
        for (int index = 0; index < ids.length; index++) {
            long id = ids[index];
            ThreadInfo info = infos[index];
            if (info == null) continue;
            liveIds.add(id);
            long reportedCpu = cpuAvailable ? threadBean.getThreadCpuTime(id) : -1;
            long cpu = reportedCpu < 0 ? -1 : reportedCpu;
            long priorCpu = previousThreadCpu.getOrDefault(id, cpu);
            double cpuPercent = cpu < 0 ? -1 : Math.max(0, (cpu - priorCpu) * 100.0 / elapsed);
            previousThreadCpu.put(id, cpu);

            long reportedAllocated = allocationAvailable ? sunBean.getThreadAllocatedBytes(id) : -1;
            long allocated = reportedAllocated < 0 ? -1 : reportedAllocated;
            long priorAllocated = previousThreadAllocated.getOrDefault(id, allocated);
            double allocationRate = allocated < 0 ? -1 : Math.max(0, (allocated - priorAllocated) * 1_000_000_000.0 / elapsed);
            previousThreadAllocated.put(id, allocated);

            String stack = "Expand this thread to capture its current Java stack.";
            if (stackThreadIds != null && stackThreadIds.contains(id)) {
                ThreadInfo stackInfo = threadBean.getThreadInfo(id, 32);
                stack = stackInfo == null ? "Thread ended before its stack could be captured." : formatStack(stackInfo);
            }
            result.add(new ThreadSnapshot(id, info.getThreadName(), info.getThreadState().name(),
                    cpuPercent, cpu, allocationRate, allocated,
                    value(rxCounters == null ? null : rxCounters.get(id)),
                    value(txCounters == null ? null : txCounters.get(id)), stack));
        }
        previousThreadCpu.keySet().retainAll(liveIds);
        previousThreadAllocated.keySet().retainAll(liveIds);
        previousThreadSampleNanos = now;
        result.sort(Comparator.comparingDouble((ThreadSnapshot item) -> item.cpuPercent).reversed());
        return result;
    }

    public synchronized List<EntitySnapshot> findEntities(String query) {
        return findEntities(query, Integer.MAX_VALUE);
    }

    public synchronized List<EntitySnapshot> findEntities(String query, int maximumResults) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (localSessions == 0 || normalized.isEmpty()) return java.util.Collections.emptyList();
        Integer requestedId = null;
        try { if (!normalized.isEmpty()) requestedId = Integer.valueOf(normalized); } catch (NumberFormatException ignored) { }

        long now = System.nanoTime();
        long elapsed = Math.max(1, now - previousEntitySampleNanos);
        Entity[] copy = entityManager.getAllEntitiesSnapshot();
        java.util.HashSet<Integer> liveIds = new java.util.HashSet<>();
        List<EntitySnapshot> result = new ArrayList<>();
        for (Entity entity : copy) {
            liveIds.add(entity.getId());
            if (requestedId != null ? entity.getId() != requestedId
                    : !entity.getName().toLowerCase(Locale.ROOT).contains(normalized)) continue;
            long cpu = value(entityCpuNanos.get(entity.getId()));
            long priorCpu = previousEntityCpu.getOrDefault(entity.getId(), cpu);
            double cpuPercent = Math.max(0, (cpu - priorCpu) * 100.0 / elapsed);
            int serializedBytes = entity.getSerializedSize();
            result.add(new EntitySnapshot(entity, cpuPercent, cpu, serializedBytes,
                    value(entityNetworkBytes.get(entity.getId()))));
            if (result.size() >= Math.max(1, maximumResults)) break;
        }
        for (Map.Entry<Integer, LongAdder> entry : entityCpuNanos.entrySet()) {
            previousEntityCpu.put(entry.getKey(), entry.getValue().sum());
        }
        previousEntityCpu.keySet().retainAll(liveIds);
        previousEntitySampleNanos = now;
        result.sort(Comparator.comparing(EntitySnapshot::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(item -> item.id));
        return result;
    }

    public String getEntityDetails(int entityId) {
        if (localSessions == 0 || entityManager == null) return "Diagnostics are no longer active.";
        Entity entity = entityManager.getEntity(entityId);
        return entity == null ? "Entity no longer exists." : entity.toString();
    }

    private static long value(LongAdder value) { return value == null ? 0 : value.sum(); }

    private static String formatStack(ThreadInfo info) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : info.getStackTrace()) builder.append("at ").append(element).append('\n');
        return builder.length() == 0 ? "No Java stack available (native or idle thread)." : builder.toString();
    }

    private static long directorySize(File file) {
        if (!file.exists()) return 0;
        if (file.isFile()) return file.length();
        File[] children = file.listFiles();
        if (children == null) return 0;
        long total = 0;
        for (File child : children) total += directorySize(child);
        return total;
    }

    public static final class ProcessSnapshot {
        public final double cpuPercent;
        public final long heapUsed, heapCommitted, heapMax;
        public final int threads;
        public final long worldBytes, networkRead, networkWritten;
        ProcessSnapshot(double cpuPercent, long heapUsed, long heapCommitted, long heapMax, int threads,
                long worldBytes, long networkRead, long networkWritten) {
            this.cpuPercent = cpuPercent; this.heapUsed = heapUsed; this.heapCommitted = heapCommitted;
            this.heapMax = heapMax; this.threads = threads; this.worldBytes = worldBytes;
            this.networkRead = networkRead; this.networkWritten = networkWritten;
        }
    }

    public static final class ThreadSnapshot {
        public final long id, cpuNanos, allocatedBytes, networkRead, networkWritten;
        public final String name, state, stack;
        public final double cpuPercent, allocationBytesPerSecond;
        ThreadSnapshot(long id, String name, String state, double cpuPercent, long cpuNanos,
                double allocationBytesPerSecond, long allocatedBytes, long networkRead, long networkWritten,
                String stack) {
            this.id = id; this.name = name; this.state = state; this.cpuPercent = cpuPercent;
            this.cpuNanos = cpuNanos; this.allocationBytesPerSecond = allocationBytesPerSecond;
            this.allocatedBytes = allocatedBytes; this.networkRead = networkRead;
            this.networkWritten = networkWritten; this.stack = stack;
        }
    }

    public static final class EntitySnapshot {
        public final int id, serializedBytes;
        public final String name;
        public final double cpuPercent;
        public final long cpuNanos, networkBytes;
        EntitySnapshot(Entity entity, double cpuPercent, long cpuNanos, int serializedBytes, long networkBytes) {
            this.id = entity.getId(); this.name = entity.getName(); this.cpuPercent = cpuPercent;
            this.cpuNanos = cpuNanos; this.serializedBytes = serializedBytes; this.networkBytes = networkBytes;
        }
        public String getName() { return name; }
    }
}
