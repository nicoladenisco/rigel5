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

import org.apache.torque.Torque;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.TableMap;
import org.commonlib5.utils.StringOper;
import org.rigel5.HtmlUtils;
import org.rigel5.db.torque.TableMapHelper;
import org.rigel5.exceptions.MissingListException;
import org.rigel5.table.BuilderRicercaGenerica;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.WrapperBase;
import org.rigel5.table.html.wrapper.EditInfo;
import org.rigel5.table.peer.html.HtmlPeerWrapperBase;
import org.rigel5.table.peer.html.PeerWrapperEditHtml;
import org.rigel5.table.peer.html.PeerWrapperFormHtml;
import org.rigel5.table.peer.html.PeerWrapperListaHtml;

/**
 * Costruisce oggetti wrapper a partire dalla definizione in TableMap di Torque.
 *
 * @author Nicola De Nisco
 */
public class PeerWrapperTmapMaker
{
  protected String baseListUrl = null;
  protected String baseFormUrl = null;
  protected String[] arExcludeFields = null;
  protected String[] arReadOnlyFields = null;
  protected TorqueObjectManager tObjMan = new TorqueObjectManager();
  protected TableMap[] tmaps = null;

  public PeerWrapperTmapMaker()
  {
  }

  /**
   * Recupera l'oggetto TorqueObjectManager associato
   * a questo generatore di wrapper.
   *
   * @return the value of tObjMan
   */
  public TorqueObjectManager getTObjMan()
  {
    return tObjMan;
  }

  /**
   * Imposta l'oggetto TorqueObjectManager associato
   * a questo generatore di wrapper.
   *
   * @param tObjMan new value of tObjMan
   */
  public void setTObjMan(TorqueObjectManager tObjMan)
  {
    this.tObjMan = tObjMan;
  }

  public void setExcludeFields(String exlFlds)
  {
    arExcludeFields = exlFlds.split(";|:|,");
  }

  public void setReadOnlyFields(String exlFlds)
  {
    arReadOnlyFields = exlFlds.split(";|:|,");
  }

  public String getBaseFormUrl()
  {
    return baseFormUrl;
  }

  public void setBaseFormUrl(String baseFormUrl)
  {
    this.baseFormUrl = baseFormUrl;
  }

  public String getBaseListUrl()
  {
    return baseListUrl;
  }

  public void setBaseListUrl(String baseListUrl)
  {
    this.baseListUrl = baseListUrl;
  }

  /////////////////////////////////////////////////////////////////////////
  //
  protected WrapperBase parseWrapper(String nomeTabella, TableMapHelper tmap, WrapperBase wb,
     boolean edit, boolean removeCaratt)
     throws Exception
  {
    HtmlPeerWrapperBase wl = (HtmlPeerWrapperBase) (parseWrapperInternal(nomeTabella, tmap, wb, edit, removeCaratt));

    String objName = tmap.getObjectName();
    String peerName = objName + "Peer";
    wl.setPeerClass(tObjMan.buildPeer(peerName));
    wl.setObjectClass(tObjMan.buildObject(objName));

    ColumnMap[] cmaps = tmap.getColumns();
    for(int i = 0; i < cmaps.length; i++)
    {
      ColumnMap cmap = cmaps[i];

      if(isExcludedColumn(cmap))
        continue;

      RigelColumnDescriptor cd = parseColumn(nomeTabella, tmap, cmap, edit, new PeerColumnDescriptor());

      wl.getPtm().addColumn(cd);
    }

    if(!removeCaratt)
    {
      RigelColumnDescriptor cd = new PeerColumnDescriptor("ZOOM", "#zoom", 10);
      cd.setCaratteristicheSelezioneRiga(true);
      cd.setCaratteristicheEditRiga(true);
      cd.setCaratteristicheCancellaRiga(true);
      wl.getPtm().addColumn(cd);
    }

    return wl;
  }

