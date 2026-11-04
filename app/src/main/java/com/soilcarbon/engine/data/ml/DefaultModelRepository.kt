package com.soilcarbon.engine.data.ml

import android.content.Context
import com.soilcarbon.engine.BuildConfig
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
        val metadata = ModelMetadataLoader.load(context)
        val sha = ModelMetadataLoader.sha256(context)
        val size = runCatching {
            context.assets.open(BuildConfig.MODEL_ASSET).use { it.available().toLong() }
        }.getOrDefault(0L)
        ModelInfo(
            metadata = metadata,
            sha256 = sha,
            bytes = size,
            loaded = metadata.modelId != ModelMetadata.FALLBACK_MODEL_ID && sha.isNotBlank(),
            fallbackReason = if (metadata.modelId == ModelMetadata.FALLBACK_MODEL_ID) {
                "Model asset missing or invalid; heuristic fallback active."
            } else null
        )
    }

    override suspend fun metadata(): ModelMetadata = withContext(Dispatchers.io) {
        ModelMetadataLoader.load(context)
    }
}
