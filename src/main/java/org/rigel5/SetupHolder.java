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

import java.text.*;
import org.commonlib5.utils.ClassOper;
import org.commonlib5.utils.StringOper;
import org.rigel5.db.ConnectionProducer;
import org.rigel5.db.sql.QueryBuilder;
import org.rigel5.table.html.HtmlMascheraRicercaGenerica;

/**
 * La classe SetupHolder mantiene una serie di settaggi
 * necessari al funzionamento di rigel.
 * Tutti i metodi sono statici e servono per consentire
 * all'applicazione ospite di impostare tutti i parametri
 * di setup utilizzati da rigel.
 *
 * @author Nicola De Nisco
 */
public class SetupHolder
{
  //
  // uri delle icone
  private static String imgLista = null;
  private static String imgSelItem = null;
  private static String imgEditItem = null;
  private static String imgEditData = null;
  private static String imgEditForeign = null;
  private static String imgFormForeign = null;
  private static String imgDeleteItem = null;
  //
  // formattatori vari
  private static Format dateFormat = null;
  private static Format timeFormat = null;
  private static Format dateTimeFormat = null;
  private static Format numberFormat = null;
  private static Format valutaFormat = null;
  /**
   * L'oggetto connection producer
   * è un produttore di connessioni SQL.
   */
  private static ConnectionProducer conProd = null;
  /**
   * Nome della classe del QueryBuilder.
   */
  private static String qryBldcname = null;
  /**
   * Numero massimo di colonne visualizzate
   * nella ricerca semplice.
   */
  private static int maxSiSeColumn = 3;
  /**
   * Nelle funzioni di generazione automatica dei combo box
   * (vedi foreign edit) è il il limite oltre il quale il combo
   * non viene più generato ma si usa il foreign edit normale.
   */
  private static int autoComboLimit = 50;
  /**
   * Limite della descrizione nei combo box.
   * La descrizione viene troncata se eccede questa dimensione.
   */
  private static int comboDescLimit = 40;
  /**
   * Quando usa l'auto combo per foreign edit inserisce
   * comunque una voce 0=Nessuno/non definito in cima al
   * combo-box anche se non appare fra i risultati collegati.
   * Questo è molto utile (e pressochè indispensabile)
   * se viene usata la proprietà comboExtraWhere della colonna.
   */
  private static boolean autoComboAlwaysHaveZero = true;
  /**
   * Per le colonne in auto foreign è l'elenco dei possibili
   * campi visualizzabili sulla tabella allegata.
   */
  private static String[] autoForeingColumns = null;
  /**
   * Personalizzatore delle url per edit, foreign, custom button, ecc.
   */
  private static RigelCustomUrlBuilder urlBuilder = null;
  /**
   * Gestore del look and feel per le funzioni di paginazione.
   */
  private static RigelUIManager uiManager = null;
  /**
   * Numero di colonne al di sopra delle quali l'attributo
   * size delle stesse viene ignorato, lasciando al browser
   * l'impaginazione ottimale. Se il valore è 0 questo comportamento
   * non viene attivato: il size rimane quello specificato.
   */
  private static int noSizeLimit = 5;
  /**
   * Gestore della cache di Rigel.
   */
  private static RigelCacheManager cacheManager = new NullCacheManager();
  /**
   * Gestore di internazionalizzazione di Rigel.
   */
  private static RigelI18nInterface ri18n = new DefaultRigelI18nImplementation();

  /* Default per protezione anti CSRF */
  private static boolean attivaProtezioneCSRF = true;

  /** costruttore maschera di ricerca */
  private static Class genricercalisteclass = HtmlMascheraRicercaGenerica.class;

  //////////////////////////////////////////////////////////////////
  //
  public static void setImgEditForeign(String _imgEditForeign)
  {
    imgEditForeign = _imgEditForeign;
  }

  public static String getImgEditForeign()
  {
    return imgEditForeign;
  }

  public static String getImgEditData()
  {
    return imgEditData;
  }

  public static void setImgEditData(String newimgEditData)
  {
    imgEditData = newimgEditData;
  }

  public static void setImgLista(String newimgLista)
  {
    imgLista = newimgLista;
  }

  public static String getImgLista()
  {
    return imgLista;
  }

  public static void setImgEditItem(String newimgEditItem)
  {
    imgEditItem = newimgEditItem;
  }

  public static String getImgEditItem()
  {
    return imgEditItem;
  }

  public static Format getDateFormat()
  {
    if(dateFormat == null)
      dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    return dateFormat;
  }

  public static void setDateFormat(Format _dateFormat)
  {
    dateFormat = _dateFormat;
  }

  public static Format getTimeFormat()
  {
    if(timeFormat == null)
      timeFormat = new SimpleDateFormat("HH:mm:ss");
    return timeFormat;
  }

  public static void setTimeFormat(Format _timeFormat)
  {
    timeFormat = _timeFormat;
  }

  public static Format getDateTimeFormat()
  {
    if(dateTimeFormat == null)
      dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    return dateTimeFormat;
  }

  public static void setDateTimeFormat(Format _dateTimeFormat)
  {
    dateTimeFormat = _dateTimeFormat;
  }

