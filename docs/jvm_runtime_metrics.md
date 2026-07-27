Here is a practical mapping of the **OpenTelemetry RuntimeTelemetry JVM metrics** to the metric names you'll typically see exported.

```text
                           JVM
                            │
    ┌──────────────┬────────┼────────┬─────────────┬──────────────┐
    ▼              ▼        ▼        ▼             ▼
  Memory           GC     Threads    CPU         Classes
```

# Memory

```text
jvm.memory.used
jvm.memory.committed
jvm.memory.limit
jvm.memory.used_after_last_gc
```

Diagram:

```text
jvm.memory.limit
┌────────────────────────────┐
│ Max Heap (-Xmx)            │
│                            │
│ jvm.memory.committed       │
│ ┌──────────────────────┐   │
│ │ Reserved by JVM      │   │
│ │                      │   │
│ │ jvm.memory.used      │   │
│ │ ┌───────────────┐    │   │
│ │ │ Actual Usage  │    │   │
│ │ └───────────────┘    │   │
│ └──────────────────────┘   │
└────────────────────────────┘

jvm.memory.used_after_last_gc
```

Useful labels:

```text
pool="G1 Eden Space"
pool="G1 Old Gen"
pool="Metaspace"
pool="Code Cache"
type="heap"
type="non_heap"
```

---

# Garbage Collection

```text
jvm.gc.duration
```

Histogram metric.

Prometheus exposes:

```text
jvm_gc_duration_seconds_bucket
jvm_gc_duration_seconds_sum
jvm_gc_duration_seconds_count
```

Diagram:

```text
Objects
   ↓
Heap Full
   ↓
GC Runs
   ↓
Pause Time
   ↓
jvm.gc.duration
```

Labels:

```text
gc="G1 Young Generation"
gc="G1 Old Generation"
```

---

# Threads

```text
jvm.thread.count
```

Labels:

```text
state="RUNNABLE"
state="BLOCKED"
state="WAITING"
state="TIMED_WAITING"
```

Diagram:

```text
jvm.thread.count
        │
        ├── RUNNABLE
        ├── BLOCKED
        ├── WAITING
        └── TIMED_WAITING
```

Example Prometheus output:

```text
jvm_thread_count{state="RUNNABLE"} 15
jvm_thread_count{state="WAITING"} 120
jvm_thread_count{state="BLOCKED"} 2
```

---

# CPU

```text
jvm.cpu.count
jvm.cpu.time
jvm.cpu.recent_utilization
```

Diagram:

```text
CPU
 │
 ├── jvm.cpu.count
 │      Available processors
 │
 ├── jvm.cpu.time
 │      Total CPU consumed
 │
 └── jvm.cpu.recent_utilization
        Current utilization
```

Examples:

```text
jvm_cpu_count 8

jvm_cpu_recent_utilization 0.72
```

Meaning:

```text
72% JVM CPU usage
```

---

# Class Loading

```text
jvm.class.loaded
jvm.class.unloaded
```

Some exporters additionally expose current loaded class count.

Diagram:

```text
ClassLoader
     │
     ├── jvm.class.loaded
     │
     └── jvm.class.unloaded
```

---

# What I usually graph first

```text
┌─────────────────────────────────────┐
│ CPU                                 │
│ jvm.cpu.recent_utilization          │
├─────────────────────────────────────┤
│ Heap                                │
│ jvm.memory.used                     │
│ jvm.memory.used_after_last_gc       │
├─────────────────────────────────────┤
│ GC                                  │
│ jvm.gc.duration                     │
├─────────────────────────────────────┤
│ Threads                             │
│ jvm.thread.count                    │
├─────────────────────────────────────┤
│ Class Loading                       │
│ jvm.class.loaded                    │
└─────────────────────────────────────┘
```

One caveat: the **exact metric names in Prometheus** may be transformed from OpenTelemetry semantic names. For example:

```text
OpenTelemetry:  jvm.memory.used
Prometheus:     jvm_memory_used_bytes

OpenTelemetry:  jvm.thread.count
Prometheus:     jvm_thread_count

OpenTelemetry:  jvm.cpu.time
Prometheus:     jvm_cpu_time_seconds_total
```