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

import org.rigel5.RigelI18nInterface;
import org.rigel5.SetupHolder;

/**
 * <p>Title: Paginatore per tabelle.</p>
 * <p>Description:
 * Implementazione standard di un paginatore per tabelle html.
 * Puo' essere utilizzato insieme a hTable, hEditTable, FormTable.
 * Consente di scorrere le informazioni per pagine con un controllo
 * avanti-indietro.
 * </p>
 * <p>
 * Attenzione: il paginatore mantiene dei dati al suo interno,
 * quindi non puo' essere utilizzato fra utenti diversi (sessioni diverse).
 * Occorre crearne uno per ogni nuova sessione.
 * </p>
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class AbstractTablePager
{
  public int start = 0;
  public int limit = 10;
  protected RigelI18nInterface i18n = SetupHolder.getRi18n();
  protected int numPagine = -1, pagCurr = -1;
  protected String formName = "fo";

  /**
   * Ritorna il nome del form da usare
   * per il codice javascript per la navigazione pagine.
   * @return
   */
  public String getFormName()
  {
    return formName;
  }

  /**
   * Imposta il nome del form da usare
   * per il codice javascript per la navigazione pagine.
   * @param formName
   */
  public void setFormName(String formName)
  {
    this.formName = formName;
  }

  /**
   * Ritorna numero pagine complessive.
   * @return
   */
  public int getNumPagine()
  {
    return numPagine;
  }

  /**
   * Ritorna la pagina corrente.
   * @return
   */
  public int getPagCurr()
  {
    return pagCurr;
  }

  /**
   * Imposta la pagina corrente da visualizzare.
   * @param cpag
   */
  public void setPagCurr(int cpag)
  {
    if(numPagine != -1 && cpag >= 0 && cpag < numPagine)
    {
      start = limit * cpag;
      pagCurr = cpag;
    }
  }

  /**
   * Va all'ultima pagina. Utilizzata dalle hEditTable per
   * aggiungere nuovi elmenti in coda.
   */
  public void gotoEnd()
     throws Exception
  {
    calcNumPagine();
    if(numPagine > 1)
      setPagCurr(numPagine - 1);
  }

  abstract public RigelTableModel getRigelTableModel();

  /**
   * Ritorna il numero complessivo dei records
   * per eseguire il calcolo del numero delle pagina.
   * Per default e' il conteggio delle righe del RigelTableModel.
   * @return
   * @throws Exception
   */
  protected long getTotalRecords()
     throws Exception
  {
    return getRigelTableModel().getRowCount();
  }

  /**
   * Calcola il numero di pagine totali in base al numero di record.
   */
  protected void calcNumPagine() throws Exception
  {
    calcNumPagine(getTotalRecords());
  }

  /**
   * Calcola il numero di pagine totali in base al numero di record.
   */
  protected void calcNumPagine(long totalRecords) throws Exception
  {
    numPagine = (int) (totalRecords / limit) + ((totalRecords % limit) == 0 ? 0 : 1);
  }

  public RigelI18nInterface getI18n()
  {
    return i18n;
  }

  public void setI18n(RigelI18nInterface i18n)
  {
    this.i18n = i18n;
  }
}
