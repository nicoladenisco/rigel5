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

import java.net.URLEncoder;
import java.util.Map;
import org.apache.commons.logging.*;
import org.commonlib5.utils.StringOper;
import org.rigel5.HtmlUtils;
import org.rigel5.RigelI18nInterface;
import org.rigel5.SetupHolder;
import org.rigel5.SqlUtils;
import org.rigel5.exceptions.InjectionDetectedException;
import org.rigel5.table.BuilderRicercaGenerica;
import org.rigel5.table.MascheraRicercaGenerica;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;

/**
 * Generazione HTML per filtro generico.
 * Contiene le funzioni comuni alle due implementazioni della maschera
 * di ricerca.
 * <p>
 * Questa classe produce l'interfaccia per la creazione dei filtri di ricerca
 * in formato HTML. Essa lavora di conserva con oggetti che implementino
 * l'interfaccia BuilderRicercaGenerica per la creazione materiale dei filtri.
 * </p>
 *
 * @deprecated genera javascript nella pagina: usare una implementazione con rigel.js
 * @author Nicola De Nisco
 * @version 1.0
 */
@Deprecated
public class HtmlMascheraRicercaGenerica implements MascheraRicercaGenerica
{
  /** Logging */
  private static final Log log = LogFactory.getLog(HtmlMascheraRicercaGenerica.class);
  protected BuilderRicercaGenerica brg = null;
  protected RigelTableModel rtm = null;
  protected String formName = "fo";
  protected RigelI18nInterface i18n = null;

  @Override
  public void init(BuilderRicercaGenerica brg, RigelTableModel rtm, RigelI18nInterface i18n)
  {
    this.brg = brg;
    this.rtm = rtm;
    this.i18n = i18n;
    formName = rtm.getFormName();
  }

  /**
   * Ritorna l'HTML completo per il combo con i criteri di ricerca.
   * @param sb accumulatore HTML
   * @param defVal valore di default
   */
  protected void getComboItems(StringBuilder sb, int defVal)
  {
    String[] comboItems = brg.getTipiConfronto();
    for(int j = 0; j < comboItems.length; j++)
    {
      sb.append(HtmlUtils.generaOptionCombo(j, i18n.msg(comboItems[j]), defVal));
    }
  }

  /**
   * Ritorna l'HTML completo per il combo per la selezione
   * del tipo di ordinamento richiesto.
   * @param sb accumulatore HTML
   * @param defVal valore di default
   */
  protected void getComboOrdinamento(StringBuilder sb, int defVal)
  {
    String asc = i18n.msg("Ascendente");
    String des = i18n.msg("Discendente");

    for(int k = 0; k < 4; k++)
    {
      int i = k + 1;
      sb.append(HtmlUtils.generaOptionCombo(i, i + "-" + asc, defVal));
    }

    for(int k = 0; k < 4; k++)
    {
      int j = k + 1001;
      sb.append(HtmlUtils.generaOptionCombo(j, j + "-" + des, defVal));
    }

    sb.append(HtmlUtils.generaOptionCombo(0, "No", defVal));
  }

  /**
   * Ritorna la porzione di javascript necessaria a consentire l'editing
   * del calendario.
   * @param cd colonna del campo
   * @param fieldName nome del campo data
   * @return il codice javascript
   */
  protected String getScriptData(RigelColumnDescriptor cd, String fieldName)
  {
    String nomeCampoInizio = "VL" + fieldName;
    String nomeCampoFine = "VF" + fieldName;
    String nomeCampoOperazione = "OP" + fieldName;

    return "\r\n"
       + "function restartd_" + nomeCampoInizio + "(strdate) {\r\n"
       + "      document." + formName + "." + nomeCampoInizio + ".value = strdate;\r\n"
       + "      document." + formName + "." + nomeCampoOperazione + ".value = 2;\r\n"
       + "}\r\n"
       + "function restartd_" + nomeCampoFine + "(strdate) {\r\n"
       + "      document." + formName + "." + nomeCampoFine + ".value = strdate;\r\n"
       + "      document." + formName + "." + nomeCampoOperazione + ".value = 8;\r\n"
       + "}\r\n"
       + "function restartd_" + nomeCampoInizio + "_" + nomeCampoFine + "(ss) {\r\n"
       + "      var idx = ss.indexOf(\"|\");\r\n"
       + "      var s1  = ss.substring( 0, idx);\r\n"
       + "      var s2  = ss.substring(idx+1);\r\n"
       + "      document." + formName + "." + nomeCampoInizio + ".value = s1;\r\n"
       + "      document." + formName + "." + nomeCampoFine + ".value = s2;\r\n"
       + "      document." + formName + "." + nomeCampoOperazione + ".value = 8;\r\n"
       + "}\r\n"
       + "\r\n";
  }

