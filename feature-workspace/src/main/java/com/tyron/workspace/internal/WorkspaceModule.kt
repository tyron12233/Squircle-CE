package com.tyron.workspace.internal

import com.blacksquircle.ui.core.data.storage.database.AppDatabase
import com.blacksquircle.ui.core.domain.coroutine.DispatcherProvider
import com.tyron.workspace.data.repository.WorkspaceRepositoryImpl
import com.tyron.workspace.domain.repository.WorkspaceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class WorkspaceModule {

    @Provides
    @ViewModelScoped
    fun provideWorkspaceRepository(
        dispatcherProvider: DispatcherProvider,
        appDatabase: AppDatabase,
    ): WorkspaceRepository {
        return WorkspaceRepositoryImpl(dispatcherProvider, appDatabase)
    }
}