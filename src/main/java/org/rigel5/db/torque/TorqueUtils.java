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
package org.rigel5.db.torque;

import java.sql.Connection;
import java.util.List;
import org.apache.torque.Column;
import org.apache.torque.TorqueException;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.map.TableMap;
import org.apache.torque.om.ColumnAccessByName;
import org.apache.torque.util.BasePeerImpl;
import org.commonlib5.utils.DateTime;
import org.json.JSONObject;
import org.rigel5.db.IndexHelper;

/**
 * Utilità per Torque om.
 *
 * @author Nicola De Nisco
 */
public class TorqueUtils
{
  public static <T> T getFirst(BasePeerImpl<T> peerImpl, Column filtCol, Object filtValue, Connection con)
     throws TorqueException
  {
    CriteriaRigel cr = new CriteriaRigel();
    cr.and(filtCol, filtValue);
    return getFirst(peerImpl, cr, con);
  }

  public static <T> T getFirst(BasePeerImpl<T> peerImpl, String tableName, Column filtCol, Object filtValue, Connection con)
     throws TorqueException
  {
    CriteriaRigel cr = new CriteriaRigel(tableName);
    cr.and(filtCol, filtValue);
    return getFirst(peerImpl, cr, con);
  }

  public static <T> T getFirst(BasePeerImpl<T> peerImpl, Criteria cr, Connection con)
     throws TorqueException
  {
    List<T> lsApps = con == null ? peerImpl.doSelect(cr) : peerImpl.doSelect(cr, con);
    return lsApps.isEmpty() ? null : lsApps.get(0);
  }

  public static JSONObject toJson(ColumnAccessByName toExport, List<String> fields, JSONObject json)
     throws Exception
  {
    for(String f : fields)
    {
      Object val = toExport.getByName(f);

      if(val instanceof java.util.Date)
      {
        // caso speciale per la data: oltre alla formattazione FULL
        // aggiunge formattazione solo DATA o solo ORA
        json.put(f + "_DATA_ISO", DateTime.formatIso((java.util.Date) val));
        json.put(f + "_ORA2_ISO", DateTime.formatIsoFull((java.util.Date) val));
      }

      json.put(f, val);
    }

    return json;
  }

  public static <T> T retrieveByAlternateKeyQuiet(int numIdx, BasePeerImpl<T> peerImpl, Connection con, Object... args)
     throws Exception
  {
    TableMap tm = peerImpl.getTableMap();
    IndexHelper ih = new IndexHelper(con, false);
    ih.loadDataEasy(tm.getFullyQualifiedTableName());

    CriteriaRigel cr = new CriteriaRigel();
    String indexName = ih.getUniqueIndexNames().stream()
       .filter((s) -> s.matches(".+_" + numIdx))
       .findFirst().orElse(null);

    if(indexName == null)
      throw new Exception("Chiave alternata " + numIdx + " non trovata.");

    List<IndexHelper.IndiciBean> lsib = ih.getUniqueIndex(indexName);

    if(lsib.size() != args.length)
      throw new Exception("Chiave alternata " + numIdx + " disallineata: erano attesi "
         + lsib.size() + " parametri invece di " + args.length);

    for(IndexHelper.IndiciBean ib : lsib)
    {
      cr.and(ib.COLUMN_NAME, args[ib.ORDINAL_POSITION - 1]);
    }

    return getFirst(peerImpl, cr, con);
  }
}
