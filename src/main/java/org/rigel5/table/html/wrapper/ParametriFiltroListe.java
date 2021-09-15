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
import java.util.List;
import java.util.Map;
import org.commonlib5.utils.StringOper;

/**
 * Insieme di parametri per il filtraggio delle liste.
 *
 * @author Nicola De Nisco
 */
public class ParametriFiltroListe
{
  protected ArrayList<ParametroListe> parametri = new ArrayList<ParametroListe>();

  public void addParametri(ParametroListe par)
  {
    parametri.add(par);
  }

  public List<ParametroListe> getParametri()
  {
    return parametri;
  }

  public boolean haveParametri()
  {
    return !parametri.isEmpty();
  }

  public boolean setParametro(String nomeParam, String valParam)
  {
    for(ParametroListe pl : parametri)
    {
      if(pl.getNome().equals(nomeParam))
      {
        pl.setValore(valParam);
        return true;
      }
    }
    return false;
  }

  public boolean populateParametri(Map params)
  {
    boolean changed = false;
    for(ParametroListe pl : parametri)
    {
      // i parametri sono utilizzati anche come memoria
      // quindi il defval va usato solo se il valore è null
      String val;
      if(pl.getValore() == null)
        val = StringOper.okStr(params.get(pl.getNome()), pl.getDefval());
      else
        val = StringOper.okStr(params.get(pl.getNome()), pl.getValore());

      if(val != null && !StringOper.isEqu(val, pl.getValore()))
      {
        pl.setValore(val);
        changed = true;
      }
    }
    return changed;
  }

  public void clearParametri()
  {
    for(ParametroListe pl : parametri)
      pl.setValore(null);
  }
}
