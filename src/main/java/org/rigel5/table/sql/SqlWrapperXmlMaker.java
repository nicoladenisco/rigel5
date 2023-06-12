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
package org.rigel5.table.sql;

import java.util.*;
import org.jdom2.*;
import org.rigel5.exceptions.XmlSyntaxException;
import org.rigel5.table.RigelBaseWrapperXmlMaker;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.WrapperBase;
import org.rigel5.table.html.wrapper.EditInfo;
import org.rigel5.table.html.wrapper.ParametroListe;
import org.rigel5.table.sql.html.HtmlSqlWrapperBase;
import org.rigel5.table.sql.html.SqlWrapperFormHtml;
import org.rigel5.table.sql.html.SqlWrapperListaHtml;
import org.rigel5.table.sql.swing.SqlWrapperListaSwing;
import org.rigel5.table.sql.xml.SqlWrapperListaXml;

/**
 * <p>
 * Produttore di wrapper specializzato per SQL.</p>
 * <p>
 * I wrapper sono dei coordinatori fra visualizzatori
 * e fornitori di dati.</p>
 * <p>
 * Questa classe costruisce wrapper specializzati per lavorare con
 * SqlTableModel ovvero con generatori di dati da query libere.</p>
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlWrapperXmlMaker extends RigelBaseWrapperXmlMaker
{
  @Override
  public WrapperBase parseWrapper(String nomeLista, Element lista, WrapperBase wb,
     boolean edit, boolean removeCaratt)
     throws Exception
  {
    HtmlSqlWrapperBase wl = (HtmlSqlWrapperBase) (super.parseWrapper(nomeLista, lista, wb, edit, removeCaratt));

    String s = null;
    if((s = lista.getChildTextTrim("select")) != null)
      wl.ssp.setSelect(s);
    if((s = lista.getChildTextTrim("from")) != null)
      wl.ssp.setFrom(s);
    if((s = lista.getChildTextTrim("where")) != null)
      wl.ssp.setWhere(s);
    if((s = lista.getChildTextTrim("having")) != null)
      wl.ssp.setHaving(s);
    if((s = lista.getChildTextTrim("delete-from")) != null)
      wl.ssp.setDeleteFrom(s);
    if((s = lista.getChildTextTrim("groupby-str")) != null)
      wl.ssp.setStrGroupby(s);

    if(wl.ssp.getSelect() == null || wl.ssp.getFrom() == null)
      throw new XmlSyntaxException(nomeLista, "Le clausole select e from sono obbligatorie!");

    List<Element> colonne = lista.getChildren("colonna");
    for(Element item : colonne)
    {
      RigelColumnDescriptor cd = buildColWorker(item, nomeLista, wb, edit);

      if(removeCaratt && cd.isCaratteristiche())
        continue;

      addColumn(item, wl.getPtm(), cd);
    }

    if((s = lista.getChildTextTrim("orderby")) != null)
      wl.ssp.setOrderby(s);

    // rimuove colonne in liste derivate
    removeColumns(lista, wl.getPtm());

    return wl;
  }

  private RigelColumnDescriptor buildColWorker(Element item, String nomeLista, WrapperBase wb, boolean edit)
     throws Exception
  {
    String nomeColonna = item.getAttributeValue("nome");

    if(wb.isCustomColumnsEnabled())
      return parseColumn(nomeLista, item, edit,
         buildColumn(nomeLista, nomeColonna, item, SqlColumnDescriptor.class));

    return parseColumn(nomeLista, item, edit, new SqlColumnDescriptor());
  }

  public SqlWrapperListaHtml getLista(String nomeLista, SqlWrapperListaHtml wr)
     throws Exception
  {
    Element lista = getElementLista(XML_LISTE_SQL, nomeLista);
    String extend = getExtends(lista);
    if(extend != null)
      wr = getLista(extend, wr);

    return (SqlWrapperListaHtml) (parseWrapper(nomeLista, lista, wr, false, false));
  }

  public SqlWrapperFormHtml getForm(String nomeForm, SqlWrapperFormHtml wr)
     throws Exception
  {
    Element form = getElementLista(XML_FORMS_SQL, nomeForm);
    String extend = getExtends(form);
    if(extend != null)
      wr = getForm(extend, wr);

    String sNumColon;
    if((sNumColon = form.getChildTextTrim("numcolonne")) != null)
      wr.setNumColonne(Integer.parseInt(sNumColon));

    return (SqlWrapperFormHtml) (parseWrapper(nomeForm, form, wr, true, false));
  }

  //////////////////////////////////////////////////////////////////////
  public SqlWrapperListaHtml getLista(String nomeLista)
     throws Exception
  {
    return getLista(nomeLista, new SqlWrapperListaHtml());
  }

  public SqlWrapperFormHtml getForm(String nomeForm)
     throws Exception
  {
    return getForm(nomeForm, new SqlWrapperFormHtml());
  }

  public SqlWrapperListaXml getListaXml(String nomeLista, boolean removeCaratt)
     throws Exception
  {
    Element lista = getElementLista(XML_XML_SQL, nomeLista);
    SqlWrapperListaXml wl = (SqlWrapperListaXml) (parseWrapper(nomeLista,
       lista, new SqlWrapperListaXml(), false, removeCaratt));

    Element e = null;
    if((e = lista.getChild("print-info")) != null)
    {
      EditInfo ei = new EditInfo();
      ei.setUrlEditRiga(e.getChildTextTrim("url"));
      List lPrintParam = e.getChildren("param");
      Iterator iterPp = lPrintParam.iterator();
      while(iterPp.hasNext())
      {
        Element item = (Element) (iterPp.next());
        ei.addParamEditRiga(item.getAttributeValue("nome"), item.getTextTrim());
      }
      wl.setPrInfo(ei);
    }
    else
      throw new XmlSyntaxException(nomeLista, "Manca la direttiva print-info: per le liste XML e' obbligatoria!");

    if((e = lista.getChild("groupby")) != null)
    {
      SqlGroupBy gb = new SqlGroupBy();
      List colonneGroupby = e.getChildren("colonna");
      Iterator iter = colonneGroupby.iterator();
      while(iter.hasNext())
      {
        Element item = (Element) (iter.next());

        RigelColumnDescriptor cd = buildColWorker(item, nomeLista, wl, false);

        gb.colonne.add(cd);
        wl.getPtmGroup().addColumn(cd);
      }

      List parametri = e.getChildren("parametro");
      Iterator iterp = parametri.iterator();
      while(iterp.hasNext())
      {
        Element item = (Element) (iterp.next());
        gb.addParametri(parseParametro(nomeLista, item, new ParametroListe()));
      }

      gb.orderby = e.getChildTextTrim("orderby");
      wl.ssp.setGroupby(gb);
    }

    // attributo fixed-cols: usa dimensioni di size (e non autocalcolate)
    if((e = lista.getChild("fixed-cols")) != null)
      wl.setFixedCols(true);

    // attributo unique-query: una unica query al db per tutti i dati
    if((e = lista.getChild("unique-query")) != null)
      wl.setUniqueQuery(true);

    return wl;
  }

  public SqlWrapperListaXml getListaXmlFromListe(String nomeLista, boolean removeCaratt)
     throws Exception
  {
    Element lista = getElementLista(XML_LISTE_SQL, nomeLista);
    SqlWrapperListaXml wl = (SqlWrapperListaXml) (parseWrapper(nomeLista,
       lista, new SqlWrapperListaXml(), false, removeCaratt));

    Element e = null;
    if((e = lista.getChild("groupby")) != null)
    {
      List colonneGroupby = e.getChildren("colonna");
      Iterator iter = colonneGroupby.iterator();
      SqlGroupBy gb = new SqlGroupBy();
      while(iter.hasNext())
      {
        Element item = (Element) (iter.next());

        RigelColumnDescriptor cd = buildColWorker(item, nomeLista, wl, false);

        gb.colonne.add(cd);
        wl.getPtmGroup().addColumn(cd);
      }

      List parametri = e.getChildren("parametro");
      Iterator iterp = parametri.iterator();
      while(iterp.hasNext())
      {
        Element item = (Element) (iterp.next());
        gb.addParametri(parseParametro(nomeLista, item, new ParametroListe()));
      }
      wl.ssp.setGroupby(gb);
    }

    // attributo fixed-cols: usa dimensioni di size (e non autocalcolate)
    if((e = lista.getChild("fixed-cols")) != null)
      wl.setFixedCols(true);

    // attributo unique-query: una unica query al db per tutti i dati
    if((e = lista.getChild("unique-query")) != null)
      wl.setUniqueQuery(true);

    return wl;
  }

  public SqlWrapperListaSwing getListaSwing(String nomeLista)
     throws Exception
  {
    Element lista = getElementLista(XML_LISTE_SQL, nomeLista);
    return (SqlWrapperListaSwing) (parseWrapper(nomeLista, lista, new SqlWrapperListaSwing(), false, false));
  }
}