  public static Format getNumberFormat()
  {
    if(numberFormat == null)
      numberFormat = NumberFormat.getInstance();
    return numberFormat;
  }

  public static void setNumberFormat(Format _numberFormat)
  {
    numberFormat = _numberFormat;
  }

  public static Format getValutaFormat()
  {
    if(valutaFormat == null)
      valutaFormat = NumberFormat.getInstance();
    return valutaFormat;
  }

  public static void setValutaFormat(Format _valutaFormat)
  {
    valutaFormat = _valutaFormat;
  }

  public static ConnectionProducer getConProd()
  {
    return conProd;
  }

  public static void setConProd(ConnectionProducer _conProd)
  {
    conProd = _conProd;
  }

  public static QueryBuilder getQueryBuilder()
     throws Exception
  {
    if(qryBldcname == null)
      throw new Exception("Nessuna definizione di QueryBuilder.");

    Class clqb = ClassOper.loadClass(qryBldcname, "org.rigel5.db.sql", null);

    if(clqb != null)
      return (QueryBuilder) clqb.newInstance();

    throw new ClassNotFoundException("La classe " + qryBldcname
       + " non e' definita o non e' derivata da org.rigel5.db.sql.QueryBuilder.");
  }

  public static void setQueryBuilderClassName(String s)
  {
    qryBldcname = s;
  }

  public static String getQueryBuilderClassName()
  {
    return qryBldcname;
  }

  public static void setImgFormForeign(String _imgFormForeign)
  {
    imgFormForeign = _imgFormForeign;
  }

  public static String getImgFormForeign()
  {
    return imgFormForeign;
  }

  public static int getMaxSiSeColumn()
  {
    return maxSiSeColumn;
  }

  public static void setMaxSiSeColumn(int aMaxSiSeColumn)
  {
    maxSiSeColumn = aMaxSiSeColumn;
  }

  public static int getAutoComboLimit()
  {
    return autoComboLimit;
  }

  public static void setAutoComboLimit(int aAutoComboLimit)
  {
    autoComboLimit = aAutoComboLimit;
  }

  public static boolean isForeignAutoCombo()
  {
    return autoComboLimit != 0;
  }

  public static boolean isAutoComboAlwaysHaveZero()
  {
    return autoComboAlwaysHaveZero;
  }

  public static void setAutoComboAlwaysHaveZero(boolean autoComboAlwaysHaveZero)
  {
    SetupHolder.autoComboAlwaysHaveZero = autoComboAlwaysHaveZero;
  }

  public static void setAutoForeingColumns(String columns)
  {
    autoForeingColumns = StringOper.split(columns, ',');
  }

  public static String[] getAutoForeingColumns()
  {
    return autoForeingColumns;
  }

  public static void setAutoForeingColumns(String[] autoForeingColumns)
  {
    SetupHolder.autoForeingColumns = autoForeingColumns;
  }

  public static RigelCustomUrlBuilder getUrlBuilder()
  {
    return urlBuilder;
  }

  public static void setUrlBuilder(RigelCustomUrlBuilder urlBuilder)
  {
    SetupHolder.urlBuilder = urlBuilder;
  }

  public static String getImgSelItem()
  {
    return imgSelItem;
  }

  public static void setImgSelItem(String imgSelItem)
  {
    SetupHolder.imgSelItem = imgSelItem;
  }

  public static String getImgDeleteItem()
  {
    return imgDeleteItem;
  }

  public static void setImgDeleteItem(String imgDeleteItem)
  {
    SetupHolder.imgDeleteItem = imgDeleteItem;
  }

  public static RigelUIManager getUiManager()
  {
    return uiManager;
  }

  public static void setUiManager(RigelUIManager uiManager)
  {
    SetupHolder.uiManager = uiManager;
  }

  public static int getNoSizeLimit()
  {
    return noSizeLimit;
  }

  public static void setNoSizeLimit(int noSizeLimit)
  {
    SetupHolder.noSizeLimit = noSizeLimit;
  }

  public static RigelCacheManager getCacheManager()
  {
    return cacheManager;
  }

  public static void setCacheManager(RigelCacheManager cacheManager)
  {
    SetupHolder.cacheManager = cacheManager;
  }

  public static RigelI18nInterface getRi18n()
  {
    return ri18n;
  }

  public static void setRi18n(RigelI18nInterface ri18n)
  {
    SetupHolder.ri18n = ri18n;
  }

  public static int getComboDescLimit()
  {
    return comboDescLimit;
  }

  public static void setComboDescLimit(int comboDescLimit)
  {
    SetupHolder.comboDescLimit = comboDescLimit;
  }

  public static boolean isAttivaProtezioneCSRF()
  {
    return attivaProtezioneCSRF;
  }

  public static void setAttivaProtezioneCSRF(boolean v)
  {
    attivaProtezioneCSRF = v;
  }

  public static Class getGenricercalisteclass()
  {
    return genricercalisteclass;
  }

  public static void setGenricercalisteclass(Class cls)
  {
    genricercalisteclass = cls;
  }
}
