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

import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test per TableDataSet.
 * Implicitamente testa anche DataSet Schema e Column.
 *
 * @author Nicola De Nisco
 */
public class TableDataSetTest
{
  private DerbyTestEnvironment dbe;

  public void preparaDatabase()
     throws Exception
  {
    dbe = DerbyTestEnvironment.getInstance();
    if(!dbe.isOpen())
      dbe.open();
  }

  @Test
  public void test()
     throws Exception
  {
    preparaDatabase();

    try (Statement stm = dbe.getConn().createStatement())
    {
      stm.executeUpdate("DELETE FROM mic_antibiotici");
      stm.executeUpdate("DELETE FROM mic_batteri");
    }

    AAA_testSchema();
    ABA_testFetchRecords();
    ACA_testSave_boolean();
    ADA_testRemoveDeletedRecords();
    AEA_testRefresh();
    AZA_testGetSelectString();
    BAA_testFetchByPrimaryKeys();
    BAB_testFetchByGenericValues();
  }

  /**
   * Test funzionalita legate a Schema.
   * @throws Exception
   */
  public synchronized void AAA_testSchema()
     throws Exception
  {
    System.out.println("schema");
    TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri");
    Schema schema = instance.schema();
    assertNotEquals(null, schema);

    Column c1 = schema.column("idbatteri");
    assertNotEquals(null, c1);
    assertTrue(c1.isPrimaryKey());
    assertEquals(1, c1.getPrimaryIndex());

    Column c2 = schema.column("codice");
    assertNotEquals(null, c2);
    assertFalse(c2.isPrimaryKey());
    assertEquals(0, c2.getPrimaryIndex());

    KeyDef kd = instance.keydef();
    assertEquals(1, kd.size());
    assertEquals("IDBATTERI", kd.getAttrib(1));
  }

  /**
   * Test of fetchRecords method, of class TableDataSet.
   */
  public synchronized void ABA_testFetchRecords()
     throws Exception
  {
    System.out.println("fetchRecords");
    TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri");
    Record r = instance.addRecord();
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
    List<Record> lsRecs = instance.fetchAllRecords();
    assertEquals(lsRecs.size(), 1);
  }

  /**
   * Test of save method, of class TableDataSet.
   */
  public synchronized void ACA_testSave_boolean()
     throws Exception
  {
    System.out.println("save");
    boolean intransaction = false;
    try (TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri"))
    {
      List<Record> lsRecs = instance.fetchAllRecords();
      Record r = instance.addRecord();
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
      int expResult = 2;
      int result = instance.save(intransaction);
      assertEquals(expResult, result);
    }
  }

  /**
   * Test of removeDeletedRecords method, of class TableDataSet.
   */
  public synchronized void ADA_testRemoveDeletedRecords()
     throws Exception
  {
    System.out.println("removeDeletedRecords");
    try (TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri"))
    {
      List<Record> lsRecs = instance.fetchAllRecords();
      assertEquals(2, lsRecs.size());
      Record todel = lsRecs.get(1);
      todel.markToBeDeleted();
      instance.save();
      instance.removeDeletedRecords();
      List<Record> lsRecs2 = instance.fetchAllRecords();
      assertEquals(1, lsRecs2.size());
    }
  }

  /**
   * Test of refresh method, of class TableDataSet.
   */
  public synchronized void AEA_testRefresh()
     throws Exception
  {
    System.out.println("refresh");
    try (TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri"))
    {
      List<Record> lsRecs = instance.fetchAllRecords();
      assertEquals(1, lsRecs.size());
      Record tomodify = lsRecs.get(0);
      tomodify.setValue("CODICEREGIONALE", "ZZ");
      assertEquals("ZZ", tomodify.getValue("CODICEREGIONALE").asString());
      instance.refresh(dbe.getConn());
      assertEquals("AA", tomodify.getValue("CODICEREGIONALE").asString());
    }
  }

  /**
   * Test of getSelectString method, of class TableDataSet.
   */
  public synchronized void AZA_testGetSelectString()
     throws Exception
  {
    System.out.println("getSelectString");
    try (TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri"))
    {
      String expResult = "SELECT * FROM mic_batteri";
      String result = instance.getSelectString();
      assertEquals(expResult, result);
    }
  }

  public synchronized void BAA_testFetchByPrimaryKeys()
     throws Exception
  {
    System.out.println("fetchByPrimaryKeys");
    try (TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri"))
    {
      List<Record> totRecs = instance.fetchAllRecords();
      assertFalse(totRecs.isEmpty());
      Map<String, Object> keyValues = new HashMap<>();
      keyValues.put("IDBATTERI", 1);
      List<Record> lsRecs = instance.fetchByPrimaryKeys(keyValues).fetchAllRecords();
      assertEquals(1, lsRecs.size());
    }
  }

  public synchronized void BAB_testFetchByGenericValues()
     throws Exception
  {
    System.out.println("fetchByPrimaryKeys");
    try (TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri"))
    {
      List<Record> totRecs = instance.fetchAllRecords();
      assertFalse(totRecs.isEmpty());
      Map<String, Object> keyValues = new HashMap<>();
      keyValues.put("CODICE", "bat1");
      List<Record> lsRecs = instance.fetchByGenericValues(keyValues).fetchAllRecords();
      assertEquals(1, lsRecs.size());
    }
  }
}
