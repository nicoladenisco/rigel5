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
 * Test per QueryDataSet.
 * Implicitamente testa anche DataSet Schema e Column.
 *
 * @author Nicola De Nisco
 */
public class QueryDataSet2Test
{
  private DerbyTestEnvironment dbe;

  public QueryDataSet2Test()
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
    TableDataSet instance = new TableDataSet(dbe.getConn(), "mic_batteri");
    {
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
    }
    {
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
    }
  }

  /**
   * Test of getSelectResults method, of class QueryDataSet2.
   */
  @Test
  public void testGetSelectResults()
     throws Exception
  {
    System.out.println("getSelectResults");
    int start = 0;
    int numberOfResults = 0;
    QueryDataSet2 instance = new QueryDataSet2();
    List<Record> expResult = null;
    List<Record> result = instance.getSelectResults(start, numberOfResults);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of fetchAllRecords method, of class QueryDataSet2.
   */
  @Test
  public void testFetchAllRecords()
     throws Exception
  {
    System.out.println("fetchAllRecords");
    Connection dbCon = null;
    String sSQL = "";
    List<Record> expResult = null;
    List<Record> result = QueryDataSet2.fetchAllRecords(dbCon, sSQL);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

}
