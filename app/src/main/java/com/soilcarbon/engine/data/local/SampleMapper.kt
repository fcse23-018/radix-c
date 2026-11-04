package com.soilcarbon.engine.data.local

import com.soilcarbon.engine.domain.model.CalibrationSummary
import com.soilcarbon.engine.domain.model.CameraExposureParameters
import com.soilcarbon.engine.domain.model.CovariateSource
import com.soilcarbon.engine.domain.model.CovariateValue
import com.soilcarbon.engine.domain.model.EnvironmentalCovariates
import com.soilcarbon.engine.domain.model.FeatureVector
import com.soilcarbon.engine.domain.model.GeoPoint
import com.soilcarbon.engine.domain.model.ImuReading
import com.soilcarbon.engine.domain.model.LightReading
import com.soilcarbon.engine.domain.model.MoistureMethod
import com.soilcarbon.engine.domain.model.Prediction
import com.soilcarbon.engine.domain.model.SampleStatus
import com.soilcarbon.engine.domain.model.SoilProperties
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.model.SyncState
import com.soilcarbon.engine.domain.model.UserRole
import com.soilcarbon.engine.domain.vision.ColorFeatures
import com.soilcarbon.engine.domain.vision.HsvColor
import com.soilcarbon.engine.domain.vision.LabColor
import com.soilcarbon.engine.domain.vision.MoistureEstimate
import com.soilcarbon.engine.domain.vision.MunsellColor
import com.soilcarbon.engine.domain.vision.ReflectanceRatios
import com.soilcarbon.engine.domain.vision.ReflectanceRgb
import com.soilcarbon.engine.domain.vision.TextureFeatures
import kotlin.math.max
import kotlin.math.min

object SampleMapper {

