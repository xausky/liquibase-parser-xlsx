package io.github.xausky.liquibase;

import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.InsertDataChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.util.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;

import java.math.BigInteger;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * Created by xausky on 3/9/17.
 */
public class XlsxParser {
    private DatabaseChangeLog changeLog;
    private Workbook workBook;
    private String filename;
    private CreateTableChange currentTable;
    private int rowNum;
    private int lastRowNum;
    private Long autoIncValue;

    public XlsxParser(Workbook workBook, String filename) {
        this.workBook = workBook;
        this.filename = filename;
    }

    public void parser(DatabaseChangeLog changeLog) {
        this.changeLog = changeLog;
        for (int i = 1; i < workBook.getNumberOfSheets(); i++) {
            Sheet sheet = workBook.getSheetAt(i);
            sheetParser(sheet);
        }
    }

    private void sheetParser(Sheet sheet) {
        ChangeSet changeSet = null;
        currentTable = null;
        rowNum = sheet.getFirstRowNum();
        lastRowNum = sheet.getLastRowNum();
        while (rowNum <= lastRowNum) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                Cell firstCell = row.getCell(0);
                if (firstCell != null) {
                    if (StringUtils.isNotEmpty(firstCell.getStringCellValue())) {
                        changeSetParser(sheet);
                    }
                }
            }
            rowNum++;
        }
    }

    private void changeSetParser(Sheet sheet) {
        Row row = sheet.getRow(rowNum);
        String author = row.getCell(0).getStringCellValue();
        String id = row.getCell(1).getStringCellValue();
        Cell table = row.getCell(2);
        ChangeSet changeSet = new ChangeSet(id, author, false, true, filename, null, null, changeLog);
        if (table != null && StringUtils.isNotEmpty(table.getStringCellValue())) {
            CreateTableChange change = createTableChangeParser(sheet);
            changeSet.addChange(change);
        }
        InsertDataChange change = insertDataChangeParser(sheet);
        while (change != null) {
            changeSet.addChange(change);
            change = insertDataChangeParser(sheet);
        }
        changeLog.addChangeSet(changeSet);
    }

    private InsertDataChange insertDataChangeParser(Sheet sheet) {
        if (rowNum <= lastRowNum) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                return null;
            }
            Cell firstCell = row.getCell(0);
            if (firstCell != null && StringUtils.isNotEmpty(firstCell.getStringCellValue())) {
                return null;
            }
            if (currentTable == null) {
                return null;
            }
            InsertDataChange change = new InsertDataChange();
            List<ColumnConfig> currentColumns = currentTable.getColumns();
            change.setTableName(currentTable.getTableName());
            int blankColumnCount = 0;
            for (int i = 0; i < currentColumns.size(); i++) {
                ColumnConfig currentColumn = currentColumns.get(i);
                ColumnConfig column = new ColumnConfig();
                column.setName(currentColumn.getName());
                Cell cell = row.getCell(3 + i);
                switch (cell.getCellType()) {
                    case Cell.CELL_TYPE_STRING:
                        if(currentColumn.isAutoIncrement() != null && currentColumn.isAutoIncrement()){
                            autoIncValue++;
                            column.setValueNumeric(autoIncValue);
                            cell.setCellValue(autoIncValue);
                        }else {
                            column.setValue(cell.getStringCellValue());
                        }
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            column.setValueDate(new Timestamp(cell.getDateCellValue().getTime()));
                        } else {
                            if(currentColumn.isAutoIncrement() != null && currentColumn.isAutoIncrement()){
                                autoIncValue = (long)cell.getNumericCellValue();
                            }
                            column.setValueNumeric(cell.getNumericCellValue());
                        }
                        break;
                    case Cell.CELL_TYPE_BOOLEAN:
                        column.setValueBoolean(cell.getBooleanCellValue());
                        break;
                    case Cell.CELL_TYPE_FORMULA:
                        CellValue value = workBook.getCreationHelper().createFormulaEvaluator().evaluate(cell);
                        switch (value.getCellType()){
                            case Cell.CELL_TYPE_STRING:
                                column.setValue(value.getStringValue());
                                break;
                            case Cell.CELL_TYPE_NUMERIC:
                                column.setValueNumeric(value.getNumberValue());
                                break;
                            case Cell.CELL_TYPE_BOOLEAN:
                                column.setValueBoolean(value.getBooleanValue());
                                break;
                        }

                        break;
                    case Cell.CELL_TYPE_BLANK:
                        blankColumnCount++;
                        column.setValue(null);
                        break;
                }
                change.addColumn(column);
            }
            rowNum++;
            if(blankColumnCount >= currentColumns.size()){
                return null;
            }
            return change;

        }else {
            return null;
        }
    }

    private CreateTableChange createTableChangeParser(Sheet sheet) {
        CreateTableChange change = new CreateTableChange();
        //TODO 多数据库Type适配
        Row typeRow = sheet.getRow(rowNum - 1);
        Row row = sheet.getRow(rowNum);
        Row dataRow = sheet.getRow(rowNum + 1);
        change.setTableName(row.getCell(2).getStringCellValue());
        for (int i = 3; i <= row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                break;
            }
            if (Utils.parserCellType(cell) == Utils.CellType.COMMIT) {
                change.setRemarks(cell.getStringCellValue());
                break;
            }
            ColumnConfig column = new ColumnConfig();
            column.setName(cell.getStringCellValue());
            ConstraintsConfig constraints = new ConstraintsConfig();
            Utils.CellType cellType = Utils.parserCellType(cell);
            String type = "VARCHAR(255)";
            if (Utils.CellType.AUTOINCRE.equals(cellType)) {
                constraints.setPrimaryKey(true);
                column.setAutoIncrement(true);
                type = "INT";
            } else if (Utils.CellType.PRIMARY.equals(cellType)) {
                constraints.setPrimaryKey(true);
                type = "INT";
            } else if (Utils.CellType.UNIQUE.equals(cellType)) {
                constraints.setUnique(true);
                constraints.setNullable(false);
            } else if (Utils.CellType.NOTNULL.equals(cellType)) {
                constraints.setNullable(false);
            }
            if (typeRow != null) {
                Cell typeCell = typeRow.getCell(i);
                if (typeCell != null && StringUtils.isNotEmpty(typeCell.getStringCellValue())) {
                    type = typeCell.getStringCellValue();
                }
            }
            column.setType(type);
            column.setConstraints(constraints);
            change.addColumn(column);
        }
        currentTable = change;
        autoIncValue = 0L;
        rowNum++;
        return change;
    }
}
