/*
 * Copyright (C) 2024 Nicola De Nisco
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

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.commonlib5.utils.Classificatore;
import org.commonlib5.utils.CommonFileUtils;
import org.commonlib5.utils.JavaLoggingToCommonLoggingRedirector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.rigel5.SetupHolder;

/**
 * Test per TableHelper.
 *
 * @author Nicola De Nisco
 */
public class TableHelperTest
{
  private static final DerbyTestHelper db = new DerbyTestHelper();

  public TableHelperTest()
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
      db.init();
      db.buildDb2();
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
    try
    {
      db.shutdown();
    }
    catch(Exception ex)
    {
      // ignorata
    }

    db.clear();
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
  public void testEsportate()
     throws Exception
  {
    System.out.println("testEsportate");

    TableHelper th = new TableHelper(db.con, true);
    th.loadDataEasy("TURBINE_USER");
    th.dumpEsportate(System.out);
    th.dumpImportate(System.out);
  }

  @Test
  public void testImportate()
     throws Exception
  {
    System.out.println("testImportate");

    TableHelper th = new TableHelper(db.con, true);
    th.loadDataEasy("TURBINE_ROLE_PERMISSION");
    th.dumpEsportate(System.out);
    th.dumpImportate(System.out);
  }
}
