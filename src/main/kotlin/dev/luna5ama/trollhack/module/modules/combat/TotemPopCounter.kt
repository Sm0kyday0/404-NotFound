package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.combat.TotemPopEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.EnumTextColor
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.format
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraft.util.math.BlockPos
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents

internal object TotemPopCounter : Module(
    name = "Totem Pop Counter",
    description = "Counts how many times players pop and Killeffect!",
    category = Category.COMBAT
) {
    private val countFriends by setting("Count Friends", true)
    private val countSelf by setting("Count Self", true)
    private val killEffect by setting("Kill Effect", true)
    private val killSound by setting("Kill Sound", true)
    private val thanksTo by setting("Thanks To", false)
    private val colorName by setting("Color Name", EnumTextColor.BLUE)
    private val colorNumber by setting("Color Number", EnumTextColor.GREEN)
    private val chat by setting("Chat", true)
    private val announce by setting("Announce", Announce.CLIENT, { chat })
    private val notification by setting("Notification", true)

    private enum class Announce {
        CLIENT, SERVER
    }

    init {
        safeListener<TotemPopEvent.Pop> {
            if (friendCheck(it.name) && selfCheck(it.name)) {
                val isSelf = it.name == player.name
                val message =
                    "${formatName(it.name)} popped ${formatNumber(it.count)} ${plural(it.count)}${ending(isSelf)}"
                sendMessage(it.name, message, !isSelf && isPublic)
            }
        }

        safeListener<TotemPopEvent.Death> {
            if (friendCheck(it.name) && selfCheck(it.name)) {
                val message = "${formatName(it.name)} died after popping ${formatNumber(it.count)} ${plural(it.count)}${
                    ending(false)
                }"
                sendMessage(it.name, message, isPublic)
            }
        }

        // プレイヤー死亡時の処理
        safeListener<EntityEvent.Death>(-1000) {
            val entity = it.entity
            if (entity is EntityPlayer && entity != player) {
                if (killEffect) {
                    spawnLightning(entity.world, entity.position)
                    Notification.send(TotemPopCounter, "${entity.name} died :) ezz")
                }
                if (killSound) {
                    playKillSound()
                }
            }
        }
    }

    private fun friendCheck(name: String): Boolean {
        return countFriends || !FriendManager.isFriend(name)
    }

    private fun selfCheck(name: String): Boolean {
        return countSelf || name != mc.player?.name
    }

    private fun formatName(name: String): String {
        return colorName.textFormatting format when {
            name == mc.player?.name -> "I"
            FriendManager.isFriend(name) -> if (isPublic) "My friend ${name}, " else "Your friend ${name}, "
            else -> name
        }
    }

    private val isPublic: Boolean
        get() = chat && announce == Announce.SERVER

    private fun formatNumber(message: Int): String {
        return colorNumber.textFormatting format message
    }

    private fun plural(count: Int): String {
        return if (count == 1) "totem" else "totems"
    }

    private fun ending(self: Boolean): String {
        return if (!self && thanksTo) " thanks to ${TrollHackMod.NAME} !" else "!"
    }

    private fun sendMessage(name: String, message: String, public: Boolean) {
        TextFormatting.getTextWithoutFormattingCodes(message)?.let {
            if (public) sendServerMessage(it)
            else if (chat) NoSpamMessage.sendMessage(name.hashCode(), "$chatName $message")
            if (notification) Notification.send(this.hashCode() * 31 + name.hashCode(), message)
        }
    }


    private fun spawnLightning(world: World, position: BlockPos) {
        val lightning = EntityLightningBolt(world, position.x.toDouble(), position.y.toDouble(), position.z.toDouble(), false)
        world.spawnEntity(lightning)
    }


    private fun playKillSound() {
        mc.player?.playSound(
            SoundEvents.ENTITY_LIGHTNING_THUNDER, // 再生する音
            1.0f,
            1.0f
        )
    }
}
