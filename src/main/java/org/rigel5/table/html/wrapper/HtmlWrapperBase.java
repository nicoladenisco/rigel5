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
package org.rigel5.table.html.wrapper;

import java.util.*;
import javax.servlet.http.*;
import org.commonlib5.utils.*;
import org.rigel5.HtmlUtils;
import org.rigel5.RigelUIManager;
import org.rigel5.SetupHolder;
import org.rigel5.exceptions.MissingColumnException;
import org.rigel5.exceptions.MissingParameterException;
import org.rigel5.table.*;
import org.rigel5.table.html.AbstractHtmlTablePager;
import org.rigel5.table.html.RigelHtmlPage;
import org.rigel5.table.html.hTable;

/**
 * <p>
 * Classe base di tutti i Wrapper HTML.</p>
 * <p>
 * I wrapper sono delle strutture molto potenti che
 * contengono al loro interno piu' oggetti per la gestione automatizzata
 * delle liste e dei forms; Il metotdo getHtmlForm() consente di recuperare
 * l'HTML completo per la visualizzazione o per l'input di una form.</p>
 * <p>
 * Ad esempio PeerWrapperListaHtml contiene al suo interno<br>
 * un oggetto ...peer.html.PeerTableModel per la produzione dei dati,<br>
 * un oggetto ...hTable per visualizzare i dati,<br>
 * un oggetto ...PeerListaElem per gestirne la paginazione.
 * Il coordinamento fra questi oggetti e' gestito in automatico
 * del Wrapper.</p>
 * <p>
 * Vedi le classi derivate per maggiori dettagli.</p>
 */
abstract public class HtmlWrapperBase extends WrapperBase
{
  protected hTable tbl = null;
  protected RigelUIManager uim = SetupHolder.getUiManager();

  public RigelUIManager getUim()
  {
    return uim;
  }

  public void setUim(RigelUIManager uim)
  {
    this.uim = uim;
    if(pager != null && pager instanceof AbstractHtmlTablePager)
      ((AbstractHtmlTablePager) pager).setUim(uim);
  }

  /////////////////////////////////////////////////////////////////
  /**
   * Salva i dati del form nell'oggetto peer associato.
   * @param params
   * @param sessione
   * @throws Exception
   */
  @Override
  abstract public void salvaDatiForm(Map params, HttpSession sessione)
     throws Exception;

  /**
   * Recupera l'HTML del form.
   * @param params
   * @param sessione
   * @return
   * @throws Exception
   */
  abstract public String getHtmlForm(Map params, HttpSession sessione)
     throws Exception;

  /**
   * Produce l'HTML della pager ordinaria.
   * Introduce una riga con la ricerca semplice.
   * @param params
   * @param sessione
   * @return
   * @throws Exception
   */
  abstract public String getHtmlLista(Map params, HttpSession sessione)
     throws Exception;

  /**
   * Produce l'HTML della pager per palmare.
   * L'HTML prodotto e' piu' compatto, adatto alla visualizzazione
   * su un palmare o comunque una device con schermo ridotto.
   * @param params
   * @param sessione
   * @return
   * @throws Exception
   */
  abstract public String getHtmlListaPalmare(Map params, HttpSession sessione)
     throws Exception;

  /**
   * Produce HTML della pagina.
   * @param params
   * @param sessione
   * @param page
   * @throws Exception
   */
  abstract public void getHtml(Map params, HttpSession sessione, RigelHtmlPage page)
     throws Exception;

  /////////////////////////////////////////////////////////////////
  public void setTbl(hTable tbl)
  {
    this.tbl = tbl;
  }

  public hTable getTbl()
  {
    return tbl;
  }

  //////////////////////////////////////////////////////////////////
  /**
   * Per una determinata riga (record del database) produce le informazioni
   * che verranno passate alle maschere chiamanti nel momento che si attiva
   * l'icona di selezione.
   * @param row indice del record
   * @param col indice di colonna (ignorato)
   * @return la striga con i dati da passare nella forma 'codice','descrizione'
   * @throws Exception
   */
  public String makeForeignServerInfo(int row, int col)
     throws Exception
  {
    if(foInfo == null || foInfo.isEmpty())
      return null;

    StringJoin sj = StringJoin.build(",", "\'");
    for(Pair<String, String> p : foInfo.getForeignColumns())
    {
      String nomec = p.first;
      String colonna = p.second;

      String[] scol = colonna.split(",");
      switch(scol.length)
      {
        case 0:
          break;

        case 1:
        {
          RigelColumnDescriptor cd = ptm.getColumn(colonna);
          if(cd == null)
            throw new MissingColumnException("Foreign-server: colonna " + colonna + " (" + nomec + ") non presente!");

          Object valObj = ptm.getRowRecord(row);
          String valStr = cd.getValueAsString(valObj);

          sj.add(StringOper.CvtJavascriptString(valStr));
          break;
        }

        default:
        {
          StringJoin valstr = StringJoin.build(" ");
          for(String colnome : scol)
          {
            RigelColumnDescriptor cd = ptm.getColumn(colnome);
            if(cd == null)
              throw new MissingColumnException("Foreign-server: colonna " + colnome + " (" + nomec + ") non presente!");

            Object valObj = ptm.getRowRecord(row);
            valstr.add(cd.getValueAsString(valObj));
          }
          if(!valstr.isEmpty())
            sj.add(valstr.join());
          break;
        }
      }
    }

    return sj.isEmpty() ? null : sj.join();
  }

  public boolean haveEditRiga()
  {
    return edInfo != null && edInfo.haveEditRiga();
  }

  public boolean isEditRigaJavascript()
  {
    return haveEditRiga() && edInfo.isEditRigaJavascript();
  }

