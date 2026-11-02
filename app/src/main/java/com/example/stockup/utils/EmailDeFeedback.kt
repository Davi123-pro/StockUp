package com.example.stockup.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Abre o app de e-mail do celular com destinatário, assunto e corpo prontos.
 *
 * Vive aqui (e não dentro de uma tela) porque dois lugares precisam disso:
 * o botão "Feedback" das Configurações e o "não achei minha dúvida" da Central
 * de Ajuda. Duplicar a lógica faria os dois caminhos divergirem com o tempo.
 *
 * `mailto:` em vez de `ACTION_SEND` de propósito: `SEND` ofereceria WhatsApp,
 * Drive e bloco de notas — o feedback viraria um arquivo solto em vez de e-mail.
 */
object EmailDeFeedback {

    /** Endereço do dono do app. É para cá que as mensagens vão. */
    const val DESTINATARIO = "davimaculada@gmail.com"

    /**
     * @param assunto assunto do e-mail (já vai preenchido)
     * @param corpo mensagem inicial, para a pessoa só completar
     */
    fun abrir(
        context: Context,
        assunto: String = "Feedback do StockUp",
        corpo: String = "Conte aqui o que você achou do app (elogio, problema ou ideia):\n\n\n\n—\nEnviado pelo StockUp"
    ) {
        val email = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(DESTINATARIO))
            putExtra(Intent.EXTRA_SUBJECT, assunto)
            putExtra(Intent.EXTRA_TEXT, corpo)
        }

        val abriu = runCatching { context.startActivity(email) }.isSuccess
        if (!abriu) {
            Toast.makeText(
                context,
                "Não encontrei um app de e-mail neste celular.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
