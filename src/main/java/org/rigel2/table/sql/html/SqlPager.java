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
package org.rigel2.table.sql.html;

import org.rigel2.db.sql.FiltroData;
import org.rigel2.table.html.*;

/**
 * Pager specializzato per liste SQL.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlPager extends CommonPager
{
  private static int idCounter = 0;

  public SqlPager()
  {
    super("SqlPager" + (idCounter++));
  }

  public SqlPager(String id)
  {
    super(id);
  }

  /**
   * Ritorna l'html per la pagina richiesta.
   * Dal db vengono prelevati solo i record necessari.
   * @param page the value of page
   * @throws java.lang.Exception
   */
  @Override
  protected void getHtmlTable(RigelHtmlPage page)
     throws Exception
  {
    SqlTableModel stm = (SqlTableModel) (getTableModel());

    stm.getQuery().setOffset(start);
    stm.getQuery().setLimit(limit);
    stm.getQuery().setIgnoreCase(true);
    stm.getQuery().setFiltro((FiltroData) (cSelezione.getOggFiltro()));

    addExtraFilter(stm);
    stm.rebind();

    if(stm.getRowCount() > 0)
      table.doHtml(page);
  }

  /**
   * Imposta dei filtri supplementari sul criterio di selezione.
   * Puo' essere ridefinita per implementare dei filtri ulteriori o
   * un ordinamento di default.* Puo'
   * <pre><code>
   * private SqlPager bleCli = new SqlPager("Clifor")
   * {
   *   protected void addExtraFilter(SqlTableModel stm)
   *    throws Exception
   * {
   * if(!isFiltro())
   * stm.setOrderBy("descrizione ASC");
   *
   * if(isAgente)
   * {
   * String agente = getUser(session).getFirstName();
   * stm.setWhere("id_agente1='"+agente+"'");
   * }
   * }
   * };
   *
   * </code</pre>
   * @
   * param stm gestore dati sql
   * @throws Exception
   */
  protected void addExtraFilter(SqlTableModel stm)
     throws Exception
  {
  }
}
