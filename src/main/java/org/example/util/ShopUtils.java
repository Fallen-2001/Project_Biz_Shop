package org.example.util;

import org.example.model.OrderStatus;
import org.example.model.Product;
import org.example.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Klasa pomocnicza z metodami utility dla sklepu internetowego
 */
public class ShopUtils {

    private static final Logger logger = LoggerFactory.getLogger(ShopUtils.class);

    private static final Locale POLISH_LOCALE = new Locale("pl", "PL");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(POLISH_LOCALE);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Formatuje cenę do polskiego formatu waluty
     */
    public static String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0,00 zł";
        }
        return CURRENCY_FORMAT.format(price);
    }

    /**
     * Formatuje datę do polskiego formatu
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMAT);
    }

    /**
     * Formatuje datę i czas do polskiego formatu
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATETIME_FORMAT);
    }

    /**
     * Zwraca tekstowy opis dostępności produktu
     */
    public static String getStockStatusText(Product product) {
        if (product == null || !product.isActive()) {
            return "Produkt niedostępny";
        }

        Integer stock = product.getStockQuantity();
        if (stock == null || stock == 0) {
            return "Wyprzedany";
        } else if (stock <= 3) {
            return "Ostatnie sztuki (" + stock + " szt.)";
        } else if (stock <= 10) {
            return "Ograniczona dostępność (" + stock + " szt.)";
        } else {
            return "Dostępny (" + stock + " szt.)";
        }
    }

    /**
     * Zwraca klasę CSS dla statusu dostępności produktu
     */
    public static String getStockStatusClass(Product product) {
        if (product == null || !product.isActive()) {
            return "stock-out";
        }

        Integer stock = product.getStockQuantity();
        if (stock == null || stock == 0) {
            return "stock-out";
        } else if (stock <= 3) {
            return "stock-low";
        } else if (stock <= 10) {
            return "stock-medium";
        } else {
            return "stock-high";
        }
    }

    /**
     * Zwraca emoji dla statusu zamówienia
     */
    public static String getOrderStatusEmoji(OrderStatus status) {
        if (status == null) {
            return "❓";
        }

        return switch (status) {
            case PENDING -> "⏳";
            case CONFIRMED -> "✅";
            case SHIPPED -> "🚛";
            case DELIVERED -> "📦";
            case CANCELLED -> "❌";
        };
    }

    /**
     * Zwraca klasę CSS dla statusu zamówienia
     */
    public static String getOrderStatusClass(OrderStatus status) {
        if (status == null) {
            return "status-unknown";
        }

        return "status-" + status.name().toLowerCase();
    }

    /**
     * Sprawdza czy produkt jest dostępny w określonej ilości
     */
    public static boolean isProductAvailable(Product product, int quantity) {
        if (product == null || !product.isActive()) {
            return false;
        }

        Integer stock = product.getStockQuantity();
        return stock != null && stock >= quantity;
    }

    /**
     * Oblicza wartość procentową rabatu
     */
    public static BigDecimal calculateDiscountPercentage(BigDecimal originalPrice, BigDecimal discountedPrice) {
        if (originalPrice == null || discountedPrice == null ||
                originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = originalPrice.subtract(discountedPrice);
        return discount.divide(originalPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Oblicza łączną wartość zamówienia
     */
    public static BigDecimal calculateOrderTotal(Order order) {
        if (order == null || order.getOrderItems() == null) {
            return BigDecimal.ZERO;
        }

        return order.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Waliduje adres email
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Waliduje numer telefonu
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // Usuń wszystkie znaki specjalne
        String cleanPhone = phone.replaceAll("[\\s\\-\\+\\(\\)\\.]", "");

        // Sprawdź czy zawiera tylko cyfry i ma odpowiednią długość
        return cleanPhone.matches("\\d{9,15}");
    }

    /**
     * Generuje bezpieczny fragment tekstu (usuwa znaki specjalne)
     */
    public static String sanitizeText(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("[<>\"'&]", "")
                .trim();
    }

    /**
     * Skraca tekst do określonej długości z wielokropkiem
     */
    public static String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Zwraca maksymalną dozwoloną ilość do zamówienia dla produktu
     */
    public static int getMaxOrderQuantity(Product product) {
        if (product == null || !product.isActive()) {
            return 0;
        }

        Integer stock = product.getStockQuantity();
        if (stock == null || stock <= 0) {
            return 0;
        }

        // Maksymalnie 10 sztuk jednego produktu lub dostępna ilość
        return Math.min(stock, 10);
    }

    /**
     * Sprawdza czy zamówienie można anulować
     */
    public static boolean canCancelOrder(Order order) {
        if (order == null) {
            return false;
        }

        OrderStatus status = order.getStatus();
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    /**
     * Sprawdza czy zamówienie zostało dostarczone
     */
    public static boolean isOrderDelivered(Order order) {
        return order != null && order.getStatus() == OrderStatus.DELIVERED;
    }

    /**
     * Oblicza procent wypełnienia profilu użytkownika
     */
    public static int calculateProfileCompletion(String firstName, String lastName,
                                                 String email, String phone, String address) {
        int completedFields = 0;
        int totalFields = 5;

        if (firstName != null && !firstName.trim().isEmpty()) completedFields++;
        if (lastName != null && !lastName.trim().isEmpty()) completedFields++;
        if (email != null && !email.trim().isEmpty()) completedFields++;
        if (phone != null && !phone.trim().isEmpty()) completedFields++;
        if (address != null && !address.trim().isEmpty()) completedFields++;

        return (completedFields * 100) / totalFields;
    }

    /**
     * Zwraca inicjały użytkownika
     */
    public static String getUserInitials(String firstName, String lastName, String username) {
        StringBuilder initials = new StringBuilder();

        if (firstName != null && !firstName.isEmpty()) {
            initials.append(firstName.charAt(0));
        }
        if (lastName != null && !lastName.isEmpty()) {
            initials.append(lastName.charAt(0));
        }

        if (initials.length() == 0 && username != null && !username.isEmpty()) {
            initials.append(username.charAt(0));
        }

        return initials.toString().toUpperCase();
    }

    /**
     * Loguje operację z odpowiednim poziomem
     */
    public static void logOperation(String operation, String details) {
        logger.info("Operation: {} - Details: {}", operation, details);
    }

    /**
     * Loguje błąd z kontekstem
     */
    public static void logError(String operation, String error, Exception e) {
        logger.error("Error in operation: {} - Error: {} - Exception: {}",
                operation, error, e != null ? e.getMessage() : "Unknown");
    }

    /**
     * Sprawdza czy lista nie jest pusta
     */
    public static boolean isNotEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * Sprawdza czy string nie jest pusty
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Zwraca wartość domyślną jeśli obiekt jest null
     */
    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}