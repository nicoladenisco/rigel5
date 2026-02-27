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
import org.apache.torque.criteria.SqlEnum;
import org.commonlib5.utils.DateTime;
import org.commonlib5.utils.StringOper;
import org.rigel5.RigelI18nInterface;
import org.rigel5.db.sql.FiltroData;
import org.rigel5.db.torque.CriteriaRigel;
import org.rigel5.table.BuilderRicercaGenerica;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;

/**
 * Costruttore del filtro dei records per le liste
 * che utilizzano query libere.
 *
 * @author Nicola De Nisco
 */
public class SqlBuilderRicercaGenerica implements BuilderRicercaGenerica
{
  protected RigelTableModel ptm = null;
  protected String nomeTabella = "";

  public SqlBuilderRicercaGenerica(RigelTableModel Ptm, String nometab)
     throws Exception
  {
    ptm = Ptm;
    nomeTabella = nometab;
  }

  /**
   * Analizza le informazioni di runtime del map builder
   * per rintracciare i nomi dei campi sulla tabella SQL.
   * Quindi costruisce il Criteria di select di conseguenza.
   * @throws java.lang.Exception
   */
  @Override
  public Object buildCriteria()
     throws Exception
  {
    FiltroData fd = new FiltroData();
    Object[] sortOrder = new Object[ptm.getColumnCount()];

    for(int i = 0; i < ptm.getColumnCount(); i++)
    {
      SqlColumnDescriptor cd = (SqlColumnDescriptor) (ptm.getColumn(i));
      sortOrder[i] = cd;

      if(cd.isEscludiRicerca() || cd.getFiltroTipo() == 0)
        continue;

      int idxCombo = cd.getFiltroTipo();

      if(idxCombo == IDX_CRITERIA_LIKE && cd.getDataType() == RigelColumnDescriptor.PDT_STRING) // LIKE
      {
        buildForLike(cd, fd);
      }
      else if(idxCombo == IDX_CRITERIA_BETWEEN) // BETWEEN
      {
        buildForBetween(cd, fd);
      }
      else
      {
        SqlEnum critItem = (idxCombo == 1) ? CriteriaRigel.EQUAL : criteriaItems[idxCombo];

        if(idxCombo == 9 || idxCombo == 10)
        {
          fd.addWhere(cd, critItem, null);
        }
        else
        {
          // parsing e riformattazione corretta del valore di filtro
          Object valFiltro = cd.parseValue(cd.getFiltroValore());
          cd.setFiltroValore(cd.formatValueRicerca(valFiltro));
          fd.addWhere(cd, critItem, valFiltro);
        }
      }
    }

    // ordina le colonne in base al filtro di ordinamento
    Arrays.sort(sortOrder, (Object o1, Object o2) ->
    {
      int n1 = ((RigelColumnDescriptor) (o1)).getFiltroSort();
      int n2 = ((RigelColumnDescriptor) (o2)).getFiltroSort();

      if(n1 > 1000)
        n1 -= 1000;
      if(n2 > 1000)
        n2 -= 1000;
      return n1 - n2;
    });

    // imposta filtro di ordinamento
    for(int i = 0; i < ptm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) (sortOrder[i]);
      if(cd.isEscludiRicerca() || cd.getFiltroSort() == 0)
        continue;

      if(cd.getFiltroSort() > 1000)
        fd.addOrderby(cd, "DESC");
      else
        fd.addOrderby(cd, "ASC");
    }

