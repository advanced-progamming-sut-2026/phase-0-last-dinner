package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import model.Plant;
import model.chapters.ChapterType;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GameplaySoundPlayer {
    public enum Effect {
        BUTTON,
        CARD,
        SEED_SELECT,
        PLANT,
        SHOVEL,
        PLANT_FOOD,
        PLANT_FOOD_COLLECT,
        SUN_PRODUCED,
        SUN_COLLECT,
        PEA_FIRE,
        PEA_HIT,
        LOB_FIRE,
        LOB_HIT,
        FIRE_HIT,
        ICE_HIT,
        ELECTRIC,
        MELEE_PUNCH,
        MELEE_WHIP,
        CHOMPER_BITE,
        ENDURIAN_HIT,
        PHAT_BEET,
        KIWI_BEAST,
        REFLECT,
        ALL_STAR,
        CRYSTAL,
        GARGANTUAR,
        GARLIC,
        EXPLOSION,
        ZOMBIE_BITE,
        ZOMBIE_DEATH,
        ZOMBIE_GROAN_EGYPT,
        ZOMBIE_GROAN_BEACH,
        ZOMBIE_GROAN_GENERIC,
        MOWER,
        WAVE,
        FINAL_WAVE,
        COIN,
        GEM,
        WIN,
        LOSS,
        VASE_BREAK,
        BOWLING,
        MATCH,
        UPGRADE,
        UNLOCK,
        ZOMBIE_PLACE,
        BRAIN_EAT,
        MAGNET,
        MAGIC,
        FREEZE,
        GRAVE,
        SANDSTORM,
        OCTOPUS,
        FISHERMAN,
        HUNTER,
        PROSPECTOR,
        DODO,
        PIANO
    }

    private static final Map<Effect, String[]> PATHS = createPaths();
    private static final Map<Effect, Long> MIN_INTERVAL_MILLIS = createIntervals();
    private static final GameplaySoundPlayer SHARED = new GameplaySoundPlayer(
            view.GameSettings.getSoundFxVolume()
    );

    private final Map<String, Sound> sounds = new HashMap<>();
    private final Set<String> failedPaths = new HashSet<>();
    private final Map<Effect, Integer> nextVariant = new EnumMap<>(Effect.class);
    private final Map<Effect, Long> lastPlayedAt = new EnumMap<>(Effect.class);
    private float volume;

    GameplaySoundPlayer(float volume) {
        setVolume(volume);
    }

    public static GameplaySoundPlayer shared() {
        return SHARED;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
    }

    public void play(Effect effect) {
        if (effect == null || this.volume <= 0f || Gdx.audio == null || Gdx.files == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long interval = MIN_INTERVAL_MILLIS.getOrDefault(effect, 0L);
        long previous = this.lastPlayedAt.getOrDefault(effect, Long.MIN_VALUE);
        if (previous != Long.MIN_VALUE && now - previous < interval) {
            return;
        }
        String[] variants = PATHS.get(effect);
        if (variants == null || variants.length == 0) {
            return;
        }
        int index = this.nextVariant.getOrDefault(effect, 0);
        String path = variants[index % variants.length];
        Sound sound = load(path);
        if (sound == null) {
            return;
        }
        this.nextVariant.put(effect, (index + 1) % variants.length);
        this.lastPlayedAt.put(effect, now);
        sound.play(Math.min(1f, this.volume * mixGain(effect)));
    }

    private float mixGain(Effect effect) {
        return switch (effect) {
            case LOB_HIT, LOSS -> 0.50f;
            case EXPLOSION, FREEZE, PIANO -> 0.58f;
            case ZOMBIE_GROAN_EGYPT, ZOMBIE_GROAN_BEACH,
                    ZOMBIE_GROAN_GENERIC, BOWLING, PROSPECTOR,
                    ALL_STAR, GARGANTUAR -> 0.65f;
            case WAVE, ELECTRIC, OCTOPUS, FISHERMAN, HUNTER, CRYSTAL -> 0.72f;
            case FIRE_HIT, ZOMBIE_DEATH, MOWER, COIN, GEM,
                    MATCH, MAGNET, MAGIC, GRAVE, SANDSTORM, DODO,
                    REFLECT, GARLIC -> 0.80f;
            case WIN, FINAL_WAVE -> 0.90f;
            default -> 1f;
        };
    }

    void playProjectileLaunch(Projectile projectile) {
        if (projectile == null) {
            return;
        }
        String sourceName = projectile.getSourcePlant() == null
                || projectile.getSourcePlant().getName() == null
                ? ""
                : projectile.getSourcePlant().getName().toLowerCase(java.util.Locale.ROOT);
        if (sourceName.contains("electric") || sourceName.contains("laser")) {
            play(Effect.ELECTRIC);
        } else if (projectile.isLobbed() || projectile.getType() == ProjectileType.LOBBED) {
            play(Effect.LOB_FIRE);
        } else {
            play(Effect.PEA_FIRE);
        }
    }

    void playProjectileImpact(Projectile projectile) {
        if (projectile == null || projectile.getHitZombies() == null
                || projectile.getHitZombies().isEmpty()) {
            return;
        }
        String sourceName = projectile.getSourcePlant() == null
                || projectile.getSourcePlant().getName() == null
                ? ""
                : projectile.getSourcePlant().getName().toLowerCase(java.util.Locale.ROOT);
        if (sourceName.contains("electric") || sourceName.contains("laser")) {
            play(Effect.ELECTRIC);
        } else if (projectile.getType() == ProjectileType.FIRE) {
            play(Effect.FIRE_HIT);
        } else if (projectile.getType() == ProjectileType.ICE) {
            play(Effect.ICE_HIT);
        } else if (projectile.isLobbed() || projectile.getType() == ProjectileType.LOBBED) {
            play(Effect.LOB_HIT);
        } else {
            play(Effect.PEA_HIT);
        }
    }

    void playPlantAttack(Plant plant) {
        if (plant == null || plant.getName() == null) {
            return;
        }
        String name = plant.getName().trim().toLowerCase(java.util.Locale.ROOT);
        if (name.contains("chomper")) {
            play(Effect.CHOMPER_BITE);
        } else if (name.contains("wasabi")) {
            play(Effect.MELEE_WHIP);
        } else if (name.contains("endurian")) {
            play(Effect.ENDURIAN_HIT);
        } else if (name.contains("phat beet")) {
            play(Effect.PHAT_BEET);
        } else if (name.contains("kiwibeast")) {
            play(Effect.KIWI_BEAST);
        } else {
            play(Effect.MELEE_PUNCH);
        }
    }

    void playZombieGroan(ChapterType chapterType) {
        if (chapterType == ChapterType.BIG_WAVE_BEACH) {
            play(Effect.ZOMBIE_GROAN_BEACH);
        } else if (chapterType == ChapterType.ANCIENT_EGYPT) {
            play(Effect.ZOMBIE_GROAN_EGYPT);
        } else {
            play(Effect.ZOMBIE_GROAN_GENERIC);
        }
    }

    void playZombieSpawn(Zombie zombie, ChapterType chapterType) {
        String alias = zombie == null || zombie.getDefinition() == null
                || zombie.getDefinition().getAlias() == null
                ? ""
                : zombie.getDefinition().getAlias().toLowerCase(java.util.Locale.ROOT);
        if (alias.contains("allstar")) {
            play(Effect.ALL_STAR);
        } else if (alias.contains("gargantuar")) {
            play(Effect.GARGANTUAR);
        } else if (alias.contains("barrelobstacle")) {
            play(Effect.BOWLING);
        } else if (alias.contains("arcademachine")) {
            play(Effect.ELECTRIC);
        } else if (alias.contains("iceblock")) {
            play(Effect.FREEZE);
        } else {
            playZombieGroan(chapterType);
        }
    }

    void playZombieDeath(Zombie zombie) {
        String alias = zombie == null || zombie.getDefinition() == null
                || zombie.getDefinition().getAlias() == null
                ? ""
                : zombie.getDefinition().getAlias().toLowerCase(java.util.Locale.ROOT);
        if (alias.contains("barrelobstacle")) {
            play(Effect.BOWLING);
        } else if (alias.contains("arcademachine")) {
            play(Effect.ELECTRIC);
        } else if (alias.contains("iceblock")) {
            play(Effect.FREEZE);
        } else {
            play(Effect.ZOMBIE_DEATH);
        }
    }

    public void dispose() {
        for (Sound sound : this.sounds.values()) {
            if (sound != null) {
                sound.dispose();
            }
        }
        this.sounds.clear();
        this.failedPaths.clear();
    }

    String auditMappedSounds() {
        ArrayList<String> failures = new ArrayList<>();
        int mapped = 0;
        for (Map.Entry<Effect, String[]> entry : PATHS.entrySet()) {
            for (String path : entry.getValue()) {
                mapped++;
                Sound sound = load(path);
                if (sound == null) {
                    failures.add(entry.getKey() + ": " + path);
                    continue;
                }
                long playbackId = sound.play(0f);
                if (playbackId < 0L) {
                    failures.add(entry.getKey() + ": playback failed for " + path);
                } else {
                    sound.stop(playbackId);
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("SFX audit failed:\n" + String.join("\n", failures));
        }
        return "loaded and played " + mapped + " mapped sound variants for " + PATHS.size() + " effects";
    }

    private Sound load(String path) {
        if (path == null || path.isBlank() || this.failedPaths.contains(path)) {
            return null;
        }
        Sound cached = this.sounds.get(path);
        if (cached != null) {
            return cached;
        }
        try {
            if (!Gdx.files.internal(path).exists()) {
                this.failedPaths.add(path);
                return null;
            }
            Sound loaded = Gdx.audio.newSound(Gdx.files.internal(path));
            this.sounds.put(path, loaded);
            return loaded;
        } catch (RuntimeException ignored) {
            this.failedPaths.add(path);
            return null;
        }
    }

    private static Map<Effect, String[]> createPaths() {
        Map<Effect, String[]> paths = new EnumMap<>(Effect.class);
        paths.put(Effect.BUTTON, variants(
                "audio/sfx/others/0099_button_click_default_audio_always_loaded_102.mp3"
        ));
        paths.put(Effect.CARD, variants(
                "audio/sfx/others/0102_button_click_snappy_audio_always_loaded_061.mp3"
        ));
        paths.put(Effect.SEED_SELECT, variants(
                "audio/sfx/others/0573_seed_packet_select_audio_always_loaded_384.mp3",
                "audio/sfx/others/0580_seedpacket_select_audio_always_loaded_148.mp3"
        ));
        paths.put(Effect.PLANT, variants(
                "audio/sfx/others/0569_seed_grass_place_lod_world_sfx_13.mp3",
                "audio/sfx/others/0576_seed_place_pshm_audio_always_loaded42.mp3"
        ));
        paths.put(Effect.SHOVEL, variants(
                "audio/sfx/others/0307_hard_snow_shovel_audio_always_loaded_156.mp3",
                "audio/sfx/others/0308_hard_snow_shovel_audio_always_loaded_73.mp3"
        ));
        paths.put(Effect.PLANT_FOOD, variants(
                "audio/sfx/others/0456_plantfood_breenweeoo_audio_always_loaded_383.mp3",
                "audio/sfx/others/0459_plantfood_weooieieiei_audio_always_loaded_248.mp3"
        ));
        paths.put(Effect.PLANT_FOOD_COLLECT, variants(
                "audio/sfx/others/0457_plantfood_brrreang_audio_always_loaded10.mp3"
        ));
        paths.put(Effect.SUN_PRODUCED, variants(
                "audio/sfx/others/0640_sun_audio_always_loaded_027.mp3"
        ));
        paths.put(Effect.SUN_COLLECT, variants(
                "audio/sfx/others/0641_sun_collect_audio_always_loaded_365.mp3",
                "audio/sfx/others/0642_sun_collect_high_audio_always_loaded_357.mp3",
                "audio/sfx/others/0643_sun_collect_low_audio_always_loaded_363.mp3"
        ));
        paths.put(Effect.PEA_FIRE, variants(
                "audio/sfx/others/0422_pea_firing_audio_always_loaded_100.mp3",
                "audio/sfx/others/0423_pea_firing_audio_always_loaded_367.mp3"
        ));
        paths.put(Effect.PEA_HIT, variants(
                "audio/sfx/others/0421_pea_collision_audio_always_loaded_369.mp3",
                "audio/sfx/others/0424_pea_hit_audio_always_loaded_306.mp3"
        ));
        paths.put(Effect.LOB_FIRE, variants(
                "audio/sfx/others/0818_wind_puff_audio_always_loaded_256.mp3"
        ));
        paths.put(Effect.LOB_HIT, variants(
                "audio/sfx/others/0178_collision_hit_audio_always_loaded_61.mp3"
        ));
        paths.put(Effect.FIRE_HIT, variants(
                "audio/sfx/others/0285_firework_pop_audio_always_loaded_295.mp3"
        ));
        paths.put(Effect.ICE_HIT, variants(
                "audio/sfx/others/0646_swipe_shimmer_slice_map_world_sfx_93.mp3"
        ));
        paths.put(Effect.ELECTRIC, variants(
                "audio/sfx/others/0255_electric_zap_audio_always_loaded_141.mp3",
                "audio/sfx/others/0256_electric_zap_audio_always_loaded_239.mp3"
        ));
        paths.put(Effect.MELEE_PUNCH, variants(
                "audio/sfx/plants/0207_plant_bonkchoy_2.mp3",
                "audio/sfx/plants/0208_plant_bonkchoy_3.mp3"
        ));
        paths.put(Effect.MELEE_WHIP, variants(
                "audio/sfx/others/0766_whip_light_joust_12_map_wolrd_sfx_22.mp3",
                "audio/sfx/others/0657_thwap_crunch_whip_audio_always_loaded_244.mp3"
        ));
        paths.put(Effect.CHOMPER_BITE, variants(
                "audio/sfx/plants/0396_plant_chomper_9.mp3",
                "audio/sfx/plants/0379_plant_chomper_1.mp3"
        ));
        paths.put(Effect.ENDURIAN_HIT, variants(
                "audio/sfx/plants/0703_plant_endurian_1.mp3",
                "audio/sfx/plants/0704_plant_endurian_2.mp3"
        ));
        paths.put(Effect.PHAT_BEET, variants(
                "audio/sfx/plants/1379_plant_phatbeet_3.mp3",
                "audio/sfx/plants/1383_plant_phatbeet_7.mp3"
        ));
        paths.put(Effect.KIWI_BEAST, variants(
                "audio/sfx/plants/1078_plant_kiwibeast_10.mp3",
                "audio/sfx/plants/1108_plant_kiwibeast_38.mp3"
        ));
        paths.put(Effect.REFLECT, variants(
                "audio/sfx/zombies/0465_zombie_darkages_jester_10.mp3",
                "audio/sfx/others/0145_clash_mirror_bounce_audio_always_loaded96.mp3"
        ));
        paths.put(Effect.ALL_STAR, variants(
                "audio/sfx/zombies/1521_zombie_modern_allstar_6.mp3",
                "audio/sfx/zombies/1522_zombie_modern_allstar_7.mp3"
        ));
        paths.put(Effect.CRYSTAL, variants(
                "audio/sfx/zombies/1402_zombie_lostcity_crystalskull_8.mp3",
                "audio/sfx/zombies/1396_zombie_lostcity_crystalskull_2.mp3"
        ));
        paths.put(Effect.GARGANTUAR, variants(
                "audio/sfx/zombies/1216_zombie_global_imp_gargantuar_29.mp3",
                "audio/sfx/zombies/1223_zombie_global_imp_gargantuar_35.mp3"
        ));
        paths.put(Effect.GARLIC, variants(
                "audio/sfx/plants/0798_plant_garlic_12.mp3",
                "audio/sfx/plants/0806_plant_garlic_9.mp3"
        ));
        paths.put(Effect.EXPLOSION, variants(
                "audio/sfx/others/0260_explosion_75124_1.mp3",
                "audio/sfx/others/0177_collision_explosion_can_audio_always_loaded_307.mp3"
        ));
        paths.put(Effect.ZOMBIE_BITE, variants(
                "audio/sfx/zombies/0202_zombie_bite_crunch_audio_always_loaded_371.mp3",
                "audio/sfx/zombies/0203_zombie_bite_crunch_audio_always_loaded_373.mp3",
                "audio/sfx/zombies/0204_zombie_bite_crunch_audio_always_loaded_376.mp3"
        ));
        paths.put(Effect.ZOMBIE_DEATH, variants(
                "audio/sfx/others/0768_whip_zombie_defeat_audio_always_loaded_83.mp3",
                "audio/sfx/others/0769_whip_zombie_defeat_audio_always_loaded_9.mp3"
        ));
        paths.put(Effect.ZOMBIE_GROAN_EGYPT, variants(
                "audio/sfx/zombies/0087_groans_egypt_1.mp3",
                "audio/sfx/zombies/0090_groans_egypt_3.mp3",
                "audio/sfx/zombies/0088_groans_egypt_10.mp3"
        ));
        paths.put(Effect.ZOMBIE_GROAN_BEACH, variants(
                "audio/sfx/zombies/0057_groans_beach_22.mp3",
                "audio/sfx/zombies/0061_groans_beach_4.mp3",
                "audio/sfx/zombies/0065_groans_beach_8.mp3"
        ));
        paths.put(Effect.ZOMBIE_GROAN_GENERIC, variants(
                "audio/sfx/zombies/0019_general_zombie_ingamesfx_10.mp3",
                "audio/sfx/zombies/0023_general_zombie_ingamesfx_14.mp3",
                "audio/sfx/zombies/0027_general_zombie_ingamesfx_18.mp3"
        ));
        paths.put(Effect.MOWER, variants("audio/sfx/others/0347_lawnmower.mp3"));
        paths.put(Effect.WAVE, variants(
                "audio/sfx/others/0846_zombie_close_warning_audio_always_loaded_19.mp3"
        ));
        paths.put(Effect.FINAL_WAVE, variants(
                "audio/sfx/others/0067_beep_wave_fade_up_joust_52_map_world_sfx_139.mp3"
        ));
        paths.put(Effect.COIN, variants(
                "audio/sfx/others/0147_coin_collect_audio_always_loaded33.mp3",
                "audio/sfx/others/0149_coin_collect_audio_always_loaded_121.mp3"
        ));
        paths.put(Effect.GEM, variants(
                "audio/sfx/others/0302_gem_collect_audio_always_loaded60.mp3"
        ));
        paths.put(Effect.WIN, variants(
                "audio/sfx/others/0496_quest_complete_audio_always_loaded_299.mp3"
        ));
        paths.put(Effect.LOSS, variants(
                "audio/sfx/others/0847_zombie_laugh_pvz1_audio_always_loaded_151.mp3"
        ));
        paths.put(Effect.VASE_BREAK, variants(
                "audio/sfx/others/0179_collision_kbop_map_world_sfx_97_minigame_vasebreaker_11.mp3",
                "audio/sfx/others/0381_minigame_vasebreaker.mp3"
        ));
        paths.put(Effect.BOWLING, variants(
                "audio/sfx/others/0305_gravel_roll_lod_world_sfx_22.mp3",
                "audio/sfx/others/0199_crash_wood_rolling_7211883_1.mp3"
        ));
        paths.put(Effect.MATCH, variants(
                "audio/sfx/others/0302_gem_collect_audio_always_loaded60.mp3",
                "audio/sfx/others/0164_coin_shing_audio_always_loaded_103.mp3"
        ));
        paths.put(Effect.UPGRADE, variants(
                "audio/sfx/others/0350_level_up_stats_audio_always_loaded_71.mp3"
        ));
        paths.put(Effect.UNLOCK, variants(
                "audio/sfx/others/0669_unlock_kweewoi_audio_always_loaded_288.mp3"
        ));
        paths.put(Effect.ZOMBIE_PLACE, variants(
                "audio/sfx/others/0570_seed_grass_squish_lod_world_sfx_6.mp3"
        ));
        paths.put(Effect.BRAIN_EAT, variants(
                "audio/sfx/zombies/0202_zombie_bite_crunch_audio_always_loaded_371.mp3"
        ));
        paths.put(Effect.MAGNET, variants(
                "audio/sfx/plants/1183_plant_magnetshroom_1.mp3"
        ));
        paths.put(Effect.MAGIC, variants(
                "audio/sfx/zombies/0486_zombie_darkages_wizard_1.mp3"
        ));
        paths.put(Effect.FREEZE, variants(
                "audio/sfx/others/0028_animation_freeze_ray_80036895_1.mp3"
        ));
        paths.put(Effect.GRAVE, variants(
                "audio/sfx/others/0109_button_gravestone_audio_always_loaded_036.mp3",
                "audio/sfx/others/0305_gravel_roll_lod_world_sfx_22.mp3"
        ));
        paths.put(Effect.SANDSTORM, variants(
                "audio/sfx/others/0848_zombie_egypt_sandstorm_1.mp3"
        ));
        paths.put(Effect.OCTOPUS, variants(
                "audio/sfx/zombies/0254_zombie_beach_octopus_1.mp3",
                "audio/sfx/zombies/0265_zombie_beach_octopus_2.mp3"
        ));
        paths.put(Effect.FISHERMAN, variants(
                "audio/sfx/zombies/0224_zombie_beach_fisher_1.mp3",
                "audio/sfx/zombies/0235_zombie_beach_fisher_2.mp3"
        ));
        paths.put(Effect.HUNTER, variants(
                "audio/sfx/zombies/1294_zombie_iceage_hunter_1.mp3",
                "audio/sfx/zombies/1296_zombie_iceage_hunter_2.mp3"
        ));
        paths.put(Effect.PROSPECTOR, variants(
                "audio/sfx/zombies/1768_zombie_wildwest_prospector_zombie_001.mp3",
                "audio/sfx/zombies/1769_zombie_wildwest_prospector_zombie_002.mp3"
        ));
        paths.put(Effect.DODO, variants(
                "audio/sfx/others/0806_whoosh_wing_flap_4312557_1.mp3",
                "audio/sfx/zombies/1259_zombie_iceage_dodo_1.mp3"
        ));
        paths.put(Effect.PIANO, variants(
                "audio/sfx/others/0259_engine_robot_piano_audio_always_loaded_34.mp3"
        ));
        return paths;
    }

    private static Map<Effect, Long> createIntervals() {
        Map<Effect, Long> intervals = new EnumMap<>(Effect.class);
        intervals.put(Effect.BUTTON, 80L);
        intervals.put(Effect.CARD, 80L);
        intervals.put(Effect.PEA_FIRE, 35L);
        intervals.put(Effect.PEA_HIT, 35L);
        intervals.put(Effect.LOB_HIT, 35L);
        intervals.put(Effect.ELECTRIC, 70L);
        intervals.put(Effect.MELEE_PUNCH, 90L);
        intervals.put(Effect.MELEE_WHIP, 120L);
        intervals.put(Effect.CHOMPER_BITE, 300L);
        intervals.put(Effect.ENDURIAN_HIT, 180L);
        intervals.put(Effect.PHAT_BEET, 150L);
        intervals.put(Effect.KIWI_BEAST, 150L);
        intervals.put(Effect.REFLECT, 180L);
        intervals.put(Effect.ALL_STAR, 500L);
        intervals.put(Effect.CRYSTAL, 500L);
        intervals.put(Effect.GARGANTUAR, 500L);
        intervals.put(Effect.GARLIC, 300L);
        intervals.put(Effect.EXPLOSION, 90L);
        intervals.put(Effect.ZOMBIE_BITE, 80L);
        intervals.put(Effect.ZOMBIE_DEATH, 80L);
        intervals.put(Effect.ZOMBIE_GROAN_EGYPT, 2200L);
        intervals.put(Effect.ZOMBIE_GROAN_BEACH, 2200L);
        intervals.put(Effect.ZOMBIE_GROAN_GENERIC, 2200L);
        intervals.put(Effect.BOWLING, 150L);
        intervals.put(Effect.MATCH, 120L);
        intervals.put(Effect.BRAIN_EAT, 300L);
        intervals.put(Effect.MAGNET, 180L);
        intervals.put(Effect.MAGIC, 180L);
        intervals.put(Effect.FREEZE, 180L);
        intervals.put(Effect.GRAVE, 250L);
        intervals.put(Effect.SANDSTORM, 1800L);
        intervals.put(Effect.OCTOPUS, 500L);
        intervals.put(Effect.FISHERMAN, 500L);
        intervals.put(Effect.HUNTER, 500L);
        intervals.put(Effect.PROSPECTOR, 500L);
        intervals.put(Effect.DODO, 350L);
        intervals.put(Effect.PIANO, 1200L);
        return intervals;
    }

    private static String[] variants(String... paths) {
        return paths;
    }
}
