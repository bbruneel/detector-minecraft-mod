package org.bruneel.notifier.client.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.bruneel.notifier.NotifierMod;
import org.bruneel.notifier.client.detect.DetectionKind;
import org.bruneel.notifier.client.detect.DetectionTarget;
import org.bruneel.notifier.client.detect.NotifierConfigStore;
import org.bruneel.notifier.client.detect.TargetRegistry;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class NotifierClientCommands {
	private NotifierClientCommands() {
	}

	@SuppressWarnings("null")
	public static void register(
		TargetRegistry registry,
		NotifierConfigStore configStore,
		BooleanSupplier verboseLoggingSupplier
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

			detectLiteral.then(ClientCommandManager.argument("kind", wordArg())
				.then(ClientCommandManager.argument("id", stringArg())
					.then(ClientCommandManager.argument("enabled", boolArg()).executes(ctx -> {
						String kindRaw = StringArgumentType.getString(ctx, "kind");
						String idRaw = StringArgumentType.getString(ctx, "id");
						boolean enabled = BoolArgumentType.getBool(ctx, "enabled");

						DetectionKind kind;
						Identifier id;
						try {
							kind = DetectionCommandParser.parseKind(kindRaw);
							id = DetectionCommandParser.parseIdentifier(idRaw);
						} catch (RuntimeException ex) {
							ctx.getSource().sendError(text("Invalid kind or id. Example: /notifier detect entity minecraft:horse true"));
							NotifierMod.LOGGER.warn("Rejecting detect command kind='{}' id='{}'", kindRaw, idRaw);
							return 0;
						}

						DetectionTarget existing = registry.find(kind, id.toString());
						DetectionTarget next = existing != null
							? existing.withEnabled(enabled)
							: defaultTarget(kind, id, enabled);

						registry.upsert(next);
						configStore.save(registry, verboseLoggingSupplier.getAsBoolean());

						ctx.getSource().sendFeedback(text(
							"notifier: " + kind.name().toLowerCase() + " " + id + " enabled=" + enabled
						));
						NotifierMod.LOGGER.info("Command detect update kind={}, id={}, enabled={}", kind, id, enabled);
						return 1;
					}))));

			detectLiteral.then(ClientCommandManager.literal("radius")
				.then(ClientCommandManager.argument("kind", wordArg())
					.then(ClientCommandManager.argument("id", stringArg())
						.then(ClientCommandManager.argument("value", doubleArg(1.0, 64.0))
							.executes(ctx -> {
								DetectionTarget target = requireTarget(ctx, registry);
								if (target == null) {
									return 0;
								}

								double value = DoubleArgumentType.getDouble(ctx, "value");
								DetectionTarget updated = target.withRadius(value);
								registry.upsert(updated);
								configStore.save(registry, verboseLoggingSupplier.getAsBoolean());

								ctx.getSource().sendFeedback(text("notifier: radius updated for " + target.id() + " -> " + value));
								NotifierMod.LOGGER.info("Command detect radius update kind={}, id={}, value={}", target.kind(), target.id(), value);
								return 1;
							})))));

			detectLiteral.then(ClientCommandManager.literal("interval")
				.then(ClientCommandManager.argument("kind", wordArg())
					.then(ClientCommandManager.argument("id", stringArg())
						.then(ClientCommandManager.argument("value", intArg(1, 1200))
							.executes(ctx -> {
								DetectionTarget target = requireTarget(ctx, registry);
								if (target == null) {
									return 0;
								}

								int value = IntegerArgumentType.getInteger(ctx, "value");
								DetectionTarget updated = target.withCheckIntervalTicks(value);
								registry.upsert(updated);
								configStore.save(registry, verboseLoggingSupplier.getAsBoolean());

								ctx.getSource().sendFeedback(text("notifier: interval updated for " + target.id() + " -> " + value));
								NotifierMod.LOGGER.info("Command detect interval update kind={}, id={}, value={}", target.kind(), target.id(), value);
								return 1;
							})))));

			detectLiteral.then(ClientCommandManager.literal("cooldown")
				.then(ClientCommandManager.argument("kind", wordArg())
					.then(ClientCommandManager.argument("id", stringArg())
						.then(ClientCommandManager.argument("value", intArg(0, 72000))
							.executes(ctx -> {
								DetectionTarget target = requireTarget(ctx, registry);
								if (target == null) {
									return 0;
								}

								int value = IntegerArgumentType.getInteger(ctx, "value");
								DetectionTarget updated = target.withCooldownTicks(value);
								registry.upsert(updated);
								configStore.save(registry, verboseLoggingSupplier.getAsBoolean());

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
		com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> ctx,
		TargetRegistry registry
	) {
		String kindRaw = StringArgumentType.getString(ctx, "kind");
		String idRaw = StringArgumentType.getString(ctx, "id");

		DetectionKind kind;
		Identifier id;
		try {
			kind = DetectionCommandParser.parseKind(kindRaw);
			id = DetectionCommandParser.parseIdentifier(idRaw);
		} catch (RuntimeException ex) {
			ctx.getSource().sendError(text("Invalid kind or id. Example: /notifier detect radius entity minecraft:horse 20"));
			NotifierMod.LOGGER.warn("Rejecting detect tune command kind='{}' id='{}'", kindRaw, idRaw);
			return null;
		}

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

	private static StringArgumentType wordArg() {
		return Objects.requireNonNull(StringArgumentType.word());
	}

	private static StringArgumentType stringArg() {
		return Objects.requireNonNull(StringArgumentType.string());
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
