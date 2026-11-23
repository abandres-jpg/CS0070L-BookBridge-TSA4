
package bookbridge.app;

import bookbridge.dao.BookDAO;
import bookbridge.dao.MemberDAO;
import bookbridge.dao.LoanDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyTest {

    public static void main(String[] args) throws Exception {

        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        String isbn = "CONCURRENT001";
        String member1 = "CONC001";
        String member2 = "CONC002";

        // Use unique test records
        bookDAO.addBook(
                isbn, "Concurrency Test Book", "Test Author", 1);

        memberDAO.registerMember(
                member1, "Test Member One", "STUDENT");

        memberDAO.registerMember(
                member2, "Test Member Two", "STUDENT");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        Runnable borrowTask = () -> {

            String memberId = Thread.currentThread()
                    .getName();

            ready.countDown();

            try {
                start.await();

                loanDAO.issueLoan(memberId, isbn, 7);
                successCount.incrementAndGet();

            } catch (Exception e) {
                rejectedCount.incrementAndGet();
                System.out.println(memberId
                        + " rejected: " + e.getMessage());
            }
        };

        Thread first = new Thread(borrowTask, member1);
        Thread second = new Thread(borrowTask, member2);

        first.start();
        second.start();

        ready.await();
        start.countDown();

        first.join();
        second.join();

        int available = -1;
        int activeLoans = -1;

        try (Connection con =
                bookbridge.dao.DatabaseConnection.getConnection()) {

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT available_copies FROM books WHERE isbn = ?")) {

                ps.setString(1, isbn);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        available = rs.getInt(1);
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM loans "
                    + "WHERE isbn = ? AND status = 'ACTIVE'")) {

                ps.setString(1, isbn);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        activeLoans = rs.getInt(1);
                    }
                }
            }
        }

        System.out.println("\n=== CONCURRENCY TEST ===");
        System.out.println("Successful requests: "
                + successCount.get());
        System.out.println("Rejected requests: "
                + rejectedCount.get());
        System.out.println("Available copies: " + available);
        System.out.println("Active loans: " + activeLoans);

        boolean passed = successCount.get() == 1
                && rejectedCount.get() == 1
                && available == 0
                && activeLoans == 1;

        System.out.println("TEST RESULT: "
                + (passed ? "PASS" : "FAIL"));
    }
}
