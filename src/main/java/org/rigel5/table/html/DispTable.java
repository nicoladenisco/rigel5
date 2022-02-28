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

import org.commonlib5.utils.StringOper;
import org.rigel5.table.ForeignDataHolder;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;

/**
 * <p>
 * Title: Rigel</p>
 * <p>
 * Description: Framework Html/Swing</p>
 * <p>
 * Copyright: Copyright (c) 2003</p>
 * <p>
 * Company: Italsystems s.r.l.</p>
 *
 * Una DispTable e' una tabella derivata da FormTable ma solo
 * di visualizzazione. Il suo utilizzo e' quello di creare una
 * maschera di consultazione a partire da una maschera originariamente
 * preparata per l'editing dei dati.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class DispTable extends FormTable
{
  public DispTable(String sId)
  {
    super(sId);
  }

  @Override
  protected String doInnerCell(int row, int col, String cellText, String cellHtml)
     throws Exception
  {
    if(cellText == null || cellText.trim().length() == 0)
      return "&nbsp;";

    RigelColumnDescriptor cd = getCD(col);

    // se nessun tipo di foreign esce adesso
    if(cd.getForeignMode() == RigelColumnDescriptor.DISP_FLD_ONLY || !(tableModel instanceof RigelTableModel))
      return cellText;

    ForeignDataHolder fd = cd.findHTableForeign(cellText, getTM(), i18n);

    if(fd == null)
    {
      // ritorna un foreign value di tipo INDEFINITO
      fd = new ForeignDataHolder();
      fd.codice = cellText;
      fd.alternateCodice = cellText;
      fd.descrizione = "INDEFINITO";
    }

    switch(cd.getForeignMode())
    {
      case RigelColumnDescriptor.DISP_FLD_ONLY:
        // nessun collegamento master-detail
        return cellText;

      case RigelColumnDescriptor.DISP_FLD_EDIT:
        // collegamento master-detail in edit senza descrizione
        return fd.descrizione;

      case RigelColumnDescriptor.DISP_DESCR_EDIT:
        // collegamento master-detail in edit con descrizione
        return "\r\n"
           + "<table border=0 cellspacing=0 cellpadding=0><tr>\r\n"
           + "<td>" + StringOper.okStr(fd.codice) + "</td>\r\n"
           + "<td>&nbsp;<b>" + StringOper.okStr(fd.descrizione) + "</b></td>\r\n"
           + "</tr></table>\r\n";

      case RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE:
        // collegamento master-detail in edit con descrizione
        return "\r\n"
           + "<table border=0 cellspacing=0 cellpadding=0><tr>\r\n"
           + "<td>" + StringOper.okStr(fd.alternateCodice) + "</td>\r\n"
           + "<td>&nbsp;<b>" + StringOper.okStr(fd.descrizione) + "</b></td>\r\n"
           + "</tr></table>\r\n";

      case RigelColumnDescriptor.DISP_FLD_DESCR:
        // collegamento master-detail di sola visualizzazione
        return fd.descrizione;

      case RigelColumnDescriptor.DISP_FLD_DESCR_ALTERNATE:
        // collegamento master-detail di sola visualizzazione (alternata)
        return fd.descrizione;

      default:
        // collegamento master-detail di sola visualizzazione
        return fd.descrizione;
    }
  }
}
