package com.terrochroma.shared.ml

import com.terrochroma.shared.ml.model.MLInput
import com.terrochroma.shared.ml.model.MLOutput

interface MLService {
    suspend fun initialize(): Result<Unit>
    suspend fun runInference(input: MLInput): Result<MLOutput>
    suspend fun release(): Result<Unit>
}
