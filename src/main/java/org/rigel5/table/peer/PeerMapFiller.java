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

import java.util.*;
import org.commonlib5.utils.StringOper;
import org.rigel5.DefaultRigelI18nImplementation;
import org.rigel5.RigelI18nInterface;
import org.rigel5.table.ForeignDataHolder;
import org.rigel5.table.MascheraRicercaGenerica;
import org.rigel5.table.RigelColumnDescriptor;

/**
 * Convertitore di oggetti e array di oggetti in tabelle di hashing
 * e viceversa.
 * In effetti per questo scopo e' molto adatta la PeerAbstractTableModel
 * che implementa gia' dei concetti di marshalling per i peer.
 * Estendiamo questa classe e sfruttiamo l'infrastruttura esistente.
 *
 * Questa classe e' stata originarimente sviluppata per
 * consentire di trasferire oggetti via XmlRpc.
 * Il convertitore puo' essere usato in due modi: indicando espilicitamente
 * i campi che si vogliono trasferire; per questa funzionalita' occorre
 * aggiungere una colonna con il metodo addColumn per ogni campo di
 * interesse. La generazione e la ricostruzione opereranno solo sui
 * campi indicati.
 * Modalita' automatica generica: utilizzando i due metodi statici
 * un oggetto qualsiasi viene scomposto e ricomposto usando tecniche
 * di introspezione dei bean per identificare i campi trasferibili.
 * E' possibile generare Hashtable con il primo metodo e creare
 * gli oggetti con il secondo.
 *
 * Il nome delle chiavi nelle hashtable viene determinato da tipoChiave.
 * Secondo le costanti definite la chiave può essere:
 * <ul>
 * <li>TIPO_CHIAVE_CAPTION: la caption della colonna</li>
 * <li>TIPO_CHIAVE_ROW_COL: una stringa nella forma ROW_?_COL_? con i numeri di riga e colonna</li>
 * <li>TIPO_CHIAVE_ROW_CAPTION: una stringa nella forma ROW_?_caption</li>
 * </ul>
 *
 * NOTA:
 * Vengono trasferiti solo i campi che hanno sia un getter che un setter
 * e manipolano dati dei seguenti tipi:
 * int, float, double, String, Date, StringKey, NumberKey
 *
 * @author Nicola De Nisco
 */
public class PeerMapFiller extends PeerAbstractTableModel
{
  protected boolean ignoreErrors = false;
  protected int tipoChiave = 0;
  protected RigelI18nInterface i18n = new DefaultRigelI18nImplementation();
  //
  public static final int TIPO_CHIAVE_CAPTION = 0;
  public static final int TIPO_CHIAVE_ROW_COL = 1;
  public static final int TIPO_CHIAVE_ROW_CAPTION = 2;

  public PeerMapFiller()
  {
  }

  /**
   * Popola la map per l'oggetto alla riga indicata.
   * @param row riga da esportare
   * @param ht map dove inserire i risultati (può essere null)
   * @return tabella con i risultati
   * @throws Exception
   */
  public Map<String, String> obj2ht(int row, Map<String, String> ht)
     throws Exception
  {
    Object record = getRowRecord(row);
    if(ht == null)
      ht = new HashMap<String, String>();

    for(int col = 0; col < getColumnCount(); col++)
    {
      try
      {
        RigelColumnDescriptor cd = getColumn(col);
        String key = nomeChiave(row, col, cd);
        String value = cd.getValueAsString(record);
        ht.put(key, value);
      }
      catch(Exception ex)
      {
        if(!ignoreErrors)
          throw ex;
      }
    }

    return ht;
  }

  /**
   * Ricostruisce l'oggetto alla riga specificata.
   * @param ht map con i valore in formato stringa
   * @param row riga da importare
   * @throws Exception
   */
  public void ht2obj(Map<String, String> ht, int row)
     throws Exception
  {
    Object record = getRowRecord(row);

    for(int col = 0; col < getColumnCount(); col++)
    {
      RigelColumnDescriptor cd = getColumn(col);
      String key = nomeChiave(row, col, cd);
      salvaDatiCella(row, col, cd, key, ht, record);
    }
  }

