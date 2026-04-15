package org.bruneel.notifier.client.detect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TargetRegistry {
	private final List<DetectionTarget> targets = new ArrayList<>();

	public synchronized List<DetectionTarget> allTargets() {
		return Collections.unmodifiableList(new ArrayList<>(targets));
	}

	public synchronized List<DetectionTarget> enabledTargets() {
		return targets.stream().filter(DetectionTarget::enabled).toList();
	}

	public synchronized DetectionTarget find(DetectionKind kind, String id) {
		return targets.stream()
			.filter(target -> target.kind() == kind && target.id().toString().equals(id))
			.findFirst()
			.orElse(null);
	}

	public synchronized void upsert(DetectionTarget target) {
		Objects.requireNonNull(target, "target");
		for (int i = 0; i < targets.size(); i++) {
			DetectionTarget existing = targets.get(i);
			if (existing.kind() == target.kind() && existing.id().equals(target.id())) {
				targets.set(i, target);
				return;
			}
		}
		targets.add(target);
	}
}
