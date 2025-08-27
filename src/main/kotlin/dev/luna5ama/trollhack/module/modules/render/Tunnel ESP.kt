package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.mask.EnumFacingMask
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.Wrapper.world
import dev.luna5ama.trollhack.util.world.isAir
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.AxisAlignedBB

internal object TunnelESP : Module(
    name = "Tunnel ESP",
    description = "Highlights tunnels",
    category = Category.RENDER
) {
    private val color by setting("Color", ColorRGB(255, 255, 255))
    private val width by setting("Width", 2.0f, 0.25f..5.0f, 0.25f)
    private val range by setting("Range", 64, 16..128, 1)
    private val minY by setting("Min Y", 64, 0..130, 1)
    private val maxY by setting("Max Y", 130, 0..130, 1)
    private val updateRate by setting("Update Rate", 5, 1..20, 1)

    private val renderer = ESPRenderer()
    private var frameCounter = 0
    private val processedPositions = mutableSetOf<BlockPos>()
    private val cachedTunnels = mutableSetOf<AxisAlignedBB>()
    
    init {
        safeListener<Render3DEvent> {
            frameCounter++
            
            if (frameCounter % updateRate == 0) {
                updateTunnels()
            }
            
            renderTunnels()
        }
    }

    private fun updateTunnels() {
        val playerPos = mc.player.position
        val world = mc.world ?: return
        
        processedPositions.clear()
        cachedTunnels.clear()

        val minYClamped = minY.coerceAtLeast(0)
        val maxYClamped = maxY.coerceAtMost(130)
        val outlineColor = ColorRGB(color.r, color.g, color.b, 255)

        for (y in minYClamped..maxYClamped step 2) {
            for (x in -range..range step 2) {
                for (z in -range..range step 2) {
                    val pos = BlockPos(playerPos.x + x, y, playerPos.z + z)
                    
                    if (processedPositions.contains(pos)) continue
                    
                    if (world.getBlockState(pos).isAir && isBasicTunnelCheck(pos)) {
                        val tunnelBounds = findCompleteTunnel(pos)
                        if (tunnelBounds != null) {
                            cachedTunnels.add(tunnelBounds)
                            markProcessedArea(tunnelBounds)
                        }
                    }
                }
            }
        }
    }

    private fun renderTunnels() {
        renderer.clear()
        val outlineColor = ColorRGB(color.r, color.g, color.b, 255)
        
        cachedTunnels.forEach { boundingBox ->
            renderer.add(boundingBox, outlineColor, EnumFacingMask.ALL)
        }
        
        renderer.aFilled = 0
        renderer.aOutline = 200
        renderer.through = true
        renderer.thickness = width
        renderer.render(true)
    }

    private fun isBasicTunnelCheck(pos: BlockPos): Boolean {
        val world = mc.world ?: return false

        return world.getBlockState(pos).isAir &&
                world.getBlockState(pos.up()).isAir &&
                !world.getBlockState(pos.down()).isAir &&
                !world.getBlockState(pos.up(2)).isAir
    }

    private fun findCompleteTunnel(startPos: BlockPos): AxisAlignedBB? {
        val world = mc.world ?: return null
        
        val xTunnel = expandTunnel(startPos, ::getXNeighbors)
        if (xTunnel != null && isValidTunnelShape(xTunnel, true)) {
            return xTunnel
        }

        val zTunnel = expandTunnel(startPos, ::getZNeighbors)
        if (zTunnel != null && isValidTunnelShape(zTunnel, false)) {
            return zTunnel
        }
        
        return null
    }

    private fun expandTunnel(startPos: BlockPos, getNeighbors: (BlockPos) -> Pair<BlockPos, BlockPos>): AxisAlignedBB? {
        val world = mc.world ?: return null
        
        var minPos = startPos
        var maxPos = startPos
        
        val (pos1, neg1) = getNeighbors(startPos)
        var current = pos1
        while (isValidTunnelSegment(current)) {
            maxPos = current
            val (next, _) = getNeighbors(current)
            if (next == current) break 
            current = next
        }
        
        current = neg1
        while (isValidTunnelSegment(current)) {
            minPos = current
            val (_, prev) = getNeighbors(current)
            if (prev == current) break
            current = prev
        }
        
        val length = if (minPos.x != maxPos.x) maxPos.x - minPos.x else maxPos.z - minPos.z
        if (length < 3) return null
        
        return AxisAlignedBB(
            minPos.x.toDouble(),
            minPos.y.toDouble(),
            minPos.z.toDouble(),
            maxPos.x + 1.0,
            maxPos.y + 2.0,
            maxPos.z + 1.0
        )
    }

    private fun getXNeighbors(pos: BlockPos): Pair<BlockPos, BlockPos> {
        return Pair(pos.east(), pos.west())
    }

    private fun getZNeighbors(pos: BlockPos): Pair<BlockPos, BlockPos> {
        return Pair(pos.north(), pos.south())
    }

    private fun isValidTunnelSegment(pos: BlockPos): Boolean {
        val world = mc.world ?: return false
        
        val posState = world.getBlockState(pos)
        val upState = world.getBlockState(pos.up())
        val downState = world.getBlockState(pos.down())
        val up2State = world.getBlockState(pos.up(2))
        
        return posState.isAir &&
                upState.isAir &&
                !downState.isAir &&
                !up2State.isAir
    }

    private fun isValidTunnelShape(boundingBox: AxisAlignedBB, isXDirection: Boolean): Boolean {
        val world = mc.world ?: return false
        
        val startX = boundingBox.minX.toInt()
        val endX = boundingBox.maxX.toInt() - 1
        val startZ = boundingBox.minZ.toInt()
        val endZ = boundingBox.maxZ.toInt() - 1
        val y = boundingBox.minY.toInt()
        
        return if (isXDirection) {
            checkWallLine(startX, endX, y, startZ - 1, true) &&
            checkWallLine(startX, endX, y, endZ + 1, true)
        } else {
            checkWallLine(startZ, endZ, y, startX - 1, false) &&
            checkWallLine(startZ, endZ, y, endX + 1, false)
        }
    }

    private fun checkWallLine(start: Int, end: Int, y: Int, wallCoord: Int, isXLine: Boolean): Boolean {
        val world = mc.world ?: return false
        
        for (coord in start..end) {
            val pos = if (isXLine) BlockPos(coord, y, wallCoord) else BlockPos(wallCoord, y, coord)
            val posUp = pos.up()
            
            if (world.getBlockState(pos).isAir || world.getBlockState(posUp).isAir) {
                return false
            }
        }
        return true
    }

    private fun markProcessedArea(boundingBox: AxisAlignedBB) {
        val startX = boundingBox.minX.toInt()
        val endX = boundingBox.maxX.toInt()
        val startZ = boundingBox.minZ.toInt()
        val endZ = boundingBox.maxZ.toInt()
        val y = boundingBox.minY.toInt()
        
        for (x in startX until endX) {
            for (z in startZ until endZ) {
                processedPositions.add(BlockPos(x, y, z))
            }
        }
    }
}
