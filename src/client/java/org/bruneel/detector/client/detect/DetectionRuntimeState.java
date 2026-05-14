package org.bruneel.detector.client.detect;

import java.util.HashMap;
import java.util.Map;

public final class DetectionRuntimeState {
	private final Map<String, Integer> messageCooldownByKey = new HashMap<>();
	private final Map<String, Integer> highlightCooldownByKey = new HashMap<>();
	private final Map<String, Integer> intervalCounterByKey = new HashMap<>();

	public int tickMessageCooldown(String key) {
		int next = Math.max(0, messageCooldownByKey.getOrDefault(key, 0) - 1);
		messageCooldownByKey.put(key, next);
		return next;
	}

	public void setMessageCooldown(String key, int value) {
		messageCooldownByKey.put(key, Math.max(0, value));
	}

	public int tickHighlightCooldown(String key) {
		int next = Math.max(0, highlightCooldownByKey.getOrDefault(key, 0) - 1);
		highlightCooldownByKey.put(key, next);
		return next;
	}

	public void setHighlightCooldown(String key, int value) {
		highlightCooldownByKey.put(key, Math.max(0, value));
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
