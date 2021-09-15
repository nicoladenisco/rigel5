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
package org.rigel5.table.html.wrapper;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import org.commonlib5.utils.Pair;
import org.commonlib5.utils.StringOper;

/**
 * Informazioni usate dalle liste quando vengono utilizzate come
 * selezione di campi foreign nei forms.
 *
 * @author Nicola De Nisco
 */
public class ForeignInfo
{
  protected StringBuilder names = null;
  protected ArrayList<Pair<String, String>> foreignColumns = new ArrayList<>();

  public Iterator<Pair<String, String>> iterator()
  {
    return foreignColumns.iterator();
  }

  public synchronized String joinNames()
  {
    if(names == null)
    {
      names = new StringBuilder();
      Iterator<Pair<String, String>> itr = foreignColumns.iterator();

      for(int i = 0; itr.hasNext(); i++)
      {
        Pair<String, String> p = itr.next();

        if(i > 0)
          names.append(',');

        names.append(p.first);
      }
    }

    return names.toString();
  }

  public Enumeration<String> getForeignColumnsKeys()
  {
    return new Enumeration<String>()
    {
      private Iterator<Pair<String, String>> itr = foreignColumns.iterator();

      @Override
      public boolean hasMoreElements()
      {
        return itr.hasNext();
      }

      @Override
      public String nextElement()
      {
        return itr.next().first;
      }
    };
  }

  public String getParam(String key)
  {
    for(Pair<String, String> p : foreignColumns)
    {
      if(StringOper.isEqu(key, p.first))
        return p.second;
    }

    return null;
  }

  public synchronized void addForeignInfo(String nome, String colonna)
  {
    names = null;
    foreignColumns.add(new Pair<String, String>(nome, colonna));
  }
}
