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
import java.util.Map;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.hEditTable;
import org.rigel5.table.html.wrapper.HtmlWrapperBase;

/**
 * Versione applicazione di una generica tabella di editing multiriga.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class HeditTableApp extends hEditTable
{
  private HtmlWrapperBase wl = null;
  protected boolean authSel = true;
  protected boolean authEdit = true;
  protected boolean authCustom = true;
  protected boolean authDelete = true;
  protected String popupEditFunction = "apriFinestraEdit";
  protected int selezioneColumn = -1;
  protected int editColumn = -1;
  protected CaratteristicheHtmlBuilder cub = null;
  protected HashMap<Integer, String> mapHtmlCustomButton = new HashMap<Integer, String>();

  public HeditTableApp()
  {
    super(null);
  }

  public HeditTableApp(String sID, HtmlWrapperBase we)
  {
    super(sID);
    init(sID, we);
  }

  public void init(String sID, HtmlWrapperBase we)
  {
    this.id = sID;
    this.wl = we;
    editPopup = popup;
  }

  @Override
  public void doRow(int row)
     throws Exception
  {
    makeHtmlCustomButtons(row);
    super.doRow(row);
  }

  /**
   * Ritorna il testo ci una colonna cella.
   * Se attive le funzioni 'caratteristiche' e la colonna
   * le ha attivate, visualizza le icone con i relativi link.
   * NOTA: questa funzione e' una copia della sua gemella in
   * AlternateColorTableBase. Far riferimento a quella classe
   * per la manutenzione.
   * @param row
   * @param col
   * @return
   * @throws Exception
   */
  @Override
  public String doCellHtml(int row, int col, String cellText)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(col);

    if(cd.isCaratteristiche())
    {
      String hcb = mapHtmlCustomButton.get(col);
      return (hcb != null) ? hcb : "&nbsp;";
    }

    return super.doCellHtml(row, col, cellText);
  }

  /**
   * Costruisce butHtml per le colonne caratteristiche.
   * L'HTML dei pulsanti caratteristiche deve essere
   * elaborato prima dell'butHtml della riga. In questa funzione
   * viene costruito l'butHtml di tutti i pulsanti e salvati
   * in una map per essere utilizzati nella giusta colonna.
   * @param row
   * @throws Exception
   */
  protected void makeHtmlCustomButtons(int row)
     throws Exception
  {
    mapHtmlCustomButton.clear();
    editColumn = selezioneColumn = -1;
    RigelTableModel rtm = wl.getPtm();

    for(int col = 0; col < rtm.getColumnCount(); col++)
    {
      RigelColumnDescriptor cd = rtm.getColumn(col);

      if(cd.isCaratteristiche())
      {
        // istanzia il costruttore di butHtml per colonne caratteristiche
        if(cub == null)
        {
          cub = new CaratteristicheHtmlBuilder(wl,
             rtm, urlBuilder, popupEditFunction, editPopup, popup, extraParamsUrls);
        }

        StringBuilder butHtml = new StringBuilder(512);

        if(authSel && cd.isCaratteristicheSelezioneRiga())
        {
          butHtml.append(cub.getHtmlCarSelezione(cd, row, col));
          selezioneColumn = col;
        }

        if(authEdit && cd.isCaratteristicheEditRiga())
        {
          butHtml.append(cub.getHtmlCarEdit(cd, row, col));
          editColumn = col;
        }

        if(authCustom && cd.isCaratteristicheCBut())
        {
          butHtml.append(cub.getHtmlCarCBut(cd, row, col));
        }

        if(authDelete && cd.isCaratteristicheCancellaRiga())
        {
          butHtml.append(cub.getHtmlCarCancella(cd, row, col));
        }

        mapHtmlCustomButton.put(col, butHtml.toString());
      }
    }
  }

  public boolean isAuthCustom()
  {
    return authCustom;
  }

  public void setAuthCustom(boolean authCustom)
  {
    this.authCustom = authCustom;
  }

  public boolean isAuthDelete()
  {
    return authDelete;
  }

  public void setAuthDelete(boolean authDelete)
  {
    this.authDelete = authDelete;
  }

  public boolean isAuthEdit()
  {
    return authEdit;
  }

  public void setAuthEdit(boolean authEdit)
  {
    this.authEdit = authEdit;
  }

  public boolean isAuthSel()
  {
    return authSel;
  }

  public void setAuthSel(boolean authSel)
  {
    this.authSel = authSel;
  }

  public String getPopupEditFunction()
  {
    return popupEditFunction;
  }

  public void setPopupEditFunction(String popupEditFunction)
  {
    this.popupEditFunction = popupEditFunction;
  }

  public HtmlWrapperBase getWl()
  {
    return wl;
  }

  public void setWl(HtmlWrapperBase wl)
  {
    this.wl = wl;
  }

  @Override
  public void setExtraParamsUrls(Map<String, String> extraParamsUrls)
  {
    super.setExtraParamsUrls(extraParamsUrls);
    if(cub != null)
      cub.setExtraParams(extraParamsUrls);
  }
}
