
package bookbridge.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {

    public void issueLoan(String memberId, String isbn, int days)
            throws SQLException {

        if (memberId == null || memberId.isBlank()
                || isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException(
                    "Member ID and ISBN are required.");
        }

        if (days < 1 || days > 30) {
            throw new IllegalArgumentException(
                    "Loan days must be between 1 and 30.");
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int available;

                String bookSql = """
                    SELECT available_copies FROM books
                    WHERE isbn = ? FOR UPDATE
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(bookSql)) {
                    ps.setString(1, isbn.trim());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Book not found.");
                        }
                        available = rs.getInt(1);
                    }
                }

                if (available < 1) {
                    throw new IllegalArgumentException(
                            "Book is unavailable.");
                }

                String type;
                String memberSql = """
                    SELECT membership_type FROM members
                    WHERE member_id = ?
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(memberSql)) {
                    ps.setString(1, memberId.trim());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Member not found.");
                        }
                        type = rs.getString(1);
                    }
                }

                int limit = switch (type) {
                    case "STUDENT" -> 14;
                    case "FACULTY" -> 30;
                    case "GUEST" -> 7;
                    default -> throw new IllegalArgumentException(
                            "Invalid membership type.");
                };

                if (days > limit) {
                    throw new IllegalArgumentException(
                            "Loan exceeds membership limit.");
                }

                String prioritySql = """
                    SELECT reservation_id FROM reservations
                    WHERE isbn = ? AND status IN ('WAITING','READY')
                    LIMIT 1
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(prioritySql)) {
                    ps.setString(1, isbn.trim());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalArgumentException(
                                    "Book has a pending reservation.");
                        }
                    }
                }

                String duplicateSql = """
                    SELECT loan_id FROM loans
                    WHERE member_id = ? AND isbn = ?
                    AND status = 'ACTIVE'
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(duplicateSql)) {
                    ps.setString(1, memberId.trim());
                    ps.setString(2, isbn.trim());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalArgumentException(
                                    "Member already has this book.");
                        }
                    }
                }

                String insertSql = """
                    INSERT INTO loans
                    (member_id, isbn, loan_days,
                     issue_date, due_date, status)
                    VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(insertSql)) {
                    ps.setString(1, memberId.trim());
                    ps.setString(2, isbn.trim());
                    ps.setInt(3, days);
                    ps.setDate(4, Date.valueOf(LocalDate.now()));
                    ps.setDate(5, Date.valueOf(
                            LocalDate.now().plusDays(days)));

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Loan was not created.");
                    }
                }

                String updateSql = """
                    UPDATE books
                    SET available_copies = available_copies - 1
                    WHERE isbn = ? AND available_copies > 0
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(updateSql)) {
                    ps.setString(1, isbn.trim());

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Book inventory update failed.");
                    }
                }

                con.commit();

            } catch (SQLException | RuntimeException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }

    public void returnBook(int loanId) throws SQLException {

        if (loanId < 1) {
            throw new IllegalArgumentException(
                    "Invalid loan ID.");
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                String isbn;
                String selectSql = """
                    SELECT isbn FROM loans
                    WHERE loan_id = ? AND status = 'ACTIVE'
                    FOR UPDATE
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(selectSql)) {
                    ps.setInt(1, loanId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Active loan not found.");
                        }
                        isbn = rs.getString(1);
                    }
                }

                String updateLoan = """
                    UPDATE loans SET status = 'RETURNED',
                    return_date = CURRENT_DATE
                    WHERE loan_id = ? AND status = 'ACTIVE'
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(updateLoan)) {
                    ps.setInt(1, loanId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Loan return update failed.");
                    }
                }

                String updateBook = """
                    UPDATE books
                    SET available_copies = available_copies + 1
                    WHERE isbn = ?
                    AND available_copies < total_copies
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(updateBook)) {
                    ps.setString(1, isbn);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Book inventory restoration failed.");
                    }
                }

                con.commit();

            } catch (SQLException | RuntimeException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }

    public List<String[]> listLoans() throws SQLException {

        List<String[]> loans = new ArrayList<>();

        String sql = """
            SELECT l.loan_id, l.member_id, m.full_name,
                   l.isbn, b.title, l.issue_date,
                   l.due_date, l.return_date, l.status
            FROM loans l
            JOIN members m ON l.member_id = m.member_id
            JOIN books b ON l.isbn = b.isbn
            ORDER BY l.loan_id DESC
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                loans.add(new String[]{
                    rs.getString("loan_id"),
                    rs.getString("member_id"),
                    rs.getString("full_name"),
                    rs.getString("isbn"),
                    rs.getString("title"),
                    rs.getString("issue_date"),
                    rs.getString("due_date"),
                    rs.getString("return_date"),
                    rs.getString("status")
                });
            }
        }

        return loans;
    }
}
