package org.example.service;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Resource(name = "mail/default")
    private Session mailSession;

    /**
     * Wysyła email z potwierdzeniem złożenia zamówienia
     */
    public void sendOrderConfirmation(Order order) throws Exception {
        logger.debug("Sending order confirmation email for order: {}", order.getId());

        if (order.getUser().getEmail() == null || order.getUser().getEmail().trim().isEmpty()) {
            logger.warn("User {} has no email address, skipping email", order.getUser().getUsername());
            return;
        }

        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress("noreply@shop.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(order.getUser().getEmail()));
            message.setSubject("Potwierdzenie zamówienia #" + order.getId() + " - Sklep Online");

            String emailContent = buildOrderConfirmationContent(order);
            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Order confirmation email sent successfully for order: {}", order.getId());

        } catch (MessagingException e) {
            logger.error("Failed to send order confirmation email for order: {}", order.getId(), e);
            throw new Exception("Nie udało się wysłać email z potwierdzeniem", e);
        }
    }

    /**
     * Buduje treść HTML dla email z potwierdzeniem zamówienia
     */
    private String buildOrderConfirmationContent(Order order) {
        StringBuilder content = new StringBuilder();

        content.append("<!DOCTYPE html>");
        content.append("<html><head><meta charset='utf-8'>");
        content.append("<style>");
        content.append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f8f9fa; }");
        content.append(".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        content.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 2rem; text-align: center; }");
        content.append(".content { padding: 2rem; }");
        content.append(".order-details { background: #f8f9fa; padding: 1.5rem; border-radius: 8px; margin: 1.5rem 0; }");
        content.append(".item-table { width: 100%; border-collapse: collapse; margin: 1rem 0; }");
        content.append(".item-table th, .item-table td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #e9ecef; }");
        content.append(".item-table th { background: #f8f9fa; font-weight: bold; }");
        content.append(".total-row { background: #e7f3ff; font-weight: bold; }");
        content.append(".footer { background: #f8f9fa; padding: 1rem; text-align: center; color: #666; font-size: 0.9rem; }");
        content.append("</style>");
        content.append("</head><body>");

        content.append("<div class='container'>");

        // Header
        content.append("<div class='header'>");
        content.append("<h1>🛍️ Sklep Online</h1>");
        content.append("<h2>Dziękujemy za złożenie zamówienia!</h2>");
        content.append("</div>");

        // Content
        content.append("<div class='content'>");
        content.append("<p>Szanowny/a <strong>").append(order.getUser().getFullName()).append("</strong>,</p>");
        content.append("<p>Twoje zamówienie zostało pomyślnie złożone i zostanie zrealizowane w najbliższym czasie.</p>");

        // Order details
        content.append("<div class='order-details'>");
        content.append("<h3>📋 Szczegóły zamówienia #").append(order.getId()).append("</h3>");
        content.append("<p><strong>📅 Data zamówienia:</strong> ").append(order.getOrderDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("</p>");
        content.append("<p><strong>📍 Adres dostawy:</strong><br>").append(order.getShippingAddress().replace("\n", "<br>")).append("</p>");
        content.append("<p><strong>📦 Status:</strong> ").append(order.getStatus().getDisplayName()).append("</p>");
        content.append("</div>");

        // Items table
        content.append("<h3>🛒 Zamówione produkty:</h3>");
        content.append("<table class='item-table'>");
        content.append("<tr><th>Produkt</th><th>Ilość</th><th>Cena jednostkowa</th><th>Suma</th></tr>");

        for (OrderItem item : order.getOrderItems()) {
            content.append("<tr>");
            content.append("<td><strong>").append(item.getProduct().getName()).append("</strong><br>");
            content.append("<small style='color: #666;'>").append(item.getProduct().getCategory()).append("</small></td>");
            content.append("<td>").append(item.getQuantity()).append(" szt.</td>");
            content.append("<td>").append(formatPrice(item.getPrice())).append("</td>");
            content.append("<td><strong>").append(formatPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).append("</strong></td>");
            content.append("</tr>");
        }

        // Total
        content.append("<tr class='total-row'>");
        content.append("<td colspan='3'><strong>ŁĄCZNA KWOTA:</strong></td>");
        content.append("<td><strong>").append(formatPrice(order.getTotalAmount())).append("</strong></td>");
        content.append("</tr>");
        content.append("</table>");

        // Additional info
        content.append("<div style='background: #e7f3ff; padding: 1rem; border-radius: 5px; margin: 2rem 0;'>");
        content.append("<h4 style='color: #0056b3; margin-bottom: 0.5rem;'>ℹ️ Informacje o dostawie</h4>");
        content.append("<p style='color: #0056b3; margin: 0;'>• Dostawa kurierska: <strong>BEZPŁATNA</strong></p>");
        content.append("<p style='color: #0056b3; margin: 0;'>• Czas dostawy: <strong>1-3 dni robocze</strong></p>");
        content.append("<p style='color: #0056b3; margin: 0;'>• Płatność: <strong>przy odbiorze</strong></p>");
        content.append("</div>");

        content.append("<p>Jeśli masz pytania dotyczące zamówienia, skontaktuj się z nami.</p>");
        content.append("<p>Dziękujemy za zakupy w naszym sklepie!</p>");
        content.append("<p style='color: #667eea;'><strong>Zespół Sklepu Online</strong></p>");
        content.append("</div>");

        // Footer
        content.append("<div class='footer'>");
        content.append("<p>To jest automatyczna wiadomość, prosimy na nią nie odpowiadać.</p>");
        content.append("<p>© 2025 Sklep Online. Wszystkie prawa zastrzeżone.</p>");
        content.append("</div>");

        content.append("</div>");
        content.append("</body></html>");

        return content.toString();
    }

    /**
     * Formatuje cenę do polskiego formatu
     */
    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0,00 zł";
        }
        return String.format("%.2f zł", price);
    }
}