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
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

/**
 * A Value represents a single cell in a database table. In other words,
 * it is the cross between a row and column and contains the
 * information held there.
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @version $Revision: 568 $
 */
public class Value
{
  /** the object that is stored in this object */
  private Object valueObject;

  /** the column number that this object came from */
  private final int columnNumber;

  /** what sql type of object is this? */
  private final int type;

  /**
   * Creates a new Value object based on the ResultSet, columnNumber and
   * type
   *
   * @param rs
   * @param columnNumber
   * @param type
   *
   * @exception SQLException
   */
  public Value(ResultSet rs, int columnNumber, int type)
     throws SQLException
  {
    this.columnNumber = columnNumber;
    this.type = type;
    this.valueObject = null;

    if(rs == null)
    {
      return;
    }

    switch(type())
    {
      case Types.BIT:

        String tmp = rs.getString(columnNumber);

        if(tmp == null)
        {
          valueObject = Boolean.FALSE;
        }
        else if(isTrue(tmp))
        {
          valueObject = Boolean.TRUE;
        }
        else
        {
          valueObject = Boolean.FALSE;
        }

        break;

      case Types.TINYINT:
        valueObject = new Byte(rs.getByte(columnNumber));

        break;

      case Types.BIGINT:
        valueObject = new Long(rs.getLong(columnNumber));

        break;

      case Types.SMALLINT:
        valueObject = new Short(rs.getShort(columnNumber));

        break;

      case Types.INTEGER:
        valueObject = new Integer(rs.getInt(columnNumber));

        break;

      case Types.REAL:
        valueObject = new Float(rs.getFloat(columnNumber));

        break;

      case Types.FLOAT:
      case Types.DOUBLE:
        valueObject = new Double(rs.getDouble(columnNumber));

        break;

      case Types.NUMERIC:
      case Types.DECIMAL:

        String number = rs.getString(columnNumber);

        if(number == null)
        {
          valueObject = null;
        }
        else
        {
          valueObject = new BigDecimal(number);
        }

        break;

      case Types.LONGVARBINARY:
      case Types.VARBINARY:
      case Types.BINARY:
        valueObject = rs.getBytes(columnNumber);

        break;

      case Types.BLOB:
        Blob blob = rs.getBlob(columnNumber);
        valueObject = blob != null ? blob.getBytes(1, (int) blob.length()) : null;

        break;

      case Types.LONGVARCHAR:
      case Types.CHAR:
      case Types.VARCHAR:
      case Types.OTHER:
        valueObject = rs.getString(columnNumber);

        break;

      case Types.DATE:
        valueObject = rs.getDate(columnNumber);

        break;

      case Types.TIME:
        valueObject = rs.getTime(columnNumber);

        break;

      case Types.TIMESTAMP:
        valueObject = rs.getTimestamp(columnNumber);

        break;

      case Types.NULL:
        valueObject = null;

        break;

      default:
        valueObject = rs.getString(columnNumber);

        break;
    }

    if(rs.wasNull())
    {
      valueObject = null;
    }

    return;
  }

  /**
   * Sets the value of this object
   *
   * @param value
   */
  public void setValue(Object value)
  {
    this.valueObject = value;
  }

  /**
   * Gets the object from this Value
   *
   * @return the object from this Value
   */
  public Object getValue()
  {
    return this.valueObject;
  }

