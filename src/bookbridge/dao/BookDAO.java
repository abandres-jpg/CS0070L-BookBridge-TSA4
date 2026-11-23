
package bookbridge.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    public void addBook(String isbn, String title,
                        String author, int copies)
            throws SQLException {

        if (isbn == null || isbn.isBlank()
                || title == null || title.isBlank()
                || author == null || author.isBlank()
                || copies < 1) {
            throw new IllegalArgumentException(
                    "Enter valid book details and copies.");
        }

        String sql = """
                INSERT INTO books
                (isbn, title, author, total_copies, available_copies)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, isbn.trim());
            ps.setString(2, title.trim());
            ps.setString(3, author.trim());
            ps.setInt(4, copies);
            ps.setInt(5, copies);

            if (ps.executeUpdate() != 1) {
                throw new SQLException("Book was not saved.");
            }
        }
    }

    public List<String[]> listBooks() throws SQLException {

        List<String[]> books = new ArrayList<>();

        String sql = """
                SELECT isbn, title, author,
                       total_copies, available_copies
                FROM books
                ORDER BY title, isbn
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                books.add(new String[]{
                    rs.getString("isbn"),
                    rs.getString("title"),
                    rs.getString("author"),
                    String.valueOf(rs.getInt("total_copies")),
                    String.valueOf(rs.getInt("available_copies"))
                });
            }
        }

        return books;
    }

    public List<String[]> searchBooks(String keyword)
            throws SQLException {

        if (keyword == null) {
            throw new IllegalArgumentException(
                    "Search keyword is required.");
        }

        List<String[]> books = new ArrayList<>();

        String sql = """
                SELECT isbn, title, author,
                       total_copies, available_copies
                FROM books
                WHERE isbn LIKE ? OR title LIKE ?
                ORDER BY title, isbn
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String search = "%" + keyword.trim() + "%";

            ps.setString(1, search);
            ps.setString(2, search);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(new String[]{
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getString("author"),
                        String.valueOf(rs.getInt("total_copies")),
                        String.valueOf(rs.getInt("available_copies"))
                    });
                }
            }
        }

        return books;
    }
}
