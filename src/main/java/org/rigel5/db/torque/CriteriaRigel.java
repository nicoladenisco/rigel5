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

import java.util.*;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.criteria.SqlEnum;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.TableMap;
import org.commonlib5.utils.ArrayOper;
import org.commonlib5.utils.DateTime;

/**
 * Estensione dell'oggetto Criteria di Torque.
 * Rigel supporta un concetto di cancellazione logica:
 * ogni tabella che lo richiede può avere un campo STATO_REC INTEGER
 * che viene interpretato con i seguenti valori:
 * <ul>
 * <li>0 : record attivo lettura/scrittura</li>
 * <li>1-9 : record in sola lettura con un grado di permission da 1 a 9</li>
 * <li>10 (o superiore) : record cancellato logicamente</li>
 * <li>NULL : equivalente a 0</li>
 * </ul>
 * questa classe consente di filtrare automaticamente i record
 * sottoposti a cancellazione logica.
 * Una serie di altri metodi semplifica operazioni comuni sui Criteria.
 *
 * @author Nicola De Nisco
 */
public class CriteriaRigel extends Criteria
{
  public static final String STATO_REC = "STATO_REC";

  public CriteriaRigel()
  {
  }

  /**
   * Costruttore con inizializzazione per esclusione record
   * cancellati logicamente.
   * @param tableName nome della tabella
   */
  public CriteriaRigel(String tableName)
  {
    // ATTENZIONE: non modificare il case di tableName altrimenti
    // quando si aggiungono altri campi al criteria il nome tabella
    // non corrisponde.
    removeDeleted(this, tableName);
  }

  public CriteriaRigel(Criteria c)
  {
    super(c);
  }

  /**
   * Costruisce il filtro SQL per escludere i record cancellati logicamente.
   * @param tableName nome della tabella
   * @return SQL
   */
  static public String filtro(String tableName)
  {
    return "((" + tableName + "." + STATO_REC + " IS NULL) OR (" + tableName + "." + STATO_REC + "<10))";
  }

  /**
   * Aggiunge al criteria indicato la regola per escludere
   * i record sottoposti a cancellazione logica.
   * @param c criteria Torque
   * @param tableName nome della tabella
   */
  static public void removeDeleted(Criteria c, String tableName)
  {
    c.andVerbatimSql(filtro(tableName), null);
  }

  /**
   * Aggiunge ad una query SQL già composta la regola per escludere
   * i record sottoposti a cancellazione logica.
   * @param sSQL una query SQL completa
   * @param tableName nome della tabella
   * @return SQL
   */
  static public String removeDeletedSQL(String sSQL, String tableName)
  {
    if(!sSQL.toUpperCase().contains("WHERE"))
    {
      // sSQL non contiene una clausola WHERE
      return sSQL + " WHERE " + filtro(tableName);
    }
    else
    {
      // sSQL gia' contiene una clausola WHERE
      return sSQL + " AND " + filtro(tableName);
    }
  }

  static public String removeDeletedSQLTestTable(String sSQL, String tableName)
     throws TorqueException
  {
    // aggiunge rimozione per eventuale cancellazione logica
    TableMap tm = Torque.getDatabaseMap().getTable(tableName.toUpperCase());
    if(tm != null && tm.getColumn("STATO_REC") != null)
      return CriteriaRigel.removeDeletedSQL(sSQL, tableName);

    return sSQL;
  }

  /**
   * Aggiunge al criteria indicato la regola confronto like senza case.
   * @param columnName
   * @param value
   * @param tableName nome della tabella
   */
  public void addILike(String tableName, String columnName, String value)
  {
    andVerbatimSql("(" + tableName + "." + columnName + " ILIKE '" + value + "')", null);
  }

  /**
   * Aggiunge al criteria indicato la regola confronto like senza case.
   * @param columnName
   * @param value
   * @param tableName nome della tabella
   */
  public void addRegExp(String tableName, String columnName, String value)
  {
    andVerbatimSql("(" + tableName + "." + columnName + " ~ '" + value + "')", null);
  }

  /**
   * Aggiunge al criteria indicato la regola confronto like senza case.
   * @param columnName
   * @param value
   * @param tableName nome della tabella
   */
  public void addIRegexp(String tableName, String columnName, String value)
  {
    andVerbatimSql("(" + tableName + "." + columnName + " ~* '" + value + "')", null);
  }

  /**
   * Aggiunge al criteria indicato la regola confronto like senza case.
   * @param column
   * @param value
   */
  public void addRegExp(ColumnMap column, String value)
  {
    andVerbatimSql("(" + column.getColumnName() + " ~ '" + value + "')", null);
  }

