package com.soilcarbon.engine.di

import android.content.Context
import androidx.room.Room
import com.soilcarbon.engine.BuildConfig
import com.soilcarbon.engine.data.calibration.CalibrationSpecLoader
import com.soilcarbon.engine.data.camera.CameraController
import com.soilcarbon.engine.data.export.DefaultExportRepository
import com.soilcarbon.engine.data.local.CalibrationDao
import com.soilcarbon.engine.data.local.CovariateCacheDao
import com.soilcarbon.engine.data.local.SampleDao
import com.soilcarbon.engine.data.local.SoilCarbonDatabase
import com.soilcarbon.engine.data.location.LocationProvider
import com.soilcarbon.engine.data.ml.DefaultModelRepository
import com.soilcarbon.engine.data.ml.TfliteSocPredictor
import com.soilcarbon.engine.data.remote.ApiClientFactory
import com.soilcarbon.engine.data.remote.OpenMeteoClient
import com.soilcarbon.engine.data.remote.SentinelNdviClient
import com.soilcarbon.engine.data.remote.SoilGridsClient
import com.soilcarbon.engine.data.repository.DefaultCalibrationRepository
import com.soilcarbon.engine.data.repository.DefaultCovariateRepository
import com.soilcarbon.engine.data.repository.DefaultSampleRepository
import com.soilcarbon.engine.data.repository.DefaultSettingsRepository
import com.soilcarbon.engine.data.security.EncryptedFileStore
import com.soilcarbon.engine.data.security.KeystoreSecretStore
import com.soilcarbon.engine.data.sensors.ImuMonitor
import com.soilcarbon.engine.data.sensors.LightMonitor
import com.soilcarbon.engine.data.vision.ArUcoTargetDetector
import com.soilcarbon.engine.data.vision.TargetImageProcessor
import com.soilcarbon.engine.domain.calibration.CalibrationTargetSpec
import com.soilcarbon.engine.domain.ml.SocPredictor
import com.soilcarbon.engine.domain.repository.CalibrationRepository
import com.soilcarbon.engine.domain.repository.CovariateRepository
import com.soilcarbon.engine.domain.repository.ExportRepository
import com.soilcarbon.engine.domain.repository.ModelRepository
import com.soilcarbon.engine.domain.repository.SampleRepository
import com.soilcarbon.engine.domain.repository.SettingsRepository
import com.soilcarbon.engine.domain.usecase.AnalyzeSampleUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSecretStore(@ApplicationContext context: Context): KeystoreSecretStore =
        KeystoreSecretStore(context)

    @Provides
    @Singleton
    fun provideEncryptedFileStore(
        @ApplicationContext context: Context,
        secretStore: KeystoreSecretStore
    ): EncryptedFileStore = EncryptedFileStore(File(context.filesDir, "secure"), secretStore)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        secretStore: KeystoreSecretStore
    ): SoilCarbonDatabase = Room.databaseBuilder(
        context,
        SoilCarbonDatabase::class.java,
        SoilCarbonDatabase.NAME
    )
        .openHelperFactory(SupportOpenHelperFactory(secretStore.databasePassphrase()))
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideSampleDao(database: SoilCarbonDatabase): SampleDao = database.sampleDao()

    @Provides
    fun provideCalibrationDao(database: SoilCarbonDatabase): CalibrationDao =
        database.calibrationDao()

    @Provides
    fun provideCovariateCacheDao(database: SoilCarbonDatabase): CovariateCacheDao =
        database.covariateCacheDao()

    @Provides
    @Singleton
    fun provideSoilGridsClient(): SoilGridsClient =
        SoilGridsClient(ApiClientFactory.soilGrids())

    @Provides
    @Singleton
    fun provideOpenMeteoClient(): OpenMeteoClient =
        OpenMeteoClient(ApiClientFactory.openMeteo())

    @Provides
    @Singleton
    fun provideSentinelNdviClient(@ApplicationContext context: Context): SentinelNdviClient {
        val token = context.getSharedPreferences("soilcarbon_secrets", Context.MODE_PRIVATE)
            .getString("sentinel_token", "") ?: ""
        return SentinelNdviClient(ApiClientFactory.sentinel(), token)
    }

    @Provides
    @Singleton
    fun provideTflitePredictor(
        @ApplicationContext context: Context
    ): TfliteSocPredictor = TfliteSocPredictor(context)

    @Provides
    @Singleton
    fun provideSocPredictor(predictor: TfliteSocPredictor): SocPredictor = predictor

    @Provides
    @Singleton
    fun provideAnalyzeSampleUseCase(predictor: SocPredictor): AnalyzeSampleUseCase =
        AnalyzeSampleUseCase(predictor)

    @Provides
    @Singleton
    fun provideCameraController(@ApplicationContext context: Context): CameraController =
        CameraController(context)

    @Provides
    @Singleton
    fun provideImuMonitor(@ApplicationContext context: Context): ImuMonitor = ImuMonitor(context)

    @Provides
    @Singleton
    fun provideLightMonitor(@ApplicationContext context: Context): LightMonitor = LightMonitor(context)

    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider =
        LocationProvider(context)

    @Provides
    @Singleton
    fun provideCalibrationSpec(@ApplicationContext context: Context): CalibrationTargetSpec =
        CalibrationSpecLoader.load(context)

    @Provides
    @Singleton
    fun provideArUcoDetector(): ArUcoTargetDetector = ArUcoTargetDetector()

    @Provides
    @Singleton
    fun provideTargetImageProcessor(): TargetImageProcessor = TargetImageProcessor()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSampleRepository(impl: DefaultSampleRepository): SampleRepository

    @Binds
    @Singleton
    abstract fun bindCalibrationRepository(impl: DefaultCalibrationRepository): CalibrationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DefaultSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCovariateRepository(impl: DefaultCovariateRepository): CovariateRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: DefaultModelRepository): ModelRepository

    @Binds
    @Singleton
    abstract fun bindExportRepository(impl: DefaultExportRepository): ExportRepository
}
