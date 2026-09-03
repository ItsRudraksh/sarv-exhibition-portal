package com.sarv.exhibitionportal;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import java.net.ServerSocket;
import java.nio.file.Files;

final class SharedEmbeddedMysql {

    private static final Object LOCK = new Object();
    private static DB db;
    private static int port;

    private SharedEmbeddedMysql() {
    }

    static void ensureStarted() {
        synchronized (LOCK) {
            if (db != null) {
                return;
            }
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            } catch (Exception ex) {
                throw new IllegalStateException("Could not reserve a port for embedded MariaDB", ex);
            }
            try {
                DBConfigurationBuilder config = DBConfigurationBuilder.newBuilder();
                config.setPort(port);
                config.setDataDir(Files.createTempDirectory("exhibition-mariadb-data-").toAbsolutePath().toString());
                config.addArg("--character-set-server=utf8mb4");
                config.addArg("--collation-server=utf8mb4_unicode_ci");
                db = DB.newEmbeddedDB(config.build());
                db.start();
            } catch (Exception ex) {
                throw new IllegalStateException("Could not start embedded MariaDB for tests", ex);
            }
        }
    }

    static String jdbcUrl(String database) {
        ensureStarted();
        try {
            db.createDB(database);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create test database " + database, ex);
        }
        return "jdbc:mysql://127.0.0.1:" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8";
    }
}