  protected String getScriptCampi(RigelColumnDescriptor cd, String fieldName)
  {
    return "\r\n"
       + "function cambia_VL" + fieldName + "() {\r\n"
       + "      document." + formName + ".OP" + fieldName + ".value = 1;\r\n"
       + "}\r\n"
       + "function cambia_VF" + fieldName + "() {\r\n"
       + "      document." + formName + ".OP" + fieldName + ".value = 8;\r\n"
       + "}\r\n"
       + "\r\n";
  }

  /**
   * Nome di base dei campi per la colonna.
   * Per ogni colonna verranno generati dei campi
   * VL... VF... OP... RS...
   * la parte finale del nome viene creata da questa funzione.
   * @param cd colonna di riferimento
   * @return nome distintivo del campo
   */
  protected String getFieldName(RigelColumnDescriptor cd)
  {
    return "_" + formName + "_" + StringOper.CvtASCIIstring(cd.getName());
  }

  /**
   * Costruisce il criterio di selezione in base ai parametri
   * impostati. Questa funzione in realta' e' solo un frontend
   * al metodo buildCriteria del BuilderRicercaGenerica.
   * @param params map con i parametri della richiesta HTTP
   * @return un oggetto di selezione (vedi BuilderRicercaGenerica)
   * @throws Exception
   */
  protected Object buildCriteria(Map params)
     throws Exception
  {
    for(int i = 0; i < rtm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = rtm.getColumn(i);
      if(cd.isEscludiRicerca())
        continue;

      String fieldName = getFieldName(cd);
      int idx = StringOper.parse(params.get("OP" + fieldName), 0);
      String val = StringOper.okStrNull(params.get("VL" + fieldName));
      String vaf = StringOper.okStrNull(params.get("VF" + fieldName));

      if(HtmlUtils.checkForJavascriptInjection(val) || SqlUtils.checkForSqlInjection(val))
        throw new InjectionDetectedException();
      if(HtmlUtils.checkForJavascriptInjection(vaf) || SqlUtils.checkForSqlInjection(vaf))
        throw new InjectionDetectedException();

      cd.setFiltroTipo(0); // non significativo

      if(idx == 9 || idx == 10)
      {
        // caso speciale: NULL / NOT NULL
        cd.setFiltroTipo(idx);
        cd.setFiltroValore(null);
      }
      else
      {
        if(val != null)
        {
          int pos = val.indexOf('|');
          if(pos == -1)
            cd.setFiltroTipo(idx);
          else
            cd.setFiltroTipo(8);
        }

        if(brg.isBetween(cd.getFiltroTipo()) && vaf != null && vaf.trim().length() > 0)
          val += "|" + vaf;

        cd.setFiltroValore(val);
      }

      cd.setFiltroSort(StringOper.parse(params.get("RS" + fieldName), 0));
    }

    // ordinamento da click sulla colonna
    String sSimpleSort = (String) (params.get("SSORT"));
    if(sSimpleSort != null && sSimpleSort.length() > 0)
    {
      int idxCol = Integer.parseInt(sSimpleSort);
      if(idxCol > 0)
      {
        RigelColumnDescriptor cd = rtm.getColumn(idxCol - 1);
        if(!cd.isEscludiRicerca() && cd.getFiltroSort() == 0)
          cd.setFiltroSort(1);
      }
      else if(idxCol < 0)
      {
        RigelColumnDescriptor cd = rtm.getColumn(-idxCol - 1);
        if(!cd.isEscludiRicerca() && cd.getFiltroSort() == 0)
          cd.setFiltroSort(1001);
      }
    }

    return brg.buildCriteria();
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

    html.append("<div id=\"rigel_search_param_" + formName + "\" class=\"rigel_search_param\">\r\n"
       + "<input type=\"hidden\" name=\"filtro\" value=\"2\">");

    buildHtmlRicercaTable(html, javascript);
    html.append("</div>\r\n");

    page.add(html);
    page.add(javascript);
  }