    fun toEntity(sample: SoilSample): SampleEntity {
        val color = sample.color
        val texture = sample.texture
        val prediction = sample.prediction
        val features = sample.features
        val covariates = sample.covariates
        val calibration = sample.calibration
        return SampleEntity(
            id = sample.id,
            tag = sample.tag,
            notes = sample.notes,
            role = sample.role.name,
            createdAtMillis = sample.createdAtMillis,
            capturedAtMillis = sample.capturedAtMillis,
            status = sample.status.name,
            syncState = sample.syncState.name,
            latitude = sample.location?.latitude,
            longitude = sample.location?.longitude,
            altitudeMeters = sample.location?.altitudeMeters,
            accuracyMeters = sample.location?.accuracyMeters,
            locationProvider = sample.location?.provider,
            locationTimestampMillis = sample.location?.timestampMillis,
            pitchDegrees = sample.imu?.pitchDegrees,
            rollDegrees = sample.imu?.rollDegrees,
            azimuthDegrees = sample.imu?.azimuthDegrees,
            imuTimestampMillis = sample.imu?.timestampMillis,
            lux = sample.light?.lux,
            lightTimestampMillis = sample.light?.timestampMillis,
            iso = sample.exposure?.iso,
            exposureTimeNs = sample.exposure?.exposureTimeNs,
            focusDistanceDiopters = sample.exposure?.focusDistanceDiopters,
            whiteBalanceKelvin = sample.exposure?.whiteBalanceKelvin,
            autoExposureLocked = sample.exposure?.autoExposureLocked,
            autoWhiteBalanceLocked = sample.exposure?.autoWhiteBalanceLocked,
            autoFocusLocked = sample.exposure?.autoFocusLocked,
            sensorOrientationDegrees = sample.exposure?.sensorOrientationDegrees,
            focalLengthMm = sample.exposure?.focalLengthMm,
            rawCaptureAvailable = sample.exposure?.rawCaptureAvailable,
            rawJpegMode = sample.exposure?.rawJpegMode,
            jpegQuality = sample.exposure?.jpegQuality,
            deviceModel = sample.exposure?.deviceModel,
            deviceManufacturer = sample.exposure?.deviceManufacturer,
            androidApiLevel = sample.exposure?.androidApiLevel,
            rawImagePath = sample.rawImagePath,
            jpegImagePath = sample.jpegImagePath,
            cardJpegPath = sample.cardJpegPath,
            apertureCropPath = sample.apertureCropPath,
            rawSha256 = sample.rawSha256,
            jpegSha256 = sample.jpegSha256,
            calCardVersion = calibration?.cardVersion,
            calSource = calibration?.sourceDescription,
            calOverallRmse = calibration?.overallRmse,
            calWorstRSquared = calibration?.worstRSquared,
            calIlluminationVariation = calibration?.illuminationVariation,
            calQuality = calibration?.qualityLabel,
            calRedGain = calibration?.redGain,
            calRedOffset = calibration?.redOffset,
            calGreenGain = calibration?.greenGain,
            calGreenOffset = calibration?.greenOffset,
            calBlueGain = calibration?.blueGain,
            calBlueOffset = calibration?.blueOffset,
            calCreatedAtMillis = calibration?.createdAtMillis,
            labL = color?.lab?.l,
            labA = color?.lab?.a,
            labB = color?.lab?.b,
            hsvHue = color?.hsv?.hueDegrees,
            hsvSaturation = color?.hsv?.saturation,
            hsvValue = color?.hsv?.value,
            reflectanceR = color?.reflectance?.r,
            reflectanceG = color?.reflectance?.g,
            reflectanceB = color?.reflectance?.b,
            redOverBlue = color?.ratios?.redOverBlue,
            normalizedDifference = color?.ratios?.normalizedDifference,
            rednessIndex = color?.ratios?.rednessIndex,
            brightness = color?.ratios?.brightness,
            samplePixelCount = color?.samplePixelCount,
            excludedPixelFraction = color?.excludedPixelFraction,
            munsellHueFamily = color?.munsell?.hueFamily,
            munsellHue = color?.munsell?.hue,
            munsellValue = color?.munsell?.value,
            munsellChroma = color?.munsell?.chroma,
            textureContrast = texture?.contrast,
            textureEnergy = texture?.energy,
            textureHomogeneity = texture?.homogeneity,
            textureCorrelation = texture?.correlation,
            textureEntropy = texture?.entropy,
            textureDissimilarity = texture?.dissimilarity,
            textureMeanGray = texture?.meanGray,
            textureGrayLevels = texture?.grayLevels,
            textureOrientations = texture?.orientations,
            textureOffsetPixels = texture?.offsetPixels,
            moisturePercent = sample.moisture?.percentGravimetric,
            moistureMethod = sample.moisture?.method?.name,
            moistureConfidence = sample.moisture?.confidence,
            moistureSource = sample.moisture?.source,
            moistureCorrectionNotes = sample.moistureCorrectionNotes,
            featureL = features?.lStar,
            featureA = features?.aStar,
            featureB = features?.bStar,
            featureMunsellValue = features?.munsellValue,
            featureGlcmContrast = features?.glcmContrast,
            featureGlcmEnergy = features?.glcmEnergy,
            featureNdvi = features?.ndviSatellite,
            featureClay = features?.clayContentPercent,
            predictionSoc = prediction?.socPercent,
            predictionSom = prediction?.somPercent,
            predictionLower95 = prediction?.socLower95,
            predictionUpper95 = prediction?.socUpper95,
            predictionUncertainty95 = prediction?.uncertainty95,
            predictionConfidence = prediction?.confidenceScore,
            predictionStockMgPerHa = prediction?.carbonStockMgPerHa,
            predictionModelId = prediction?.modelId,
            predictionModelVersion = prediction?.modelVersion,
            predictionInferenceMillis = prediction?.inferenceMillis,
            predictionFallbackUsed = prediction?.fallbackUsed,
            predictionNotes = prediction?.notes,
            covNdvi = covariates?.ndvi?.value,
            covNdviSource = covariates?.ndvi?.source?.name,
            covClay = covariates?.clayPercent?.value,
            covClaySource = covariates?.clayPercent?.source?.name,
            covSocBaseline = covariates?.socBaselinePercent?.value,
            covSocBaselineSource = covariates?.socBaselinePercent?.source?.name,
            covElevation = covariates?.elevationMeters?.value,
            covElevationSource = covariates?.elevationMeters?.source?.name,
            covPrecip = covariates?.meanAnnualPrecipMm?.value,
            covPrecipSource = covariates?.meanAnnualPrecipMm?.source?.name,
            covTemp = covariates?.meanAnnualTempC?.value,
            covTempSource = covariates?.meanAnnualTempC?.source?.name,
            covOfflineFallback = covariates?.isOfflineFallback,
            covFetchedAtMillis = covariates?.soilGridsFetchedAtMillis,
            bulkDensityGcm3 = sample.soil.bulkDensityGcm3,
            depthCm = sample.soil.depthCm,
            provenanceSha256 = sample.provenanceSha256,
            modelId = sample.modelId,
            modelVersion = sample.modelVersion,
            exportedAtMillis = sample.exportedAtMillis,
            syncedAtMillis = sample.syncedAtMillis,
            errorMessage = sample.errorMessage
        )
    }

