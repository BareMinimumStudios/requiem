package dev.pokesmells.requiem

import me.fzzyhmstrs.fzzy_config.annotations.NonSync
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import net.minecraft.resources.ResourceLocation
import java.util.Locale

const val MOD_ID = "requiem_cinematics"

fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

class RequiemConfig : Config(id("settings")) {
    var mortality = MortalityMode.UNLIMITED
    var defaultLives = 3
    var chargePlayerDeaths = true
    var chargeMobDeaths = true
    var chargeEnvironmentalDeaths = true
    var creditRecentAttacker = true
    var allowVoluntaryFinalDeath = false
    var allowDeferredDecision = true
    var permanentDeathPolicy = FinalPolicy.SPECTATOR
    var respawn = RespawnMode.MANUAL
    var automaticDelaySeconds = 5
    var playerOverrides = ValidatedStringMap(emptyMap(), ValidatedString(), ValidatedString())
    var firstJoinCinematic = false

    @NonSync var playCinematics = true
    @NonSync var reducedMotion = false
    @NonSync var muteCinematics = false
    @NonSync var plainInterface = false
    @NonSync var showLivesRemaining = false
    @NonSync var title = "The Between"
    @NonSync var subtitle = "Beyond the silence, the world awaits you once more."
    @NonSync var returnText = "Return to Existence"
    @NonSync var finalText = "Embrace Eternity"
    @NonSync var deferText = "Choose Later"
    @NonSync var skyColor = 0x14152D
    @NonSync var starColor = 0xA9BBFF
    @NonSync var accentColor = 0x819BEF
    @NonSync var textColor = 0xEFF3FF
    @NonSync var titleScale = 1.5
    @NonSync var starCount = 320
    @NonSync var colorfulStars = true
    @NonSync var twinkleHoldTicks = 20
    @NonSync var warpTicks = 102
    @NonSync var revealTicks = 36
    @NonSync var cameraSettle = true
    @NonSync var buttonHoverSound = "requiem_cinematics:button_hover"
    @NonSync var buttonHoverVolume = 0.24
    @NonSync var buttonClickSound = "requiem_cinematics:button_click"
    @NonSync var buttonClickVolume = 0.36
    @NonSync var warpSound = "requiem_cinematics:warp"
    @NonSync var warpVolume = 0.48
    @NonSync var warpStartPitch = 0.72
    @NonSync var warpPeakPitch = 1.28
    @NonSync var crossfadeWorldAudio = true
    @NonSync var warpWorldVolume = 0.12

    @NonSync var deathSound = "minecraft:block.amethyst_block.resonate"
    @NonSync var returnSound = "minecraft:block.amethyst_block.chime"

    fun rules(): MortalityRules {
        val overrides = playerOverrides.get().mapNotNull { (key, value) ->
            val parts = value.trim().split(':', limit = 2)
            val mode = MortalityMode.entries.find { it.name.equals(parts[0], ignoreCase = true) }
                ?: return@mapNotNull null
            val lives = parts.getOrNull(1)?.toIntOrNull() ?: defaultLives
            key.trim().lowercase(Locale.ROOT) to PlayerOverride(mode, lives)
        }.toMap()
        return MortalityRules(
            mortality, defaultLives, buildSet {
                if (chargePlayerDeaths) add(DeathCause.PLAYER)
                if (chargeMobDeaths) add(DeathCause.MOB)
                if (chargeEnvironmentalDeaths) add(DeathCause.ENVIRONMENT)
            }, allowVoluntaryFinalDeath, allowDeferredDecision, permanentDeathPolicy,
            respawn, automaticDelaySeconds, overrides,
        )
    }
}

object RequiemSettings {
    val config: RequiemConfig by lazy { ConfigApi.registerAndLoadConfig(::RequiemConfig) }
}
