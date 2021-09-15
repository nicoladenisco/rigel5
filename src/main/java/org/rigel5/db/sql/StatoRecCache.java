/*
 * Copyright (C) 2020 Nicola De Nisco
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
package org.rigel5.db.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.commonlib5.utils.StringOper;
import org.rigel5.SetupHolder;
import org.rigel5.db.DbUtils;

/**
 * Singletone cache per verifica esistenza campo STATO_REC in tabelle.
 *
 * @author Nicola De Nisco
 */
public final class StatoRecCache
{
  private static StatoRecCache instance = null;
  private final HashMap<String, Boolean> statoRecTables = new HashMap<>();

  private StatoRecCache()
  {
  }

  public static StatoRecCache getInstance()
  {
    if(instance == null)
      instance = new StatoRecCache();

    return instance;
  }

  /**
   * Controlla presenza del campo stato_rec nella tabella indicata.
   * Serve a determinare se una tabella supporta il concetto di
   * cancellazione logica.
   * @param nomeTabella tabella da verificare
   * @return vero se la tabella contiene il campo STATO_REC
   */
  public boolean haveStatoRec(String nomeTabella)
  {
    int pos;
    String test = nomeTabella.toLowerCase();
    if((pos = test.lastIndexOf('.')) != -1)
      test = test.substring(pos + 1);

    Boolean rv = statoRecTables.get(test);
    if(rv == null)
    {
      rv = findTable(test);
      statoRecTables.put(test, rv);
    }

    return rv;
  }

  private Boolean findTable(String test)
  {
    if(SetupHolder.getConProd() == null)
      throw new RuntimeException("Connection producer non inizializzato.");

    try
    {
      return SetupHolder.getConProd().functionConnection((con) -> findTable(test, con));
    }
    catch(Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  private boolean findTable(String test, Connection con)
     throws Exception
  {
    try (ResultSet rsMeta = con.getMetaData().getTables(null, null, null, DbUtils.TABLES_FILTER))
    {
      while(rsMeta.next())
      {
        String mTabella = rsMeta.getString(3);

        if(StringOper.isEquNocase(test, mTabella))
          return findColumnTable(con, mTabella);
      }
    }
    return false;
  }

  private boolean findColumnTable(Connection con, String tableName)
     throws SQLException
  {
    try (ResultSet rsMeta = con.getMetaData().getColumns(null, null, tableName, null))
    {
      while(rsMeta.next())
      {
        String mCampo = rsMeta.getString(4);
        if(StringOper.isEquNocase("stato_rec", mCampo))
          return true;
      }
    }
    return false;
  }
}
