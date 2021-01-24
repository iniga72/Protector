package LuckyCode.protect.database;



import LuckyCode.protector;

import java.util.logging.Level;

public class Error {
    public static void execute(protector plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Couldn't execute MySQL statement: ", ex);
    }
    public static void close(protector plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Failed to close MySQL connection: ", ex);
    }
}
