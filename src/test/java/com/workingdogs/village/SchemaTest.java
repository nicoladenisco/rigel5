/*
 * Copyright (C) 2022 Nicola De Nisco
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.workingdogs.village;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test di Schema.java.
 *
 * @author Nicola De Nisco
 */
public class SchemaTest
{
  private DerbyTestEnvironment dbe;

  public SchemaTest()
  {
  }

  @BeforeClass
  public static void setUpClass()
  {
  }

  @AfterClass
  public static void tearDownClass()
  {
  }

  @Before
  public void setUp()
  {
    try
    {
      dbe = DerbyTestEnvironment.getInstance();
      if(!dbe.isOpen())
        dbe.open();

      inserisciDati();
    }
    catch(Exception ex)
    {
      Logger.getLogger(TableDataSetTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @After
  public void tearDown()
  {
  }

  private void inserisciDati()
     throws SQLException, DataSetException
  {
    try ( Statement stm = dbe.getConn().createStatement())
    {
      stm.executeUpdate("DELETE FROM mic_antibiotici");
      stm.executeUpdate("DELETE FROM mic_batteri");
    }

    TableDataSet batteri = new TableDataSet(dbe.getConn(), "mic_batteri");
    {
      Record r = batteri.addRecord();
      r.setValue("idbatteri", 1);
      r.setValue("codice", "bat1");
      r.setValue("descrizione", "Batterio1");
      r.setValue("tiporecord", 1);
      r.setValue("stato_rec", 0);
      r.setValue("id_user", 0);
      r.setValue("id_ucrea", 0);
      r.setValue("ult_modif", new Date());
      r.setValue("creazione", new Date());
      r.setValue("codiceregionale", "AA");
      r.save();
    }
    {
      Record r = batteri.addRecord();
      r.setValue("idbatteri", 2);
      r.setValue("codice", "bat2");
      r.setValue("descrizione", "Batterio2");
      r.setValue("tiporecord", 1);
      r.setValue("stato_rec", 0);
      r.setValue("id_user", 0);
      r.setValue("id_ucrea", 0);
      r.setValue("ult_modif", new Date());
      r.setValue("creazione", new Date());
      r.setValue("codiceregionale", "BB");
      r.save();
    }

    TableDataSet antibiotici = new TableDataSet(dbe.getConn(), "mic_antibiotici");
    {
      Record r = antibiotici.addRecord();
      r.setValue("idantibiotici", 1);
      r.setValue("codice", "ant1");
      r.setValue("descrizione", "antibiotioco1");
      r.setValue("micdose1", "11");
      r.setValue("micdose2", "22");
      r.setValue("farmaco", "farmaco");
      r.setValue("tiporecord", 1);
      r.setValue("stato_rec", 0);
      r.setValue("id_user", 0);
      r.setValue("id_ucrea", 0);
      r.setValue("ult_modif", new Date());
      r.setValue("creazione", new Date());
      r.setValue("codiceregionale", "BB");
    }
    {
      Record r = antibiotici.addRecord();
      r.setValue("idantibiotici", 2);
      r.setValue("codice", "ant2");
      r.setValue("descrizione", "antibiotioco2");
      r.setValue("micdose1", "11");
      r.setValue("micdose2", "22");
      r.setValue("farmaco", "farmaco");
      r.setValue("tiporecord", 1);
      r.setValue("stato_rec", 0);
      r.setValue("id_user", 0);
      r.setValue("id_ucrea", 0);
      r.setValue("ult_modif", new Date());
      r.setValue("creazione", new Date());
      r.setValue("codiceregionale", "BB");
    }
    antibiotici.save();
  }

  @Test
  public void testInitSchemas()
     throws Exception
  {
    System.out.println("initSchemas");
    Connection conn = dbe.getConn();
    Schema.initSchemas(conn);
    // TODO review the generated test code and remove the default call to fail.
    //fail("The test case is a prototype.");
  }

//  @Test
//  public void testMakeKeyHash()
//  {
//    System.out.println("makeKeyHash");
//    String connURL = "pippo";
//    String tableName = "pluto";
//    String expResult = "pippo|pluto";
//    String result = Schema.makeKeyHash(connURL, tableName);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    //fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testSchema_Connection_String()
//     throws Exception
//  {
//    System.out.println("schema");
//    Connection conn = null;
//    String tableName = "";
//    Schema expResult = null;
//    Schema result = Schema.schema(conn, tableName);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testSchema_3args()
//     throws Exception
//  {
//    System.out.println("schema");
//    Connection conn = null;
//    String tableName = "";
//    String columnsAttribute = "";
//    Schema expResult = null;
//    Schema result = Schema.schema(conn, tableName, columnsAttribute);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testAppendTableName()
//  {
//    System.out.println("appendTableName");
//    String app = "";
//    Schema instance = new Schema();
//    instance.appendTableName(app);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testAttributes()
//  {
//    System.out.println("attributes");
//    Schema instance = new Schema();
//    String expResult = "";
//    String result = instance.attributes();
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testColumn_int()
//     throws Exception
//  {
//    System.out.println("column");
//    int i = 0;
//    Schema instance = new Schema();
//    Column expResult = null;
//    Column result = instance.column(i);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testColumn_String()
//     throws Exception
//  {
//    System.out.println("column");
//    String colName = "";
//    Schema instance = new Schema();
//    Column expResult = null;
//    Column result = instance.column(colName);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testGetColumn_String()
//     throws Exception
//  {
//    System.out.println("getColumn");
//    String colName = "";
//    Schema instance = new Schema();
//    Column expResult = null;
//    Column result = instance.getColumn(colName);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testGetColumn_String_String()
//     throws Exception
//  {
//    System.out.println("getColumn");
//    String tableName = "";
//    String colName = "";
//    Schema instance = new Schema();
//    Column expResult = null;
//    Column result = instance.getColumn(tableName, colName);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testGetColumns()
//  {
//    System.out.println("getColumns");
//    Schema instance = new Schema();
//    Column[] expResult = null;
//    Column[] result = instance.getColumns();
//    assertArrayEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testGetTableName()
//     throws Exception
//  {
//    System.out.println("getTableName");
//    Schema instance = new Schema();
//    String expResult = "";
//    String result = instance.getTableName();
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testGetAllTableNames()
//  {
//    System.out.println("getAllTableNames");
//    Schema instance = new Schema();
//    String[] expResult = null;
//    String[] result = instance.getAllTableNames();
//    assertArrayEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testIndex_String()
//     throws Exception
//  {
//    System.out.println("index");
//    String colName = "";
//    Schema instance = new Schema();
//    int expResult = 0;
//    int result = instance.index(colName);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testIndex_String_String()
//     throws Exception
//  {
//    System.out.println("index");
//    String tableName = "";
//    String colName = "";
//    Schema instance = new Schema();
//    int expResult = 0;
//    int result = instance.index(tableName, colName);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testIsSingleTable()
//  {
//    System.out.println("isSingleTable");
//    Schema instance = new Schema();
//    boolean expResult = false;
//    boolean result = instance.isSingleTable();
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testNumberOfColumns()
//  {
//    System.out.println("numberOfColumns");
//    Schema instance = new Schema();
//    int expResult = 0;
//    int result = instance.numberOfColumns();
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testPopulate_3args_1()
//     throws Exception
//  {
//    System.out.println("populate");
//    ResultSetMetaData meta = null;
//    String tableName = "";
//    Connection con = null;
//    Schema instance = new Schema();
//    instance.populate(meta, tableName, con);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testPopulate_3args_2()
//     throws Exception
//  {
//    System.out.println("populate");
//    ResultSet dbMeta = null;
//    String catalog = "";
//    DatabaseMetaData databaseMetaData = null;
//    Schema instance = new Schema();
//    instance.populate(dbMeta, catalog, databaseMetaData);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testSetAttributes()
//  {
//    System.out.println("setAttributes");
//    String attributes = "";
//    Schema instance = new Schema();
//    instance.setAttributes(attributes);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testSetTableName()
//  {
//    System.out.println("setTableName");
//    String tableName = "";
//    Schema instance = new Schema();
//    instance.setTableName(tableName);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testTableName()
//     throws Exception
//  {
//    System.out.println("tableName");
//    Schema instance = new Schema();
//    String expResult = "";
//    String result = instance.tableName();
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testToString()
//  {
//    System.out.println("toString");
//    Schema instance = new Schema();
//    String expResult = "";
//    String result = instance.toString();
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testFindInSchemaIgnoreCaseQuiet_String_String()
//     throws Exception
//  {
//    System.out.println("findInSchemaIgnoreCaseQuiet");
//    String nomeTabella = "";
//    String nomeColonna = "";
//    Schema instance = new Schema();
//    Column expResult = null;
//    Column result = instance.findInSchemaIgnoreCaseQuiet(nomeTabella, nomeColonna);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testFindInSchemaIgnoreCaseQuiet_String()
//     throws Exception
//  {
//    System.out.println("findInSchemaIgnoreCaseQuiet");
//    String nomeColonna = "";
//    Schema instance = new Schema();
//    Column expResult = null;
//    Column result = instance.findInSchemaIgnoreCaseQuiet(nomeColonna);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testFindInSchemaIgnoreCase()
//     throws Exception
//  {
//    System.out.println("findInSchemaIgnoreCase");
//    String nomeColonna = "";
//    Schema instance = new Schema();
//    Column expResult = null;
//    Column result = instance.findInSchemaIgnoreCase(nomeColonna);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testGetPrimaryKeys()
//  {
//    System.out.println("getPrimaryKeys");
//    Schema instance = new Schema();
//    List<Column> expResult = null;
//    List<Column> result = instance.getPrimaryKeys();
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("The test case is a prototype.");
//  }
}
