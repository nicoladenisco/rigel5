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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlib5.utils.StringOper;

/**
 * Utility per la cancellazione ricorsiva di record in tabella.
 *
 * @author Nicola De Nisco
 */
public class TableHelperDelete extends TableHelper
{
  private static final Log log = LogFactory.getLog(TableHelperDelete.class);

  protected final List<String> comandi = new ArrayList<>();
  protected final List<String> recurse = new ArrayList<>();

  public TableHelperDelete(Connection con, boolean dryrun)
     throws SQLException
  {
    super(con, dryrun);
  }

  public void prepareDeleteCascade(String fieldPrimary, int primaryKey)
     throws Exception
  {
    int[] primaryKeys = new int[1];
    primaryKeys[0] = primaryKey;
    prepareDeleteCascade(fieldPrimary, primaryKeys);
  }

  public void prepareDeleteCascade(String fieldPrimary, int[] primaryKeys)
     throws Exception
  {
    Stack<String> sttable = new Stack<>();
    deleteCascade(sttable, fieldPrimary, primaryKeys);
  }

  protected void deleteCascade(Stack<String> sttable, String fieldPrimary, int[] primaryKeys)
     throws Exception
  {
    String key = schemaName + "." + tableName;
    if(sttable.contains(key))
    {
      log.warn("Rilevata dipendanza circolare fra tabelle: " + sttable);
      recurse.add(sttable.toString());
      return;
    }

    sttable.push(key);
    deleteTable(fieldPrimary, primaryKeys);

    HashMap<String, int[]> mapKeys = new HashMap<>();
    mapKeys.put(fieldPrimary, primaryKeys);

    for(Map.Entry<String, List<RelazioniBean>> entry : esportate.entrySet())
    {
      String nome = entry.getKey();
      List<RelazioniBean> lsRel = entry.getValue();

      if(lsRel.size() > 1)
        throw new Exception("Funzionamento limitato a 1 colonna esporata");

      RelazioniBean b = lsRel.get(0);
      int[] alternateKeys = mapKeys.get(b.pkcolumn_name);
      if(alternateKeys == null)
      {
        alternateKeys = getAlternateKeys(fieldPrimary, primaryKeys,
           b.pkcolumn_name, b.pktable_schem + "." + b.pktable_name);
        mapKeys.put(b.pkcolumn_name, alternateKeys);
      }

      if(alternateKeys.length != 0)
      {
        TableHelperDelete thd = new TableHelperDelete(con, dryrun);
        thd.loadData(b.fktable_schem, b.fktable_name);
        thd.deleteCascade(sttable, b.fkcolumn_name, alternateKeys);
        comandi.addAll(thd.comandi);
        recurse.addAll(thd.recurse);
      }
    }

    sttable.pop();
  }

  private void deleteTable(String fieldPrimary, int[] primaryKeys)
     throws Exception
  {
    String sDEL
       = "DELETE FROM " + schemaName + "." + tableName
       + " WHERE " + fieldPrimary + " IN(" + StringOper.join(primaryKeys, ',') + ")";

    comandi.add(sDEL);
  }

  private int[] getAlternateKeys(String fieldPrimary, int[] primaryKeys, String targetField, String tableName)
     throws Exception
  {
    String sSQL
       = "SELECT DISTINCT " + targetField
       + "  FROM " + tableName
       + " WHERE " + fieldPrimary + " IN(" + StringOper.join(primaryKeys, ',') + ")";

    return DbUtils.queryForID(con, sSQL);
  }

  public List<String> getComandi()
  {
    return comandi;
  }

  public List<String> getRecurse()
  {
    return recurse;
  }

  public long executeDelete()
     throws Exception
  {
    long numDeleted = 0;

    for(int i = comandi.size() - 1; i >= 0; i--)
    {
      final String sDEL = (String) comandi.get(i);

      if(dryrun)
        log.info("sDEL=" + sDEL);
      else
        numDeleted += DbUtils.executeStatement(sDEL, con);
    }

    return numDeleted;
  }
}
