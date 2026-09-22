package com.example.pugprint.imaging

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/** Where the image pipeline (decode → crop → dither) runs; tests substitute a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ImagingDispatcher

@Module
@InstallIn(SingletonComponent::class)
object ImagingModule {
    @Provides
    @Singleton
    fun photoSource(
        handoff: DrawingHandoff,
        resolver: ContentResolverPhotoSource,
    ): PhotoSource = HandoffPhotoSource(handoff, resolver)

    @Provides
    @ImagingDispatcher
    fun imagingDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
