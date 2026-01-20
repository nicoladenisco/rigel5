/*
 * Copyright (C) 2026 Nicola De Nisco
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

import java.util.Map;
import java.util.Properties;
import org.commonlib5.utils.StringBuilderPair;
import org.commonlib5.utils.StringJoin;
import org.commonlib5.utils.StringOper;
import static org.rigel5.HtmlUtils.encodeURI;

/**
 * Builder specializzato per la costruzione di URI.
 *
 * @author Nicola De Nisco
 */
public class UrlBuilder extends StringBuilderPair
{
  public UrlBuilder()
  {
  }

  public UrlBuilder(int size)
  {
    super(size);
  }

  public UrlBuilder mergePath(String... str)
  {
    if(!sb.toString().endsWith("/"))
      sb.append("/");

    sb.append(StringJoin.build("/").add(str).join());
    return this;
  }

  /**
   * Fonde una url e un parametro applicando i controlli
   * sui caratteri di concatenamento.
   * I parametri e i loro valori vengono fusi alla URL utilizzando
   * la convenzione di encoding delle URL, ovvero qualsiasi carattere
   * sia contenuto nel nome o nel valore viene correttamente codificato
   * affinchè sia decodificato dal server.
   * @param paramName nome del parametro
   * @param paramValue valore del parametro
   * @return
   */
  public UrlBuilder mergeUrl(String paramName, String paramValue)
  {
    if(!StringOper.isOkStr(paramName) || !StringOper.isOkStr(paramValue))
      return this;

    if(paramValue.startsWith("&") || paramValue.startsWith("?"))
      paramValue = paramValue.substring(1);

    if(contains("?"))
      append("&");
    else
      append("?");

    append(encodeURI(paramName)).append("=").append(encodeURI(paramValue));
    return this;
  }

  public UrlBuilder mergeUrl(String paramName, int paramValue)
  {
    return mergeUrl(paramName, String.valueOf(paramValue));
  }

  public UrlBuilder mergeUrl(String paramName, long paramValue)
  {
    return mergeUrl(paramName, String.valueOf(paramValue));
  }

  public UrlBuilder mergeUrl(String paramName, double paramValue)
  {
    return mergeUrl(paramName, String.valueOf(paramValue));
  }

  public UrlBuilder mergeUrl(String paramName, boolean paramValue)
  {
    return mergeUrl(paramName, String.valueOf(paramValue));
  }

  public UrlBuilder mergeUrl(Map<String, String> params)
  {
    if(params != null && !params.isEmpty())
    {
      for(Map.Entry<String, String> entry : params.entrySet())
      {
        String key = StringOper.okStrNull(entry.getKey());
        String val = StringOper.okStrNull(entry.getValue());
        mergeUrl(key, val);
      }
    }
    return this;
  }

  public UrlBuilder mergeUrl(Properties params)
  {
    if(params != null && !params.isEmpty())
    {
      for(Map.Entry<Object, Object> entry : params.entrySet())
      {
        String key = StringOper.okStrNull(entry.getKey());
        String val = StringOper.okStrNull(entry.getValue());
        if(key != null && val != null)
          mergeUrl(key, val);
      }
    }
    return this;
  }

  /**
   * Fonde i parametri alla url verificando che non ci siano duplicati.
   * @param params parametri da aggiungere
   * @return
   */
  public UrlBuilder mergeUrlTestUnique(Map<String, String> params)
  {
    if(params != null && !params.isEmpty())
    {
      for(Map.Entry<String, String> entry : params.entrySet())
      {
        String key = entry.getKey();
        String val = entry.getValue();

        if(!contains(key + "="))
          mergeUrl(key, val);
      }
    }
    return this;
  }

  public UrlBuilder mergeUrlPair(Object... params)
  {
    if((params.length & 1) != 0)
      throw new IllegalArgumentException("params list must be pair");

    for(int i = 0; i < params.length; i += 2)
    {
      String key = StringOper.okStrNull(params[i]);
      String val = StringOper.okStrNull(params[i + 1]);
      if(key != null && val != null)
        mergeUrl(key, val);
    }

    return this;
  }

  public UrlBuilder mergeUrlPairTestUnique(Object... params)
  {
    if((params.length & 1) != 0)
      throw new IllegalArgumentException("params list must be pair");

    for(int i = 0; i < params.length; i += 2)
    {
      String key = StringOper.okStrNull(params[i]);
      String val = StringOper.okStrNull(params[i + 1]);
      if(key != null && val != null && !contains(key + "="))
        mergeUrl(key, val);
    }

    return this;
  }
}
