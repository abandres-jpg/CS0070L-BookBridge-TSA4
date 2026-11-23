
package bookbridge.ui;

import bookbridge.service.LibraryService;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
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
    private final List<JButton> buttons = new ArrayList<>();

    private final DefaultTableModel booksModel =
            model("ISBN", "Title", "Author",
                    "Total Copies", "Available Copies");

    private final DefaultTableModel membersModel =
            model("Member ID", "Full Name", "Membership");

    private final DefaultTableModel loansModel =
            model("Loan ID", "Member ID", "Name", "ISBN",
                    "Book", "Issue Date", "Due Date",
                    "Return Date", "Status");

    private final DefaultTableModel reservationsModel =
            model("Reservation ID", "Member ID", "Name",
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

        tabs.addTab("Catalog", catalogPanel());
        tabs.addTab("Members", membersPanel());
        tabs.addTab("Circulation", circulationPanel());
        tabs.addTab("Reservations", reservationsPanel());
        tabs.addTab("Reports", reportsPanel());

        add(tabs, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10));

        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setPreferredSize(new Dimension(130, 18));

        bottom.add(status, BorderLayout.CENTER);
        bottom.add(progress, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        refreshBooks();
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    private JButton button(String text) {
        JButton button = new JButton(text);
        buttons.add(button);
        return button;
    }

    private JPanel catalogPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JTextField isbn = new JTextField(10);
        JTextField title = new JTextField(14);
        JTextField author = new JTextField(12);
        JTextField copies = new JTextField(5);

        JButton add = button("Add Book");
        JButton search = button("Search");
        JButton refresh = button("Refresh");

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

        add.addActionListener(e -> {
            String bookIsbn = isbn.getText().trim();
            String bookTitle = title.getText().trim();
            String bookAuthor = author.getText().trim();

            try {
                int count = Integer.parseInt(copies.getText().trim());

                runTask(() -> {
                    service.addBook(
                            bookIsbn, bookTitle, bookAuthor, count);
                    return service.listBooks();
                }, rows -> {
                    fillTable(booksModel, rows);
                    status.setText("Book added successfully.");
                });

            } catch (NumberFormatException ex) {
                showError("Copies must be a valid whole number.");
            }
        });

        search.addActionListener(e -> {
            String keyword = isbn.getText().isBlank()
                    ? title.getText().trim()
                    : isbn.getText().trim();

            runTask(() -> service.searchBooks(keyword),
                    rows -> {
                        fillTable(booksModel, rows);
                        status.setText("Search completed.");
                    });
        });

        refresh.addActionListener(e -> refreshBooks());

        return panel;
    }

    private JPanel membersPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JTextField id = new JTextField(10);
        JTextField name = new JTextField(16);

        JComboBox<String> type = new JComboBox<>(
                new String[]{"STUDENT", "FACULTY", "GUEST"});

        JButton register = button("Register Member");
        JButton refresh = button("Refresh Members");

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

        register.addActionListener(e -> {
            String memberId = id.getText().trim();
            String memberName = name.getText().trim();
            String memberType = (String) type.getSelectedItem();

            runTask(() -> {
                service.registerMember(
                        memberId, memberName, memberType);
                return service.listMembers();
            }, rows -> {
                fillTable(membersModel, rows);
                status.setText("Member registered successfully.");
            });
        });

        refresh.addActionListener(e -> refreshMembers());

        return panel;
    }

    private JPanel circulationPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JTextField memberId = new JTextField(9);
        JTextField isbn = new JTextField(12);
        JTextField days = new JTextField(5);

        JButton issue = button("Issue Loan");
        JButton returnButton = button("Return Book");
        JButton refresh = button("Refresh Loans");

        form.add(new JLabel("Member ID"));
        form.add(memberId);
        form.add(new JLabel("ISBN"));
        form.add(isbn);
        form.add(new JLabel("Loan Days"));
        form.add(days);
        form.add(issue);
        form.add(returnButton);
        form.add(refresh);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(loansTable), BorderLayout.CENTER);

        issue.addActionListener(e -> {
            String id = memberId.getText().trim();
            String bookIsbn = isbn.getText().trim();

            try {
                int loanDays = Integer.parseInt(
                        days.getText().trim());

                runTask(() -> {
                    service.issueLoan(id, bookIsbn, loanDays);
                    return service.listLoans();
                }, rows -> {
                    fillTable(loansModel, rows);
                    status.setText("Loan issued successfully.");
                });

            } catch (NumberFormatException ex) {
                showError("Loan days must be a valid whole number.");
            }
        });

        returnButton.addActionListener(e -> {

            int row = loansTable.getSelectedRow();

            if (row < 0) {
                showError("Select a loan first.");
                return;
            }

            int loanId = Integer.parseInt(
                    loansTable.getValueAt(row, 0).toString());

            int confirm = JOptionPane.showConfirmDialog(
                    this, "Return selected book?",
                    "Confirm Return", JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            runTask(() -> {
                service.returnBook(loanId);
                return service.listLoans();
            }, rows -> {
                fillTable(loansModel, rows);
                status.setText("Book returned successfully.");
            });
        });

        refresh.addActionListener(e -> refreshLoans());

        return panel;
    }

    private JPanel reservationsPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JTextField memberId = new JTextField(10);
        JTextField isbn = new JTextField(12);

        JButton reserve = button("Reserve");
        JButton cancel = button("Cancel Reservation");
        JButton refresh = button("Refresh");

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

        reserve.addActionListener(e -> {
            String id = memberId.getText().trim();
            String bookIsbn = isbn.getText().trim();

            runTask(() -> {
                service.reserveBook(id, bookIsbn);
                return service.listReservations();
            }, rows -> {
                fillTable(reservationsModel, rows);
                status.setText("Reservation created successfully.");
            });
        });

        cancel.addActionListener(e -> {

            int row = reservationsTable.getSelectedRow();

            if (row < 0) {
                showError("Select a reservation first.");
                return;
            }

            int reservationId = Integer.parseInt(
                    reservationsTable.getValueAt(row, 0).toString());

            int confirm = JOptionPane.showConfirmDialog(
                    this, "Cancel selected reservation?",
                    "Confirm Cancellation",
                    JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            runTask(() -> {
                service.cancelReservation(reservationId);
                return service.listReservations();
            }, rows -> {
                fillTable(reservationsModel, rows);
                status.setText("Reservation cancelled.");
            });
        });

        refresh.addActionListener(e -> refreshReservations());

        return panel;
    }

    private JPanel reportsPanel() {

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton generate = button("Generate Report");
        top.add(generate);

        reportArea.setEditable(false);
        reportArea.setFont(
                new Font("Monospaced", Font.PLAIN, 13));

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea),
                BorderLayout.CENTER);

        generate.addActionListener(e -> runTask(() -> {

            List<String[]> books = service.listBooks();
            List<String[]> members = service.listMembers();
            List<String[]> loans = service.listLoans();
            List<String[]> reservations =
                    service.listReservations();

            int available = 0;
            int checkedOut = 0;
            int active = 0;

            for (String[] book : books) {
                int total = Integer.parseInt(book[3]);
                int remaining = Integer.parseInt(book[4]);

                available += remaining;
                checkedOut += total - remaining;
            }

            for (String[] loan : loans) {
                if ("ACTIVE".equals(loan[8])) {
                    active++;
                }
            }

            return "========== BOOKBRIDGE TSA4 REPORT ==========\n"
                    + "Registered Members: " + members.size()
                    + "\nBook Titles: " + books.size()
                    + "\nAvailable Copies: " + available
                    + "\nChecked-out Copies: " + checkedOut
                    + "\nActive Loans: " + active
                    + "\nReservations: " + reservations.size();

        }, text -> {
            reportArea.setText(text);
            status.setText("Report generated successfully.");
        }));

        return panel;
    }

    private void fillTable(DefaultTableModel tableModel,
                           List<String[]> rows) {

        tableModel.setRowCount(0);

        for (String[] row : rows) {
            tableModel.addRow(row);
        }
    }

    private interface DatabaseTask<T> {
        T execute() throws Exception;
    }

    // Manage the GUI while a background task is running
    private void setBusy(boolean value) {

        busy = value;

        for (JButton button : buttons) {
            button.setEnabled(!value);
        }

        progress.setVisible(value);

        status.setText(value
                ? "Processing database request..."
                : "Ready");
    }

    private <T> void runTask(DatabaseTask<T> task,
                             Consumer<T> onSuccess) {

        if (busy) {
            return;
        }

        setBusy(true);

        new SwingWorker<T, Void>() {

            @Override
            protected T doInBackground() throws Exception {
                return task.execute();
            }

            @Override
            protected void done() {

                try {
                    T result = get();
                    onSuccess.accept(result);

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

                } finally {
                    setBusy(false);
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
                rows -> {
                    fillTable(booksModel, rows);
                    status.setText("Catalog refreshed.");
                });
    }

    private void refreshMembers() {
        runTask(service::listMembers,
                rows -> {
                    fillTable(membersModel, rows);
                    status.setText("Members refreshed.");
                });
    }

    private void refreshLoans() {
        runTask(service::listLoans,
                rows -> {
                    fillTable(loansModel, rows);
                    status.setText("Loans refreshed.");
                });
    }

    private void refreshReservations() {
        runTask(service::listReservations,
                rows -> {
                    fillTable(reservationsModel, rows);
                    status.setText("Reservations refreshed.");
                });
    }
}
