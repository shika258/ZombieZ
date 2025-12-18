# ZombieZ Performance Analysis Report

## Executive Summary

This analysis identifies performance anti-patterns in the ZombieZ codebase that could impact the target of **200 concurrent players at 20 TPS**. The findings are categorized by severity and include specific recommendations.

---

## Critical Issues

### 1. High-Frequency Timer Tasks Iterating All Players

**Location:** `DynamicBossBarManager.java:64-68`
```java
}.runTaskTimer(plugin, 5L, 5L); // Every 0.25 seconds
for (Player player : Bukkit.getOnlinePlayers()) {
    updatePlayerBossBar(player);
}
```

**Problem:** With 200 players, this creates **800 boss bar updates per second** on the main thread. The `updatePlayerBossBar()` method calls `determineState()` which checks multiple managers (EventManager, MomentumManager) for each player.

**Impact:** ~4 method calls per player × 200 players × 4 times/second = **3,200+ method invocations/second**

**Recommendation:**
- Increase interval to 10-20 ticks (0.5-1 second)
- Batch updates in smaller groups per tick
- Only update on state change, not continuously

---

### 2. Repeated `Bukkit.getOnlinePlayers()` Iteration in OccultisteTalentListener

**Location:** `OccultisteTalentListener.java` - multiple locations (lines 227, 1133, 2164, 2439, 2615, 2721, 2817, 3569)

**Problem:** The talent listener iterates over ALL online players multiple times per game tick to find players with specific talents:

```java
for (Player player : Bukkit.getOnlinePlayers()) {
    if (hasTalentEffect(player, Talent.TalentEffectType.PERMAFROST)) {
        // ...
    }
}
```

**Impact:** This is an **O(n) lookup** repeated in combat calculations, turning what should be O(1) into O(n×m) where m is the number of talent checks.

**Recommendation:**
- Maintain a pre-computed Set of players per talent effect
- Update the cache on player join/leave/talent change
- Use `activeOccultistes` pattern consistently (already partially implemented)

---

### 3. ZombieAIManager Tick Task with Server.getEntity() Calls

**Location:** `ZombieAIManager.java:161-180`
```java
private void tickAllAIs() {
    for (Map.Entry<UUID, ZombieAI> entry : activeAIs.entrySet()) {
        UUID zombieId = entry.getKey();
        Entity entity = plugin.getServer().getEntity(zombieId); // O(1) but still overhead
        if (entity == null || !entity.isValid() || entity.isDead()) {
            activeAIs.remove(zombieId); // ConcurrentModificationException risk
            // ...
        }
        ai.tick();
    }
}
```

**Problem:**
1. `getEntity()` called for every tracked zombie every 5 ticks
2. Modifying `activeAIs` while iterating can cause issues (though ConcurrentHashMap mitigates this)
3. With 1000+ zombies at peak, this is significant overhead

**Recommendation:**
- Store entity reference directly in ZombieAI class
- Use Iterator.remove() pattern or collect IDs to remove
- Consider ticking AIs in batches across multiple server ticks

---

## High Severity Issues

### 4. `getNearbyEntities()` Called Frequently in Combat

**Locations:**
- `SpawnSystem.java:366` - density check
- `EliteZombieAI.java` - 12 occurrences
- `TankZombieAI.java:161, 199`
- `ConsumableEffects.java` - 8 occurrences

**Problem:** `getNearbyEntities()` creates new collections and performs spatial queries. In combat-heavy scenarios with many zombies using abilities simultaneously:

```java
zombie.getWorld().getNearbyEntities(zombie.getLocation(), 2, 2, 2).stream()
    .filter(e -> e instanceof Player)
    .map(e -> (Player) e)
    .forEach(p -> { /* damage */ });
```

**Impact:** Each call allocates memory and performs AABB intersection tests. With 100 zombies using abilities, this could be **1000+ spatial queries per second**.

**Recommendation:**
- Cache nearby player lists with short TTL (250ms)
- Use Paper's `getNearbyPlayers()` when only looking for players
- Consider spatial partitioning for high-density scenarios

---

### 5. Stream Operations Creating Garbage in Hot Paths

**Locations:** 610 occurrences across 84 files

**Problem:** Stream operations like `.stream().filter().map().forEach()` create temporary objects (lambdas, iterators, optional) that increase GC pressure:

```java
world.getNearbyEntities(loc, 5, 3, 5).stream()
    .filter(e -> e instanceof LivingEntity)
    .filter(e -> e != player)
    .forEach(e -> { /* ... */ });
```

**Impact:** In combat loops, these allocations can cause GC stutters, especially noticeable at high player counts.

**Recommendation:**
- Use traditional for-loops in hot paths
- Pre-allocate reusable lists where possible
- Reserve streams for initialization and infrequent operations

---

### 6. LeaderboardManager Iterating All Players After Refresh

