package org.artemchik.newmrim.di

import android.content.Context
import androidx.room.Room
import org.artemchik.newmrim.data.SettingsDataStore
import org.artemchik.newmrim.db.MessageDao
import org.artemchik.newmrim.db.MrimDatabase
import org.artemchik.newmrim.protocol.MrimClient
import org.artemchik.newmrim.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMrimClient(): MrimClient = MrimClient()

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore = SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MrimDatabase = Room.databaseBuilder(
        context,
        MrimDatabase::class.java,
        "mrim_messages.db"
    ).build()

    @Provides
    @Singleton
    fun provideMessageDao(db: MrimDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideChatRepository(
        mrimClient: MrimClient,
        messageDao: MessageDao,
        @ApplicationContext context: Context
    ): ChatRepository = ChatRepository(mrimClient, messageDao, context)
}