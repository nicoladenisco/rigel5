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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.commonlib5.lambda.LEU;
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
      stm.executeUpdate("DELETE FROM tblPosts");
    }

    AAA_testSchema();
    ABA_testFetchRecords();
    ACA_testSave_boolean();
    ACB_testSave_timestamp();
    ADA_testRemoveDeletedRecords();
    AEA_testRefresh();
    AZA_testGetSelectString();
    BAA_testFetchByPrimaryKeys();
    BAB_testFetchByGenericValues();
    CCA_test_fetchOneRecordOrNew();
    DAA_saveWithInsertAndGetGeneratedKeys();
//    EAA_tdsCompat();
//    EAC_tdsCompat();
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
    System.out.println("testSave_boolean");
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
   * Test of save method, of class TableDataSet.
   */
  public synchronized void ACB_testSave_timestamp()
     throws Exception
  {
    System.out.println("testSave_timestamp");
    boolean intransaction = false;
    try (TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri"))
    {
      List<Record> lsRecs = instance.fetchAllRecords();
      Record r = instance.addRecord();
      r.setValue("idbatteri", 3);
      r.setValue("codice", "bat3");
      r.setValue("descrizione", "Batterio3");
      r.setValue("tiporecord", 1);
      r.setValue("stato_rec", 0);
      r.setValue("id_user", 0);
      r.setValue("id_ucrea", 0);
      r.setValue("ult_modif", "2023-08-07 10:50:36");
      r.setValue("creazione", "20230807105036");
      r.setValue("codiceregionale", "BB");
      int expResult = 3;
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
      assertFalse(lsRecs.isEmpty());
      int numBerforeDelete = lsRecs.size();
      Record todel = lsRecs.get(1);
      todel.markToBeDeleted();
      instance.save();
      instance.removeDeletedRecords();
      List<Record> lsRecs2 = instance.fetchAllRecords();
      assertEquals(numBerforeDelete - 1, lsRecs2.size());
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
      assertFalse(lsRecs.isEmpty());
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
    System.out.println("fetchByGenericValues");
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

  /**
   * Test of save method, of class TableDataSet.
   */
  public synchronized void CCA_test_fetchOneRecordOrNew()
     throws Exception
  {
    System.out.println("fetchOneRecordOrNew");
    boolean intransaction = false;
    try (TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri"))
    {
      Record r = instance.fetchOneRecordOrNew("idbatteri=3", true);
      if(r.toBeSavedWithInsert())
        r.setValue("idbatteri", instance.getNextID());

      r.setValue("codice", "bat3");
      r.setValue("descrizione", "Batterio3");
      r.setValue("tiporecord", 1);
      r.setValue("stato_rec", 0);
      r.setValue("id_user", 0);
      r.setValue("id_ucrea", 0);
      r.setValue("ult_modif", new Date());
      r.setValue("creazione", new Date());
      r.setValue("codiceregionale", "BB");
      int expResult = 1;
      int result = instance.save(intransaction);
      assertEquals(expResult, result);
    }
  }

  public synchronized void DAA_saveWithInsertAndGetGeneratedKeys()
     throws Exception
  {
    System.out.println("saveWithInsertAndGetGeneratedKeys");
    int count = 0;
    try (TableDataSet instance = new TableDataSet(dbe.getConn(), "tblPosts"))
    {
      instance.setPreferInsertAndGetGeneratedKeys(true);

      for(int i = 0; i < 3; i++)
      {
        Record r1 = instance.addRecord();
        count++;
        // aggiunge valori tranne la chiave primaria
        r1.setValue("strContent", "content" + count);
        r1.setValue("strLink", "link" + count);
        r1.setValue("strImage", "image" + count);
        r1.save();
        //r.setValue("", 0);
        //r.setValue("", 0);
        long id = r1.getValue("nId").asLong();
        assertNotEquals(0, id);

        instance.clear();
        Record r2 = instance.fetchOneRecordOrNew("nId=" + id, false);
        assertNotNull(r2);
        r2.setValue("strContent", "2content" + count);
        r2.setValue("strLink", "2link" + count);
        r2.setValue("strImage", "2image" + count);
        r2.save();

        instance.clear();
        List<Record> recs = instance.fetchAllRecords();
        assertTrue(recs.stream()
           .filter(LEU.rethrowPredicate((rr) -> id == r1.getValue("nId").asLong()))
           .findFirst().orElse(null) != null);
      }

      instance.clear();
      List<Record> recs = instance.fetchAllRecords();
      System.out.println("** " + recs);
    }
  }

  public synchronized void EAA_tdsCompat()
     throws Exception
  {
    System.out.println("tdsCompat - inserimento in tabella, db differente da connessione");

    Class.forName("net.sourceforge.jtds.jdbc.Driver");
    try (Connection con = DriverManager.getConnection(
       "jdbc:jtds:sqlserver://192.168.100.243:1433/DIAMANTE", "firma", "firma"))
    {

      DatabaseMetaData dbMeta = con.getMetaData();
      try (ResultSet dbPrimary = dbMeta.getPrimaryKeys("DB_COMUNE", "dbo", "Medici_Proponenti"))
      {
        while(dbPrimary.next())
          System.out.println("pri " + dbPrimary);
      }

      int count = 0;
      try (TableDataSet tds = new TableDataSet(con, "DB_COMUNE..Medici_Proponenti"))
      {
        String where = "Codice_Mnemonico = 'ZZTEST'";
        Record r = tds.fetchOneRecordOrNew(where, true);

        if(r.toBeSavedWithInsert())
        {
          r.setValue("Codice_Numerico", tds.getNextID());
          r.setValue("Codice_Mnemonico", "ZZTEST");
        }

        r.setValue("CognomeNome", "TEST MEDICO");
        r.setValue("flagTipo", "F");
        r.setValue("IDTipo", 4);
        tds.save();

        System.out.println("Inserito " + r);

//        List<Record> lsRecs = tds.fetchAllRecords();
//        if(!lsRecs.isEmpty())
//          System.out.println("PRIMO MEDICO: " + lsRecs.get(0));
      }
    }
  }

  public synchronized void EAB_tdsCompat()
     throws Exception
  {
    System.out.println("tdsCompat - inserimento in tabella, stesso db");

    Class.forName("net.sourceforge.jtds.jdbc.Driver");
    try (Connection con = DriverManager.getConnection(
       "jdbc:jtds:sqlserver://192.168.100.243:1433/DB_COMUNE", "firma", "firma"))
    {

      DatabaseMetaData dbMeta = con.getMetaData();
      try (ResultSet dbPrimary = dbMeta.getPrimaryKeys("DB_COMUNE", "dbo", "Medici_Proponenti"))
      {
        while(dbPrimary.next())
          System.out.println("pri " + dbPrimary);
      }

      int count = 0;
      try (TableDataSet tds = new TableDataSet(con, "Medici_Proponenti"))
      {
        String where = "Codice_Mnemonico = 'ZZTEST'";
        Record r = tds.fetchOneRecordOrNew(where, true);

        if(r.toBeSavedWithInsert())
        {
          r.setValue("Codice_Numerico", tds.getNextID());
          r.setValue("Codice_Mnemonico", "ZZTEST");
        }

        r.setValue("CognomeNome", "TEST MEDICO");
        r.setValue("flagTipo", "F");
        r.setValue("IDTipo", 4);
        tds.save();

        System.out.println("Inserito " + r);

//        List<Record> lsRecs = tds.fetchAllRecords();
//        if(!lsRecs.isEmpty())
//          System.out.println("PRIMO MEDICO: " + lsRecs.get(0));
      }
    }
  }

  public synchronized void EAC_tdsCompat()
     throws Exception
  {
    System.out.println("tdsCompat - inserimento in tabella senza chiave primaria");

    Class.forName("net.sourceforge.jtds.jdbc.Driver");
    try (Connection con = DriverManager.getConnection(
       "jdbc:jtds:sqlserver://192.168.100.243:1433/DB_COMUNE", "firma", "firma"))
    {
      try (TableDataSet tdr = new TableDataSet(con, "REPARTI", new KeyDef("ID")))
      {
        Record r = tdr.fetchOneRecordOrNew("codice = 'PROVA'", true);

        if(r.toBeSavedWithInsert())
        {
          r.setValue("id", tdr.getNextID());
          r.setValue("codice", "PROVA");
        }

        r.setValue("descrizione", "Reparto di prova");
        r.setValue("altaspecialita", 0);
        r.save();

        int idReparto = r.getValue("id").asInt();
        System.out.println("ID=" + idReparto);
      }
    }
  }
}
