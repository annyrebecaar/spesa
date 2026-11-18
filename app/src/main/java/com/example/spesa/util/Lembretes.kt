package com.example.spesa.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.model.Lembrete
import com.example.spesa.ui.SplashActivity

/**
 * Notificações dos lembretes.
 * Como funciona: pedimos ao AlarmManager do Android para "acordar" o app na hora do aviso.
 * Quando chega a hora, ele chama o AvisoReceiver (abaixo), que mostra a notificação.
 */
object Lembretes {
    private const val CANAL = "lembretes"

    fun criarCanal(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Lembretes", NotificationManager.IMPORTANCE_DEFAULT)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private fun pendente(ctx: Context, lem: Lembrete): PendingIntent {
        val intent = Intent(ctx, AvisoReceiver::class.java)
            .putExtra("titulo", "${lem.tipo.rotulo}: ${lem.texto}")
            .putExtra("texto", "${Datas.diaMes(lem.data)} às ${Datas.hora(lem.data)}")
        return PendingIntent.getBroadcast(ctx, lem.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Marca (ou remarca) o aviso deste lembrete. Não faz nada se os lembretes estão desligados. */
    fun agendar(ctx: Context, lem: Lembrete) {
        val alarmes = ctx.getSystemService(AlarmManager::class.java)
        alarmes.cancel(pendente(ctx, lem))
        if (!Repositorio.preferencia(Repositorio.CHAVE_LEMBRETES_LIGADOS, true) || lem.feito) return
        val hora = lem.data - lem.aviso.minutosAntes * 60_000
        if (hora < System.currentTimeMillis()) return
        alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hora, pendente(ctx, lem))
    }

    /** Usado quando o usuário liga/desliga os lembretes em Ajustes. */
    fun reagendarTodos(ctx: Context) = Repositorio.lembretes.forEach { agendar(ctx, it) }
}

/** Recebe o "despertador" do Android e mostra a notificação. */
class AvisoReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Lembretes.criarCanal(ctx)
        val abrir = PendingIntent.getActivity(ctx, 0, Intent(ctx, SplashActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notificacao = NotificationCompat.Builder(ctx, "lembretes")
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(intent.getStringExtra("titulo"))
            .setContentText(intent.getStringExtra("texto"))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(System.currentTimeMillis().toInt(), notificacao)
        } catch (e: SecurityException) {
            // Sem permissão de notificação: não mostra nada.
        }
    }
}