  /**
   * This is used in Record in order to do a saveWithInsert/Update/Delete
   *
   * @param stmt
   * @param stmtNumber
   *
   * @exception DataSetException
   * @exception SQLException
   */
  public void setPreparedStatementValue(PreparedStatement stmt, int stmtNumber)
     throws DataSetException, SQLException
  {
    if(isNull())
    {
      stmt.setNull(stmtNumber, type());

      return;
    }

    switch(type())
    {
      case Types.BIT:
        stmt.setBoolean(stmtNumber, this.asBoolean());

        break;

      case Types.TINYINT:
        stmt.setByte(stmtNumber, this.asByte());

        break;

      case Types.BIGINT:
        stmt.setLong(stmtNumber, this.asLong());

        break;

      case Types.SMALLINT:
        stmt.setShort(stmtNumber, this.asShort());

        break;

      case Types.INTEGER:
        stmt.setInt(stmtNumber, this.asInt());

        break;

      case Types.REAL:
        stmt.setFloat(stmtNumber, this.asFloat());

        break;

      case Types.FLOAT:
      case Types.DOUBLE:
        stmt.setDouble(stmtNumber, this.asDouble());

        break;

      case Types.NUMERIC:
      case Types.DECIMAL:
        stmt.setBigDecimal(stmtNumber, this.asBigDecimal());

        break;

      case Types.LONGVARBINARY:
      case Types.VARBINARY:
      case Types.BINARY:
      case Types.BLOB:

        // The following form is reported to work and be necessary for
        // Oracle when the blob exceeds 4k.
        byte[] value = this.asBytes();
        stmt.setBinaryStream(stmtNumber,
           new java.io.ByteArrayInputStream(value), value.length);

        break;

      case Types.LONGVARCHAR:
      case Types.CHAR:
      case Types.VARCHAR:
      case Types.OTHER:
        stmt.setString(stmtNumber, this.asString());

        break;

      case Types.DATE:
        stmt.setDate(stmtNumber, this.asDate());

        break;

      case Types.TIME:
        stmt.setTime(stmtNumber, this.asTime());

        break;

      case Types.TIMESTAMP:
        stmt.setTimestamp(stmtNumber, this.asTimestamp());

        break;

      case Types.NULL:
        stmt.setNull(stmtNumber, 0);

        break;

      default:
        stmt.setString(stmtNumber, this.asString());

        break;
    }
  }

  /**
   * Returns the string representation of this object
   *
   * @return a string
   */
  @Override
  public String toString()
  {
    return this.asString();
  }

  /**
   * Returns the string representation of this object
   *
   * @return a string
   */
  public String asString()
  {
    if(isNull())
    {
      return null;
    }
    else if(isString())
    {
      return (String) valueObject;
    }
    else if(isBytes())
    {
      return new String((byte[]) valueObject);
    }
    else
    {
      return valueObject.toString();
    }
  }