  public String getUrlEditRiga()
  {
    return edInfo == null ? null : StringOper.okStrNull(edInfo.getUrlEditRiga());
  }

  /**
   * Per una determinata riga (record del database) produce le informazioni
   * per consentire l'editing del record da parte di un form.
   * La url (urlEditRiga) puo' contentere una macro nella forma
   * javascript:miaFunzione(@@@)
   * dove @@@ verranno sostituiti con i valori dei parametri
   * separati da virgola.
   * @param row indice del record
   * @param col indice di colonna (ignorato)
   * @return la stringa con l'url completa
   * @throws Exception
   */
  public String makeUrlEditRiga(int row, int col)
     throws Exception
  {
    if(edInfo == null || !edInfo.haveEditRiga())
      throw new MissingParameterException("Direttiva edit-info mancante o non corretta.");

    if(!edInfo.haveEditParam())
      throw new MissingParameterException("Nessun parametro di link per effetuare l'editing del record.");

    if(edInfo.isEditRigaJavascript())
      return makeUrlEditRigaJavascript(row, col);
    else
      return makeUrlEditRigaLink(row, col);
  }

  protected String makeUrlEditRigaLink(int row, int col)
     throws Exception
  {
    String url = edInfo.getUrlEditRiga();
    Enumeration enumParam = edInfo.getEnumParamsKey();
    while(enumParam.hasMoreElements())
    {
      String nomec = (String) enumParam.nextElement();
      String valore = edInfo.getParam(nomec);

      if(valore.startsWith("#"))
      {
        // aggancio dinamico a valore di colonna
        RigelColumnDescriptor cd = ptm.getColumn(valore.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + valore + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        valore = cd.getValueAsString(valObj);
      }
      else if(valore.startsWith("@"))
      {
        // aggancio dinamico a valore di campo tabella
        RigelColumnDescriptor cd = ptm.getColumnByName(valore.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + valore + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        valore = cd.getValueAsString(valObj);
      }

      url = HtmlUtils.mergeUrl(url, nomec, valore);
    }

    return url;
  }

  protected String makeUrlEditRigaJavascript(int row, int col)
     throws Exception
  {
    // se non c'e' la macro di valore ritorna semplicemente l'url
    String urlEditRiga = edInfo.getUrlEditRiga();
    if(!urlEditRiga.contains(VAR_MACRO))
      return urlEditRiga;

    String param = "";
    Enumeration enumParam = edInfo.getEnumParamsKey();
    while(enumParam.hasMoreElements())
    {
      String panome = (String) enumParam.nextElement();
      String valore = edInfo.getParam(panome);

      if(valore.startsWith("#"))
      {
        // aggancio dinamico a caption di colonna
        RigelColumnDescriptor cd = ptm.getColumn(valore.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + valore + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        String valCol = cd.getValueAsString(valObj);
        valore = cd.isNumeric() ? valCol : "'" + valCol + "'";
      }
      else if(valore.startsWith("@"))
      {
        // aggancio dinamico a valore di campo tabella
        RigelColumnDescriptor cd = ptm.getColumnByName(valore.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + valore + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        String valCol = cd.getValueAsString(valObj);
        valore = cd.isNumeric() ? valCol : "'" + valCol + "'";
      }

      param += "," + valore;
    }

    if(param.length() > 0 && param.charAt(0) == ',')
      param = param.substring(1);

    return StringOper.strReplace(urlEditRiga, VAR_MACRO, param);
  }

  /**
   * Costruisce dei campi invisibili con i dati di chiamata.
   * Viene utilizzata da ????WrapperFormHtml per consentire la
   * post del form recuperando i parametri utilizzati per la
   * selezione del record da modificare.
   * @param row indice di riga (di solito 0: ignorato)
   * @return la stringa HTML con i campi hidden da inserire nel form
   * @throws Exception
   */
  public String makeHiddenEditParametri(int row)
     throws Exception
  {
    Map<String, String> pmap = getEditInfoParametri(row);
    StringBuilder rv = new StringBuilder();

    for(Map.Entry<String, String> entry : pmap.entrySet())
    {
      String campo = entry.getKey();
      String valore = entry.getValue();

      rv.append("<input type=\"hidden\" name=\"").append(HtmlUtils.encodeURI(campo))
         .append("\" value=\"").append(HtmlUtils.encodeURI(valore)).append("\">\r\n");
    }

    return rv.toString();
  }

  /**
   * Recupero dati di chiamata per edit record.
   * Produce una mappa campo/valore per identificare univocamente il record
   * utilizzando le indicazioni della sezione [edit-info].
   * @param row indice di riga (di solito 0: ignorato)
   * @return mappa chiave/valore per il record indicato
   * @throws Exception
   */
  public Map<String, String> getEditInfoParametri(int row)
     throws Exception
  {
    ArrayMap<String, String> rv = new ArrayMap<>();
    Enumeration enumParam = edInfo.getEnumParamsKey();
    while(enumParam.hasMoreElements())
    {
      String nomec = (String) enumParam.nextElement();
      String valore = edInfo.getParam(nomec);

      if(valore.startsWith("#"))
      {
        // aggancio dinamico a valore di colonna
        RigelColumnDescriptor cd = ptm.getColumn(valore.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + valore + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        valore = cd.getValueAsString(valObj);
      }
      else if(valore.startsWith("@"))
      {
        // aggancio dinamico a valore di campo tabella
        RigelColumnDescriptor cd = ptm.getColumnByName(valore.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + valore + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        valore = cd.getValueAsString(valObj);
      }

      rv.put(nomec, valore);
    }

    return rv;
  }
}
