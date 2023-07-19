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
package org.rigel5.db;

import org.junit.After;
import org.junit.Before;

/**
 * Test per TableHelper.java.
 *
 * @author Nicola De Nisco
 */
public class TableHelperTest
{
  private static final DerbyTestHelper db = new DerbyTestHelper();

  public TableHelperTest()
  {
  }

//  @BeforeClass
//  public static void setUpClass()
//  {
//    // configurazione per il logging a console (Log4j/apache commons)
//    BasicConfigurator.configure(new ConsoleAppender(
//       new PatternLayout("%d [%t] %-5p %c{1} - %m%n")));
//
//    // accoda i messaggi del logger standard di java al layer Log4j
//    // NOTA: la configurazione di Log4j ha un default nelle risorse
//    // ed un eventuale settaggio esplicito in Log4j.properties
//    // letto in SetupData.initSetup
//    JavaLoggingToCommonLoggingRedirector.activate();
//
//    try
//    {
//      db.init();
//      db.buildDb1();
//    }
//    catch(Exception ex)
//    {
//      ex.printStackTrace();
//    }
//  }
//
//  @AfterClass
//  public static void tearDownClass()
//  {
//    try
//    {
//      db.shutdown();
//    }
//    catch(Exception ex)
//    {
//      ex.printStackTrace();
//    }
//  }
  @Before
  public void setUp()
  {
  }

  @After
  public void tearDown()
  {
  }

//  @Test
//  public void testDumpRelation()
//     throws Exception
//  {
//    System.out.println("dumpRelation");
//    String schemaName = "inf";
//    String tableName = "in_accettazioni";
//    TableHelperDelete instance = new TableHelperDelete(con, false);
//    instance.loadData(schemaName, tableName);
//    System.out.println("--- ESPORTATE --------------------------------");
//    instance.dumpEsportate(System.out);
//    System.out.println("--- IMPORTATE --------------------------------");
//    instance.dumpImportate(System.out);
//
////    int[] primaryKeys =
////    {
////      150800, 150801, 150802, 150803, 150804, 150805, 150806, 150807, 150808, 150809
////    };
////
////    instance.prepareDeleteCascade("in_accettazioni_id", primaryKeys);
////
////    System.out.println("Relazioni circolari: " + instance.getRecurse());
////
////    instance.executeDelete();
//    // TODO review the generated test code and remove the default call to fail.
//    if(false)
//      fail("The test case is a prototype.");
//  }
//
//  @Test
//  public void testDumpRelation2()
//     throws Exception
//  {
//    System.out.println("dumpRelation2");
//    String schemaName = "inf";
//    String tableName = "in_parametri";
//    TableHelperDelete instance = new TableHelperDelete(con, false);
//    instance.loadData(schemaName, tableName);
//    System.out.println("--- ESPORTATE --------------------------------");
//    instance.dumpEsportate(System.out);
//    System.out.println("--- IMPORTATE --------------------------------");
//    instance.dumpImportate(System.out);
//  }
}
