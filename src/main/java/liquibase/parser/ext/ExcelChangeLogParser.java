package liquibase.parser.ext;

import io.github.xausky.liquibase.Utils;
import io.github.xausky.liquibase.XlsxParser;
import liquibase.change.Change;
import liquibase.change.ColumnConfig;
import liquibase.change.core.CreateTableChange;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.ChangeLogParseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.core.ParsedNode;
import liquibase.resource.ResourceAccessor;
import liquibase.structure.core.Column;
import liquibase.util.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by xausky on 3/9/17.
 */
public class ExcelChangeLogParser implements ChangeLogParser {
    public DatabaseChangeLog parse(String filename, ChangeLogParameters changeLogParameters, ResourceAccessor resourceAccessor) throws ChangeLogParseException {
        DatabaseChangeLog changeLog = new DatabaseChangeLog();
        try {
            InputStream inputStream = (InputStream)resourceAccessor.getResourcesAsStream(filename).toArray()[0];
            Workbook workbook = WorkbookFactory.create(inputStream);
            XlsxParser parser = new XlsxParser(workbook,filename);
            parser.parser(changeLog);
        }catch (Exception e){
            e.printStackTrace();
        }
        return changeLog;
    }

    public boolean supports(String changeLogFile, ResourceAccessor resourceAccessor) {
        return changeLogFile.endsWith(".xlsx");
    }

    public int getPriority() {
        return PRIORITY_DEFAULT;
    }
}
