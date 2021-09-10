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
package org.rigel2.glue.table;

import java.util.Map;
import org.commonlib.utils.StringOper;
import org.rigel2.HtmlUtils;
import org.rigel2.RigelCustomUrlBuilder;
import org.rigel2.RigelI18nInterface;
import org.rigel2.SetupHolder;
import org.rigel2.glue.custom.CustomButtonFactory;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.RigelTableModel;
import org.rigel2.table.html.wrapper.CustomButtonInfo;
import org.rigel2.table.html.wrapper.CustomButtonRuntimeInterface;
import org.rigel2.table.html.wrapper.HtmlWrapperBase;

/**
 * Questa classe costruisce le uri della colonna marcata
 * con caratteristiche. Viene utilizzata in: HeditTableApp (lista
 * con edit dei campi), AlternateColorTableAppBase (lista semplice
 * senza edit).
 *
 * @author Nicola De Nisco
 */
public class CaratteristicheHtmlBuilder
{
  protected HtmlWrapperBase wl = null;
  protected RigelTableModel tableModel = null;
  protected RigelCustomUrlBuilder urlBuilder = null;
  protected RigelI18nInterface i18n = null;
  protected boolean editPopup = false;
  protected boolean popup = false;
  protected String popupEditFunction = null;
  protected String lineEditUrl, lineEditScript;
  protected Map<String, String> extraParams;

  public CaratteristicheHtmlBuilder(HtmlWrapperBase wl, RigelTableModel tableModel,
     RigelCustomUrlBuilder urlBuilder, String popupEditFunction,
     boolean editPopup, boolean popup, Map<String, String> extraParams)
  {
    this.wl = wl;
    this.tableModel = tableModel;
    this.urlBuilder = urlBuilder;
    this.editPopup = editPopup;
    this.popup = popup;
    this.popupEditFunction = popupEditFunction;
    this.extraParams = extraParams;
    this.i18n = wl.getTbl().getI18n();
  }

  public String getHtmlCarSelezione(RigelColumnDescriptor cd, int row, int col)
     throws Exception
  {
    String url = wl.makeForeignServerInfo(row, col);

    url = urlBuilder.buildUrlSelezionaRecord(popup, url, tableModel,
       cd, cd.getName(), row, extraParams);

    return "<a href=\"" + url + "\">"
       + SetupHolder.getImgSelItem()
       + "</a>&nbsp;";
  }

  public String getHtmlCarEdit(RigelColumnDescriptor cd, int row, int col)
     throws Exception
  {
    String url = wl.makeUrlEditRiga(row, col);

    if(wl.isEditRigaJavascript())
    {
      // la funzionalita' di edit viene assicurata
      // da una funzione javascritp; non ci curiamo
      // di altro, basta chiamare la funzione
      return "<a href=\"" + url + "\">" + SetupHolder.getImgEditItem() + "</a>&nbsp;";
    }
    else
    {
      // costruisce la url classica di edit
      url = urlBuilder.buildUrlEditRecord(editPopup, url, tableModel,
         cd, cd.getName(), row, extraParams);

      if(HtmlUtils.isJavascriptBegin(url))
      {
        // chiama funzione javascript
        return "<a href=\"" + url + "\">" + SetupHolder.getImgEditItem() + "</a>&nbsp;";
      }
      else
      {
        if(editPopup || wl.getEdInfo().getPopupMode() != 0)
        {
          String nomeWin = "edit_" + wl.getNome() + "_" + cd.getName();
          return "<a href=\"javascript:" + popupEditFunction + "('" + url + "', '" + nomeWin.replace(' ', '_') + "')\">"
             + SetupHolder.getImgEditItem()
             + "</a>&nbsp;";
        }
        else
        {
          return "<a href=\"" + url + "\">" + SetupHolder.getImgEditItem() + "</a>&nbsp;";
        }
      }
    }
  }

