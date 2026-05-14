package org.bruneel.detector.client.detect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.util.Identifier;
import org.bruneel.detector.DetectorMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class DetectorConfigStore {
	private static final String CONFIG_FILE_NAME = "detector-client.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configPath;

	public DetectorConfigStore(Path configDir) {
		this.configPath = configDir.resolve(CONFIG_FILE_NAME);
	}

	public LoadResult loadOrDefault() {
		if (!Files.exists(configPath)) {
			return defaults();
		}

		try (Reader reader = Files.newBufferedReader(configPath)) {
			ConfigFile parsed = GSON.fromJson(reader, ConfigFile.class);
			return fromConfigFile(parsed);
		} catch (IOException | JsonParseException ex) {
			DetectorMod.LOGGER.warn("Detector config failed to load from {}; using defaults", configPath, ex);
			return defaults();
		}
	}

	public void save(TargetRegistry registry, boolean verboseLogging, boolean highlightOnMatch) {
		ConfigFile data = toConfigFile(registry, verboseLogging, highlightOnMatch);
		Path tmpPath = configPath.resolveSibling(CONFIG_FILE_NAME + ".tmp");
		try {
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(tmpPath)) {
				GSON.toJson(data, writer);
			}
			Files.move(tmpPath, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException ex) {
			DetectorMod.LOGGER.error("Failed to save detector config at {}", configPath, ex);
		}
	}

	private LoadResult fromConfigFile(ConfigFile file) {
		TargetRegistry registry = new TargetRegistry();
		int prunedDisabledTargets = 0;
		if (file.targets != null) {
			for (TargetEntry entry : file.targets) {
				DetectionTarget target = entry.toTarget();
				if (target == null) {
					DetectorMod.LOGGER.warn("Skipping invalid detector target entry in {}", configPath);
					continue;
				}
				if (!target.enabled()) {
					prunedDisabledTargets++;
					continue;
				}
				registry.upsert(target);
			}
		}
		if (prunedDisabledTargets > 0) {
			DetectorMod.LOGGER.info(
				"Pruned {} disabled detector targets while loading {}",
				prunedDisabledTargets,
				configPath
			);
		}
		if (registry.allTargets().isEmpty()) {
			DetectorMod.LOGGER.warn("Detector config {} had no valid targets; using defaults", configPath);
			return defaults();
		}
		return new LoadResult(registry, file.verboseLogging, file.highlightOnMatch);
	}

	private static ConfigFile toConfigFile(TargetRegistry registry, boolean verboseLogging, boolean highlightOnMatch) {
		ConfigFile file = new ConfigFile();
		file.verboseLogging = verboseLogging;
		file.highlightOnMatch = highlightOnMatch;
		file.targets = registry.allTargets().stream().map(TargetEntry::fromTarget).toList();
		return file;
	}

	private static LoadResult defaults() {
		TargetRegistry registry = new TargetRegistry();
		registry.upsert(new DetectionTarget(
			DetectionKind.ENTITY,
			Identifier.of("minecraft", "chicken"),
			true,
			16.0,
			10,
			100,
			20,
			"A chicken is in the neighbourhood!"
		));
		return new LoadResult(registry, false, true);
	}

	public record LoadResult(TargetRegistry registry, boolean verboseLogging, boolean highlightOnMatch) {
	}

	private static final class ConfigFile {
		boolean verboseLogging = false;
		boolean highlightOnMatch = true;
		List<TargetEntry> targets = new ArrayList<>();
	}

	private static final class TargetEntry {
		String kind;
		String id;
		boolean enabled;
		double radius;
		int checkIntervalTicks;
		Integer cooldownTicks;
		Integer messageCooldownTicks;
		Integer highlightCooldownTicks;
		String messageTemplate;

		static TargetEntry fromTarget(DetectionTarget target) {
			TargetEntry entry = new TargetEntry();
			entry.kind = target.kind().name();
			entry.id = target.id().toString();
			entry.enabled = target.enabled();
			entry.radius = target.radius();
			entry.checkIntervalTicks = target.checkIntervalTicks();
			entry.messageCooldownTicks = target.messageCooldownTicks();
			entry.highlightCooldownTicks = target.highlightCooldownTicks();
			entry.messageTemplate = target.messageTemplate();
			return entry;
		}

		DetectionTarget toTarget() {
			try {
				DetectionKind parsedKind = DetectionKind.valueOf(kind);
				Identifier parsedId = Identifier.of(id);
				double safeRadius = radius > 0 ? radius : 16.0;
				int safeInterval = Math.max(1, checkIntervalTicks);
				int fallbackCooldown = Math.max(0, cooldownTicks != null ? cooldownTicks : 100);
				int safeHighlightCooldown = Math.max(
					0,
					highlightCooldownTicks != null ? highlightCooldownTicks : fallbackCooldown
				);
				int safeMessageCooldown = Math.max(
					0,
					messageCooldownTicks != null ? messageCooldownTicks : Math.max(safeHighlightCooldown * 3, 100)
				);
				String safeMessage = messageTemplate != null && !messageTemplate.isBlank()
					? messageTemplate
					: "Detected " + parsedId;
				return new DetectionTarget(
					parsedKind,
					parsedId,
					enabled,
					safeRadius,
					safeInterval,
					safeMessageCooldown,
					safeHighlightCooldown,
					safeMessage
				);
			} catch (RuntimeException ex) {
				return null;
			}
		}
	}
}
