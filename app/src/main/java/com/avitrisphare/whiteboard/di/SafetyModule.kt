package com.avitrisphare.whiteboard.di

import android.content.Context
import com.avitrisphare.whiteboard.data.local.AuditLogStore
import com.avitrisphare.whiteboard.data.local.SchoolProfanityStore
import com.avitrisphare.whiteboard.domain.safety.AuditLogRepository
import com.avitrisphare.whiteboard.domain.safety.ProfanityFilter
import com.avitrisphare.whiteboard.domain.safety.ProfanityFilterService
import com.avitrisphare.whiteboard.domain.safety.SafetyDictionaryProvider
import com.avitrisphare.whiteboard.domain.safety.SchoolProfanityRepository
import com.google.gson.Gson

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SafetyModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()


    @Provides
    @Singleton
    fun provideAuditLogRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): AuditLogRepository {
        return AuditLogStore(context, gson)
    }

    @Provides
    @Singleton
    fun provideSchoolProfanityRepository(
        @ApplicationContext context: Context,
        auditLogRepository: AuditLogRepository,
        gson: Gson
    ): SchoolProfanityRepository {
        return SchoolProfanityStore(context, auditLogRepository, gson)
    }

    @Provides
    @Singleton
    fun provideSafetyDictionaryProvider(
        @ApplicationContext context: Context,
        gson: Gson
    ): SafetyDictionaryProvider {
        return SafetyDictionaryProvider(context, gson)
    }

    @Provides
    @Singleton
    fun provideProfanityFilter(
        schoolProfanityRepository: SchoolProfanityRepository,
        dictionaryProvider: SafetyDictionaryProvider
    ): ProfanityFilter {
        return ProfanityFilter(schoolProfanityRepository, dictionaryProvider)
    }


    @Provides
    @Singleton
    fun provideProfanityFilterService(
        profanityFilter: ProfanityFilter,
        auditLogRepository: AuditLogRepository,
        schoolProfanityRepository: SchoolProfanityRepository
    ): ProfanityFilterService {
        return ProfanityFilterService(
            profanityFilter = profanityFilter,
            auditLogRepository = auditLogRepository,
            schoolProfanityRepository = schoolProfanityRepository
        )
    }
}
