package com.soilcarbon.engine.domain.usecase

import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.FeatureVector
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.TextureFeatures

/**
 * Converts calibrated colour + texture + satellite covariates into the mandated SRS vector:
 * [L*, a*, b*, Munsell_Value, GLCM_Contrast, GLCM_Energy, NDVI_Sat, Clay_Content].
 */
object FeatureVectorFactory {

    fun build(
        color: ColorFeatures,
        texture: TextureFeatures,
        covariates: EnvironmentalCovariates
    ): FeatureVector = FeatureVector(
        lStar = color.lab.l.coerceIn(0.0, 100.0),
        aStar = color.lab.a.coerceIn(-128.0, 127.0),
        bStar = color.lab.b.coerceIn(-128.0, 127.0),
        munsellValue = color.munsell.value.coerceIn(0.0, 10.0),
        glcmContrast = texture.contrast.coerceIn(0.0, 1.0),
        glcmEnergy = texture.energy.coerceIn(0.0, 1.0),
        ndviSatellite = covariates.ndvi.value.coerceIn(-1.0, 1.0),
        clayContentPercent = covariates.clayPercent.value.coerceIn(0.0, 100.0)
    )

    /**
     * Validation hook used before inference. Returns a list of human-readable failures; an empty
     * list means the vector is within the domains used to train the shipped model.
     */
    fun validate(vector: FeatureVector): List<String> = buildList {
        if (vector.lStar < 2.0) add("L* unusually dark; check lens contamination or under-exposure")
        if (vector.glcmContrast > 0.85) add("Texture contrast saturated; soil is out of focus or clumped")
        if (vector.glcmEnergy < 0.05) add("Texture energy extremely low; aperture may be occluded")
        if (vector.clayContentPercent !in 1.0..80.0) add("Clay covariate outside plausible range")
    }
}
