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
import org.apache.commons.logging.*;
import org.commonlib5.utils.CircularArray;
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
 * @author Nicola De Nisco
 * @version 1.0
 */
public class HtmlMascheraRicercaGenericaNoscript implements MascheraRicercaGenerica
{
  /** Logging */
  private static final Log log = LogFactory.getLog(HtmlMascheraRicercaGenericaNoscript.class);
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
      int idx = parseIntFiltro(params, "OP" + fieldName);
      String val = parseStringFiltro(params, "VL" + fieldName);
      String vaf = parseStringFiltro(params, "VF" + fieldName);
      int sortOrder = parseIntFiltro(params, "RS" + fieldName);

      if(HtmlUtils.checkForJavascriptInjection(val) || SqlUtils.checkForSqlInjection(val))
        throw new InjectionDetectedException();
      if(HtmlUtils.checkForJavascriptInjection(vaf) || SqlUtils.checkForSqlInjection(vaf))
        throw new InjectionDetectedException();

      // imposta default a non significativo
      cd.setFiltroTipo(0);
      cd.setFiltroValore(null);

      // se attivo autolike e tipo filtro non significativo
      // ma l'utente ha specificato un valore, promuove automaticamente il tipo ricerca a LIKE
      if(!cd.isComboRicerca())
        if(SetupHolder.isAutolike() && idx == 0 && val != null)
          idx = BuilderRicercaGenerica.IDX_CRITERIA_LIKE;

      if(idx == BuilderRicercaGenerica.IDX_CRITERIA_ISNULL || idx == BuilderRicercaGenerica.IDX_CRITERIA_ISNOTNULL)
      {
        // caso speciale: NULL / NOT NULL
        cd.setFiltroTipo(idx);
        cd.setFiltroValore(null);
      }
      else
      {
        if(val != null)
        {
          if(val.startsWith("re:") || val.startsWith("ri:"))
          {
            // regular expression; vedi sqlbuilderricercagenerica
            cd.setFiltroTipo(BuilderRicercaGenerica.IDX_CRITERIA_LIKE);
          }
          else
          {
            int pos = val.indexOf('|');
            if(pos == -1)
              cd.setFiltroTipo(idx);
            else
              cd.setFiltroTipo(BuilderRicercaGenerica.IDX_CRITERIA_BETWEEN);
          }

          if(brg.isBetween(cd.getFiltroTipo()) && vaf != null && vaf.trim().length() > 0)
            val += "|" + vaf;

          cd.setFiltroValore(val);
        }
      }

