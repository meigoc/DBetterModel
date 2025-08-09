package meigo.dbettermodel.services;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ListTag;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.event.CloseTrackerEvent;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import meigo.dbettermodel.denizen.objects.BMBoneTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ModelService implements Listener {

    private static final ModelService INSTANCE = new ModelService();
    private final Map<String, BoneController> boneControllerCache = new ConcurrentHashMap<>();
    private final BoneMechanismHandler mechanismHandler = new BoneMechanismHandler();

    private ModelService() {}

    public static ModelService getInstance() {
        return INSTANCE;
    }

    public void initialize(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        boneControllerCache.clear();
    }

    @EventHandler
    public void onTrackerClose(CloseTrackerEvent event) {
        Tracker tracker = event.getTracker();
        if (tracker instanceof EntityTracker entityTracker) {
            String prefix = entityTracker.registry().uuid().toString() + "," + entityTracker.name() + ",";
            boneControllerCache.keySet().removeIf(key -> key.startsWith(prefix));
        }
    }

    private Optional<BoneController> getBoneController(UUID entityUUID, String modelId, String boneId) {
        return BetterModel.registry(entityUUID)
                .flatMap(registry -> Optional.ofNullable(registry.tracker(modelId)))
                .flatMap(tracker -> {
                    return Optional.ofNullable(tracker.bone(boneId))
                            .map(bone -> {
                                String key = entityUUID.toString() + "," + modelId + "," + boneId;
                                return boneControllerCache.computeIfAbsent(key, k -> new BoneController(tracker, bone));
                            });
                });
    }

    // --- Tag Getters ---
    public Location getBoneWorldLocation(UUID entityUUID, String modelId, String boneId) {
        return getBoneController(entityUUID, modelId, boneId)
                .map(BoneController::getWorldLocation)
                .orElse(null);
    }

    public Vector3f getBoneWorldRotationEuler(UUID entityUUID, String modelId, String boneId) {
        return getBoneController(entityUUID, modelId, boneId)
                .map(BoneController::getWorldRotationEuler)
                .orElse(null);
    }

    public boolean isBoneVisible(UUID entityUUID, String modelId, String boneId) {
        return getBoneController(entityUUID, modelId, boneId)
                .map(BoneController::isVisible)
                .orElse(false);
    }

    // --- Mechanism Handlers ---
    public void adjustBone(BMBoneTag boneTag, Mechanism mechanism) {
        getBoneController(boneTag.getEntityUUID(), boneTag.getModelName(), boneTag.getBoneName())
                .ifPresent(controller -> mechanismHandler.handle(controller, mechanism));
    }

    // --- Command Logic ---
    public void mountEntity(Entity entityToMount, BMBoneTag boneTag) {
        getBoneController(boneTag.getEntityUUID(), boneTag.getModelName(), boneTag.getBoneName())
                .ifPresent(controller -> controller.mount(entityToMount));
    }

    public void dismountEntity(Entity entityToDismount, BMBoneTag boneTag) {
        getBoneController(boneTag.getEntityUUID(), boneTag.getModelName(), boneTag.getBoneName())
                .ifPresent(controller -> controller.dismount(entityToDismount));
    }

    public void dismountAll(BMBoneTag boneTag) {
        getBoneController(boneTag.getEntityUUID(), boneTag.getModelName(), boneTag.getBoneName())
                .ifPresent(BoneController::dismountAll);
    }

    public void playAnimationForPlayers(EntityTracker tracker, String animation, AnimationModifier modifier, ListTag players) {
        if (players == null || players.isEmpty()) {
            tracker.animate(bone -> true, animation, modifier, () -> {});
            return;
        }

        Consumer<Player> playForPlayer = player -> {
            AnimationModifier perPlayerModifier = AnimationModifier.builder()
                    .start(modifier.start())
                    .end(modifier.end())
                    .type(modifier.type())
                    .speed(modifier.speed())
                    .override(modifier.override())
                    .player(player)
                    .build();
            tracker.animate(bone -> true, animation, perPlayerModifier, () -> {});
        };

        players.filter(PlayerTag.class, DenizenCore.implementation.getTagContext((com.denizenscript.denizencore.scripts.ScriptEntry) null))
                .forEach(p -> playForPlayer.accept(p.getPlayerEntity()));
    }
}