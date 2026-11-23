
package bookbridge.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public void registerMember(String id, String name,
                               String type)
            throws SQLException {

        if (id == null || id.isBlank()
                || name == null || name.isBlank()
                || type == null) {
            throw new IllegalArgumentException(
                    "Member details are required.");
        }

        String membership = type.trim().toUpperCase();

        if (!membership.equals("STUDENT")
                && !membership.equals("FACULTY")
                && !membership.equals("GUEST")) {
            throw new IllegalArgumentException(
                    "Invalid membership type.");
        }

        String sql = """
                INSERT INTO members
                (member_id, full_name, membership_type)
                VALUES (?, ?, ?)
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, id.trim());
            ps.setString(2, name.trim());
            ps.setString(3, membership);

            if (ps.executeUpdate() != 1) {
                throw new SQLException("Member was not saved.");
            }
        }
    }

    public List<String[]> listMembers() throws SQLException {

        List<String[]> members = new ArrayList<>();

        String sql = """
                SELECT member_id, full_name, membership_type
                FROM members
                ORDER BY member_id
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                members.add(new String[]{
                    rs.getString("member_id"),
                    rs.getString("full_name"),
                    rs.getString("membership_type")
                });
            }
        }

        return members;
    }

    public String[] findMember(String memberId)
            throws SQLException {

        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException(
                    "Member ID is required.");
        }

        String sql = """
                SELECT member_id, full_name, membership_type
                FROM members
                WHERE member_id = ?
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, memberId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                        rs.getString("member_id"),
                        rs.getString("full_name"),
                        rs.getString("membership_type")
                    };
                }
            }
        }

        return null;
    }
}
