package com.soilcarbon.engine.data.local

import com.google.gson.Gson
import com.soilcarbon.engine.domain.model.CalibrationProfile
import com.soilcarbon.engine.domain.model.ProvenanceHasher
import com.soilcarbon.engine.domain.vision.ChannelAffine
import com.soilcarbon.engine.domain.vision.CalibrationResult
import com.soilcarbon.engine.domain.vision.PatchObservation
import com.soilcarbon.engine.domain.vision.RgbColor
import com.soilcarbon.engine.domain.vision.XyzColor

private data class PatchDto(
    val id: String = "",
    val name: String = "",
    val targetReflectance: Double = 0.0,
    val r: Double = 0.0,
    val g: Double = 0.0,
    val b: Double = 0.0,
    val linearR: Double = 0.0,
    val linearG: Double = 0.0,
    val linearB: Double = 0.0,
    val spatialStdDev: Double = 0.0,
    val pixelCount: Int = 0
)

object CalibrationMapper {

    private val gson = Gson()

    fun toEntity(profile: CalibrationProfile): CalibrationEntity = CalibrationEntity(
        id = profile.id,
        cardVersion = profile.cardVersion,
        deviceModel = profile.deviceModel,
        sourceDescription = profile.result.sourceDescription,
        overallRmse = profile.result.overallRmse,
        worstRSquared = profile.result.worstRSquared,
        illuminationVariation = profile.result.illuminationVariation,
        d65X = profile.result.d65WhitePoint.x,
        d65Y = profile.result.d65WhitePoint.y,
        d65Z = profile.result.d65WhitePoint.z,
        redGain = profile.result.red.gain,
        redOffset = profile.result.red.offset,
        redRmse = profile.result.red.rmse,
        redRSquared = profile.result.red.rSquared,
        greenGain = profile.result.green.gain,
        greenOffset = profile.result.green.offset,
        greenRmse = profile.result.green.rmse,
        greenRSquared = profile.result.green.rSquared,
        blueGain = profile.result.blue.gain,
        blueOffset = profile.result.blue.offset,
        blueRmse = profile.result.blue.rmse,
        blueRSquared = profile.result.blue.rSquared,
        patchesJson = gson.toJson(profile.result.patches.map { it.toDto() }),
        sha256 = profile.sha256,
        createdAtMillis = profile.createdAtMillis
    )

    fun toDomain(entity: CalibrationEntity): CalibrationProfile {
        val patches = runCatching {
            gson.fromJson(entity.patchesJson, Array<PatchDto>::class.java).map { it.toDomain() }
        }.getOrElse { emptyList() }
        val result = CalibrationResult(
            patches = patches,
            red = ChannelAffine(entity.redGain, entity.redOffset, entity.redRmse, entity.redRSquared),
            green = ChannelAffine(entity.greenGain, entity.greenOffset, entity.greenRmse, entity.greenRSquared),
            blue = ChannelAffine(entity.blueGain, entity.blueOffset, entity.blueRmse, entity.blueRSquared),
            overallRmse = entity.overallRmse,
            worstRSquared = entity.worstRSquared,
            illuminationVariation = entity.illuminationVariation,
            d65WhitePoint = XyzColor(entity.d65X, entity.d65Y, entity.d65Z),
            sourceDescription = entity.sourceDescription,
            createdAtMillis = entity.createdAtMillis
        )
        return CalibrationProfile(
            id = entity.id,
            cardVersion = entity.cardVersion,
            deviceModel = entity.deviceModel,
            result = result,
            sha256 = entity.sha256,
            createdAtMillis = entity.createdAtMillis
        )
    }

    fun digest(profile: CalibrationProfile): String = ProvenanceHasher.sha256Hex(
        listOf(
            profile.cardVersion,
            profile.deviceModel,
            profile.createdAtMillis,
            profile.result.red.gain,
            profile.result.red.offset,
            profile.result.green.gain,
            profile.result.green.offset,
            profile.result.blue.gain,
            profile.result.blue.offset,
            profile.result.illuminationVariation,
            profile.result.worstRSquared
        ).joinToString("|")
    )

    private fun PatchObservation.toDto(): PatchDto = PatchDto(
        id = id,
        name = name,
        targetReflectance = targetReflectance,
        r = observedRgb.r,
        g = observedRgb.g,
        b = observedRgb.b,
        linearR = observedLinearRgb.r,
        linearG = observedLinearRgb.g,
        linearB = observedLinearRgb.b,
        spatialStdDev = spatialStdDev,
        pixelCount = pixelCount
    )

    private fun PatchDto.toDomain(): PatchObservation = PatchObservation(
        id = id,
        name = name,
        targetReflectance = targetReflectance,
        observedRgb = RgbColor(r, g, b),
        observedLinearRgb = RgbColor(linearR, linearG, linearB),
        spatialStdDev = spatialStdDev,
        pixelCount = pixelCount
    )
}