      cd.setFiltroSort(sortOrder);
    }

    // ordinamento da click sulla colonna
    String sSimpleSort = parseStringFiltro(params, "SSORT");
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

  private int parseIntFiltro(Map params, String fieldName)
  {
    return StringOper.parse(params.get(fieldName), 0);
  }

  private String parseStringFiltro(Map params, String fieldName)
  {
    Object o = params.get(fieldName);
    if(o == null)
      return null;

    if(o instanceof Object[])
    {
      Object[] oa = (Object[]) o;
      if(oa.length == 0)
        return null;
      return StringOper.okStrNull(oa[0]);
    }

    return StringOper.okStrNull(o);
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

    html.append("<div id=\"rigel_search_param_" + formName + "\" class=\"rigel_search_param\">\r\n"
       + "<input type=\"hidden\" name=\"filtro\" value=\"2\">");

    buildHtmlRicercaTable(html);
    html.append("</div>\r\n");

    page.add(html);
  }

  protected void buildHtmlRicercaTable(RigelHtmlPageComponent html)
     throws Exception
  {
    html.append("<table width=100%>\r\n"
       + "<tr>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\">" + i18n.msg("Campo") + "</td>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\">" + i18n.msg("Tipo filtro") + "</td>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\" colspan=2>" + i18n.msg("Valore filtro") + "</td>\r\n"
       + "<td class=\"rigel_search_param_header\" align=\"center\">" + i18n.msg("Ordinamento") + "</td>\r\n"
       + "</tr>\r\n");

    buildHtmlColumns(html);

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

  public static class InfoColonne
  {
    boolean jumpok;
    public RigelColumnDescriptor cd;
    public String fieldName, caption, campoVL, campoVF, htmlVL, htmlVF;
  }

  protected void buildHtmlColumns(RigelHtmlPageComponent html)
     throws Exception
  {
    int n = rtm.getColumnCount();
    CircularArray<InfoColonne> lsColonne = new CircularArray<>(n);

    // prima passata: costruisce array info colonne
    for(int i = 0; i < n; i++)
    {
      RigelColumnDescriptor cd = rtm.getColumn(i);
      if(cd.isEscludiRicerca())
        continue;

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

      InfoColonne ifc = new InfoColonne();
      lsColonne.add(ifc);

      ifc.cd = cd;
      ifc.fieldName = getFieldName(cd);
      ifc.campoVL = "VL" + ifc.fieldName;
      ifc.campoVF = "VF" + ifc.fieldName;
      ifc.caption = i18n.localizeTableCaption(null, rtm, cd, i, cd.getCaption());

      String funcMoveVL = "${moveVL_" + ifc.fieldName + "}";
      String funcMoveVF = "${moveVF_" + ifc.fieldName + "}";

      if(cd.isComboRicerca())
      {
        ifc.htmlVL = "<td colspan='2'>" + brg.getHtmlComboColonnaMaschera(formName, ifc.fieldName, cd, val, i18n) + "</td>\r\n";
      }
      else if(cd.isDate() && SetupHolder.getImgEditData() != null)
      {
        ifc.htmlVL = campoRicerca(ifc.campoVL, val, funcMoveVL, "rigel.apriCalIntR1('" + formName + "','" + ifc.fieldName + "')");
        ifc.htmlVF = campoRicerca(ifc.campoVF, vaf, funcMoveVF, "rigel.apriCalIntR2('" + formName + "','" + ifc.fieldName + "')");
        ifc.jumpok = true;
      }
      else
      {
        ifc.htmlVL = campoRicerca(ifc.campoVL, val, funcMoveVL, null);
        ifc.htmlVF = campoRicerca(ifc.campoVF, vaf, funcMoveVF, null);
        ifc.jumpok = true;
      }
    }

    // seconda passata: produce html tabella ricerca
    for(int i = 0; i < lsColonne.size(); i++)
    {
      InfoColonne ifc = lsColonne.get(i);
      int idx = ifc.cd.getFiltroTipo();

      if(ifc.htmlVL != null)
      {
        String funcMoveVL = "${moveVL_" + ifc.fieldName + "}";
        InfoColonne prevVL = lsColonne.getPrevValid(i, (ic) -> ic.jumpok && ic.campoVL != null);
        InfoColonne succVL = lsColonne.getSuccValid(i, (ic) -> ic.jumpok && ic.campoVL != null);

        if(prevVL != null && succVL != null)
        {
          String funcMove = "onkeydown=\"return rigel.moveKeyRicerca("
             + "'id_" + prevVL.campoVL + "', "
             + "'id_" + succVL.campoVL + "', event);\"";

          ifc.htmlVL = ifc.htmlVL.replace(funcMoveVL, funcMove);
        }
        else
        {
          ifc.htmlVL = ifc.htmlVL.replace(funcMoveVL, "");
        }
      }

      if(ifc.htmlVF != null)
      {
        String funcMoveVF = "${moveVF_" + ifc.fieldName + "}";
        InfoColonne prevVF = lsColonne.getPrevValid(i, (ic) -> ic.jumpok && ic.campoVF != null);
        InfoColonne succVF = lsColonne.getSuccValid(i, (ic) -> ic.jumpok && ic.campoVF != null);

        if(prevVF != null && succVF != null)
        {
          String funcMove = "onkeydown=\"return rigel.moveKeyRicerca("
             + "'id_" + prevVF.campoVF + "', "
             + "'id_" + succVF.campoVF + "', event);\"";

          ifc.htmlVF = ifc.htmlVF.replace(funcMoveVF, funcMove);
        }
        else
        {
          ifc.htmlVF = ifc.htmlVF.replace(funcMoveVF, "");
        }
      }

      html.append("<tr>\r\n");
      html.append("<td>").append(ifc.caption).append("</td>\r\n");
      html.append("<td><select name=\"OP").append(ifc.fieldName).append("\">");
      getComboItems(html.getContent(), idx);
      html.append("</select></td>\r\n");

      if(ifc.htmlVL != null && ifc.htmlVF == null)
      {
        html.append(ifc.htmlVL);
      }
      else if(ifc.htmlVL != null && ifc.htmlVF != null)
      {
        html.append(ifc.htmlVL);
        html.append(ifc.htmlVF);
      }
      else
      {
        // caso anomalo; in realta non dovrebbe capitare mai
        log.debug("RIGEL: caso anomalo in ricerca");
      }

      html.append("<td><select name=\"RS").append(ifc.fieldName).append("\">");
      getComboOrdinamento(html.getContent(), ifc.cd.getFiltroSort());
      html.append("</select></td>\r\n");

      html.append("</tr>\r\n");
    }
  }

  private String campoRicerca(String nome, String val, String funcMove, String scriptData)
  {
    StringBuilder htmlCampo = new StringBuilder();
    htmlCampo.append("<td>");
    htmlCampo.append("<input type=\"text\" size=\"20\"")
       .append(" id=\"id_").append(nome).append("\"")
       .append(" name=\"").append(nome).append("\"")
       .append(" value=\"").append(val).append("\"")
       .append(" ").append(funcMove).append(" >");

    if(scriptData != null)
      htmlCampo.append("&nbsp;<a href='#' onclick=\"").append(scriptData).append("\">")
         .append(SetupHolder.getImgEditData()).append("</a>");

    htmlCampo.append(" </td>\r\n");
    return htmlCampo.toString();
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
   * @param haveFilter
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
    String funTest = "onkeypress=\"return rigel.testInvio('" + formName + "', event);\"";

    RigelHtmlPageComponent html = new RigelHtmlPageComponent(PageComponentType.HTML, "simplesearch");
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

            html
               .append("&nbsp;<a onclick=\"rigel.apriCalRic('")
               .append(formName).append("','").append(fieldName).append("')\">")
               .append(SetupHolder.getImgEditData())
               .append("</a>");
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

    html
       .append("<!-- MORE SIMPLE SEARCH -->\r\n")
       .append("<input type=\"button\" name=\"SimpleSearch_").append(formName).append("\" value=\"")
       .append(i18n.getCaptionButtonCerca()).append("\" onclick=\"document.").append(formName).append(".submit();\"/>\r\n")
       .append("<input type=\"button\" name=\"publisciSimpleSearch_").append(formName).append("\" value=\"")
       .append(i18n.getCaptionButtonPulisci()).append("\" onclick=\"rigel.pulisciRicercaSemplice('").append(formName).append("');\"/>\r\n")
       .append(haveFilter ? " [" + i18n.msg("Filtro attivo") + "]" : "")
       .append("<!-- END FORM SIMPLE SEARCH -->\r\n")
       .append("</div>\r\n");

    page.add(html);
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
