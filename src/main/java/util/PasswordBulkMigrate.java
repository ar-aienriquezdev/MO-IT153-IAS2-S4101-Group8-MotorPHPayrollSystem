package util;

import db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ONE-TIME migration utility.
 *
 * Converts legacy plaintext values in authentication.passwordHash
 * into BCrypt hashes.
 *
 * Run this once after creating a database backup.
 */
public final class PasswordBulkMigrate {

    private PasswordBulkMigrate() {
    }

    public static void main(String[] args) {

        String selectSql =
                "SELECT userID, passwordHash " +
                "FROM authentication";

        String updateSql =
                "UPDATE authentication " +
                "SET passwordHash = ? " +
                "WHERE userID = ?";

        try (Connection connection =
                     DatabaseConnection.getInstance()
                                       .getConnection()) {

            connection.setAutoCommit(false);

            try {

                List<PasswordRecord> legacyPasswords =
                        new ArrayList<>();

                try (PreparedStatement select =
                             connection.prepareStatement(selectSql);
                     ResultSet rs =
                             select.executeQuery()) {

                    while (rs.next()) {

                        String userID =
                                rs.getString("userID");

                        String storedValue =
                                rs.getString("passwordHash");

                        /*
                         * Only migrate values that are not
                         * already valid BCrypt hashes.
                         */
                        if (!PasswordUtil.isHash(storedValue)) {

                            legacyPasswords.add(
                                    new PasswordRecord(
                                            userID,
                                            storedValue
                                    )
                            );
                        }
                    }
                }

                System.out.println(
                        "Legacy passwords found: "
                        + legacyPasswords.size()
                );

                try (PreparedStatement update =
                             connection.prepareStatement(updateSql)) {

                    for (PasswordRecord record :
                            legacyPasswords) {

                        String hashedPassword =
                                PasswordUtil.hash(
                                        record.plaintextPassword()
                                );

                        update.setString(
                                1,
                                hashedPassword
                        );

                        update.setString(
                                2,
                                record.userID()
                        );

                        update.addBatch();
                    }

                    update.executeBatch();
                }

                connection.commit();

                System.out.println(
                        "BCrypt password migration completed successfully."
                );

            } catch (Exception ex) {

                connection.rollback();

                System.err.println(
                        "Password migration failed. Changes were rolled back."
                );

                throw ex;

            } finally {

                connection.setAutoCommit(true);
            }

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Unable to migrate legacy passwords.",
                    ex
            );
        }
    }

    private record PasswordRecord(
            String userID,
            String plaintextPassword
    ) {
    }
}