  public boolean isExcludedColumn(ColumnMap cmap)
  {
    if(arExcludeFields == null)
      return false;

    for(int i = 0; i < arExcludeFields.length; i++)
    {
      if(StringOper.isEquNocase(cmap.getColumnName(), arExcludeFields[i]))
        return true;
      if(StringOper.isEquNocase(cmap.getJavaName(), arExcludeFields[i]))
        return true;
    }
    return false;
  }

  public boolean isReadOnlyColumn(ColumnMap cmap)
  {
    if(arReadOnlyFields == null)
      return false;

    for(int i = 0; i < arReadOnlyFields.length; i++)
    {
      if(StringOper.isEquNocase(cmap.getColumnName(), arReadOnlyFields[i]))
        return true;
      if(StringOper.isEquNocase(cmap.getJavaName(), arReadOnlyFields[i]))
        return true;
    }
    return false;
  }

  public WrapperBase parseWrapperInternal(String nomeTabella, TableMapHelper tmap, WrapperBase wl,
     boolean edit, boolean removeCaratt)
     throws Exception
  {
    wl.setNome(nomeTabella);
    wl.setEleXml(null);

    wl.setHeader(nomeTabella);
    wl.setTitolo(nomeTabella);
    wl.setPermessi(null);
    wl.setNumPerPage(20);

    EditInfo ei = new EditInfo();
    ColumnMap[] cmaps = tmap.getColumns();
    for(int i = 0; i < cmaps.length; i++)
    {
      ColumnMap cmap = cmaps[i];
      if(cmap.isPrimaryKey())
      {
        String javaName = TableMapHelper.getJavaName(cmap.getColumnName());
        ei.addParamEditRiga(javaName, "@" + javaName);
        wl.addSortColumn(javaName);
      }
    }

    ei.setUrlEditRiga(HtmlUtils.mergeUrl(baseFormUrl, "type", nomeTabella));
    wl.setEdInfo(ei);

    // flags abilitazione salva e nuovo
//    if((e = lista.getChild("disableEdit")) != null)
//      wl.setEditEnabled(false);
//    if((e = lista.getChild("disableSave")) != null)
//      wl.setSaveEnabled(false);
//    if((e = lista.getChild("disableNew")) != null)
//      wl.setNewEnabled(false);
//    if((e = lista.getChild("foreign-server")) != null)
//    {
//      List lForeignInfo = tmap.g
//      Iterator iter = lForeignInfo.iterator();
//      while(iter.hasNext())
//      {
//        Element item = (Element) iter.next();
//        wl.addForeignInfo(item.getName(), item.getTextTrim());
//      }
//    }
    return wl;
  }

