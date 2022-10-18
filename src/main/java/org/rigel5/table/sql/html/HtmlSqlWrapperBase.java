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
package org.rigel5.table.sql.html;

import java.util.*;
import org.rigel5.db.sql.FiltroData;
import org.rigel5.db.torque.CriteriaRigel;
import org.rigel5.exceptions.MissingColumnException;
import org.rigel5.exceptions.MissingParameterException;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.wrapper.HtmlWrapperBase;
import org.rigel5.table.html.wrapper.ParametroListe;
import org.rigel5.table.sql.SqlSelectParam;

/**
 * <p>
 * Title: Classe base di tutti i Wrapper basati su query SQL libere.</p>
 * <p>
 * Description: Questa e' la classe base dei Wrapper
 * che utilizzano query SQL liber (non Peer).</p>
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class HtmlSqlWrapperBase extends HtmlWrapperBase
{
  public SqlSelectParam ssp = new SqlSelectParam();

  public void setSelect(String string)
  {
    ssp.setSelect(string);
  }

  public void setFrom(String string)
  {
    ssp.setFrom(string);
    setNomeTabella(string);
  }

  public void setWhere(String string)
  {
    ssp.setWhere(string);
  }

  public void setOrderby(String string)
  {
    ssp.setOrderby(string);
  }

  public void setGroupBy(String string)
  {
    ssp.setStrGroupby(string);
  }

  public void setHaving(String having)
  {
    ssp.setHaving(having);
  }

  public void setDeleteFrom(String deleteFrom)
  {
    ssp.setDeleteFrom(deleteFrom);
    setNomeTabella(deleteFrom);
  }

  @Override
  public String getNomeTabella()
  {
    if(nomeTabella == null)
      nomeTabella = ssp.getDeleteFrom();
    if(nomeTabella == null)
      nomeTabella = ssp.getFrom();
    return super.getNomeTabella();
  }

  /**
   * Ritorna una colonna a partire dal nome del campo sul database
   * visualizzato dalla colonna.
   * @param nomeSQL il nome del campo della tabella SQL
   * @return l'oggetto RigelColumnDescriptor corrispondente oppure null
   */
  public RigelColumnDescriptor getColumn(String nomeSQL)
  {
    return ptm.getColumnByName(nomeSQL);
  }

  /**
   * Produce il criterio di selezione del record sottoposto a modifica
   * in base ai parametri passati. Viene utilizzata da jsform.jsp per
   * indivisuare il record da sottoporre ad editing.
   * @param param i parametri della richiesta HTTP
   * @return il criterio di selezione del record
   * @throws Exception
   */
  public FiltroData makeCriteriaEditRiga(Map param)
     throws Exception
  {
    FiltroData fd = new FiltroData();
    Enumeration enumParam = edInfo.getEnumParamsKey();
    while(enumParam.hasMoreElements())
    {
      String nomec = (String) enumParam.nextElement();
      String campo = edInfo.getParam(nomec);

      String valore = (String) (param.get(nomec));
      if(valore == null)
        throw new MissingParameterException("Parametro " + nomec + " non specificato nella richiesta!");

      if(campo.startsWith("#"))
      {
        // aggancio dinamico a valore di colonna
        RigelColumnDescriptor cd = ptm.getColumn(campo.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + campo + " non trovata!");

        fd.addWhere(cd, CriteriaRigel.EQUAL, valore);
      }
      else if(campo.startsWith("@"))
      {
        // aggancio dinamico a valore di campo tabella
        RigelColumnDescriptor cd = ptm.getColumnByName(campo.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + campo + " non trovata!");

        fd.addWhere(cd, CriteriaRigel.EQUAL, valore);
      }
    }

    return fd;
  }

  /**
   * Costruisce dei campi invisibili con i dati di chiamata.
   * Viene utilizzata nelle jsp e bean per le stampe XML per consentire la
   * post del form recuperando i parametri utilizzati per la
   * selezione del record da modificare.
   * @param row indice di riga (di solito 0: ignorato)
   * @return la stringa HTML con i campi hidden da inserire nel form
   * @throws Exception
   */
  public String makeHiddenPrintParametri(int row)
     throws Exception
  {
    String sOut = "";
    Enumeration enumParam = prInfo.getEnumParamsKey();
    while(enumParam.hasMoreElements())
    {
      String nomec = (String) enumParam.nextElement();
      String valore = prInfo.getParam(nomec);

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

      sOut += "<input type=\"hidden\" name=\"" + nomec + "\" value=\"" + valore + "\">\r\n";
    }

    return sOut;
  }

  public String getOrderby()
  {
    return getOrderby(ptm);
  }

  public String getOrderby(RigelTableModel ptm)
  {
    if(ssp.getOrderby() == null && !sortColumns.isEmpty())
    {
      String orderby = "";
      for(String item : sortColumns)
      {
        RigelColumnDescriptor cd = ptm.getColumn(item);
        if(cd != null)
          orderby += "," + cd.getName();
      }

      orderby = orderby.length() == 0 ? null : orderby.substring(1);
      ssp.setOrderby(orderby);
    }
    return ssp.getOrderby();
  }

  public String getWhereParametri()
  {
    if(!filtro.haveParametri())
      return null;

    String rv = "";
    for(ParametroListe pl : filtro.getParametri())
    {
      if(pl.getValore() != null)
        rv += " AND " + pl.getCampo() + pl.getOperazione() + pl.getValoreFmt();
    }

    return rv.length() == 0 ? null : rv.substring(5);
  }

  public FiltroData getFiltroParametri()
  {
    if(!filtro.haveParametri())
      return null;

    FiltroData fd = new FiltroData();
    for(ParametroListe pl : filtro.getParametri())
    {
      if(pl.getValore() != null)
        fd.addWhere(pl.getTipo(), pl.getCampo(), pl.getOperazione(), pl.getValore());
    }

    return fd;
  }
}