**Location:** `LeaderboardManager.java:73-75`
```java
for (Player player : plugin.getServer().getOnlinePlayers()) {
    updatePlayerRanks(player.getUniqueId());
}
```

And `updatePlayerRanks()` at lines 121-135:
```java
for (LeaderboardType type : LeaderboardType.values()) {  // 11 types
    List<LeaderboardEntry> entries = leaderboardCache.get(type);
    for (int i = 0; i < entries.size(); i++) {  // up to 100 entries
        if (entries.get(i).getUuid().equals(uuid)) {
            ranks.put(type, i + 1);
            break;
        }
    }
}
```

**Impact:** 200 players × 11 types × up to 100 entries = **220,000 comparisons** every minute.

**Recommendation:**
- Build a reverse index `Map<UUID, Map<LeaderboardType, Integer>>` during refresh
- Update ranks incrementally when scores change

---

## Medium Severity Issues

### 7. Main Thread Zone Checking

**Location:** `ZombieZPlugin.java:776`
```java
Bukkit.getScheduler().runTaskTimer(this, () -> {
    if (zoneManager != null) {
        zoneManager.checkPlayersZones();
    }
}, 10L, 10L); // Every 0.5 seconds
```

**Problem:** While the ZoneManager uses TreeMap for O(log n) zone lookup, checking 200 players every 10 ticks adds up.

**Recommendation:** Already optimized with caching, but consider:
- Only checking players who have moved (use PlayerMoveListener cache)
- Event-driven zone detection instead of polling

---

### 8. Spawn Location Validation Loop

**Location:** `SpawnSystem.java:260-281`
```java
for (int attempt = 0; attempt < 10; attempt++) {
    // Calculate random position
    Location groundLoc = findGround(loc);  // Inner loop: world.getBlockAt() calls
    if (groundLoc != null && isValidSpawnLocation(groundLoc, player)) {
        // isValidSpawnLocation calls isSpawnDensityValid -> getNearbyEntities
    }
}
```

**Problem:** Up to 10 attempts × block checks × density validation per spawn. With 200 players spawning zombies, this is significant.

**Recommendation:**
- Pre-compute spawn locations asynchronously
- Cache valid spawn areas per zone
- Reduce density check radius or frequency

---

### 9. World.getEntities() on Startup

**Location:** `ZombieZPlugin.java:837-838`
```java
for (World world : Bukkit.getWorlds()) {
    for (Entity entity : world.getEntities()) {
        // Cleanup logic
    }
}
```

**Problem:** Iterates ALL entities in all worlds. On a server with thousands of entities, this causes a startup lag spike.

**Recommendation:**
- Run asynchronously where possible
- Process in batches with `runTaskTimer`
- Use chunk-based entity iteration

---

### 10. Pet Manager Multiple Tick Tasks

**Location:** `PetManager.java:631-643`
```java
// Task 1: Every 20 ticks
passiveTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    for (Player player : Bukkit.getOnlinePlayers()) {
        tickPassive(player);
    }
}, 20L, 20L);

// Task 2: Every 10 ticks
ultimateTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    for (Player player : Bukkit.getOnlinePlayers()) {
        autoActivateUltimate(player);
    }
}, 10L, 10L);
```

**Problem:** Multiple tasks iterating all players independently. With 200 players, that's 400 iterations per second just for pets.

**Recommendation:**
- Combine into a single tick task with internal counters
- Only process players with active pets

---

## Positive Patterns Found

The codebase already implements several good practices:

1. **Caffeine Cache** for PlayerData - Excellent choice for high-throughput scenarios
2. **HikariCP Connection Pool** - Properly configured for 200+ players
3. **ConcurrentHashMap** usage - Thread-safe collections where needed
4. **TreeMap for Zone Lookups** - O(log n) instead of O(n)
5. **Weighted Alias Tables** - O(1) zombie type selection
6. **Async Database Operations** - CompletableFuture pattern
7. **Player Move Event Optimization** - Only checking on block change

---

## Priority Recommendations

### Immediate (Before Production)
1. Increase DynamicBossBarManager update interval
2. Pre-cache talent effect player sets in OccultisteTalentListener
3. Reduce `getNearbyEntities()` calls in zombie AI

### Short-term
4. Build reverse leaderboard index
5. Batch zombie AI ticks across server ticks
6. Combine pet manager tasks

### Long-term
7. Implement spatial partitioning for entity queries
8. Consider async spawn location computation
9. Profile with 200 concurrent connections to identify bottlenecks

---

## Testing Recommendations

1. **Load Test**: Use a bot client to simulate 200 players
2. **Profiler**: Run spark profiler during peak combat
3. **GC Analysis**: Monitor garbage collection with `-Xlog:gc*`
4. **TPS Monitoring**: Track TPS degradation as player count increases

---

*Analysis performed on: 2025-12-18*
*Codebase version: 1.0.0-SNAPSHOT*
