package io.github.xausky.liquibase;

import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.CreateTableChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.util.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by xausky on 3/9/17.
 */
public class XlsxParser {
    private DatabaseChangeLog changeLog;
    private Workbook workBook;
    private String filename;
    private List<ColumnConfig> currentColumns;

    public XlsxParser(Workbook workBook, String filename){
        this.workBook = workBook;
        this.filename = filename;
    }
    public void parser(DatabaseChangeLog changeLog){
        this.changeLog = changeLog;
        for (int i = 1; i < workBook.getNumberOfSheets(); i++) {
            Sheet sheet = workBook.getSheetAt(i);
            sheetParser(sheet);
        }
    }

    private void sheetParser(Sheet sheet){
        ChangeSet changeSet = null;
        int rowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();
        while(rowNum <= lastRowNum){
            Row row = sheet.getRow(rowNum);
            if(row != null){
                Cell firstCell = row.getCell(0);
                if(firstCell != null){
                    short firstColor = firstCell.getCellStyle().getFontIndex();
                    if(StringUtils.isNotEmpty(firstCell.getStringCellValue()) && Utils.parserCellType(firstCell) == Utils.CellType.DEFAULT){
                        rowNum += changeSetParser(sheet,rowNum);
                    }
                }
            }
            rowNum++;
        }
    }

    private int changeSetParser(Sheet sheet, int rowNum){
        Row row = sheet.getRow(rowNum);
        String author = row.getCell(0).getStringCellValue();
        String id = row.getCell(1).getStringCellValue();
        Cell table = row.getCell(2);
        ChangeSet changeSet = new ChangeSet(id,author,false,true,filename,null,null,changeLog);
        if(table != null && StringUtils.isNotEmpty(table.getStringCellValue())){
            CreateTableChange change = createTableChangeParser(sheet,rowNum);
            changeSet.addChange(change);
        }
        changeLog.addChangeSet(changeSet);
        return 1;
    }

    private CreateTableChange createTableChangeParser(Sheet sheet, int rowNum){
        CreateTableChange change = new CreateTableChange();
        //TODO 多数据库Type适配
        Row typeRow = sheet.getRow(rowNum-1);
        Row row = sheet.getRow(rowNum);
        Row dataRow = sheet.getRow(rowNum+1);
        change.setTableName(row.getCell(2).getStringCellValue());
        for(int i = 3; i <= row.getLastCellNum(); i++){
            Cell cell = row.getCell(i);
            if (cell == null){
                break;
            }
            if(Utils.parserCellType(cell) == Utils.CellType.COMMIT){
                change.setRemarks(cell.getStringCellValue());
                break;
            }
            ColumnConfig column = new ColumnConfig();
            column.setName(cell.getStringCellValue());
            ConstraintsConfig constraints = new ConstraintsConfig();
            String type = "VARCHAR(255)";
            Utils.CellType cellType = Utils.parserCellType(cell);
            if(Utils.CellType.PRIMARY.equals(cellType)){
                type = "INT";
                constraints.setPrimaryKey(true);
                constraints.setNullable(false);
                if(typeRow != null){
                    Cell typeCell = typeRow.getCell(i);
                    if(typeCell != null){
                        if(typeCell.getCellType() == Cell.CELL_TYPE_STRING){
                            type = typeCell.getStringCellValue();
                        }else if(typeCell.getCellType() == Cell.CELL_TYPE_NUMERIC){
                            type = Integer.toString((int)typeCell.getNumericCellValue());
                        }
                    }
                }
                //如果Type列为数字，表示这是一个以该数字自增的列。
                try {
                    column.setIncrementBy(new BigInteger(type));
                    column.setAutoIncrement(true);
                    type = "INT";
                }catch (NumberFormatException e){
                    //如果是数字类型，就需要判断下面数据列是否是非数字，是的话说明该列自增。
                    if("INT".equals(type)){
                        if(dataRow != null){
                            Cell dataCell = dataRow.getCell(i);
                            if(dataCell != null && dataCell.getCellType() == Cell.CELL_TYPE_STRING){
                                column.setAutoIncrement(true);
                            }
                        }
                    }
                }
            }else{
                if(typeRow != null){
                    Cell typeCell = typeRow.getCell(i);
                    if(typeCell != null && StringUtils.isNotEmpty(typeCell.getStringCellValue())){
                        type = typeCell.getStringCellValue();
                    }
                }
                if(Utils.CellType.UNIQUE.equals(cellType)){
                    constraints.setUnique(true);
                    constraints.setNullable(false);
                }else if(Utils.CellType.NOTNULL.equals(cellType)){
                    constraints.setNullable(false);
                }
            }
            column.setType(type);
            column.setConstraints(constraints);
            change.addColumn(column);
        }
        currentColumns = change.getColumns();
        return change;
    }
}
