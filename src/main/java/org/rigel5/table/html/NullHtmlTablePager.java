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
package org.rigel5.table.html;

import java.util.Map;
import javax.servlet.http.HttpSession;

/**
 * Paginatore nullo per tabelle.
 * Implementazione di un paginatore che di fatto non pagina mai.
 * Viene usato nelle tabelle di edit dove la paginazione nella
 * maggior parte dei casi non è possibile. Quindi se occorre
 * un paginatore per inserirlo in un wrapper, ma di fatto non
 * vogliamo la paginazione possiamo utilizzare questo paginatore.
 * @author Nicola De Nisco
 * @version 1.0
 */
public class NullHtmlTablePager extends AbstractHtmlTablePager
{
  /**
   * Funzione principale per la visualizzazione della pagina.
   * Dai parametri recupera il record di partenza.
   * Questo paginatore di fatto non li usa e quindi params
   * @param params parametri della richiesta http
   * @param sessione the value of sessione
   * @param page the value of page
   * @throws Exception
   */
  @Override
  public void getHtml(Map params, HttpSession sessione, RigelHtmlPage page)
     throws Exception
  {
    getHtmlTable(page);
  }

  /**
   * Da ridefinire nelle classi derivate: restituisce l'url della pagina
   * in cui viene inserita la tabella con la relativa indicazione del record
   * di start.
   */
  @Override
  public String getSelfUrl(String prefix, int rStart)
     throws Exception
  {
    return "";
  }

  @Override
  public String getSelfUrl(int rStart, HttpSession sessione)
     throws Exception
  {
    return "";
  }

  @Override
  public String getSortUrl(String fldName)
  {
    return null;
  }
}