    fun toDomain(entity: SampleEntity): SoilSample {
        val location = if (entity.latitude != null && entity.longitude != null) {
            GeoPoint(
                latitude = entity.latitude,
                longitude = entity.longitude,
                altitudeMeters = entity.altitudeMeters,
                accuracyMeters = entity.accuracyMeters,
                provider = entity.locationProvider ?: "unknown",
                timestampMillis = entity.locationTimestampMillis ?: entity.createdAtMillis
            )
        } else null

        val imu = if (entity.pitchDegrees != null && entity.rollDegrees != null) {
            ImuReading(
                pitchDegrees = entity.pitchDegrees,
                rollDegrees = entity.rollDegrees,
                azimuthDegrees = entity.azimuthDegrees ?: 0f,
                timestampMillis = entity.imuTimestampMillis ?: entity.createdAtMillis
            )
        } else null

        val light = entity.lux?.let {
            LightReading(it, entity.lightTimestampMillis ?: entity.createdAtMillis)
        }

        val exposure = if (entity.iso != null && entity.exposureTimeNs != null) {
            CameraExposureParameters(
                iso = entity.iso,
                exposureTimeNs = entity.exposureTimeNs,
                focusDistanceDiopters = entity.focusDistanceDiopters ?: 6.67f,
                whiteBalanceKelvin = entity.whiteBalanceKelvin ?: 6500,
                autoExposureLocked = entity.autoExposureLocked ?: true,
                autoWhiteBalanceLocked = entity.autoWhiteBalanceLocked ?: true,
                autoFocusLocked = entity.autoFocusLocked ?: true,
                sensorOrientationDegrees = entity.sensorOrientationDegrees ?: 90,
                focalLengthMm = entity.focalLengthMm,
                rawCaptureAvailable = entity.rawCaptureAvailable ?: false,
                rawJpegMode = entity.rawJpegMode ?: false,
                jpegQuality = entity.jpegQuality ?: 100,
                deviceModel = entity.deviceModel ?: "unknown",
                deviceManufacturer = entity.deviceManufacturer ?: "unknown",
                androidApiLevel = entity.androidApiLevel ?: 0
            )
        } else null

        val color = if (entity.labL != null && entity.labA != null && entity.labB != null) {
            ColorFeatures(
                lab = LabColor(entity.labL, entity.labA, entity.labB),
                hsv = HsvColor(
                    entity.hsvHue ?: 0.0,
                    entity.hsvSaturation ?: 0.0,
                    entity.hsvValue ?: 0.0
                ),
                munsell = MunsellColor(
                    hueFamily = entity.munsellHueFamily ?: "YR",
                    hue = entity.munsellHue ?: 0.0,
                    value = entity.munsellValue ?: 0.0,
                    chroma = entity.munsellChroma ?: 0.0
                ),
                reflectance = ReflectanceRgb(
                    entity.reflectanceR ?: 0.0,
                    entity.reflectanceG ?: 0.0,
                    entity.reflectanceB ?: 0.0
                ),
                ratios = ReflectanceRatios(
                    redOverBlue = entity.redOverBlue ?: 0.0,
                    normalizedDifference = entity.normalizedDifference ?: 0.0,
                    rednessIndex = entity.rednessIndex ?: 0.0,
                    brightness = entity.brightness ?: 0.0
                ),
                samplePixelCount = entity.samplePixelCount ?: 0,
                excludedPixelFraction = entity.excludedPixelFraction ?: 0.0
            )
        } else null

        val texture = if (entity.textureContrast != null && entity.textureEnergy != null) {
            TextureFeatures(
                contrast = entity.textureContrast,
                energy = entity.textureEnergy,
                homogeneity = entity.textureHomogeneity ?: 0.0,
                correlation = entity.textureCorrelation ?: 0.0,
                entropy = entity.textureEntropy ?: 0.0,
                dissimilarity = entity.textureDissimilarity ?: 0.0,
                meanGray = entity.textureMeanGray ?: 0.0,
                grayLevels = entity.textureGrayLevels ?: 32,
                orientations = entity.textureOrientations ?: 4,
                offsetPixels = entity.textureOffsetPixels ?: 1
            )
        } else null

        val moisture = if (entity.moisturePercent != null) {
            MoistureEstimate(
                percentGravimetric = entity.moisturePercent,
                method = enumOrDefault(entity.moistureMethod, MoistureMethod.NONE),
                confidence = entity.moistureConfidence ?: 0.0,
                source = entity.moistureSource ?: "unknown"
            )
        } else null

        val features = if (entity.featureL != null && entity.featureA != null && entity.featureB != null) {
            FeatureVector.fromValues(
                doubleArrayOf(
                    entity.featureL,
                    entity.featureA,
                    entity.featureB,
                    entity.featureMunsellValue ?: 0.0,
                    entity.featureGlcmContrast ?: 0.0,
                    entity.featureGlcmEnergy ?: 1.0,
                    entity.featureNdvi ?: 0.0,
                    entity.featureClay ?: 0.0
                )
            )
        } else null

        val prediction = if (entity.predictionSoc != null) {
            val soc = entity.predictionSoc
            val lower = min(entity.predictionLower95 ?: 0.0, soc)
            val upper = max(entity.predictionUpper95 ?: soc, soc)
            Prediction(
                socPercent = soc.coerceAtLeast(0.0),
                somPercent = max(entity.predictionSom ?: soc * 1.724, soc),
                socLower95 = lower.coerceAtLeast(0.0),
                socUpper95 = upper,
                uncertainty95 = entity.predictionUncertainty95 ?: (upper - soc),
                confidenceScore = (entity.predictionConfidence ?: 0.0).coerceIn(0.0, 1.0),
                carbonStockMgPerHa = entity.predictionStockMgPerHa,
                modelId = entity.predictionModelId ?: entity.modelId ?: "unknown",
                modelVersion = entity.predictionModelVersion ?: entity.modelVersion ?: "unknown",
                inferenceMillis = entity.predictionInferenceMillis ?: 0L,
                fallbackUsed = entity.predictionFallbackUsed ?: true,
                notes = entity.predictionNotes ?: ""
            )
        } else null

        val covariates = if (entity.covNdvi != null && entity.covClay != null) {
            EnvironmentalCovariates(
                ndvi = CovariateValue(
                    entity.covNdvi,
                    enumOrDefault(entity.covNdviSource, CovariateSource.OFFLINE_CACHE)
                ),
                clayPercent = CovariateValue(
                    entity.covClay,
                    enumOrDefault(entity.covClaySource, CovariateSource.OFFLINE_CACHE)
                ),
                socBaselinePercent = CovariateValue(
                    entity.covSocBaseline ?: 1.5,
                    enumOrDefault(entity.covSocBaselineSource, CovariateSource.REGIONAL_DEFAULT)
                ),
                elevationMeters = entity.covElevation?.let {
                    CovariateValue(it, enumOrDefault(entity.covElevationSource, CovariateSource.DERIVED))
                },
                meanAnnualPrecipMm = entity.covPrecip?.let {
                    CovariateValue(it, enumOrDefault(entity.covPrecipSource, CovariateSource.OFFLINE_CACHE))
                },
                meanAnnualTempC = entity.covTemp?.let {
                    CovariateValue(it, enumOrDefault(entity.covTempSource, CovariateSource.OFFLINE_CACHE))
                },
                soilGridsFetchedAtMillis = entity.covFetchedAtMillis,
                isOfflineFallback = entity.covOfflineFallback ?: true
            )
        } else null

        val calibration = if (entity.calRedGain != null && entity.calRedOffset != null) {
            CalibrationSummary(
                cardVersion = entity.calCardVersion ?: "unknown",
                sourceDescription = entity.calSource ?: "unknown",
                overallRmse = entity.calOverallRmse ?: 0.0,
                worstRSquared = entity.calWorstRSquared ?: 0.0,
                illuminationVariation = entity.calIlluminationVariation ?: 1.0,
                qualityLabel = entity.calQuality ?: "Unknown",
                redGain = entity.calRedGain,
                redOffset = entity.calRedOffset,
                greenGain = entity.calGreenGain ?: 1.0,
                greenOffset = entity.calGreenOffset ?: 0.0,
                blueGain = entity.calBlueGain ?: 1.0,
                blueOffset = entity.calBlueOffset ?: 0.0,
                createdAtMillis = entity.calCreatedAtMillis ?: entity.createdAtMillis
            )
        } else null

        return SoilSample(
            id = entity.id,
            tag = entity.tag,
            notes = entity.notes,
            role = enumOrDefault(entity.role, UserRole.FIELD_TECHNICIAN),
            createdAtMillis = entity.createdAtMillis,
            capturedAtMillis = entity.capturedAtMillis,
            status = enumOrDefault(entity.status, SampleStatus.DRAFT),
            syncState = enumOrDefault(entity.syncState, SyncState.LOCAL_ONLY),
            location = location,
            imu = imu,
            light = light,
            exposure = exposure,
            rawImagePath = entity.rawImagePath,
            jpegImagePath = entity.jpegImagePath,
            cardJpegPath = entity.cardJpegPath,
            apertureCropPath = entity.apertureCropPath,
            rawSha256 = entity.rawSha256,
            jpegSha256 = entity.jpegSha256,
            calibration = calibration,
            color = color,
            texture = texture,
            lab = color?.lab,
            munsell = color?.munsell,
            reflectance = color?.reflectance,
            moisture = moisture,
            moistureCorrectionNotes = entity.moistureCorrectionNotes,
            features = features,
            prediction = prediction,
            covariates = covariates,
            soil = SoilProperties(entity.bulkDensityGcm3, entity.depthCm),
            provenanceSha256 = entity.provenanceSha256,
            modelId = entity.modelId,
            modelVersion = entity.modelVersion,
            exportedAtMillis = entity.exportedAtMillis,
            syncedAtMillis = entity.syncedAtMillis,
            errorMessage = entity.errorMessage
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        if (name == null) default else enumValues<T>().firstOrNull { it.name == name } ?: default
}
