package dev.luna5ama.trollhack.gui.hudgui.elements.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.manager.managers.TimerManager
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import net.minecraft.block.material.Material
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

internal object PhaseTime : LabelHud(
    name = "Phase Time",
    category = Category.COMBAT,
    description = "Phase is idiot :<"
) {
    private var timeInBlock = 0L
    private var lastUpdateTime = System.currentTimeMillis()
    private var lastOutOfBlockTime = System.currentTimeMillis()
    private val resetDelay = 1000L

    override fun SafeClientEvent.updateText() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastUpdateTime
        lastUpdateTime = currentTime
        if (isPlayerInsideBlock()) {
            timeInBlock += deltaTime
            lastOutOfBlockTime = currentTime
        } else {
            if (currentTime - lastOutOfBlockTime >= resetDelay) {
                timeInBlock = 0L // リセット
                displayText.clear()
            }
        }

        // 時間を表示（秒単位）
        if (timeInBlock > 0) {
            val timeInSeconds = timeInBlock / 1000.0
            displayText.clear()
            displayText.add("%.1f seconds".format(timeInSeconds), GuiSetting.text)
        }
    }

    /**
     * プレイヤーが空気以外のブロック内にいるかを判定
     */
    private fun SafeClientEvent.isPlayerInsideBlock(): Boolean {
        val boundingBox = player.entityBoundingBox.shrink(0.25)//aa
        val blockStates = world.getCollisionBoxes(player, boundingBox)
        return blockStates.any { block ->
            val minX = block.minX.toInt()
            val minY = block.minY.toInt()
            val minZ = block.minZ.toInt()
            val blockPos = BlockPos(minX, minY, minZ)
            world.getBlockState(blockPos).material != Material.AIR
        }
    }
}
