package org.bruneel.notifier.client.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.bruneel.notifier.NotifierMod;
import org.bruneel.notifier.client.detect.DetectionKind;
import org.bruneel.notifier.client.detect.DetectionScanService;
import org.bruneel.notifier.client.detect.DetectionTarget;
import org.bruneel.notifier.client.detect.NotifierConfigStore;
import org.bruneel.notifier.client.detect.ScanHighlightState;
import org.bruneel.notifier.client.detect.TargetRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public final class NotifierClientCommands {
	private static final List<Identifier> PRECIOUS_ORE_IDS = List.of(
		Identifier.of("minecraft", "diamond_ore"),
		Identifier.of("minecraft", "ancient_debris")
	);
	private static final List<Identifier> ESSENTIAL_ORE_IDS = List.of(
		Identifier.of("minecraft", "diamond_ore"),
		Identifier.of("minecraft", "ancient_debris"),
		Identifier.of("minecraft", "iron_ore"),
		Identifier.of("minecraft", "gold_ore"),
		Identifier.of("minecraft", "redstone_ore"),
		Identifier.of("minecraft", "lapis_ore")
	);

	private NotifierClientCommands() {
	}

	@SuppressWarnings("null")
	public static void register(
		TargetRegistry registry,
		NotifierConfigStore configStore,
		BooleanSupplier verboseLoggingSupplier,
		AtomicBoolean highlightOnMatchRef,
		ScanHighlightState highlightState
	) {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var detectLiteral = ClientCommandManager.literal("detect");

			detectLiteral.then(ClientCommandManager.literal("list").executes(ctx -> {
				var source = ctx.getSource();
				for (DetectionTarget target : registry.allTargets()) {
					source.sendFeedback(text(
						target.kind() + " " + target.id() + " enabled=" + target.enabled()
							+ " radius=" + target.radius()
							+ " interval=" + target.checkIntervalTicks()
							+ " cooldown=" + target.cooldownTicks()
					));
				}
				return 1;
			}));

			detectLiteral.then(ClientCommandManager.literal("scan").executes(ctx -> {
				var source = ctx.getSource();
				var client = source.getClient();
				if (client.player == null || client.world == null) {
					source.sendError(text("Client player/world is not ready yet."));
					return 0;
				}

				var enabledTargets = registry.enabledTargets();
				NotifierMod.LOGGER.info("Detect scan requested enabledTargets={}", enabledTargets.size());

				DetectionScanService.ScanExecution execution = DetectionScanService.run(
					client.world,
					client.player,
					enabledTargets
				);
				ScanHighlightState.HighlightBatchResult highlighted = highlightState.replaceWithScanResults(
					execution.hits(),
					client.world.getTime()
				);

				for (String line : execution.report().chatLines()) {
					client.player.sendMessage(text(line), false);
				}

				NotifierMod.LOGGER.info(
					"Detect scan finished enabledTargets={}, totalMatches={}, shownMatches={}, truncatedMatches={}, invalidTargets={}",
					execution.enabledTargets(),
					execution.report().totalMatches(),
					execution.report().shownMatches(),
					execution.report().truncatedMatches(),
					execution.report().invalidTargets()
				);
				NotifierMod.LOGGER.info(
					"Detect highlights updated entities={}, blocks={}, total={}, durationTicks={}",
					highlighted.entities(),
					highlighted.blocks(),
					highlighted.total(),
					highlighted.ttlTicks()
				);
				return 1;
			}));

			detectLiteral.then(ClientCommandManager.literal("precious_ores")
				.executes(ctx -> applyPresetCommand(
					ctx.getSource(),
					registry,
					configStore,
					verboseLoggingSupplier,
					highlightOnMatchRef,
					"precious_ores",
					PRECIOUS_ORE_IDS,
					true
				))
				.then(ClientCommandManager.argument("enabled", boolArg()).executes(ctx -> applyPresetCommand(
					ctx.getSource(),
					registry,
					configStore,
					verboseLoggingSupplier,
					highlightOnMatchRef,
					"precious_ores",
					PRECIOUS_ORE_IDS,
					BoolArgumentType.getBool(ctx, "enabled")
				))));

			detectLiteral.then(ClientCommandManager.literal("essential_ores")
				.executes(ctx -> applyPresetCommand(
					ctx.getSource(),
					registry,
					configStore,
					verboseLoggingSupplier,
					highlightOnMatchRef,
					"essential_ores",
					ESSENTIAL_ORE_IDS,
					true
				))
				.then(ClientCommandManager.argument("enabled", boolArg()).executes(ctx -> applyPresetCommand(
					ctx.getSource(),
					registry,
					configStore,
					verboseLoggingSupplier,
					highlightOnMatchRef,
					"essential_ores",
					ESSENTIAL_ORE_IDS,
					BoolArgumentType.getBool(ctx, "enabled")
				))));

			detectLiteral.then(ClientCommandManager.literal("all_ores")
				.executes(ctx -> applyPresetCommand(
					ctx.getSource(),
					registry,
					configStore,
					verboseLoggingSupplier,
					highlightOnMatchRef,
					"all_ores",
					allOreIdsFromRegistry(Registries.BLOCK.getIds()),
					true
				))
				.then(ClientCommandManager.argument("enabled", boolArg()).executes(ctx -> applyPresetCommand(
					ctx.getSource(),
					registry,
					configStore,
					verboseLoggingSupplier,
					highlightOnMatchRef,
					"all_ores",
					allOreIdsFromRegistry(Registries.BLOCK.getIds()),
					BoolArgumentType.getBool(ctx, "enabled")
				))));

			detectLiteral.then(ClientCommandManager.literal("highlightOnMatch")
				.then(ClientCommandManager.argument("value", boolArg()).executes(ctx -> {
					boolean value = BoolArgumentType.getBool(ctx, "value");
					highlightOnMatchRef.set(value);
					configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());
					ctx.getSource().sendFeedback(text("notifier: highlightOnMatch=" + value));
					NotifierMod.LOGGER.info("Command detect highlightOnMatch updated value={}", value);
					return 1;
				})));

			detectLiteral.then(kindLiteral(DetectionKind.ENTITY)
				.then(idArgWithSuggestions(DetectionKind.ENTITY)
					.then(ClientCommandManager.argument("enabled", boolArg()).executes(ctx -> {
						Identifier id = ctx.getArgument("id", Identifier.class);
						boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
						DetectionKind kind = DetectionKind.ENTITY;

						DetectionTarget existing = registry.find(kind, id.toString());
						DetectionTarget next = existing != null
							? existing.withEnabled(enabled)
							: defaultTarget(kind, id, enabled);

						registry.upsert(next);
						configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());

						ctx.getSource().sendFeedback(text(
							"notifier: " + kind.name().toLowerCase() + " " + id + " enabled=" + enabled
						));
						NotifierMod.LOGGER.info("Command detect update kind={}, id={}, enabled={}", kind, id, enabled);
						return 1;
					}))));

			detectLiteral.then(kindLiteral(DetectionKind.BLOCK)
				.then(idArgWithSuggestions(DetectionKind.BLOCK)
					.then(ClientCommandManager.argument("enabled", boolArg()).executes(ctx -> {
						Identifier id = ctx.getArgument("id", Identifier.class);
						boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
						DetectionKind kind = DetectionKind.BLOCK;

						DetectionTarget existing = registry.find(kind, id.toString());
						DetectionTarget next = existing != null
							? existing.withEnabled(enabled)
							: defaultTarget(kind, id, enabled);

						registry.upsert(next);
						configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());

						ctx.getSource().sendFeedback(text(
							"notifier: " + kind.name().toLowerCase() + " " + id + " enabled=" + enabled
						));
						NotifierMod.LOGGER.info("Command detect update kind={}, id={}, enabled={}", kind, id, enabled);
						return 1;
					}))));

			detectLiteral.then(ClientCommandManager.literal("radius")
				.then(kindLiteral(DetectionKind.ENTITY)
					.then(idArgWithSuggestions(DetectionKind.ENTITY)
						.then(ClientCommandManager.argument("value", doubleArg(1.0, 64.0))
							.executes(ctx -> {
								DetectionTarget target = requireTarget(ctx, registry, DetectionKind.ENTITY);
								if (target == null) {
									return 0;
								}

								double value = DoubleArgumentType.getDouble(ctx, "value");
								DetectionTarget updated = target.withRadius(value);
								registry.upsert(updated);
								configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());

								ctx.getSource().sendFeedback(text("notifier: radius updated for " + target.id() + " -> " + value));
								NotifierMod.LOGGER.info("Command detect radius update kind={}, id={}, value={}", target.kind(), target.id(), value);
								return 1;
							}))))
				.then(kindLiteral(DetectionKind.BLOCK)
					.then(idArgWithSuggestions(DetectionKind.BLOCK)
						.then(ClientCommandManager.argument("value", doubleArg(1.0, 64.0))
							.executes(ctx -> {
								DetectionTarget target = requireTarget(ctx, registry, DetectionKind.BLOCK);
								if (target == null) {
									return 0;
								}

								double value = DoubleArgumentType.getDouble(ctx, "value");
								DetectionTarget updated = target.withRadius(value);
								registry.upsert(updated);
								configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());

								ctx.getSource().sendFeedback(text("notifier: radius updated for " + target.id() + " -> " + value));
								NotifierMod.LOGGER.info("Command detect radius update kind={}, id={}, value={}", target.kind(), target.id(), value);
								return 1;
							})))));

			detectLiteral.then(ClientCommandManager.literal("interval")
				.then(kindLiteral(DetectionKind.ENTITY)
					.then(idArgWithSuggestions(DetectionKind.ENTITY)
						.then(ClientCommandManager.argument("value", intArg(1, 1200))
							.executes(ctx -> {
								DetectionTarget target = requireTarget(ctx, registry, DetectionKind.ENTITY);
								if (target == null) {
									return 0;
								}

								int value = IntegerArgumentType.getInteger(ctx, "value");
								DetectionTarget updated = target.withCheckIntervalTicks(value);
								registry.upsert(updated);
								configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());

								ctx.getSource().sendFeedback(text("notifier: interval updated for " + target.id() + " -> " + value));
								NotifierMod.LOGGER.info("Command detect interval update kind={}, id={}, value={}", target.kind(), target.id(), value);
								return 1;
							}))))
				.then(kindLiteral(DetectionKind.BLOCK)
					.then(idArgWithSuggestions(DetectionKind.BLOCK)
						.then(ClientCommandManager.argument("value", intArg(1, 1200))
							.executes(ctx -> {
								DetectionTarget target = requireTarget(ctx, registry, DetectionKind.BLOCK);
								if (target == null) {
									return 0;
								}

								int value = IntegerArgumentType.getInteger(ctx, "value");
								DetectionTarget updated = target.withCheckIntervalTicks(value);
								registry.upsert(updated);
								configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());

								ctx.getSource().sendFeedback(text("notifier: interval updated for " + target.id() + " -> " + value));
								NotifierMod.LOGGER.info("Command detect interval update kind={}, id={}, value={}", target.kind(), target.id(), value);
								return 1;
							})))));

			detectLiteral.then(ClientCommandManager.literal("cooldown")
				.then(kindLiteral(DetectionKind.ENTITY)
					.then(idArgWithSuggestions(DetectionKind.ENTITY)
						.then(ClientCommandManager.argument("value", intArg(0, 72000))
							.executes(ctx -> {
								DetectionTarget target = requireTarget(ctx, registry, DetectionKind.ENTITY);
								if (target == null) {
									return 0;
								}

								int value = IntegerArgumentType.getInteger(ctx, "value");
								DetectionTarget updated = target.withCooldownTicks(value);
								registry.upsert(updated);
								configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());

								ctx.getSource().sendFeedback(text("notifier: cooldown updated for " + target.id() + " -> " + value));
								NotifierMod.LOGGER.info("Command detect cooldown update kind={}, id={}, value={}", target.kind(), target.id(), value);
								return 1;
							}))))
				.then(kindLiteral(DetectionKind.BLOCK)
					.then(idArgWithSuggestions(DetectionKind.BLOCK)
						.then(ClientCommandManager.argument("value", intArg(0, 72000))
							.executes(ctx -> {
								DetectionTarget target = requireTarget(ctx, registry, DetectionKind.BLOCK);
								if (target == null) {
									return 0;
								}

								int value = IntegerArgumentType.getInteger(ctx, "value");
								DetectionTarget updated = target.withCooldownTicks(value);
								registry.upsert(updated);
								configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());

								ctx.getSource().sendFeedback(text("notifier: cooldown updated for " + target.id() + " -> " + value));
								NotifierMod.LOGGER.info("Command detect cooldown update kind={}, id={}, value={}", target.kind(), target.id(), value);
								return 1;
							})))));

			dispatcher.register(ClientCommandManager.literal("notifier").then(detectLiteral));
		});
	}

	private static DetectionTarget defaultTarget(DetectionKind kind, Identifier id, boolean enabled) {
		if (kind == DetectionKind.BLOCK) {
			return new DetectionTarget(
				kind,
				id,
				enabled,
				8.0,
				30,
				120,
				"Detected block nearby: " + id
			);
		}

		return new DetectionTarget(
			kind,
			id,
			enabled,
			16.0,
			10,
			100,
			"Detected entity nearby: " + id
		);
	}

	@SuppressWarnings("null")
	private static DetectionTarget requireTarget(
		com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx,
		TargetRegistry registry,
		DetectionKind kind
	) {
		Identifier id = ctx.getArgument("id", Identifier.class);

		DetectionTarget target = registry.find(kind, id.toString());
		if (target == null) {
			ctx.getSource().sendError(text("Target not configured. Add it first, e.g. /notifier detect "
				+ kind.name().toLowerCase() + " " + id + " true"));
			NotifierMod.LOGGER.warn("Cannot tune unknown target kind={}, id={}", kind, id);
			return null;
		}
		return target;
	}

	private static Text text(String value) {
		return Objects.requireNonNull(Text.literal(value));
	}

	private static int applyPresetCommand(
		FabricClientCommandSource source,
		TargetRegistry registry,
		NotifierConfigStore configStore,
		BooleanSupplier verboseLoggingSupplier,
		AtomicBoolean highlightOnMatchRef,
		String presetName,
		Iterable<Identifier> ids,
		boolean enabled
	) {
		return applyPreset(
			source,
			registry,
			configStore,
			verboseLoggingSupplier,
			highlightOnMatchRef,
			presetName,
			ids,
			enabled
		);
	}

	private static int applyPreset(
		FabricClientCommandSource source,
		TargetRegistry registry,
		NotifierConfigStore configStore,
		BooleanSupplier verboseLoggingSupplier,
		AtomicBoolean highlightOnMatchRef,
		String presetName,
		Iterable<Identifier> ids,
		boolean enabled
	) {
		int added = 0;
		int changed = 0;
		for (Identifier id : ids) {
			DetectionTarget existing = registry.find(DetectionKind.BLOCK, id.toString());
			DetectionTarget next;
			if (existing == null) {
				if (!enabled) {
					continue;
				}
				next = defaultTarget(DetectionKind.BLOCK, id, true);
				added++;
				changed++;
			} else {
				next = existing.withEnabled(enabled);
				if (existing.enabled() != enabled) {
					changed++;
				}
			}
			registry.upsert(next);
		}

		configStore.save(registry, verboseLoggingSupplier.getAsBoolean(), highlightOnMatchRef.get());
		source.sendFeedback(text(
			"notifier: preset " + presetName + " applied; value=" + enabled + " changed=" + changed + " added=" + added
		));
		NotifierMod.LOGGER.info(
			"Command detect preset applied name={}, value={}, changed={}, added={}",
			presetName,
			enabled,
			changed,
			added
		);
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestEntityIds(
		com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx,
		SuggestionsBuilder builder
	) {
		return CommandSource.suggestIdentifiers(suggestedIdsForKind(DetectionKind.ENTITY), builder);
	}

	private static CompletableFuture<Suggestions> suggestBlockIds(
		com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx,
		SuggestionsBuilder builder
	) {
		return CommandSource.suggestIdentifiers(suggestedIdsForKind(DetectionKind.BLOCK), builder);
	}

	static Iterable<Identifier> suggestedIdsForKind(DetectionKind kind) {
		return suggestedIdsForKind(kind, Registries.ENTITY_TYPE.getIds(), Registries.BLOCK.getIds());
	}

	static List<Identifier> allOreIdsFromRegistry(Iterable<Identifier> blockIds) {
		List<Identifier> ores = new ArrayList<>();
		for (Identifier id : blockIds) {
			if (!"minecraft".equals(id.getNamespace())) {
				continue;
			}
			String path = id.getPath();
			if (path.endsWith("_ore") || "ancient_debris".equals(path)) {
				ores.add(id);
			}
		}
		ores.sort(Comparator.comparing(Identifier::toString));
		return List.copyOf(ores);
	}

	static Iterable<Identifier> suggestedIdsForKind(
		DetectionKind kind,
		Iterable<Identifier> entityIds,
		Iterable<Identifier> blockIds
	) {
		if (kind == DetectionKind.ENTITY) {
			return entityIds;
		}
		if (kind == DetectionKind.BLOCK) {
			return blockIds;
		}
		return java.util.List.of();
	}

	private static RequiredArgumentBuilder<FabricClientCommandSource, Identifier> idArgWithSuggestions(DetectionKind kind) {
		RequiredArgumentBuilder<FabricClientCommandSource, Identifier> builder =
			ClientCommandManager.argument("id", identifierArg());
		if (kind == DetectionKind.ENTITY) {
			return builder.suggests(NotifierClientCommands::suggestEntityIds);
		}
		return builder.suggests(NotifierClientCommands::suggestBlockIds);
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> kindLiteral(DetectionKind kind) {
		return ClientCommandManager.literal(kind.name().toLowerCase());
	}

	private static IdentifierArgumentType identifierArg() {
		return Objects.requireNonNull(IdentifierArgumentType.identifier());
	}

	private static BoolArgumentType boolArg() {
		return Objects.requireNonNull(BoolArgumentType.bool());
	}

	private static DoubleArgumentType doubleArg(double min, double max) {
		return Objects.requireNonNull(DoubleArgumentType.doubleArg(min, max));
	}

	private static IntegerArgumentType intArg(int min, int max) {
		return Objects.requireNonNull(IntegerArgumentType.integer(min, max));
	}
}
