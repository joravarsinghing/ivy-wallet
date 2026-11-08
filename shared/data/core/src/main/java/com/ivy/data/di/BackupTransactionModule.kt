package com.ivy.data.di

import com.ivy.data.backup.BackupTransactionRunner
import com.ivy.data.backup.RoomBackupTransactionRunner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupTransactionModule {
    @Binds
    abstract fun bindBackupTransactionRunner(
        runner: RoomBackupTransactionRunner,
    ): BackupTransactionRunner
}
