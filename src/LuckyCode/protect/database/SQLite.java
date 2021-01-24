package LuckyCode.protect.database;

import LuckyCode.protector;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class SQLite extends Database{
    public SQLite(protector instance){
        super(instance);
    }
    public String SQLiteCreateTokensTable = "CREATE TABLE IF NOT EXISTS Protector (`player` varchar(32) NOT NULL,`region` VARCHAR(4000) NOT NULL);";


    // SQL creation stuff, You can leave the blow stuff untouched.
    public Connection getSQLConnection() {
        File dataFolder = new File(plugin.getDataFolder(), "database.db");
        if (!dataFolder.exists()){
            try {
                dataFolder.createNewFile();
            } catch (IOException ignored) {

            }
        }
        try {
            if(connection_log!=null&&!connection_log.isClosed()){
                return connection_log;
            }
            Class.forName("org.sqlite.JDBC");
            connection_log = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            return connection_log;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    public void load() {
        connection_log = getSQLConnection();
        try {
            Statement s = connection_log.createStatement();
            s.executeUpdate(SQLiteCreateTokensTable);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initialize();
    }
}

