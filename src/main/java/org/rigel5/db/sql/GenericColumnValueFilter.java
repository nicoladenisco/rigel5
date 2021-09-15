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

import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.RowSet;
import javax.sql.rowset.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlib5.utils.StringOper;

/**
 * Implementazione generica di un filtro da utilizzare con FilteredRowSet.
 *
 * @author Nicola De Nisco
 */
public class GenericColumnValueFilter implements Predicate
{
  private static class Pair
  {
    int colIdx;
    Object value;
  }
  /** Logging */
  private static Log log = LogFactory.getLog(GenericColumnValueFilter.class);
  //
  private ArrayList<Pair> arFilter = new ArrayList<Pair>();
  private boolean caseSensitive = false;

  public GenericColumnValueFilter()
  {
  }

  public void addFilter(int colIdx, Object value)
  {
    Pair p = new Pair();
    p.colIdx = colIdx;
    p.value = value;
    arFilter.add(p);
  }

  public boolean isCaseSensitive()
  {
    return caseSensitive;
  }

  public void setCaseSensitive(boolean caseSensitive)
  {
    this.caseSensitive = caseSensitive;
  }

  @Override
  public boolean evaluate(RowSet rs)
  {
    try
    {
      for(Pair p : arFilter)
      {
        Object value = rs.getObject(p.colIdx);
        if(caseSensitive)
        {
          if(!StringOper.isEqu(p.value, value))
            return false;
        }
        else
        {
          if(!StringOper.isEquNocase(p.value, value))
            return false;
        }
      }
      return true;
    }
    catch(Exception ex)
    {
      log.error("Errore nel filtro:", ex);
      return false;
    }
  }

  @Override
  public boolean evaluate(Object value, int column)
     throws SQLException
  {
    return true;
  }

  @Override
  public boolean evaluate(Object value, String columnName)
     throws SQLException
  {
    return true;
  }
}
