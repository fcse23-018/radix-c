package com.terrochroma.shared.ml.fusion

import com.terrochroma.shared.ble.model.SpectrumData
import com.terrochroma.shared.domain.model.ColorFeatures
import com.terrochroma.shared.domain.model.TextureFeatures

/**
 * Data fusion logic for combining camera color features with NIR spectrum data
 * Uses External Parameter Orthogonalization (EPO) for moisture correction
 */
class DataFusion {

    /**
     * Fuse camera color features with NIR spectrum data
     * @param colorFeatures Color features from camera
     * @param textureFeatures Texture features from GLCM
     * @param spectrumData NIR spectrum data from spectrometer
     * @return Fused feature vector for ML model
     */
    fun fuseFeatures(
        colorFeatures: ColorFeatures,
        textureFeatures: TextureFeatures,
        spectrumData: SpectrumData
    ): FusedFeatures {
        // Extract key NIR wavelengths
        val nirFeatures = extractNIRFeatures(spectrumData)
        
        // Calculate moisture content from NIR
        val moisture = estimateMoisture(spectrumData)
        
        // Apply EPO moisture correction to color features
        val correctedColor = applyEPOCorrection(colorFeatures, moisture)
        
        // Combine all features
        return FusedFeatures(
            // Color features (corrected)
            cieLabL = correctedColor.cieLab.L,
            cieLabA = correctedColor.cieLab.a,
            cieLabB = correctedColor.cieLab.b,
            hsvH = correctedColor.hsv.h,
            hsvS = correctedColor.hsv.s,
            hsvV = correctedColor.hsv.v,
            
            // Texture features
            textureContrast = textureFeatures.contrast,
            textureHomogeneity = textureFeatures.homogeneity,
            textureEnergy = textureFeatures.energy,
            textureEntropy = textureFeatures.entropy,
            
            // NIR features
            nirRedEdge = nirFeatures.redEdge,
            nirWaterBand = nirFeatures.waterBand,
            nirVegetationIndex = nirFeatures.vegetationIndex,
            
            // Moisture
            moistureContent = moisture,
            
            // Quality metrics
            dataQuality = calculateDataQuality(spectrumData, colorFeatures)
        )
    }

    /**
     * Extract key NIR spectral features
     */
    private fun extractNIRFeatures(spectrumData: SpectrumData): NIRFeatures {
        val wavelengths = spectrumData.wavelengths
        val intensities = spectrumData.intensities
        
        // Red edge (680-730 nm) - indicates vegetation health
        val redEdge = calculateSpectralMean(wavelengths, intensities, 680.0, 730.0)
        
        // Water absorption band (1400-1500 nm)
        val waterBand = calculateSpectralMean(wavelengths, intensities, 1400.0, 1500.0)
        
        // Vegetation index (simple NDVI-like using NIR)
        val vegetationIndex = calculateVegetationIndex(wavelengths, intensities)
        
        return NIRFeatures(
            redEdge = redEdge,
            waterBand = waterBand,
            vegetationIndex = vegetationIndex
        )
    }

    /**
     * Estimate moisture content from NIR spectrum
     */
    private fun estimateMoisture(spectrumData: SpectrumData): Double {
        val wavelengths = spectrumData.wavelengths
        val intensities = spectrumData.intensities
        
        // Use water absorption bands for moisture estimation
        val waterBand1 = calculateSpectralMean(wavelengths, intensities, 1400.0, 1500.0)
        val waterBand2 = calculateSpectralMean(wavelengths, intensities, 1900.0, 2000.0)
        
        // Reference band (less affected by water)
        val referenceBand = calculateSpectralMean(wavelengths, intensities, 800.0, 900.0)
        
        // Moisture index
        val moistureIndex = (waterBand1 + waterBand2) / (2.0 * referenceBand)
        
        // Convert to percentage (0-100%)
        return moistureIndex * 100.0
    }

    /**
     * Apply External Parameter Orthogonalization (EPO) for moisture correction
     * Removes moisture-related variation from color features
     */
    private fun applyEPOCorrection(colorFeatures: ColorFeatures, moisture: Double): ColorFeatures {
        // EPO transformation matrix (simplified)
        // In production, this would be trained on paired data
        
        val moistureFactor = moisture / 100.0
        
        // Correct L*a*b* values based on moisture
        val correctedL = colorFeatures.cieLab.L * (1.0 - 0.1 * moistureFactor)
        val correctedA = colorFeatures.cieLab.a * (1.0 - 0.05 * moistureFactor)
        val correctedB = colorFeatures.cieLab.b * (1.0 - 0.05 * moistureFactor)
        
        // Correct HSV values
        val correctedH = colorFeatures.hsv.h
        val correctedS = colorFeatures.hsv.s * (1.0 - 0.05 * moistureFactor)
        val correctedV = colorFeatures.hsv.v * (1.0 - 0.1 * moistureFactor)
        
        return ColorFeatures(
            cieLab = com.terrochroma.shared.domain.model.CIELab(
                L = correctedL,
                a = correctedA,
                b = correctedB
            ),
            hsv = com.terrochroma.shared.domain.model.HSV(
                h = correctedH,
                s = correctedS,
                v = correctedV
            ),
            munsell = colorFeatures.munsell
        )
    }

    /**
     * Calculate spectral mean for wavelength range
     */
    private fun calculateSpectralMean(
        wavelengths: List<Double>,
        intensities: List<Double>,
        minWavelength: Double,
        maxWavelength: Double
    ): Double {
        val values = wavelengths.zip(intensities)
            .filter { (wavelength, _) -> wavelength in minWavelength..maxWavelength }
            .map { (_, intensity) -> intensity }
        
        return if (values.isNotEmpty()) {
            values.average()
        } else {
            0.0
        }
    }

    /**
     * Calculate vegetation index from NIR spectrum
     */
    private fun calculateVegetationIndex(
        wavelengths: List<Double>,
        intensities: List<Double>
    ): Double {
        val red = calculateSpectralMean(wavelengths, intensities, 650.0, 700.0)
        val nir = calculateSpectralMean(wavelengths, intensities, 750.0, 800.0)
        
        // Simple NDVI-like index
        return (nir - red) / (nir + red + 0.001)
    }

    /**
     * Calculate overall data quality metric
     */
    private fun calculateDataQuality(
        spectrumData: SpectrumData,
        colorFeatures: ColorFeatures
    ): Double {
        var quality = 1.0
        
        // Check spectrum data quality
        if (spectrumData.wavelengths.isEmpty()) {
            quality *= 0.5
        }
        
        // Check for saturated values
        val maxIntensity = spectrumData.intensities.maxOrNull() ?: 0.0
        if (maxIntensity > 0.95) {
            quality *= 0.8
        }
        
        // Check color feature validity
        if (colorFeatures.cieLab.L < 0 || colorFeatures.cieLab.L > 100) {
            quality *= 0.7
        }
        
        return quality
    }
}

data class FusedFeatures(
    val cieLabL: Double,
    val cieLabA: Double,
    val cieLabB: Double,
    val hsvH: Double,
    val hsvS: Double,
    val hsvV: Double,
    val textureContrast: Double,
    val textureHomogeneity: Double,
    val textureEnergy: Double,
    val textureEntropy: Double,
    val nirRedEdge: Double,
    val nirWaterBand: Double,
    val nirVegetationIndex: Double,
    val moistureContent: Double,
    val dataQuality: Double
)

data class NIRFeatures(
    val redEdge: Double,
    val waterBand: Double,
    val vegetationIndex: Double
)
