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
package org.rigel5.table.sql.html;

import java.util.Map;
import javax.servlet.http.HttpSession;
import org.rigel5.DefaultUIManager;
import org.rigel5.db.sql.FiltroData;
import org.rigel5.table.FiltroListe;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.html.AbstractHtmlTablePagerFilter;
import org.rigel5.table.html.RigelHtmlPage;

/**
 * Pager destinato al solo caricamento dei dati.
 * Permette di recuperare i records dal table model senza generare HTML.
 * Può essere utile fare il parsing della maschera di ricerca ed ottenere
 * il set di record relativo senza voler generare una tabella HTML.
 * Questa classe permette di effettuare il parsing della maschera di
 * ricerca con parseFiltro() e di caricare i rispettivi record nel
 * table model con loadRecords().
 * I records sono poi accessibili dal table model con i suoi metodi
 * getRowCount() e getRowRecord().
 *
 * @author Nicola De Nisco
 */
public class SqlDataonlyPager extends AbstractHtmlTablePagerFilter
{
  private static int idCounter = 0;
  private SqlWrapperListaHtml wl;

  public SqlDataonlyPager()
  {
    super("SqlDataonlyPager" + (idCounter++));
  }

  public SqlDataonlyPager(SqlWrapperListaHtml wl)
     throws Exception
  {
    super("SqlDataonlyPager" + (idCounter++));
    this.wl = wl;

    // imposta numero per pagina
    if(wl.getNumPerPage() != 0)
      limit = wl.getNumPerPage();
  }

  public SqlWrapperListaHtml getWl()
  {
    return wl;
  }

  public void setWl(SqlWrapperListaHtml wl)
  {
    this.wl = wl;
  }

  @Override
  protected void getHtmlTable(RigelHtmlPage page)
     throws Exception
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public String getHtmlRicerca()
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    getHtmlFiltro(page);
    DefaultUIManager uim = new DefaultUIManager();
    return uim.formatHtmlLista(FILTRO_MACHERA, page);
  }

  public void clearFiltro()
     throws Exception
  {
    start = 0;
    cSelezione = new FiltroListe();
    for(int i = 0; i < getTableModel().getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = getTableModel().getColumn(i);
      cd.setFiltroSort(0);
      cd.setFiltroTipo(0);
      cd.setFiltroValore(null);
    }
  }

  public FiltroListe parseFiltro(Map params)
     throws Exception
  {
    start = 0;
    return cSelezione = creaFiltro(params);
  }

  public void loadRecords(int offset, int numrecs)
     throws Exception
  {
    SqlTableModel stm = (SqlTableModel) getTableModel();
    stm.getQuery().setOffset(0);
    stm.getQuery().setLimit(300);
    stm.getQuery().setIgnoreCase(true);
    stm.getQuery().setFiltro((FiltroData) (cSelezione.getOggFiltro()));
    stm.rebind();
  }

  /**
   * Ridefinita per annullare paginazione.
   */
  @Override
  public String getSelfUrl(String prefix, int rStart)
     throws Exception
  {
    return "";
  }

  /**
   * Ridefinita per annullare paginazione.
   */
  @Override
  public String getSelfUrl(int rStart, HttpSession sessione)
     throws Exception
  {
    return "";
  }

  /**
   * Ridefinita per annullare paginazione.
   */
  @Override
  public String getSortUrl(String fldName)
  {
    return null;
  }
}
