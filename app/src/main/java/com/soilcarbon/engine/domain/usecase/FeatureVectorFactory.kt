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
}
