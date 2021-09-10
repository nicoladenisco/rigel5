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
package org.rigel2.table;

import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.torque.criteria.SqlEnum;
import org.commonlib.utils.StringOper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.rigel2.RigelCacheManager;
import org.rigel2.SetupHolder;
import org.rigel2.exceptions.MissingListException;
import org.rigel2.exceptions.MissingSectionException;
import org.rigel2.exceptions.XmlSyntaxException;
import org.rigel2.glue.custom.CustomEditFactory;
import org.rigel2.glue.custom.CustomFormatterFactory;
import org.rigel2.table.html.wrapper.*;

/**
 * <p>
 * Title: Produttore base dei Wrapper.</p>
 * <p>
 * Description: Questa e' la classe base dei produttori di Wrapper
 * a partire da file di configurazione XML. Il file XML di configurazione
 * viene letto e le strutture del wrapper costrutite di conseguenza.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
public class RigelBaseWrapperXmlMaker
{
  protected Document doc = null;
  public static final String XML_LISTE_PEER = "liste";
  public static final String XML_EDIT_PEER = "listeEdit";
  public static final String XML_FORMS_PEER = "forms";
  public static final String XML_XML_PEER = "xml-data";
  public static final String XML_LISTE_SQL = "liste-sql";
  public static final String XML_EDIT_SQL = "listeEdit-sql";
  public static final String XML_FORMS_SQL = "forms-sql";
  public static final String XML_XML_SQL = "xml-data-sql";

  public Document getDocument()
  {
    return doc;
  }

  public void setDocument(Document docu)
  {
    doc = docu;
  }

  public String getExtends(Element lista)
  {
    return lista.getAttributeValue("extend",
       lista.getAttributeValue("extends",
          lista.getAttributeValue("estende")));
  }

  /**
   * Recupera l'elemento lista dalla sezione richiesta.
   * @param nomeSezione nome della sezione dove cercare l'elemento lista.
   * @param nomeLista nome dell'elemento lista.
   * @return
   * @throws Exception
   */
  public Element getElementLista(String nomeSezione, String nomeLista)
     throws Exception
  {
    List ll = getDocument().getRootElement().getChildren(nomeSezione);
    if(ll == null || ll.isEmpty())
      throw new MissingSectionException("Sezione " + nomeSezione + " non presente!");

    Iterator iter = ll.iterator();
    while(iter.hasNext())
    {
      Element liste = (Element) iter.next();
      Element lista = liste.getChild(nomeLista);
      if(lista != null)
        return lista;
    }

    throw new MissingListException("Lista " + nomeLista + " non trovata!");
  }

  /**
   * Parsing di base di un wrapper.
   * Vengono caricate dal documento xml tutte le informzioni
   * generali comuni a tutti i tipi di wrapper (sia sql che peer).
   * @param nomeLista
   * @param lista
   * @param wl
   * @param edit
   * @param removeCaratt
   * @return
   * @throws Exception
   */
  public WrapperBase parseWrapper(String nomeLista, Element lista, WrapperBase wl,
     boolean edit, boolean removeCaratt)
     throws Exception
  {
    Element e = null;
    wl.setNome(nomeLista);
    wl.setEleXml(lista);

    if((e = lista.getChild("header")) != null)
      wl.setHeader(e.getTextTrim());
    if((e = lista.getChild("titolo")) != null)
      wl.setTitolo(e.getTextTrim());
    if((e = lista.getChild("title")) != null)
      wl.setTitolo(e.getTextTrim());
    if((e = lista.getChild("permessi")) != null)
      wl.setPermessi(parsePermessi(e));
    if((e = lista.getChild("permission")) != null)
      wl.setPermessi(parsePermessi(e));
    if((e = lista.getChild("table")) != null)
      wl.setTableStatement(e.getTextTrim());

    // flags abilitazione salva e nuovo
    if((e = lista.getChild("disableEdit")) != null)
      wl.setEditEnabled(false);
    if((e = lista.getChild("disableSave")) != null)
      wl.setSaveEnabled(false);
    if((e = lista.getChild("disableNew")) != null)
      wl.setNewEnabled(false);

    // flag abilitazione classi colonna custom
    if((e = lista.getChild("customColumns")) != null)
      wl.setCustomColumnsEnabled(true);

    List parametri = lista.getChildren("parametro");
    Iterator itrPar = parametri.iterator();
    while(itrPar.hasNext())
    {
      Element item = (Element) (itrPar.next());
      wl.addParametri(parseParametro(nomeLista, item, new ParametroListe()));
    }

    List lsHeaderButtons = lista.getChildren("hbutton");
    Iterator itrHbut = lsHeaderButtons.iterator();
    while(itrHbut.hasNext())
    {
      Element eb = (Element) itrHbut.next();
      CustomButtonInfo cb = new CustomButtonInfo();
      parseCustomButton(nomeLista, eb, cb);
      wl.addHeaderButton(cb);
    }

    if((e = lista.getChild("custom-script")) != null)
      wl.setCustomScript(e.getTextTrim());

    if((e = lista.getChild("foreign-server")) != null)
    {
      ForeignInfo fi = new ForeignInfo();
      List lForeignInfo = e.getChildren();
      Iterator iter = lForeignInfo.iterator();
      while(iter.hasNext())
      {
        Element item = (Element) iter.next();
        fi.addForeignInfo(item.getName(), item.getTextTrim());
      }
      wl.setFoInfo(fi);
    }

    String sNumPPage;
    if((sNumPPage = lista.getChildTextTrim("numppage")) != null)
      wl.setNumPerPage(Integer.parseInt(sNumPPage));

    if((e = lista.getChild("edit-info")) != null)
    {
      EditInfo ei = new EditInfo();
      String sPopup = StringOper.okStrNull(e.getAttributeValue("popup"));
      if(sPopup != null)
        ei.setPopupMode(StringOper.parse(sPopup, 0));
      ei.setUrlEditRiga(e.getChildTextTrim("url"));
      List lEditParam = e.getChildren("param");
      Iterator iterEp = lEditParam.iterator();
      while(iterEp.hasNext())
      {
        Element item = (Element) (iterEp.next());
        ei.addParamEditRiga(item.getAttributeValue("nome"), item.getTextTrim());
      }

      wl.setEdInfo(ei);
    }

    Element md;
    if((md = lista.getChild("master-detail")) != null)
    {
      MasterDetailInfo mdi = new MasterDetailInfo();
      mdi.setEleXml(md);

      if((e = md.getChild("role-master")) != null)
        mdi.setRole(MasterDetailInfo.ROLE_MASTER);
      if((e = md.getChild("role-detail")) != null)
        mdi.setRole(MasterDetailInfo.ROLE_DETAIL);

      if((e = md.getChild("edit-list")) != null)
        mdi.setEditList(e.getTextTrim());
      if((e = md.getChild("view-list")) != null)
        mdi.setViewList(e.getTextTrim());

      if((e = md.getChild("link")) != null)
      {
        List lMdParam = e.getChildren("param");
        Iterator iterMd = lMdParam.iterator();
        while(iterMd.hasNext())
        {
          Element item = (Element) (iterMd.next());
          mdi.addLinkParam(item.getAttributeValue("nome"), item.getTextTrim(),
             StringOper.okStrNull(item.getAttributeValue("defval")));
        }
      }

      wl.setMdInfo(mdi);
    }

    // imposta attributo di nosize (non inserisce size nelle tabelle)
    wl.setNosize(testAttributeBoolean(nomeLista, lista, "nosize", false));
    if(lista.getChild("nosize") != null)
      wl.setNosize(true);

    return wl;
  }

  /**
   * Parsing di una colonna con tutti i sui attibuti.
   * @param item
   * @param edit
   * @param cd
   * @return
   * @throws Exception
   */
  public RigelColumnDescriptor parseColumn(String nomeLista, Element item, boolean edit, RigelColumnDescriptor cd)
     throws Exception
  {
    Element e;
    String st;

    String caption = item.getAttributeValue("nome");
    if(caption == null)
      throw new XmlSyntaxException(nomeLista, "Attributo nome non presente!");

    String nomeCl = nomeLista + ":" + caption;

    if((e = item.getChild("campo")) == null)
      throw new XmlSyntaxException(nomeCl, "Nodo campo non presente!");
    String campo = e.getTextTrim();
    if((e = item.getChild("size")) == null)
      throw new XmlSyntaxException(nomeCl, "Nodo size non presente!");
    int size = Integer.parseInt(e.getTextTrim());

    cd.setCaption(caption);
    cd.setNomeCalc(campo);
    cd.setSize(size);

    // valori di default
    cd.setEditable(edit);
    cd.setVisible(true);
    cd.setTestfortype(false);
    cd.setTestfornull(false);
    cd.setTestforzero(false);
    cd.setTestforcf(false);
    cd.setTestforpi(false);

    // flags impostati come attributi di colonna:
    // <colonna nome="pippo" editabile="true" visibile="true" ...
    cd.setEditable(testAttributeBoolean(nomeCl, item, "editabile", testAttributeBoolean(nomeCl, item, "editable", edit)));
    cd.setVisible(testAttributeBoolean(nomeCl, item, "visibile", testAttributeBoolean(nomeCl, item, "visible", true)));
    cd.setHtmlPara(testAttributeBoolean(nomeCl, item, "paragrafo", testAttributeBoolean(nomeCl, item, "para", false)));
    cd.setHiddenEdit(testAttributeBoolean(nomeCl, item, "hiddenedit", testAttributeBoolean(nomeCl, item, "hidden-edit", false)));
    cd.setEscludiRicerca(testAttributeBoolean(nomeCl, item, "noricerca", testAttributeBoolean(nomeCl, item, "nosearch", false)));
    cd.setPrimaryKey(testAttributeBoolean(nomeCl, item, "primary", testAttributeBoolean(nomeCl, item, "key", false)));
    cd.setAutoIncremento(
       testAttributeBoolean(nomeCl, item, "autoinc",
          testAttributeBoolean(nomeCl, item, "autoInc",
             testAttributeBoolean(nomeCl, item, "autoIncrement",
                testAttributeBoolean(nomeCl, item, "autoIncremento", false)))));

    cd.setEnableCache(testAttributeBoolean(nomeCl, item, "cache", cd.isEnableCache()));
    cd.setComboRicerca(testAttributeBoolean(nomeCl, item, "combo-ricerca-self", false));
    cd.setForeignAutoCombo(testAttributeBoolean(nomeCl, item, "foreign-auto-combo", true));
    cd.setPrintable(
       testAttributeBoolean(nomeCl, item, "printable",
          testAttributeBoolean(nomeCl, item, "stampabile",
             cd.isPrintable())));

    // in origine ricerca-semplice era un boolean, quindi per compatibilita'
    // con i formati precedenti diamo 1 se troviamo true
    // adesso ricerca-semplice puo' avere un indice (0=no 1=LIKE 2=ric.>=)
    if((st = item.getAttributeValue("ricerca-semplice")) != null && st.trim().length() > 0)
      cd.setRicercaSemplice(testAttributeRicercaSemplice(st, BuilderRicercaGenerica.IDX_CRITERIA_ALL));

    cd.setTestfortype(testAttributeBoolean(nomeCl, item, "testtipo", false));
    cd.setTestfornull(testAttributeBoolean(nomeCl, item, "testnull", false));
    cd.setTestforzero(testAttributeBoolean(nomeCl, item, "testzero", false));
    cd.setTestforcf(testAttributeBoolean(nomeCl, item, "testcf", false));
    cd.setTestforpi(testAttributeBoolean(nomeCl, item, "testpi", false));

    if((st = item.getAttributeValue("testcustom")) != null && st.trim().length() > 0)
      cd.setTestcustom(st);

    // flags impostati come elementi di colonna:
    // <colonna nome="pippo">
    //    <editabile/> <visibile/> ...
    if(item.getChild("editabile") != null || item.getChild("editable") != null)
      cd.setEditable(true);
    if(item.getChild("visibile") != null || item.getChild("visible") != null)
      cd.setVisible(true);
    if(item.getChild("paragrafo") != null || item.getChild("para") != null)
      cd.setHtmlPara(true);
    if(item.getChild("hiddenedit") != null || item.getChild("hidden-edit") != null)
      cd.setHiddenEdit(true);
    if(item.getChild("noricerca") != null || item.getChild("nosearch") != null)
      cd.setEscludiRicerca(true);
    if(item.getChild("primary") != null || item.getChild("key") != null)
      cd.setPrimaryKey(true);
    if(item.getChild("autoinc") != null || item.getChild("autoInc") != null
       || item.getChild("autoincrement") != null || item.getChild("autoIncrement") != null
       || item.getChild("autoincremento") != null || item.getChild("autoIncremento") != null)
      cd.setAutoIncremento(true);
    if(item.getChild("combo-ricerca-self") != null)
      cd.setComboRicerca(true);

    // se presente solo il tag <ricerca-semplice/> assegna valore 1
    // altrimenti <ricerca-semplice>2</ricerca-semplice> il valore indicato
    if((e = item.getChild("ricerca-semplice")) != null)
    {
      cd.setRicercaSemplice(BuilderRicercaGenerica.IDX_CRITERIA_LIKE);
      if((st = e.getTextTrim()) != null && st.trim().length() > 0)
        cd.setRicercaSemplice(testAttributeRicercaSemplice(st, BuilderRicercaGenerica.IDX_CRITERIA_LIKE));
    }

    // gestione plugin generazione HTML custom
    if((e = item.getChild("custom-edit")) != null)
    {
      String className = e.getChildText("class");
      if(className == null)
        className = e.getChildText("classname");

      if(className == null)
        throw new XmlSyntaxException(nomeCl, "Class mancante in definizione custom-edit: aggiungere un tag <class></class>!");

      CustomColumnEdit ce = CustomEditFactory.getInstance().getCustomColumnEdit(className);
      ce.init(e);
      cd.setColedit(ce);
    }

    if(item.getChild("testtipo") != null)
      cd.setTestfortype(true);
    if(item.getChild("testnull") != null)
      cd.setTestfornull(true);
    if(item.getChild("testzero") != null)
      cd.setTestforzero(true);
    if(item.getChild("testcodice") != null)
      cd.setTestforcodice(true);

    if(item.getChild("testcf") != null)
      cd.setTestforcf(true);
    if(item.getChild("testpi") != null)
      cd.setTestforpi(true);
    if((e = item.getChild("testcustom")) != null)
      cd.setTestcustom(e.getTextTrim());

    if((e = item.getChild("testrange")) != null)
    {
      String s;
      if((s = e.getAttributeValue("min")) == null)
        throw new XmlSyntaxException(nomeCl, "Attributo obbligatorio 'min' di 'testrange' non trovato.");
      cd.setTestrangemin(s);
      if((s = e.getAttributeValue("max")) == null)
        throw new XmlSyntaxException(nomeCl, "Attributo obbligatorio 'max' di 'testrange' non trovato.");
      cd.setTestrangemax(s);

      cd.setTestrange(true);
      cd.setTestfortype(true);
    }

    String align = item.getAttributeValue("align");
    if(align != null)
    {
      if(align.equalsIgnoreCase("left"))
        cd.setHtmlAlign(RigelColumnDescriptor.HTML_ALIGN_LEFT);
      if(align.equalsIgnoreCase("center"))
        cd.setHtmlAlign(RigelColumnDescriptor.HTML_ALIGN_CENTER);
      if(align.equalsIgnoreCase("right"))
        cd.setHtmlAlign(RigelColumnDescriptor.HTML_ALIGN_RIGHT);
    }

    // autoincremento implica non editabile
    if(cd.isAutoIncremento())
      cd.setEditable(false);

    if((e = item.getChild("tipo")) != null)
      cd.setDataType(testAttributeTipoDato(e.getTextTrim(), RigelColumnDescriptor.PDT_UNDEFINED));
    if((e = item.getChild("type")) != null)
      cd.setDataType(testAttributeTipoDato(e.getTextTrim(), RigelColumnDescriptor.PDT_UNDEFINED));

    if((e = item.getChild("width")) != null)
    {
      int newSize = Integer.parseInt(e.getTextTrim());
      cd.setMinWidth(newSize);
      cd.setWidth(newSize);
      cd.setPreferredWidth(newSize);
    }

    if((e = item.getChild("span")) != null)
      cd.setHtmlSpan(Integer.parseInt(e.getTextTrim()));

    if((e = item.getChild("def-val")) != null)
      cd.setDefVal(e.getTextTrim());
    if((e = item.getChild("def-val-param")) != null)
      cd.setDefValParam(e.getTextTrim());

    if((e = item.getChild("combo-ricerca")) != null)
    {
      String tbl = e.getChildText("tabella");
      String lnk = e.getChildText("link");
      String dis = e.getChildText("display");

      if(tbl == null || lnk == null || dis == null)
        throw new XmlSyntaxException(nomeCl, "Parametri mancanti per combo-ricerca.");

      cd.AttivaComboRicerca(tbl, lnk, dis);
      buildExtraWhere(e, cd);
    }

    if((e = item.getChild("combo-display")) != null)
    {
      String tbl = e.getChildText("tabella");
      String lnk = e.getChildText("link");
      String dis = e.getChildText("display");

      if(tbl == null || lnk == null || dis == null)
        throw new XmlSyntaxException(nomeCl, "Parametri mancanti per combo-display.");

      cd.AttivaCombo(tbl, lnk, dis);
      buildExtraWhere(e, cd);
    }

    if((e = item.getChild("foreign-disp")) != null
       || (e = item.getChild("foreign-display")) != null)
    {
      int mode = testAttributeTipoForeign(e.getChildTextTrim("mode"), RigelColumnDescriptor.DISP_DESCR_ONLY);

      String tbl = e.getChildText("tabella");
      String lnk = e.getChildText("link");
      String dis = e.getChildText("display");

      if(tbl == null || lnk == null || dis == null)
        throw new XmlSyntaxException(nomeCl, "Parametri mancanti per foreign-disp.");

      if(mode == RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE || mode == RigelColumnDescriptor.DISP_FLD_DESCR_ALTERNATE)
      {
        String alternate = e.getChildText("alternate");
        if(alternate == null)
          throw new XmlSyntaxException(nomeCl, "Parametro alternate mancante per foreign-disp.");

        cd.AttivaForeignModeAlternate(mode, tbl, lnk, alternate, dis);
      }
      else
        cd.AttivaForeignMode(mode, tbl, lnk, dis);

      if(e.getChild("combo-ricerca") != null)
        cd.AttivaComboRicerca(tbl, lnk, dis);
      if(e.getChild("combo") != null)
        cd.AttivaCombo(tbl, lnk, dis);
      buildExtraWhere(e, cd);
    }

    if((e = item.getChild("foreign-edit-values")) != null
       || (e = item.getChild("foreign-values")) != null)
    {
      int mode = testAttributeTipoForeign(e.getChildTextTrim("mode"),
         e.getName().equals("foreign-values") ? RigelColumnDescriptor.DISP_DESCR_ONLY
            : RigelColumnDescriptor.DISP_DESCR_EDIT);

      ArrayList<ForeignDataHolder> fv = new ArrayList<ForeignDataHolder>();
      Iterator itr = e.getChildren("value").iterator();
      while(itr.hasNext())
      {
        Element ec = (Element) itr.next();
        ForeignDataHolder fd = new ForeignDataHolder();
        if((fd.codice = StringOper.okStrNull(ec.getAttributeValue("key"))) != null)
        {
          fd.descrizione = ec.getTextTrim();
          fv.add(fd);
        }
      }

      cd.AttivaForeignMode(mode, fv);
      cd.AttivaComboSelf();
    }

    boolean foreignEdit = false;
    boolean foreignEditAuto = false;
    if((e = item.getChild("foreign-edit")) != null)
      foreignEdit = true;
    else if((e = item.getChild("foreign-edit-auto")) != null)
      foreignEditAuto = true;

    if(foreignEdit || foreignEditAuto)
    {
      int mode = testAttributeTipoForeign(e.getChildTextTrim("mode"), RigelColumnDescriptor.DISP_DESCR_EDIT);

      if(foreignEdit)
      {
        String tbl = e.getChildText("tabella");
        String lnk = e.getChildText("link");
        String dis = e.getChildText("display");
        String url = e.getChildText("url");

        if(tbl == null || lnk == null || dis == null)
          throw new XmlSyntaxException(nomeCl, "Parametri mancanti per foreign-edit.");

        if(url == null && !SetupHolder.isForeignAutoCombo())
          throw new XmlSyntaxException(nomeCl,
             "URL di modifica per foreign-edit non specificata e uso del combo non abilitato a setup.");

        if(mode == RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE || mode == RigelColumnDescriptor.DISP_FLD_DESCR_ALTERNATE)
        {
          String alternate = e.getChildText("alternate");
          if(alternate == null)
            throw new XmlSyntaxException(nomeCl, "Parametro alternate mancante per foreign-edit.");

          cd.AttivaForeignModeAlternate(mode, tbl, lnk, alternate, dis, url);
        }
        else
          cd.AttivaForeignMode(mode, tbl, lnk, dis, url);

        if(e.getChild("combo-ricerca") != null)
          cd.AttivaComboRicerca(tbl, lnk, dis);
        if(e.getChild("combo") != null)
          cd.AttivaCombo(tbl, lnk, dis);

        buildExtraWhere(e, cd);
        buildExtraScript(e, cd);
      }
      else
      {
        String url = e.getChildText("url");
        String dis = e.getChildText("display");

        if(url == null && !SetupHolder.isForeignAutoCombo())
          throw new XmlSyntaxException(nomeCl, "Parametri mancanti per foreign-edit-auto.");

        if(mode == RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE || mode == RigelColumnDescriptor.DISP_FLD_DESCR_ALTERNATE)
        {
          String alternate = e.getChildText("alternate");
          if(alternate == null)
            throw new XmlSyntaxException(nomeCl, "Parametro alternate mancante per foreign-edit-auto.");

          cd.AttivaForeignModeAutoAlternate(mode, alternate, url);
        }
        else
          cd.AttivaForeignModeAuto(mode, url);

        if(dis != null)
          cd.setForeignCampoDisplay(dis);

        buildExtraWhere(e, cd);
        buildExtraScript(e, cd);
      }

      if(e.getAttributeValue("foreign-auto-combo") != null)
        cd.setForeignAutoCombo(testAttributeBoolean(nomeCl, e, "foreign-auto-combo", true));

      Element f;
      if((f = e.getChild("form")) != null)
      {
        String urlForm = f.getChildText("url");
        if(urlForm == null)
          throw new XmlSyntaxException(nomeCl, "Parametri mancanti per foreign-edit (form senza url).");

        cd.setForeignFormUrl(urlForm);
        Iterator iter = f.getChildren("param").iterator();
        while(iter.hasNext())
        {
          Element frmParam = (Element) iter.next();
          String parName = frmParam.getAttributeValue("nome",
             frmParam.getAttributeValue("name"));
          if(parName == null)
            continue;
          String parValue = frmParam.getTextTrim();
          cd.addForeignFormParam(parName, parValue);
        }
      }
    }

    if((e = item.getChild("caratteristiche")) != null)
    {
      Element e1;
      if((e1 = e.getChild("selezione")) != null)
        cd.setCaratteristicheSelezioneRiga(true);

      if((e1 = e.getChild("edit")) != null)
        cd.setCaratteristicheEditRiga(true);

      if((e1 = e.getChild("cancella")) != null)
        cd.setCaratteristicheCancellaRiga(true);

      List cList;
      if((cList = e.getChildren("custom")) != null)
      {
        Iterator iter = cList.iterator();
        while(iter.hasNext())
        {
          Element e2 = (Element) iter.next();
          CustomButtonInfo cb = new CustomButtonInfo();
          cd.addCustomButton(parseCustomButton(nomeLista, e2, cb));
        }
      }

      cd.setEscludiRicerca(true);
    }

    if((e = item.getChild("formatter")) != null)
    {
      cd.setFormatter(buildFormatter(e));
    }

    if((e = item.getChild("numformat")) != null)
    {
      String fmt = e.getTextTrim();
      // preleva dal servizio NumFormatter i simboli di separazione
      // per formattare il numero correttamente secondo le regole locali
      cd.setFormatter(new DecimalFormat(fmt));
    }

    cd.setFixedText(StringOper.okStr(item.getChildText("testo"), item.getChildTextTrim("text")));
    cd.setHtmlStyle(StringOper.okStr(item.getChildText("stile"), item.getChildTextTrim("style")));

    return cd;
  }

  protected void buildExtraWhere(Element e, RigelColumnDescriptor cd)
  {
    Element ew = e.getChild("extra-where");
    if(ew == null)
      return;

    boolean removeZero = StringOper.checkTrueFalse(ew.getAttributeValue("removeZero"), cd.isComboExtraWhereRemoveZero());
    String extraWhere = StringOper.okStrNull(ew.getText());

    if(extraWhere != null)
      cd.setComboExtraWhere(extraWhere, removeZero);
  }

  protected void buildExtraScript(Element e, RigelColumnDescriptor cd)
  {
    String es = e.getChildText("extra-script");
    if(es != null)
      cd.setExtraScript(es);
  }

  protected Format buildFormatter(Element xml)
  {
    try
    {
      return CustomFormatterFactory.getInstance().getFormatter(xml);
    }
    catch(Throwable t)
    {
      Logger.getLogger(RigelBaseWrapperXmlMaker.class.getName()).log(Level.SEVERE, null, t);
    }

    return null;
  }

  protected <T extends RigelColumnDescriptor> T buildColumn(String nomeLista, String nomeColonna,
     Element xml, Class<T> defaultClass)
     throws Exception
  {
    String key = "RigelColumnDescriptor_" + nomeLista + "_" + nomeColonna + "_class";
    RigelCacheManager cm = SetupHolder.getCacheManager();
    Class cls = (Class) cm.getGenericCachedData(key);

    if(cls == null)
    {
      String fmtClass = null;

      if(fmtClass == null || fmtClass.isEmpty())
        fmtClass = xml.getChildText("class");
      if(fmtClass == null || fmtClass.isEmpty())
        fmtClass = xml.getChildText("classname");

      // caricamento dinamico della classe colonna
      if(!(fmtClass == null || fmtClass.isEmpty()))
      {
        try
        {
          cls = Class.forName(fmtClass);
          cm.putGenericCachedData(key, cls);
        }
        catch(Throwable t)
        {
          throw new RuntimeException(t);
        }
      }
      else
      {
        cm.putGenericCachedData(key, defaultClass);
      }
    }

    // se la classe non è stata trovata o non è inizializzabile ritorna il default
    if(cls == null || cls.equals(defaultClass))
    {
      try
      {
        return defaultClass.newInstance();
      }
      catch(Throwable t)
      {
        throw new RuntimeException(t);
      }
    }

    // cerca un costruttore che accetti il descrittore xml
    try
    {
      Constructor c = cls.getConstructor(Element.class);
      return (T) c.newInstance(xml);
    }
    catch(Throwable t)
    {
    }

    // riprova con un costruttore vuoto
    try
    {
      return (T) cls.newInstance();
    }
    catch(Throwable t)
    {
      throw new RuntimeException(t);
    }
  }

  protected int addColumn(Element itemCol, RigelTableModel rtm, RigelColumnDescriptor cd)
     throws Exception
  {
    String replace = itemCol.getAttributeValue("replace",
       itemCol.getAttributeValue("sostituisci"));

    int pos = StringOper.parse(itemCol.getAttributeValue("pos"), -1);

    // verifica per colonna con la stessa caption: sostituisce colonna
    int ncPrev = rtm.findColumn(cd.getCaption());
    if(ncPrev != -1)
      replace = cd.getCaption();

    if(replace != null)
      return rtm.addOrReplaceColumn(cd, replace);
    else if(pos != -1)
      return rtm.addColumn(pos, cd);
    else
      return rtm.addColumn(cd);
  }

  /**
   * Rimozione colonne in liste derivate.
   * @param lista
   * @param rtm
   * @return
   */
  protected int removeColumns(Element lista, RigelTableModel rtm)
  {
    int removed = 0;
    List<Element> elRemove = lista.getChildren("remove-column");
    for(Element er : elRemove)
    {
      String caption = StringOper.okStrAny(
         er.getAttributeValue("nome"),
         er.getAttributeValue("name"));
      removed += rtm.delColumn(caption);
    }
    return removed;
  }

  public CustomButtonInfo parseCustomButton(String nomeLista, Element item, CustomButtonInfo cb)
     throws Exception
  {
    String sPopup;
    if(item.getChild("url") != null && (sPopup = item.getChild("url").getAttributeValue("popup")) != null)
      cb.setPopup(Integer.parseInt(sPopup.trim()));
    if((sPopup = item.getChildTextTrim("popup")) != null)
      cb.setPopup(Integer.parseInt(sPopup));

    cb.setUrl(item.getChildTextTrim("url"));
    cb.setIcon(item.getChildTextTrim("icon"));
    cb.setText(item.getChildTextTrim("text"));
    cb.setClassName(item.getChildTextTrim("class"));
    cb.setConfirm(item.getChildTextTrim("confirm"));
    cb.setJavascript(item.getChildTextTrim("script"));
    cb.setHtml(item.getChildTextTrim("html"));
    cb.setLineEdit(item.getChild("line-edit") != null);

    List lEditParam = item.getChildren("param");
    Iterator iterEp = lEditParam.iterator();
    while(iterEp.hasNext())
    {
      Element param = (Element) (iterEp.next());
      String nome = StringOper.okStrNull(param.getAttributeValue("nome"));
      if(nome == null)
        nome = StringOper.okStrNull(param.getAttributeValue("name"));

      if(nome == null)
        throw new XmlSyntaxException(param, nomeLista, "Parametro per custombutton senza nome.");

      cb.addParam(nome, param.getTextTrim());
    }

    return cb;
  }

  public ParametroListe parseParametro(String nomeLista, Element item, ParametroListe par)
     throws Exception
  {
    par.setNome(item.getAttributeValue("nome").trim());

    // imposta nome del campo HTML (per default lo ricava dal nome)
    String nomeCampoHtml = item.getAttributeValue("param");
    if(nomeCampoHtml == null)
      par.setHtmlCampo(par.getNome().replace(' ', '_'));
    else
      par.setHtmlCampo(nomeCampoHtml.trim());

    par.setDescrizione(item.getChildTextTrim("descr"));
    par.setCampo(item.getChildTextTrim("campo"));
    par.setDefval(item.getChildTextTrim("defval"));
    par.setOperazione(testTipoConfronto(nomeLista, item.getChild("campo").getAttributeValue("cmp")));

    Element e;
    if((e = item.getChild("tipo")) != null)
      par.setTipo(testAttributeTipoDato(e.getTextTrim(), RigelColumnDescriptor.PDT_UNDEFINED));
    if((e = item.getChild("type")) != null)
      par.setTipo(testAttributeTipoDato(e.getTextTrim(), RigelColumnDescriptor.PDT_UNDEFINED));

    if((e = item.getChild("foreign-edit")) != null
       || (e = item.getChild("foreign")) != null)
    {
      String tbl = e.getChildText("tabella");
      String lnk = e.getChildText("link");
      String dis = e.getChildText("display");
      String url = e.getChildText("url");

      if(tbl == null || lnk == null || dis == null)
        throw new XmlSyntaxException(nomeLista, "Parametri mancanti per foreign-edit.");

      if(url == null && !SetupHolder.isForeignAutoCombo())
        throw new XmlSyntaxException(nomeLista,
           "URL di modifica per foreign-edit non specificata e uso del combo non abilitato a setup.");

      par.AttivaForeignMode(
         testAttributeTipoForeign(e.getChildTextTrim("mode"), RigelColumnDescriptor.DISP_DESCR_ONLY),
         tbl, lnk, dis, url);
    }

    return par;
  }

  protected boolean testAttributeBoolean(String nomeLista, Element item, String nomeAttr, boolean defval)
     throws Exception
  {
    if(item == null)
      return defval;

    String val = item.getAttributeValue(nomeAttr);
    if(val == null)
      return defval;

    if(val.equalsIgnoreCase("true"))
      return true;
    if(val.equalsIgnoreCase("vero"))
      return true;
    if(val.equalsIgnoreCase("1"))
      return true;
    if(val.equalsIgnoreCase("false"))
      return false;
    if(val.equalsIgnoreCase("falso"))
      return false;
    if(val.equalsIgnoreCase("0"))
      return false;

    throw new XmlSyntaxException(nomeLista, "Valore di " + nomeAttr + " non consentito (deve essere true,vero,1,false,falso,0).");
  }

  protected int testAttributeTipoDato(String val, int defval)
     throws Exception
  {
    if(val == null || val.length() == 0)
      return defval;

    if(val.equals("PDT_UNDEFINED"))
      return RigelColumnDescriptor.PDT_UNDEFINED;
    if(val.equals("PDT_STRING"))
      return RigelColumnDescriptor.PDT_STRING;
    if(val.equals("PDT_BOOLEAN"))
      return RigelColumnDescriptor.PDT_BOOLEAN;
    if(val.equals("PDT_INTEGER"))
      return RigelColumnDescriptor.PDT_INTEGER;
    if(val.equals("PDT_FLOAT"))
      return RigelColumnDescriptor.PDT_FLOAT;
    if(val.equals("PDT_DOUBLE"))
      return RigelColumnDescriptor.PDT_DOUBLE;
    if(val.equals("PDT_DATE"))
      return RigelColumnDescriptor.PDT_DATE;
    if(val.equals("PDT_TIMESTAMP_CMPDATEONLY"))
      return RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY;
    if(val.equals("PDT_TIMESTAMP_CMPHOURONLY"))
      return RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY;
    if(val.equals("PDT_TIMESTAMP_CMPTOSEC"))
      return RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC;
    if(val.equals("PDT_TIMESTAMP_CMPTOMIN"))
      return RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN;
    if(val.equals("PDT_TIMESTAMP"))
      return RigelColumnDescriptor.PDT_TIMESTAMP;
    if(val.equals("PDT_TIME"))
      return RigelColumnDescriptor.PDT_TIME;
    if(val.equals("PDT_STRINGKEY"))
      return RigelColumnDescriptor.PDT_STRINGKEY;
    if(val.equals("PDT_NUMBERKEY"))
      return RigelColumnDescriptor.PDT_NUMBERKEY;
    if(val.equals("PDT_MONEY"))
      return RigelColumnDescriptor.PDT_MONEY;
    if(val.equals("PDT_FILE"))
      return RigelColumnDescriptor.PDT_FILE;

    return Integer.parseInt(val);
  }

  protected int testAttributeTipoForeign(String val, int defval)
     throws Exception
  {
    if(val == null || val.length() == 0)
      return defval;

    if(val.equals("DISP_FLD_ONLY"))
      return RigelColumnDescriptor.DISP_FLD_ONLY;
    if(val.equals("DISP_DESCR_ONLY"))
      return RigelColumnDescriptor.DISP_DESCR_ONLY;
    if(val.equals("DISP_FLD_DESCR"))
      return RigelColumnDescriptor.DISP_FLD_DESCR;
    if(val.equals("DISP_FLD_DESCR_ALTERNATE"))
      return RigelColumnDescriptor.DISP_FLD_DESCR_ALTERNATE;
    if(val.equals("DISP_FLD_EDIT"))
      return RigelColumnDescriptor.DISP_FLD_EDIT;
    if(val.equals("DISP_DESCR_EDIT"))
      return RigelColumnDescriptor.DISP_DESCR_EDIT;
    if(val.equals("DISP_DESCR_EDIT_ALTERNATE"))
      return RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE;

    return Integer.parseInt(val);
  }

  protected int testAttributeRicercaSemplice(String val, int defval)
     throws Exception
  {
    if(val == null || val.length() == 0)
      return defval;

    if(val.equals("IDX_CRITERIA_ALL") || val.equals("ALL"))
      return BuilderRicercaGenerica.IDX_CRITERIA_ALL;
    if(val.equals("IDX_CRITERIA_LIKE") || val.equals("LIKE"))
      return BuilderRicercaGenerica.IDX_CRITERIA_LIKE;
    if(val.equals("IDX_CRITERIA_EQUAL") || val.equals("EQUAL"))
      return BuilderRicercaGenerica.IDX_CRITERIA_EQUAL;
    if(val.equals("IDX_CRITERIA_LESS_THAN") || val.equals("LESS_THAN"))
      return BuilderRicercaGenerica.IDX_CRITERIA_LESS_THAN;
    if(val.equals("IDX_CRITERIA_GREATER_THAN") || val.equals("GREATER_THAN"))
      return BuilderRicercaGenerica.IDX_CRITERIA_GREATER_THAN;
    if(val.equals("IDX_CRITERIA_NOT_EQUAL") || val.equals("NOT_EQUAL"))
      return BuilderRicercaGenerica.IDX_CRITERIA_NOT_EQUAL;
    if(val.equals("IDX_CRITERIA_LESS_EQUAL") || val.equals("LESS_EQUAL"))
      return BuilderRicercaGenerica.IDX_CRITERIA_LESS_EQUAL;
    if(val.equals("IDX_CRITERIA_GREATER_EQUAL") || val.equals("GREATER_EQUAL"))
      return BuilderRicercaGenerica.IDX_CRITERIA_GREATER_EQUAL;
    if(val.equals("IDX_CRITERIA_BETWEEN") || val.equals("BETWEEN"))
      return BuilderRicercaGenerica.IDX_CRITERIA_BETWEEN;
    if(val.equals("IDX_CRITERIA_ISNULL") || val.equals("ISNULL"))
      return BuilderRicercaGenerica.IDX_CRITERIA_ISNULL;
    if(val.equals("IDX_CRITERIA_ISNOTNULL") || val.equals("ISNOTNULL"))
      return BuilderRicercaGenerica.IDX_CRITERIA_ISNOTNULL;

    // compatibilita' con sintassi precedente (vero/falso)
    if(val.equalsIgnoreCase("true"))
      return BuilderRicercaGenerica.IDX_CRITERIA_LIKE;
    if(val.equalsIgnoreCase("vero"))
      return BuilderRicercaGenerica.IDX_CRITERIA_LIKE;
    if(val.equalsIgnoreCase("1"))
      return BuilderRicercaGenerica.IDX_CRITERIA_LIKE;
    if(val.equalsIgnoreCase("false"))
      return BuilderRicercaGenerica.IDX_CRITERIA_ALL;
    if(val.equalsIgnoreCase("falso"))
      return BuilderRicercaGenerica.IDX_CRITERIA_ALL;
    if(val.equalsIgnoreCase("0"))
      return BuilderRicercaGenerica.IDX_CRITERIA_ALL;

    return Integer.parseInt(val);
  }

  public SqlEnum testTipoConfronto(String nomeLista, String in)
     throws Exception
  {
    if(in == null)
      return null;

    in = in.trim().toUpperCase();

    if(in.equalsIgnoreCase("EQ"))
      return SqlEnum.EQUAL;
    else if(in.equalsIgnoreCase("NE"))
      return SqlEnum.NOT_EQUAL;
    else if(in.equalsIgnoreCase("L"))
      return SqlEnum.LESS_THAN;
    else if(in.equalsIgnoreCase("G"))
      return SqlEnum.GREATER_THAN;
    else if(in.equalsIgnoreCase("LE"))
      return SqlEnum.LESS_EQUAL;
    else if(in.equalsIgnoreCase("GE"))
      return SqlEnum.GREATER_EQUAL;
    else if(in.equalsIgnoreCase("LIKE"))
      return SqlEnum.LIKE;
    else if(in.equalsIgnoreCase("ILIKE"))
      return SqlEnum.ILIKE;
    else if(in.equalsIgnoreCase("IN"))
      return SqlEnum.IN;

    throw new XmlSyntaxException(nomeLista, "Operatore di confronto non corretto:" + in);
  }

  /**
   * Parsing di un blocco permessi.
   * I quattro tipi di permessi sono attributi del tag permessi.
   * Viene mantenuta la compatibilità con la gestione precedente,
   * impostanto lo stesso permesso per tutti se è contenuto come tag.
   * @param ele elemento xml
   * @return
   */
  protected BloccoPermessi parsePermessi(Element ele)
  {
    BloccoPermessi rv = new BloccoPermessi();

    // compatibilità sintassi precedente (o comunque un default)
    String oldValue = StringOper.okStrNull(ele.getText());
    if(oldValue != null)
    {
      rv.setLettura(oldValue);
      rv.setScrittura(oldValue);
      rv.setCreazione(oldValue);
      rv.setCancellazione(oldValue);
    }

    rv.setLettura(
       StringOper.okStr(ele.getAttributeValue("lettura"),
          StringOper.okStrNull(ele.getAttributeValue("read"))));

    rv.setScrittura(
       StringOper.okStr(ele.getAttributeValue("scrittura"),
          StringOper.okStrNull(ele.getAttributeValue("write"))));

    rv.setCreazione(
       StringOper.okStr(ele.getAttributeValue("creazione"),
          StringOper.okStrNull(ele.getAttributeValue("create"))));

    rv.setCancellazione(
       StringOper.okStr(ele.getAttributeValue("cancellazione"),
          StringOper.okStrNull(ele.getAttributeValue("delete"))));

    return rv.isValid() ? rv : null;
  }
}
