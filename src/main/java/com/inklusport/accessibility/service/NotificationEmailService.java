package com.inklusport.accessibility.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * Envía email de cualquier tipo de notificación. Retorna true solo si SMTP aceptó el mensaje.
     * Nota: no usa @Async para poder reportar el resultado real al caller.
     */
    public boolean sendNotificationEmail(String to, String title, String body) {
        if (!mailEnabled) {
            log.debug("Email deshabilitado (mail.enabled=false). No se envía a {}", to);
            return false;
        }
        if (to == null || !to.contains("@") || fromEmail == null || fromEmail.isBlank()) {
            log.warn("No se puede enviar email de notificación: destinatario/remitente inválido");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("InkluSport: " + title);
            helper.setText(buildHtml(title, body), true);
            mailSender.send(message);
            log.info("Email de notificación enviado a {}", to);
            return true;
        } catch (Exception e) {
            log.error("Error enviando email de notificación a {}: {}", to, e.getMessage());
            return false;
        }
    }

    private String buildHtml(String title, String body) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background:#f8fafc; padding:24px;">
              <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;padding:24px;border:1px solid #e2e8f0;">
                <h2 style="color:#A30D11;margin:0 0 12px;">InkluSport</h2>
                <h3 style="color:#0f172a;margin:0 0 12px;">%s</h3>
                <p style="color:#334155;line-height:1.5;">%s</p>
                <p style="color:#64748b;font-size:13px;margin-top:24px;">Revisa también el apartado Notificaciones en la aplicación.</p>
              </div>
            </body>
            </html>
            """.formatted(escape(title), escape(body).replace("\n", "<br/>"));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
