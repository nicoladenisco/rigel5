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
package org.rigel5.table;

import com.google.inject.internal.Iterators;
import com.workingdogs.village.Record;
import java.util.*;
import javax.swing.table.*;
import org.apache.commons.logging.*;
import org.commonlib5.utils.StringOper;
import org.rigel5.HtmlUtils;
import org.rigel5.RigelI18nInterface;
import org.rigel5.SetupHolder;
import org.rigel5.db.sql.QueryBuilder;
import org.rigel5.exceptions.MissingColumnException;

/**
 * <p>
 * Title: Rigel</p>
 * <p>
 * Description: Fornitore dati per tabelle.</p>
 * <p>
 * Questa classe non viene mai utilizzata direttamente, ma
 * se ne usa le derivazioni: PeerTableModel e SqlTableModel.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class RigelTableModel extends AbstractTableModel
{
  /** Logging */
  private static Log log = LogFactory.getLog(RigelTableModel.class);
  protected Vector vColumn = new Vector(10, 10);
  protected TableColumnModel dtcmdl = null;
  protected java.awt.Color rowColor[] = null;
  protected boolean markDeleted[] = null;
  protected long totalRecords = -1;
  protected long totalRecordsFilter = -1;
  protected QueryBuilder query = null;
  protected String formName = "fo";
  protected final Map<String, String> properties = new HashMap<>();

  public int addColumn(RigelColumnDescriptor tc)
  {
    tc.setModelIndex(vColumn.size());
    vColumn.add(tc);
    return getColumnCount() - 1;
  }

  public int addColumn(int pos, RigelColumnDescriptor tc)
  {
    vColumn.add(pos, tc);
    assignIndexColumn();
    return pos;
  }

  public int addOrReplaceColumn(RigelColumnDescriptor tc, String oldColumnHeader)
  {
    if(oldColumnHeader != null)
    {
      for(int i = 0; i < vColumn.size(); i++)
      {
        RigelColumnDescriptor cd = (RigelColumnDescriptor) (vColumn.get(i));
        if(oldColumnHeader.equalsIgnoreCase(cd.getCaption()))
        {
          vColumn.remove(i);
          vColumn.add(i, tc);
          tc.setModelIndex(i);
          return i;
        }
      }
    }
    return addColumn(tc);
  }

  private void assignIndexColumn()
  {
    for(int i = 0; i < vColumn.size(); i++)
    {
      ((RigelColumnDescriptor) (vColumn.get(i))).setModelIndex(i);
    }
  }

  public void delColumn(int col)
  {
    vColumn.remove(col);
    assignIndexColumn();
  }

  public int delColumn(String header)
  {
    int removed = 0;
    for(int i = 0; i < vColumn.size(); i++)
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) (vColumn.get(i));
      if(header.equalsIgnoreCase(cd.getCaption()))
      {
        vColumn.remove(i);
        i--;
        removed++;
      }
      else
      {
        cd.setModelIndex(i);
      }
    }

    return removed;
  }

  public int delColumnByName(String name)
  {
    int removed = 0;
    for(int i = 0; i < vColumn.size(); i++)
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) (vColumn.get(i));
      if(name.equalsIgnoreCase(cd.getName()))
      {
        vColumn.remove(i);
        i--;
        removed++;
      }
      else
      {
        cd.setModelIndex(i);
      }
    }

    return removed;
  }

  public void delAllColumns()
  {
    vColumn.clear();
  }

  public void delColumns(int startIndex, int numCol)
  {
    if(startIndex >= vColumn.size())
      return;

    if(startIndex + numCol > vColumn.size())
      numCol = vColumn.size() - startIndex;

    while(numCol-- > 0)
      vColumn.remove(startIndex);

    assignIndexColumn();
  }

  public RigelColumnDescriptor getColumn(int col)
  {
    return (RigelColumnDescriptor) vColumn.get(col);
  }

  /**
   * Ritorna l'ultima colonna inserita.
   */
  public RigelColumnDescriptor lc()
  {
    return (RigelColumnDescriptor) vColumn.get(vColumn.size() - 1);
  }

  public RigelColumnDescriptor getColumn(String header)
  {
    int idx = findColumn(header);
    return idx == -1 ? null : (RigelColumnDescriptor) vColumn.get(idx);
  }

  public RigelColumnDescriptor getColumnByName(String name)
  {
    int idx = findColumnByName(name);
    return idx == -1 ? null : (RigelColumnDescriptor) vColumn.get(idx);
  }

  @Override
  public int findColumn(String header)
  {
    for(int i = 0; i < vColumn.size(); i++)
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) vColumn.get(i);
      if(header.equalsIgnoreCase(cd.getCaption()))
        return i;
    }
    return -1;
  }

  public int findColumnByName(String name)
  {
    for(int i = 0; i < vColumn.size(); i++)
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) vColumn.get(i);
      if(name.equalsIgnoreCase(cd.getName()))
        return i;
    }
    return -1;
  }

  @Override
  public String getColumnName(int column)
  {
    RigelColumnDescriptor cd = (RigelColumnDescriptor) vColumn.get(column);
    return cd.getName();
  }

  @Override
  public Object getValueAt(int row, int col)
  {
    try
    {
      return getColumn(col).getValue(getRowRecord(row));
    }
    catch(Exception e)
    {
      log.error("Error getValueAt() at row=" + row + " col=" + col, e);
    }

    return null;
  }

  @Override
  public int getColumnCount()
  {
    return vColumn.size();
  }

  @Override
  public boolean isCellEditable(int row, int col)
  {
    return getColumn(col).isEditable();
  }

  @Override
  public void setValueAt(Object aValue, int row, int col)
  {
    try
    {
      getColumn(col).setValue(getRowRecord(row), aValue);
      fireTableCellUpdated(row, col);
    }
    catch(Exception e)
    {
      RigelColumnDescriptor cd = getColumn(col);
      System.err.println(
         "Error setValueAt() at row=" + row + " col=" + col + " (" + cd.getName() + ") " + e.getMessage() + "\r\n"
         + "Property class=" + cd.getValClass().getName() + " (" + cd.getDataType() + ")\r\n"
         + "Value class=" + aValue.getClass().getName() + " (" + aValue + ")\r\n");
      log.error("RIGEL:", e);
    }
  }

  @Override
  public Class getColumnClass(int col)
  {
    return getColumn(col).getValClass();
  }

  public void setColumnVisible(int col, boolean visible)
  {
    getColumn(col).setVisible(visible);
  }

  public boolean isColumnVisible(int col)
  {
    return getColumn(col).isVisible();
  }

  public boolean toggleColumnVisible(int col)
  {
    boolean rv = !getColumn(col).isVisible();
    getColumn(col).setVisible(rv);
    return rv;
  }

  public void setRowColor(int row, java.awt.Color newRowColor)
  {
    rowColor[row] = newRowColor;
  }

  public java.awt.Color getRowColor(int row)
  {
    return rowColor[row];
  }

  public void setColumnColor(int col, java.awt.Color newColumnColor)
  {
    getColumn(col).setColor(newColumnColor);
  }

  public java.awt.Color getColumnColor(int col)
  {
    return getColumn(col).getColor();
  }

  public boolean isRowDeleted(int row)
  {
    return markDeleted[row];
  }

  public void setRowDeleted(int row, boolean val)
  {
    markDeleted[row] = val;
  }

  public boolean[] getAllRowDeleted()
  {
    return markDeleted;
  }

  public void importaAscii(Object bean, String sLinea)
     throws Exception
  {
    Enumeration enumCol = vColumn.elements();
    while(enumCol.hasMoreElements())
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) (enumCol.nextElement());
      cd.setValueAsciiLinea(bean, sLinea);
    }
  }

  public long getTotalRecords()
     throws Exception
  {
    return totalRecords;
  }

  public long getTotalRecords(FiltroListe fl)
     throws Exception
  {
    return totalRecordsFilter;
  }

  public void setTotalRecords(long totalRecords)
  {
    this.totalRecords = totalRecords;
  }

  public void setTotalRecordsFilter(long totalRecordsFilter)
  {
    this.totalRecordsFilter = totalRecordsFilter;
  }

  public void clearTotalRecords()
  {
    this.totalRecords = -1;
    this.totalRecordsFilter = -1;
  }

  public void initFrom(RigelTableModel rtm)
     throws Exception
  {
    vColumn = new Vector(rtm.vColumn);
    dtcmdl = rtm.dtcmdl;
    rowColor = rtm.rowColor;
    markDeleted = rtm.markDeleted;
    totalRecords = rtm.totalRecords;
    totalRecordsFilter = rtm.totalRecordsFilter;
  }

  public QueryBuilder makeQueryBuilder()
     throws Exception
  {
    return SetupHolder.getQueryBuilder();
  }

  public QueryBuilder getQuery()
  {
    return query;
  }

  public void setQuery(QueryBuilder query)
  {
    this.query = query;
  }

  /**
   * Collega le colonne alla tabella.
   */
  abstract public void reAttach();

  abstract public MascheraRicercaGenerica getMascheraRG(RigelI18nInterface i18n)
     throws Exception;

  abstract public boolean isInitalized();

  @Override
  abstract public int getRowCount();

  abstract public Object getRowRecord(int row);

  abstract public boolean isNewRecord(int row);

  abstract public String createQueryKey(int row)
     throws Exception;

  abstract public int deleteByQueryKey(String sKey)
     throws Exception;

  public void setFormName(String formName)
  {
    this.formName = formName;
  }

  public String getFormName()
  {
    return formName;
  }

  /**
   * Restituisce un valore elaborato come macro.
   * Il parametro 'valore' può contenenere al suo
   * interno delle macro che vengono sostiutite
   * rispettivamente:
   * '@nomecampo' diventa il valore corrispondente del campo
   * '#nomecolonna' diventa il valore corrispondente della colonna
   * NOTA: il valore viene elaborato in toto, ovvero la stringa
   * valore deve contenere solo una macro o in alternativa un
   * valore fisso (stringhe del tipo 'mio#colonna' non vengono elaborate).
   * @param row riga di riferimento
   * @param col colonna di riferimento
   * @param valore valore in input.
   * @return il valore elaborato
   * @throws Exception
   */
  public String getValueMacro(int row, int col, String valore)
     throws Exception
  {
    if(!StringOper.isOkStr(valore))
      return null;

    Object valObj = getRowRecord(row);
    if(valObj == null)
      return null;

    if(valore.startsWith("#"))
    {
      // aggancio dinamico a valore di colonna
      RigelColumnDescriptor cd = getColumn(valore.substring(1));
      if(cd == null)
        throw new MissingColumnException("Colonna " + valore + " non trovata!");

      valore = cd.getValueAsString(valObj);
    }
    else if(valore.startsWith("@"))
    {
      // aggancio dinamico a valore di campo tabella
      RigelColumnDescriptor cd = getColumnByName(valore.substring(1));
      if(cd == null)
        throw new MissingColumnException("Colonna " + valore + " non trovata!");

      valore = cd.getValueAsString(valObj);
    }

    return valore;
  }

  /**
   * Restituisce un valore elaborato come macro.
   * Il parametro 'valore' può contenenere al suo
   * interno delle macro che vengono sostiutite
   * rispettivamente:
   * '@nomecampo' diventa il valore corrispondente del campo
   * '#nomecolonna' diventa il valore corrispondente della colonna
   * NOTA: può contenere macro ('pippo=mio@campo1' viene elaborato
   * correttamente es: 'pippo=mio100' se campo1 corrisponde ad una
   * colonna intero con valore 100 per il record indicato da row.
   * @param row riga di riferimento
   * @param col colonna di riferimento
   * @param valore valore in input.
   * @param useQuote se vero i valori stringa vengono richiusi fra apici
   * @param encodeURI applica l'encoding ai valori sostituiti (parametri get)
   * @return il valore elaborato
   * @throws Exception
   */
  public String getValueMacroInside(int row, int col, String valore,
     boolean useQuote, boolean encodeURI)
     throws Exception
  {
    if(!StringOper.isOkStr(valore))
      return null;

    Object valObj = getRowRecord(row);
    if(valObj == null)
      return null;

    valore = resolveMacro('#', row, col, valore, useQuote, encodeURI);
    valore = resolveMacro('@', row, col, valore, useQuote, encodeURI);

    return valore;
  }

  /**
   * Usata internamente da getValueMacroInside per ogni carattere
   * che identifica l'inizio di una macro.
   * @param chbegin carattere che identifica l'inizio di una macro (solo '#' e '@' sono supportate)
   * @param row riga di riferimento
   * @param col colonna di riferimento
   * @param valore valore in input.
   * @param useQuote se vero i valori stringa vengono richiusi fra apici
   * @param encodeURI applica l'encoding ai valori sostituiti (parametri get)
   * @return il valore elaborato
   * @throws Exception
   */
  protected String resolveMacro(int chbegin, int row, int col, String valore,
     boolean useQuote, boolean encodeURI)
     throws Exception
  {
    int pos1 = 0, pos2 = 0;
    while((pos1 = valore.indexOf(chbegin)) != -1)
    {
      pos2 = pos1 + 1;
      while(pos2 < valore.length()
         && (Character.isLetterOrDigit(valore.charAt(pos2)) || valore.charAt(pos2) == '_'))
        pos2++;

      String macro = valore.substring(pos1 + 1, pos2);
      String subval = null;

      switch(chbegin)
      {
        case '#':
          if(StringOper.isEqu(macro, "row"))
          {
            subval = Integer.toString(row);
          }
          else if(StringOper.isEqu(macro, "col"))
          {
            subval = Integer.toString(col);
          }
          else
          {
            // aggancio dinamico a valore di colonna
            RigelColumnDescriptor cd = getColumn(macro);
            if(cd == null)
              throw new MissingColumnException("Colonna " + macro + " non trovata!");

            Object valObj = getRowRecord(row);
            if(valObj != null)
              subval = cd.getValueAsString(valObj);

            if(useQuote && !cd.isNumeric())
              subval = "'" + subval + "'";

            if(encodeURI)
              subval = HtmlUtils.encodeURI(subval);
          }
          break;

        case '@':
          if(StringOper.isEqu(macro, "row"))
          {
            subval = Integer.toString(row);
          }
          else if(StringOper.isEqu(macro, "col"))
          {
            subval = Integer.toString(col);
          }
          else
          {
            // aggancio dinamico a valore di campo tabella
            RigelColumnDescriptor cd = getColumnByName(macro);
            if(cd == null)
              throw new MissingColumnException("Colonna " + macro + " non trovata!");

            Object valObj = getRowRecord(row);
            if(valObj != null)
              subval = cd.getValueAsString(valObj);

            if(useQuote && !cd.isNumeric())
              subval = "'" + subval + "'";

            if(encodeURI)
              subval = HtmlUtils.encodeURI(subval);
          }
          break;

        default:
          throw new IllegalArgumentException(
             "Le macro con il carattere iniziale '" + chbegin + "' non sono definite.");
      }

      if(pos1 > 0)
      {
        valore = valore.substring(0, pos1) + subval + valore.substring(pos2);
      }
      else
      {
        valore = subval + valore.substring(pos2);
      }
    }

    return valore;
  }

  public String getProperty(String name)
  {
    return properties.get(name);
  }

  public void removeProperty(String name)
  {
    properties.remove(name);
  }

  public void clearProperties()
  {
    properties.clear();
  }

  public void setProperty(String name, String value)
  {
    properties.put(name, value);
  }

  public void setProperties(Map<String, String> prop)
  {
    properties.putAll(prop);
  }

  public Iterator<String> propertyKeys()
  {
    return Iterators.unmodifiableIterator(properties.keySet().iterator());
  }

  public Map<String, String> getAllProperties()
  {
    return Collections.unmodifiableMap(properties);
  }

  /**
   * Ritorna un array con i valori di un campo per tutte le righe.
   * @param campo campo di interesse
   * @return un array di interi col il corrispettivo valore per la riga
   * @throws Exception
   */
  public int[] getAllValuesInt(String campo)
     throws Exception
  {
    int[] allPbr = new int[getRowCount()];
    for(int i = 0; i < getRowCount(); i++)
    {
      Record r = (Record) getRowRecord(i);
      allPbr[i] = r.getValue(campo).asInt();
    }
    return allPbr;
  }

  /**
   * Ritorna un array con i valori di un campo per tutte le righe.
   * @param campo campo di interesse
   * @return un array di stringhe col il corrispettivo valore per la riga
   * @throws Exception
   */
  public String[] getAllValuesString(String campo)
     throws Exception
  {
    String[] allPbr = new String[getRowCount()];
    for(int i = 0; i < getRowCount(); i++)
    {
      Record r = (Record) getRowRecord(i);
      allPbr[i] = r.getValue(campo).asString();
    }
    return allPbr;
  }

  public TableColumnModel getColumnModel()
  {
    return dtcmdl;
  }

  public void setColumnModel(TableColumnModel dtcmdl)
  {
    this.dtcmdl = dtcmdl;
  }
}
