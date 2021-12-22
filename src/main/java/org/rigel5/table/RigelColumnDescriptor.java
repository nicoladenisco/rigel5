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

import java.net.URLEncoder;
import java.text.Format;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import org.apache.commons.logging.*;
import org.apache.torque.om.NumberKey;
import org.apache.torque.om.StringKey;
import org.commonlib5.utils.StringOper;
import org.rigel5.HtmlUtils;
import org.rigel5.RigelI18nInterface;
import org.rigel5.SetupHolder;
import org.rigel5.exceptions.InvalidForeignModeException;
import org.rigel5.table.html.wrapper.CustomButtonInfo;

/**
 * Descrittore di colonna per l'uso con RigelTableModel.
 * Ogni colonna viene rappresentata da un'istanza di RigelColumnDescriptor
 * che ne conserva le caratteristiche e partecipa al reperimento e formattazione
 * dei dati.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class RigelColumnDescriptor extends TableColumn
{
  /** Logging */
  private static Log log = LogFactory.getLog(RigelColumnDescriptor.class);
  //
  public static final int HTML_ALIGN_DEFAULT = 0;
  public static final int HTML_ALIGN_LEFT = 1;
  public static final int HTML_ALIGN_CENTER = 2;
  public static final int HTML_ALIGN_RIGHT = 3;
  //
  public static final int PDT_UNDEFINED = 0;
  public static final int PDT_STRING = 1;
  public static final int PDT_BOOLEAN = 2;
  public static final int PDT_INTEGER = 3;
  public static final int PDT_FLOAT = 4;
  public static final int PDT_DOUBLE = 5;
  public static final int PDT_DATE = 6;
  public static final int PDT_STRINGKEY = 7;
  public static final int PDT_NUMBERKEY = 8;
  public static final int PDT_MONEY = 9;
  public static final int PDT_TIMESTAMP_CMPDATEONLY = 10;
  public static final int PDT_TIMESTAMP_CMPHOURONLY = 11;
  public static final int PDT_TIMESTAMP_CMPTOSEC = 12;
  public static final int PDT_TIMESTAMP_CMPTOMIN = 13;
  public static final int PDT_TIMESTAMP = 14;
  public static final int PDT_TIME = 15;
  public static final int PDT_FILE = 16;
  //
  public static final int DISP_FLD_ONLY = 0;
  public static final int DISP_DESCR_ONLY = 1;
  public static final int DISP_FLD_DESCR = 2;
  public static final int DISP_FLD_DESCR_ALTERNATE = 3;
  public static final int DISP_FLD_EDIT = 4;
  public static final int DISP_DESCR_EDIT = 5;
  public static final int DISP_DESCR_EDIT_ALTERNATE = 6;
  //
  protected String name;
  protected boolean editable = false;
  protected boolean visible = true;
  protected boolean printable = true;
  protected java.awt.Color color = null;
  protected Class valClass = null;
  protected int size = 0;
  protected Format formatter;
  protected int asciiPos = 0;
  protected int asciiLun = 0;
  protected int htmlAlign = HTML_ALIGN_DEFAULT;
  protected JComponent formControl;
  protected boolean escludiRicerca = false;
  protected int filtroTipo;
  protected String filtroValore;
  protected int filtroSort;
  protected int dataType = PDT_UNDEFINED;
  protected int htmlSpan = 0;
  protected boolean caratteristicheSelezioneRiga;
  protected boolean caratteristicheEditRiga;
  protected boolean caratteristicheCancellaRiga;
  protected boolean suNuovaRiga;
  protected String fixedText;
  protected boolean testfortype;
  protected boolean testfornull;
  protected boolean testforcf;
  protected boolean testforpi;
  protected boolean testforzero;
  protected boolean testforcodice;
  protected boolean testrange;
  protected String testcustom, testrangemin, testrangemax;
  protected String extraScript;
  protected boolean primaryKey;
  protected boolean autoIncremento;
  protected String htmlStyle;
  protected boolean aggregatoSql;
  protected String defVal;
  protected String defValParam;
  protected Vector<CustomButtonInfo> customButtons = new Vector<CustomButtonInfo>();
  protected int ricercaSemplice;
  //
  protected boolean comboDisplay = false;
  protected boolean comboRicerca = false;
  protected String comboRicercaTabella;
  protected String comboRicercaCampoDisplay;
  protected String comboRicercaCampoLink;
  protected String comboExtraWhere;
  protected boolean comboExtraWhereRemoveZero = false;
  //
  protected String foreignCampoDisplay;
  protected String foreignCampoLink;
  protected String foreignCampoAlternateLink;
  protected String foreignTabella;
  protected String foreignEditUrl;
  protected String foreignFormUrl;
  protected int foreignMode = DISP_FLD_ONLY;
  protected boolean foreignAuto = false;
  protected boolean foreignAutoCombo = true;
  protected Hashtable<String, String> foreignFormParams = new Hashtable<String, String>();
  protected List<ForeignDataHolder> lForeignValues = null;
  //
  protected CustomColumnEdit coledit = null;
  /**
   * SOLO HTML: indica un campo visualizzato senza edit (editable = false)
   * ma con un campo nascosto per poter inserire valori con javascript.
   */
  protected boolean hiddenEdit;
  /**
   * SOLO HTML: indica un campo visualizzato senza edit (editable = false)
   * ma con un indicazione di paragrafo per poter modificare il valore
   * visualizzato con javascript.
   */
  protected boolean htmlPara;
  /**
   * Indica un campo inesistente a database, solo calcolato internamente.
   */
  protected boolean calcolato;
  /**
   * Indica se i dati di questo campo sono salvabili in cache.
   * In caso di visualizzazione sotto forma di combo o altri utilizzi in genere
   * questo flag stabilisce la memorizzazione in cache dei risultati intermedi.
   */
  protected boolean enableCache = true;

  public RigelColumnDescriptor()
  {
    super();
  }

  public void setNomeCalc(String Name)
  {
    if(Name.startsWith("#"))
    {
      calcolato = true;
      name = Name.substring(1);
    }
    else
    {
      calcolato = false;
      name = Name;
    }

    // se nel nome ci sono parentesi allora e'
    // una funzione aggregata sql (count(), sum(), average(), ...)
    aggregatoSql = name.contains("(");
  }

  public static int checkForType(String clName)
  {
    if(clName.equals("java.lang.String"))
      return PDT_STRING;
    if(clName.equals("java.lang.Integer")
       || clName.equals("java.lang.Short")
       || clName.equals("java.lang.Long"))
      return PDT_INTEGER;
    if(clName.equals("java.lang.Float"))
      return PDT_FLOAT;
    if(clName.equals("java.lang.Boolean"))
      return PDT_BOOLEAN;
    if(clName.equals("java.lang.Double"))
      return PDT_DOUBLE;
    if(clName.equals("java.util.Date")
       || clName.equals("java.sql.Date"))
      return PDT_DATE;
    if(clName.equals("org.apache.turbine.om.StringKey"))
      return PDT_STRINGKEY;
    if(clName.equals("org.apache.turbine.om.NumberKey"))
      return PDT_NUMBERKEY;

    return PDT_UNDEFINED;
  }

  public String getCaption()
  {
    return getHeaderValue().toString();
  }

  public void setCaption(String newCaption)
  {
    setHeaderValue(newCaption);
  }
  private String captionHtml = null;

  public String getCaptionHtml()
  {
    if(captionHtml == null)
      captionHtml = StringOper.CvtWEBstring(getCaption());
    return captionHtml;
  }

  public void setName(String newName)
  {
    name = newName;
  }

  public String getName()
  {
    return name;
  }

  public void setSize(int newSize)
  {
    size = newSize;
    setMinWidth(newSize);
    setWidth(newSize);
    setPreferredWidth(newSize);
  }

  public int getSize()
  {
    return size;
  }

  public void setEditable(boolean newEditable)
  {
    editable = newEditable;
  }

  public boolean isEditable()
  {
    return editable;
  }

  public boolean isVisible()
  {
    return visible;
  }

  public void setVisible(boolean v)
  {
    visible = v;
  }

  public boolean isPrintable()
  {
    return printable;
  }

  public void setPrintable(boolean printable)
  {
    this.printable = printable;
  }

  public void setColor(java.awt.Color newColor)
  {
    color = newColor;
  }

  public java.awt.Color getColor()
  {
    return color;
  }
  private static final Short sCl = new Short((short) 0);
  private static final Integer iCl = new Integer(0);
  private static final Long lCl = new Long(0);
  private static final Float fCl = new Float(0.0);
  private static final Double dCl = new Double(0.0);
  private static final Boolean bCl = new Boolean(false);

  public void setValClass(Class cl)
  {
    String clName = cl.getName();

    if(clName.equals("float"))
      valClass = fCl.getClass();
    else if(clName.equals("double"))
      valClass = dCl.getClass();
    else if(clName.equals("int"))
      valClass = iCl.getClass();
    else if(clName.equals("short"))
      valClass = sCl.getClass();
    else if(clName.equals("long"))
      valClass = lCl.getClass();
    else if(clName.equals("boolean"))
      valClass = bCl.getClass();
    else
      valClass = cl;

    // se dataType non e' ancora stato definito
    // lo imposta ad un valore ragionevole con valClass
    if(dataType == PDT_UNDEFINED)
    {
      clName = valClass.getName();
      int tipo = checkForType(clName);
      if(tipo == PDT_UNDEFINED)
        log.debug("Warning: tipo indefinito colonna " + getName() + " " + clName);

      setDataType(tipo);
    }
  }

  public Class getValClass()
  {
    return valClass;
  }

  abstract public Object getValue(Object bean)
     throws Exception;

  abstract public void setValue(Object bean, Object value)
     throws Exception;

  public String getValueAsString(Object bean)
     throws Exception
  {
    Object val = getValue(bean);
    return val == null ? "" : formatValue(val);
  }

  public void setFormatter(Format newFormatter)
  {
    formatter = newFormatter;
  }

  public Format getFormatter()
  {
    return formatter;
  }

  public void setAsciiPos(int newAsciiPos)
  {
    asciiPos = newAsciiPos;
  }

  public int getAsciiPos()
  {
    return asciiPos;
  }

  public void setAsciiLun(int newAsciiLun)
  {
    asciiLun = newAsciiLun;
  }

  public int getAsciiLun()
  {
    return asciiLun;
  }

  public String leggiAscii(String sLinea)
  {
    return sLinea.substring(asciiPos, asciiPos + asciiLun);
  }

  /**
   * Produce la stringa rappresentazione del dato.
   * Se la colonna ha un formattatore esplicito lo usa.
   */
  public String formatValue(Object value)
  {
    try
    {
      if(value == null)
        return "";

      if(formatter != null)
        return formatter.format(value);

      switch(dataType)
      {
        case PDT_BOOLEAN:
        case PDT_INTEGER:
        case PDT_STRINGKEY:
        case PDT_NUMBERKEY:
        case PDT_STRING:
        case PDT_FILE:
          break;
        case PDT_FLOAT:
        case PDT_DOUBLE:
          return SetupHolder.getNumberFormat().format(value);
        case PDT_MONEY:
          return SetupHolder.getValutaFormat().format(value);
        case PDT_DATE:
          return SetupHolder.getDateFormat().format(value);
        case PDT_TIMESTAMP_CMPDATEONLY:
        case PDT_TIMESTAMP_CMPHOURONLY:
        case PDT_TIMESTAMP_CMPTOSEC:
        case PDT_TIMESTAMP_CMPTOMIN:
        case PDT_TIMESTAMP:
          return SetupHolder.getDateTimeFormat().format(value);
        case PDT_TIME:
          return SetupHolder.getTimeFormat().format(value);
      }

      if(value instanceof Date)
        return SetupHolder.getDateFormat().format(value);

      return value.toString();
    }
    catch(Exception ex)
    {
      log.debug("[RigelColumnDescriptor.formatValue] value=" + value.toString() + " " + ex.getMessage());
      return "";
    }
  }

  /**
   * Produce una stringa del dato specicfica per la maschera di ricerca.
   * @param value
   * @return
   */
  public String formatValueRicerca(Object value)
  {
    if(value == null)
      return "";

    String s;
    switch(dataType)
    {
      case PDT_TIMESTAMP_CMPDATEONLY:
        return SetupHolder.getDateFormat().format(value);
      case PDT_TIMESTAMP_CMPHOURONLY:
        s = SetupHolder.getDateTimeFormat().format(value);
        return s.substring(0, s.length() - 6);
      case PDT_TIMESTAMP_CMPTOSEC:
        return SetupHolder.getDateTimeFormat().format(value);
      case PDT_TIMESTAMP_CMPTOMIN:
        s = SetupHolder.getDateTimeFormat().format(value);
        return s.substring(0, s.length() - 3);
    }

    return formatValue(value);
  }

  /**
   * Cerca di convertire il tipo in ingresso con un tipo
   * consono al tipo di dato della colonna.
   * ES: se la colonna e' PDT_DOUBLE e obj e' java.lang.Long
   * ritorna un java.lang.Double con il valore del Long.
   *
   * @param obj il dato in ingresso
   * @return il valore convertito
   */
  public Object convertiTipo(Object obj)
     throws Exception
  {
    return convertiTipo(dataType, obj);
  }

  public Object convertiTipo(int dataType, Object obj)
     throws Exception
  {
    // test per conversione non necessaria
    switch(dataType)
    {
      case PDT_BOOLEAN:
        if(obj instanceof Boolean)
          return obj;
      case PDT_INTEGER:
        if(obj instanceof Integer)
          return obj;
      case PDT_FLOAT:
        if(obj instanceof Float)
          return obj;
      case PDT_DOUBLE:
      case PDT_MONEY:
        if(obj instanceof Double)
          return obj;
      case PDT_DATE:
      case PDT_TIMESTAMP_CMPDATEONLY:
      case PDT_TIMESTAMP_CMPHOURONLY:
      case PDT_TIMESTAMP_CMPTOSEC:
      case PDT_TIMESTAMP_CMPTOMIN:
      case PDT_TIMESTAMP:
      case PDT_TIME:
        if(obj instanceof Date)
          return obj;
      case PDT_STRINGKEY:
        if(obj instanceof StringKey)
          return obj;
      case PDT_NUMBERKEY:
        if(obj instanceof NumberKey)
          return obj;
      case PDT_STRING:
      case PDT_FILE:
        if(obj instanceof String)
          return obj;
    }

    if(obj instanceof Number)
    {
      Number tmp = (Number) obj;

      switch(dataType)
      {
        case PDT_BOOLEAN:
          return tmp.longValue() != 0;
        case PDT_INTEGER:
          return tmp.intValue();
        case PDT_FLOAT:
          return tmp.floatValue();
        case PDT_DOUBLE:
        case PDT_MONEY:
          return tmp.doubleValue();
        case PDT_DATE:
        case PDT_TIMESTAMP_CMPDATEONLY:
        case PDT_TIMESTAMP_CMPHOURONLY:
        case PDT_TIMESTAMP_CMPTOSEC:
        case PDT_TIMESTAMP_CMPTOMIN:
        case PDT_TIMESTAMP:
        case PDT_TIME:
          return new Date();
        case PDT_STRINGKEY:
          return new StringKey(tmp.toString());
        case PDT_NUMBERKEY:
          return new NumberKey(tmp.longValue());
        case PDT_STRING:
        case PDT_FILE:
          return obj.toString();
      }

      return obj.toString();
    }

    if(obj instanceof java.lang.String)
    {
      String tmp = (String) obj;

      switch(dataType)
      {
        case PDT_BOOLEAN:
          return parseBoolean(tmp);
        case PDT_INTEGER:
          return parseInteger(tmp);
        case PDT_FLOAT:
          return parseFloat(tmp);
        case PDT_DOUBLE:
        case PDT_MONEY:
          return parseDouble(tmp);
        case PDT_DATE:
        case PDT_TIMESTAMP_CMPDATEONLY:
        case PDT_TIMESTAMP_CMPHOURONLY:
        case PDT_TIMESTAMP_CMPTOSEC:
        case PDT_TIMESTAMP_CMPTOMIN:
        case PDT_TIMESTAMP:
        case PDT_TIME:
          return new Date();
        case PDT_STRINGKEY:
          return new StringKey(tmp);
        case PDT_NUMBERKEY:
          return new NumberKey(tmp);
        case PDT_STRING:
        case PDT_FILE:
          return obj;
      }
    }

    // ritorna l'oggetto cosi' come' sperando che vada bene!
    return obj;
  }

  public Boolean parseBoolean(String val)
     throws Exception
  {
    return StringOper.checkTrue(val);
  }

  public Integer parseInteger(String val)
     throws Exception
  {
    return new Integer(val);
  }

  public Long parseLong(String val)
     throws Exception
  {
    return new Long(val);
  }

  public Short parseShort(String val)
     throws Exception
  {
    return new Short(val);
  }

  public Float parseFloat(String val)
     throws Exception
  {
    return ((Number) (SetupHolder.getNumberFormat().parseObject(val))).floatValue();
  }

  public Double parseDouble(String val)
     throws Exception
  {
    return ((Number) (SetupHolder.getNumberFormat().parseObject(val))).doubleValue();
  }

  public Double parseMoney(String val)
     throws Exception
  {
    return ((Number) (SetupHolder.getValutaFormat().parseObject(val))).doubleValue();
  }

  public Date parseDate(String val)
     throws Exception
  {
    return ((Date) (SetupHolder.getDateFormat().parseObject(val)));
  }

  public Date parseTime(String val)
     throws Exception
  {
    return ((Date) (SetupHolder.getTimeFormat().parseObject(val)));
  }

  public Date parseDateTime(String val)
     throws Exception
  {
    return ((Date) (SetupHolder.getDateTimeFormat().parseObject(val)));
  }

  /**
   * effettua parsing della stringa in accordo con il tipo di dato
   */
  public Object parseValueNull(String value)
  {
    if(value == null || value.trim().length() == 0)
      switch(dataType)
      {
        case PDT_BOOLEAN:
        case PDT_INTEGER:
        case PDT_FLOAT:
        case PDT_DOUBLE:
        case PDT_MONEY:
          break;
        case PDT_DATE:
        case PDT_TIMESTAMP_CMPDATEONLY:
        case PDT_TIMESTAMP_CMPHOURONLY:
        case PDT_TIMESTAMP_CMPTOSEC:
        case PDT_TIMESTAMP_CMPTOMIN:
        case PDT_TIMESTAMP:
        case PDT_TIME:
        case PDT_STRINGKEY:
        case PDT_NUMBERKEY:
        case PDT_STRING:
        case PDT_FILE:
          return null;
      }
    return parseValue(value);
  }

  /**
   * effettua parsing della stringa in accordo con il tipo di dato
   */
  public Object parseValue(String value)
  {
    try
    {
      if(formatter != null && value != null)
      {
        Object obj = formatter.parseObject(value);
        // converte il tipo ritornato in un tipo piu' appropriato
        return convertiTipo(obj);
      }

      boolean invalid = value == null || value.trim().length() == 0;

      switch(dataType)
      {
        case PDT_BOOLEAN:
          return invalid ? false : parseBoolean(value);
        case PDT_INTEGER:
        {
          String clName = getValClass().getName();
          if(clName.equals("java.lang.Integer"))
            return invalid ? new Integer(0) : parseInteger(value);
          if(clName.equals("java.lang.Short"))
            return invalid ? new Short((short) 0) : parseShort(value);
          if(clName.equals("java.lang.Long"))
            return invalid ? new Long((long) 0) : parseLong(value);
        }
        case PDT_FLOAT:
          return invalid ? new Float(0.0f) : parseFloat(value);
        case PDT_DOUBLE:
          return invalid ? new Double(0.0) : parseDouble(value);
        case PDT_MONEY:
          return invalid ? new Double(0.0) : parseMoney(value);
        case PDT_DATE:
          return invalid ? new Date() : parseDate(value);
        case PDT_TIMESTAMP_CMPDATEONLY:
        case PDT_TIMESTAMP_CMPHOURONLY:
        case PDT_TIMESTAMP_CMPTOSEC:
        case PDT_TIMESTAMP_CMPTOMIN:
        case PDT_TIMESTAMP:
          return invalid ? new Date() : parseDateTime(value);
        case PDT_TIME:
          return invalid ? new Date() : parseTime(value);
        case PDT_STRINGKEY:
          return invalid ? new StringKey() : new StringKey(value);
        case PDT_NUMBERKEY:
          return invalid ? new NumberKey() : new NumberKey(value);
        case PDT_STRING:
        case PDT_FILE:
          return invalid ? "" : value;
      }
      return value;
    }
    catch(Exception ex)
    {
      log.debug("[RigelColumnDescriptor.parseValue] value=" + value);
      log.error("RIGEL:", ex);
      return null;
    }
  }

  public String adjSqlValue(String val)
  {
    switch(dataType)
    {
      default:
      case PDT_BOOLEAN:
      case PDT_DATE:
      case PDT_TIMESTAMP_CMPDATEONLY:
      case PDT_TIMESTAMP_CMPHOURONLY:
      case PDT_TIMESTAMP_CMPTOSEC:
      case PDT_TIMESTAMP_CMPTOMIN:
      case PDT_TIMESTAMP:
      case PDT_TIME:
        // link non consentito per boolean o date
        return null;
      case PDT_INTEGER:
      case PDT_FLOAT:
      case PDT_DOUBLE:
      case PDT_MONEY:
      case PDT_NUMBERKEY:
        return val.trim();
      case PDT_STRINGKEY:
      case PDT_STRING:
      case PDT_FILE:
        return "'" + val.trim() + "'";
    }
  }

  public boolean isDate()
  {
    switch(dataType)
    {
      case PDT_DATE:
      case PDT_TIMESTAMP_CMPDATEONLY:
      case PDT_TIMESTAMP_CMPHOURONLY:
      case PDT_TIMESTAMP_CMPTOSEC:
      case PDT_TIMESTAMP_CMPTOMIN:
      case PDT_TIMESTAMP:
      case PDT_TIME:
        return true;
    }
    return false;
  }

  public boolean isNumeric()
  {
    switch(dataType)
    {
      case PDT_INTEGER:
      case PDT_FLOAT:
      case PDT_DOUBLE:
      case PDT_MONEY:
      case PDT_NUMBERKEY:
        return true;
    }
    return false;
  }

  public boolean isAlpha()
  {
    switch(dataType)
    {
      case PDT_STRINGKEY:
      case PDT_STRING:
      case PDT_FILE:
        return true;
    }
    return false;
  }

  public boolean isBoolean()
  {
    return dataType == PDT_BOOLEAN;
  }

  public boolean isComboSelf()
  {
    return comboRicercaCampoDisplay == null
       || comboRicercaCampoLink == null
       || comboRicercaTabella == null;
  }

  /**
   * Recupera valore foreign.
   * Quando un campo combo è in sola lettura, viene usata
   * questa funzione per recuperare il valore corrispondente
   * sulla tabella collegata, da visulizzare al posto del combo.
   * @param ptm data model con i dati
   * @param defVal valore da utilizzare come default
   * @return valore da visualizzare
   * @throws Exception
   */
  public String getValueComboAttached(int row, int col, RigelTableModel ptm, String defVal, RigelI18nInterface i18n)
     throws Exception
  {
    List lValues = ptm.getQuery().getDataComboColonnaAttached(row, col, ptm, this, i18n);

    String sOut = defVal;
    Iterator itrVal = lValues.iterator();
    while(itrVal.hasNext())
    {
      ForeignDataHolder fd = (ForeignDataHolder) itrVal.next();
      if(defVal != null && fd.codice.equals(defVal))
      {
        sOut = fd.descrizione;
        break;
      }
    }

    return sOut;
  }

  /**
   * Funzione di servizio per la generazione di un combo.
   * @param lValues lista di valori
   * @param defVal valore di default
   * @param removeZero the value of removeZero
   * @throws Exception
   * @return the java.lang.String
   */
  protected String getHtmlComboFromForeignData(List<ForeignDataHolder> lValues, String defVal, boolean removeZero)
     throws Exception
  {
    StringBuilder sOut = new StringBuilder(1024);
    defVal = StringOper.okStr(defVal, "0");

    for(ForeignDataHolder fd : lValues)
    {
      if(removeZero && fd.codice.equals("0"))
        continue;

      sOut.append(HtmlUtils.generaOptionCombo(fd.codice, fd.descrizione, fd.codice.equals(defVal)));
    }

    return sOut.toString();
  }

  /**
   * Restituisce HTML di un combo con valori foreign.
   * Per uso interno: usare piuttosto getHtmlForeignAutoCombo().
   * @param row the value of row
   * @param col the value of col
   * @param ptm data model con i dati
   * @param defVal valore da utilizzare come default
   * @param i18n the value of i18n
   * @param removeZero se vero il valore 0-Nessuno/indefinito non viene incluso
   * @throws Exception
   * @return the java.lang.String
   */
  public String getHtmlComboColonnaAttached(int row, int col,
     RigelTableModel ptm, String defVal, RigelI18nInterface i18n, boolean removeZero)
     throws Exception
  {
    List<ForeignDataHolder> lValues = ptm.getQuery().getDataComboColonnaAttached(row, col, ptm, this, i18n);
    return getHtmlComboFromForeignData(lValues, defVal, removeZero);
  }

  /**
   * Restituisce un auto combo.
   * I valori vengono estratti con una SELECT DISTINCT dalla
   * tabella e colonna indicata.
   * @param ptm data model con i dati
   * @param nomeTabella tabella con i valori
   * @param nomeCampo colonna con i valori
   * @param defVal valore da utilizzare come default
   * @return HTML interno del combo (solo le option)
   * @throws Exception
   */
  public String getHtmlComboColonnaSelf(RigelTableModel ptm,
     String nomeTabella, String nomeCampo,
     String defVal)
     throws Exception
  {
    if(lForeignValues == null)
    {
      List<ForeignDataHolder> lValues = ptm.getQuery().getDataComboColonnaSelf(this, nomeTabella, nomeCampo);
      return getHtmlComboFromForeignData(lValues, defVal, false);
    }

    return getHtmlComboFromForeignData(lForeignValues, defVal, false);
  }

  public void setValueAscii(Object bean, String s)
     throws Exception
  {
    setValue(bean, parseValue(s));
  }

  public void setValueAsciiLinea(Object bean, String sLinea)
     throws Exception
  {
    setValueAscii(bean, leggiAscii(sLinea));
  }

  public void setHtmlAlign(int newHtmlAlign)
  {
    htmlAlign = newHtmlAlign;
  }

  public int getHtmlAlign()
  {
    return htmlAlign;
  }

  public void setFormControl(JComponent newFormControl)
  {
    formControl = newFormControl;
  }

  public JComponent getFormControl()
  {
    return formControl;
  }

  /**
   * Attiva la ricerca con combo automatico.
   * Viene eseguita una query sulla stessa tabella
   * evidenziando tutti i possibili valori per questo
   * campo. I risultati sono inseriti in un combo
   * nella maschera di ricerca.
   */
  public void AttivaComboSelf()
  {
    comboRicerca = true;
    comboRicercaTabella = null;
    comboRicercaCampoLink = null;
    comboRicercaCampoDisplay = null;
  }

  /**
   * Attiva esplicitamente la visualizzazione con combo.
   * Il combo viene visualizzato sia nel form che eventualmente nella ricerca.
   * @param tabella tabella collegata dove prelevare i valori
   * @param link campo su tabella collegata da usare come link
   * @param display campo su tabella collegata da visualizzare
   */
  public void AttivaCombo(String tabella, String link, String display)
  {
    comboDisplay = true;
    comboRicerca = true;
    comboRicercaTabella = tabella;
    comboRicercaCampoLink = link;
    comboRicercaCampoDisplay = display;
  }

  /**
   * Attiva la visulizzazione con combo solo nella ricerca.
   * @param tabella tabella collegata dove prelevare i valori
   * @param link campo su tabella collegata da usare come link
   * @param display campo su tabella collegata da visualizzare
   */
  public void AttivaComboRicerca(String tabella, String link, String display)
  {
    comboRicerca = true;
    comboRicercaTabella = tabella;
    comboRicercaCampoLink = link;
    comboRicercaCampoDisplay = display;
  }

  /**
   * Attiva la modalità foreign.
   * @param mode deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT
   * @param tabella tabella collegata dove prelevare i valori
   * @param link campo su tabella collegata da usare come link
   * @param display campo su tabella collegata da visualizzare
   * @throws Exception
   */
  public void AttivaForeignMode(int mode, String tabella, String link, String display)
     throws Exception
  {
    if(mode == DISP_FLD_EDIT || mode == DISP_DESCR_EDIT || mode == DISP_DESCR_EDIT_ALTERNATE)
      throw new InvalidForeignModeException("Modo non consentito: per l'editing devi specifiare anche una url di edit.");

    foreignMode = mode;
    foreignTabella = tabella;
    foreignCampoLink = link;
    foreignCampoDisplay = display;
  }

  /**
   * Attiva la modalità foreign.
   * @param mode deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT
   * @param tabella tabella collegata dove prelevare i valori
   * @param link campo su tabella collegata da usare come link
   * @param display campo su tabella collegata da visualizzare
   * @throws Exception
   */
  public void AttivaForeignModeAlternate(int mode, String tabella, String link, String alternateLink, String display)
     throws Exception
  {
    if(mode != DISP_DESCR_EDIT_ALTERNATE || mode != DISP_FLD_DESCR_ALTERNATE)
      throw new InvalidForeignModeException("Solo per modalità con codice alternativo.");

    foreignMode = mode;
    foreignTabella = tabella;
    foreignCampoLink = link;
    foreignCampoAlternateLink = alternateLink;
    foreignCampoDisplay = display;
  }

  /**
   * Attiva la modalità foreign.
   * @param mode deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT
   * @param tabella tabella collegata dove prelevare i valori
   * @param link campo su tabella collegata da usare come link
   * @param display campo su tabella collegata da visualizzare
   * @param urlEdit specifica url da utilizzare per l'edit del record collegato su tabella foreign
   * @throws Exception
   */
  public void AttivaForeignMode(int mode, String tabella, String link, String display, String urlEdit)
     throws Exception
  {
    if(mode != DISP_FLD_EDIT && mode != DISP_DESCR_EDIT)
      throw new InvalidForeignModeException("Modo non consentito: deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT.");

    foreignMode = mode;
    foreignTabella = tabella;
    foreignCampoLink = link;
    foreignCampoDisplay = display;
    foreignEditUrl = StringOper.okStrNull(urlEdit);
    if(foreignEditUrl == null)
      foreignAutoCombo = true;
  }

  /**
   * Attiva la modalità foreign.
   * @param mode deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT
   * @param tabella tabella collegata dove prelevare i valori
   * @param link campo su tabella collegata da usare come link
   * @param display campo su tabella collegata da visualizzare
   * @param urlEdit specifica url da utilizzare per l'edit del record collegato su tabella foreign
   * @throws Exception
   */
  public void AttivaForeignModeAlternate(int mode, String tabella, String link, String alternateLink, String display, String urlEdit)
     throws Exception
  {
    if(mode != DISP_DESCR_EDIT_ALTERNATE)
      throw new InvalidForeignModeException("Modo non consentito: deve essere DISP_DESCR_EDIT_ALTERNATE.");

    foreignMode = mode;
    foreignTabella = tabella;
    foreignCampoLink = link;
    foreignCampoAlternateLink = alternateLink;
    foreignCampoDisplay = display;
    foreignEditUrl = StringOper.okStrNull(urlEdit);
    if(foreignEditUrl == null)
      foreignAutoCombo = true;
  }

  /**
   * Attiva la modalità foreign con dati espliciti.
   * @param mode modo di visualizzazione (vedi DISP_...)
   * @param foreignValues lista di valori possibili da visualizzare in un combo
   * @throws Exception
   */
  public void AttivaForeignMode(int mode, List<ForeignDataHolder> foreignValues)
     throws Exception
  {
    if(mode == DISP_DESCR_EDIT_ALTERNATE || mode == DISP_FLD_DESCR_ALTERNATE)
      throw new InvalidForeignModeException("Modo non consentito: non ha senso con lista dati esterni.");

    foreignMode = mode;
    foreignAutoCombo = true;
    lForeignValues = foreignValues;

    // nessun dato collegato dal db
    foreignTabella = null;
    foreignCampoLink = null;
    foreignCampoDisplay = null;
  }

  /**
   * Attiva la modalità foreign automatica.
   * Vengono utilizzate le informazioni dei tablemap per determinare
   * automaticamente la tabella e il campo di link da utilizzare.
   * Il campo foreign da visualizzare può essere specificato esplicitamente
   * con setForeignCampoDisplay() ma se null viene ricavato cercando
   * nella tabella foreign una serie di campi specificati nel SetupHolder.
   * @param mode deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT
   * @throws Exception
   */
  public void AttivaForeignModeAuto(int mode)
     throws Exception
  {
    if(mode == DISP_FLD_EDIT || mode == DISP_DESCR_EDIT || mode == DISP_DESCR_EDIT_ALTERNATE)
      throw new InvalidForeignModeException("Modo non consentito: per l'editing devi specifiare anche una url di edit.");

    foreignAuto = true;
    foreignMode = mode;
  }

  /**
   * Attiva la modalità foreign automatica.
   * @param mode deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT
   * @param urlEdit specifica url da utilizzare per l'edit del record collegato su tabella foreign
   * @throws Exception
   */
  public void AttivaForeignModeAuto(int mode, String urlEdit)
     throws Exception
  {
    if(mode != DISP_FLD_EDIT && mode != DISP_DESCR_EDIT)
      throw new InvalidForeignModeException("Modo non consentito: deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT.");

    foreignAuto = true;
    foreignMode = mode;
    foreignEditUrl = StringOper.okStrNull(urlEdit);
    if(foreignEditUrl == null)
      foreignAutoCombo = true;
  }

  /**
   * Attiva la modalità foreign automatica.
   * @param mode deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT
   * @param urlEdit specifica url da utilizzare per l'edit del record collegato su tabella foreign
   * @throws Exception
   */
  public void AttivaForeignModeAutoAlternate(int mode, String alternateLink, String urlEdit)
     throws Exception
  {
    if(mode != DISP_DESCR_EDIT_ALTERNATE)
      throw new InvalidForeignModeException("Modo non consentito: deve essere DISP_DESCR_EDIT_ALTERNATE.");

    foreignAuto = true;
    foreignMode = mode;
    foreignCampoAlternateLink = alternateLink;
    foreignEditUrl = StringOper.okStrNull(urlEdit);
    if(foreignEditUrl == null)
      foreignAutoCombo = true;
  }

  public boolean isForeignAlternate()
  {
    return foreignMode == DISP_DESCR_EDIT_ALTERNATE || foreignMode == DISP_FLD_DESCR_ALTERNATE;
  }

  public boolean isForeignFromDatabase()
  {
    return foreignAuto || (foreignTabella != null && foreignCampoLink != null);
  }

  public boolean isForeignAuto()
  {
    return foreignAuto;
  }

  protected void setForeignAuto(boolean foreignAuto)
  {
    this.foreignAuto = foreignAuto;
  }

  public void setEscludiRicerca(boolean escludiRicerca)
  {
    this.escludiRicerca = escludiRicerca;
  }

  public boolean isEscludiRicerca()
  {
    return escludiRicerca;
  }

  protected void setComboDisplay(boolean comboDisplay)
  {
    this.comboDisplay = comboDisplay;
  }

  public boolean isComboDisplay()
  {
    return comboDisplay;
  }

  protected void setComboRicerca(boolean newComboRicerca)
  {
    comboRicerca = newComboRicerca;
  }

  public boolean isComboRicerca()
  {
    return comboRicerca;
  }

  public void setComboRicercaTabella(String newComboRicercaTabella)
  {
    comboRicercaTabella = newComboRicercaTabella;
  }

  public String getComboRicercaTabella()
  {
    return comboRicercaTabella;
  }

  public void setComboRicercaCampoDisplay(String newComboRicercaCampoDisplay)
  {
    comboRicercaCampoDisplay = newComboRicercaCampoDisplay;
  }

  public String getComboRicercaCampoDisplay()
  {
    return comboRicercaCampoDisplay;
  }

  public void setComboRicercaCampoLink(String newComboRicercaCampoLink)
  {
    comboRicercaCampoLink = newComboRicercaCampoLink;
  }

  public String getComboRicercaCampoLink()
  {
    return comboRicercaCampoLink;
  }

  public void setFiltroTipo(int newFiltroTipo)
  {
    filtroTipo = newFiltroTipo;
  }

  public int getFiltroTipo()
  {
    return filtroTipo;
  }

  public void setFiltroValore(String newFiltroValore)
  {
    filtroValore = newFiltroValore;
  }

  public String getFiltroValore()
  {
    return filtroValore;
  }

  public void setFiltroSort(int newFiltroSort)
  {
    filtroSort = newFiltroSort;
  }

  public int getFiltroSort()
  {
    return filtroSort;
  }

  public void setHiddenEdit(boolean newHiddenEdit)
  {
    hiddenEdit = newHiddenEdit;
  }

  public boolean isHiddenEdit()
  {
    return hiddenEdit;
  }

  public void setHtmlPara(boolean newHtmlPara)
  {
    htmlPara = newHtmlPara;
  }

  public boolean isHtmlPara()
  {
    return htmlPara;
  }

  public void setCalcolato(boolean newCalcolato)
  {
    calcolato = newCalcolato;
  }

  public boolean isCalcolato()
  {
    return calcolato;
  }

  public boolean isEnableCache()
  {
    return enableCache;
  }

  public void setEnableCache(boolean enableCache)
  {
    this.enableCache = enableCache;
  }

  public void setDataType(int dataType)
  {
    this.dataType = dataType;
  }

  public int getDataType()
  {
    return dataType;
  }

  public void setHtmlSpan(int htmlSpan)
  {
    this.htmlSpan = htmlSpan;
  }

  public int getHtmlSpan()
  {
    return htmlSpan;
  }

  public void setForeignCampoDisplay(String foreignCampoDisplay)
  {
    this.foreignCampoDisplay = foreignCampoDisplay;
  }

  public String getForeignCampoDisplay()
  {
    return foreignCampoDisplay;
  }

  public void setForeignCampoLink(String foreignCampoLink)
  {
    this.foreignCampoLink = foreignCampoLink;
  }

  public String getForeignCampoLink()
  {
    return foreignCampoLink;
  }

  public void setForeignTabella(String foreignTabella)
  {
    this.foreignTabella = foreignTabella;
  }

  public String getForeignTabella()
  {
    return foreignTabella;
  }

  public String getForeignCampoAlternateLink()
  {
    return foreignCampoAlternateLink;
  }

  public void setForeignCampoAlternateLink(String foreignCampoAlternateLink)
  {
    this.foreignCampoAlternateLink = foreignCampoAlternateLink;
  }

  /**
   * Imposta modalità foreign.
   * Provoca un reset di tutti i dati foreign.
   * <code>
   * foreignCampoDisplay = null;
   * foreignCampoLink = null;
   * foreignTabella = null;
   * foreignAuto = false;
   * foreignEditUrl = null;
   * foreignFormUrl = null;
   * foreignAutoCombo = false;
   * </code>
   * @param foreignMode
   */
  public void setForeignMode(int foreignMode)
  {
    this.foreignMode = foreignMode;
    if(foreignMode == DISP_FLD_ONLY)
    {
      foreignCampoDisplay = null;
      foreignCampoLink = null;
      foreignTabella = null;
      foreignAuto = false;
      foreignEditUrl = null;
      foreignFormUrl = null;
      foreignAutoCombo = false;
    }
  }

  public int getForeignMode()
  {
    return foreignMode;
  }

  public void setForeignEditUrl(String foreignEditUrl)
  {
    this.foreignEditUrl = foreignEditUrl;
  }

  public String getForeignEditUrl()
  {
    return foreignEditUrl;
  }

  public void setCaratteristicheSelezioneRiga(boolean caratteristicheSelezioneRiga)
  {
    this.caratteristicheSelezioneRiga = caratteristicheSelezioneRiga;
  }

  public boolean isCaratteristicheSelezioneRiga()
  {
    return caratteristicheSelezioneRiga;
  }

  public void setCaratteristicheEditRiga(boolean caratteristicheEditRiga)
  {
    this.caratteristicheEditRiga = caratteristicheEditRiga;
  }

  public boolean isCaratteristicheEditRiga()
  {
    return caratteristicheEditRiga;
  }

  public void setCaratteristicheCancellaRiga(boolean caratteristicheCancellaRiga)
  {
    this.caratteristicheCancellaRiga = caratteristicheCancellaRiga;
  }

  public boolean isCaratteristicheCancellaRiga()
  {
    return caratteristicheCancellaRiga;
  }

  /**
   * Ritorna vero se questa colonna ha almeno una caratteristica.
   * @return
   */
  public boolean isCaratteristiche()
  {
    return caratteristicheSelezioneRiga
       || caratteristicheEditRiga
       || caratteristicheCancellaRiga
       || isCaratteristicheCBut();
  }

  public void setFixedText(String fixedText)
  {
    this.fixedText = fixedText;
  }

  public String getFixedText()
  {
    return fixedText;
  }

  public void setTestfortype(boolean testfortype)
  {
    this.testfortype = testfortype;
  }

  public boolean isTestfortype()
  {
    return testfortype;
  }

  public void setTestfornull(boolean testfornull)
  {
    this.testfornull = testfornull;
  }

  public boolean isTestfornull()
  {
    return testfornull;
  }

  public boolean isTestforzero()
  {
    return testforzero;
  }

  public void setTestforzero(boolean testforzero)
  {
    this.testforzero = testforzero;
  }

  public boolean isTestforcodice()
  {
    return testforcodice;
  }

  public void setTestforcodice(boolean testforcodice)
  {
    this.testforcodice = testforcodice;
  }

  public void setExtraScript(String extraScript)
  {
    this.extraScript = extraScript;
  }

  public String getExtraScript()
  {
    return extraScript;
  }

  public void setPrimaryKey(boolean primaryKey)
  {
    this.primaryKey = primaryKey;
  }

  public boolean isPrimaryKey()
  {
    return primaryKey;
  }

  public void setComboExtraWhere(String comboExtraWhere, boolean removeZero)
  {
    this.comboExtraWhere = comboExtraWhere;
    this.comboExtraWhereRemoveZero = removeZero;
  }

  public String getComboExtraWhere()
  {
    return comboExtraWhere;
  }

  public boolean isComboExtraWhereRemoveZero()
  {
    return comboExtraWhereRemoveZero;
  }

  public void setHtmlStyle(String htmlStyle)
  {
    this.htmlStyle = htmlStyle;
  }

  public String getHtmlStyle()
  {
    return htmlStyle;
  }

  public void setAggregatoSql(boolean aggregatoSql)
  {
    this.aggregatoSql = aggregatoSql;
  }

  public boolean isAggregatoSql()
  {
    return aggregatoSql;
  }

  public void setDefVal(String defVal)
  {
    this.defVal = defVal;
  }

  public String getDefVal()
  {
    return defVal;
  }

  public void setDefValParam(String defValParam)
  {
    this.defValParam = defValParam;
  }

  public String getDefValParam()
  {
    return defValParam;
  }

  public void setRicercaSemplice(int ricercaSemplice)
  {
    this.ricercaSemplice = ricercaSemplice;
  }

  public int getRicercaSemplice()
  {
    return ricercaSemplice;
  }

  public CustomColumnEdit getColedit()
  {
    return coledit;
  }

  public void setColedit(CustomColumnEdit coledit)
  {
    this.coledit = coledit;
  }

  //////////////////////////////////////////////////////////////////
  /**
   * Aggiunge un pulsante personalizzato a questa colonna
   *
   * @param ci un descrittore del pulsante (icona) personalizzato
   */
  public void addCustomButton(CustomButtonInfo ci)
  {
    customButtons.add(ci);
  }

  public boolean isCaratteristicheCBut()
  {
    return !customButtons.isEmpty();
  }

  public int getNumCustomButtons()
  {
    return customButtons.size();
  }

  public CustomButtonInfo getCustomButton(int button)
  {
    return customButtons.get(button);
  }

  public void setTestforcf(boolean testforcf)
  {
    this.testforcf = testforcf;
  }

  public boolean isTestforcf()
  {
    return testforcf;
  }

  public void setTestforpi(boolean testforpi)
  {
    this.testforpi = testforpi;
  }

  public boolean isTestforpi()
  {
    return testforpi;
  }

  public void setTestcustom(String testcustom)
  {
    this.testcustom = testcustom;
  }

  public String getTestcustom()
  {
    return testcustom;
  }

  public boolean isTestrange()
  {
    return testrange;
  }

  public void setTestrange(boolean testrange)
  {
    this.testrange = testrange;
  }

  public String getTestrangemin()
  {
    return testrangemin;
  }

  public void setTestrangemin(String testrangemin)
  {
    this.testrangemin = testrangemin;
  }

  public String getTestrangemax()
  {
    return testrangemax;
  }

  public void setTestrangemax(String testrangemax)
  {
    this.testrangemax = testrangemax;
  }

  public void setAutoIncremento(boolean autoIncremento)
  {
    this.autoIncremento = autoIncremento;
  }

  public boolean isAutoIncremento()
  {
    return autoIncremento;
  }
  ////////////////////////////////////////////////////////////

  public void setForeignFormUrl(String foreignFormUrl)
  {
    this.foreignFormUrl = foreignFormUrl;
  }

  public String getForeignFormUrl()
  {
    return foreignFormUrl;
  }

  /**
   * Aggiunge parametri foreing.
   * @param paramName nome del parametro
   * @param paramValue valore (può contenere macro)
   */
  public void addForeignFormParam(String paramName, String paramValue)
  {
    foreignFormParams.put(paramName, paramValue);
  }

  /**
   * Formattazione dei parametri per foreign form.
   * Gli eventuali parametri specificati per il foreign form
   * vengono analizzati e sostituite le macro con i corrispettivi
   * valori. I nomi e i valori sono sottoposti a encoding
   * consentendo di utilizzarli direttamente nella chiamata
   * al browser.
   * @param row riga per cui si chiede la risoluzione
   * @param ptm table model con i dati
   * @return i parametri sotto forma di stringa (&nome=valore&nome=valore...)
   * @throws Exception
   */
  public String makeForeignFormParamsForUrl(int row, RigelTableModel ptm)
     throws Exception
  {
    if(foreignFormParams.isEmpty())
      return null;

    // NOTA: se uno dei parametri non e' valido tutta l'url non e' valida
    String param = "";
    Enumeration enumKeys = foreignFormParams.keys();
    while(enumKeys.hasMoreElements())
    {
      String nome = (String) enumKeys.nextElement();
      if(!StringOper.isOkStr(nome))
        return null;

      String valore = (String) foreignFormParams.get(nome);
      if(!StringOper.isOkStr(valore))
        return null;

      valore = ptm.getValueMacro(row, -1, valore);
      if(!StringOper.isOkStr(valore))
        return null;

      param += "&" + URLEncoder.encode(nome, "UTF-8") + "=" + URLEncoder.encode(valore, "UTF-8");
    }

    return param.substring(1);
  }

  /**
   * Recupera numero di record su tabella foreign.
   * Esegue un conteggio dei record sulla tabella foreign
   * usato principalmente per determinare se utilizzare o
   * meno una visualizzazione combo.
   * Il dato viene salvato in cache.
   * @param ptm table model con i dati
   * @return numero dei record
   * @throws Exception
   */
  public long getForeignValuesCount(int row, int col, RigelTableModel ptm)
     throws Exception
  {
    return ptm.getQuery().estimateForeignDataList(row, col, ptm, this);
  }

  /**
   * Verifica se questa colonna deve essere visualizzata come combo.
   * @param ptm table model con i dati
   * @return vero se richiesta visualizzazione combo
   * @throws Exception
   */
  public boolean useForeignAutoCombo(int row, int col, RigelTableModel ptm)
     throws Exception
  {
    if(!foreignAutoCombo)
      return false;

    if(foreignMode != RigelColumnDescriptor.DISP_FLD_EDIT
       && foreignMode != RigelColumnDescriptor.DISP_DESCR_EDIT
       && foreignMode != RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE)
      return false;

    // se non è indicata la foreignEditUrl siamo obbligati
    // ad usare un combo box per la selezione del valore
    if(foreignEditUrl == null)
      return true;

    if(!SetupHolder.isForeignAutoCombo())
      return false;

    if(isForeignFromDatabase() && getForeignValuesCount(row, col, ptm) > SetupHolder.getAutoComboLimit())
      return false;

    return true;
  }

  public boolean isForeignAutoCombo()
  {
    return foreignAutoCombo;
  }

  /**
   * Imposta esplicitamente la visualizzazione come combo.
   * @param foreignAutoCombo vero se richiesto combo
   */
  public void setForeignAutoCombo(boolean foreignAutoCombo)
  {
    this.foreignAutoCombo = foreignAutoCombo;
  }

  /**
   * Recupera dati foreign.
   * Cerca nella cache dei dati foreign il valore identificato da key.
   * @param key valore del campo codice da cercare
   * @return il dato in cache o null se non presente
   */
  public ForeignDataHolder findHTableForeign(String key, RigelTableModel ptm, RigelI18nInterface i18n)
     throws Exception
  {
    // in caso di foreign da db i dati sono comunque in cache quindi ripetiamo la query quanto vogliamo
    if(isForeignFromDatabase())
      lForeignValues = ptm.getQuery().getForeignDataListAll(0, 0, ptm, this, i18n);

    if(lForeignValues != null)
      for(ForeignDataHolder f : lForeignValues)
      {
        if(StringOper.isEqu(key, f.codice))
          return f;
      }

    return null;
  }

  /**
   * Recupera dati foreign utilizzando il codice alternativo.
   * Cerca nella cache dei dati foreign il valore identificato da key.
   * @param key valore del campo codice da cercare
   * @return il dato in cache o null se non presente
   */
  public ForeignDataHolder findHTableForeignAlternate(String key, RigelTableModel ptm, RigelI18nInterface i18n)
     throws Exception
  {
    // in caso di foreign da db i dati sono comunque in cache quindi ripetiamo la query quanto vogliamo
    if(isForeignFromDatabase())
      lForeignValues = ptm.getQuery().getForeignDataListAll(0, 0, ptm, this, i18n);

    if(lForeignValues != null)
      for(ForeignDataHolder f : lForeignValues)
      {
        if(StringOper.isEqu(key, f.alternateCodice))
          return f;
      }

    return null;
  }

  /**
   * Recupera dati foreign.
   * @param ptm table model con i dati
   * @return lista di oggetti con i dati collegati
   * @throws Exception
   */
  public List<ForeignDataHolder> getForeignValues(RigelTableModel ptm, RigelI18nInterface i18n)
     throws Exception
  {
    // in caso di foreign da db i dati sono comunque in cache quindi ripetiamo la query quanto vogliamo
    if(isForeignFromDatabase())
      lForeignValues = ptm.getQuery().getForeignDataList(0, 0, ptm, this, i18n);

    return lForeignValues;
  }

  /**
   * Costruisce HTML del combo.
   * Ritorna il combo per la selezione del dato foreign.
   * @param row riga di riferimento
   * @param col colonna di riferimento
   * @param ptm data model con i dati
   * @param defVal valore da utilizzare come default
   * @return HTML interno del combo (solo le option)
   * @throws Exception
   */
  public String getHtmlForeignAutoCombo(int row, int col, RigelTableModel ptm, String defVal, RigelI18nInterface i18n)
     throws Exception
  {
    if(getComboExtraWhere() == null || !isForeignFromDatabase())
    {
      // non ci sono extra-where quindi il set di foreign values non cambia
      return getHtmlComboFromForeignData(getForeignValues(ptm, i18n), defVal, false);
    }
    else
    {
      // attenzione: qui non possiamo mettere in cache i risultati, in quanto
      // nella query per i foreign values potrebbero essere presenti delle macro
      // il cui valore cambia a seconda della riga e quindi può cambiare anche
      // la lista dei valori foreign da cui viene estratto il combo
      List<ForeignDataHolder> localForeignValues = ptm.getQuery().getForeignDataList(row, col, ptm, this, i18n);
      return getHtmlComboFromForeignData(localForeignValues, defVal, isComboExtraWhereRemoveZero());
    }
  }

  /**
   * Risolutore di macro di colonna.
   * Una serie di macro con valori legati alla colonna sono supportate
   * durante la generazione di codice javascript o di url.
   * In questa funzione vengono risolte le macro con i corrispettivi
   * valori reali:
   * <ul>
   * <li>@foreignTabella: il nome della tabella foreign</li>
   * <li>@foreignCampoLink: il nome di campo collegamento nella tabella foreign</li>
   * <li>@foreignCampoDisplay: il nome del campo foreign da visualizzare</li>
   * <li>@comboRicercaTabella: nella ricerca con combo il nome della tabella</li>
   * <li>@comboRicercaCampoLink: nella ricerca con combo il nome del campo di collegamento</li>
   * <li>@comboRicercaCampoDisplay: nella ricerca con combo il nome del campo da visualizzare</li>
   * <li>@comboExtraWhere: nella ricerca con combo eventuali clausole where da aggiungere alla query</li>
   * <li>@colname: nome della colonna (nome del campo)</li>
   * <li>@colcaption: nome della caption della colonna (nome visto dall'utente)</li>
   * </ul>
   * @param input
   * @return
   */
  public String parseMacro(String input)
  {
    input = StringOper.strReplace(input, "@foreignCampoDisplay", foreignCampoDisplay);
    input = StringOper.strReplace(input, "@foreignCampoLink", foreignCampoLink);
    input = StringOper.strReplace(input, "@foreignTabella", foreignTabella);

    input = StringOper.strReplace(input, "@comboRicercaTabella", comboRicercaTabella);
    input = StringOper.strReplace(input, "@comboRicercaCampoDisplay", comboRicercaCampoDisplay);
    input = StringOper.strReplace(input, "@comboRicercaCampoLink", comboRicercaCampoLink);
    input = StringOper.strReplace(input, "@comboExtraWhere", comboExtraWhere);

    input = StringOper.strReplace(input, "@colname", name);
    input = StringOper.strReplace(input, "@colcaption", getHeaderValue().toString());

    return input;
  }

  @Override
  public String toString()
  {
    return getCaption() + " (" + getName() + ")";
  }

  public boolean isSuNuovaRiga()
  {
    return suNuovaRiga;
  }

  public void setSuNuovaRiga(boolean suNuovaRiga)
  {
    this.suNuovaRiga = suNuovaRiga;
  }
}
