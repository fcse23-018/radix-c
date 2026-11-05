package com.soilcarbon.engine.data.ml

import android.content.Context
import com.soilcarbon.engine.core.Dispatchers
import com.soilcarbon.engine.domain.model.ModelInfo
import com.soilcarbon.engine.domain.model.ModelMetadata
import com.soilcarbon.engine.domain.repository.ModelRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class DefaultModelRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : ModelRepository {

    override suspend fun loadModel(): ModelInfo = withContext(Dispatchers.io) {
        val sha = ModelAssets.sha256(context)
        val size = ModelAssets.sizeBytes(context)
        val metadata = runCatching { ModelAssets.metadata(context) }.getOrNull()
        if (metadata == null || sha.isBlank()) {
            val fallback = ModelMetadata.fallback()
            return@withContext ModelInfo(
                metadata = fallback,
                sha256 = sha,
                bytes = size,
                loaded = false,
                fallbackReason = "Model asset or sidecar missing/invalid; no SOC inference is available."
            )
        }
        ModelInfo(
            metadata = metadata.toAppModelMetadata(sha),
            sha256 = sha,
            bytes = size,
            loaded = true,
            fallbackReason = if (metadata.isPlaceholder) {
                "Placeholder model: SOC output is blocked in release builds and marked DEMO – NOT VALID in debug."
            } else {
                null
            }
        )
    }

    override suspend fun metadata(): ModelMetadata = withContext(Dispatchers.io) {
        runCatching { ModelAssets.metadata(context).toAppModelMetadata() }
            .getOrElse { ModelMetadata.fallback() }
    }
}
