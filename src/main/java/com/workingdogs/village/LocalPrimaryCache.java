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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.commonlib5.utils.StringOper;

/**
 *
 * @author Nicola De Nisco
 */
public class LocalPrimaryCache
{
  private final String catalog;
  private final DatabaseMetaData dbMeta;
  private final HashMap<String, Map<String, Integer>> pkCache = new HashMap<>();

  public LocalPrimaryCache(String catalog, DatabaseMetaData dbMeta)
  {
    this.dbMeta = dbMeta;
    this.catalog = catalog;
  }

  public int findInPrimary(String metaSchemaName, String metaTableName, String metaColumnName)
     throws SQLException
  {
    String key = StringOper.okStr(metaSchemaName, "NO_SCHEMA") + "|" + metaTableName;

    Map<String, Integer> tablepks = pkCache.get(key);

    if(tablepks == null)
    {
      tablepks = new HashMap<>();
      try (ResultSet dbPrimary = dbMeta.getPrimaryKeys(catalog, metaSchemaName, metaTableName))
      {
        while(dbPrimary.next())
        {
          String nomeColonna = dbPrimary.getString("COLUMN_NAME");
          int kinfo = dbPrimary.getInt("KEY_SEQ");
          tablepks.put(nomeColonna, kinfo);
        }
      }
      pkCache.put(key, tablepks);
    }

    return tablepks.getOrDefault(metaColumnName, 0);
  }
}
