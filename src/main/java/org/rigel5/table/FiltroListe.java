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
package org.rigel5.table;

import java.util.Vector;

/**
 * Un oggetto per mantenere il filtro applicato a una lista.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class FiltroListe
{
  private Object oggFiltro;
  private int limit;
  private int offset;
  private boolean ignoreCase;

  public Object getOggFiltro()
  {
    return oggFiltro;
  }

  public void setOggFiltro(Object oggFiltro)
  {
    this.oggFiltro = oggFiltro;
  }

  public void setLimit(int limit)
  {
    this.limit = limit;
  }

  public int getLimit()
  {
    return limit;
  }

  public void setOffset(int offset)
  {
    this.offset = offset;
  }

  public int getOffset()
  {
    return offset;
  }

  public void setIgnoreCase(boolean ignoreCase)
  {
    this.ignoreCase = ignoreCase;
  }

  public boolean isIgnoreCase()
  {
    return ignoreCase;
  }

  public void setOrdinamento(boolean ordinamento)
  {
    this.ordinamento = ordinamento;
  }

  public boolean isOrdinamento()
  {
    return ordinamento;
  }

  public boolean isEmpty()
  {
    return oggFiltro == null;
  }

  @Override
  public String toString()
  {
    return "FiltroListe{" + "limit=" + limit + ", offset=" + offset + ", ignoreCase=" + ignoreCase + ", ordinamento=" + ordinamento + '}';
  }

  //////////////////////////////////////////////////////////
  private class columnInfoFiltro
  {
    int indice, tipo, ordine;
    String valore;

    columnInfoFiltro(int indice, int tipo, int ordine, String valore)
    {
      this.indice = indice;
      this.tipo = tipo;
      this.ordine = ordine;
      this.valore = valore;
    }

    @Override
    public String toString()
    {
      return "columnInfoFiltro{" + "tipo=" + tipo + ", ordine=" + ordine + ", valore=" + valore + '}';
    }
  }
  private Vector<columnInfoFiltro> colInfo = new Vector<columnInfoFiltro>();
  private boolean ordinamento;

  /**
   * Recupera i dati di sorting dalle colonne e li salva
   * all'interno del vettore parametri di sorting.
   * @param rtm
   */
  public void salvaInfoColonne(RigelTableModel rtm)
  {
    colInfo.clear();
    for(int i = 0; i < rtm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = rtm.getColumn(i);
      if(cd.getFiltroTipo() != 0 || cd.getFiltroSort() != 0)
        colInfo.add(new columnInfoFiltro(i,
           cd.getFiltroTipo(), cd.getFiltroSort(), cd.getFiltroValore()));
    }
  }

  /**
   * Ripristina i dati di sorting delle colonne recuperandoli
   * dal vettore parametri salvati.
   * @param rtm
   */
  public void recuperaInfoColonne(RigelTableModel rtm)
  {
    ordinamento = false;

    // pulizia preventiva di valori precedenti
    for(int i = 0; i < rtm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = rtm.getColumn(i);
      cd.setFiltroTipo(0);
      cd.setFiltroSort(0);
      cd.setFiltroValore(null);
    }

    // imposta i valori
    for(columnInfoFiltro ci : colInfo)
    {
      if(ci.indice < rtm.getColumnCount())
      {
        RigelColumnDescriptor cd = rtm.getColumn(ci.indice);
        cd.setFiltroTipo(ci.tipo);
        cd.setFiltroSort(ci.ordine);
        cd.setFiltroValore(ci.valore);

        if(ci.ordine != 0)
          ordinamento = true;
      }
    }
  }
}
