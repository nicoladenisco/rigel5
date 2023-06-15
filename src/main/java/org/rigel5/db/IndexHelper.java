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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.commonlib5.utils.Classificatore;

/**
 * Recupera informazioni sugli indici di una tabella.
 *
 * @author Nicola De Nisco
 */
public class IndexHelper
{
  protected final Connection con;
  protected final DatabaseMetaData meta;
  protected final Classificatore<String, IndiciBean> unique = new Classificatore<>();
  protected final Classificatore<String, IndiciBean> indici = new Classificatore<>();
  protected String schemaName, tableName;
  protected boolean dryrun;

  public IndexHelper(Connection con, boolean dryrun)
     throws SQLException
  {
    this.con = con;
    this.meta = con.getMetaData();
    this.dryrun = dryrun;
  }

  public void loadDataEasy(String fullTableName)
     throws Exception
  {
    DbUtils.scanTabelleColonne(con, fullTableName, null,
       (Connection con2, String nomeSchema, String nomeTabella, String nomeColonna) ->
    {
      loadData(nomeSchema, nomeTabella);
      return 0;
    });
  }

  public void loadData(String schemaName, String tableName)
     throws Exception
  {
    this.schemaName = schemaName;
    this.tableName = tableName;

    unique.clear();
    indici.clear();

    try(ResultSet rs = meta.getIndexInfo(null, schemaName, tableName, false, false))
    {
      while(rs.next())
      {
        IndiciBean b = new IndiciBean();
        b.read(rs);
        indici.aggiungi(b.INDEX_NAME, b);

        if(b.NON_UNIQUE == false)
          unique.aggiungi(b.INDEX_NAME, b);
      }
    }
  }

  public Set<String> getIndexNames()
  {
    return indici.keySet();
  }

  public Set<String> getUniqueIndexNames()
  {
    return unique.keySet();
  }

  public List<IndiciBean> getIndex(String indexName)
  {
    return indici.get(indexName);
  }

  public List<IndiciBean> getUniqueIndex(String indexName)
  {
    return unique.get(indexName);
  }

  public static class IndiciBean
  {
    public String TABLE_CAT,
       TABLE_SCHEM,
       TABLE_NAME,
       INDEX_QUALIFIER,
       INDEX_NAME,
       COLUMN_NAME,
       ASC_OR_DESC;

    public boolean NON_UNIQUE;

    public int TYPE, ORDINAL_POSITION;

    public void read(ResultSet rs)
       throws SQLException
    {
      TABLE_CAT = rs.getString("TABLE_CAT");
      TABLE_SCHEM = rs.getString("TABLE_SCHEM");
      TABLE_NAME = rs.getString("TABLE_NAME");
      NON_UNIQUE = rs.getBoolean("NON_UNIQUE");
      INDEX_QUALIFIER = rs.getString("INDEX_QUALIFIER");
      INDEX_NAME = rs.getString("INDEX_NAME");
      TYPE = rs.getInt("TYPE");
      ORDINAL_POSITION = rs.getInt("ORDINAL_POSITION");
      COLUMN_NAME = rs.getString("COLUMN_NAME");
      ASC_OR_DESC = rs.getString("ASC_OR_DESC");
    }

    @Override
    public String toString()
    {
      return "IndiciBean{" + "IN=" + INDEX_NAME + ", CN=" + COLUMN_NAME + ", P=" + ORDINAL_POSITION + '}';
    }
  }
}