  protected RigelColumnDescriptor parseColumn(String nomeTabella, TableMapHelper tmap, ColumnMap cmap,
     boolean edit, RigelColumnDescriptor cd)
     throws Exception
  {
    String javaName = TableMapHelper.getJavaName(cmap.getColumnName());
    String caption = javaName;
    String nomeCl = nomeTabella + ":" + caption;
    String campo = javaName;
    int size = cmap.getSize();
    if(size > 80)
      size = 80;

    if(isReadOnlyColumn(cmap))
      edit = false;

    cd.setCaption(caption);
    cd.setNomeCalc(campo);
    cd.setSize(size);

    // valori di default
    cd.setEditable(edit);
    cd.setVisible(true);
    cd.setTestfortype(false);
    cd.setTestfornull(false);

    cd.setVisible(true);
    cd.setHtmlPara(false);
    cd.setHiddenEdit(false);
    cd.setEscludiRicerca(false);
    cd.setPrimaryKey(cmap.isPrimaryKey());
    cd.setAutoIncremento(cmap.isPrimaryKey() && tmap.isAutoIncrement());

//    cd.setComboRicerca(false);
    cd.setForeignAutoCombo(true);
    cd.setRicercaSemplice(BuilderRicercaGenerica.IDX_CRITERIA_ALL);

    cd.setTestfortype(true);
    cd.setTestfornull(cmap.isNotNull());
    cd.setTestforcf(false);
    cd.setTestforpi(false);
    cd.setTestcustom(null);

// TODO: allineamento in base al tipo di dato
//    String align = item.getAttributeValue("align");
//    if(align != null)
//    {
//      if(align.equalsIgnoreCase("left"))
//        cd.setHtmlAlign(RigelColumnDescriptor.HTML_ALIGN_LEFT);
//      if(align.equalsIgnoreCase("center"))
//        cd.setHtmlAlign(RigelColumnDescriptor.HTML_ALIGN_CENTER);
//      if(align.equalsIgnoreCase("right"))
//        cd.setHtmlAlign(RigelColumnDescriptor.HTML_ALIGN_RIGHT);
//    }
    // autoincremento implica non editabile
    if(cd.isAutoIncremento())
      cd.setEditable(false);

    cd.setDataType(RigelColumnDescriptor.PDT_UNDEFINED);

//    if((e = item.getChild("width")) != null)
//    {
//      int newSize = Integer.parseInt(e.getTextTrim());
//      cd.setMinWidth(newSize);
//      cd.setWidth(newSize);
//      cd.setPreferredWidth(newSize);
//    }
    cd.setHtmlSpan(1);

    if(StringOper.isOkStr(cmap.getDefault()))
      cd.setDefVal(cmap.getDefault());

    if(edit)
    {
      // attiva foreign edit auto
      if(tmap.findForeignKeyByColumnNameSimple(cmap.getColumnName()) != null)
      {
        String urlList = baseListUrl + "?type=" + javaName;
        String urlForm = baseFormUrl + "?type=" + javaName;

        cd.AttivaForeignModeAuto(RigelColumnDescriptor.DISP_DESCR_EDIT, urlList);
        cd.setForeignFormUrl(urlForm);
        cd.addForeignFormParam(javaName, "@" + javaName);
      }
    }
    else
    {
      // attiva foreign disp auto
      if(tmap.findForeignKeyByColumnNameSimple(cmap.getColumnName()) != null)
      {
        cd.AttivaForeignModeAuto(RigelColumnDescriptor.DISP_FLD_DESCR);
      }
    }

    return cd;
  }

  protected TableMapHelper getTableMapHelper(String nomeTabella)
     throws Exception
  {
    if(tmaps == null)
      tmaps = Torque.getDatabaseMap().getTables();

    for(int i = 0; i < tmaps.length; i++)
    {
      TableMap tm = tmaps[i];
      if(StringOper.isEquNocase(nomeTabella, tm.getName()))
        return new TableMapHelper(tm);

      String javaName = TableMapHelper.getJavaName(tm.getName());
      if(StringOper.isEquNocase(nomeTabella, javaName))
        return new TableMapHelper(tm);
    }
    throw new MissingListException("Tabella con nome " + nomeTabella + " non trovata.");
  }

  /////////////////////////////////////////////////////////////////////////
  //
  public PeerWrapperListaHtml getLista(String nomeTabella)
     throws Exception
  {
    TableMapHelper tmap = getTableMapHelper(nomeTabella);
    return (PeerWrapperListaHtml) (parseWrapper(nomeTabella, tmap, new PeerWrapperListaHtml(), false, false));
  }

  public PeerWrapperEditHtml getListaEdit(String nomeTabella)
     throws Exception
  {
    TableMapHelper tmap = getTableMapHelper(nomeTabella);
    return (PeerWrapperEditHtml) (parseWrapper(nomeTabella, tmap, new PeerWrapperEditHtml(), true, false));
  }

  public PeerWrapperFormHtml getForm(String nomeTabella)
     throws Exception
  {
    TableMapHelper tmap = getTableMapHelper(nomeTabella);
    PeerWrapperFormHtml wl
       = (PeerWrapperFormHtml) (parseWrapper(nomeTabella, tmap, new PeerWrapperFormHtml(), true, true));

    wl.setNumColonne(1);
    return wl;
  }

}
