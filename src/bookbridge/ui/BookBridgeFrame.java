
package bookbridge.ui;

import bookbridge.service.LibraryService;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BookBridgeFrame extends JFrame {

    private final LibraryService service = new LibraryService();

    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel status = new JLabel("Ready");
    private final JProgressBar progress = new JProgressBar();

    private final DefaultTableModel booksModel =
            createModel("ISBN", "Title", "Author",
                    "Total Copies", "Available Copies");

    private final DefaultTableModel membersModel =
            createModel("Member ID", "Full Name", "Membership");

    private final DefaultTableModel loansModel =
            createModel("Loan ID", "Member ID", "Name", "ISBN",
                    "Book", "Issue Date", "Due Date",
                    "Return Date", "Status");

    private final DefaultTableModel reservationsModel =
            createModel("Reservation ID", "Member ID", "Name",
                    "ISBN", "Book", "Date", "Status");

    private final JTable booksTable = new JTable(booksModel);
    private final JTable membersTable = new JTable(membersModel);
    private final JTable loansTable = new JTable(loansModel);
    private final JTable reservationsTable =
            new JTable(reservationsModel);

    private final JTextArea reportArea = new JTextArea();

    private boolean busy = false;

    public BookBridgeFrame() {

        super("BookBridge TSA4 - Library Management System");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        tabs.addTab("Catalog", createCatalogPanel());
        tabs.addTab("Members", createMembersPanel());
        tabs.addTab("Circulation", createCirculationPanel());
        tabs.addTab("Reservations", createReservationsPanel());
        tabs.addTab("Reports", createReportsPanel());

        add(tabs, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        progress.setIndeterminate(true);
        progress.setVisible(false);

        bottom.add(status, BorderLayout.CENTER);
        bottom.add(progress, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        refreshAll();
    }

    private static DefaultTableModel createModel(String... columns) {

        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JPanel createCatalogPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JTextField isbn = new JTextField(10);
        JTextField title = new JTextField(14);
        JTextField author = new JTextField(12);
        JTextField copies = new JTextField(5);

        JButton add = new JButton("Add Book");
        JButton search = new JButton("Search");
        JButton refresh = new JButton("Refresh");

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        form.add(new JLabel("ISBN"));
        form.add(isbn);
        form.add(new JLabel("Title"));
        form.add(title);
        form.add(new JLabel("Author"));
        form.add(author);
        form.add(new JLabel("Copies"));
        form.add(copies);
        form.add(add);
        form.add(search);
        form.add(refresh);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(booksTable), BorderLayout.CENTER);

        add.addActionListener(e -> runTask(
                () -> {
                    service.addBook(isbn.getText(), title.getText(),
                            author.getText(),
                            Integer.parseInt(copies.getText().trim()));
                    return service.listBooks();
                },
                rows -> {
                    fillTable(booksModel, rows);
                    status.setText("Book added successfully.");
                }
        ));

        search.addActionListener(e -> runTask(
                () -> service.searchBooks(isbn.getText().isBlank()
                        ? title.getText() : isbn.getText()),
                rows -> fillTable(booksModel, rows)
        ));

        refresh.addActionListener(e -> refreshBooks());

        return panel;
    }

    private JPanel createMembersPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JTextField id = new JTextField(10);
        JTextField name = new JTextField(16);

        JComboBox<String> type = new JComboBox<>(
                new String[]{"STUDENT", "FACULTY", "GUEST"});

        JButton register = new JButton("Register Member");
        JButton refresh = new JButton("Refresh Members");

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        form.add(new JLabel("Member ID"));
        form.add(id);
        form.add(new JLabel("Full Name"));
        form.add(name);
        form.add(new JLabel("Membership"));
        form.add(type);
        form.add(register);
        form.add(refresh);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(membersTable), BorderLayout.CENTER);

        register.addActionListener(e -> runTask(
                () -> {
                    service.registerMember(id.getText(), name.getText(),
                            type.getSelectedItem().toString());
                    return service.listMembers();
                },
                rows -> {
                    fillTable(membersModel, rows);
                    status.setText("Member registered successfully.");
                }
        ));

        refresh.addActionListener(e -> refreshMembers());

        return panel;
    }

    private JPanel createCirculationPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JTextField memberId = new JTextField(9);
        JTextField isbn = new JTextField(12);
        JTextField days = new JTextField(5);

        JButton issue = new JButton("Issue Loan");
        JButton returnBook = new JButton("Return Book");
        JButton refresh = new JButton("Refresh Loans");

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        form.add(new JLabel("Member ID"));
        form.add(memberId);
        form.add(new JLabel("ISBN"));
        form.add(isbn);
        form.add(new JLabel("Loan Days"));
        form.add(days);
        form.add(issue);
        form.add(returnBook);
        form.add(refresh);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(loansTable), BorderLayout.CENTER);

        issue.addActionListener(e -> runTask(
                () -> {
                    service.issueLoan(memberId.getText(), isbn.getText(),
                            Integer.parseInt(days.getText().trim()));
                    return service.listLoans();
                },
                rows -> {
                    fillTable(loansModel, rows);
                    status.setText("Loan issued successfully.");
                }
        ));

        returnBook.addActionListener(e -> {

            int row = loansTable.getSelectedRow();

            if (row < 0) {
                showError("Select a loan from the table first.");
                return;
            }

            int id = Integer.parseInt(
                    loansTable.getValueAt(row, 0).toString());

            int confirm = JOptionPane.showConfirmDialog(
                    this, "Return selected book?",
                    "Confirm Return", JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            runTask(
                    () -> {
                        service.returnBook(id);
                        return service.listLoans();
                    },
                    rows -> {
                        fillTable(loansModel, rows);
                        status.setText("Book returned successfully.");
                    }
            );
        });

        refresh.addActionListener(e -> refreshLoans());

        return panel;
    }

    private JPanel createReservationsPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JTextField memberId = new JTextField(10);
        JTextField isbn = new JTextField(12);

        JButton reserve = new JButton("Reserve");
        JButton cancel = new JButton("Cancel Reservation");
        JButton refresh = new JButton("Refresh");

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        form.add(new JLabel("Member ID"));
        form.add(memberId);
        form.add(new JLabel("ISBN"));
        form.add(isbn);
        form.add(reserve);
        form.add(cancel);
        form.add(refresh);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(reservationsTable),
                BorderLayout.CENTER);

        reserve.addActionListener(e -> runTask(
                () -> {
                    service.reserveBook(memberId.getText(), isbn.getText());
                    return service.listReservations();
                },
                rows -> {
                    fillTable(reservationsModel, rows);
                    status.setText("Reservation created successfully.");
                }
        ));

        cancel.addActionListener(e -> {

            int row = reservationsTable.getSelectedRow();

            if (row < 0) {
                showError("Select a reservation first.");
                return;
            }

            int id = Integer.parseInt(
                    reservationsTable.getValueAt(row, 0).toString());

            int confirm = JOptionPane.showConfirmDialog(
                    this, "Cancel selected reservation?",
                    "Confirm Cancellation",
                    JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            runTask(
                    () -> {
                        service.cancelReservation(id);
                        return service.listReservations();
                    },
                    rows -> {
                        fillTable(reservationsModel, rows);
                        status.setText("Reservation cancelled.");
                    }
            );
        });

        refresh.addActionListener(e -> refreshReservations());

        return panel;
    }

    private JPanel createReportsPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JButton generate = new JButton("Generate Report");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(generate);

        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        generate.addActionListener(e -> runTask(
                () -> {
                    List<String[]> books = service.listBooks();
                    List<String[]> members = service.listMembers();
                    List<String[]> loans = service.listLoans();
                    List<String[]> reservations =
                            service.listReservations();

                    int available = 0;
                    int checkedOut = 0;
                    int activeLoans = 0;

                    for (String[] book : books) {
                        available += Integer.parseInt(book[4]);
                        checkedOut += Integer.parseInt(book[3])
                                - Integer.parseInt(book[4]);
                    }

                    for (String[] loan : loans) {
                        if ("ACTIVE".equals(loan[8])) {
                            activeLoans++;
                        }
                    }

                    return "========== BOOKBRIDGE TSA4 REPORT ==========\n"
                            + "Registered Members: " + members.size()
                            + "\nBook Titles: " + books.size()
                            + "\nAvailable Copies: " + available
                            + "\nChecked-out Copies: " + checkedOut
                            + "\nActive Loans: " + activeLoans
                            + "\nReservations: " + reservations.size();
                },
                text -> reportArea.setText(text)
        ));

        return panel;
    }

    private void fillTable(DefaultTableModel model,
                           List<String[]> rows) {

        model.setRowCount(0);

        for (String[] row : rows) {
            model.addRow(row);
        }
    }

    private interface DatabaseTask<T> {
        T execute() throws Exception;
    }

    private <T> void runTask(DatabaseTask<T> task,
                             Consumer<T> onSuccess) {

        if (busy) {
            return;
        }

        busy = true;
        progress.setVisible(true);
        status.setText("Processing database request...");

        new SwingWorker<T, Void>() {

            @Override
            protected T doInBackground() throws Exception {
                return task.execute();
            }

            @Override
            protected void done() {

                busy = false;
                progress.setVisible(false);

                try {
                    onSuccess.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError("Operation interrupted.");
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    if (cause instanceof SQLException) {
                        showError("Database error: "
                                + cause.getMessage());
                    } else {
                        showError(cause == null
                                ? "Operation failed."
                                : cause.getMessage());
                    }
                }
            }
        }.execute();
    }

    private void showError(String message) {

        status.setText("Error: " + message);

        JOptionPane.showMessageDialog(
                this, message, "BookBridge Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private void refreshBooks() {
        runTask(service::listBooks,
                rows -> fillTable(booksModel, rows));
    }

    private void refreshMembers() {
        runTask(service::listMembers,
                rows -> fillTable(membersModel, rows));
    }

    private void refreshLoans() {
        runTask(service::listLoans,
                rows -> fillTable(loansModel, rows));
    }

    private void refreshReservations() {
        runTask(service::listReservations,
                rows -> fillTable(reservationsModel, rows));
    }

    private void refreshAll() {
        refreshBooks();
    }
}