  protected void buildHtmlRicercaTable(RigelHtmlPageComponent html, RigelHtmlPageComponent javascript)
     throws Exception
  {
    html.append("<table width=100%>\r\n"
       + "<tr>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\">" + i18n.msg("Campo") + "</td>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\">" + i18n.msg("Tipo filtro") + "</td>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\" colspan=2>" + i18n.msg("Valore filtro") + "</td>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\">" + i18n.msg("Ordinamento") + "</td>\r\n"
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
        html.append("<td colspan=2>")
           .append(brg.getHtmlComboColonnaMaschera(formName, fieldName, cd, val, i18n))
           .append("</td>\r\n");

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

      html.append("<td><select name=\"RS").append(fieldName).append("\">");
      getComboOrdinamento(html.getContent(), cd.getFiltroSort());
      html.append("</select></td>\r\n");

      html.append("</tr>\r\n");
    }

    // help
    html.append("<TR><TD COLSPAN=5>&nbsp;</TD></TR>");
    html.append("<tr>\r\n");
    html.append("<td class=\"rigel_search_param_footer\" valign=\"top\" width=15%>")
       .append(i18n.msg("Campo su cui applicare la regola di filtro."))
       .append("</td>\r\n<td class=\"rigel_search_param_footer\" valign=\"top\" width=20%>")
       .append(i18n.msg("Tipo di filtro da applicare al campo."))
       .append("</td>\r\n<td class=\"rigel_search_param_footer\" valign=\"top\" width=25%>")
       .append(i18n.msg("Valore del filtro; se il tipo di filtro e' <b>compreso</b> rappresenta il valore iniziale."))
       .append("</td>\r\n<td class=\"rigel_search_param_footer\" valign=\"top\" width=25%>")
       .append(i18n.msg("Valido solo se il tipo di filtro e' <b>compreso</b>: rappresenta il valore finale."))
       .append("</td>\r\n<td class=\"rigel_search_param_footer\" valign=\"top\" width=15%>")
       .append(i18n.msg("Stabilisce se il campo partecipa all'ordinamento dei risultati ed in quale ordine."))
       .append("</td>\r\n</tr>\r\n");
    html.append("</table>\r\n");
  }

  /**
   * Ritorna l'HTML completo della ricerca semplice.
   * @param nomeForm the value of nomeForm
   * @param sizeFld the value of sizeFld
   * @param page the value of page
   * @throws Exception
   */
  @Override
  public void buildHtmlRicercaSemplice(String nomeForm, int sizeFld, boolean haveFilter, RigelHtmlPage page)
     throws Exception
  {
    buildHtmlRicercaSemplice(nomeForm, sizeFld, haveFilter, page, SetupHolder.getMaxSiSeColumn());
  }

