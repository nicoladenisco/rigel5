/*
 * Copyright (C) 2023 Nicola De Nisco
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
package org.rigel5.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Set;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.commonlib5.utils.JavaLoggingToCommonLoggingRedirector;
import org.commonlib5.utils.StringOper;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rigel5.SetupHolder;

/**
 *
 * @author Nicola De Nisco
 */
public class IndexHelperTest
{
  private static Connection con;

  public IndexHelperTest()
  {
  }

  @BeforeClass
  public static void setUpClass()
  {
    // configurazione per il logging a console (Log4j/apache commons)
    BasicConfigurator.configure(new ConsoleAppender(
       new PatternLayout("%d [%t] %-5p %c{1} - %m%n")));

    // accoda i messaggi del logger standard di java al layer Log4j
    // NOTA: la configurazione di Log4j ha un default nelle risorse
    // ed un eventuale settaggio esplicito in Log4j.properties
    // letto in SetupData.initSetup
    JavaLoggingToCommonLoggingRedirector.activate();

    try
    {
      Class.forName("org.postgresql.Driver");
      String url = "jdbc:postgresql://192.168.56.1/perseof2dasmelab?user=postgres&password=1234";
      con = DriverManager.getConnection(url);
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }

    SetupHolder.setQueryBuilderClassName("Postgre73QueryBuilder");
  }

  @AfterClass
  public static void tearDownClass()
  {
  }

  @Before
  public void setUp()
  {
  }

  @After
  public void tearDown()
  {
  }

  @Test
  public void testLoadData()
     throws Exception
  {
    System.out.println("loadData");
    String schemaName = "stp";
    String tableName = "transcode";
    IndexHelper instance = new IndexHelper(con, true);
    instance.loadData(schemaName, tableName);
    assertFalse(instance.indici.isEmpty());
    // TODO review the generated test code and remove the default call to fail.
    //fail("The test case is a prototype.");
  }

  @Test
  public void testLoadDataEasy()
     throws Exception
  {
    System.out.println("loadDataEasy");
    IndexHelper instance = new IndexHelper(con, true);
    instance.loadDataEasy("STP.TRANSCODE");
    assertFalse(instance.indici.isEmpty());
  }

  @Test
  public void testGetIndexNames()
     throws Exception
  {
    System.out.println("getIndexNames");
    String schemaName = "stp";
    String tableName = "transcode";
    IndexHelper instance = new IndexHelper(con, true);
    instance.loadData(schemaName, tableName);
    assertFalse(instance.indici.isEmpty());
    Set<String> indexNames = instance.getIndexNames();
    assertFalse(indexNames.isEmpty());
    System.out.println("Indici: " + StringOper.join(indexNames.iterator(), ','));
  }

  @Test
  public void testGetUniqueIndexNames()
     throws Exception
  {
    System.out.println("getUniqueIndexNames");
    String schemaName = "stp";
    String tableName = "transcode";
    IndexHelper instance = new IndexHelper(con, true);
    instance.loadData(schemaName, tableName);
    assertFalse(instance.indici.isEmpty());
    Set<String> indexNames = instance.getUniqueIndexNames();
    assertFalse(indexNames.isEmpty());
    System.out.println("Unique: " + StringOper.join(indexNames.iterator(), ','));
  }

  @Test
  public void testGetIndex()
     throws Exception
  {
    System.out.println("getIndex");
    String schemaName = "stp";
    String tableName = "transcode";
    IndexHelper instance = new IndexHelper(con, true);
    instance.loadData(schemaName, tableName);
    final String idxName = "idx_transcode_1";
    List<IndexHelper.IndiciBean> idxColumn = instance.getIndex(idxName);
    assertNotNull("indice non trovato " + idxName, idxColumn);
    assertFalse("indice senza colonne " + idxName, idxColumn.isEmpty());
    System.out.println("Indice " + idxName + " column " + StringOper.join2(idxColumn, (i) -> i.COLUMN_NAME, ",", "'"));
  }

  @Test
  public void testGetUniqueIndex()
     throws Exception
  {
    System.out.println("getUniqueIndex");
    String schemaName = "stp";
    String tableName = "transcode";
    IndexHelper instance = new IndexHelper(con, true);
    instance.loadData(schemaName, tableName);
    final String idxName = "transcode_pkey";
    List<IndexHelper.IndiciBean> idxColumn = instance.getUniqueIndex(idxName);
    assertNotNull("indice non trovato " + idxName, idxColumn);
    assertFalse("indice senza colonne " + idxName, idxColumn.isEmpty());
    System.out.println("Indice " + idxName + " column " + StringOper.join2(idxColumn, (i) -> i.COLUMN_NAME, ",", "'"));
  }
}
