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
package org.rigel5.glue.table;

import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.html.AlternateColorTable;

/**
 * Tabella a colori alternati con evidenziamento della riga
 * contentente il cursore e funzione di selezione della riga.
 * Quando la riga viene cliccata viene invocato il codice
 * javascript puntato da linkColStat.
 *
 * @author Nicola De Nisco
 */
public class AlternateColorTableAppGeneric extends AlternateColorTable
{
  protected String defColStat = null;
  protected String linkColStat = null;

  public AlternateColorTableAppGeneric()
  {
    super();

    setOddRowStatement("tr class=\"rowmenu1\"");
    setEvenRowStatement("tr class=\"rowmenu2\"");

    // recupera lo statement di colonna di default
    defColStat = getColStatement();
  }

  /**
   * Ritorna vero se la cella deve consentire di
   * chimare il link principale.
   * @param row
   * @param col
   * @return
   * @throws Exception
   */
  public boolean isLineLinkCell(int row, int col)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(col);
    return !(linkColStat == null || cd == null || cd.isCaratteristiche());
  }

  @Override
  public void doCell(int row, int col, String cellText, String text)
     throws Exception
  {
    if(isLineLinkCell(row, col))
      setColStatement("TD valign=\"middle\" onclick=\"" + linkColStat + "\"");
    else
      setColStatement(defColStat);

    super.doCell(row, col, cellText, text);
  }

  public void setLineLink(String uri)
  {
    linkColStat = "window.location.href='" + uri + "'";
  }

  public void setLineJavascipt(String javascript)
  {
    linkColStat = javascript;
  }

  public void clearLineLink()
  {
    linkColStat = null;
  }

  public boolean haveLineLink()
  {
    return linkColStat != null;
  }

  public String getLinkColStat()
  {
    return linkColStat;
  }

  public void setLinkColStat(String linkColStat)
  {
    this.linkColStat = linkColStat;
  }
}
