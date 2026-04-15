package org.bruneel.notifier.client.detect;

import java.util.HashMap;
import java.util.Map;

public final class DetectionRuntimeState {
	private final Map<String, Boolean> wasNearbyByKey = new HashMap<>();
	private final Map<String, Integer> cooldownByKey = new HashMap<>();
	private final Map<String, Integer> intervalCounterByKey = new HashMap<>();

	public boolean wasNearby(String key) {
		return wasNearbyByKey.getOrDefault(key, false);
	}

	public void setWasNearby(String key, boolean value) {
		wasNearbyByKey.put(key, value);
	}

	public int tickCooldown(String key) {
		int next = Math.max(0, cooldownByKey.getOrDefault(key, 0) - 1);
		cooldownByKey.put(key, next);
		return next;
	}

	public void setCooldown(String key, int value) {
		cooldownByKey.put(key, Math.max(0, value));
	}

	public int nextIntervalCount(String key) {
		int next = intervalCounterByKey.getOrDefault(key, 0) + 1;
		intervalCounterByKey.put(key, next);
		return next;
	}

	public void resetIntervalCount(String key) {
		intervalCounterByKey.put(key, 0);
	}
}
