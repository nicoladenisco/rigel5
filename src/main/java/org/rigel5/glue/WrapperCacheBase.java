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
package org.rigel5.glue;

import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlib5.utils.ClassOper;
import org.jdom2.Element;
import org.rigel5.RigelI18nInterface;
import org.rigel5.RigelXmlSetupInterface;
import org.rigel5.db.sql.QueryBuilder;
import org.rigel5.exceptions.MissingListException;
import org.rigel5.exceptions.MissingSectionException;
import org.rigel5.glue.pager.PeerPagerAppMaint;
import org.rigel5.glue.pager.PeerTablePagerEditApp;
import org.rigel5.glue.pager.SqlPagerAppMaint;
import org.rigel5.glue.table.AlternateColorTableAppBase;
import org.rigel5.glue.table.HeditTableApp;
import org.rigel5.glue.table.PeerAppMaintDispTable;
import org.rigel5.glue.table.PeerAppMaintFormTable;
import org.rigel5.glue.table.SqlAppMaintFormTable;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.AbstractHtmlTablePager;
import org.rigel5.table.html.FormTable;
import org.rigel5.table.html.hTable;
import org.rigel5.table.html.wrapper.HtmlWrapperBase;
import org.rigel5.table.html.wrapper.MasterDetailInfo;
import org.rigel5.table.peer.html.PeerWrapperEditHtml;
import org.rigel5.table.peer.html.PeerWrapperFormHtml;
import org.rigel5.table.peer.html.PeerWrapperListaHtml;
import org.rigel5.table.sql.html.SqlWrapperFormHtml;
import org.rigel5.table.sql.html.SqlWrapperListaHtml;

/**
 * Classe base di cache dei wrapper.
 * Questa classe deve essere estesa nell'applicazione ospite
 * per gestire una cache dei wrapper. In genere una istanza di
 * questo oggetto viene salvata nei dati di sessione per utente.
 *
 * @author Nicola De Nisco
 */
abstract public class WrapperCacheBase
{
  /** Logging */
  private static Log log = LogFactory.getLog(WrapperCacheBase.class);
  //
  public static final String CACHE_LISTE_PEER = "ListaBase/liste-peer/";
  public static final String CACHE_LISTE_SQL = "ListaBase/liste-sql/";
  public static final String CACHE_LISTE_TMAP = "ListaBase/liste-tmap/";
  //
  public static final String CACHE_FORM_PEER = "ListaBase/form-peer/";
  public static final String CACHE_FORM_SQL = "ListaBase/form-sql/";
  public static final String CACHE_FORM_TMAP = "ListaBase/form-tmap/";
  //
  public static final String CACHE_DISP_PEER = "ListaBase/disp-peer/";
  public static final String CACHE_DISP_TMAP = "ListaBase/disp-tmap/";
  //
  public static final String CACHE_LISTE_EDIT_PEER = "ListaBase/liste-edit-peer/";
  public static final String CACHE_LISTE_EDIT_SQL = "ListaBase/liste-edit-sql/";
  public static final String CACHE_LISTE_EDIT_TMAP = "ListaBase/liste-edit-tmap/";
  //
  // cache delle liste
  protected Hashtable htListe = new Hashtable();
  // cache delle liste edit
  protected Hashtable htLedit = new Hashtable();
  // cache dei forms
  protected Hashtable htForms = new Hashtable();
  // gestore modelli xml
  protected WrapperBuilderInterface wrpBuilder = null;
  // tag delle tabelle
  protected String tagTabelleForm = null;
  protected String tagTabelleList = null;
  // internazionalizzazione
  protected RigelI18nInterface i18n = null;

  protected String[] basePath = null;

  /**
   * Svuota questa cache di tutti gli elementi contenuti.
   */
  public void clear()
  {
    htForms.clear();
    htLedit.clear();
    htListe.clear();
  }

