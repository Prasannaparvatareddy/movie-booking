package com.booking.controller;

import com.booking.dto.ApiResponse;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CacheStatsController — Exposes live cache statistics so you can
 * verify caching is working directly from Postman.
 *
 * Endpoints:
 *   GET  /api/v1/cache/stats          → hit count, miss count, hit rate for all caches
 *   GET  /api/v1/cache/stats/{name}   → stats for one specific cache
 *   DELETE /api/v1/cache/evict/{name} → manually clear a specific cache
 *   DELETE /api/v1/cache/evict/all    → manually clear ALL caches
 *
 * HOW TO PROVE CACHE IS WORKING IN POSTMAN:
 *
 *   Step 1: GET /api/v1/cache/stats
 *           → all hitCount = 0, missCount = 0
 *
 *   Step 2: GET /api/v1/movies/1          (first call)
 *   Step 3: GET /api/v1/cache/stats
 *           → movies: missCount = 1, hitCount = 0
 *
 *   Step 4: GET /api/v1/movies/1          (second call — same endpoint)
 *   Step 5: GET /api/v1/cache/stats
 *           → movies: missCount = 1, hitCount = 1, hitRate = 50%
 *
 *   Step 6: GET /api/v1/movies/1          (third call)
 *   Step 7: GET /api/v1/cache/stats
 *           → movies: missCount = 1, hitCount = 2, hitRate = 66.7%
 *
 *   The hitRate keeps rising — PROOF that cache is working!
 */
@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
public class CacheStatsController {

    private final CacheManager cacheManager;

    /**
     * GET /api/v1/cache/stats
     * Returns hit count, miss count, eviction count, and hit rate for every cache.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllCacheStats() {
        Map<String, Object> allStats = new LinkedHashMap<>();

        cacheManager.getCacheNames().forEach(cacheName -> {
            CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (caffeineCache != null) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                com.github.benmanes.caffeine.cache.stats.CacheStats stats = nativeCache.stats();

                Map<String, Object> cacheStats = new LinkedHashMap<>();
                cacheStats.put("estimatedSize",    nativeCache.estimatedSize());
                cacheStats.put("hitCount",         stats.hitCount());
                cacheStats.put("missCount",        stats.missCount());
                cacheStats.put("hitRate",          String.format("%.1f%%", stats.hitRate() * 100));
                cacheStats.put("evictionCount",    stats.evictionCount());
                cacheStats.put("requestCount",     stats.requestCount());

                allStats.put(cacheName, cacheStats);
            }
        });

        return ResponseEntity.ok(ApiResponse.success("Cache statistics", allStats));
    }

    /**
     * GET /api/v1/cache/stats/{cacheName}
     * Returns stats for one specific cache.
     * Example: GET /api/v1/cache/stats/movies
     */
    @GetMapping("/stats/{cacheName}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats(
            @PathVariable String cacheName) {

        CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache(cacheName);
        if (caffeineCache == null) {
            return ResponseEntity.ok(ApiResponse.error("Cache not found: " + cacheName));
        }

        Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = nativeCache.stats();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cacheName",      cacheName);
        result.put("estimatedSize",  nativeCache.estimatedSize());
        result.put("hitCount",       stats.hitCount());
        result.put("missCount",      stats.missCount());
        result.put("hitRate",        String.format("%.1f%%", stats.hitRate() * 100));
        result.put("missRate",       String.format("%.1f%%", stats.missRate() * 100));
        result.put("evictionCount",  stats.evictionCount());
        result.put("requestCount",   stats.requestCount());
        result.put("loadSuccessCount", stats.loadSuccessCount());
        result.put("totalLoadTime",  stats.totalLoadTime() + "ns");

        return ResponseEntity.ok(ApiResponse.success("Stats for cache: " + cacheName, result));
    }

    /**
     * DELETE /api/v1/cache/evict/{cacheName}
     * Manually clears all entries in a specific cache.
     * Example: DELETE /api/v1/cache/evict/movies
     */
    @DeleteMapping("/evict/{cacheName}")
    public ResponseEntity<ApiResponse<String>> evictCache(@PathVariable String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return ResponseEntity.ok(ApiResponse.error("Cache not found: " + cacheName));
        }
        cache.clear();
        return ResponseEntity.ok(ApiResponse.success("Cache cleared: " + cacheName, "All entries evicted"));
    }

    /**
     * DELETE /api/v1/cache/evict/all
     * Manually clears ALL caches.
     */
    @DeleteMapping("/evict/all")
    public ResponseEntity<ApiResponse<String>> evictAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.clear();
        });
        return ResponseEntity.ok(ApiResponse.success(
            "All caches cleared", cacheManager.getCacheNames().toString()));
    }
}
