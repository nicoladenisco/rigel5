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
package org.rigel5.table.peer;

/**
 * Title: Newstar
 * Description:
 * Copyright: Copyright (c) 2002
 * Company: Italsystems s.r.l.
 * @author Nicola De Nisco
 * @version 1.0
 */
import java.util.*;
import org.apache.torque.criteria.SqlEnum;
import org.apache.torque.map.*;
import org.commonlib5.utils.StringOper;
import org.rigel5.RigelI18nInterface;
import org.rigel5.db.torque.CriteriaRigel;
import org.rigel5.db.torque.TableMapHelper;
import org.rigel5.table.BuilderRicercaGenerica;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;

/**
 * Costruttore del filtro dei records per le liste
 * che utilizzano gli oggetti OM di torque (peer).
 *
 * @author Nicola De Nisco
 */
public class PeerBuilderRicercaGenerica implements BuilderRicercaGenerica
{
  protected RigelTableModel ptm = null;
  protected TableMapHelper tmap = new TableMapHelper();
  protected String nomeTabella = "";

  public PeerBuilderRicercaGenerica(RigelTableModel Ptm, TableMap Tmap)
     throws Exception
  {
    ptm = Ptm;
    tmap.setTmap(Tmap);
    nomeTabella = tmap.getNomeTabella();
  }

  /**
   * Analizza le informazioni di runtime del map builder
   * per rintracciare i nomi dei campi sulla tabella SQL.
   * Quindi costruisce il Criteria di select di conseguenza.
   */
  @Override
  public Object buildCriteria()
     throws Exception
  {
    CriteriaRigel c = new CriteriaRigel();
    Object[] sortOrder = new Object[ptm.getColumnCount()];
    boolean haveFilter = false, haveSort = false;

    for(int i = 0; i < ptm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = ptm.getColumn(i);
      sortOrder[i] = cd;

      if(cd.isEscludiRicerca() || cd.getFiltroTipo() == 0)
        continue;

      int idxCombo = cd.getFiltroTipo();
      String nomeCampo = tmap.getNomeCampo(cd.getName());
      SqlEnum critItem = criteriaItems[idxCombo];

      if(idxCombo == 1) // LIKE
      {
        String filtro = "";
        String valFiltro = cd.getFiltroValore();
        StringTokenizer stok = new StringTokenizer(valFiltro);
        while(stok.hasMoreTokens())
        {
          String s = stok.nextToken();
          filtro += "%" + s.trim() + "% ";
        }
        c.and(nomeCampo, (Object) (filtro.trim()), critItem);
        haveFilter = true;
      }
      else if(idxCombo == 8) // BETWEEN
      {
        String valFiltro = cd.getFiltroValore();
        StringTokenizer stok = new StringTokenizer(valFiltro, "|");
        String filtroDa = stok.nextToken();
        String filtroA = stok.nextToken();

        // parsing e riformattazione corretta del valore di filtro
        Object valFiltroDa = cd.parseValue(filtroDa);
        Object valFiltroA = cd.parseValue(filtroA);
        cd.setFiltroValore(cd.formatValueRicerca(valFiltroDa) + "|" + cd.formatValueRicerca(valFiltroA));

        switch(cd.getDataType())
        {
          case RigelColumnDescriptor.PDT_BOOLEAN:
          case RigelColumnDescriptor.PDT_INTEGER:
          case RigelColumnDescriptor.PDT_STRINGKEY:
          case RigelColumnDescriptor.PDT_NUMBERKEY:
          case RigelColumnDescriptor.PDT_STRING:
            c.isBetween(nomeCampo, filtroDa, filtroA);
            haveFilter = true;
            break;
          case RigelColumnDescriptor.PDT_FLOAT:
          case RigelColumnDescriptor.PDT_DOUBLE:
          case RigelColumnDescriptor.PDT_MONEY:
            c.isBetween(nomeCampo, (Double) valFiltroDa, (Double) valFiltroA);
            haveFilter = true;
            break;
          case RigelColumnDescriptor.PDT_DATE:
          case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
          case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
          case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
          case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
          case RigelColumnDescriptor.PDT_TIMESTAMP:
          case RigelColumnDescriptor.PDT_TIME:
            c.isBetween(nomeCampo, (Date) valFiltroDa, (Date) valFiltroA);
            haveFilter = true;
            break;
        }
      }
      else
      {
        // parsing e riformattazione corretta del valore di filtro
        Object valFiltro = cd.parseValue(cd.getFiltroValore());
        cd.setFiltroValore(cd.formatValueRicerca(valFiltro));

        c.and(nomeCampo, valFiltro, critItem);
        haveFilter = true;
      }
    }

    // ordina le colonne in base al filtro di ordinamento
    Arrays.sort(sortOrder, new Comparator()
    {
      @Override
      public int compare(Object o1, Object o2)
      {
        int n1 = ((RigelColumnDescriptor) (o1)).getFiltroSort();
        int n2 = ((RigelColumnDescriptor) (o2)).getFiltroSort();

        if(n1 > 1000)
          n1 -= 1000;
        if(n2 > 1000)
          n2 -= 1000;
        return n1 - n2;
      }

      @Override
      public boolean equals(Object obj)
      {
        return false;
      }
    });

    // imposta filtro di ordinamento
    for(int i = 0; i < ptm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) (sortOrder[i]);
      if(cd.isEscludiRicerca() || cd.getFiltroSort() == 0)
        continue;

      //log.debug("Colonna "+i+" "+cd.getName());
      ColumnMap cmap = tmap.getCampo(cd.getName());
      if(cd.getFiltroSort() > 1000)
        c.addDescendingOrderByColumn(cmap);
      else
        c.addAscendingOrderByColumn(cmap);

      haveSort = true;
    }

    //log.debug("SQL="+c.toString());
    return (haveFilter || haveSort) ? c : null;
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
      sOut += cd.getHtmlComboColonnaSelf(ptm,
         nomeTabella, tmap.getNomeCampo(cd.getName()), defVal);
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
      sOut.append(cd.getHtmlComboColonnaSelf(ptm, nomeTabella, tmap.getNomeCampo(cd.getName()), defVal));
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
    return criteriaItems[idx].equals(CriteriaRigel.BETWEEN);
  }
}
