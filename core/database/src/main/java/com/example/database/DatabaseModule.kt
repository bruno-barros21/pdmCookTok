package com.example.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCookTokDatabase(
        @ApplicationContext context: Context
    ): CookTokDatabase = Room.databaseBuilder(
        context,
        CookTokDatabase::class.java,
        "cooktok.db"
    ).build()

    @Provides
    fun provideRecipeDao(database: CookTokDatabase): RecipeDao = database.recipeDao()
}