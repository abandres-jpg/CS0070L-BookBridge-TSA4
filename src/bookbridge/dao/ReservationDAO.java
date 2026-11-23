
package bookbridge.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    // Create a reservation for an unavailable book
    public void reserveBook(String memberId, String isbn)
            throws SQLException {

        if (memberId == null || memberId.isBlank()
                || isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException(
                    "Member ID and ISBN are required.");
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Check if the member exists
                String memberSql =
                        "SELECT member_id FROM members WHERE member_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(memberSql)) {

                    ps.setString(1, memberId.trim());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Member not found.");
                        }
                    }
                }

                // Check book availability
                int available;

                String bookSql = """
                    SELECT available_copies
                    FROM books
                    WHERE isbn = ?
                    FOR UPDATE
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(bookSql)) {

                    ps.setString(1, isbn.trim());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Book not found.");
                        }

                        available = rs.getInt("available_copies");
                    }
                }

                if (available > 0) {
                    throw new IllegalArgumentException(
                            "Book is available. Issue a loan instead.");
                }

                // Prevent the borrower from reserving the same book
                String activeSql = """
                    SELECT loan_id FROM loans
                    WHERE member_id = ?
                    AND isbn = ?
                    AND status = 'ACTIVE'
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(activeSql)) {

                    ps.setString(1, memberId.trim());
                    ps.setString(2, isbn.trim());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalArgumentException(
                                    "You already borrowed this book.");
                        }
                    }
                }

                // Prevent duplicate active reservations
                String duplicateSql = """
                    SELECT reservation_id FROM reservations
                    WHERE member_id = ?
                    AND isbn = ?
                    AND status IN ('WAITING', 'READY')
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(duplicateSql)) {

                    ps.setString(1, memberId.trim());
                    ps.setString(2, isbn.trim());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalArgumentException(
                                    "Reservation already exists.");
                        }
                    }
                }

                // Save the reservation
                String insertSql = """
                    INSERT INTO reservations
                    (member_id, isbn, status)
                    VALUES (?, ?, 'WAITING')
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(insertSql)) {

                    ps.setString(1, memberId.trim());
                    ps.setString(2, isbn.trim());

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Reservation was not saved.");
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

    // Cancel an existing reservation
    public void cancelReservation(int reservationId)
            throws SQLException {

        if (reservationId < 1) {
            throw new IllegalArgumentException(
                    "Invalid reservation ID.");
        }

        String sql = """
            UPDATE reservations
            SET status = 'CANCELLED'
            WHERE reservation_id = ?
            AND status IN ('WAITING', 'READY')
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reservationId);

            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "Active reservation not found.");
            }
        }
    }

    // Retrieve reservation records
    public List<String[]> listReservations()
            throws SQLException {

        List<String[]> reservations = new ArrayList<>();

        String sql = """
            SELECT r.reservation_id,
                   r.member_id,
                   m.full_name,
                   r.isbn,
                   b.title,
                   r.reservation_date,
                   r.status
            FROM reservations r
            JOIN members m ON r.member_id = m.member_id
            JOIN books b ON r.isbn = b.isbn
            ORDER BY r.reservation_id DESC
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reservations.add(new String[]{
                    rs.getString("reservation_id"),
                    rs.getString("member_id"),
                    rs.getString("full_name"),
                    rs.getString("isbn"),
                    rs.getString("title"),
                    rs.getString("reservation_date"),
                    rs.getString("status")
                });
            }
        }

        return reservations;
    }
}
