
package bookbridge.service;

import bookbridge.dao.BookDAO;
import bookbridge.dao.MemberDAO;
import bookbridge.dao.LoanDAO;
import bookbridge.dao.ReservationDAO;

import java.sql.SQLException;
import java.util.List;

public class LibraryService {

    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final LoanDAO loanDAO = new LoanDAO();
    private final ReservationDAO reservationDAO =
            new ReservationDAO();

    // Catalog operations
    public void addBook(String isbn, String title,
                        String author, int copies)
            throws SQLException {

        bookDAO.addBook(isbn, title, author, copies);
    }

    public List<String[]> listBooks() throws SQLException {
        return bookDAO.listBooks();
    }

    public List<String[]> searchBooks(String keyword)
            throws SQLException {

        return bookDAO.searchBooks(keyword);
    }

    // Membership operations
    public void registerMember(String id, String name,
                               String type)
            throws SQLException {

        memberDAO.registerMember(id, name, type);
    }

    public List<String[]> listMembers() throws SQLException {
        return memberDAO.listMembers();
    }

    public String[] findMember(String memberId)
            throws SQLException {

        return memberDAO.findMember(memberId);
    }

    // Circulation operations
    public void issueLoan(String memberId, String isbn, int days)
            throws SQLException {

        loanDAO.issueLoan(memberId, isbn, days);
    }

    public void returnBook(int loanId) throws SQLException {
        loanDAO.returnBook(loanId);
    }

    public List<String[]> listLoans() throws SQLException {
        return loanDAO.listLoans();
    }

    // Reservation operations
    public void reserveBook(String memberId, String isbn)
            throws SQLException {

        reservationDAO.reserveBook(memberId, isbn);
    }

    public void cancelReservation(int reservationId)
            throws SQLException {

        reservationDAO.cancelReservation(reservationId);
    }

    public List<String[]> listReservations()
            throws SQLException {

        return reservationDAO.listReservations();
    }
}
