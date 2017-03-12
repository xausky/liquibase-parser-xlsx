package liquibase.parser.ext;


import io.github.xausky.liquibase.Utils;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import org.junit.Assert;
import org.junit.Test;

import java.sql.*;
import java.util.GregorianCalendar;

public class XlsxParserTest {
    @Test
    public void parserTest() throws SQLException, LiquibaseException {
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost/test?useSSL=false","root","root");
        MySQLDatabase database = new MySQLDatabase();
        database.setConnection(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase("example.xlsx", new Utils(), database);
        liquibase.dropAll();
        liquibase.update(new Contexts());
        Statement statement =  connection.createStatement();
        ResultSet set = statement.executeQuery("SELECT * FROM `group`");
        set.next();
        Assert.assertEquals(set.getInt("id"),1);
        Assert.assertEquals(set.getString("name"),"default");
        Assert.assertEquals(set.getDate("crate").getDate(),11);
        set.close();
        set = statement.executeQuery("SELECT * FROM `user`");
        set.next();
        Assert.assertEquals(set.getInt("id"),1);
        Assert.assertEquals(set.getString("name"),"xausky");
        Assert.assertEquals(set.getInt("group"),1);
        Assert.assertEquals(set.getBoolean("enable"),true);
        set.close();
        statement.close();
        connection.close();
    }
}