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
package org.rigel5;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.commonlib5.utils.StringOper;

/**
 * Funzioni statiche di utilita' generale per la
 * costruzione di HTML.
 *
 * @author Nicola De Nisco
 */
public class HtmlUtils
{
  public static final String JAVASCRIPT_BEGIN = "javascript:";

  public static boolean isJavascriptBegin(String url)
  {
    return url != null
       && url.toLowerCase().startsWith(JAVASCRIPT_BEGIN);
  }

  public static String makeHref(String where, String link)
  {
    return "<a href=\"" + encodeURI(where) + "\">" + link + "</a>";
  }

  public static String makeHref(String where, String link, String cls)
  {
    return "<a href=\"" + encodeURI(where) + "\" class=\"" + cls + "\">" + link + "</a>";
  }

  public static String makeHref(String where, String[] param, String link)
  {
    return makeHref(where, param, link, null);
  }

  /**
   * Costruisce un tag di link a risorsa.
   * Costruisce il tag con una serie di parametri e opzioni.
   * @param where url a cui puntare
   * @param param array di parametri (chiave=valore)
   * @param link html del link (parte visualizzata)
   * @param cls classe da usare per il tag a (può essere null)
   * @return HTML del tag
   */
  public static String makeHref(String where, String[] param, String link, String cls)
  {
    int pos;
    String ref = encodeURI(where);
    for(int i = 0; i < param.length; i++)
    {
      String sp = param[i];
      if((pos = sp.indexOf('=')) != -1)
      {
        String nomeParam = sp.substring(0, pos);
        String valueParam = sp.substring(pos + 1);
        ref += mergeUrl(ref, nomeParam, valueParam);
      }
    }

    String sOut = "<a href=\"" + ref;

    if(cls == null)
      sOut += "\">" + link + "</a>";
    else
      sOut += "\" class=\"" + cls + "\">" + link + "</a>";

    return sOut;
  }

  /**
   * Costruisce un tag di link a risorsa.
   * Costruisce il tag con una serie di parametri e opzioni.
   * @param where url a cui puntare
   * @param param mappa di parametri (nome -> valore)
   * @param link html del link
   * @param cls classe da usare per il tag a (può essere null)
   * @return HTML del tag
   */
  public static String makeHref(String where, Map<String, String> param, String link, String cls)
  {
    String sOut = "<a href=\"" + mergeUrl(encodeURI(where), param);

    if(cls == null)
      sOut += "\">" + link + "</a>";
    else
      sOut += "\" class=\"" + cls + "\">" + link + "</a>";

    return sOut;
  }

  public static String makeHrefNoenc(String where, String link)
  {
    return "<a href=\"" + where + "\">" + link + "</a>";
  }

  public static String makeHrefNoenc(String where, String link, String cls)
  {
    return "<a href=\"" + where + "\" class=\"" + cls + "\">" + link + "</a>";
  }

  public static String makeHrefJScript(String script, String link)
  {
    return "<a href=\"javascript:" + script + "\">" + link + "</a>";
  }

  public static String makeHrefJScript(String script, String link, String cls)
  {
    return "<a href=\"javascript:" + script + "\" class=\"" + cls + "\">" + link + "</a>";
  }

  /**
   * Ritorna vero se la url specificata fa riferimento
   * ad una risorsa di tipo HTTP.
   * @param url url da analizzare
   * @return vero se http, https
   */
  public static boolean isHttp(String url)
  {
    return url != null && (url.startsWith("http:") || url.startsWith("https:"));
  }

  /**
   * Fonde una url e un parametro applicando i controlli
   * sui caratteri di concatenamento.
   * I parametri e i loro valori vengono fusi alla URL utilizzando
   * la convenzione di encoding delle URL, ovvero qualsiasi carattere
   * sia contenuto nel nome o nel valore viene correttamente codificato
   * affinchè sia decodificato dal server.
   * @param url dove fondere il parametro
   * @param paramName nome del parametro
   * @param paramValue valore del parametro
   * @return
   */
  public static String mergeUrl(String url, String paramName, String paramValue)
  {
    if(url == null)
      return null;

    if(!StringOper.isOkStr(paramName) || !StringOper.isOkStr(paramValue))
      return url;

    if(paramValue.startsWith("&"))
      paramValue = paramValue.substring(1);

    return url + ((url.indexOf('?') == -1) ? '?' : '&') + encodeURI(paramName) + "=" + encodeURI(paramValue);
  }

  public static String mergeUrl(String url, String paramName, int paramValue)
  {
    return mergeUrl(url, paramName, String.valueOf(paramValue));
  }

  public static String mergeUrl(String url, String paramName, long paramValue)
  {
    return mergeUrl(url, paramName, String.valueOf(paramValue));
  }

  public static String mergeUrl(String url, String paramName, double paramValue)
  {
    return mergeUrl(url, paramName, String.valueOf(paramValue));
  }

  public static String mergeUrl(String url, String paramName, boolean paramValue)
  {
    return mergeUrl(url, paramName, String.valueOf(paramValue));
  }

  public static String mergeUrl(String url, Map<String, String> params)
  {
    if(url != null && params != null && !params.isEmpty())
    {
      for(Map.Entry<String, String> entry : params.entrySet())
      {
        String key = entry.getKey();
        String val = entry.getValue();
        url = mergeUrl(url, key, val);
      }
    }
    return url;
  }

  public static String mergeUrl(String url, Properties params)
  {
    if(url != null && params != null && !params.isEmpty())
    {
      for(Map.Entry<Object, Object> entry : params.entrySet())
      {
        String key = StringOper.okStrNull(entry.getKey());
        String val = StringOper.okStrNull(entry.getValue());
        if(key != null && val != null)
          url = mergeUrl(url, key, val);
      }
    }
    return url;
  }