  protected hTable getTableCustom(Element ele)
  {
    Element eleCustom = ele.getChild("custom-classes"); // NOI18N
    if(eleCustom == null)
      return null;
    String className = eleCustom.getChildTextTrim("table"); // NOI18N
    if(className == null)
      return null;

    try
    {
      return (hTable) ClassOper.loadClass(className, basePath).newInstance();
    }
    catch(Exception ex)
    {
      log.error(i18n.msg("Impossibile istanziare la tabella custom '%s':", className), ex);
    }

    return null;
  }

  protected AbstractHtmlTablePager getPagerCustom(Element ele)
  {
    Element eleCustom = ele.getChild("custom-classes"); // NOI18N
    if(eleCustom == null)
      return null;
    String className = eleCustom.getChildTextTrim("pager"); // NOI18N
    if(className == null)
      return null;

    try
    {
      return (AbstractHtmlTablePager) ClassOper.loadClass(className, basePath).newInstance();
    }
    catch(Exception ex)
    {
      log.error(i18n.msg("Impossibile istanziare la tabella custom '%s':", className), ex);
    }

    return null;
  }

  abstract public AlternateColorTableAppBase buildDefaultTableList();

  abstract public HeditTableApp buildDefaultTableEdit();

  abstract public PeerAppMaintFormTable buildDefaultPeerTableForm();

  abstract public SqlAppMaintFormTable buildDefaultSqlTableForm();

  abstract public PeerObjectSaver buildDefaultPeerSaver();

  abstract public RecordObjectSaver buildDefaultRecordSaver();

  abstract public void populateTableModelProperties(RigelTableModel tm);

  protected PeerWrapperEditHtml creaListaEditPeer(String type)
     throws Exception
  {
    PeerWrapperEditHtml wl = wrpBuilder.getListaEditPeer(type);

    HeditTableApp tbl = (HeditTableApp) getTableCustom(wl.getEleXml());
    if(tbl == null)
      tbl = buildDefaultTableEdit();

    PeerTablePagerEditApp pager = (PeerTablePagerEditApp) getPagerCustom(wl.getEleXml());
    if(pager == null)
      pager = new PeerTablePagerEditApp();

    tbl.setWl(wl);
    tbl.setId("EP");
    tbl.setTableStatement(tagTabelleForm);
    tbl.setI18n(i18n);

    if(tbl instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) tbl).setEleXml(wl.getEleXml());

    pager.init(wl, type, buildDefaultPeerSaver());
    pager.setI18n(i18n);