  /**
   * Ritorna l'HTML completo della ricerca semplice.
   * @param nomeForm the value of nomeForm
   * @param sizeFld the value of sizeFld
   * @param page the value of page
   * @param maxFields numero massimo campi ammessi in ricerca semplice
   * @throws Exception
   */
  protected void buildHtmlRicercaSemplice(String nomeForm, int sizeFld, boolean haveFilter, RigelHtmlPage page, int maxFields)
     throws Exception
  {
    this.formName = nomeForm;
    boolean valid = false;
    String firstControl = null;
    int simpleSearchColumn = 0;
    int simpleSearchWeight = 0;
    int numSiSeColumn = 0;
    String clearForm = "";
    String funSubmit = "document." + formName + ".submit();";
    String funTest = "onkeypress='return testInvio_" + formName + "(event);'";

    RigelHtmlPageComponent html = new RigelHtmlPageComponent(PageComponentType.HTML, "simplesearch");
    RigelHtmlPageComponent javascript = new RigelHtmlPageComponent(PageComponentType.JAVASCRIPT, "simplesearch");
    html.append("<div class=\"rigel_simple_search\">\r\n")
       .append("<!-- BEGIN SIMPLE SEARCH -->\r\n");

    for(int i = 0; i < rtm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = rtm.getColumn(i);

      if(!cd.isEscludiRicerca() && ((cd.getFiltroSort() % 1000) > simpleSearchWeight))
      {
        simpleSearchColumn = cd.getFiltroSort() > 1000 ? -(i + 1) : (i + 1);
        simpleSearchWeight = cd.getFiltroSort() % 1000;
      }

      if(cd.getRicercaSemplice() == 0)
        continue;

      if(numSiSeColumn >= maxFields)
        continue;

      String fieldName = getFieldName(cd);
      String caption = i18n.localizeTableCaption(null, rtm, cd, i, cd.getCaption());
      String defval = cd.getFiltroValore();
      int idx = cd.getFiltroTipo();
      if(idx == 0)
        defval = "";

      clearForm += "document." + formName + ".VL" + fieldName + ".value='';\r\n";

      if(cd.isComboRicerca())
      {
        html.append(caption).append("&nbsp;");
        html.append("<input type=\"hidden\" name=\"OP").append(fieldName).append("\" value=\"")
           .append(BuilderRicercaGenerica.IDX_CRITERIA_EQUAL).append("\">")
           .append(brg.getHtmlComboColonnaRicSemplice(formName, fieldName, cd, defval, i18n))
           .append("&nbsp;&nbsp;\r\n");
      }
      else
      {
        int opIdx = cd.getRicercaSemplice();

        html.append(caption).append("&nbsp;");

        if(cd.isDate())
        {
          if(opIdx == BuilderRicercaGenerica.IDX_CRITERIA_BETWEEN)
          {
            // l'uso di IDX_CRITERIA_BETWEEN su un campo date produce un combo box con gli intervalli più utilizzati
            html.append("<input type=\"hidden\" name=\"OP")
               .append(fieldName).append("\" value=\"").append(opIdx).append("\">");
            getComboBoxAnnoCompleto(html.getContent(), "VL" + fieldName, StringOper.parse(defval, 0));
          }
          else if(SetupHolder.getImgEditData() != null)
          {
            // campo per input data forzato a EQUAL
            html
               .append("<input type=\"hidden\" name=\"OP").append(fieldName).append("\" value=\"")
               .append(BuilderRicercaGenerica.IDX_CRITERIA_EQUAL)
               .append("\"><input type=\"text\" name=\"VL").append(fieldName).append("\" value=\"")
               .append(defval == null ? "" : defval).append("\" size=\"").append(sizeFld).append("\" ")
               .append(funTest).append(">");

            // aggiunge calendario per i campi data
            String nomeCampoInizio = "VL" + fieldName;
            String sds = URLEncoder.encode("restartd_" + nomeCampoInizio, "UTF-8");

            html
               .append("&nbsp;<a href=\"javascript:apriCalendarioForm('")
               .append(formName).append("','").append(sds).append("')\">")
               .append(SetupHolder.getImgEditData())
               .append("</a>");

            javascript.append(getScriptData(cd, fieldName));
          }
        }
        else
        {
          // solo i campi alfanumerici possono avere un filtro
          // di ricerca rapida diverso da uguale
          if(!cd.isAlpha())
            opIdx = BuilderRicercaGenerica.IDX_CRITERIA_EQUAL;

          // in tutti gli altri casi il campo per il valore di ricerca
          html
             .append("<input type=\"hidden\" name=\"OP").append(fieldName).append("\" value=\"").append(opIdx)
             .append("\"><input type=\"text\" name=\"VL").append(fieldName).append("\" value=\"")
             .append(defval == null ? "" : defval).append("\" size=\"").append(sizeFld).append("\" ")
             .append(funTest).append(">");
        }

        html.append("&nbsp;&nbsp;\r\n");
      }

      if(firstControl == null)
        firstControl = "VL" + fieldName;

      valid = true;
      numSiSeColumn++;
    }

    if(!valid)
      return;

    html
       .append("<input type=\"hidden\" name=\"filtro\" value=\"").append(AbstractHtmlTablePagerFilter.FILTRO_APPLICA).append("\"/>\r\n")
       .append("<input type=\"hidden\" name=\"SSORT\" value=\"").append(simpleSearchColumn).append("\"/>\r\n");

    javascript
       .append("document.").append(formName).append(".").append(firstControl).append(".focus();\r\n")
       .append("\r\n")
       .append("function SimpleSort_").append(formName).append("(idx)\r\n")
       .append("{\r\n")
       .append("   val=document.").append(formName).append(".SSORT.value;\r\n")
       .append("   if(idx == Math.abs(val))\r\n")
       .append("   {\r\n")
       .append("     document.").append(formName).append(".SSORT.value=-val;\r\n")
       .append("   }\r\n")
       .append("   else\r\n")
       .append("   {\r\n")
       .append("     document.").append(formName).append(".SSORT.value=idx;\r\n")
       .append("   }\r\n")
       .append("   document.").append(formName).append(".submit();\r\n")
       .append("}\r\n")
       .append("\r\n")
       .append("function pulisciRicercaSemplice_").append(formName).append("()\r\n")
       .append("{\r\n")
       .append(clearForm)
       .append("   document.").append(formName).append(".SSORT.value=0;\r\n")
       .append("   document.").append(formName).append(".submit();\r\n")
       .append("}\r\n")
       .append("function testInvio_").append(formName).append("(e)\r\n")
       .append("{\r\n")
       .append("  if(e == null) e=event;\r\n")
       .append("  if(e.keyCode == 13){\r\n")
       .append("   document.").append(formName).append(".submit();\r\n")
       .append("   return false;\r\n")
       .append("  }\r\n")
       .append("  return true;\r\n")
       .append("}\r\n")
       .append("");

    html
       .append("<!-- MORE SIMPLE SEARCH -->\r\n")
       .append("<input type=\"button\" name=\"SimpleSearch_").append(formName).append("\" value=\"")
       .append(i18n.getCaptionButtonCerca()).append("\" onclick=\"document.").append(formName).append(".submit();\"/>\r\n")
       .append("<input type=\"button\" name=\"publisciSimpleSearch_").append(formName).append("\" value=\"")
       .append(i18n.getCaptionButtonPulisci()).append("\" onclick=\"pulisciRicercaSemplice_").append(formName).append("();\"/>\r\n")
       .append(haveFilter ? " [" + i18n.msg("Filtro attivo") + "]" : "")
       .append("<!-- END FORM SIMPLE SEARCH -->\r\n")
       .append("</div>\r\n");

    page.add(html);
    page.add(javascript);
  }

