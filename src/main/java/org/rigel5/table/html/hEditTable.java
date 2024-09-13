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

import com.workingdogs.village.Record;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.om.Persistent;
import org.commonlib5.utils.StringOper;
import org.rigel5.HtmlUtils;
import org.rigel5.SetupHolder;
import org.rigel5.table.CustomColumnEdit;
import org.rigel5.table.ForeignDataHolder;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.peer.html.PeerTableModel;
import org.rigel5.table.sql.html.SqlTableModel;

/**
 * Tabella con edit dei campi.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class hEditTable extends hTable
{
  /** Logging */
  private static final Log log = LogFactory.getLog(hEditTable.class);

  protected boolean colonnaAnnulla = true;
  protected String captionAnnulla = "An";
  protected String id = "";
  protected RigelHtmlPageComponent scriptTest = new RigelHtmlPageComponent(PageComponentType.JAVASCRIPT_PART, "test");
  protected String popupListaFunction = "apriFinestraLista";
  protected String popupFormFunction = "apriFinestraForm";
  protected boolean attivaProtezioneCSRF = false;

  public hEditTable(String sId)
  {
    id = sId;
    attivaProtezioneCSRF = SetupHolder.isAttivaProtezioneCSRF();
  }

  public void setColonnaAnnulla(boolean newColonnaAnnulla)
  {
    colonnaAnnulla = newColonnaAnnulla;
  }

  public boolean isColonnaAnnulla()
  {
    return colonnaAnnulla;
  }

  public boolean isAttivaProtezioneCSRF()
  {
    return attivaProtezioneCSRF;
  }

  public void setAttivaProtezioneCSRF(boolean attivaProtezioneCSRF)
  {
    this.attivaProtezioneCSRF = attivaProtezioneCSRF;
  }

  public void setCaptionAnnulla(String newCaptionAnnulla)
  {
    captionAnnulla = newCaptionAnnulla;
  }

  public String getCaptionAnnulla()
  {
    return captionAnnulla;
  }

  public String getNomeCampo(int row, int col)
  {
    return (id + "_" + getFldName(col) + "_" + row).toLowerCase();
  }

  @Override
  public String getNomePara(int row, int col)
  {
    return (id + "_" + getFldName(col) + "_P_" + row).toLowerCase();
  }

  public String getNomeCheckDel(int row)
  {
    return (id + "_ckd_" + row).toLowerCase();
  }

  /**
   * Parsing del valore dalla sua rappresentazione stringa.
   * Se la colonna ha un formattatore esplicito lo usa.
   * @param row riga corrente
   * @param col colonna corrente
   * @param value rappresentazione stringa del valore
   * @return valore sotto forma di Object (salvabile attraverso il table model)
   * @throws Exception
   */
  public Object parseCell(int row, int col, String value)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return null;

    if(cd.getForeignMode() == RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE)
    {
      // in questo caso editiamo un valore che in realtà non esiste
      // su questa tabella, ma è un altro campo della tabella collegata
      // quindi qui lo recuperiamo per il salvataggio
      ForeignDataHolder fd = cd.findHTableForeignAlternate(value, getTM(), i18n);

      if(fd != null)
        value = fd.codice;
    }

    return cd.parseValueNull(value);
  }

  public String getFldName(int col)
  {
    return ((RigelColumnDescriptor) (columnModel.getColumn(col))).getName();
  }

  public int getFldSize(int col)
  {
    return ((RigelColumnDescriptor) (columnModel.getColumn(col))).getSize();
  }

  /**
   * Salva i dati di un riga.
   * @param row riga da salvare
   * @param params mappa dei parametri
   * @throws Exception
   */
  public void salvaDatiRiga(int row, Map params)
     throws Exception
  {
    if(colonnaAnnulla)
    {
      String valore = StringOper.okStrNull(params.get(getNomeCheckDel(row)));
      if(valore != null && tableModel instanceof RigelTableModel)
      {
        ((RigelTableModel) (tableModel)).setRowDeleted(row, true);
        //log.debug("Record "+row+" cancellato!");
        return;
      }
    }

    for(int col = 0, num = tableModel.getColumnCount(); col < num; col++)
      salvaDatiCella(row, col, params);
  }

  /**
   * Salva i dati di una cella.
   * @param row riga da salvare
   * @param col colonna da salvare
   * @param params mappa dei parametri
   * @throws Exception
   */
  public void salvaDatiCella(int row, int col, Map params)
     throws Exception
  {
    String nomeCampo = null, valore = null;
    RigelColumnDescriptor cd = null;

    try
    {
      nomeCampo = getNomeCampo(row, col);
      if((cd = (RigelColumnDescriptor) (columnModel.getColumn(col))) == null)
        throw new Exception("Colonna " + col + " non trovata.");

      valore = StringOper.okStrNull(params.get(nomeCampo));

      // compatibilità con ParameterParser di turbine che potrebbe
      // convertire tutte le chiavi in minuscolo o in maiuscolo
      if(valore == null)
        valore = StringOper.okStrNull(params.get(nomeCampo.toLowerCase()));
      if(valore == null)
        valore = StringOper.okStrNull(params.get(nomeCampo.toUpperCase()));

      if(valore == null && cd.getDefVal() != null)
        valore = cd.getDefVal();
      if(valore == null && cd.isBoolean())
        valore = "0";

      if((isColumnEditable(row, col) || cd.isHiddenEdit()))
      {
        String originale = formatValoreCampo(row, col);

        // gestione default su colonne non visibili (il default e' gia' caricato)
        if(originale != null && valore == null && !cd.isVisible())
          return;

        if(cd.getColedit() != null && cd.getColedit().haveCustomParser())
        {
          // il parser custom può modificare il valore attingendo a campi diversi
          // in questo caso il contenuto di valore non ha significato
          Object colValue = cd.getColedit().parseValue(
             cd, tableModel, row, col, valore, nomeCampo, originale, params, i18n);

          tableModel.setValueAt(colValue, row, col);
        }
        else if(!checkValueEqual(originale, valore))
        {
          // converte il valore del campo nel tipo corretto e lo salva
          Object colValue = parseCell(row, col, valore);
          tableModel.setValueAt(colValue, row, col);
        }
      }
    }
    catch(Exception ex)
    {
      String msg = id + " Parsing di: row=" + row + " col=" + col + " nome=" + nomeCampo + " val=" + valore;
      log.error(msg, ex);
    }
  }

  /**
   * Verifica per dati uguali.
   * Confronta il contenuto precedente di un campo e il suo valore
   * attuale restituendo true se sono da considerarsi uguale (ovvero
   * il campo non verra' aggiornato).
   * @param originale contenuto del campo
   * @param valore nuovo dato da memorizzare nel campo
   * @return true se sono da considerarsi uguali (non aggiornare il campo)
   * @throws java.lang.Exception
   */
  protected boolean checkValueEqual(String originale, String valore)
     throws Exception
  {
    return StringOper.isEqu(originale, valore);
  }

  /**
   * Salva i dati di questa tabella.
   * @param params mappa dei parametri
   * @throws Exception
   */
  public void salvaDati(Map params)
     throws Exception
  {
    salvaDati(params, 0, tableModel.getRowCount());
  }

  /**
   * Salva i dati di un gruppo di record.
   * @param params mappa dei parametri
   * @param rStart record di partenza
   * @param numRec numero di record da salvare
   * @throws Exception
   */
  public void salvaDati(Map params, int rStart, int numRec)
     throws Exception
  {
    if(rStart > tableModel.getRowCount())
      return;
    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    while(numRec-- > 0)
    {
      salvaDatiRiga(rStart, params);
      rStart++;
    }
  }

  @Override
  public void doCell(int row, int col, String cellText, String cellHtml)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(col);
    if(cd.isVisible())
    {
      if(isColumnEditable(row, col) || cd.isHiddenEdit())
      {
        // genera campo di input per dato (eventualmente invisibile)
        String inner = StringOper.okStr(doInnerCell(row, col, cellText, cellHtml), "&nbsp;");
        html.append(cellBegin(row, col));
        html.append(inner);
        html.append(cellEnd(row, col));
        return;
      }

      // genera cella con il valore semplice
      super.doCell(row, col, cellText, cellHtml);
      return;
    }

    // solo campo invisibile se richiesto (senza <td>)
    if(cd.isHiddenEdit())
      html.append("<input type=\"hidden\" name=\"")
         .append(getNomeCampo(row, col))
         .append("\" value=\"").append(cellText).append("\">\r\n");
  }

  /**
   * Elabora il contenuto interno di una cella.
   * @param row riga corrente
   * @param col colonna corrente
   * @param cellText testo interno della cella
   * @param cellHtml html interno della cella
   * @return HTML
   * @throws Exception
   */
  protected String doInnerCell(int row, int col, String cellText, String cellHtml)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(col);
    final CustomColumnEdit coledit = cd.getColedit();
    final String nomeCampo = getNomeCampo(row, col);

    if(coledit != null && coledit.haveCustomHtml())
    {
      // colonna con generatore HTML custom per l'editing
      String htmlCell = coledit.getHtmlEdit(cd, tableModel, row, col, cellText, cellHtml, nomeCampo, i18n);

      // se il custom edit ritorna null vuol dire che per questa colonna va bene il comportamento di default
      if(htmlCell != null)
      {
        // caso speciale del test custom applicato anche a campi custom edit
        if(cd.getTestcustom() != null)
        {
          String campoForm = "document." + formName + "." + nomeCampo;
          String caption = i18n.localizeTableCaption(this, getTM(), cd, col, cd.getCaption());
          scriptTest.append("    if(!" + cd.getTestcustom() + "(" + campoForm + ",\"" + nomeCampo + "\", \"" + caption + "\"))  return false;\r\n");
        }

        return htmlCell;
      }
    }

    String rigelHtml = buildHtml(cd, row, col, cellText, cellHtml);

    if(coledit != null && coledit.haveAddHtml())
    {
      // eventuale controllo con html aggiunto
      return coledit.addHtmlEdit(cd, tableModel, row, col, cellText, nomeCampo, rigelHtml, i18n);
    }

    return rigelHtml;
  }

  private String buildHtml(RigelColumnDescriptor cd, int row, int col, String cellText, String cellHtml)
     throws Exception
  {
    if(cd.useForeignAutoCombo(row, col, getTM()))
      return getForeignEditAutocombo(row, col, cellText, cellHtml);

    switch(cd.getForeignMode())
    {
      case RigelColumnDescriptor.DISP_FLD_ONLY:
        // nessun collegamento master-detail
        return cellHtml;
      case RigelColumnDescriptor.DISP_FLD_EDIT:
        // collegamento master-detail in edit senza descrizione
        return getForeignEditFld(row, col, cellText, cellHtml);
      case RigelColumnDescriptor.DISP_DESCR_EDIT:
        // collegamento master-detail in edit con descrizione
        return getForeignEditDescr(row, col, cellText, cellHtml);
      case RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE:
        // collegamento master-detail in edit con descrizione
        return getForeignEditDescrAlternate(row, col, cellText, cellHtml);
      default:
        // collegamento master-detail di sola visualizzazione
        return getForeignDataViewOnly(row, col, cellText, cellHtml);
    }
  }

  /**
   * Visualizza i dati foreign in sola lettura.
   * @param row riga corrente
   * @param col colonna corrente
   * @param cellText testo interno della cella
   * @param cellHtml html interno della cella
   * @return HTML
   * @throws Exception
   */
  protected String getForeignDataViewOnly(int row, int col, String cellText, String cellHtml)
     throws Exception
  {
    String foreignVal = "";
    RigelColumnDescriptor cd = getCD(col);

    Object val = tableModel.getValueAt(row, col);
    if(val != null)
      foreignVal = getForeignData(cd, val.toString());

    foreignVal = elaboraFixedText(row, col, foreignVal);

    if(!isColumnEditable(row, col))
      return foreignVal;

    return "\r\n"
       + "<table border=0 cellspacing=0 cellpadding=0><tr>\r\n"
       + "<td>" + cellHtml + "</td>\r\n"
       + "<td>&nbsp;<b>" + foreignVal + "</b></td>\r\n"
       + "</tr></table>\r\n";
  }

  protected boolean isZeroValue(int row, int col)
     throws Exception
  {
    RigelTableModel tm = getTM();
    if(tm == null)
      return false;

    String testVal = StringOper.okStr(tm.getValueAt(row, col));
    return testVal.equals("0");
  }

  /**
   * In autocombo viene visualizzato a fianco del combo box
   * solo l'icona di visualizzazione del record allegato
   * (se la url è presente).
   * @param row riga corrente
   * @param col colonna corrente
   * @param cellText testo interno della cella
   * @param cellHtml html interno della cella
   * @return HTML
   * @throws Exception
   */
  protected String getForeignEditAutocombo(int row, int col, String cellText, String cellHtml)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(col);
    String fldName = getNomeCampo(row, col);
    String formUrl = null;

    String rv = "\r\n"
       + "<table border=0 cellspacing=0 cellpadding=0><tr>\r\n"
       + "<td>" + cellHtml + "</td>\r\n";

    // attiva visualizzazione/edit del record su tabella allegata
    if(getImgFormForeign() != null && cd.getForeignFormUrl() != null
       && tableModel instanceof RigelTableModel
       && !isZeroValue(row, col)
       && (formUrl = buildForeignFormUrl(cd, fldName, row, col)) != null)
      rv += "<td>&nbsp;" + HtmlUtils.makeHrefNoenc(formUrl, getImgFormForeign())
         + "&nbsp;</td>\r\n";

    rv += "</tr></table>\r\n";

    return rv;
  }

  /**
   * Visualizza il campo con a fianco le icone di lista e form.
   * @param row riga corrente
   * @param col colonna corrente
   * @param cellText testo interno della cella
   * @param cellHtml html interno della cella
   * @return HTML
   * @throws Exception
   */
  protected String getForeignEditFld(int row, int col, String cellText, String cellHtml)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(col);
    String fldName = getNomeCampo(row, col);
    String editUrl = null, formUrl = null;

    String rv = "\r\n"
       + "<table border=0 cellspacing=0 cellpadding=0><tr>\r\n"
       + "<td>" + cellHtml + "</td>\r\n";

    // attiva scelta attraverso tabella allegata
    if(getImgEditForeign() != null && cd.getForeignEditUrl() != null
       && (editUrl = buildForeignListUrl(cd, fldName, row, col)) != null)
      rv += "<td>&nbsp;" + HtmlUtils.makeHrefNoenc(editUrl, getImgEditForeign())
         + "&nbsp;</td>\r\n";

    // attiva visualizzazione/edit del record su tabella allegata
    if(getImgFormForeign() != null && cd.getForeignFormUrl() != null
       && tableModel instanceof RigelTableModel
       && !isZeroValue(row, col)
       && (formUrl = buildForeignFormUrl(cd, fldName, row, col)) != null)
      rv += "<td>&nbsp;" + HtmlUtils.makeHrefNoenc(formUrl, getImgFormForeign())
         + "&nbsp;</td>\r\n";

    rv += "</tr></table>\r\n";

    addScriptEdit(row, col, fldName, false, cd.getExtraScript());

    return rv;
  }

  /**
   * Visualizza il campo con a fianco le icone di lista e form, più il campo descrizione.
   * Il valore del campo descrizione viene prelevato dalla tabella collegata.
   * @param row riga corrente
   * @param col colonna corrente
   * @param cellText testo interno della cella
   * @param cellHtml html interno della cella
   * @return HTML
   * @throws Exception
   */
  protected String getForeignEditDescr(int row, int col, String cellText, String cellHtml)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(col);
    String fldName = getNomeCampo(row, col);
    String foreign = getForeignData(row, col);
    String editUrl = null, formUrl = null;

    String rv = "\r\n"
       + "<table border=0 cellspacing=0 cellpadding=0><tr>\r\n"
       + "<td>" + cellHtml + "</td>\r\n";

    // attiva scelta attraverso tabella allegata
    if(getImgEditForeign() != null && cd.getForeignEditUrl() != null
       && (editUrl = buildForeignListUrl(cd, fldName, row, col)) != null)
      rv += "<td>&nbsp;" + HtmlUtils.makeHrefNoenc(editUrl, getImgEditForeign())
         + "&nbsp;</td>\r\n";

    // attiva visualizzazione/edit del record su tabella allegata
    if(getImgFormForeign() != null && cd.getForeignFormUrl() != null
       && tableModel instanceof RigelTableModel
       && !isZeroValue(row, col)
       && (formUrl = buildForeignFormUrl(cd, fldName, row, col)) != null)
      rv += "<td>&nbsp;" + HtmlUtils.makeHrefNoenc(formUrl, getImgFormForeign())
         + "&nbsp;</td>\r\n";

    rv += "<td><div id=\"" + fldName + "_ED\" style=\"font-weight: bold;\">"
       + foreign + "&nbsp;</div></td>\r\n"
       + "</tr></table>\r\n";

    addScriptEdit(row, col, fldName, true, cd.getExtraScript());

    return rv;
  }

  /**
   * Visualizza il campo con a fianco le icone di lista e form, più il campo descrizione.
   * Il valore del campo descrizione viene prelevato dalla tabella collegata.
   * @param row riga corrente
   * @param col colonna corrente
   * @param cellText testo interno della cella
   * @param cellHtml html interno della cella
   * @return HTML
   * @throws Exception
   */
  protected String getForeignEditDescrAlternate(int row, int col, String cellText, String cellHtml)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(col);
    String fldName = getNomeCampo(row, col);
    String foreign = getForeignData(row, col);
    String editUrl = null, formUrl = null;

    String rv = "\r\n"
       + "<table border=0 cellspacing=0 cellpadding=0><tr>\r\n"
       + "<td>" + cellHtml + "</td>\r\n";

    // attiva scelta attraverso tabella allegata
    if(getImgEditForeign() != null && cd.getForeignEditUrl() != null
       && (editUrl = buildForeignListUrl(cd, fldName, row, col)) != null)
      rv += "<td>&nbsp;" + HtmlUtils.makeHrefNoenc(editUrl, getImgEditForeign())
         + "&nbsp;</td>\r\n";

    // attiva visualizzazione/edit del record su tabella allegata
    if(getImgFormForeign() != null && cd.getForeignFormUrl() != null
       && tableModel instanceof RigelTableModel
       && !isZeroValue(row, col)
       && (formUrl = buildForeignFormUrl(cd, fldName, row, col)) != null)
      rv += "<td>&nbsp;" + HtmlUtils.makeHrefNoenc(formUrl, getImgFormForeign())
         + "&nbsp;</td>\r\n";

    rv += "<td><div id=\"" + fldName + "_ED\" style=\"font-weight: bold;\">"
       + foreign + "&nbsp;</div></td>\r\n"
       + "</tr></table>\r\n";

    addScriptEdit(row, col, fldName, true, cd.getExtraScript());

    return rv;
  }

  /**
   * Costruisce l'url per aprire la finestra di lista foreign.
   * @param cd descrittore di colonna
   * @param fldName nome del campo
   * @param row riga corrente
   * @param col colonna corrente
   * @return HTML con la chiamata javascript per aprire la finestra
   * @throws Exception
   */
  protected String buildForeignListUrl(RigelColumnDescriptor cd, String fldName, int row, int col)
     throws Exception
  {
    String url = urlBuilder.buildUrlForeginList(editPopup,
       (RigelTableModel) tableModel, cd, fldName, row, col);
    if(url == null)
      return null;

    // verfica per url con macro
    int pos;
    if((pos = url.indexOf("@@@")) != -1)
    {
      String url1 = url.substring(0, pos);
      String url2 = url.substring(pos + 3);

      url1 = cd.parseMacro(url1);
      url1 = ((RigelTableModel) tableModel).getValueMacroInside(row, col, url1, false, true);

      url2 = cd.parseMacro(url2);
      url2 = ((RigelTableModel) tableModel).getValueMacroInside(row, col, url2, false, true);

      String sds = "func=" + URLEncoder.encode("imposta_" + fldName, "UTF8");
      return url1 + sds + url2;
    }
    else
    {
      url = cd.parseMacro(url);
      url = ((RigelTableModel) tableModel).getValueMacroInside(row, col, url, false, true);
      url = HtmlUtils.mergeUrl(url, "func", "imposta_" + fldName);
    }

    return "javascript:" + popupListaFunction + "('" + url + "', 'subList_" + StringOper.purge(cd.getName()) + "')";
  }

  /**
   * Costruisce url per aprire la finestra di form foreign.
   * @param cd descrittore di colonna
   * @param fldName nome del campo
   * @param row riga corrente
   * @param col colonna corrente
   * @return HTML con la chiamata javascript per aprire la finestra
   * @throws Exception
   */
  protected String buildForeignFormUrl(RigelColumnDescriptor cd, String fldName, int row, int col)
     throws Exception
  {
    String url = urlBuilder.buildUrlForeginForm(editPopup,
       (RigelTableModel) tableModel, cd, fldName, row, col);
    if(url == null)
      return null;

    String sds = cd.makeForeignFormParamsForUrl(row, (RigelTableModel) tableModel);
    if(sds == null)
      return null;

    // verfica per url con macro
    int pos;
    if((pos = url.indexOf("@@@")) != -1)
    {
      String url1 = url.substring(0, pos);
      String url2 = url.substring(pos + 3);

      url1 = cd.parseMacro(url1);
      url1 = ((RigelTableModel) tableModel).getValueMacroInside(row, col, url1, false, true);

      url2 = cd.parseMacro(url2);
      url2 = ((RigelTableModel) tableModel).getValueMacroInside(row, col, url2, false, true);

      return url1 + sds + url2;
    }

    url = cd.parseMacro(url);
    url = ((RigelTableModel) tableModel).getValueMacroInside(row, col, url, false, true);

    url = (url.indexOf('?') == -1) ? url + "?" + sds : url + "&" + sds;
    return "javascript:" + popupFormFunction + "('" + url + "', 'subForm_" + StringOper.purge(cd.getName()) + "')";
  }

  /**
   * Produce Javascript per le funzioni di movimento fra campi.
   * @param row riga corrente
   * @param col colonna corrente
   * @return HTML con la chiamata javascript per gestire i movimenti
   * @throws Exception
   */
  protected String moveKey(int row, int col)
     throws Exception
  {
    if(numRows > 1)
    {
      int rowu = (row - 1) % numRows;
      int rowd = (row + 1) % numRows;
      if(rowu == -1)
        rowu = numRows - 1;
      return "onKeyDown=\"return moveKey(document." + formName + "." + getNomeCampo(rowu, col)
         + ", document." + formName + "." + getNomeCampo(rowd, col) + ", event)\" ";
    }
    else
    {
      int colu = (col - 1) % numCols;
      int cold = (col + 1) % numCols;
      if(colu == -1)
        colu = numCols - 1;
      return "onKeyDown=\"return moveKey(document." + formName + "." + getNomeCampo(row, colu)
         + ", document." + formName + "." + getNomeCampo(row, cold) + ", event)\" ";
    }
  }

  /**
   * Ritorna la il valore da editare formattato come stringa.
   * @param row riga corrente
   * @param col colonna corrente
   * @return il valore da editare sotto forma di stringa
   * @throws Exception
   */
  protected String formatValoreCampo(int row, int col)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    // definizione del valore da inserire nel campo
    String sval = doCellText(row, col, tableModel.getValueAt(row, col));
    if(cd.getForeignMode() == RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE)
    {
      // in questo caso editiamo un valore che in realtà non esiste
      // su questa tabella, ma è un altro campo della tabella collegata
      // quindi qui lo recuperiamo per la visualizzazione
      ForeignDataHolder fd = cd.findHTableForeign(sval, getTM(), i18n);

      if(fd != null)
        sval = fd.alternateCodice;
    }

    return sval;
  }

  protected String campoAlfanumerico(int row, int col)
     throws Exception
  {
    int size = getFldSize(col);
    String navUpDown = moveKey(row, col);

    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    String style = null;
    switch(cd.getHtmlAlign())
    {
      default:
      case RigelColumnDescriptor.HTML_ALIGN_DEFAULT:
        style = "aldefault";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_LEFT:
        style = "alsinistra";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_CENTER:
        style = "alcentro";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_RIGHT:
        style = "aldestra";
        break;
    }

    // definizione del valore da inserire nel campo
    String sval = formatValoreCampo(row, col);

    if(size < 0)
    {
      // mette la lughezza fino alla fine dell'area ed il numero di colonne è l'assoluto del size (-3=3 righe)
      return "<textarea name=\"" + getNomeCampo(row, col) + "\" "
         + "style=\"width: 100%;\" rows=\"" + Math.abs(size) + "\">"
         + sval
         + "</textarea>";
    }

    if(size < 100)
      return "<input name=\"" + getNomeCampo(row, col) + "\" " + navUpDown
         + "size=\"" + size + "\" value=\""
         + sval + "\""
         + " class=\"" + style + "\""
         + ">";

    int numcol = size % 100;
    int numrow = size / 100;
    // nota: textarea non ha il navUpDown perchè interferisce con il corretto funzionamento
    return "<textarea name=\"" + getNomeCampo(row, col) + "\" "
       + "rows=\"" + numrow + "\" cols=\"" + numcol + "\">"
       + sval
       + "</textarea>";
  }

  protected String campoNumerico(int row, int col)
     throws Exception
  {
    return campoAlfanumerico(row, col);
  }

  protected String campoBooleano(int row, int col)
     throws Exception
  {
    String navUpDown = moveKey(row, col);
    Boolean b = (Boolean) (tableModel.getValueAt(row, col));

    return "<input type=\"checkbox\" name=\"" + getNomeCampo(row, col) + "\" " + navUpDown
       + "value=\"1\" "
       + (b != null && b ? "checked" : "") + ">";
  }

  protected String campoData(int row, int col)
     throws Exception
  {
    if(getImgEditData() == null)
      return campoAlfanumerico(row, col);

    String fldName = getNomeCampo(row, col);
    String sds = URLEncoder.encode("restartd_" + fldName, "UTF-8");

    addScriptData(fldName, getCD(col).getExtraScript());

    return campoAlfanumerico(row, col)
       + "&nbsp;" + HtmlUtils.makeHrefNoenc(
          "javascript:apriCalendarioForm('" + formName + "', '" + sds + "')",
          getImgEditData());
  }

  protected String campoFile(int row, int col)
     throws Exception
  {
    int size = getFldSize(col);
    String navUpDown = moveKey(row, col);
    String style = null;

    switch(getCD(col).getHtmlAlign())
    {
      default:
      case RigelColumnDescriptor.HTML_ALIGN_DEFAULT:
        style = "aldefault";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_LEFT:
        style = "alsinistra";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_CENTER:
        style = "alcentro";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_RIGHT:
        style = "aldestra";
        break;
    }

    String nomeCampo = getNomeCampo(row, col);
    String value = doCellText(row, col, tableModel.getValueAt(row, col));

    javascript.append(""
       + "    function fileChange_" + nomeCampo + "()\r\n"
       + "    {\r\n"
       + "      var filePath = document." + formName + "." + nomeCampo + "_file.value;\r\n"
       + "      // check for IE's lovely security speil\n"
       + "      if(filePath.match(/fakepath/)) {\n"
       + "          // update the file-path text using case-insensitive regex\n"
       + "          filePath = filePath.replace(/C:\\\\fakepath\\\\/i, '');\n"
       + "      }\n"
       + "      document." + formName + "." + nomeCampo + ".value = filePath;\r\n"
       + "    }\r\n"
    );

    return "<input name=\"" + nomeCampo + "\" " + navUpDown
       + "size=\"" + (size - 15) + "\" value=\"" + value + "\"" + " class=\"" + style + "\""
       + ">&nbsp;<input type=\"file\" name=\"" + nomeCampo + "_file\" "
       + "size=\"5\" value=\"\"" + " class=\"" + style + "\" onchange=\"fileChange_" + nomeCampo + "()\">\r\n"
       + "\r\n";
  }

  protected String campoHidden(int row, int col)
     throws Exception
  {
    String defVal = doCellText(row, col, tableModel.getValueAt(row, col));

    return "<input type=\"hidden\" name=\"" + getNomeCampo(row, col) + "\" value=\""
       + defVal + "\">" + (defVal.length() == 0 ? "&nbsp;" : "");
  }

  /**
   * Aggiunge la porzione di javascript necessaria a consentire l'editing
   * del calendario. ATTENZIONE: questa funzione e' strettamente legata
   * al calendario di newstar (calendario.jsp).
   * @param fldName nome del campo data
   * @param extraScript codice java script supplementare
   * @throws Exception
   */
  protected void addScriptData(String fldName, String extraScript)
     throws Exception
  {
    javascript.append("\r\n"
       + "  function restartd_" + fldName + "(strdate)\r\n"
       + "  {\r\n"
       + "      document." + formName + "." + fldName + ".value = strdate;\r\n"
       + (extraScript == null ? "" : extraScript)
       + "  }\r\n"
       + "\r\n"
    );
  }

  /**
   * Aggiunge la porzione di javascript necessaria a consentire l'editing
   * di dati esterni.
   * @param row riga di interesse
   * @param col colonna di interesse
   * @param fldName nome del campo
   * @param conDescrizione emette codice per modifica della descrizione non editabile
   * @param extraScript eventuale script extra da eseguire al momento dell'editing
   * @throws Exception
   */
  protected void addScriptEdit(int row, int col, String fldName, boolean conDescrizione, String extraScript)
     throws Exception
  {
    if(extraScript != null)
    {
      RigelColumnDescriptor cd = getCD(col);
      if(cd != null)
      {
        extraScript = cd.parseMacro(extraScript);
        extraScript = ((RigelTableModel) tableModel).getValueMacroInside(row, col, extraScript, true, false);
      }
    }

    addScriptEdit(fldName, conDescrizione, extraScript);
  }

  /**
   * Aggiunge la porzione di javascript necessaria a consentire l'editing
   * di dati esterni.
   * @param fldName nome del campo
   * @param conDescrizione
   * @param extraScript
   * @throws Exception
   */
  protected void addScriptEdit(String fldName, boolean conDescrizione, String extraScript)
     throws Exception
  {
    String script = null;

    if(conDescrizione)
      script = "\r\n"
         + "  function imposta_" + fldName + "(codice, descri)\r\n"
         + "  {\r\n"
         + "    document." + formName + "." + fldName + ".value=codice;\r\n"
         + "    document.getElementById('" + fldName + "_ED').innerHTML=descri;\r\n"
         + (extraScript == null ? "" : extraScript)
         + "  }\r\n"
         + "\r\n";
    else
      script = "\r\n"
         + "  function imposta_" + fldName + "(codice, descri)\r\n"
         + "  {\r\n"
         + "    document." + formName + "." + fldName + ".value=codice;\r\n"
         + (extraScript == null ? "" : extraScript)
         + "  }\r\n"
         + "\r\n";

    javascript.append(script);
  }

  /**
   * Produce e accumula in scriptTest il codice javascript
   * per testare la validita' dei dati inseriti nei campi.
   * @param tc oggetto colonna di riferimento
   * @param row riga corrente
   * @param col colonna corrente
   * @throws java.lang.Exception
   */
  protected void addToFormTests(RigelColumnDescriptor tc, int row, int col)
     throws Exception
  {
    String nomeCampo = getNomeCampo(row, col);
    String campoForm = "document." + formName + "." + nomeCampo;
    String caption = i18n.localizeTableCaption(this, getTM(), tc, col, tc.getCaption());

    if(tc.isTestfornull())
      scriptTest.append("    if(!testCampoNull(" + campoForm + ",\"" + caption + "\"))  return false;\r\n");

    if(tc.isTestfortype() || (tc.isTestforzero() && tc.isNumeric()))
    {
      String testFun = null, testRangeFun = null;
      switch(tc.getDataType())
      {
        case RigelColumnDescriptor.PDT_BOOLEAN:
          // i campi boolean non sono sottoposti a controllo
          break;
        case RigelColumnDescriptor.PDT_NUMBERKEY:
        case RigelColumnDescriptor.PDT_INTEGER:
          testFun = "testCampoNumericoIntero";
          testRangeFun = "testCampoRangeIntero";
          break;
        case RigelColumnDescriptor.PDT_FLOAT:
        case RigelColumnDescriptor.PDT_DOUBLE:
        case RigelColumnDescriptor.PDT_MONEY:
          testFun = "testCampoNumericoFloat";
          testRangeFun = "testCampoRangeFloat";
          break;
        case RigelColumnDescriptor.PDT_DATE:
        case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
        case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
        case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
        case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
        case RigelColumnDescriptor.PDT_TIMESTAMP:
        case RigelColumnDescriptor.PDT_TIME:
          testFun = "testCampoData";
          break;
        case RigelColumnDescriptor.PDT_STRINGKEY:
        case RigelColumnDescriptor.PDT_STRING:
          testFun = "testCampoAlfanumerico";
          break;
      }

      if(testFun != null)
        scriptTest.append("    if(!" + testFun + "(" + campoForm + ",\"" + caption + "\"))  return false;\r\n");

      if(tc.isTestrange() && testRangeFun != null)
        scriptTest.append("    if(!" + testRangeFun + "(" + campoForm + ",\"" + caption + "\", "
           + tc.getTestrangemin() + ", " + tc.getTestrangemax() + "))  return false;\r\n");
    }

    if(tc.isTestforcodice())
      scriptTest.append("    if(!testCampoCodice(" + campoForm + ",\"" + caption + "\"))  return false;\r\n");

    if(tc.isTestforzero() && tc.isNumeric())
      scriptTest.append("    if(!testCampoZero(" + campoForm + ",\"" + caption + "\"))  return false;\r\n");

    if(tc.isTestforcf())
      scriptTest.append("    if(!testCampoCodFis(" + campoForm + ",\"" + caption + "\"))  return false;\r\n");

    if(tc.isTestforpi())
      scriptTest.append("    if(!testCampoPIVA(" + campoForm + ",\"" + caption + "\"))  return false;\r\n");

    if(tc.getTestcustom() != null)
      scriptTest.append("    if(!" + tc.getTestcustom() + "(" + campoForm + ",\"" + nomeCampo + "\", \"" + caption + "\"))  return false;\r\n");
  }

  /**
   * Formatta l'interno della cella (non la parte foreign).
   * Ridefinita dalla classe base, in base al tipo di dato
   * produce l'HTML per l'edit del campo.
   * @param row riga corrente
   * @param col colonna corrente
   * @param cellText contenuto dell campo
   * @return HTML interno della colonna (senza tag TD)
   * @throws Exception
   */
  @Override
  public String doCellHtml(int row, int col, String cellText)
     throws Exception
  {
    RigelColumnDescriptor tc = getCD(col);

    if((tableModel instanceof RigelTableModel) && tc.isComboDisplay())
    {
      if(tc.isEditable())
      {
        String navUpDown = moveKey(row, col);
        String nomeCombo = getNomeCampo(row, col);
        String sOut = "<select name=\"" + nomeCombo + "\" " + navUpDown;
        if(tc.getExtraScript() != null)
          sOut += " onChange=\"" + tc.getExtraScript() + "\" ";
        sOut += ">\n";
        sOut += tc.getHtmlComboColonnaAttached(row, col, getTM(), cellText, i18n, false);
        sOut += "</select>";
        addToFormTests(tc, row, col);
        return sOut;
      }
      else
        return tc.getValueComboAttached(row, col, (RigelTableModel) tableModel, cellText, i18n);
    }

    if((tableModel instanceof RigelTableModel) && tc.isEditable() && tc.useForeignAutoCombo(row, col, getTM()))
    {
      // funzione autocombo: trasforma una foreign edit in un combo se possibile
      String navUpDown = moveKey(row, col);
      String nomeCombo = getNomeCampo(row, col);
      String sOut = "<select name=\"" + nomeCombo + "\" " + navUpDown;
      if(tc.getExtraScript() != null)
        sOut += " onChange=\"" + tc.getExtraScript() + "\" ";
      sOut += ">\n";
      sOut += tc.getHtmlForeignAutoCombo(row, col, getTM(), cellText, i18n);
      sOut += "\n</select>";
      addToFormTests(tc, row, col);
      return sOut;
    }
    else if((tableModel instanceof RigelTableModel) && isColumnEditable(row, col))
    {
      addToFormTests(tc, row, col);

      switch(tc.getDataType())
      {
        case RigelColumnDescriptor.PDT_BOOLEAN:
          return campoBooleano(row, col);
        case RigelColumnDescriptor.PDT_INTEGER:
          return campoNumerico(row, col);
        case RigelColumnDescriptor.PDT_FLOAT:
          return campoNumerico(row, col);
        case RigelColumnDescriptor.PDT_DOUBLE:
          return campoNumerico(row, col);
        case RigelColumnDescriptor.PDT_MONEY:
          return campoNumerico(row, col);
        case RigelColumnDescriptor.PDT_DATE:
        case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
        case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
        case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
        case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
        case RigelColumnDescriptor.PDT_TIMESTAMP:
        case RigelColumnDescriptor.PDT_TIME:
          return campoData(row, col);
        case RigelColumnDescriptor.PDT_STRINGKEY:
          return campoAlfanumerico(row, col);
        case RigelColumnDescriptor.PDT_NUMBERKEY:
          return campoNumerico(row, col);
        case RigelColumnDescriptor.PDT_STRING:
          return campoAlfanumerico(row, col);
        case RigelColumnDescriptor.PDT_FILE:
          return campoFile(row, col);
      }

      return campoAlfanumerico(row, col);
    }
    else if(tc.isHiddenEdit())
    {
      addToFormTests(tc, row, col);
      return campoHidden(row, col);
    }
    else
      return super.doCellHtml(row, col, cellText);
  }

  public boolean isColumnEditable(int row, int col)
  {
    return ((RigelTableModel) (tableModel)).isCellEditable(row, col);
  }

  @Override
  public float getNormalizeRef()
  {
    return colonnaAnnulla ? 95.0f : 100.0f;
  }

  @Override
  public synchronized void doHtml(int rStart, int numRec, RigelHtmlPage page)
     throws Exception
  {
    clearScriptTest();
    html.clear();
    javascript.clear();

    if(tableModel instanceof RigelTableModel)
      formName = ((RigelTableModel) (tableModel)).getFormName();

    if(rStart > tableModel.getRowCount())
      return;
    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    normalizeCols();
    html.append("<div id=\"rigel_hedittable_").append(formName)
       .append("\" class=\"rigel_hedittable\">\r\n<")
       .append(tableStatement).append(">\r\n");

    if(showHeader)
      doHeader();

    doRows(rStart, numRec);

    html.append("</TABLE>\r\n</div>\r\n");

    if(!html.isEmpty())
      page.add(html);

    if(!javascript.isEmpty())
      page.add(javascript);

    if(!scriptTest.isEmpty())
      page.add(scriptTest);
  }

  public String getId()
  {
    return id;
  }

  public void setId(String id)
  {
    this.id = id;
  }

  public void clearScriptTest()
  {
    scriptTest.clear();
  }

  public String getScriptTest()
  {
    return scriptTest == null ? "" : scriptTest.toString();
  }

  public String getPopupListaFunction()
  {
    return popupListaFunction;
  }

  public void setPopupListaFunction(String popupListaFunction)
  {
    this.popupListaFunction = popupListaFunction;
  }

  public String getPopupFormFunction()
  {
    return popupFormFunction;
  }

  public void setPopupFormFunction(String popupFormFunction)
  {
    this.popupFormFunction = popupFormFunction;
  }

  /**
   * Carica eventuali valori di default per il nuovo oggetto.
   * @param newObj nuovo oggetto creato
   * @param param parametri della richiesta
   * @param radiceNomeParametri radice del nome da cercare nei parametri (generlamente wrapper.getNome())
   * @throws Exception
   */
  public void caricaDefaultsNuovoOggetto(Persistent newObj, Map param, String radiceNomeParametri)
     throws Exception
  {
    PeerTableModel ptm = (PeerTableModel) (getModel());
    for(int i = 0, num = ptm.getColumnCount(); i < num; i++)
    {
      RigelColumnDescriptor cd = ptm.getColumn(i);
      if(cd.getDefVal() != null)
      {
        String sVal = cd.getDefVal();
        if(sVal.equals("@today"))
          sVal = cd.formatValue(new Date());
        cd.setValueAscii(newObj, sVal);
      }

      String key = cd.getDefValParam();
      if(key != null)
      {
        Object defVal = param.get(key);
        if(StringOper.okStrNull(defVal) == null)
          defVal = param.get(radiceNomeParametri + key);
        if(StringOper.okStrNull(defVal) != null)
          cd.setValueAscii(newObj, defVal.toString());
      }
    }
  }

  /**
   * Carica eventuali valori di default per il nuovo oggetto.
   * @param newObj nuovo oggetto creato
   * @param param parametri della richiesta
   * @param radiceNomeParametri radice del nome da cercare nei parametri (generlamente wrapper.getNome())
   * @throws Exception
   */
  public void caricaDefaultsNuovoOggetto(Record newObj, Map param, String radiceNomeParametri)
     throws Exception
  {
    SqlTableModel ptm = (SqlTableModel) (getModel());
    for(int i = 0, num = ptm.getColumnCount(); i < num; i++)
    {
      RigelColumnDescriptor cd = ptm.getColumn(i);
      if(cd.getDefVal() != null)
      {
        String sVal = cd.getDefVal();
        if(sVal.equals("@today"))
          sVal = cd.formatValue(new Date());
        cd.setValueAscii(newObj, sVal);
      }

      String key = cd.getDefValParam();
      if(key != null)
      {
        Object defVal = param.get(key);
        if(StringOper.okStrNull(defVal) == null)
          defVal = param.get(radiceNomeParametri + key);
        if(StringOper.okStrNull(defVal) != null)
          cd.setValueAscii(newObj, defVal.toString());
      }
    }
  }
}
