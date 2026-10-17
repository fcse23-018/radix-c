package com.terrochroma.shared.ml.model

import com.terrochroma.shared.camera.model.CaptureResult
import com.terrochroma.shared.vision.model.ColorFeatures
import com.terrochroma.shared.vision.model.TextureFeatures

data class MLInput(
    val captureResult: CaptureResult,
    val colorFeatures: ColorFeatures,
    val textureFeatures: TextureFeatures,
    val moistureCorrected: Boolean = false
)
