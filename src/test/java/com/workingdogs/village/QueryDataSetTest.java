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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.torque.criteria.SqlEnum;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rigel5.db.sql.FiltroData;

/**
 * Test per QueryDataSet.
 * Implicitamente testa anche DataSet Schema e Column.
 *
 * @author Nicola De Nisco
 */
public class QueryDataSetTest
{
  private DerbyTestEnvironment dbe;

  public QueryDataSetTest()
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
    try (Statement stm = dbe.getConn().createStatement())
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
  public void testGetSelectResults()
     throws Exception
  {
    System.out.println("getSelectResults");
    int start = 0;
    int numberOfResults = -1;
    Connection dbCon = dbe.getConn();
    String sSQL = "SELECT * FROM mic_antibiotici";
    try (QueryDataSet instance = new QueryDataSet(dbCon, sSQL))
    {
      List<Record> result = instance.getSelectResults(start, numberOfResults);
      assertEquals(2, result.size());
    }
  }

  @Test
  public void testFetchAllRecords()
     throws Exception
  {
    System.out.println("fetchAllRecords");
    Connection dbCon = dbe.getConn();
    String sSQL = "SELECT * FROM mic_batteri";
    List<Record> result = QueryDataSet.fetchAllRecords(dbCon, sSQL);
    assertEquals(2, result.size());
  }

  @Test
  public void testCaseInsensitiveColumnName()
     throws Exception
  {
    System.out.println("testCaseInsensitiveColumnName");
    Connection dbCon = dbe.getConn();
    String sSQL = "SELECT * FROM mic_batteri ORDER BY idbatteri";
    List<Record> result = QueryDataSet.fetchAllRecords(dbCon, sSQL);
    assertEquals(2, result.size());
    assertEquals(1, result.get(0).getValue("idbatteri").asInt());
    assertEquals(1, result.get(0).getValue("IDBATTERI").asInt());
  }

  @Test
  public void testFiltroData1()
     throws Exception
  {
    System.out.println("testFiltroData");
    Connection dbCon = dbe.getConn();
    FiltroData fd = new FiltroData();
    fd.addWhere("IDBATTERI", SqlEnum.GREATER_EQUAL, 1);
    fd.addSelect("IDBATTERI");
    fd.addOrderby("IDBATTERI");
    try (QueryDataSet qds = new QueryDataSet(dbCon, "codice", "mic_batteri", fd))
    {
      System.out.println("sSQL=" + qds.getSelectString());
      List<Record> result = qds.fetchAllRecords();
      assertEquals(2, result.size());
      assertEquals(1, result.get(0).getValue("idbatteri").asInt());
      assertEquals(1, result.get(0).getValue("IDBATTERI").asInt());
    }
  }

  @Test
  public void testFiltroData2()
     throws Exception
  {
    System.out.println("testFiltroData");
    Connection dbCon = dbe.getConn();
    FiltroData fd = new FiltroData();
    fd.addWhere("IDBATTERI", SqlEnum.GREATER_EQUAL, 1);
    fd.addWhere("IDBATTERI", SqlEnum.IN, Arrays.asList(1, 2));
    fd.addWhere("codice", SqlEnum.ISNOTNULL);
    fd.addSelect("IDBATTERI");
    fd.addOrderby("IDBATTERI");
    try (QueryDataSet qds = new QueryDataSet(dbCon, "codice", "mic_batteri", fd))
    {
      System.out.println("sSQL=" + qds.getSelectString());
      List<Record> result = qds.fetchAllRecords();
      assertEquals(2, result.size());
      assertEquals(1, result.get(0).getValue("idbatteri").asInt());
      assertEquals(1, result.get(0).getValue("IDBATTERI").asInt());
    }
  }
}
