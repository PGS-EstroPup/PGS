package net.minecraft.util.math

typealias BlockPos = net.minecraft.core.BlockPos
typealias Box = net.minecraft.world.phys.AABB
typealias Vec3d = net.minecraft.world.phys.Vec3
typealias Vec2f = net.minecraft.world.phys.Vec2

fun BlockPos.down(): BlockPos = below()


