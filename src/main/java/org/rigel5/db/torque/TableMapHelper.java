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
package org.rigel5.db.torque;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.IDMethod;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.ForeignKeyMap;
import org.apache.torque.map.TableMap;
import org.commonlib5.utils.DateTime;
import org.commonlib5.utils.Pair;
import org.commonlib5.utils.StringOper;

/**
 * Supporto all'introspezione dei campi tabella.
 * Semplifica l'uso dei TableMap.
 * <pre>
 *  // ricerca case insensitive del nome tabella
 *  TableMapHelper tm = TableMapHelper.getByTableName(tableName);
 *
 *  // ricerca case insensitive del nome colonna
 *  ColumnMap cm = tm.getCampo("codice");
 * </pre>
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class TableMapHelper
{
  private TableMap tmap;
  private ColumnMap cmaps[] = null;
  private String nomeTabella;
  private String objectName = null;

  public TableMapHelper()
  {
  }

  public TableMapHelper(String tableName)
     throws TorqueException
  {
    DatabaseMap databaseMap = Torque.getDatabaseMap();

    for(TableMap table : databaseMap.getTables())
    {
      if(table == null)
        continue;

      if(tableName.equalsIgnoreCase(table.getName()) || tableName.equalsIgnoreCase(table.getJavaName()))
      {
        setTmap(table);
        return;
      }
    }

    throw new TorqueException("Table " + tableName + " not found in database map.");
  }

  public TableMapHelper(TableMap tmap)
  {
    setTmap(tmap);
  }

  public String getObjectName()
  {
    return objectName;
  }

  public ColumnMap getCampo(String nomeColonna)
  {
    // ricerca diretta del nome sql
    ColumnMap cmap;
    if((cmap = tmap.getColumn(nomeColonna)) != null)
      return cmap;

    // ricarca del nome con la convenzione turbine
    for(int i = 0; i < cmaps.length; i++)
    {
      String s;
      if((s = cmaps[i].getColumnName()) != null)
      {
        if(testNomeColonna(nomeColonna, s))
          return cmaps[i];
      }
    }

    return null;
  }

  public Pair<ColumnMap, Object> getCampoAndParseValue(String nomeColonna, String value)
  {
    ColumnMap campo = getCampo(nomeColonna);
    if(campo == null)
      return null;

    return new Pair<>(campo, parseValue(campo, value));
  }

  public Object parseValue(ColumnMap campo, String value)
  {
    switch(campo.getJavaType())
    {
      case "int":
      case "short":
      case "Integer":
        return StringOper.parse(value, 0);

      case "long":
      case "Long":
        return (long) StringOper.parse(value, 0.0);

      case "float":
      case "Float":
        return (float) StringOper.parse(value, 0.0);

      case "double":
      case "Double":
      case "Number":
      case "BigDecimal":
        return StringOper.parse(value, 0.0);

      case "Date":
      case "Time":
      case "Timestamp":
        return DateTime.parseIsoFull(value, null);

      case "String":
        return value;

      default:
        return value;
    }
  }

  public boolean isNumeric(ColumnMap campo)
  {
    switch(campo.getJavaType())
    {
      case "int":
      case "short":
      case "Integer":

      case "long":
      case "Long":

      case "float":
      case "Float":

      case "double":
      case "Double":
      case "Number":
      case "BigDecimal":
        return true;

      case "Date":
      case "Time":
      case "Timestamp":

      case "String":

      default:
        return false;
    }
  }

  public boolean isDate(ColumnMap campo)
  {
    switch(campo.getJavaType())
    {
      case "int":
      case "short":
      case "Integer":

      case "long":
      case "Long":

      case "float":
      case "Float":

      case "double":
      case "Double":
      case "Number":
      case "BigDecimal":
        return false;

      case "Date":
      case "Time":
      case "Timestamp":
        return true;

      case "String":

      default:
        return false;
    }
  }

  public boolean isString(ColumnMap campo)
  {
    switch(campo.getJavaType())
    {
      case "int":
      case "short":
      case "Integer":

      case "long":
      case "Long":

      case "float":
      case "Float":

      case "double":
      case "Double":
      case "Number":
      case "BigDecimal":

      case "Date":
      case "Time":
      case "Timestamp":
        return false;

      case "String":
        return true;

      default:
        return false;
    }
  }

  public boolean testNomeColonna(String nomeColonna, String nomeTorque)
  {
    if(nomeTorque.equalsIgnoreCase(nomeColonna))
      return true;

    String som = tmap.removeUnderScores(nomeTorque);
    return som.equalsIgnoreCase(nomeColonna);
  }

  public String getNomeCampo(String nomeColonna)
  {
    ColumnMap cmap = getCampo(nomeColonna);
    return cmap == null ? nomeTabella + "." + nomeColonna : cmap.getFullyQualifiedName();
  }

  public void setNomeTabella(String nomeTabella)
  {
    this.nomeTabella = nomeTabella;
  }

  public String getNomeTabella()
  {
    return nomeTabella;
  }

  public void setTmap(TableMap tmap)
  {
    this.tmap = tmap;
    nomeTabella = tmap.getName();
    cmaps = tmap.getColumns();
    if(tmap.getPrefix() == null)
      tmap.setPrefix(nomeTabella + ".");
    objectName = tmap.getJavaName();
  }

  public TableMap getTmap()
  {
    return tmap;
  }

  public ColumnMap[] getColumns()
  {
    return cmaps;
  }
  private static final char[] sepCap = new char[]
  {
    '.', '_', ' '
  };

  public static String getJavaName(String s)
  {
    String rv = WordUtils.capitalizeFully(s, sepCap);
    rv = StringUtils.remove(rv, '.');
    rv = StringUtils.remove(rv, '_');
    rv = StringUtils.remove(rv, ' ');
    return rv;
  }

  public boolean isAutoIncrement()
  {
    return !tmap.getPrimaryKeyMethod().equals(IDMethod.NO_ID_METHOD);
  }

  public int getNumColumnsPrimaryKeys()
  {
    int count = 0;
    for(Iterator<ColumnMap> iterator = getPrimaryKeys(); iterator.hasNext();)
      count++;
    return count;
  }

  public ColumnMap getFirstPrimaryKey()
  {
    for(Iterator<ColumnMap> iterator = getPrimaryKeys(); iterator.hasNext();)
      return iterator.next();

    return null;
  }

  public Iterator<ColumnMap> getPrimaryKeys()
  {
    return new Iterator<ColumnMap>()
    {
      private int count = 0;
      private ColumnMap cm = null;

      @Override
      public boolean hasNext()
      {
        while(count < cmaps.length)
        {
          cm = cmaps[count++];
          if(cm.isPrimaryKey())
            return true;
        }
        return false;
      }

      @Override
      public ColumnMap next()
      {
        return cm;
      }

      @Override
      public void remove()
      {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    };
  }

  public Iterator<ColumnMap> getForeignKeys()
  {
    final ArrayList<ColumnMap> tmp = new ArrayList<>();
    tmap.getForeignKeys().forEach((fk) -> fk.getColumns().forEach((cp) -> tmp.add(cp.getLocal())));
    return tmp.iterator();
  }

  public ForeignKeyMap findForeignKeyByColumnName(String colName)
  {
    List<ForeignKeyMap> lsfk = tmap.getForeignKeys();
    for(ForeignKeyMap fk : lsfk)
    {
      List<ForeignKeyMap.ColumnPair> lscp = fk.getColumns();
      for(ForeignKeyMap.ColumnPair cp : lscp)
      {
        if(testNomeColonna(colName, cp.getLocal().getColumnName()))
          return cp.getForeignKeyMap();
      }
    }
    return null;
  }

  /**
   * Restituisce una foreigntable/foreigncolumn se la colonna locale
   * è implicata in una foreign key semplice (no chiavi composte).
   * @param colName nome colonna locale
   * @return coppia tabella/colonna esterna oppure null
   */
  public Pair<String, String> findForeignKeyByColumnNameSimple(String colName)
  {
    ForeignKeyMap fk = findForeignKeyByColumnName(colName);
    return fk == null || fk.getColumns().size() != 1 ? null
              : new Pair<>(fk.getForeignTableName(), fk.getColumns().get(0).getForeign().getColumnName());
  }

  public static TableMapHelper getByTableName(String tableName)
     throws Exception
  {
    DatabaseMap dbMap = Torque.getDatabaseMap();
    TableMap tMap = dbMap.getTable(tableName);
    if(tMap != null)
      return new TableMapHelper(tMap);

    TableMap[] tmaps = dbMap.getTables();
    for(int i = 0; i < tmaps.length; i++)
    {
      tMap = tmaps[i];
      if(StringOper.isEquNocaseAny(tableName, tMap.getName(), tMap.getJavaName()))
        return new TableMapHelper(tMap);

      String som = tMap.removeUnderScores(tMap.getName());
      if(som.equalsIgnoreCase(tableName))
        return new TableMapHelper(tMap);
    }

    return null;
  }
}