  public String getHtmlCarCBut(RigelColumnDescriptor cd, int row, int col)
     throws Exception
  {
    String sOut = "";
    lineEditScript = lineEditUrl = null;

    for(int i = 0; i < cd.getNumCustomButtons(); i++)
    {
      CustomButtonInfo cb = cd.getCustomButton(i);

      if(cb.haveClassName() && cb.getCbri() == null)
      {
        // istanzia oggetto custom di controllo del bottone (solo la prima volta)
        cb.setCbri(CustomButtonFactory.getInstance().getCustomButton(cb.getClassName()));
      }

      CustomButtonRuntimeInterface cbri;
      if((cbri = cb.getCbri()) != null)
      {
        cb = cbri.setRowData(cb, tableModel, i18n, row, col);

        // se non visibile per questa riga salta a successivo
        if(!cbri.isVisible())
          continue;
      }

      String text = StringOper.okStr(cb.getText());
      String html = StringOper.okStrNull(cb.getHtml());

      if(cbri == null || cbri.isEnabled())
      {
        String url = null;
        if(cb.haveJavascript())
        {
          String script = cb.makeJavascript((RigelTableModel) (tableModel), row);
          url = "javascript:" + script;

          if(cb.isLineEdit() && (lineEditScript == null && lineEditUrl == null))
            lineEditScript = script;
        }
        else
        {
          url = urlBuilder.buildUrlCustomButton(popup || (cb.getPopup() > 0),
             (RigelTableModel) (tableModel), cd, cd.getName(), row, cb);

          if(cb.isLineEdit() && (lineEditScript == null && lineEditUrl == null))
            lineEditUrl = url;
        }

        // modifica url in base alle opzioni del custom button
        if(!HtmlUtils.isJavascriptBegin(url))
        {
          if(cb.getPopup() > 0)
          {
            url = "javascript:apriPopup" + cb.getPopup() + "('"
               + url + "', '" + StringOper.purge(text) + "')";
          }
          else
          {
            // url semplice: in questo caso possiamo chiedere conferma all'utente se previsto
            if(cb.haveConfirm())
            {
              String confirm = cb.makeConfirmMessage((RigelTableModel) (tableModel), row, col);
              url = "javascript:confermaCB('" + confirm + "', '" + url + "')";
            }
          }
        }

        if(html == null)
        {
          // recupera icona del custom button
          String icon = urlBuilder.buildImageCustomButton(popup,
             (RigelTableModel) (tableModel), cd, cd.getName(), row, cb);

          if(url == null)
            sOut += icon + "&nbsp;";
          else
            sOut += "<a href=\"" + url + "\">" + icon + "</a>&nbsp;";
        }
        else
        {
          // risolve eventuali macro nell'html
          html = cb.makeHtmlCustom(tableModel, row, col);

          if(url == null)
            sOut += html + "&nbsp;";
          else
            sOut += "<a href=\"" + url + "\">" + html + "</a>&nbsp;";
        }
      }
      else
      {
        // bottone disattivato: viene visualizzato ma senza iperlink
        if(html == null)
        {
          // recupera icona del custom button
          String icon = urlBuilder.buildImageCustomButton(popup,
             (RigelTableModel) (tableModel), cd, cd.getName(), row, cb);

          sOut += icon;
        }
        else
        {
          // risolve eventuali macro nell'html
          html = cb.makeHtmlCustom(tableModel, row, col);

          sOut += html;
        }
      }
    }

    return sOut;
  }

  public String getHtmlCarCancella(RigelColumnDescriptor cd, int row, int col)
     throws Exception
  {
    // recupera una stringa rappresentazione della chiave primaria
    String sKey = ((RigelTableModel) (tableModel)).createQueryKey(row);
    if(sKey != null)
    {
      String url = urlBuilder.buildUrlCancellaRecord(popup, sKey, tableModel,
         cd, cd.getName(), row, extraParams);

      return "<a href=\"" + url + "\">"
         + SetupHolder.getImgDeleteItem()
         + "</a>&nbsp;";
    }

    return "&nbsp;";
  }

  public String getLineEditUrl()
  {
    return lineEditUrl;
  }

  public String getLineEditScript()
  {
    return lineEditScript;
  }

  public Map<String, String> getExtraParams()
  {
    return extraParams;
  }

  public void setExtraParams(Map<String, String> extraParams)
  {
    this.extraParams = extraParams;
  }
}
