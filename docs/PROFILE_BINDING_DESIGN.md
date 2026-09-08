# Config, Profile & Conditional Wiring Design

Design notes for replicating Spring Boot's config-loading priority, relaxed
binding, profile selection, and conditional bean registration in the custom
(non-Spring) framework.

## Concept in 3 words

`read -> filter -> build`

## The 6 key components

### 1. ConfigSource — uniform face for every source

```java
public interface ConfigSource {
    /** Return the raw value for the canonical key, or null if not present. */
    String get(String key);

    /** Lower order = higher priority (cmdline wins over env, env over file). */
    int order();
}
```

Implementations: `EnvVarSource`, `SystemPropsSource`, `CmdLineSource`,
`FileSource`. A `FileSource` wraps one `Properties` file.

### 2. ConfigRepository — the ordered stack (override = position)

```java
public class ConfigRepository {
    private final List<ConfigSource> sources; // sorted by order()

    public String get(String key) {
        for (ConfigSource s : sources) {
            String v = s.get(key);
            if (v != null) return v; // first hit wins — no override logic needed
        }
        return null;
    }
}
```

### 3. ConfigKey + normalizer — relaxed binding

Canonical form is lowercase, dot-separated: `server.app`.

Each `ConfigSource` translates the canonical key into its own dialect:

| Source | Key to look up |
|---|---|
| properties/yaml | `server.app` |
| env var | `SERVER_APP` (dot -> underscore, uppercase) |
| cmdline | `--server.app` |

### 4. Binder — value coercion

```java
public class Binder {
    private final ConfigRepository repo;
    int getInt(String key, int def);
    boolean getBoolean(String key, boolean def);
    String get(String key, String def);
    // optional: bind(key, Class<T>) for records/POJOs
}
```

Note: the existing static `AppConfig` is this component minus the priority
stack; the desired end state is `AppConfig` backed by `ConfigRepository`.

### 5. ProfileActivator — the selection engine

```java
public class ProfileActivator {
    private final ConfigRepository repo;
    private final Set<String> activeProfiles = new HashSet<>();

    public void activate() {
        String profile = repo.get("server.app"); // priority chain already in repo
        if (profile != null && !profile.isBlank()) activeProfiles.add(profile);
        // load application-{profile}.properties and INSERT it ABOVE the base file
        // so profile-specific values win
    }

    public boolean isActive(String profile) { return activeProfiles.contains(profile); }
    public Set<String> getActiveProfiles() { return activeProfiles; }
}
```

### 6. ConditionEvaluator + ProfileCondition — the @Profile gate

```java
public interface Condition {
    boolean matches(ConditionContext ctx);
}

public class ConditionContext {
    ProfileActivator profiles;
    ConfigRepository config;
    // anything else a condition needs
}

public class ProfileCondition implements Condition {
    private final String[] profiles;
    boolean matches(ConditionContext ctx) {
        for (String p : profiles) {
            if (ctx.profiles.isActive(p)) return true;
            if (p.startsWith("!")) {
                String negated = p.substring(1);
                if (!ctx.profiles.isActive(negated)) return true;
            }
        }
        return false;
    }
}

public class ConditionEvaluator {
    boolean shouldRegister(Class<?> candidate, ConditionContext ctx);
    // reads @Profile(...) annotation, builds ProfileCondition, calls matches(ctx)
    // false => the candidate is skipped during wiring
}
```

## Wiring step (stand-in for the bean container)

During startup, iterate candidate components (handlers, bootstraps, services);
run each through `ConditionEvaluator`; only wire the ones that pass.

## Startup assembly order (mirrors Spring Boot)

```
1. ConfigRepository = [cmdline, env, system, base-file]      // load env/files in priority order
2. ProfileActivator.activate() -> repo.get("server.app")      // read active profile
3. if profile != null -> load application-{profile}.properties, insert above base-file
4. build components -> ConditionEvaluator filters by active profiles
5. serve
```

## What is genuinely tricky vs. mechanical

- **Chicken-and-egg** (need the profile to load profile files, but the profile
  may live in a file): Spring solves it with multi-pass `ConfigDataEnvironment`.
  Our case is trivial because the profile never comes from the file it selects.
- **Override semantics**: not logic — just stack position.
- **Bean gating**: mechanical (`Condition` -> boolean -> skip).

## Mapping to the current codebase

- `AppConfig` -> Binder (needs ConfigRepository backing)
- `ServerApp`/`ServerProfile` -> ProfileActivator (read `server.app`, build the
  right `DefaultLegacyConnectionHandler` vs `NIOIncomingSocketHandler`)
- `application.properties` -> base FileSource
- No bean container exists yet -> `ConditionEvaluator` only needed if/when
  component registration becomes annotation-driven