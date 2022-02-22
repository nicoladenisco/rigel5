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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
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

  public TableDataSetTest()
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

      try (Statement stm = dbe.getConn().createStatement())
      {
        stm.executeUpdate("DELETE FROM mic_antibiotici");
        stm.executeUpdate("DELETE FROM mic_batteri");
      }
    }
    catch(ClassNotFoundException | SQLException ex)
    {
      Logger.getLogger(TableDataSetTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @After
  public void tearDown()
  {
  }

  /**
   * Test of fetchRecords method, of class TableDataSet.
   */
  @Test
  public void testFetchRecords()
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
  @Test
  public void testSave_boolean()
     throws Exception
  {
    System.out.println("save");
    boolean intransaction = false;
    TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri");
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
    int expResult = 1;
    int result = instance.save(intransaction);
    assertEquals(expResult, result);
  }

  /**
   * Test of removeDeletedRecords method, of class TableDataSet.
   */
  @Test
  public void testRemoveDeletedRecords()
     throws Exception
  {
    System.out.println("removeDeletedRecords");
    TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri");
    instance.fetchRecords();
    instance.removeDeletedRecords();
  }

  /**
   * Test of refresh method, of class TableDataSet.
   */
  @Test
  public void testRefresh()
     throws Exception
  {
    System.out.println("refresh");
    TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri");
    instance.fetchRecords();
    instance.refresh(dbe.getConn());
  }

  /**
   * Test of getSelectString method, of class TableDataSet.
   */
  @Test
  public void testGetSelectString()
     throws Exception
  {
    System.out.println("getSelectString");
    TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri");
    String expResult = "SELECT * FROM mic_batteri";
    String result = instance.getSelectString();
    assertEquals(expResult, result);
  }
}