  /**
   * Get the value as a BigDecimal
   *
   * @return a BigDecimal
   *
   * @exception DataSetException
   */
  public BigDecimal asBigDecimal()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isBigDecimal())
      {
        return (BigDecimal) valueObject;
      }
      else if(isDouble())
      {
        return new BigDecimal(((Double) valueObject).doubleValue());
      }
      else if(isFloat())
      {
        return new BigDecimal(((Float) valueObject).doubleValue());
      }
      else if(isString() || isInt() || isLong() || isShort() || isByte())
      {
        return new BigDecimal(asString());
      }
      else
      {
        return null;
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Illegal conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a BigDecimal
   *
   * @param scale TODO: DOCUMENT ME!
   *
   * @return a BigDecimal
   *
   * @exception DataSetException
   */
  public BigDecimal asBigDecimal(int scale)
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isBigDecimal())
      {
        return ((BigDecimal) valueObject).setScale(scale);
      }
      else if(isDouble())
      {
        return new BigDecimal(((Double) valueObject).doubleValue())
           .setScale(scale);
      }
      else if(isFloat())
      {
        return new BigDecimal(((Float) valueObject).doubleValue())
           .setScale(scale);
      }
      else if(isString() || isInt() || isLong() || isShort() || isByte())
      {
        return new BigDecimal(asString()).setScale(scale);
      }
      else
      {
        return null;
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a asBoolean
   *
   * @return a boolean
   *
   * @exception DataSetException
   */
  public boolean asBoolean()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return false;
      }
      else if(isBoolean())
      {
        return ((Boolean) valueObject).booleanValue();
      }

      String check = asString();

      return (check == null) ? false : isTrue(check);
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a Boolean object
   *
   * @return a Boolean
   *
   * @exception DataSetException
   */
  public Boolean asBooleanObj()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isBoolean())
      {
        return (Boolean) valueObject;
      }

      String check = asString();

      if(check == null)
      {
        return null;
      }
      else if(isTrue(check))
      {
        return Boolean.TRUE;
      }
      else
      {
        return Boolean.FALSE;
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a asInt
   *
   * @return an int
   *
   * @exception DataSetException
   */
  public int asInt()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return 0;
      }
      else if(isInt())
      {
        return ((Integer) valueObject).intValue();
      }
      else if(isString())
      {
        return Integer.valueOf((String) valueObject).intValue();
      }
      else if(isLong())
      {
        return ((Long) valueObject).intValue();
      }
      else if(isDouble())
      {
        return ((Double) valueObject).intValue();
      }
      else if(isFloat())
      {
        return ((Float) valueObject).intValue();
      }
      else if(isBigDecimal())
      {
        return ((BigDecimal) valueObject).intValue();
      }
      else
      {
        return Integer.valueOf(asString()).intValue();
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a Integer Ojbect
   *
   * @return an Integer
   *
   * @exception DataSetException
   */
  public Integer asIntegerObj()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isInt())
      {
        return ((Integer) valueObject);
      }
      else if(isString() || isDouble() || isFloat() || isBigDecimal()
         || isLong() || isShort() || isByte())
      {
        return new Integer(asString());
      }
      else
      {
        throw new DataSetException("Invalid type for Integer");
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Illegal conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a asByte
   *
   * @return a byte
   *
   * @exception DataSetException
   */
  public byte asByte()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return 0;
      }
      else if(isByte())
      {
        return ((Byte) valueObject).byteValue();
      }
      else if(isString())
      {
        return Integer.valueOf((String) valueObject).byteValue();
      }
      else if(isShort())
      {
        return ((Short) valueObject).byteValue();
      }
      else if(isInt())
      {
        return ((Integer) valueObject).byteValue();
      }
      else if(isLong())
      {
        return ((Long) valueObject).byteValue();
      }
      else if(isDouble())
      {
        return ((Double) valueObject).byteValue();
      }
      else if(isFloat())
      {
        return ((Float) valueObject).byteValue();
      }
      else if(isBigDecimal())
      {
        return ((BigDecimal) valueObject).byteValue();
      }
      else
      {
        return Integer.valueOf(asString()).byteValue();
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a Byte Object
   *
   * @return a Byte
   *
   * @exception DataSetException
   */
  public Byte asByteObj()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isByte())
      {
        return ((Byte) valueObject);
      }
      else if(isString() || isDouble() || isFloat() || isInt()
         || isLong() || isShort() || isBigDecimal())
      {
        return new Byte(asString());
      }
      else
      {
        throw new DataSetException("Invalid type for Byte");
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Illegal conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a asBytes
   *
   * @return a byte array
   *
   * @exception DataSetException
   */
  public byte[] asBytes()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return new byte[0];
      }
      else if(isBytes())
      {
        return (byte[]) valueObject;
      }
      else if(isString())
      {
        return ((String) valueObject).getBytes();
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }

    return new byte[0];
  }

  /**
   * Get the value as a asShort
   *
   * @return a short
   *
   * @exception DataSetException
   */
  public short asShort()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return 0;
      }
      else if(isShort())
      {
        return ((Short) valueObject).shortValue();
      }
      else if(isString())
      {
        return Integer.valueOf((String) valueObject).shortValue();
      }
      else if(isInt())
      {
        return ((Integer) valueObject).shortValue();
      }
      else if(isLong())
      {
        return ((Long) valueObject).shortValue();
      }
      else if(isDouble())
      {
        return ((Double) valueObject).shortValue();
      }
      else if(isFloat())
      {
        return ((Float) valueObject).shortValue();
      }
      else if(isBigDecimal())
      {
        return ((BigDecimal) valueObject).shortValue();
      }
      else
      {
        return Integer.valueOf(asString()).shortValue();
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a Short Object
   *
   * @return a Short
   *
   * @exception DataSetException
   */
  public Short asShortObj()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isShort())
      {
        return ((Short) valueObject);
      }
      else if(isString() || isDouble() || isFloat() || isInt()
         || isLong() || isBigDecimal() || isByte())
      {
        return new Short(asString());
      }
      else
      {
        throw new DataSetException("Invalid type for Short");
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Illegal conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a asLong
   *
   * @return a long
   *
   * @exception DataSetException
   */
  public long asLong()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return 0;
      }
      else if(isLong())
      {
        return ((Long) valueObject).longValue();
      }
      else if(isString())
      {
        return Integer.valueOf((String) valueObject).longValue();
      }
      else if(isShort())
      {
        return ((Short) valueObject).longValue();
      }
      else if(isInt())
      {
        return ((Integer) valueObject).longValue();
      }
      else if(isDouble())
      {
        return ((Double) valueObject).longValue();
      }
      else if(isFloat())
      {
        return ((Float) valueObject).longValue();
      }
      else if(isBigDecimal())
      {
        return ((BigDecimal) valueObject).longValue();
      }
      else
      {
        return Integer.valueOf(asString()).longValue();
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a Long Object
   *
   * @return a Long
   *
   * @exception DataSetException
   */
  public Long asLongObj()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isLong())
      {
        return ((Long) valueObject);
      }
      else if(isString() || isDouble() || isFloat() || isInt()
         || isBigDecimal() || isShort() || isByte())
      {
        return new Long(asString());
      }
      else
      {
        throw new DataSetException("Invalid type for Long");
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Illegal conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a asDouble
   *
   * @return a double
   *
   * @exception DataSetException
   */
  public double asDouble()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return 0.0D;
      }
      else if(isDouble())
      {
        return ((Double) valueObject).doubleValue();
      }
      else if(isString())
      {
        return Integer.valueOf((String) valueObject).doubleValue();
      }
      else if(isShort())
      {
        return ((Short) valueObject).doubleValue();
      }
      else if(isInt())
      {
        return ((Integer) valueObject).doubleValue();
      }
      else if(isLong())
      {
        return ((Long) valueObject).doubleValue();
      }
      else if(isFloat())
      {
        return ((Float) valueObject).doubleValue();
      }
      else if(isBigDecimal())
      {
        return ((BigDecimal) valueObject).doubleValue();
      }
      else
      {
        return Integer.valueOf(asString()).doubleValue();
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a Double Object
   *
   * @return a Double
   *
   * @exception DataSetException
   */
  public Double asDoubleObj()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isDouble())
      {
        return ((Double) valueObject);
      }
      else if(isString() || isBigDecimal() || isFloat() || isInt()
         || isLong() || isShort() || isByte())
      {
        return new Double(asString());
      }
      else
      {
        throw new DataSetException("Invalid type for Double");
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Illegal conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a asFloat
   *
   * @return a float
   *
   * @exception DataSetException
   */
  public float asFloat()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return 0.0F;
      }
      else if(isFloat())
      {
        return ((Float) valueObject).floatValue();
      }
      else if(isString())
      {
        return Integer.valueOf((String) valueObject).floatValue();
      }
      else if(isShort())
      {
        return ((Short) valueObject).floatValue();
      }
      else if(isInt())
      {
        return ((Integer) valueObject).floatValue();
      }
      else if(isLong())
      {
        return ((Long) valueObject).floatValue();
      }
      else if(isDouble())
      {
        return ((Double) valueObject).floatValue();
      }
      else if(isBigDecimal())
      {
        return ((BigDecimal) valueObject).floatValue();
      }
      else
      {
        return Integer.valueOf(asString()).floatValue();
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Bad conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a Float Obj
   *
   * @return a Float
   *
   * @exception DataSetException
   */
  public Float asFloatObj()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isFloat())
      {
        return ((Float) valueObject);
      }
      else if(isString() || isDouble() || isBigDecimal() || isInt()
         || isLong() || isShort() || isByte())
      {
        return new Float(asString());
      }
      else
      {
        throw new DataSetException("Invalid type for Float");
      }
    }
    catch(Exception e)
    {
      throw new DataSetException("Illegal conversion: " + e.toString());
    }
  }

  /**
   * Get the value as a asTime
   *
   * @return a Time
   *
   * @exception DataSetException
   */
  public Time asTime()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isTime())
      {
        return (Time) valueObject;
      }

      Calendar cal = Calendar.getInstance();

      if(isTimestamp())
      {
        cal.setTime((Timestamp) valueObject);

        return new Time(cal.getTime().getTime());
      }
      else if(isUtilDate())
      {
        cal.setTime((java.util.Date) valueObject);

        return new Time(cal.getTime().getTime());
      }
      else if(isString())
      {
        return Time.valueOf((String) valueObject);
      }
      else
      {
        return Time.valueOf(asString());
      }
    }
    catch(IllegalArgumentException a)
    {
      throw new DataSetException("Bad date value - "
         + "Java Time Objects cannot be earlier than 1/1/70");
    }
    catch(Exception b)
    {
      throw new DataSetException("Bad conversion: " + b.toString());
    }
  }

  /**
   * Get the value as a asTimestamp
   *
   * @return a Timestamp
   *
   * @exception DataSetException
   */
  public Timestamp asTimestamp()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isTimestamp())
      {
        return (Timestamp) valueObject;
      }

      if(isTime())
      {
        Calendar cal = Calendar.getInstance();
        cal.setTime((Time) valueObject);

        return new Timestamp(cal.getTime().getTime());
      }
      else if(isUtilDate())
      {
        return new Timestamp(((java.util.Date) valueObject).getTime());
      }
      else if(isString())
      {
        return Timestamp.valueOf((String) valueObject);
      }
      else
      {
        return Timestamp.valueOf(asString());
      }
    }
    catch(IllegalArgumentException a)
    {
      throw new DataSetException("Bad date value - "
         + "Java Timestamp Objects cannot be earlier than 1/1/70");
    }
    catch(Exception b)
    {
      throw new DataSetException("Bad conversion: " + b.toString());
    }
  }

  /**
   * Get the value as a asDate
   *
   * @return a java.sql.Date
   *
   * @exception DataSetException
   */
  public java.sql.Date asDate()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isDate())
      {
        return (java.sql.Date) valueObject;
      }

      Calendar cal = Calendar.getInstance();

      if(isTimestamp())
      {
        Timestamp ts = (Timestamp) valueObject;
        long date = ts.getTime();
        int nanos = ts.getNanos();

        return new java.sql.Date(date + (nanos / 1000000));
      }
      else if(isTime())
      {
        cal.setTime((Time) valueObject);

        return java.sql.Date.valueOf(cal.get(Calendar.YEAR) + "-"
           + leadingZero(cal.get(Calendar.MONTH) + 1) + "-"
           + leadingZero(cal.get(Calendar.DAY_OF_MONTH)));
      }
      else if(isUtilDate())
      {
        cal.setTime((java.util.Date) valueObject);

        return java.sql.Date.valueOf(cal.get(Calendar.YEAR) + "-"
           + leadingZero(cal.get(Calendar.MONTH) + 1) + "-"
           + leadingZero(cal.get(Calendar.DAY_OF_MONTH)));
      }
      else if(isString())
      {
        return java.sql.Date.valueOf((String) valueObject);
      }
      else
      {
        return java.sql.Date.valueOf(asString());
      }
    }
    catch(IllegalArgumentException a)
    {
      throw new DataSetException("Bad date value - "
         + "Java Timestamp Objects cannot be earlier than 1/1/70");
    }
    catch(Exception b)
    {
      throw new DataSetException("Bad conversion: " + b.toString());
    }
  }

  /**
   * Get the value as a asUtilDate
   *
   * @return a java.util.Date
   *
   * @exception DataSetException
   */
  public java.util.Date asUtilDate()
     throws DataSetException
  {
    try
    {
      if(isNull())
      {
        return null;
      }
      else if(isUtilDate())
      {
        return (java.util.Date) valueObject;
      }

      Calendar cal = Calendar.getInstance();

      if(isTimestamp())
      {
        Timestamp ts = (Timestamp) valueObject;
        long date = ts.getTime();
        int nanos = ts.getNanos();

        return new java.util.Date(date + (nanos / 1000000));
      }
      else if(isTime())
      {
        cal.setTime((Time) valueObject);

        return java.sql.Date.valueOf(cal.get(Calendar.YEAR) + "-"
           + leadingZero(cal.get(Calendar.MONTH) + 1) + "-"
           + leadingZero(cal.get(Calendar.DAY_OF_MONTH)));
      }
      else if(isUtilDate())
      {
        cal.setTime((java.util.Date) valueObject);

        return java.sql.Date.valueOf(cal.get(Calendar.YEAR) + "-"
           + leadingZero(cal.get(Calendar.MONTH) + 1) + "-"
           + leadingZero(cal.get(Calendar.DAY_OF_MONTH)));
      }
      else
      {
        return null;
      }
    }
    catch(IllegalArgumentException a)
    {
      throw new DataSetException("Bad date value - "
         + "Java java.util.Date Objects cannot be earlier than 1/1/70");
    }
    catch(Exception b)
    {
      throw new DataSetException("Bad conversion: " + b.toString());
    }
  }

  /**
   * Is the value a isBigDecimal
   *
   * @return true if BigDecimal
   */
  public boolean isBigDecimal()
  {
    return valueObject instanceof BigDecimal;
  }

  /**
   * Is the value a isByte
   *
   * @return true if is Byte
   */
  public boolean isByte()
  {
    return valueObject instanceof Byte;
  }

  /**
   * Is the value a isBytes
   *
   * @return true if is byte[]
   */
  public boolean isBytes()
  {
    return valueObject instanceof byte[];
  }

  /**
   * Is the value a isDate
   *
   * @return true if is java.sql.Date
   */
  public boolean isDate()
  {
    return valueObject instanceof java.sql.Date;
  }

  /**
   * Is the value a isShort
   *
   * @return true if is Short
   */
  public boolean isShort()
  {
    return valueObject instanceof Short;
  }

  /**
   * Is the value a isInt
   *
   * @return true if is Integer
   */
  public boolean isInt()
  {
    return valueObject instanceof Integer;
  }

  /**
   * Is the value a isLong
   *
   * @return true if is Long
   */
  public boolean isLong()
  {
    return valueObject instanceof Long;
  }

  /**
   * Is the value a isDouble
   *
   * @return true if is Double
   */
  public boolean isDouble()
  {
    return valueObject instanceof Double;
  }

  /**
   * Is the value a isFloat
   *
   * @return true if is Float
   */
  public boolean isFloat()
  {
    return valueObject instanceof Float;
  }

  /**
   * Is the value a isBoolean
   *
   * @return true if is Boolean
   */
  public boolean isBoolean()
  {
    return valueObject instanceof Boolean;
  }

  /**
   * Is the value a isNull
   *
   * @return true if is null
   */
  public boolean isNull()
  {
    return valueObject == null;
  }

  /**
   * Is the value a isString
   *
   * @return true if is String
   */
  public boolean isString()
  {
    return valueObject instanceof String;
  }

  /**
   * Is the value a isTime
   *
   * @return true if is java.sql.Time
   */
  public boolean isTime()
  {
    return valueObject instanceof java.sql.Time;
  }

  /**
   * Is the value a isTimestamp
   *
   * @return true if is java.sql.Timestamp
   */
  public boolean isTimestamp()
  {
    return valueObject instanceof java.sql.Timestamp;
  }

  /**
   * Is the value a isUtilDate
   *
   * @return true if is java.util.Date
   */
  public boolean isUtilDate()
  {
    return valueObject instanceof java.util.Date;
  }

  /**
   * Return the type of this value
   *
   * @return the type of this value
   */
  public int type()
  {
    return this.type;
  }

  /**
   * Gets the columnNumber which this value represents.
   *
   * @return an int
   */
  public int columnNumber()
  {
    return this.columnNumber;
  }

  /**
   * DOCUMENT ME!
   *
   * @param value TODO: DOCUMENT ME!
   *
   * @return true if (true || t | yes | y | 1)
   */
  private boolean isTrue(String value)
  {
    return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("t")
       || value.equalsIgnoreCase("yes")
       || value.equalsIgnoreCase("y") || value.equals("1"));
  }

  /**
   * Convert an int to a two digit String with a leading zero where necessary.
   *
   * @param val The value to be converted.
   * @return A two character String with leading zero.
   */
  private String leadingZero(int val)
  {
    return (val < 10 ? "0" : "") + val;
  }
}