  /**
   * Aggiunge al criteria indicato la regola confronto like senza case.
   * @param column
   * @param value
   */
  public void addIRegexp(ColumnMap column, String value)
  {
    andVerbatimSql("(" + column.getColumnName() + " ~* '" + value + "')", null);
  }

  /**
   * Aggiunge ad una query SQL già composta la regola confronto like senza case.
   * @param sSQL una query SQL completa
   * @param tableName nome della tabella
   * @param columnName nome della colonna
   * @param value valore (deve contenere eventualmente %: 'PIPPO%' '%PLUTO' '%ALTRO%')
   * @return SQL
   */
  static public String addILikeSQL(String sSQL, String tableName, String columnName, String value)
  {
    if(!sSQL.toUpperCase().contains("WHERE"))
    {
      // sSQL non contiene una clausola WHERE
      return sSQL + " WHERE " + tableName + "." + columnName + " ILIKE " + value;
    }
    else
    {
      // sSQL gia' contiene una clausola WHERE
      return sSQL + " AND " + tableName + "." + columnName + " ILIKE " + value;
    }
  }

  /**
   * Comparison type.
   */
  public static final SqlEnum BETWEEN = SqlEnum.ESCAPE;

  /**
   * Represents the Is NULL in the WHERE
   * clause of an SQL Statement
   *
   * @param columnname the column name
   * @return this object
   */
  public CriteriaRigel isNull(String columnname)
  {
    super.andVerbatimSql("(" + columnname + " is NULL)", null);
    return this;
  }

  public CriteriaRigel isNull(ColumnMap cm)
  {
    return isNull(cm.getColumnName());
  }

  /**
   * Represents the Is NOT NULL in the WHERE
   * clause of an SQL Statement
   *
   * @param columnname the column name
   * @return this object
   */
  public CriteriaRigel isNotNull(String columnname)
  {
    super.andVerbatimSql("(" + columnname + " is NOT NULL)", null);
    return this;
  }