    return fd;
  }

  protected void buildForBetween(SqlColumnDescriptor cd, FiltroData fd)
  {
    String valFiltro = cd.getFiltroValore();
    String[] ss = StringOper.split(valFiltro, '|');

    if(ss.length == 1 && cd.isDate())
    {
      // caso speciale: campo date con intervallo da ricerca semplice con combo box
      Calendar now = new GregorianCalendar();
      Object valFiltroDa = null;
      Object valFiltroA = DateTime.fineGiorno(now.getTime());

      switch(valFiltro)
      {
        case "0": // tutti
          valFiltroDa = valFiltroA = null;
          cd.setFiltroTipo(0);
          break;

        case "1": // Oggi
          valFiltroDa = DateTime.inizioGiorno(now.getTime());
          break;

        case "2": // Ieri e oggi
          now.add(Calendar.DAY_OF_YEAR, -1);
          valFiltroDa = DateTime.inizioGiorno(now.getTime());
          break;

        case "3": // Ultimi due giorni
          now.add(Calendar.DAY_OF_YEAR, -2);
          valFiltroDa = DateTime.inizioGiorno(now.getTime());
          break;

        case "4": // Ultima settimana
          now.add(Calendar.WEEK_OF_YEAR, -1);
          valFiltroDa = DateTime.inizioGiorno(now.getTime());
          break;

        case "5": // Ultimi 15 giorni
          now.add(Calendar.DAY_OF_YEAR, -15);
          valFiltroDa = DateTime.inizioGiorno(now.getTime());
          break;

        case "6": // Ultimi 6 mesi
          now.add(Calendar.MONTH, -6);
          valFiltroDa = DateTime.inizioGiorno(now.getTime());
          break;

        case "7": // Ultimo anno
          now.add(Calendar.YEAR, -1);
          valFiltroDa = DateTime.inizioGiorno(now.getTime());
          break;
      }

      if(valFiltroDa != null && valFiltroA != null)
      {
        fd.addBetween(cd, valFiltroDa, valFiltroA);
      }
    }
    else if(ss.length >= 2)
    {
      // parsing e riformattazione corretta del valore di filtro
      Object valFiltroDa = cd.parseValue(ss[0]);
      Object valFiltroA = cd.parseValue(ss[1]);
      cd.setFiltroValore(cd.formatValueRicerca(valFiltroDa) + "|" + cd.formatValueRicerca(valFiltroA));
      fd.addBetween(cd, valFiltroDa, valFiltroA);
    }
  }

  protected void buildForLike(SqlColumnDescriptor cd, FiltroData fd)
  {
    SqlEnum critItem = SqlEnum.LIKE;
    String filtro = "";
    String valFiltro = StringOper.okStr(cd.getFiltroValore());

    // supporto alle regular expression
    if(valFiltro.startsWith("ri:"))
    {
      // confronto case insensitive (usiamo MINUS_ALL non essendo previsto un operatore specifico)
      critItem = SqlEnum.MINUS_ALL;
      filtro = valFiltro.substring(3);
    }
    else if(valFiltro.startsWith("re:"))
    {
      // confronto case sensitive (usiamo MINUS non essendo previsto un operatore specifico)
      critItem = SqlEnum.MINUS;
      filtro = valFiltro.substring(3);
    }
    else
    {
      StringTokenizer stok = new StringTokenizer(valFiltro);
      while(stok.hasMoreTokens())
      {
        String s = stok.nextToken();
        filtro += "%" + s.trim() + "% ";
      }
    }

    fd.addWhere(cd, critItem, filtro.trim());
  }

  @Override
  public String getHtmlComboColonnaMaschera(String formName, String fieldName,
     RigelColumnDescriptor cd, String defVal, RigelI18nInterface i18n)
     throws Exception
  {
    String sOut = "";
    String nomeCombo = "VL" + fieldName;

    // imposta automaticamente il combo del criteria a uguale
    String setComboFunjs = "document." + formName
       + ".OP" + fieldName + "[" + IDX_CRITERIA_EQUAL + "].selected = '1';";
    sOut = "<select name=\"" + nomeCombo + "\" onChange=\"" + setComboFunjs + "\">";

    if(cd.isComboSelf())
      sOut += cd.getHtmlComboColonnaSelf(0, 0, ptm, nomeTabella, cd.getName(), defVal);
    else
      sOut += cd.getHtmlComboColonnaAttached(0, 0, ptm, defVal, i18n, false);
    sOut += "</select>";

    return sOut;
  }

  @Override
  public String getHtmlComboColonnaRicSemplice(String formName, String fieldName,
     RigelColumnDescriptor cd, String defVal, RigelI18nInterface i18n)
     throws Exception
  {
    StringBuilder sOut = new StringBuilder(512);
    String nomeCombo = "VL" + fieldName;

    // imposta automaticamente il combo del criteria a uguale
    String setComboFunjs = "document." + formName + ".submit();";
    sOut.append("<select name=\"").append(nomeCombo).append("\" onChange=\"").append(setComboFunjs).append("\">");

    // aggiunge il valore per annullare questo campo
    defVal = StringOper.okStrNull(defVal);
    if(defVal == null || defVal.equals("0"))
      sOut.append("<option value=\"\" selected>TUTTI</option>");
    else
      sOut.append("<option value=\"\">TUTTI</option>");

    if(cd.isComboSelf())
      sOut.append(cd.getHtmlComboColonnaSelf(0, 0, ptm, nomeTabella, cd.getName(), defVal));
    else
      sOut.append(cd.getHtmlComboColonnaAttached(0, 0, ptm, defVal, i18n, true));

    sOut.append("</select>");

    return sOut.toString();
  }

  @Override
  public String[] getTipiConfronto()
  {
    return comboItems;
  }

  @Override
  public SqlEnum[] getTipiCriteria()
  {
    return criteriaItems;
  }

  @Override
  public boolean isBetween(int idx)
  {
    return idx == 8;
  }
}
