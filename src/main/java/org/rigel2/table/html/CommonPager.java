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
package org.rigel2.table.html;

import java.util.*;
import javax.servlet.http.HttpSession;

/**
 * Oggetto base di PeerListaElem e SqlListaElem.
 * Contiene i metodi per la restituzione dell'HTML
 * correttamente formattato e impaginato dal RigelUIManager.
 * @author Nicola De Nisco
 */
abstract public class CommonPager extends AbstractHtmlTablePagerFilter
{
  public CommonPager(String id)
  {
    super(id);
  }

  /**
   * Ritorna l'html per la pagina.
   * @param params parametri della richiesta HTML
   * @param sessione sessione della richiesta
   * @return HTML completo
   * @throws Exception
   */
  @Override
  public String getHtml(Map params, HttpSession sessione)
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    getHtml(params, sessione, page);
    return uim.formatHtmlLista(filtro, page);
  }

  /**
   * Ritorna l'html per la pagina con inclusa la
   * ricerca semplice per i campi appositamente
   * etichettati.
   * @param params parametri della richiesta HTML
   * @param sessione sessione della richiesta
   * @return l'html da visualizzare
   * @throws java.lang.Exception
   */
  @Override
  public String getHtmlSimpleSearch(Map params, HttpSession sessione)
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    getHtml(params, sessione, page);
    return uim.formatSimpleSearch(filtro, page);
  }

  /**
   * Ritorna l'html per la pagina con inclusa la
   * ricerca semplice per i campi appositamente
   * etichettati.
   * @param params parametri della richiesta HTML
   * @param sessione sessione della richiesta
   * @return l'html da visualizzare
   * @throws java.lang.Exception
   */
  @Override
  public String getHtmlSimpleSearchPalmare(Map params, HttpSession sessione)
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    getHtml(params, sessione, page);
    return uim.formatSimpleSearchPalmare(filtro, page);
  }
}
