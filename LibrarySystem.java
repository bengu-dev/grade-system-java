import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 📖 Kütüphane Yönetim Sistemi
 * Kavramlar: OOP, Inheritance, File I/O, Stream API, LocalDate
 */

abstract class LibraryItem {
    private String id;
    private String title;
    private boolean isAvailable;

    public LibraryItem(String id, String title) {
        this.id = id;
        this.title = title;
        this.isAvailable = true;
    }

    public abstract String getType();
    public abstract String getDetails();

    public String getId()            { return id; }
    public String getTitle()         { return title; }
    public boolean isAvailable()     { return isAvailable; }
    public void setAvailable(boolean available) { this.isAvailable = available; }

    @Override
    public String toString() {
        return String.format("[%s] %s — %s (%s)",
            id, title, getDetails(), isAvailable ? "✅ Mevcut" : "❌ Ödünçte");
    }
}

class Book extends LibraryItem {
    private String author;
    private String isbn;
    private int pageCount;

    public Book(String id, String title, String author, String isbn, int pageCount) {
        super(id, title);
        this.author = author;
        this.isbn = isbn;
        this.pageCount = pageCount;
    }

    @Override public String getType()    { return "KİTAP"; }
    @Override public String getDetails() { return "Yazar: " + author + " | " + pageCount + " sayfa"; }
    public String getAuthor()            { return author; }
}

class Magazine extends LibraryItem {
    private String publisher;
    private int issueNumber;

    public Magazine(String id, String title, String publisher, int issueNumber) {
        super(id, title);
        this.publisher = publisher;
        this.issueNumber = issueNumber;
    }

    @Override public String getType()    { return "DERGİ"; }
    @Override public String getDetails() { return "Yayınevi: " + publisher + " | Sayı: " + issueNumber; }
}

class BorrowRecord {
    private String memberId;
    private String itemId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    public BorrowRecord(String memberId, String itemId) {
        this.memberId = memberId;
        this.itemId = itemId;
        this.borrowDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(14);
    }

    public void returnItem() { this.returnDate = LocalDate.now(); }
    public boolean isOverdue() { return returnDate == null && LocalDate.now().isAfter(dueDate); }
    public boolean isReturned() { return returnDate != null; }

    @Override
    public String toString() {
        return String.format("Üye: %s | Materyal: %s | Ödünç: %s | Son: %s | %s",
            memberId, itemId, borrowDate, dueDate,
            isReturned() ? "İade: " + returnDate : isOverdue() ? "⚠️ GECİKMİŞ!" : "Aktif");
    }

    public String getItemId()   { return itemId; }
    public String getMemberId() { return memberId; }
}

class Library {
    private Map<String, LibraryItem> items = new LinkedHashMap<>();
    private List<BorrowRecord> records = new ArrayList<>();
    private static final String LOG_FILE = "library_log.txt";

    public void addItem(LibraryItem item) {
        items.put(item.getId(), item);
        System.out.println("📚 Eklendi: " + item.getTitle());
    }

    public void borrowItem(String itemId, String memberId) {
        LibraryItem item = items.get(itemId);
        if (item == null)        { System.out.println("❌ Materyal bulunamadı!"); return; }
        if (!item.isAvailable()) { System.out.println("❌ Materyal şu an ödünçte!"); return; }

        item.setAvailable(false);
        records.add(new BorrowRecord(memberId, itemId));
        log("ÖDÜNÇ | Üye: " + memberId + " | Materyal: " + item.getTitle());
        System.out.printf("✅ '%s' kitabı %s'e ödünç verildi. (14 gün)%n", item.getTitle(), memberId);
    }

    public void returnItem(String itemId, String memberId) {
        LibraryItem item = items.get(itemId);
        if (item == null) { System.out.println("❌ Materyal bulunamadı!"); return; }

        records.stream()
            .filter(r -> r.getItemId().equals(itemId) && r.getMemberId().equals(memberId) && !r.isReturned())
            .findFirst()
            .ifPresentOrElse(r -> {
                r.returnItem();
                item.setAvailable(true);
                log("İADE | Üye: " + memberId + " | Materyal: " + item.getTitle());
                System.out.printf("✅ '%s' iade alındı.%n", item.getTitle());
            }, () -> System.out.println("❌ Aktif ödünç kaydı bulunamadı!"));
    }

    public void searchByTitle(String keyword) {
        System.out.println("\n🔍 '" + keyword + "' için arama sonuçları:");
        items.values().stream()
            .filter(i -> i.getTitle().toLowerCase().contains(keyword.toLowerCase()))
            .forEach(System.out::println);
    }

    public void listOverdue() {
        System.out.println("\n⚠️ GECİKMİŞ İADELER:");
        records.stream().filter(BorrowRecord::isOverdue).forEach(System.out::println);
    }

    public void printCatalog() {
        System.out.println("\n========== KÜTÜPHANE KATALOĞU ==========");
        items.values().forEach(System.out::println);
        long available = items.values().stream().filter(LibraryItem::isAvailable).count();
        System.out.printf("\nToplam: %d | Mevcut: %d | Ödünçte: %d%n",
            items.size(), available, items.size() - available);
    }

    private void log(String message) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            w.write("[" + LocalDate.now() + "] " + message);
            w.newLine();
        } catch (IOException e) { System.out.println("Log hatası: " + e.getMessage()); }
    }
}

public class LibrarySystem {
    public static void main(String[] args) {
        Library library = new Library();

        library.addItem(new Book("B001", "Clean Code", "Robert C. Martin", "978-0132350884", 431));
        library.addItem(new Book("B002", "Design Patterns", "Gang of Four", "978-0201633610", 395));
        library.addItem(new Book("B003", "The Pragmatic Programmer", "Andrew Hunt", "978-0135957059", 352));
        library.addItem(new Magazine("M001", "Popular Science", "Bonnier Corp", 245));
        library.addItem(new Magazine("M002", "IEEE Spectrum", "IEEE", 102));

        library.printCatalog();
        library.borrowItem("B001", "BENGU001");
        library.borrowItem("B001", "AHMET002");
        library.borrowItem("B002", "AYSE003");
        library.searchByTitle("clean");
        library.printCatalog();
        library.returnItem("B001", "BENGU001");
        library.printCatalog();
    }
}
