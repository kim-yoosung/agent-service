package com.example.tracing.dbtracing;

import com.sun.rowset.CachedRowSetImpl;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class PrepareStatementExecuteHandler {

    public static String exportToFile(File outFile, ResultSet rs) throws SQLException {
        CachedRowSet crs = null;

        try {
            rs.beforeFirst(); // 커서 초기화
            crs = copyResultSetFully(rs); // ✅ ResultSet -> CachedRowSet

        } catch (SQLException e) {
            System.out.println("SQLException during hard copy to CachedRowSet, " + e.getMessage());
            throw e;
        }

        // 2. 직렬화해서 파일로 저장
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outFile))) {
            oos.writeObject(crs);
        } catch (IOException e) {
            System.out.println("Failed to generate blob file: " + outFile.getName() + ", " + e.getMessage());
        }

        // 3. row 수 확인 (선택)
        crs.beforeFirst();
        int crsLength = 0;
        while (crs.next()) {
            crsLength++;
        }
        crs.beforeFirst();

        System.out.println("CachedRowSet has " + crsLength + " rows after hard-copy");

        return outFile.getAbsolutePath();
    }

    public static CachedRowSet copyResultSetFully(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int colCount = rsmd.getColumnCount();

        RowSetMetaDataImpl rowSetMeta = new RowSetMetaDataImpl();
        rowSetMeta.setColumnCount(colCount);

        for (int i = 1; i <= colCount; i++) {
            rowSetMeta.setColumnName(i, rsmd.getColumnName(i));
            rowSetMeta.setColumnLabel(i, rsmd.getColumnLabel(i));
            rowSetMeta.setColumnType(i, rsmd.getColumnType(i));
            rowSetMeta.setColumnTypeName(i, rsmd.getColumnTypeName(i));
            rowSetMeta.setNullable(i, rsmd.isNullable(i));
            rowSetMeta.setTableName(i, rsmd.getTableName(i));
            rowSetMeta.setPrecision(i, rsmd.getPrecision(i));
            rowSetMeta.setScale(i, rsmd.getScale(i));
            rowSetMeta.setAutoIncrement(i, rsmd.isAutoIncrement(i));
        }

        CachedRowSetImpl crs = new CachedRowSetImpl();
        crs.setMetaData(rowSetMeta);

        // 데이터를 CachedRowSet에 복사
        int rowIndex = 0;
        while (rs.next()) {
            crs.moveToInsertRow();
            for (int i = 1; i <= colCount; i++) {
                crs.updateObject(i, rs.getObject(i));
            }
            crs.insertRow();
            crs.moveToCurrentRow();
            rowIndex++;
        }

        System.out.println("Copied rowIndex: " + rowIndex);
        crs.beforeFirst(); // 커서 초기화

        return crs;
    }

}
