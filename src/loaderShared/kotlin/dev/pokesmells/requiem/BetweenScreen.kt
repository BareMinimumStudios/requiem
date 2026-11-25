package dev.pokesmells.requiem

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import org.lwjgl.glfw.GLFW
import kotlin.math.ceil

class BetweenScreen(
    private val view: DeathView,
    private val send: (DeathChoice) -> Unit,
    private var starClockOffset: Float = 0f,
) : Screen(label("screen")) {
    internal val isTransition = view.presentation == DeathPresentation.TRANSITION
    private val particles = DreamParticles()
    private val actionButtons = mutableListOf<Button>()
    private var soundPlayed = false
    private val config get() = RequiemSettings.config
    private var age = 0
    private var returnStarted: Int? = null
    private var choiceSent = false
    private var finalStarted: Int? = null
    private var titleY = 0
    private var titleScale = 1f
    private var subtitleY = 0
    private var subtitleLines = emptyList<FormattedCharSequence>()
    private var livesY = 0
    private var livesLabel: Component? = null
    private val reducedMotion = !config.playCinematics || config.reducedMotion || config.plainInterface
    private val timing = CinematicTiming.forSettings(reducedMotion, config.warpTicks, config.revealTicks)
    private val dialogue = if (!isTransition && !reducedMotion && config.playDecisionDialogue) {
        listOf("…", "…", config.disorientedText)
    } else {
        emptyList()
    }
    private var dialogueStep = 0
    private var dialogueChangedAt = 0
    private var dialogueKeyHeld = false
    private val dialoguePending get() = dialogueStep < dialogue.size
    private val twinkleHoldTicks = config.twinkleHoldTicks.coerceIn(0, 80)

    private fun sceneClock(partialTick: Float): Float = (age - 1).coerceAtLeast(0) + partialTick.coerceIn(0f, 1f)

    internal fun cameraTilt(partialTick: Float): Float =
        if (!isTransition || reducedMotion || !config.cameraSettle) 0f
        else -4f * (1f - timing.reveal(sceneClock(partialTick)))

    internal fun warpAudioFrame(): WarpAudioFrame? {
        if (finalStarted != null) return WarpAudioFrame(0f, 0f, 0f)
        if (isTransition) {
            val clock = age.toFloat()
            val flight = if (reducedMotion) 0.25f else clock / timing.revealStart.coerceAtLeast(1)
            val stretch = if (reducedMotion) 0f else StarFlight.stretch(clock)
            val tail = if (reducedMotion) 0 else config.warpTailTicks.coerceIn(0, 80)
            return WarpEnvelope.flight(flight, timing.reveal(clock), stretch, tail)
        }
        return returnStarted?.let { primingAudio((age - it).toFloat()) }
    }

    internal fun primingAudio(elapsed: Float): WarpAudioFrame =
        WarpEnvelope.prime(elapsed / twinkleHoldTicks.coerceIn(1, 20))

    fun cancelHandoff() {
        val wasFinal = finalStarted != null
        finalStarted = null
        returnStarted = null
        choiceSent = false
        if (wasFinal && minecraft?.screen === this) minecraft?.setScreen(this)
        actionButtons.forEach { it.active = true }
    }

    fun starClockAtHandoff(elapsed: Float): Float =
        starClockOffset + (finalStarted ?: returnStarted ?: age) + elapsed

    internal fun starClock() = starClockOffset + age

    fun renderHandoff(graphics: GuiGraphics, clock: Float) {
        renderStarField(graphics, starClockAtHandoff(clock))
    }

    internal fun renderStarField(graphics: GuiGraphics, clock: Float, reveal: Float = 0f) {
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        val opacity = 1f - reveal
        graphics.fillGradient(0, 0, width, height, tinted(0x03040c, opacity), tinted(config.skyColor, opacity))
        if (!config.plainInterface) {
            drawParticles(graphics, width, height, clock, 1f - smoothStep(reveal), null)
        }
    }

    @Suppress("DEPRECATION")
    private fun drawParticles(
        graphics: GuiGraphics, width: Int, height: Int, skyClock: Float,
        opacity: Float, flightTicks: Float?,
    ) {
        graphics.drawManaged {
            particles.draw(graphics, width, height, skyClock, opacity, flightTicks, reducedMotion,
                config.starCount, config.starColor, config.accentColor, config.colorfulStars)
        }
    }

    override fun isPauseScreen() = false
    override fun shouldCloseOnEsc() = isTransition

    override fun init() {
        actionButtons.clear()
        if (finalStarted != null) return
        if (!isTransition && !soundPlayed && config.playCinematics) {
            soundPlayed = true
            RequiemAudio.play(config.deathSound)
        }
        if (isTransition) {
            addAction(dreamButton((width - 82).coerceAtLeast(0), 10, minOf(72, width.coerceAtLeast(1)), label("skip")) { onClose() })
            return
        }
        val textWidth = minOf(310, (width - 24).coerceAtLeast(1))
        val availableScale = textWidth.toFloat() / font.width(config.title).coerceAtLeast(1)
        titleScale = minOf(config.titleScale.coerceIn(0.75, 3.0).toFloat(), availableScale)
        val titleHeight = ceil(font.lineHeight * titleScale).toInt()
        val buttonCount = listOf(view.lives != 0, view.canFinalize, view.canDefer).count { it }
        livesLabel = when {
            !config.showLivesRemaining -> null
            view.lives < 0 -> label("unlimited")
            else -> label("lives", view.lives)
        }
        val livesHeight = if (livesLabel != null) font.lineHeight + 8 else 0
        val fixedHeight = titleHeight + 12 + livesHeight + 18 + (buttonCount * 24 - 4)
        val maxLines = ((height - fixedHeight - 16) / font.lineHeight).coerceAtLeast(1)
        val wrapped = font.split(Component.literal(config.subtitle), textWidth)
        subtitleLines = if (wrapped.size <= maxLines) wrapped
            else wrapped.take(maxLines - 1) + Component.literal("…").visualOrderText
        val subtitleHeight = subtitleLines.size * font.lineHeight
        titleY = ((height - fixedHeight - subtitleHeight) / 2).coerceAtLeast(4)
        subtitleY = titleY + titleHeight + 12
        livesY = subtitleY + subtitleHeight + 8
        var top = subtitleY + subtitleHeight + livesHeight + 18
        fun button(text: String, action: () -> Unit) {
            val buttonWidth = minOf(200, width - 20).coerceAtLeast(1)
            val x = (width - buttonWidth) / 2
            addAction(dreamButton(x, top, buttonWidth, Component.literal(text), action))
            top += 24
        }
        if (view.lives != 0) button(config.returnText) { choose(Decision.RETURN) }
        if (view.canFinalize) button(config.finalText) {
            minecraft?.setScreen(FinalDeathScreen(view.transaction, this) { confirmed, clock ->
                starClockOffset = clock - age
                if (confirmed && config.playCinematics && !config.plainInterface) {
                    finalStarted = age
                    RequiemClient.beginFinal(this)
                }
                minecraft?.setScreen(this)
                if (confirmed) choose(Decision.FINAL)
            })
        }
        if (view.canDefer) button(config.deferText) { choose(Decision.DEFER) }
    }

    private fun addAction(button: Button) {
        if (dialoguePending) {
            button.visible = false
            button.active = false
        }
        actionButtons.add(addRenderableWidget(button))
    }

    internal fun isDecisionFor(transaction: java.util.UUID): Boolean = !isTransition && view.transaction == transaction

    private fun choose(choice: Decision) {
        if (choiceSent || returnStarted != null) return
        actionButtons.forEach { it.active = false }
        if (choice == Decision.RETURN && !reducedMotion) {
            returnStarted = age
            RequiemClient.beginReturn(this)
            if (twinkleHoldTicks == 0) sendChoice(choice)
        } else {
            sendChoice(choice)
        }
    }

    private fun sendChoice(choice: Decision) {
        choiceSent = true
        send(DeathChoice(view.transaction, choice))
    }

    override fun tick() {
        age++
        if (isTransition && !soundPlayed && age >= timing.revealStart.coerceAtLeast(1)) {
            soundPlayed = true
            RequiemAudio.play(config.returnSound, 0.20)
        }
        returnStarted?.let {
            if (!choiceSent && age - it >= twinkleHoldTicks) sendChoice(Decision.RETURN)
        }
        if (isTransition && age > timing.duration) onClose()
        if (minecraft?.level == null) onClose()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val renderPartial = minecraft?.timer?.getGameTimeDeltaPartialTick(false) ?: partialTick
        if (finalStarted != null) {
            renderStarField(graphics, starClockOffset + sceneClock(renderPartial))
            return
        }
        val clock = sceneClock(renderPartial)
        val reveal = if (isTransition) timing.reveal(clock) else 0f
        val emergence = if (isTransition || reducedMotion) 1f else smoothStep(clock / 19f)
        val windup = returnStarted?.let { smoothStep((clock - it) / (twinkleHoldTicks - 1).coerceAtLeast(1)) } ?: 0f
        val darkness = if (isTransition) 1f - reveal else emergence
        val flightTicks = if (isTransition && !reducedMotion) clock else null
        val controls = when {
            isTransition -> 1f - reveal
            dialoguePending -> 0f
            reducedMotion -> 1f
            else -> smoothStep((clock - dialogueChangedAt - 4f) / 12f) * (1f - windup)
        }

        graphics.fillGradient(0, 0, width, height, tinted(0x03040c, darkness), tinted(config.skyColor, darkness))
        if (!config.plainInterface) {
            val starlight = if (isTransition) 1f - smoothStep((reveal - 0.16f) / 0.84f) else darkness
            drawParticles(graphics, width, height, starClockOffset + clock, starlight, flightTicks)
        }
        actionButtons.forEach {
            it.visible = controls > 0.05f
            it.active = it.visible && !choiceSent && returnStarted == null
            it.setAlpha(controls)
        }
        if (isTransition) RequiemRecap.render(graphics, view.transaction, clock, timing.revealStart)
        if (dialoguePending) drawDialogue(graphics, clock)
        if (!isTransition && controls > 0.05f) {
            val textColor = tinted(config.textColor, controls)
            val pose = graphics.pose()
            pose.pushPose()
            pose.translate((width / 2).toFloat(), titleY.toFloat(), 0f)
            pose.scale(titleScale, titleScale, 1f)
            graphics.drawCenteredString(font, config.title, 0, 0, textColor)
            pose.popPose()
            subtitleLines.forEachIndexed { index, line ->
                graphics.drawCenteredString(font, line, width / 2, subtitleY + index * font.lineHeight, textColor)
            }
            livesLabel?.let { graphics.drawCenteredString(font, it, width / 2, livesY, textColor) }
        }
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!dialoguePending || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button)
        }
        advanceDialogue()
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if ((dialoguePending || dialogueKeyHeld) && isDialogueKey(keyCode)) {
            if (!dialogueKeyHeld) advanceDialogue()
            dialogueKeyHeld = true
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (isDialogueKey(keyCode)) dialogueKeyHeld = false
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    private fun isDialogueKey(keyCode: Int) =
        keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE

    private fun advanceDialogue() {
        RequiemAudio.click()
        dialogueStep++
        dialogueChangedAt = age
    }

    private fun drawDialogue(graphics: GuiGraphics, clock: Float) {
        val alpha = smoothStep((clock - dialogueChangedAt) / 7f)
        if (alpha < 0.025f) return
        val lines = font.split(Component.literal(dialogue[dialogueStep]), (width - 24).coerceAtLeast(1))
        val top = (height - lines.size * font.lineHeight) / 2
        lines.forEachIndexed { index, line ->
            graphics.drawCenteredString(font, line, width / 2, top + index * font.lineHeight, tinted(config.textColor, alpha))
        }
        graphics.drawCenteredString(font, label("dialogue_continue"), width / 2,
            (top + lines.size * font.lineHeight + 12).coerceAtMost((height - font.lineHeight).coerceAtLeast(0)),
            tinted(config.textColor, alpha * 0.6f))
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) = Unit
}

private fun label(key: String, vararg args: Any) = Component.translatable("requiem_cinematics.$key", *args)
