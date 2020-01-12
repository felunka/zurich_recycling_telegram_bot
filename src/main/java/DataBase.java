import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBase {

    Connection connection;

    public DataBase() {
        connection = null;
        try
        {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:bot.sqlite");
        }
        catch(SQLException e)
        {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        }
    }

    public void initDB() {
        query("CREATE TABLE IF NOT EXISTS locations (chatID bigint, zip int)");
        query("CREATE TABLE IF NOT EXISTS subscriptions (chatID bigint, groupName varchar(32))");

    }

    public boolean query(String query) {
        try {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate(query);
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public ResultSet queryWithResult(String query) {
        try {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            return statement.executeQuery(query);
        } catch(SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
