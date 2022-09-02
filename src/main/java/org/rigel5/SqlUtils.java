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
package org.rigel5;

import java.sql.Statement;
import java.util.regex.Pattern;
import org.apache.torque.criteria.SqlEnum;
import org.commonlib5.utils.StringOper;

/**
 * Funzioni di utilità per SQL.
 *
 * @author Nicola De Nisco
 */
public class SqlUtils
{
  /**
   * Esegue un comando SQL utilizzando il connection producer a setup.
   *
   * @param sSQL comando da eseguire
   * @return numero di record interessati
   * @throws Exception
   */
  public static int executeSQL(String sSQL)
     throws Exception
  {
    return SetupHolder.getConProd().functionConnection((con) ->
    {
      try ( Statement stm = con.createStatement())
      {
        return stm.executeUpdate(sSQL);
      }
    });
  }

  /**
   * Esegue un comando SQL utilizzando il connection producer a setup. Come la executeSQL() ma non solleva eccezione. In
   * caso di errore ritorna -1.
   *
   * @param sSQL comando da eseguire
   * @return numero di record interessati (-1 in caso di errore)
   */
  public static int executeSQLQuiet(String sSQL)
  {
    try
    {
      return executeSQL(sSQL);
    }
    catch(Throwable e)
    {
      return -1;
    }
  }

  /**
   * Aggiunge una clausola where ad una query in costruzione. Aggiunge WHERE o AND a seconda se esiste o meno una where
   * preesitente.
   *
   * @param sSQL query da copletare
   * @param where clausola where da aggiungere
   * @return query completa
   */
  public static String addWhere(String sSQL, String where)
  {
    if(sSQL.toUpperCase().contains("WHERE"))
    {
      return sSQL + " AND " + where;
    }
    else
    {
      return sSQL + " WHERE " + where;
    }
  }

  /**
   * Comverte la stringa nella equivalente costante SqlEnum.
   *
   * @param s
   * @return
   */
  public static SqlEnum str2Sql(String s)
  {
    if(SqlEnum.EQUAL.toString().equals(s))
    {
      return SqlEnum.EQUAL;
    }
    if(SqlEnum.NOT_EQUAL.toString().equals(s))
    {
      return SqlEnum.NOT_EQUAL;
    }
    if(SqlEnum.ALT_NOT_EQUAL.toString().equals(s))
    {
      return SqlEnum.ALT_NOT_EQUAL;
    }
    if(SqlEnum.GREATER_THAN.toString().equals(s))
    {
      return SqlEnum.GREATER_THAN;
    }
    if(SqlEnum.LESS_THAN.toString().equals(s))
    {
      return SqlEnum.LESS_THAN;
    }
    if(SqlEnum.GREATER_EQUAL.toString().equals(s))
    {
      return SqlEnum.GREATER_EQUAL;
    }
    if(SqlEnum.LESS_EQUAL.toString().equals(s))
    {
      return SqlEnum.LESS_EQUAL;
    }
    if(SqlEnum.LIKE.toString().equals(s))
    {
      return SqlEnum.LIKE;
    }
    if(SqlEnum.NOT_LIKE.toString().equals(s))
    {
      return SqlEnum.NOT_LIKE;
    }
    if(SqlEnum.ILIKE.toString().equals(s))
    {
      return SqlEnum.ILIKE;
    }
    if(SqlEnum.NOT_ILIKE.toString().equals(s))
    {
      return SqlEnum.NOT_ILIKE;
    }
    if(SqlEnum.IN.toString().equals(s))
    {
      return SqlEnum.IN;
    }
    if(SqlEnum.NOT_IN.toString().equals(s))
    {
      return SqlEnum.NOT_IN;
    }
    if(SqlEnum.CUSTOM.toString().equals(s))
    {
      return SqlEnum.CUSTOM;
    }
    if(SqlEnum.JOIN.toString().equals(s))
    {
      return SqlEnum.JOIN;
    }
    if(SqlEnum.DISTINCT.toString().equals(s))
    {
      return SqlEnum.DISTINCT;
    }
    if(SqlEnum.ALL.toString().equals(s))
    {
      return SqlEnum.ALL;
    }
    if(SqlEnum.ASC.toString().equals(s))
    {
      return SqlEnum.ASC;
    }
    if(SqlEnum.DESC.toString().equals(s))
    {
      return SqlEnum.DESC;
    }
    if(SqlEnum.ISNULL.toString().equals(s))
    {
      return SqlEnum.ISNULL;
    }
    if(SqlEnum.ISNOTNULL.toString().equals(s))
    {
      return SqlEnum.ISNOTNULL;
    }
    if(SqlEnum.CURRENT_DATE.toString().equals(s))
    {
      return SqlEnum.CURRENT_DATE;
    }
    if(SqlEnum.CURRENT_TIME.toString().equals(s))
    {
      return SqlEnum.CURRENT_TIME;
    }
//    if(SqlEnum.LEFT_JOIN.toString().equals(s))
//    {
//      return SqlEnum.LEFT_JOIN;
//    }
//    if(SqlEnum.RIGHT_JOIN.toString().equals(s))
//    {
//      return SqlEnum.RIGHT_JOIN;
//    }
//    if(SqlEnum.INNER_JOIN.toString().equals(s))
//    {
//      return SqlEnum.INNER_JOIN;
//    }
    if(SqlEnum.ON.toString().equals(s))
    {
      return SqlEnum.ON;
    }
    if(SqlEnum.AS.toString().equals(s))
    {
      return SqlEnum.AS;
    }
    if(SqlEnum.ESCAPE.toString().equals(s))
    {
      return SqlEnum.ESCAPE;
    }

    return null;
  }

  public static final Pattern p1 = Pattern.compile("SELECT.+FROM.+");
  public static final Pattern p2 = Pattern.compile("UPDATE.+SET.+");
  public static final Pattern p3 = Pattern.compile("DELETE.+FROM.+");
  public static final Pattern p4 = Pattern.compile("UNION.+\\(");

  public static boolean checkForSqlInjection(String val)
  {
    String test = StringOper.okStr(val).toUpperCase();

    if(p1.matcher(test).find())
      return true;
    if(p2.matcher(test).find())
      return true;
    if(p3.matcher(test).find())
      return true;
    if(p4.matcher(test).find())
      return true;

    return false;
  }
}
