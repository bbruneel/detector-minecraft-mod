package org.bruneel.notifier.client.detect;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifierConfigStoreTest {
	@Test
	void loadOrDefaultWhenMissingFile_defaultsHighlightOnMatchTrueAndVerboseLoggingFalse(@TempDir Path tempDir) {
		NotifierConfigStore store = new NotifierConfigStore(tempDir);

		NotifierConfigStore.LoadResult result = store.loadOrDefault();
		assertTrue(result.highlightOnMatch(), "Expected highlightOnMatch to default to true when config is missing");
		assertFalse(result.verboseLogging(), "Expected verboseLogging to default to false when config is missing");
	}

	@Test
	void loadFromConfigFile_parsesHighlightOnMatch(@TempDir Path tempDir) throws Exception {
		String json = """
			{
			  "verboseLogging": false,
			  "highlightOnMatch": false,
			  "targets": [
			    {
			      "kind": "ENTITY",
			      "id": "minecraft:chicken",
			      "enabled": true,
			      "radius": 16.0,
			      "checkIntervalTicks": 10,
			      "cooldownTicks": 100,
			      "messageTemplate": "test"
			    }
			  ]
			}
			""";

		Files.writeString(tempDir.resolve("notifier-client.json"), json);

		NotifierConfigStore store = new NotifierConfigStore(tempDir);
		NotifierConfigStore.LoadResult result = store.loadOrDefault();

		assertFalse(result.highlightOnMatch());
		assertFalse(result.verboseLogging());
	}

	@Test
	void savePersistsHighlightOnMatch(@TempDir Path tempDir) throws Exception {
		TargetRegistry registry = new TargetRegistry();
		registry.upsert(new DetectionTarget(
			DetectionKind.ENTITY,
			Identifier.of("minecraft", "chicken"),
			true,
			16.0,
			10,
			100,
			"test"
		));

		NotifierConfigStore store = new NotifierConfigStore(tempDir);
		store.save(registry, false, false);

		NotifierConfigStore reloaded = new NotifierConfigStore(tempDir);
		NotifierConfigStore.LoadResult result = reloaded.loadOrDefault();

		assertFalse(result.highlightOnMatch());
		assertEquals(false, result.verboseLogging());
	}
}

