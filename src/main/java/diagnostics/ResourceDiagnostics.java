package diagnostics;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.management.OperatingSystemMXBean;

import entity.EntityManager;
import protonova.protobuf.EntityProto.Entity;

/** Collects honest, low-overhead diagnostics for the server diagnostics window. */
public class ResourceDiagnostics {
    private static final ConcurrentHashMap<Long, LongAdder> THREAD_RX = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, LongAdder> THREAD_TX = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, StackTraceElement> THREAD_CREATION = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> THREAD_NAME_SEQUENCES = new ConcurrentHashMap<>();

    private final EntityManager entityManager;
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final OperatingSystemMXBean osBean;
    private final ConcurrentHashMap<Integer, LongAdder> entityCpuNanos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, LongAdder> entityNetworkBytes = new ConcurrentHashMap<>();
    private final Map<Long, Long> previousThreadCpu = new HashMap<>();
    private final Map<Long, Long> previousThreadAllocated = new HashMap<>();
    private final Map<Integer, Long> previousEntityCpu = new HashMap<>();
    private long previousThreadSampleNanos = System.nanoTime();
    private long previousEntitySampleNanos = System.nanoTime();
    private long cachedWorldBytes;
    private long worldSizeCheckedAt;

    public ResourceDiagnostics(EntityManager entityManager) {
        this.entityManager = entityManager;
        java.lang.management.OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
        this.osBean = base instanceof OperatingSystemMXBean ? (OperatingSystemMXBean) base : null;
        if (threadBean.isThreadCpuTimeSupported() && !threadBean.isThreadCpuTimeEnabled()) {
            try { threadBean.setThreadCpuTimeEnabled(true); } catch (SecurityException ignored) { }
        }
        if (threadBean instanceof com.sun.management.ThreadMXBean) {
            com.sun.management.ThreadMXBean sunThreadBean = (com.sun.management.ThreadMXBean) threadBean;
            if (sunThreadBean.isThreadAllocatedMemorySupported() && !sunThreadBean.isThreadAllocatedMemoryEnabled()) {
                try { sunThreadBean.setThreadAllocatedMemoryEnabled(true); } catch (SecurityException ignored) { }
            }
        }
    }

    public static void recordNetworkRead(long bytes) {
        THREAD_RX.computeIfAbsent(Thread.currentThread().getId(), id -> new LongAdder()).add(bytes);
    }

    public static void recordNetworkWrite(long bytes) {
        THREAD_TX.computeIfAbsent(Thread.currentThread().getId(), id -> new LongAdder()).add(bytes);
    }

    /** Creates a named thread and records the source line that requested it. */
    public static Thread newThread(String name, Runnable task) {
        StackTraceElement creationSite = callerOutsideDiagnostics();
        return new Thread(() -> {
            THREAD_CREATION.put(Thread.currentThread().getId(), creationSite);
            task.run();
        }, uniqueThreadName(name));
    }

    /** Creates an executor thread factory whose threads point back to the pool's source line. */
    public static ThreadFactory threadFactory(String namePrefix) {
        StackTraceElement creationSite = callerOutsideDiagnostics();
        return task -> {
            return new Thread(() -> {
                THREAD_CREATION.put(Thread.currentThread().getId(), creationSite);
                task.run();
            }, uniqueThreadName(namePrefix));
        };
    }

    private static String uniqueThreadName(String prefix) {
        int sequence = THREAD_NAME_SEQUENCES.computeIfAbsent(prefix, ignored -> new AtomicInteger()).incrementAndGet();
        return prefix + "-" + sequence;
    }

    public void recordEntityCpu(int entityId, long nanos) {
        if (nanos > 0) entityCpuNanos.computeIfAbsent(entityId, id -> new LongAdder()).add(nanos);
    }

    public void recordEntityNetwork(int entityId, long bytes) {
        if (bytes > 0) entityNetworkBytes.computeIfAbsent(entityId, id -> new LongAdder()).add(bytes);
    }

    public synchronized ProcessSnapshot getProcessSnapshot() {
        Runtime runtime = Runtime.getRuntime();
        long now = System.currentTimeMillis();
        if (now - worldSizeCheckedAt > 10_000) {
            cachedWorldBytes = directorySize(new File("worldRoot"));
            worldSizeCheckedAt = now;
        }
        long rx = THREAD_RX.values().stream().mapToLong(LongAdder::sum).sum();
        long tx = THREAD_TX.values().stream().mapToLong(LongAdder::sum).sum();
        return new ProcessSnapshot(
                osBean == null ? -1 : osBean.getProcessCpuLoad() * 100.0,
                runtime.totalMemory() - runtime.freeMemory(), runtime.totalMemory(), runtime.maxMemory(),
                threadBean.getThreadCount(), cachedWorldBytes, rx, tx);
    }

