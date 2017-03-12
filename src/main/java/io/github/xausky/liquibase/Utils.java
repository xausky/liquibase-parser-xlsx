package io.github.xausky.liquibase;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.logging.Logger;
import liquibase.logging.core.DefaultLogger;
import liquibase.resource.ResourceAccessor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Collections;
import java.util.Set;

/**
 * Created by xausky on 3/11/17.
 */
public class Utils implements ResourceAccessor{
    private static final Logger logger = new DefaultLogger();

    public Set<InputStream> getResourcesAsStream(String path) throws IOException {
        return Collections.singleton(this.toClassLoader().getResourceAsStream(path));
    }

    public Set<String> list(String relativeTo, String path, boolean includeFiles, boolean includeDirectories, boolean recursive) throws IOException {
        return Collections.EMPTY_SET;
    }

    public ClassLoader toClassLoader() {
        return this.getClass().getClassLoader();
    }

    public enum CellType {
        PRIMARY,UNIQUE,COMMIT,DEFAULT,UNKNOWN,NOTNULL,AUTOINCRE
    }
    public static CellType parserCellType(Cell cell){
        int r,g,b;
        Workbook workbook = cell.getRow().getSheet().getWorkbook();
        XSSFColor color = ((XSSFFont)workbook.getFontAt(cell.getCellStyle().getFontIndex())).getXSSFColor();
        if(color == null || color.isAuto()){
            return CellType.DEFAULT;
        }
        byte[] rgb = color.getRgb();
        r = 0xFF & rgb[0];
        g = 0xFF & rgb[1];
        b = 0xFF & rgb[2];
        if(Math.abs(r-b) < 16 && g < r/2){
            return CellType.AUTOINCRE;
        }
        if(r > g + b){
            return CellType.PRIMARY;
        }
        if(b > r + g){
            return CellType.UNIQUE;
        }
        if(g > r + b){
            return CellType.NOTNULL;
        }
        if((Math.abs(r-g) + Math.abs(g-b) + Math.abs(b-r)) < 16){
            return CellType.COMMIT;
        }
        return CellType.UNKNOWN;
    }

    public static boolean isNumeric(String str){
        for(int i=str.length();--i>=0;){
            int chr=str.charAt(i);
            if(chr<48 || chr>57)
                return false;
        }
        return true;
    }

    public static void main(String[] args){
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost/test?useSSL=false","root","root");
            MySQLDatabase database = new MySQLDatabase();
            database.setConnection(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase("example.xlsx", new Utils(), database);
            liquibase.dropAll();
            liquibase.update(new Contexts());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
