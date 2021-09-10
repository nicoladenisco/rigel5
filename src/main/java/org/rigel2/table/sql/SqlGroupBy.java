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
package org.rigel2.table.sql;

import java.util.*;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.html.wrapper.ParametriFiltroListe;
import org.rigel2.table.html.wrapper.ParametroListe;

/**
 * Holder per la clausola group by.
 *
 * @author Nicola De Nisco
 */
public class SqlGroupBy
{
  public ArrayList<RigelColumnDescriptor> colonne = new ArrayList<RigelColumnDescriptor>();
  public ParametriFiltroListe filtro = new ParametriFiltroListe();
  public String orderby = null;

  public SqlGroupBy()
  {
  }

  public void addParametri(ParametroListe par)
  {
    filtro.addParametri(par);
  }
}