    public synchronized List<ThreadSnapshot> getThreadSnapshots() {
        long now = System.nanoTime();
        long elapsed = Math.max(1, now - previousThreadSampleNanos);
        List<ThreadSnapshot> result = new ArrayList<>();
        com.sun.management.ThreadMXBean sunBean = threadBean instanceof com.sun.management.ThreadMXBean
                ? (com.sun.management.ThreadMXBean) threadBean : null;

        for (long id : threadBean.getAllThreadIds()) {
            ThreadInfo info = threadBean.getThreadInfo(id, 64);
            if (info == null) continue;
            long cpu = threadBean.isThreadCpuTimeSupported() ? Math.max(0, threadBean.getThreadCpuTime(id)) : -1;
            long priorCpu = previousThreadCpu.getOrDefault(id, cpu);
            double cpuPercent = cpu < 0 ? -1 : Math.max(0, (cpu - priorCpu) * 100.0 / elapsed);
            previousThreadCpu.put(id, cpu);

            long allocated = sunBean == null || !sunBean.isThreadAllocatedMemorySupported()
                    ? -1 : Math.max(0, sunBean.getThreadAllocatedBytes(id));
            long priorAllocated = previousThreadAllocated.getOrDefault(id, allocated);
            double allocationRate = allocated < 0 ? -1 : Math.max(0, (allocated - priorAllocated) * 1_000_000_000.0 / elapsed);
            previousThreadAllocated.put(id, allocated);

            result.add(new ThreadSnapshot(id, info.getThreadName(), info.getThreadState().name(),
                    cpuPercent, cpu, allocationRate, allocated,
                    value(THREAD_RX.get(id)), value(THREAD_TX.get(id)), formatCreationSite(id), formatStack(info)));
        }
        previousThreadCpu.keySet().retainAll(toThreadIdList(result));
        previousThreadAllocated.keySet().retainAll(toThreadIdList(result));
        THREAD_CREATION.keySet().retainAll(toThreadIdList(result));
        previousThreadSampleNanos = now;
        result.sort(Comparator.comparingDouble((ThreadSnapshot item) -> item.cpuPercent).reversed());
        return result;
    }

    public synchronized List<EntitySnapshot> findEntities(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
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
                    : !normalized.isEmpty() && !entity.getName().toLowerCase().contains(normalized)) continue;
            long cpu = value(entityCpuNanos.get(entity.getId()));
            long priorCpu = previousEntityCpu.getOrDefault(entity.getId(), cpu);
            double cpuPercent = Math.max(0, (cpu - priorCpu) * 100.0 / elapsed);
            int serializedBytes = entity.getSerializedSize();
            result.add(new EntitySnapshot(entity, cpuPercent, cpu, serializedBytes,
                    value(entityNetworkBytes.get(entity.getId()))));
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

    private static long value(LongAdder value) { return value == null ? 0 : value.sum(); }

    private static List<Long> toThreadIdList(List<ThreadSnapshot> snapshots) {
        List<Long> ids = new ArrayList<>();
        for (ThreadSnapshot snapshot : snapshots) ids.add(snapshot.id);
        return ids;
    }

    private static String formatStack(ThreadInfo info) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : info.getStackTrace()) builder.append("at ").append(element).append('\n');
        return builder.length() == 0 ? "No Java stack available (native or idle thread)." : builder.toString();
    }

    private static StackTraceElement callerOutsideDiagnostics() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (!element.getClassName().equals(Thread.class.getName())
                    && !element.getClassName().equals(ResourceDiagnostics.class.getName())) return element;
        }
        return new StackTraceElement("unknown", "unknown", "Unknown source", -1);
    }

    private static String formatCreationSite(long threadId) {
        StackTraceElement site = THREAD_CREATION.get(threadId);
        if (site == null) return "Not captured (thread was created by the JVM, a library, or before diagnostics started)";
        String topLevelClass = site.getClassName().split("\\$", 2)[0];
        String sourcePath = site.getFileName() == null ? "Unknown file"
                : "src/main/java/" + topLevelClass.replace('.', '/') + ".java";
        String line = site.getLineNumber() < 0 ? "unknown" : String.valueOf(site.getLineNumber());
        return sourcePath + ":" + line + " — " + site.getClassName() + "." + site.getMethodName() + "()";
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
        public final String name, state, creationSite, stack;
        public final double cpuPercent, allocationBytesPerSecond;
        ThreadSnapshot(long id, String name, String state, double cpuPercent, long cpuNanos,
                double allocationBytesPerSecond, long allocatedBytes, long networkRead, long networkWritten,
                String creationSite, String stack) {
            this.id = id; this.name = name; this.state = state; this.cpuPercent = cpuPercent;
            this.cpuNanos = cpuNanos; this.allocationBytesPerSecond = allocationBytesPerSecond;
            this.allocatedBytes = allocatedBytes; this.networkRead = networkRead;
            this.networkWritten = networkWritten; this.creationSite = creationSite; this.stack = stack;
        }
    }

    public static final class EntitySnapshot {
        public final int id, serializedBytes;
        public final String name, details;
        public final double cpuPercent;
        public final long cpuNanos, networkBytes;
        EntitySnapshot(Entity entity, double cpuPercent, long cpuNanos, int serializedBytes, long networkBytes) {
            this.id = entity.getId(); this.name = entity.getName(); this.cpuPercent = cpuPercent;
            this.cpuNanos = cpuNanos; this.serializedBytes = serializedBytes; this.networkBytes = networkBytes;
            this.details = entity.toString();
        }
        public String getName() { return name; }
    }
}
