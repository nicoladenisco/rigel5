/*
 * Copyright (C) 2026 Nicola De Nisco
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
package org.rigel5.db.torque;

import com.workingdogs.village.Record;
import com.workingdogs.village.TableDataSet;
import com.workingdogs.village.VillageUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.torque.Torque;
import org.apache.torque.util.TorqueConnection;
import org.apache.torque.util.Transaction;

/**
 * Helper per inizializzare Torque.
 *
 * @author Nicola De Nisco
 */
public class TorqueTestHelper
{
  public Configuration cfg;

  public void init()
     throws Exception
  {
    cfg = buildConfiguration();

    Torque.init(cfg);
  }

  public void shutdown()
  {
    try
    {
      // chiude tutti i database e il motore db
      Torque.shutdown();
    }
    catch(Exception ex)
    {
      System.err.println(ex.getMessage());
    }
  }

  public Configuration buildConfiguration()
     throws Exception
  {
    URL uuu = this.getClass().getResource("/Torque.properties");
    if(uuu == null)
      throw new IOException("ERRORE FATALE: mancata lettura config.properties.");

    Configurations configs = new Configurations();
    FileBasedConfigurationBuilder<PropertiesConfiguration> builder
       = configs.fileBasedBuilder(PropertiesConfiguration.class, uuu);

    return builder.getConfiguration();
  }

  public void buildDb1(Connection con)
     throws Exception
  {
    try(InputStream ires = this.getClass().getResourceAsStream("/db1.sql"))
    {
      VillageUtils.executeSqlScript(con, new InputStreamReader(ires, "UTF-8"), true);
    }
  }

  public void buildDb2(Connection con)
     throws Exception
  {
    try(InputStream ires = this.getClass().getResourceAsStream("/db2.sql"))
    {
      VillageUtils.executeSqlScript(con, new InputStreamReader(ires, "UTF-8"), true);
    }
  }

  public boolean existTable(Connection con, String tableName)
  {
    try
    {
      String query
         = "SELECT TRUE "
         + "  FROM SYS.SYSTABLES "
         + " WHERE TABLENAME = ? AND TABLETYPE = 'T'"; // Leave TABLETYPE out if you don't care about it

      try(PreparedStatement ps = con.prepareStatement(query))
      {
        // il nome tabella deve essere maiuscolo
        ps.setString(1, "TRANSCODE");
        try(ResultSet rs = ps.executeQuery())
        {
          return (rs.next() && rs.getBoolean(1));
        }
      }
    }
    catch(Throwable t)
    {
      return false;
    }
  }

  public void checkAndBuildDb1()
     throws Exception
  {
    try(TorqueConnection con = Transaction.begin())
    {
      if(!existTable(con, "stp.transcode"))
      {
        buildDb1(con);

        try(TableDataSet td = new TableDataSet(con, "stp.transcode"))
        {
          /*
            app VARCHAR(16) NOT NULL,
            tipo VARCHAR(16) NOT NULL,
            codice_caleido VARCHAR(64) NOT NULL,
            codice_app VARCHAR(64) NOT NULL,
           */

          for(int i = 0; i < 10; i++)
          {
            String cc = String.format("C%04d", i);
            String ca = String.format("A%04d", i);

            Record r = td.addRecord();
            r.setValue("app", "test");
            r.setValue("tipo", "A");
            r.setValue("codice_caleido", cc);
            r.setValue("codice_app", ca);
            r.save();
          }
        }

        Transaction.commit(con);
      }
    }
  }
}