  protected String nomeChiave(int row, int col, RigelColumnDescriptor cd)
     throws Exception
  {
    switch(tipoChiave)
    {
      default:
      case TIPO_CHIAVE_CAPTION:
        return cd.getCaption().toUpperCase();
      case TIPO_CHIAVE_ROW_COL:
        return "ROW_" + row + "_COL_" + col;
      case TIPO_CHIAVE_ROW_CAPTION:
        return "ROW_" + row + "_" + cd.getCaption().toUpperCase();
    }
  }

  protected void salvaDatiCella(int row, int col, RigelColumnDescriptor cd, String nomeCampo, Map params, Object record)
     throws Exception
  {
    String valore = null;

    try
    {
      valore = StringOper.okStrNull(params.get(nomeCampo));

      // compatibilità con ParameterParser di turbine che potrebbe
      // convertire tutte le chiavi in minuscolo o in maiuscolo
      if(valore == null)
        valore = StringOper.okStrNull(params.get(nomeCampo.toLowerCase()));
      if(valore == null)
        valore = StringOper.okStrNull(params.get(nomeCampo.toUpperCase()));

      if(valore == null && cd.getDefVal() != null)
        valore = cd.getDefVal();
      if(valore == null && cd.isBoolean())
        valore = "0";

      if((isColumnEditable(row, col) || cd.isHiddenEdit()))
      {
        String originale = cd.getValueAsString(record);

        // gestione default su colonne non visibili (il default e' gia' caricato)
        if(originale != null && valore == null && !cd.isVisible())
          return;

        if(!checkValueEqual(originale, valore))
        {
          Object colValue;

          if(cd.getColedit() != null && cd.getColedit().haveCustomParser())
            colValue = cd.getColedit().parseValue(cd, this, row, col, valore, nomeCampo, originale, params, i18n);
          else
            colValue = parseCell(row, col, valore);

          setValueAt(colValue, row, col);
        }
      }
    }
    catch(Exception ex)
    {
      if(!ignoreErrors)
        throw ex;
    }
  }

  protected boolean isColumnEditable(int row, int col)
  {
    return isCellEditable(row, col);
  }

  protected boolean checkValueEqual(String originale, String valore)
  {
    return StringOper.isEqu(originale, valore);
  }

  /**
   * Parsing del valore dalla sua rappresentazione stringa.
   * Se la colonna ha un formattatore esplicito lo usa.
   * @param row riga corrente
   * @param col colonna corrente
   * @param value rappresentazione stringa del valore
   * @return valore sotto forma di Object (salvabile attraverso il table model)
   * @throws Exception
   */
  protected Object parseCell(int row, int col, String value)
     throws Exception
  {
    RigelColumnDescriptor cd = getColumn(col);

    if(cd.getForeignMode() == RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE)
    {
      // in questo caso editiamo un valore che in realtà non esiste
      // su questa tabella, ma è un altro campo della tabella collegata
      // quindi qui lo recuperiamo per il salvataggio
      ForeignDataHolder fd = cd.findHTableForeignAlternate(value, this, i18n);

      if(fd != null)
        value = fd.codice;
    }

    return cd.parseValueNull(value);
  }

  @Override
  public MascheraRicercaGenerica getMascheraRG(RigelI18nInterface i18n)
     throws Exception
  {
    return null;
  }

  @Override
  public void reAttach()
  {
  }

  public boolean isIgnoreErrors()
  {
    return ignoreErrors;
  }

  public void setIgnoreErrors(boolean ignoreErrors)
  {
    this.ignoreErrors = ignoreErrors;
  }

  public int getTipoChiave()
  {
    return tipoChiave;
  }

  public void setTipoChiave(int tipoChiave)
  {
    this.tipoChiave = tipoChiave;
  }

  public RigelI18nInterface getI18n()
  {
    return i18n;
  }

  public void setI18n(RigelI18nInterface i18n)
  {
    this.i18n = i18n;
  }
}