  /**
   * Fonde i parametri alla url verificando che non ci siano duplicati.
   * @param url url orgine
   * @param params parametri da aggiungere
   * @return
   */
  public static String mergeUrlTestUnique(String url, Map<String, String> params)
  {
    if(params != null && !params.isEmpty())
    {
      for(Map.Entry<String, String> entry : params.entrySet())
      {
        String key = entry.getKey();
        String val = entry.getValue();

        if(!url.contains(key + "="))
          url = mergeUrl(url, key, val);
      }
    }
    return url;
  }

  public static String mergeUrlPair(String url, Object... params)
  {
    if((params.length & 1) != 0)
      throw new IllegalArgumentException();

    for(int i = 0; i < params.length; i += 2)
    {
      String key = StringOper.okStrNull(params[i]);
      String val = StringOper.okStrNull(params[i + 1]);
      if(key != null && val != null)
        url = mergeUrl(url, key, val);
    }

    return url;
  }

  public static String mergeUrlPairTestUnique(String url, Object... params)
  {
    if((params.length & 1) != 0)
      throw new IllegalArgumentException();

    for(int i = 0; i < params.length; i += 2)
    {
      String key = StringOper.okStrNull(params[i]);
      String val = StringOper.okStrNull(params[i + 1]);
      if(key != null && val != null && !url.contains(key + "="))
        url = mergeUrl(url, key, val);
    }

    return url;
  }

  /**
   * Effettua l'encoding di una stringa.
   * Tutti i caratteri non consentiti in una URL vengono opportunamente codificati.
   * @param uri stringa origine
   * @return stringa uri con encoding
   */
  public static String encodeURI(String uri)
  {
    try
    {
      return URLEncoder.encode(uri, "UTF-8");
    }
    catch(UnsupportedEncodingException ex)
    {
      return uri;
    }
  }

  public static String addSpanClass(String styleName, String inner)
  {
    return "<span class=\"" + styleName + "\">" + inner + "</span>";
  }

  public static String addSpanClasses(List<String> styleNames, String inner)
  {
    for(String s : styleNames)
      inner = addSpanClass(s, inner);

    return inner;
  }

  /**
   * Fonde stringhe path insieme evitando duplicati di separatore.
   * A differenza del file sistem qui il serparatore è sempre '/'.
   * @param path path origine
   * @param s nome di file o directory da aggiungere
   * @return la path con i separatori corretti
   */
  public static String mergePath(String path, String s)
  {
    boolean ep = path.endsWith("/");
    boolean ss = s.startsWith("/");

    if(ep && ss)
      return path + s.substring(1);
    else if(ep && !ss)
      return path + s;
    else if(!ep && ss)
      return path + s;
    else
      return path + "/" + s;
  }

  /**
   * Fonde stringhe path insieme evitando duplicati di separatore.
   * A differenza del file sistem qui il serparatore è sempre '/'.
   * @param path path origine
   * @param arComp array di nomi file da fondere
   * @return la path con i separatori corretti
   */
  public static String mergePath(String path, String[] arComp)
  {
    StringBuilder sb = new StringBuilder(512);
    if(path.endsWith("/"))
      sb.append(path.substring(0, path.length() - 1));
    else
      sb.append(path);

    for(String sp : arComp)
    {
      sb.append("/").append(sp);
    }

    return sb.toString();
  }

  /**
   * Fonde stringhe path insieme evitando duplicati di separatore.
   * A differenza del file sistem qui il serparatore è sempre '/'.
   * @param path path origine
   * @param lsComp lista di nomi file da fondere
   * @return la path con i separatori corretti
   */
  public static String mergePath(String path, List<String> lsComp)
  {
    return mergePath(path, StringOper.toArray(lsComp));
  }

  /**
   * Generazione di una voce di combobox.
   * @param valore valore da inserire
   * @param defVal default da selezionare
   * @return stringa HTML
   */
  public static String generaOptionCombo(String valore, String defVal)
  {
    return generaOptionCombo(valore, valore, defVal);
  }

  /**
   * Generazione di una voce di combobox.
   * @param codice valore restituito nel post
   * @param descrizione descrizione visualizzata
   * @param defVal default (codice) da selezionare
   * @return stringa HTML
   */
  public static String generaOptionCombo(String codice, String descrizione, String defVal)
  {
    return generaOptionCombo(codice, descrizione, StringOper.isEqu(codice, defVal));
  }

  /**
   * Generazione di una voce di combobox.
   * @param codice valore restituito nel post
   * @param descrizione descrizione visualizzata
   * @param isSelected vero per l'elemento da selezionare
   * @return stringa HTML
   */
  public static String generaOptionCombo(String codice, String descrizione, boolean isSelected)
  {
    String sdes = aggiustaDescrizione(descrizione);

    if(isSelected)
      return "<option value=\"" + codice + "\" selected>" + sdes + "</option>";
    else
      return "<option value=\"" + codice + "\">" + sdes + "</option>";
  }

  /**
   * Generazione di una voce di combobox.
   * @param codice valore restituito nel post
   * @param descrizione descrizione visualizzata
   * @param defVal default (codice) da selezionare
   * @return stringa HTML
   */
  public static String generaOptionCombo(int codice, String descrizione, int defVal)
  {
    String sdes = aggiustaDescrizione(descrizione);

    if(codice == defVal)
      return "<option value=\"" + codice + "\" selected>" + sdes + "</option>";
    else
      return "<option value=\"" + codice + "\">" + sdes + "</option>";
  }

  public static String aggiustaDescrizione(String descrizione)
  {
    return StringUtils.abbreviate(StringOper.okStr(descrizione, "&nbsp;"), SetupHolder.getComboDescLimit());
  }
}
