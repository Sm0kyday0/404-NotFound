package dev.luna5ama.trollhack.module.modules.movement
/*
import dev.luna5ama.kmogusync.Timing
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.setting.setting
import dev.luna5ama.trollhack.util.PlayerUtils
import jdk.nashorn.internal.runtime.Timing
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.*
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.roundToInt
//IQを振りかける
internal object BlockMove : Module(
    name = "BlockMove",
    description = "Allows directional block-aligned movement within collision bounds",
    category = Category.MOVEMENT) {

    private val middle by setting("Middle", true)
    private val delay by setting("Delay", 250, 0..2000, 50)
    private val onlyInBlock by setting("Only In Block", true)
    private val avoidOut by setting("Avoid Out", true) { !onlyInBlock }

    private val sides = arrayOf(
        Vec3d(0.24, 0.0, 0.24),
        Vec3d(-0.24, 0.0, 0.24),
        Vec3d(0.24, 0.0, -0.24),
        Vec3d(-0.24, 0.0, -0.24)
    )

    private val timer = Timing()

    @SubscribeEvent
    fun onInputUpdate(event: InputUpdateEvent) {
        val player = mc.player ?: return
        val world = mc.world ?: return
        val playerBox = player.entityBoundingBox
        val basePos = player.positionVector
        var inBlock = false

        outer@ for (offset in sides) {
            for (i in 0..1) {
                val checkPos = BlockPos(basePos.add(offset).add(0.0, i.toDouble(), 0.0))
                val state = world.getBlockState(checkPos)
                if (!state.material.isReplaceable) {
                    val box = state.getCollisionBoundingBox(world, checkPos)?.offset(checkPos) ?: continue
                    if (playerBox.intersects(box)) {
                        inBlock = true
                        break@outer
                    }
                }
            }
        }

        if (onlyInBlock && !inBlock) return

        val input = event.movementInput
        if (input is MovementInputFromOptions && timer.passedMs(delay.toLong())) {
            val pos = if (middle) PlayerUtils.playerPos else BlockPos(
                basePos.x.roundToInt(), basePos.y.toInt(), basePos.z.roundToInt()
            )

            val facing = player.horizontalFacing
            val dx = facing.xOffset
            val dz = facing.zOffset
            val isXAxis = dx != 0

            val target = when {
                input.forwardKeyDown -> getTargetPos(pos, isXAxis, if (isXAxis) dx < 0 else dz < 0)
                input.backKeyDown -> getTargetPos(pos, isXAxis, if (isXAxis) dx > 0 else dz > 0)
                input.leftKeyDown -> getTargetPos(pos, !isXAxis, if (isXAxis) dx > 0 else dz < 0)
                input.rightKeyDown -> getTargetPos(pos, !isXAxis, if (isXAxis) dx < 0 else dz > 0)
                else -> null
            }

            target?.let {
                player.setPosition(it.x, it.y, it.z)
                timer.reset()

                input.forwardKeyDown = false
                input.backKeyDown = false
                input.leftKeyDown = false
                input.rightKeyDown = false
                input.moveForward = 0f
                input.moveStrafe = 0f
            }
        }
    }

    private fun getTargetPos(pos: BlockPos, xAxis: Boolean, negative: Boolean): Vec3d? {
        val target = when {
            xAxis && negative -> pos.add(-1, 0, 0)
            xAxis && !negative -> pos.add(1, 0, 0)
            !xAxis && negative -> pos.add(0, 0, -1)
            else -> pos.add(0, 0, 1)
        }
        return computePosition(target)
    }

    private fun computePosition(pos: BlockPos): Vec3d? {
        val world = mc.world ?: return null

        if (middle) {
            return Vec3d(pos).add(0.5, 0.0, 0.5)
        }

        var lastVec = Vec3d(pos)
        var blocked = false

        val variants = listOf(
            Vec3d(pos.x - 1e-8, pos.y.toDouble(), pos.z.toDouble()),
            Vec3d(pos.x.toDouble(), pos.y.toDouble(), pos.z - 1e-8),
            Vec3d(pos.x - 1e-8, pos.y.toDouble(), pos.z - 1e-8)
        )

        for (vec in variants) {
            val blockPos = BlockPos(vec)
            if (world.isAirBlock(blockPos) && world.isAirBlock(blockPos.up())) {
                lastVec = vec
            } else {
                blocked = true
            }
        }

        return if (!onlyInBlock && avoidOut && !blocked) null else lastVec
    }

*/