  protected void getComboBoxAnnoCompleto(StringBuilder sb, String nomeCampo, int anno1)
  {
    String setComboFunjs = "document." + formName + ".submit();";
    sb.append("<SELECT name=\"").append(nomeCampo).append("\" onchange=\"").append(setComboFunjs).append("\">\n");

    sb.append(HtmlUtils.generaOptionCombo(0, i18n.msg("TUTTI"), anno1));
    sb.append(HtmlUtils.generaOptionCombo(1, i18n.msg("Oggi"), anno1));
    sb.append(HtmlUtils.generaOptionCombo(2, i18n.msg("Ieri e oggi"), anno1));
    sb.append(HtmlUtils.generaOptionCombo(3, i18n.msg("Ultimi due giorni"), anno1));
    sb.append(HtmlUtils.generaOptionCombo(4, i18n.msg("Ultima settimana"), anno1));
    sb.append(HtmlUtils.generaOptionCombo(5, i18n.msg("Ultimi 15 giorni"), anno1));
    sb.append(HtmlUtils.generaOptionCombo(6, i18n.msg("Ultimi 6 mesi"), anno1));
    sb.append(HtmlUtils.generaOptionCombo(7, i18n.msg("Ultimo anno"), anno1));

    sb.append("</SELECT>\n");
  }

  /**
   * Risponde al metodo POST dell'HTTP ovvero costruisce
   * il criterio di ricerca in base ai parametri caricati nella
   * maschera generata da buildHtmlRicerca.
   * Chiama la buildCriteria.
   * @param params map con i parametri della richiesta HTTP
   * @return un oggetto di selezione (vedi BuilderRicercaGenerica)
   * @throws Exception
   */
  @Override
  public Object buildCriteriaSafe(Map params)
     throws Exception
  {
    try
    {
      return buildCriteria(params);
    }
    catch(InjectionDetectedException ex)
    {
      throw ex;
    }
    catch(Exception ex)
    {
      log.error("RIGEL:", ex);
      return null;
    }
  }
}