    if(pager instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) pager).setEleXml(wl.getEleXml());

    wl.setTableStatement(tagTabelleForm);
    wl.setTbl(tbl);
    wl.setPager(pager);
    wl.init();

    org.rigel5.table.peer.html.PeerTableModel ptm = (org.rigel5.table.peer.html.PeerTableModel) wl.getPtm();
    populateTableModelProperties(ptm);

    if(wl.getMdInfo() != null && wl.getMdInfo().getRole() == MasterDetailInfo.ROLE_DETAIL)
    {
      // in caso di master/detail il detail non puo' bindarsi a tutti i records
      // ma viene bindato un unico elemento giusto per inizializzare le colonne.
      // il bind vero e proprio verra' eseguito in seguito in base al master che
      // controllera' un sottoinsieme di records da bindare nel detail.
      ptm.rebind(wl.getObjectClass().newInstance());
    }
    else
    {
      ptm.rebind(wl.getRecords());
    }

    log.debug("Creato nuovo PeerWrapperEditHtml " + type);
    return wl;
  }

  protected PeerWrapperEditHtml getListaEditPeer(String type)
     throws Exception
  {
    String cacheKey = CACHE_LISTE_EDIT_PEER + type;
    PeerWrapperEditHtml wl = (PeerWrapperEditHtml) (htLedit.get(cacheKey));
    if(wl == null)
    {
      wl = creaListaEditPeer(type);
      htLedit.put(cacheKey, wl);
    }
    return wl;
  }

  protected PeerWrapperEditHtml creaListaEditTmap(String type)
     throws Exception
  {
    PeerWrapperEditHtml wl = wrpBuilder.getListaEditTmap(type);
    HeditTableApp tbl = buildDefaultTableEdit();
    PeerTablePagerEditApp pager = new PeerTablePagerEditApp();

    tbl.setWl(wl);
    tbl.setId("ET");
    tbl.setTableStatement(tagTabelleForm);
    tbl.setI18n(i18n);

    if(tbl instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) tbl).setEleXml(wl.getEleXml());

    pager.init(wl, type, buildDefaultPeerSaver());
    pager.setI18n(i18n);

    if(pager instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) pager).setEleXml(wl.getEleXml());

    wl.setTableStatement(tagTabelleForm);
    wl.setTbl(tbl);
    wl.setPager(pager);
    wl.init();

    org.rigel5.table.peer.html.PeerTableModel ptm = (org.rigel5.table.peer.html.PeerTableModel) wl.getPtm();
    populateTableModelProperties(ptm);

    if(wl.getMdInfo() != null && wl.getMdInfo().getRole() == MasterDetailInfo.ROLE_DETAIL)
    {
      // in caso di master/detail il detail non puo' bindarsi a tutti i records
      // ma viene bindato un unico elemento giusto per inizializzare le colonne.
      // il bind vero e proprio verra' eseguito in seguito in base al master che
      // controllera' un sottoinsieme di records da bindare nel detail.
      ptm.rebind(wl.getObjectClass().newInstance());
    }
    else
    {
      ptm.rebind(wl.getRecords());
    }

    log.debug("Creato nuovo PeerWrapperEditHtml " + type);
    return wl;
  }

  protected PeerWrapperEditHtml getListaEditTmap(String type)
     throws Exception
  {
    String cacheKey = CACHE_LISTE_EDIT_TMAP + type;
    PeerWrapperEditHtml wl = (PeerWrapperEditHtml) (htLedit.get(cacheKey));
    if(wl == null)
    {
      wl = creaListaEditPeer(type);
      htLedit.put(cacheKey, wl);
    }
    return wl;
  }

  ////////
  /**
   * Crea un nuovo oggetto wrapper form dai modelli XML.
   * @param type tipo di form richiesto
   * @return oggetto wrapper form relativo
   * @throws Exception
   */
  protected PeerWrapperFormHtml creaFormPeer(String type)
     throws Exception
  {
    PeerWrapperFormHtml wf = wrpBuilder.getFormPeer(type);

    FormTable tbl = (FormTable) getTableCustom(wf.getEleXml());
    if(tbl == null)
      tbl = buildDefaultPeerTableForm();

    if(tbl instanceof PeerAppMaintFormTable)
    {
      ((PeerAppMaintFormTable) tbl).init("FP", wf, buildDefaultPeerSaver());
    }
    else if(tbl instanceof PeerAppMaintDispTable)
    {
      ((PeerAppMaintDispTable) tbl).init("FP", wf);
    }

    tbl.setTableStatement(tagTabelleForm);
    tbl.setI18n(i18n);

    if(tbl instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) tbl).setEleXml(wf.getEleXml());

    wf.setTableStatement(tagTabelleForm);
    wf.setTbl(tbl);
    wf.init();

    populateTableModelProperties(wf.getPtm());

    log.debug("Creato nuovo PeerWrapperFormHtml " + type);
    return wf;
  }

  /**
   * Utilizza una cache locale per recuperare un
   * wrapper form precedentemente creato. Se non e' stato
   * creato ne instanzia uno nuovo attraverso creaFormPeer()
   * e lo salva nella cache.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperFormHtml getFormPeer(String type)
     throws Exception
  {
    String cacheKey = CACHE_FORM_PEER + type;
    PeerWrapperFormHtml wl = (PeerWrapperFormHtml) (htForms.get(cacheKey));
    if(wl == null)
    {
      wl = creaFormPeer(type);
      htForms.put(cacheKey, wl);
    }
    return wl;
  }

  /**
   * Crea un nuovo oggetto wrapper lista dai modelli XML.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperFormHtml creaFormTmap(String type)
     throws Exception
  {
    PeerWrapperFormHtml wf = wrpBuilder.getFormTmap(type);
    PeerAppMaintFormTable table = buildDefaultPeerTableForm();

    table.init("FT", wf, buildDefaultPeerSaver());
    table.setTableStatement(tagTabelleForm);
    table.setI18n(i18n);

    wf.setTableStatement(tagTabelleForm);
    wf.setTbl(table);
    wf.init();

    populateTableModelProperties(wf.getPtm());

    log.debug("Creato nuovo PeerWrapperFormHtml " + type);
    return wf;
  }

  /**
   * Utilizza una cache locale per recuperare un
   * wrapper form precedentemente creato. Se non e' stato
   * creato ne instanzia uno nuovo attraverso creaFormPeer()
   * e lo salva nella cache.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperFormHtml getFormTmap(String type)
     throws Exception
  {
    String cacheKey = CACHE_FORM_TMAP + type;
    PeerWrapperFormHtml wl = (PeerWrapperFormHtml) (htForms.get(cacheKey));
    if(wl == null)
    {
      wl = creaFormTmap(type);
      htForms.put(cacheKey, wl);
    }
    return wl;
  }

  /**
   * Crea un nuovo oggetto wrapper lista dai modelli XML.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected SqlWrapperFormHtml creaFormSql(String type)
     throws Exception
  {
    SqlWrapperFormHtml wf = wrpBuilder.getFormSql(type);
    SqlAppMaintFormTable table = buildDefaultSqlTableForm();

    table.init("FT", wf, buildDefaultRecordSaver());
    table.setTableStatement(tagTabelleForm);
    table.setI18n(i18n);

    // crea il query builder con il macro resolver: qui deve avvenire prima di wl.init()
    QueryBuilder qb = wf.getPtm().makeQueryBuilder();
    wf.getPtm().setQuery(qb);
    populateTableModelProperties(wf.getPtm());

    wf.setTableStatement(tagTabelleForm);
    wf.setTbl(table);
    wf.init(qb);

    populateTableModelProperties(wf.getPtm());

    log.debug("Creato nuovo SqlWrapperFormHtml " + type);
    return wf;
  }

  /**
   * Utilizza una cache locale per recuperare un
   * wrapper form precedentemente creato. Se non e' stato
   * creato ne instanzia uno nuovo attraverso creaFormSql()
   * e lo salva nella cache.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected SqlWrapperFormHtml getFormSql(String type)
     throws Exception
  {
    String cacheKey = CACHE_FORM_SQL + type;
    SqlWrapperFormHtml wl = (SqlWrapperFormHtml) (htForms.get(cacheKey));
    if(wl == null)
    {
      wl = creaFormSql(type);
      htForms.put(cacheKey, wl);
    }
    return wl;
  }

  /**
   * Crea un nuovo oggetto wrapper display dai modelli XML.
   * Gli oggetti display sono simili ai form ma hanno una
   * DispTable al posto di una FormTable di conseguenza sono
   * utilizzati solo in visualizzazione.
   * @param type tipo di form richiesto
   * @return oggetto wrapper form relativo
   * @throws Exception
   */
  protected PeerWrapperFormHtml creaDispPeer(String type)
     throws Exception
  {
    PeerWrapperFormHtml wf = wrpBuilder.getFormPeer(type);

    PeerAppMaintDispTable table = (PeerAppMaintDispTable) getTableCustom(wf.getEleXml());
    if(table == null)
      table = new PeerAppMaintDispTable();

    table.init("DP", wf);
    table.setTableStatement(tagTabelleForm);
    table.setI18n(i18n);

    if(table instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) table).setEleXml(wf.getEleXml());

    wf.setTableStatement(tagTabelleForm);
    wf.setTbl(table);
    wf.init();

    populateTableModelProperties(wf.getPtm());

    log.debug("Creato nuovo PeerWrapperFormHtml/Disp " + type);
    return wf;
  }

  /**
   * Utilizza una cache locale per recuperare un
   * wrapper form precedentemente creato. Se non e' stato
   * creato ne instanzia uno nuovo attraverso creaDispPeer()
   * e lo salva nella cache.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperFormHtml getDispPeer(String type)
     throws Exception
  {
    String cacheKey = CACHE_DISP_PEER + type;
    PeerWrapperFormHtml wl = (PeerWrapperFormHtml) (htForms.get(cacheKey));
    if(wl == null)
    {
      wl = creaDispPeer(type);
      htForms.put(cacheKey, wl);
    }
    return wl;
  }

  /**
   * Crea un nuovo oggetto wrapper lista dai modelli XML.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperFormHtml creaDispTmap(String type)
     throws Exception
  {
    PeerWrapperFormHtml wf = wrpBuilder.getFormTmap(type);
    PeerAppMaintDispTable table = new PeerAppMaintDispTable();

    wf.setTableStatement(tagTabelleForm);
    wf.setTbl(table);
    wf.init();

    table.init("DP", wf);
    table.setTableStatement(tagTabelleForm);
    table.setI18n(i18n);

    populateTableModelProperties(wf.getPtm());

    log.debug("Creato nuovo PeerWrapperFormHtml/Disp " + type);
    return wf;
  }

  /**
   * Utilizza una cache locale per recuperare un
   * wrapper form precedentemente creato. Se non e' stato
   * creato ne instanzia uno nuovo attraverso creaFormPeer()
   * e lo salva nella cache.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperFormHtml getDispTmap(String type)
     throws Exception
  {
    String cacheKey = CACHE_DISP_TMAP + type;
    PeerWrapperFormHtml wl = (PeerWrapperFormHtml) (htForms.get(cacheKey)); // NOI18N
    if(wl == null)
    {
      wl = creaDispTmap(type);
      htForms.put(cacheKey, wl);
    }
    return wl;
  }

  /**
   * Crea un nuovo oggetto wrapper lista dai modelli XML.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperListaHtml creaListaPeer(String type)
     throws Exception
  {
    PeerWrapperListaHtml wl = wrpBuilder.getListaPeer(type);

    AlternateColorTableAppBase tbl = (AlternateColorTableAppBase) getTableCustom(wl.getEleXml());
    if(tbl == null)
      tbl = buildDefaultTableList();

    PeerPagerAppMaint pager = (PeerPagerAppMaint) getPagerCustom(wl.getEleXml());
    if(pager == null)
      pager = new PeerPagerAppMaint();

    tbl.init(wl);
    tbl.setTableStatement(tagTabelleList);
    tbl.setI18n(i18n);

    if(tbl instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) tbl).setEleXml(wl.getEleXml());

    pager.setWl(wl);
    pager.setI18n(i18n);

    if(pager instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) pager).setEleXml(wl.getEleXml());

    wl.setTableStatement(tagTabelleList);
    wl.setTbl(tbl);
    wl.setPager(pager);
    wl.init();

    populateTableModelProperties(wl.getPtm());

    log.debug("Creato nuovo PeerWrapperListaHtml " + type);
    return wl;
  }

  /**
   * Crea un nuovo oggetto wrapper lista dai modelli XML.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected SqlWrapperListaHtml creaListaSql(String type)
     throws Exception
  {
    SqlWrapperListaHtml wl = wrpBuilder.getListaSql(type);

    AlternateColorTableAppBase tbl = (AlternateColorTableAppBase) getTableCustom(wl.getEleXml());
    if(tbl == null)
      tbl = buildDefaultTableList();

    SqlPagerAppMaint pager = (SqlPagerAppMaint) getPagerCustom(wl.getEleXml());
    if(pager == null)
      pager = new SqlPagerAppMaint();

    tbl.init(wl);
    tbl.setTableStatement(tagTabelleList);
    tbl.setI18n(i18n);

    if(tbl instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) tbl).setEleXml(wl.getEleXml());

    pager.setWl(wl);
    pager.setI18n(i18n);

    if(pager instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) pager).setEleXml(wl.getEleXml());

    // crea il query builder con il macro resolver: qui deve avvenire prima di wl.init()
    QueryBuilder qb = wl.getPtm().makeQueryBuilder();
    wl.getPtm().setQuery(qb);
    populateTableModelProperties(wl.getPtm());

    wl.setTableStatement(tagTabelleList);
    wl.setTbl(tbl);
    wl.setPager(pager);
    wl.init(qb);

    log.debug("Creato nuovo SqlWrapperListaHtml " + type);
    return wl;
  }

  /**
   * Crea un nuovo oggetto wrapper lista direttamente
   * dalla table map della tabella indicata.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperListaHtml creaListaTmap(String type)
     throws Exception
  {
    PeerWrapperListaHtml wl = wrpBuilder.getListaTmap(type);
    PeerPagerAppMaint pager = new PeerPagerAppMaint();
    AlternateColorTableAppBase tbl = buildDefaultTableList();

    tbl.init(wl);
    tbl.setTableStatement(tagTabelleList);
    tbl.setI18n(i18n);

    if(tbl instanceof RigelXmlSetupInterface)
      ((RigelXmlSetupInterface) tbl).setEleXml(wl.getEleXml());

    wl.setTableStatement(tagTabelleList);
    wl.setTbl(tbl);
    wl.setPager(pager);
    wl.init();

    pager.setWl(wl);
    pager.setI18n(i18n);
    pager.limit = wl.getNumPerPage();

    populateTableModelProperties(wl.getPtm());

    log.debug("Creato nuovo PeerWrapperListaHtml " + type);
    return wl;
  }

  /**
   * Utilizza una cache locale per recuperare un
   * wrapper lista precedentemente creato. Se non e' stato
   * creato ne instanzia uno nuovo attraverso creaLista()
   * e lo salva nella cache.
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperListaHtml getListaPeer(String type)
     throws Exception
  {
    String cacheKey = CACHE_LISTE_PEER + type;
    PeerWrapperListaHtml wl = (PeerWrapperListaHtml) (htListe.get(cacheKey));
    if(wl == null)
    {
      wl = creaListaPeer(type);
      htListe.put(cacheKey, wl);
    }
    return wl;
  }

  /**
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected SqlWrapperListaHtml getListaSql(String type)
     throws Exception
  {
    String cacheKey = CACHE_LISTE_SQL + type;
    SqlWrapperListaHtml wl = (SqlWrapperListaHtml) (htListe.get(cacheKey));
    if(wl == null)
    {
      wl = creaListaSql(type);
      htListe.put(cacheKey, wl);
    }
    return wl;
  }

  /**
   * @param type tipo di lista richiesta
   * @return oggetto wrapper lista relativo
   * @throws Exception
   */
  protected PeerWrapperListaHtml getListaTmap(String type)
     throws Exception
  {
    String cacheKey = CACHE_LISTE_TMAP + type;
    PeerWrapperListaHtml wl = (PeerWrapperListaHtml) (htListe.get(cacheKey));
    if(wl == null)
    {
      wl = creaListaTmap(type);
      htListe.put(cacheKey, wl);
    }
    return wl;
  }

  /**
   * Interroga la cache per verificare se la lista esiste
   * e di che tipo di lista sia (sql o peer)
   * @param type nome della lista
   * @return la lista se esiste altrimenti null
   * @throws java.lang.Exception
   */
  public synchronized HtmlWrapperBase getListaCache(String type)
     throws Exception
  {
    HtmlWrapperBase objCache = null;

    // cerca la lista nella cache
    if((objCache = (HtmlWrapperBase) htListe.get(CACHE_LISTE_PEER + type)) != null)
      return objCache;
    if((objCache = (HtmlWrapperBase) htListe.get(CACHE_LISTE_SQL + type)) != null)
      return objCache;
    if((objCache = (HtmlWrapperBase) htListe.get(CACHE_LISTE_TMAP + type)) != null)
      return objCache;

    // tenta costruzione come PEER
    try
    {
      if((objCache = getListaPeer(type)) != null)
        return objCache;
    }
    catch(MissingListException | MissingSectionException e)
    {
    }

    // tenta costruzione come SQL
    try
    {
      if((objCache = getListaSql(type)) != null)
        return objCache;
    }
    catch(MissingListException | MissingSectionException e)
    {
    }

    // tenta costruzione come TMAP
    try
    {
      if((objCache = getListaTmap(type)) != null)
        return objCache;
    }
    catch(MissingListException | MissingSectionException e)
    {
    }

    throw new MissingListException(i18n.msg("Lista %s non trovata o non inizializzabile.", type));
  }

  /**
   * Interroga la cache per verificare se la lista esiste
   * e di che tipo di lista sia (sql o peer)
   * @param type nome della lista
   * @return la lista se esiste altrimenti null
   * @throws java.lang.Exception
   */
  public synchronized HtmlWrapperBase getListaEditCache(String type)
     throws Exception
  {
    HtmlWrapperBase objCache = null;

    // cerca la lista nella cache
    if((objCache = (PeerWrapperEditHtml) htListe.get(CACHE_LISTE_EDIT_PEER + type)) != null)
      return objCache;
    if((objCache = (PeerWrapperEditHtml) htListe.get(CACHE_LISTE_TMAP + type)) != null)
      return objCache;
    if((objCache = (PeerWrapperEditHtml) htListe.get(CACHE_LISTE_EDIT_TMAP + type)) != null)
      return objCache;

    // tenta costruzione come PEER
    try
    {
      if((objCache = getListaEditPeer(type)) != null)
        return objCache;
    }
    catch(MissingListException e)
    {
    }

    // tenta costruzione come TMAP
    try
    {
      if((objCache = getListaEditTmap(type)) != null)
        return objCache;
    }
    catch(MissingListException e)
    {
    }

    throw new MissingListException(i18n.msg("Lista edit %s non trovata o non inizializzabile.", type));
  }

  public synchronized HtmlWrapperBase getFormCache(String type)
     throws Exception
  {
    HtmlWrapperBase objCache = null;

    // cerca la lista nella cache
    if((objCache = (PeerWrapperFormHtml) htListe.get(CACHE_FORM_PEER + type)) != null)
      return objCache;
    if((objCache = (PeerWrapperFormHtml) htListe.get(CACHE_FORM_TMAP + type)) != null)
      return objCache;
    if((objCache = (PeerWrapperFormHtml) htListe.get(CACHE_FORM_SQL + type)) != null)
      return objCache;

    // tenta costruzione come PEER
    try
    {
      if((objCache = getFormPeer(type)) != null)
        return objCache;
    }
    catch(MissingListException e)
    {
    }

    // tenta costruzione come TMAP
    try
    {
      if((objCache = getFormTmap(type)) != null)
        return objCache;
    }
    catch(MissingListException e)
    {
    }

    // tenta costruzione come SQL
    try
    {
      if((objCache = getFormSql(type)) != null)
        return objCache;
    }
    catch(MissingListException e)
    {
    }

    throw new MissingListException(i18n.msg("Form %s non trovato o non inizializzabile.", type));
  }

  public synchronized PeerWrapperFormHtml getDispCache(String type)
     throws Exception
  {
    PeerWrapperFormHtml objCache = null;

    // cerca la lista nella cache
    if((objCache = (PeerWrapperFormHtml) htListe.get(CACHE_DISP_PEER + type)) != null)
      return objCache;
    if((objCache = (PeerWrapperFormHtml) htListe.get(CACHE_DISP_TMAP + type)) != null)
      return objCache;

    // tenta costruzione come PEER
    try
    {
      if((objCache = getFormPeer(type)) != null)
        return objCache;
    }
    catch(MissingListException e)
    {
    }

    // tenta costruzione come TMAP
    try
    {
      if((objCache = getFormTmap(type)) != null)
        return objCache;
    }
    catch(MissingListException e)
    {
    }

    throw new MissingListException(i18n.msg("Disp %s non trovata o non inizializzabile.", type));
  }
}
