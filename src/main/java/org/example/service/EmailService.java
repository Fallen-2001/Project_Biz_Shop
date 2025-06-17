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
import org.example.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Resource(name = "mail/default")
    private Session mailSession;

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
            message.setSubject("Potwierdzenie zamówienia #" + order.getId());

            String emailContent = buildOrderConfirmationContent(order);
            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Order confirmation email sent successfully for order: {}", order.getId());

        } catch (MessagingException e) {
            logger.error("Failed to send order confirmation email for order: {}", order.getId(), e);
            throw new Exception("Nie udało się wysłać email z potwierdzeniem", e);
        }
    }

    public void sendOrderStatusUpdate(Order order, OrderStatus oldStatus, OrderStatus newStatus) throws Exception {
        logger.debug("Sending order status update email for order: {}", order.getId());

        if (order.getUser().getEmail() == null || order.getUser().getEmail().trim().isEmpty()) {
            logger.warn("User {} has no email address, skipping email", order.getUser().getUsername());
            return;
        }

        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress("noreply@shop.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(order.getUser().getEmail()));
            message.setSubject("Aktualizacja statusu zamówienia #" + order.getId());

            String emailContent = buildStatusUpdateContent(order, oldStatus, newStatus);
            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Order status update email sent successfully for order: {}", order.getId());

        } catch (MessagingException e) {
            logger.error("Failed to send order status update email for order: {}", order.getId(), e);
            throw new Exception("Nie udało się wysłać email z aktualizacją statusu", e);
        }
    }

    public void sendOrderCancellation(Order order) throws Exception {
        logger.debug("Sending order cancellation email for order: {}", order.getId());

        if (order.getUser().getEmail() == null || order.getUser().getEmail().trim().isEmpty()) {
            logger.warn("User {} has no email address, skipping email", order.getUser().getUsername());
            return;
        }

        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress("noreply@shop.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(order.getUser().getEmail()));
            message.setSubject("Anulowanie zamówienia #" + order.getId());

            String emailContent = buildCancellationContent(order);
            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Order cancellation email sent successfully for order: {}", order.getId());

        } catch (MessagingException e) {
            logger.error("Failed to send order cancellation email for order: {}", order.getId(), e);
            throw new Exception("Nie udało się wysłać email z informacją o anulowaniu", e);
        }
    }

    private String buildOrderConfirmationContent(Order order) {
        StringBuilder content = new StringBuilder();

        content.append("<!DOCTYPE html>");
        content.append("<html><head><meta charset='utf-8'></head><body>");
        content.append("<h2>Dziękujemy za złożenie zamówienia!</h2>");
        content.append("<p>Szanowny/a ").append(order.getUser().getFullName()).append(",</p>");
        content.append("<p>Twoje zamówienie zostało złożone pomyślnie.</p>");

        content.append("<h3>Szczegóły zamówienia #").append(order.getId()).append("</h3>");
        content.append("<p><strong>Data zamówienia:</strong> ").append(order.getOrderDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("</p>");
        content.append("<p><strong>Status:</strong> ").append(order.getStatus().getDisplayName()).append("</p>");
        content.append("<p><strong>Adres dostawy:</strong> ").append(order.getShippingAddress()).append("</p>");

        content.append("<h3>Zamówione produkty:</h3>");
        content.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        content.append("<tr><th>Produkt</th><th>Ilość</th><th>Cena</th><th>Suma</th></tr>");

        for (OrderItem item : order.getOrderItems()) {
            content.append("<tr>");
            content.append("<td>").append(item.getProduct().getName()).append("</td>");
            content.append("<td>").append(item.getQuantity()).append("</td>");
            content.append("<td>").append(formatPrice(item.getPrice())).append("</td>");
            content.append("<td>").append(formatPrice(item.getSubtotal())).append("</td>");
            content.append("</tr>");
        }

        content.append("</table>");
        content.append("<p><strong>Łączna kwota: ").append(formatPrice(order.getTotalAmount())).append("</strong></p>");

        content.append("<p>Dziękujemy za zakupy w naszym sklepie!</p>");
        content.append("<p>Zespół Sklepu</p>");
        content.append("</body></html>");

        return content.toString();
    }

    private String buildStatusUpdateContent(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        StringBuilder content = new StringBuilder();

        content.append("<!DOCTYPE html>");
        content.append("<html><head><meta charset='utf-8'></head><body>");
        content.append("<h2>Aktualizacja statusu zamówienia</h2>");
        content.append("<p>Szanowny/a ").append(order.getUser().getFullName()).append(",</p>");
        content.append("<p>Status Twojego zamówienia #").append(order.getId()).append(" został zmieniony.</p>");

        content.append("<p><strong>Poprzedni status:</strong> ").append(oldStatus.getDisplayName()).append("</p>");
        content.append("<p><strong>Nowy status:</strong> ").append(newStatus.getDisplayName()).append("</p>");

        if (newStatus == OrderStatus.SHIPPED) {
            content.append("<p>Twoje zamówienie zostało wysłane i jest w drodze do Ciebie!</p>");
        } else if (newStatus == OrderStatus.DELIVERED) {
            content.append("<p>Twoje zamówienie zostało dostarczone. Dziękujemy za zakupy!</p>");
        }

        content.append("<p>Dziękujemy za zakupy w naszym sklepie!</p>");
        content.append("<p>Zespół Sklepu</p>");
        content.append("</body></html>");

        return content.toString();
    }

    private String buildCancellationContent(Order order) {
        StringBuilder content = new StringBuilder();

        content.append("<!DOCTYPE html>");
        content.append("<html><head><meta charset='utf-8'></head><body>");
        content.append("<h2>Anulowanie zamówienia</h2>");
        content.append("<p>Szanowny/a ").append(order.getUser().getFullName()).append(",</p>");
        content.append("<p>Twoje zamówienie #").append(order.getId()).append(" zostało anulowane.</p>");

        content.append("<p>Stan magazynowy produktów został przywrócony.</p>");
        content.append("<p>Jeśli masz pytania, skontaktuj się z naszym działem obsługi klienta.</p>");

        content.append("<p>Dziękujemy za zrozumienie.</p>");
        content.append("<p>Zespół Sklepu</p>");
        content.append("</body></html>");

        return content.toString();
    }

    private String formatPrice(BigDecimal price) {
        return String.format("%.2f zł", price);
    }
}