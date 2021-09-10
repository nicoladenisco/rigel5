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
package org.rigel2.table.html.wrapper;

import org.apache.torque.criteria.SqlEnum;
import org.rigel2.table.RigelColumnDescriptor;

/**
 * <p>
 * Title: holder di parametri per liste SQL.</p>
 * <p>
 * Description: Le liste SQL possono essere filtrate
 * in base a dei parametri: questa classe mantiene uno dei
 * parametri di filtro da combinare alla clausola where di default.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
public class ParametroListe
{
  private String nome;
  private String descrizione;
  private String campo;
  private SqlEnum operazione;
  private String defval;
  private String valore;
  private int tipo;
  private String htmlCampo;
  protected String foreignCampoDisplay;
  protected String foreignCampoLink;
  protected String foreignTabella;
  protected int foreignMode = RigelColumnDescriptor.DISP_FLD_ONLY;
  protected String foreignEditUrl;

  public ParametroListe()
  {
  }

  public static String adjValue(int tipo, String val)
  {
    String qryVal;

    switch(tipo)
    {
      default:
      case RigelColumnDescriptor.PDT_BOOLEAN:
        qryVal = val.trim();
        break;
      case RigelColumnDescriptor.PDT_DATE:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
      case RigelColumnDescriptor.PDT_TIMESTAMP:
      case RigelColumnDescriptor.PDT_TIME:
        qryVal = "'" + val.trim() + "'";
        break;
      case RigelColumnDescriptor.PDT_INTEGER:
      case RigelColumnDescriptor.PDT_FLOAT:
      case RigelColumnDescriptor.PDT_DOUBLE:
      case RigelColumnDescriptor.PDT_MONEY:
      case RigelColumnDescriptor.PDT_NUMBERKEY:
        qryVal = val.trim();
        break;
      case RigelColumnDescriptor.PDT_STRINGKEY:
      case RigelColumnDescriptor.PDT_STRING:
        qryVal = "'" + val.trim() + "'";
        break;
    }

    return qryVal;
  }

  public void AttivaForeignMode(int mode, String tabella, String link, String display, String urlEdit)
     throws Exception
  {
    if(mode != RigelColumnDescriptor.DISP_FLD_EDIT
       && mode != RigelColumnDescriptor.DISP_DESCR_EDIT)
      throw new Exception("Modo non consentito: deve essere DISP_FLD_EDIT oppure DISP_DESCR_EDIT.");

    foreignMode = mode;
    foreignTabella = tabella;
    foreignCampoLink = link;
    foreignCampoDisplay = display;
    foreignEditUrl = urlEdit;
  }

  public String getNome()
  {
    return nome;
  }

  public void setNome(String nome)
  {
    this.nome = nome;
  }

  public void setDescrizione(String descrizione)
  {
    this.descrizione = descrizione;
  }

  public String getDescrizione()
  {
    return descrizione;
  }

  public void setCampo(String campo)
  {
    this.campo = campo;
  }

  public String getCampo()
  {
    return campo;
  }

  public void setOperazione(SqlEnum operazione)
  {
    this.operazione = operazione;
  }

  public SqlEnum getOperazione()
  {
    return operazione;
  }

  public void setDefval(String defval)
  {
    this.defval = defval;
  }

  public String getDefval()
  {
    return defval;
  }

  public void setValore(String valore)
  {
    this.valore = valore;
  }

  public String getValore()
  {
    return valore;
  }

  public String getValoreFmt()
  {
    if(SqlEnum.IN.equals(operazione))
      return "(" + adjValue(tipo, valore) + ")";

    return adjValue(tipo, valore);
  }

  public void setTipo(int tipo)
  {
    this.tipo = tipo;
  }

  public int getTipo()
  {
    return tipo;
  }

  public void setHtmlCampo(String htmlCampo)
  {
    this.htmlCampo = htmlCampo;
  }

  public String getHtmlCampo()
  {
    return htmlCampo;
  }

  public String getForeignCampoDisplay()
  {
    return foreignCampoDisplay;
  }

  public String getForeignCampoLink()
  {
    return foreignCampoLink;
  }

  public String getForeignEditUrl()
  {
    return foreignEditUrl;
  }

  public int getForeignMode()
  {
    return foreignMode;
  }

  public String getForeignTabella()
  {
    return foreignTabella;
  }

  public void setForeignCampoDisplay(String foreignCampoDisplay)
  {
    this.foreignCampoDisplay = foreignCampoDisplay;
  }

  public void setForeignCampoLink(String foreignCampoLink)
  {
    this.foreignCampoLink = foreignCampoLink;
  }

  public void setForeignEditUrl(String foreignEditUrl)
  {
    this.foreignEditUrl = foreignEditUrl;
  }

  public void setForeignMode(int foreignMode)
  {
    this.foreignMode = foreignMode;
  }

  public void setForeignTabella(String foreignTabella)
  {
    this.foreignTabella = foreignTabella;
  }
}
