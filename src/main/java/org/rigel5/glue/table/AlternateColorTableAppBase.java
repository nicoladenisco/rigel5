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

import java.util.HashMap;
import org.rigel5.HtmlUtils;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.wrapper.HtmlWrapperBase;

/**
 * Tabella a righe alternate con le funzionalita'
 * delle colonne 'caratteristiche'.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class AlternateColorTableAppBase extends AlternateColorTableAppGeneric
{
  protected HtmlWrapperBase wl = null;
  protected boolean authSel = true;
  protected boolean authEdit = true;
  protected boolean authCustom = true;
  protected boolean authDelete = true;
  protected int selezioneColumn = -1;
  protected int editColumn = -1;
  protected String popupEditFunction = "apriFinestraEdit";
  protected CaratteristicheHtmlBuilder cub = null;
  protected HashMap<Integer, String> mapHtmlCustomButton = new HashMap<Integer, String>();

  public AlternateColorTableAppBase()
  {
    editPopup = popup;
  }

  public void init(HtmlWrapperBase wl)
     throws Exception
  {
    this.wl = wl;
    setSelEditColumn();
  }

  /**
   * Verifica se esiste una colonna selezione e modifica in modo da attivare
   * la selezione o la modifica di riga per tutti i campi della riga
   */
  protected void setSelEditColumn()
  {
    editColumn = -1;
    selezioneColumn = -1;
    RigelTableModel rtm = wl.getPtm();
    for(int i = 0; i < rtm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = rtm.getColumn(i);
      if(authSel && cd.isCaratteristicheSelezioneRiga())
      {
        selezioneColumn = i;
      }
      if(authEdit && cd.isCaratteristicheEditRiga())
      {
        editColumn = i;
      }
    }
  }

  /**
   * Identica a quella di htable, ma chiama clearLineLink()
   * per ogni linea, in modo da azzerare il link di linea.
   * @param rStart
   * @param numRec
   * @throws Exception
   */
  @Override
  public synchronized void doRows(int rStart, int numRec)
     throws Exception
  {
    if(rStart > tableModel.getRowCount())
      return;
    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    strartRow = rStart;
    numRows = numRec;
    numCols = tableModel.getColumnCount();

    while(numRec-- > 0)
    {
      clearLineLink();
      doRow(rStart++);
    }
  }

  /**
   * Ridefinita; determina se attivare il linelink e che
   * funzione associargli. Se e' attiva la selezione verra'
   * utilizzato per la selezione. Se la selezione non e'
   * attiva, ma e' attivo l'edit, verra' utilizzato per l'edit.
   * @param row
   * @throws Exception
   */
  @Override
  public void doRow(int row)
     throws Exception
  {
    makeHtmlCustomButtons(row);

    if(!haveLineLink())
    {
      // eventuale custom button promosso a line edit
      if(cub != null && cub.getLineEditScript() != null)
        setLineJavascipt(cub.getLineEditScript());
      else if(cub != null && cub.getLineEditUrl() != null)
        setLineLink(cub.getLineEditUrl());
      else
      {
        // se non e' stato impostato il link di linea
        // cerca di impostarlo prima alla selezione (se presente)
        // altrimenti alla modifica del record(se presente)
        if(authSel && selezioneColumn != -1)
        {
          setLineLinkSelezione(row);
        }
        else
        {
          if(authEdit && editColumn != -1)
          {
            setLineLinkEdit(row);
          }
        }
      }
    }

    super.doRow(row);
  }

  /**
   * Funzione destinata alla ridefinizione in classi derivate.
   * Consente di impostare direttamente un javascript associato
   * al linelink, bypassando la selezione automatica effettuata
   * da doRow().
   * @param row
   * @param javascript
   * @throws Exception
   */
  public void doRowLineJavascript(int row, String javascript)
     throws Exception
  {
    setLineJavascipt(javascript);
    super.doRow(row);
  }

  /**
   * Funzione destinata alla ridefinizione in classi derivate.
   * Consente di impostare direttamente un link associato
   * al linelink, bypassando la selezione automatica effettuata
   * da doRow().
   * @param row
   * @param url
   * @throws Exception
   */
  public void doRowLineLink(int row, String url)
     throws Exception
  {
    setLineLink(url);
    super.doRow(row);
  }

  /**
   * Ritorna il testo di una colonna header.
   * A differenza della versione base supporta
   * il link per effettuare l'ordinamento cliccando
   * sul testo del nome colonna.
   * @param row
   * @param col
   * @return
   * @throws Exception
   */
  @Override
  public String doFormCellHeader(int row, int col)
     throws Exception
  {
    String colText = getColumnCaption(col);

    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return colText;

    if(cd.isEscludiRicerca())
      return colText;

    int filtroSort = cd.getFiltroSort();

    if(filtroSort > 0 && filtroSort < 1000)
      colText = "[\u2191]" + colText;
    if(filtroSort >= 1000 && filtroSort < 2000)
      colText = "[\u2193]" + colText;

    return "<a onclick=\"rigel.simpleSort('" + formName + "', '" + (col + 1) + "')\">" + colText + "</a>";

//       cd.isEscludiRicerca() ? colText
//              : HtmlUtils.makeHrefJScript("SimpleSort_" + formName + "('" + (col + 1) + "')",
//          colText, "txt-yellow-bold-12-nul");
  }

  /**
   * Ritorna il testo ci una colonna cella.
   * Se attive le funzioni 'caratteristiche' e la colonna
   * le ha attivate, visualizza le icone con i relativi link.
   * @param row
   * @param col
   * @return
   * @throws Exception
   */
  @Override
  public String doCellHtml(int row, int col, String cellText)
     throws Exception
  {
    String sOut = super.doCellHtml(row, col, cellText);
    RigelColumnDescriptor cd = getCD(col);

    String hcb;
    if(cd.isCaratteristiche() && (hcb = mapHtmlCustomButton.get(col)) != null)
      sOut += hcb;

    return sOut;
  }

  /**
   * Costruisce html per le colonne caratteristiche.
   * L'HTML dei pulsanti caratteristiche deve essere
   * elaborato prima dell'html della riga. In questa funzione
   * viene costruito l'html di tutti i pulsanti e salvati
   * in una map per essere utilizzati nella giusta colonna.
   * @param row
   * @throws Exception
   */
  protected void makeHtmlCustomButtons(int row)
     throws Exception
  {
    mapHtmlCustomButton.clear();
    editColumn = selezioneColumn = -1;
    RigelTableModel rtm = (RigelTableModel) tableModel;

    for(int col = 0; col < rtm.getColumnCount(); col++)
    {
      RigelColumnDescriptor cd = rtm.getColumn(col);

      if(cd.isCaratteristiche())
      {
        // istanzia il costruttore di html per colonne caratteristiche
        if(cub == null)
        {
          cub = new CaratteristicheHtmlBuilder(wl,
             rtm, urlBuilder, popupEditFunction, editPopup, popup, extraParamsUrls);
        }

        StringBuilder cbHtml = new StringBuilder(512);

        if(authSel && cd.isCaratteristicheSelezioneRiga())
        {
          cbHtml.append(cub.getHtmlCarSelezione(cd, row, col));
          selezioneColumn = col;
        }

        if(authEdit && cd.isCaratteristicheEditRiga())
        {
          cbHtml.append(cub.getHtmlCarEdit(cd, row, col));
          editColumn = col;
        }

        if(authCustom && cd.isCaratteristicheCBut())
        {
          cbHtml.append(cub.getHtmlCarCBut(cd, row, col));
        }

        if(authDelete && cd.isCaratteristicheCancellaRiga())
        {
          cbHtml.append(cub.getHtmlCarCancella(cd, row, col));
        }

        mapHtmlCustomButton.put(col, cbHtml.toString());
      }
    }
  }

  public void setLineLinkSelezione(int row)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(selezioneColumn);
    String inputUrl = wl.makeForeignServerInfo(row, selezioneColumn);

    // costruisce la url classica di edit
    String url = urlBuilder.buildUrlLineSelezione(editPopup, inputUrl, (RigelTableModel) getModel(),
       cd, cd.getName(), row, extraParamsUrls);

    if(HtmlUtils.isJavascriptBegin(url))
      setLineJavascipt(url.substring(HtmlUtils.JAVASCRIPT_BEGIN.length()));
    else
      setLineLink(url);
  }

  public void setLineLinkEdit(int row)
     throws Exception
  {
    String url = wl.makeUrlEditRiga(row, editColumn);
    RigelColumnDescriptor cd = getCD(editColumn);

    if(wl.isEditRigaJavascript())
    {
      // la funzionalita' di edit viene assicurata
      // da una funzione javascritp; non ci curiamo
      // di altro, basta chiamare la funzione
      setLineJavascipt(url.substring(HtmlUtils.JAVASCRIPT_BEGIN.length()));
    }
    else
    {
      // costruisce la url classica di edit
      url = urlBuilder.buildUrlLineEdit(editPopup, url, (RigelTableModel) getModel(),
         cd, cd.getName(), row, extraParamsUrls);

      if(HtmlUtils.isJavascriptBegin(url))
      {
        // la funzionalita' di edit viene assicurata
        // da una funzione javascritp
        setLineJavascipt(url.substring(HtmlUtils.JAVASCRIPT_BEGIN.length()));
      }
      else
      {
        if(editPopup || wl.getEdInfo().getPopupMode() != 0)
        {
          String nomeWin = "edit_" + wl.getNome() + "_" + cd.getName();
          setLineJavascipt(popupEditFunction + "('" + url + "', '" + nomeWin.replace(' ', '_') + "')");
        }
        else
        {
          setLineLink(url);
        }
      }
    }
  }

  @Override
  public void setLineLink(String uri)
  {
    linkColStat = "goLink('" + uri + "');";
  }

  public boolean isAuthCustom()
  {
    return authCustom;
  }

  public boolean isAuthDelete()
  {
    return authDelete;
  }

  public boolean isAuthEdit()
  {
    return authEdit;
  }

  public boolean isAuthSel()
  {
    return authSel;
  }

  public void setAuthCustom(boolean authCustom)
  {
    this.authCustom = authCustom;
  }

  public void setAuthDelete(boolean authDelete)
  {
    this.authDelete = authDelete;
  }

  public void setAuthEdit(boolean authEdit)
  {
    this.authEdit = authEdit;
    setSelEditColumn();
  }

  public void setAuthSel(boolean authSel)
  {
    this.authSel = authSel;
    setSelEditColumn();
  }

  public String getPopupEditFunction()
  {
    return popupEditFunction;
  }

  public void setPopupEditFunction(String popupEditFunction)
  {
    this.popupEditFunction = popupEditFunction;
    cub = null;
  }
}
