package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object CookTokNotificationHelper {

    const val CHANNEL_ID_RECIPES = "cooktok_recipes_channel"
    const val CHANNEL_ID_FOLLOWED_CHEFS = "cooktok_followed_chefs_channel" // <-- ADICIONADO AQUI

    const val EXTRA_RECIPE_ID = "extra_recipe_id"
    const val EXTRA_OPEN_DESTINATION = "extra_open_destination"

    /**
     * Deve ser chamado uma única vez no arranque da app (Application.onCreate()).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Canal para as tuas próprias publicações
            val ownPostsChannel = NotificationChannel(
                CHANNEL_ID_RECIPES,
                "As Tuas Publicações",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Confirmação quando publicas uma nova receita no CookTok."
            }
            manager.createNotificationChannel(ownPostsChannel)

            // 2. Canal para receitas de Chefs que segues (WorkManager em background) <-- ADICIONADO AQUI
            val followedChannel = NotificationChannel(
                CHANNEL_ID_FOLLOWED_CHEFS,
                "Novas Receitas de Chefs Seguidos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifica quando um Chef que segues publica um novo CookTok."
            }
            manager.createNotificationChannel(followedChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 1. Notificação imediata quando TU publicas uma receita (chamada no CreationScreen).
     */
    fun notifyRecipePublished(
        context: Context,
        recipeId: String,
        recipeTitle: String,
        locationTag: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val pendingIntent = buildRecipePendingIntent(context, recipeId) ?: return

        val contentBody = if (locationTag.isNotBlank()) {
            "\"$recipeTitle\" ($locationTag) já está disponível no CookTok."
        } else {
            "\"$recipeTitle\" já está disponível em Receitas Criadas."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_RECIPES)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Receita Publicada no CookTok!")
            .setContentText(contentBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Ver Receita",
                pendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(recipeId.hashCode(), notification)
    }

    /**
     * 2. Notificação em background quando um CHEF SEGUIDO publica uma receita (chamada pelo WorkManager).
     * <-- ADICIONADO AQUI
     */
    fun notifyNewRecipeFromFollowedChef(
        context: Context,
        recipeId: String,
        chefUsername: String,
        recipeTitle: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val pendingIntent = buildRecipePendingIntent(context, recipeId) ?: return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOLLOWED_CHEFS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Novo CookTok de @$chefUsername!")
            .setContentText("Acabou de publicar \"$recipeTitle\". Toca para ver a receita.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Abrir Receita",
                pendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(recipeId.hashCode(), notification)
    }

    private fun buildRecipePendingIntent(context: Context, recipeId: String): PendingIntent? {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_DESTINATION, "recipe_detail")
                putExtra(EXTRA_RECIPE_ID, recipeId)
            } ?: return null

        return PendingIntent.getActivity(
            context,
            recipeId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}