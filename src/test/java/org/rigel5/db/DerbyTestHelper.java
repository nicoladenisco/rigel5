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

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import org.commonlib5.utils.CommonFileUtils;

/**
 * Classe di supporto per l'utilizzo del db Derby per le unit di test.
 *
 * @author Nicola De Nisco
 */
public class DerbyTestHelper
{
  public static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String protocol = "jdbc:derby:";

  public Connection con;
  public Properties props = new Properties();
  public File dbPath = new File("/tmp/derbyDB");

  public void init()
     throws Exception
  {
    // carica il driver e inizializza il motore db
    Class.forName(driver).newInstance();

    con = DriverManager.getConnection(protocol + dbPath.getAbsolutePath() + ";create=true", props);
  }

  public void shutdown()
     throws Exception
  {
    // chiude tutti i database e il motore db
    DriverManager.getConnection("jdbc:derby:;shutdown=true");
  }

  public void clear()
  {
    try
    {
      CommonFileUtils.deleteDir(dbPath);
    }
    catch(Exception ex)
    {
    }
  }

  public void buildDb1()
     throws Exception
  {
    try(InputStream ires = this.getClass().getResourceAsStream("/db1.sql"))
    {
      DbUtils.executeSqlScript(con, new InputStreamReader(ires, "UTF-8"), true);
    }
  }

  public void buildDb2()
     throws Exception
  {
    try(InputStream ires = this.getClass().getResourceAsStream("/db2.sql"))
    {
      DbUtils.executeSqlScript(con, new InputStreamReader(ires, "UTF-8"), true);
    }
  }
}
