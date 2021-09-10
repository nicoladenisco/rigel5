package com.workingdogs.village;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * This class represents a Column in the database and its associated meta information. A <a href="Record.html">Record</A> is a
 * collection of columns.
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @version $Revision: 568 $
 */
public class Column
{
    /** name of the column */
    private String name = "";

    /** what java.sql.Type is this column? */
    private int columnType = Types.LONGVARCHAR;

    /** name of table that this column belongs to */
    private String tableName = "";

    /** is null allowed for this column? */
    private boolean nullAllowed = false;

    /** is this a read only column? */
    private boolean readOnly = false;

    /**
     * constructor
     */
    public Column()
    {
        this.name = "";
        this.tableName = "";
        this.columnType = Types.LONGVARCHAR;
        this.nullAllowed = false;
        this.readOnly = false;
    }

    /**
     * internal package method for populating a Column instance
     *
     * @param rsmd TODO: DOCUMENT ME!
     * @param colNum TODO: DOCUMENT ME!
     * @param tableName TODO: DOCUMENT ME!
     * @param columnName The name of the column
     *
     * @throws SQLException TODO: DOCUMENT ME!
     */
    public void populate(ResultSetMetaData rsmd, int colNum, String tableName, String columnName)
            throws SQLException
    {
        this.name = columnName;
        this.tableName = tableName;

        this.columnType = rsmd.getColumnType(colNum);
        this.nullAllowed = rsmd.isNullable(colNum) == 1;
    }

    /**
     * internal package method for populating a Column instance
     *
     * @param tableName The name of the table
     * @param columnName The name of the column
     * @param columnTypeName The Data source dependent type name
     * @param columnType The SQL type from java.sql.Types
     * @param isNullable true if NULL allowed.
     *
     */
    public void populate(String tableName, String columnName, String columnTypeName,
            int columnType, boolean isNullable)
    {
        this.name = columnName;
        this.tableName = tableName;
        this.columnType = columnType;
        this.nullAllowed = isNullable;
    }

    /**
     * the name of the column
     *
     * @return the name of the column
     */
    public String name()
    {
        return this.name;
    }

    /**
     * the data type of a column
     *
     * @return the java.sql.Types enum
     */
    public int typeEnum()
    {
        return this.columnType;
    }

    /**
     * does this column allow null?
     *
     * @return whether or not the column has null Allowed
     */
    public boolean nullAllowed()
    {
        return this.nullAllowed;
    }

    /**
     * is this column read only?
     *
     * @return whether or not this column is read only
     */
    public boolean readOnly()
    {
        return this.readOnly;
    }

    /**
     * the type of the column as a string
     *
     * @return the type of the column as a string
     */
    public String type()
    {
        if (isBoolean())
        {
            return "BOOLEAN";
        }
        else if (isByte())
        {
            return "BYTE";
        }
        else if (isShort())
        {
            return "SHORT";
        }
        else if (isInt())
        {
            return "INTEGER";
        }
        else if (isLong())
        {
            return "LONG";
        }
        else if (isFloat())
        {
            return "FLOAT";
        }
        else if (isDouble())
        {
            return "DOUBLE";
        }
        else if (isBigDecimal())
        {
            return "BIGDECIMAL";
        }
        else if (isDate())
        {
            return "DATE";
        }
        else if (isTime())
        {
            return "TIME";
        }
        else if (isTimestamp())
        {
            return "TIMESTAMP";
        }
        else if (isString())
        {
            return "STRING";
        }
        else if (isBinary())
        {
            return "BINARY";
        }
        else if (isVarBinary())
        {
            return "VARBINARY";
        }
        else if (isLongVarBinary())
        {
            return "LONGVARBINARY";
        }

        return "UNKNOWN TYPE: " + typeEnum();
    }

    /**
     * column isBoolean: -7
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isBoolean()
    {
        return this.typeEnum() == Types.BIT;
    }

    /**
     * column isBigDecimal: 2 || 3
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isBigDecimal()
    {
        return (this.typeEnum() == Types.NUMERIC) || (this.typeEnum() == Types.DECIMAL);
    }

    /**
     * column isBinary: -2
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isBinary()
    {
        return this.typeEnum() == Types.BINARY;
    }

    /**
     * column isByte: -6
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isByte()
    {
        return this.typeEnum() == Types.TINYINT;
    }

    /**
     * column isBytes: -4 || -3 || -2
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isBytes()
    {
        return (this.typeEnum() == Types.LONGVARBINARY)
                || (this.typeEnum() == Types.VARBINARY)
                || (this.columnType == Types.BINARY);
    }

    /**
     * column isBytes: 91
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isDate()
    {
        return this.typeEnum() == Types.DATE;
    }

    /**
     * column isDouble: 6 || 8
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isDouble()
    {
        return (this.typeEnum() == Types.FLOAT) || (this.typeEnum() == Types.DOUBLE);
    }

    /**
     * column isFloat: 7
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isFloat()
    {
        return this.typeEnum() == Types.REAL;
    }

    /**
     * column isInt: 4
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isInt()
    {
        return this.typeEnum() == Types.INTEGER;
    }

    /**
     * column isLong: -5
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isLong()
    {
        return this.typeEnum() == Types.BIGINT;
    }

    /**
     * column isShort: 5
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isShort()
    {
        return this.typeEnum() == Types.SMALLINT;
    }

    /**
     * column isString: -1 || -11 || 12
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isString()
    {
        return (this.typeEnum() == Types.LONGVARCHAR)
                || (this.typeEnum() == Types.VARCHAR)
                || (this.typeEnum() == 11);
    }

    /**
     * column isTime: 92
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isTime()
    {
        return this.typeEnum() == Types.TIME;
    }

    /**
     * column isTimestamp: 93
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isTimestamp()
    {
        return this.typeEnum() == Types.TIMESTAMP;
    }

    /**
     * column isVarBinary: -3
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isVarBinary()
    {
        return this.typeEnum() == Types.VARBINARY;
    }

    /**
     * column isLongVarBinary: -4
     *
     * @return TODO: DOCUMENT ME!
     */
    public boolean isLongVarBinary()
    {
        return this.typeEnum() == Types.LONGVARBINARY;
    }

    /**
     * TODO: DOCUMENT ME!
     *
     * @return TODO: DOCUMENT ME!
     */
    public String getTableName()
    {
        return tableName;
    }
}
