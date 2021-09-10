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

import java.net.URLEncoder;
import org.rigel2.RigelI18nInterface;
import org.rigel2.SetupHolder;
import org.rigel2.table.BuilderRicercaGenerica;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.RigelTableModel;

/**
 * Generazione HTML per filtro semplificato.
 * Versione semplificata di HtmlMascheraRicercaGenerica.
 * Genera una maschera di ricerca ma più semplice di quella generica.
 * Vengono generati unicamente i campi di ricerca, senza intestazioni
 * ne ordinamento.
 * Il suo scopo è di fornire una maschera di filtro a partire da un TableModel
 * per usi diversi di quelli previsti da Rigel.
 */
public class HtmlMascheraRicercaSemplice extends HtmlMascheraRicercaGenerica
{
  protected String fldPrefix = "";

  public HtmlMascheraRicercaSemplice(BuilderRicercaGenerica brg, RigelTableModel rtm, RigelI18nInterface i18n)
  {
    super(brg, rtm, i18n);
  }

  public HtmlMascheraRicercaSemplice(BuilderRicercaGenerica brg, RigelTableModel rtm, RigelI18nInterface i18n, String fldPrefix)
  {
    super(brg, rtm, i18n);
    this.fldPrefix = fldPrefix;
  }

  @Override
  protected String getFieldName(RigelColumnDescriptor cd)
  {
    return fldPrefix + super.getFieldName(cd);
  }

  /**
   * Ritorna l'HTML completo della maschera per l'impostazione
   * dei parametri di filtro e di ordinamento.
   * @param nomeForm the value of nomeForm
   * @param page the value of page
   * @throws Exception
   */
  @Override
  public void buildHtmlRicerca(String nomeForm, RigelHtmlPage page)
     throws Exception
  {
    this.formName = nomeForm;
    RigelHtmlPageComponent html = new RigelHtmlPageComponent(PageComponentType.HTML, "search");
    RigelHtmlPageComponent javascript = new RigelHtmlPageComponent(PageComponentType.JAVASCRIPT, "search");

    html.append("<table width=100%>\r\n"
       + "<tr>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\">" + i18n.msg("Campo") + " </td>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\">" + i18n.msg("Tipo filtro") + "</td>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\" colspan=2>" + i18n.msg("Valore filtro") + "</td>\r\n"
       + "</tr>\r\n");

    int u, d, n = rtm.getColumnCount();
    for(int i = 0; i < n; i++)
    {
      RigelColumnDescriptor cd = rtm.getColumn(i);
      if(cd.isEscludiRicerca())
        continue;

      // determina campi successivo e precedente
      u = d = i;
      RigelColumnDescriptor cdu, cdd;

      do
      {
        u = --u % n;
        if(u == -1)
          u = n - 1;
        cdu = rtm.getColumn(u);
      }
      while(cdu.isEditable() && u != i);

      do
      {
        d = ++d % n;
        cdd = rtm.getColumn(d);
      }
      while(cdd.isEditable() && d != i);

      int idx = cd.getFiltroTipo();
      String val = cd.getFiltroValore();
      if(val == null)
        val = "";

      int pos;
      String vaf = "";
      if((pos = val.indexOf("|")) != -1)
      {
        vaf = val.substring(pos + 1);
        val = val.substring(0, pos);
      }

      String fieldName = getFieldName(cd);
      String fieldNameUp = getFieldName(cdu);
      String fieldNameDw = getFieldName(cdd);
      String caption = i18n.localizeTableCaption(null, rtm, cd, i, cd.getCaption());

      html.append("<tr>\r\n");
      html.append("<td>").append(caption).append("</td>\r\n");
      html.append("<td><select name=\"OP").append(fieldName).append("\">");
      getComboItems(html.getContent(), idx);
      html.append("</select></td>\r\n");

      if(cd.isComboRicerca())
        html.append("<td colspan=2>").
           append(brg.getHtmlComboColonnaMaschera(formName, fieldName, cd, val, i18n)).
           append("</td>\r\n");

      else if(cd.isDate() && SetupHolder.getImgEditData() != null)
      {
        String sds = URLEncoder.encode("restartd_VL" + fieldName, "UTF-8");
        String sdt = URLEncoder.encode("restartd_VF" + fieldName, "UTF-8");
        String sdi = URLEncoder.encode("restartd_VL" + fieldName + "_VF" + fieldName, "UTF-8");

        html.append("<td><input type=\"text\" size=\"20\""
           + " name=\"VL" + fieldName + "\""
           + " value=\"" + val + "\" "
           + " onkeydown=\"return moveKey(document." + formName + ".VL" + fieldNameUp
           + " , document." + formName + ".VL" + fieldNameDw + ", event);\"> "
           + "&nbsp;<a href=\"javascript:apriCalendarioIntervalloForm('" + formName + "','" + sds + "','" + sdi + "')\">"
           + SetupHolder.getImgEditData() + "</a>"
           + "</td>\r\n");

        html.append("<td><input type=\"text\" size=\"20\""
           + " name=\"VF" + fieldName + "\""
           + " value=\"" + vaf + "\" "
           + " onkeydown=\"return moveKey(document." + formName + ".VF" + fieldNameUp
           + " , document." + formName + ".VF" + fieldNameDw + ", event);\"> "
           + "&nbsp;<a href=\"javascript:apriCalendarioIntervalloForm('" + formName + "','" + sdt + "','" + sdi + "')\">"
           + SetupHolder.getImgEditData() + "</a>"
           + "</td>\r\n");

        javascript.append(getScriptData(cd, fieldName));
      }
      else
      {
        html.append("<td><input type=\"text\" size=\"20\""
           + " name=\"VL" + fieldName + "\""
           + " value=\"" + val + "\" "
           + " onchange=\"cambia_VL" + fieldName + "()\" "
           + " onkeydown=\"return moveKey(document." + formName + ".VL" + fieldNameUp
           + " , document." + formName + ".VL" + fieldNameDw + ", event);\"> "
           + "</td>\r\n");

        html.append("<td><input type=\"text\" size=\"20\""
           + " name=\"VF" + fieldName + "\""
           + " value=\"" + vaf + "\" "
           + " onchange=\"cambia_VF" + fieldName + "()\" "
           + " onkeydown=\"return moveKey(document." + formName + ".VF" + fieldNameUp
           + " , document." + formName + ".VF" + fieldNameDw + ", event);\"> "
           + "</td>\r\n");

        javascript.append(getScriptCampi(cd, fieldName));
      }

      html.append("</tr>\r\n");
    }

    html.append("</table>\r\n");

    page.add(html);
    page.add(javascript);
  }
}