  public CriteriaRigel isNotNull(ColumnMap cm)
  {
    return isNotNull(cm.getColumnName());
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return this object
   */
  public CriteriaRigel isBetween(String columnname, int min, int max)
  {
    super.and(columnname, min, Criteria.GREATER_EQUAL);
    super.and(columnname, max, Criteria.LESS_EQUAL);
    return this;
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return this object
   */
  public CriteriaRigel isBetween(ColumnMap columnname, int min, int max)
  {
    super.and(columnname, min, Criteria.GREATER_EQUAL);
    super.and(columnname, max, Criteria.LESS_EQUAL);
    return this;
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return this object
   */
  public CriteriaRigel isBetween(String columnname, double min, double max)
  {
    super.and(columnname, min, Criteria.GREATER_EQUAL);
    super.and(columnname, max, Criteria.LESS_EQUAL);
    return this;
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return this object
   */
  public CriteriaRigel isBetween(ColumnMap columnname, double min, double max)
  {
    super.and(columnname, min, Criteria.GREATER_EQUAL);
    super.and(columnname, max, Criteria.LESS_EQUAL);
    return this;
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return this object
   */
  public CriteriaRigel isBetween(String columnname, Date min, Date max)
  {
    super.and(columnname, min, Criteria.GREATER_EQUAL);
    super.and(columnname, max, Criteria.LESS_EQUAL);
    return this;
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return this object
   */
  public CriteriaRigel isBetween(ColumnMap columnname, Date min, Date max)
  {
    super.and(columnname, min, Criteria.GREATER_EQUAL);
    super.and(columnname, max, Criteria.LESS_EQUAL);
    return this;
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return this object
   */
  public CriteriaRigel isBetweenTimestamp(String columnname, Date min, Date max)
  {
    super.and(columnname, min, Criteria.GREATER_EQUAL);
    super.and(columnname, max, Criteria.LESS_EQUAL);
    return this;
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return this object
   */
  public CriteriaRigel isBetweenTimestamp(ColumnMap columnname, Date min, Date max)
  {
    super.and(columnname, min, Criteria.GREATER_EQUAL);
    super.and(columnname, max, Criteria.LESS_EQUAL);
    return this;
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return this object
   */
  public CriteriaRigel isBetweenTimestampDateTrunc(String columnname, Date min, Date max)
  {
    min = DateTime.inizioGiorno(min);
    max = DateTime.fineGiorno(max);

    super.and(columnname, min, Criteria.GREATER_EQUAL);
    super.and(columnname, max, Criteria.LESS_EQUAL);
    return this;
  }

  public static String isBetweenTimestampDateTruncSQL(String columnname, Date min, Date max)
  {
    min = DateTime.inizioGiorno(min);
    max = DateTime.fineGiorno(max);

    throw new UnsupportedOperationException();

//    try
//    {
//      Adapter db = Torque.getDB(Torque.getDefaultDB());
//      return "((" + columnname + " >= '" + db.getDateString(min) + "') AND (" + columnname + " <= '" + db.getDateString(max) + "'))";
//    }
//    catch(TorqueException ee)
//    {
//      throw new RuntimeException(ee);
//    }
  }

  public CriteriaRigel isEqualDate(String columnname, Date data)
  {
    return isBetweenTimestampDateTrunc(columnname, data, data);
  }

  public CriteriaRigel isEqualDate(ColumnMap cm, Date data)
  {
    return isBetweenTimestampDateTrunc(cm, data, data);
  }

  public static String isEqualDateSQL(String columnname, Date data)
  {
    return isBetweenTimestampDateTruncSQL(columnname, data, data);
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return
   */
  public CriteriaRigel isBetween(ColumnMap columnname, String min, String max)
  {
    super.and(columnname, (Object) min, Criteria.GREATER_EQUAL);
    super.and(columnname, (Object) max, Criteria.LESS_EQUAL);
    return this;
  }

  /**
   * Represents the BETWEEN operator
   *
   * @param columnname the column name
   * @param min the min value (incluse in query)
   * @param max the max value (include in query)
   * @return
   */
  public CriteriaRigel isBetween(String columnname, String min, String max)
  {
    super.and(columnname, (Object) min, Criteria.GREATER_EQUAL);
    super.and(columnname, (Object) max, Criteria.LESS_EQUAL);
    return this;
  }

  public CriteriaRigel addILike(ColumnMap cm, String value)
  {
    return (CriteriaRigel) and(cm, value, ILIKE);
  }

  /**
   * Represents the ILIKE operator
   *
   * @param tableColumn
   * @param value
   * @return
   */
  public CriteriaRigel addILike(String tableColumn, String value)
  {
    return (CriteriaRigel) and(tableColumn, value, ILIKE);
  }

  public CriteriaRigel andIn(ColumnMap cm, int[] idRes)
  {
    andIn(cm, ArrayOper.asList(idRes));
    return this;
  }

  public CriteriaRigel andNotIn(ColumnMap cm, int[] idRes)
  {
    andNotIn(cm, ArrayOper.asList(idRes));
    return this;
  }

  public CriteriaRigel add(ColumnMap col, Object value)
  {
    and(col, value);
    return this;
  }

  public CriteriaRigel add(ColumnMap col, Object value, SqlEnum test)
  {
    and(col, value, test);
    return this;
  }

  public CriteriaRigel addIn(ColumnMap cm, int[] idRes)
  {
    andIn(cm, ArrayOper.asList(idRes));
    return this;
  }

  public CriteriaRigel addNotIn(ColumnMap cm, int[] idRes)
  {
    andNotIn(cm, ArrayOper.asList(idRes));
    return this;
  }

  public CriteriaRigel isBetweenTimestampDateTrunc(ColumnMap col, Date min, Date max)
  {
    min = DateTime.inizioGiorno(min);
    max = DateTime.fineGiorno(max);

    super.and(col, min, Criteria.GREATER_EQUAL);
    super.and(col, max, Criteria.LESS_EQUAL);
    return this;
  }

  public CriteriaRigel addIn(ColumnMap cm, Object[] ids)
  {
    super.andIn(cm, ids);
    return this;
  }

  public CriteriaRigel addIn(ColumnMap cm, Collection<?> ids)
  {
    super.andIn(cm, ids);
    return this;
  }

  public CriteriaRigel addNotIn(ColumnMap cm, Object[] ids)
  {
    super.andNotIn(cm, ids);
    return this;
  }

  public CriteriaRigel addNotIn(ColumnMap cm, Collection<?> ids)
  {
    super.andNotIn(cm, ids);
    return this;
  }

  public CriteriaRigel addVerbatimSql(final String sql)
  {
    return (CriteriaRigel) andVerbatimSql(sql, null);
  }

  public CriteriaRigel setIn(ColumnMap columnname, Collection<?> lFiltri)
  {
    if(lFiltri != null && !lFiltri.isEmpty())
      addIn(columnname, lFiltri);

    return this;
  }

  public CriteriaRigel isBetweenDate(ColumnMap columnname, Date min, Date max)
     throws Exception
  {
    if(min == null || max == null)
      return this;

    isBetweenTimestampDateTrunc(columnname, min, max);
    return this;
  }
}
