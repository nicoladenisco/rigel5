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
package org.rigel5.db;

import gnu.getopt.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.commonlib5.utils.LongOptExt;
import org.commonlib5.utils.StringOper;
import org.jdom2.CDATA;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Generatore automatico di liste XML.
 *
 * @author Nicola De Nisco
 */
abstract public class AbstractGenListe extends ParseSchemaBase
{
  public int numPPage = 20;
  public int numColonne = 1;
  public int listSizeMax = 60;
  public int formSizeMax = 80;
  public int defSize = 10;
  public String lPeerRadix = "Lp";
  public String liSqlRadix = "Ls";
  public String formsRadix = "Form";
  public String adapter = "mysql";
  public String sqloutput = null;
  public boolean addSelezione = true;
  public boolean addEditCanc = true;
  public boolean removeUnder = true;
  public boolean compatTorque4 = false;
  public String useAs = " "; // oppure ' AS ' per i db che lo supportano
  public boolean verbose = false, debug = false;
  public Namespace ns;
  //
  public HashMap<String, String> htLPeer = new HashMap<String, String>();
  public HashMap<String, String> htLiSql = new HashMap<String, String>();
  public HashMap<String, String> htForms = new HashMap<String, String>();
  public HashMap<String, String> sql2Rigel = new HashMap<String, String>();

  //
  //  public String[] arNoFieldsListe =
  //  {
  //    "STATO_REC", "ID_USER", "ID_UCREA", "ULT_MODIF", "CREAZIONE", "SCADENZA", "TIPORECORD"
  //  };
  //  public String[] arFieldsDescrizione =
  //  {
  //    "DESCRIZIONE", "NOME", "COGNOME"
  //  };
  //  String[] toRemove =
  //  {
  //    "INF.STP_", "INF.IN_", "INF.OUT_", "INF.RUN_", "INF.", "GEN."
  //  };
  //
  //  public static final String DATE_FORMATTER = "it.infomed.caleido.utils.format.DateTimeFormat";
  //  public static final String STYLE_TECH_FIELD = "cell_form_tech";
  //
  public AbstractGenListe()
  {
    sql2Rigel.put("CHAR", "PDT_STRING");
    sql2Rigel.put("VARCHAR", "PDT_STRING");
    sql2Rigel.put("CLOB", "PDT_STRING");
    sql2Rigel.put("TEXT", "PDT_STRING");
    sql2Rigel.put("BOOL", "PDT_BOOLEAN");
    sql2Rigel.put("INTEGER", "PDT_INTEGER");
    sql2Rigel.put("FLOAT", "PDT_FLOAT");
    sql2Rigel.put("DOUBLE", "PDT_DOUBLE");
    sql2Rigel.put("COURRENCY", "PDT_MONEY");
    sql2Rigel.put("DATE", "PDT_DATE");
    sql2Rigel.put("TIME", "PDT_TIME");
    sql2Rigel.put("TIMESTAMP", "PDT_TIMESTAMP");
  }

  public abstract String[] getArNoFieldsListe();

  public abstract String[] getArFieldsDescrizione();

  public abstract String[] getArtoRemove();

  public abstract String getStyleTechField();

  public abstract String getDateFormatter();

  public boolean isNoFieldListe(String fieldName)
  {
    String[] arNoFieldsListe = getArNoFieldsListe();

    for(int i = 0; i < arNoFieldsListe.length; i++)
    {
      if(fieldName.equalsIgnoreCase(arNoFieldsListe[i]))
        return true;
    }

    return false;
  }

  public boolean isFieldDescrizione(String fieldName)
  {
    String[] arFieldsDescrizione = getArFieldsDescrizione();

    for(int i = 0; i < arFieldsDescrizione.length; i++)
    {
      if(fieldName.equalsIgnoreCase(arFieldsDescrizione[i]))
        return true;
    }

    return false;
  }

  /**
   * Genera tutti i nomi delle liste e dei form e li salva in due
   * hashtable associandoli ai rispettivi nomi di tabella.
   * @throws Exception
   */
  public void genNames()
     throws Exception
  {
    Iterator iterTable = doc.getRootElement().getChildren("table", ns).iterator();
    while(iterTable.hasNext())
    {
      Element table = (Element) (iterTable.next());
      String tableName = table.getAttributeValue("name").toLowerCase();

      String ObjName = removeUnderScores(tableName);
      htLPeer.put(tableName, lPeerRadix + ObjName);
      htLiSql.put(tableName, liSqlRadix + ObjName);
      htForms.put(tableName, formsRadix + ObjName);
    }
  }

  public String createPeerName(String tableName)
  {
    if(compatTorque4)
    {
      // in Torque4 i peer vengono generati al netto dello schema
      // quindi qui rimuove lo schema dal nome tabella ('app.coda' diventa 'coda')
      int pos = tableName.indexOf('.');
      if(pos != -1)
        tableName = tableName.substring(pos + 1);
    }

    return removeUnderScores(tableName);
  }

  /**
   * Recupera il descrittore di una tabella in base al nome.
   * @param name nome della tabella
   * @return descrittore o null se non trovato
   * @throws Exception
   */
  public Element getTable(String name)
     throws Exception
  {
    Iterator iterTable = doc.getRootElement().getChildren("table", ns).iterator();
    while(iterTable.hasNext())
    {
      Element table = (Element) (iterTable.next());
      String tableName = table.getAttributeValue("name").toLowerCase();
      if(StringOper.isEquNocase(name, tableName))
        return table;
    }
    return null;
  }

  /**
   * Genera le liste Peer.
   * @return elemento liste
   * @throws Exception
   */
  public Element genListe()
     throws Exception
  {
    Element liste = new Element("liste");

    Iterator iterTable = doc.getRootElement().getChildren("table", ns).iterator();
    while(iterTable.hasNext())
    {
      Element table = (Element) (iterTable.next());
      String tableName = table.getAttributeValue("name").toLowerCase();
      String ObjName = createPeerName(tableName);
      String listName = htLPeer.get(tableName);
      String formName = htForms.get(tableName);

      String descrizione = getListCaption(table);
      if(descrizione == null)
        descrizione = table.getAttributeValue("description");
      if(descrizione == null)
        descrizione = ObjName;

      String header = table.getAttributeValue("header");
      if(header == null)
        header = preparaHeader(tableName);

      Element nt = new Element(listName);
      nt.addContent(new Element("object").addContent(ObjName));
      nt.addContent(new Element("peer").addContent(ObjName + "Peer"));
      nt.addContent(new Element("header").addContent(header));
      nt.addContent(new Element("titolo").addContent(descrizione));
      nt.addContent(new Element("numppage").addContent("" + numPPage));
      nt.addContent(new Element("menu").addContent(ObjName));

      Element editInfo = new Element("edit-info");
      editInfo.addContent(new Element("url").addContent("@form?type=" + formName));
      nt.addContent(editInfo);

      List<Element> lsUnique = table.getChildren("unique", ns);
      if(!lsUnique.isEmpty())
      {
        boolean isValid = false;
        Element ppValidator = new Element("post-parse-validator");
        ppValidator.addContent(new Element("class").addContent("UndoLogicalDeleteValidator"));

        for(Element euni : lsUnique)
        {
          List<Element> lsUniCol = euni.getChildren("unique-column", ns);
          if(!lsUniCol.isEmpty())
          {
            Element alternateKey = new Element("alternate-key");
            ppValidator.addContent(alternateKey);

            for(Element eunicol : lsUniCol)
            {
              String colName = eunicol.getAttributeValue("name");
              alternateKey.addContent(new Element("field").addContent(colName));
              isValid = true;
            }
          }
        }

        if(isValid)
          nt.addContent(ppValidator);
      }

      int numPrimay = countPrimaryKeys(table);

      String foreignCodice = null, foreignDescription = null;
      Iterator iterCol = table.getChildren("column", ns).iterator();
      while(iterCol.hasNext())
      {
        Element column = (Element) (iterCol.next());
        String fieldName = column.getAttributeValue("name");

        if(isNoFieldListe(fieldName))
          continue;

        boolean isAutoIncrement = false;
        boolean isPrimaryKey = false;

        String s;
        if((s = column.getAttributeValue("autoIncrement")) != null)
          isAutoIncrement = StringOper.checkTrueFalse(s.trim(), false);
        if((s = column.getAttributeValue("primaryKey")) != null)
          isPrimaryKey = StringOper.checkTrueFalse(s.trim(), false);

        String columnName = getListCaption(column);
        if(columnName == null)
          columnName = fieldName;
        if(columnName.startsWith("ID_"))
          columnName = columnName.substring(3);
        columnName = columnName.replace('_', ' ');

        String peerColName = removeUnderScores(fieldName);

        String typeCol = column.getAttributeValue("type");
        int sizeCol = StringOper.parse(column.getAttributeValue("size"), defSize);
        if(sizeCol > listSizeMax)
          sizeCol = listSizeMax;

        Element col = new Element("colonna");
        col.setAttribute("nome", columnName);
        col.addContent(new Element("campo").addContent(peerColName));
        col.addContent(new Element("size").addContent(Integer.toString(sizeCol)));
        nt.addContent(col);

        if(isFieldDescrizione(fieldName))
        {
          col.setAttribute("ricerca-semplice", "true");
          foreignDescription = columnName;
        }

        if(isPrimaryKey)
        {
          col.setAttribute("primary", "true");

          Element ped = new Element("param");
          ped.setAttribute("nome", peerColName);
          ped.addContent("@" + peerColName);
          editInfo.addContent(ped);

          if(numPrimay == 1)
          {
            ped.setAttribute("nome", "ID");
            col.setAttribute("nome", "ID");
            col.setAttribute("editable", "false");
            foreignCodice = "ID";
          }
        }
      }

      if(foreignCodice != null && foreignDescription != null)
      {
        Element foreignServer = new Element("foreign-server");
        foreignServer.addContent(new Element("codice").setText(foreignCodice));
        foreignServer.addContent(new Element("descrizione").setText(foreignDescription));
        nt.addContent(foreignServer);
      }

      if(addEditCanc)
      {
        Element editcanc = new Element("colonna");
        editcanc.setAttribute("nome", "Fun.");
        editcanc.setAttribute("align", "center");
        editcanc.addContent(new Element("campo").addContent("#fun"));
        editcanc.addContent(new Element("size").addContent("10"));
        editcanc.addContent(new Element("caratteristiche").addContent(
           new Element("edit")).addContent(new Element("cancella")));
        nt.addContent(editcanc);
      }

      // aggiunge la lista al documento XML di output
      System.out.println("Generata lista peer " + listName);
      liste.addContent(nt);

      if(addSelezione)
      {
        Element lsel = new Element(listName + "Sel");
        lsel.setAttribute("extend", listName);

        Element selezione = new Element("colonna");
        selezione.setAttribute("nome", "Sel.");
        selezione.setAttribute("align", "center");
        selezione.setAttribute("pos", "0");
        selezione.addContent(new Element("campo").addContent("#sel"));
        selezione.addContent(new Element("size").addContent("5"));
        selezione.addContent(new Element("caratteristiche").addContent(
           new Element("selezione")));
        lsel.addContent(selezione);

        // aggiunge la lista al documento XML di output
        System.out.println("Generata lista peer " + listName + "Sel");
        liste.addContent(lsel);
      }
    }

    return liste;
  }

  /**
   * Genera le liste Peer.
   * @return elemento liste
   * @throws Exception
   */
  public Element genListeEdit()
     throws Exception
  {
    Element liste = new Element("listeEdit");

    Iterator iterTable = doc.getRootElement().getChildren("table", ns).iterator();
    while(iterTable.hasNext())
    {
      Element table = (Element) (iterTable.next());
      String tableName = table.getAttributeValue("name").toLowerCase();
      String ObjName = createPeerName(tableName);
      String listName = htLPeer.get(tableName);
      String formName = htForms.get(tableName);

      String descrizione = getListCaption(table);
      if(descrizione == null)
        descrizione = table.getAttributeValue("description");
      if(descrizione == null)
        descrizione = ObjName;

      String header = table.getAttributeValue("header");
      if(header == null)
        header = preparaHeader(tableName);

      Element nt = new Element(listName);
      nt.addContent(new Element("object").addContent(ObjName));
      nt.addContent(new Element("peer").addContent(ObjName + "Peer"));
      nt.addContent(new Element("header").addContent(header));
      nt.addContent(new Element("titolo").addContent(descrizione));
      nt.addContent(new Element("numppage").addContent("" + numPPage));
      nt.addContent(new Element("menu").addContent(ObjName));

      Element editInfo = new Element("edit-info");
      editInfo.addContent(new Element("url").addContent("@form?type=" + formName));
      nt.addContent(editInfo);

      List<Element> lsUnique = table.getChildren("unique", ns);
      if(!lsUnique.isEmpty())
      {
        boolean isValid = false;
        Element ppValidator = new Element("post-parse-validator");
        ppValidator.addContent(new Element("class").addContent("UndoLogicalDeleteValidator"));

        for(Element euni : lsUnique)
        {
          List<Element> lsUniCol = euni.getChildren("unique-column", ns);
          if(!lsUniCol.isEmpty())
          {
            Element alternateKey = new Element("alternate-key");
            ppValidator.addContent(alternateKey);

            for(Element eunicol : lsUniCol)
            {
              String colName = eunicol.getAttributeValue("name");
              alternateKey.addContent(new Element("field").addContent(colName));
              isValid = true;
            }
          }
        }

        if(isValid)
          nt.addContent(ppValidator);
      }

      int numPrimay = countPrimaryKeys(table);

      String foreignCodice = null, foreignDescription = null;
      Iterator iterCol = table.getChildren("column", ns).iterator();
      while(iterCol.hasNext())
      {
        Element column = (Element) (iterCol.next());
        String fieldName = column.getAttributeValue("name");

        if(isNoFieldListe(fieldName))
          continue;

        boolean isAutoIncrement = false;
        boolean isPrimaryKey = false;

        String s;
        if((s = column.getAttributeValue("autoIncrement")) != null)
          isAutoIncrement = StringOper.checkTrueFalse(s.trim(), false);
        if((s = column.getAttributeValue("primaryKey")) != null)
          isPrimaryKey = StringOper.checkTrueFalse(s.trim(), false);
        boolean isRequired = StringOper.checkTrue(column.getAttributeValue("required"));

        String columnName = getListCaption(column);
        if(columnName == null)
          columnName = fieldName;
        if(columnName.startsWith("ID_"))
          columnName = columnName.substring(3);
        columnName = columnName.replace('_', ' ');

        String peerColName = removeUnderScores(fieldName);

        String typeCol = column.getAttributeValue("type");
        int sizeCol = StringOper.parse(column.getAttributeValue("size"), defSize);
        if(typeCol.equals("CLOB"))
          sizeCol = formSizeMax;
        if(sizeCol > listSizeMax)
          sizeCol = listSizeMax;

        Element col = new Element("colonna");
        col.setAttribute("nome", columnName);
        col.addContent(new Element("campo").addContent(peerColName));
        col.addContent(new Element("size").addContent(Integer.toString(sizeCol)));

        if(isRequired)
          col.addContent(new Element("testnull"));

        if(fieldName.toUpperCase().startsWith("CODICE"))
          col.addContent(new Element("testcodice"));

        nt.addContent(col);

        if(isFieldDescrizione(fieldName))
        {
          col.setAttribute("ricerca-semplice", "true");
          foreignDescription = columnName;
        }

        if(isPrimaryKey)
        {
          col.setAttribute("primary", "true");

          Element ped = new Element("param");
          ped.setAttribute("nome", peerColName);
          ped.addContent("@" + peerColName);
          editInfo.addContent(ped);

          if(numPrimay == 1)
          {
            ped.setAttribute("nome", "ID");
            col.setAttribute("nome", "ID");
            col.setAttribute("editable", "false");
            foreignCodice = "ID";
          }
        }
      }

      if(foreignCodice != null && foreignDescription != null)
      {
        Element foreignServer = new Element("foreign-server");
        foreignServer.addContent(new Element("codice").setText(foreignCodice));
        foreignServer.addContent(new Element("descrizione").setText(foreignDescription));
        nt.addContent(foreignServer);
      }

      if(addEditCanc)
      {
        Element editcanc = new Element("colonna");
        editcanc.setAttribute("nome", "Fun.");
        editcanc.setAttribute("align", "center");
        editcanc.addContent(new Element("campo").addContent("#fun"));
        editcanc.addContent(new Element("size").addContent("10"));
        editcanc.addContent(new Element("caratteristiche").addContent(
           new Element("edit")).addContent(new Element("cancella")));
        nt.addContent(editcanc);
      }

      // aggiunge la lista al documento XML di output
      System.out.println("Generata lista-edit peer " + listName);
      liste.addContent(nt);

      if(addSelezione)
      {
        Element lsel = new Element(listName + "Sel");
        lsel.setAttribute("extend", listName);

        Element selezione = new Element("colonna");
        selezione.setAttribute("nome", "Sel.");
        selezione.setAttribute("align", "center");
        selezione.setAttribute("pos", "0");
        selezione.addContent(new Element("campo").addContent("#sel"));
        selezione.addContent(new Element("size").addContent("5"));
        selezione.addContent(new Element("caratteristiche").addContent(
           new Element("selezione")));
        lsel.addContent(selezione);

        // aggiunge la lista al documento XML di output
        System.out.println("Generata lista peer " + listName + "Sel");
        liste.addContent(lsel);
      }
    }

    return liste;
  }

  public Element genForms()
     throws Exception
  {
    Element forms = new Element("forms");

    Iterator iterTable = doc.getRootElement().getChildren("table", ns).iterator();
    while(iterTable.hasNext())
    {
      Element table = (Element) (iterTable.next());
      String tableName = table.getAttributeValue("name").toLowerCase();
      String formName = htForms.get(tableName);
      HashMap<String, ForeignInfo> htForeign = genForeignKeyInfo(table);
      int numPrimay = countPrimaryKeys(table);

      Element nt = new Element(formName);
      String ObjName = createPeerName(tableName);
      nt.addContent(new Element("object").addContent(ObjName));
      nt.addContent(new Element("peer").addContent(ObjName + "Peer"));
      nt.addContent(new Element("header").addContent(preparaHeader(tableName)));
      nt.addContent(new Element("titolo").addContent(ObjName));
      nt.addContent(new Element("numcolonne").addContent("" + numColonne));

      Element editInfo = new Element("edit-info");
      nt.addContent(editInfo);

      ForeignInfo eleForeign = null;
      Iterator iterCol = table.getChildren("column", ns).iterator();
      while(iterCol.hasNext())
      {
        Element column = (Element) (iterCol.next());
        String fieldName = column.getAttributeValue("name");

        if(fieldName.equalsIgnoreCase("STATO_REC"))
          continue;

        String columnName = getFormCaption(column);
        if(columnName == null)
          columnName = fieldName;
        if(columnName.startsWith("ID_"))
          columnName = columnName.substring(3);

        String peerColName = removeUnderScores(fieldName);

        boolean isPrimary = StringOper.checkTrue(column.getAttributeValue("primaryKey"));
        boolean isRequired = StringOper.checkTrue(column.getAttributeValue("required"));
        String typeCol = column.getAttributeValue("type");
        int sizeCol = StringOper.parse(column.getAttributeValue("size"), defSize);
        if(typeCol.equals("CLOB"))
          sizeCol = formSizeMax;
        if(sizeCol > formSizeMax)
          sizeCol = formSizeMax;

        Element col = new Element("colonna");
        col.addContent(new Element("campo").addContent(peerColName));
        col.addContent(new Element("size").addContent(Integer.toString(sizeCol)));
        col.addContent(new Element("testtipo"));

        if(fieldName.equalsIgnoreCase("ID_USER") || fieldName.equalsIgnoreCase("ID_UCREA"))
        {
          columnName = fieldName.equalsIgnoreCase("ID_UCREA") ? "Utente creazione"
                          : "Utente ultima modifica";
          Element foreignDisplay = new Element("foreign-display");
          foreignDisplay.addContent(new Element("tabella").addContent("turbine_user"));
          foreignDisplay.addContent(new Element("link").addContent("user_id"));
          foreignDisplay.addContent(new Element("display").addContent("first_name, last_name"));
          col.addContent(foreignDisplay);
          col.addContent(new Element("style").addContent(getStyleTechField()));
          col.setAttribute("editable", "false");
        }
        else if(fieldName.equalsIgnoreCase("ULT_MODIF") || fieldName.equalsIgnoreCase("CREAZIONE"))
        {
          columnName = fieldName.equalsIgnoreCase("CREAZIONE") ? "Data creazione"
                          : "Data ultima modifica";
          col.addContent(new Element("formatter").addContent(getDateFormatter()));
          col.addContent(new Element("style").addContent(getStyleTechField()));
          col.setAttribute("editable", "false");
        }
        else
        {
          if(isRequired)
            col.addContent(new Element("testnull"));

          if(fieldName.toUpperCase().startsWith("CODICE"))
            col.addContent(new Element("testcodice"));

          if((eleForeign = htForeign.get(fieldName)) != null)
          {
            String foreignListName = htLiSql.get(eleForeign.table.toLowerCase());
            Element foreignEdit = new Element("foreign-edit-auto");
            foreignEdit.addContent(new Element("mode").addContent("DISP_DESCR_EDIT"));
            if(addSelezione)
            {
              foreignEdit.addContent(new Element("url").addContent("@list?type=" + foreignListName + "Sel"));
              String foreignFormName = htForms.get(eleForeign.table.toLowerCase());
              if(foreignFormName != null)
              {
                Element foreignFormEdit = new Element("form");
                foreignFormEdit.addContent(new Element("url").addContent("@form?type=" + foreignFormName));
                foreignFormEdit.addContent(new Element("param").setAttribute("name", "ID").addContent("@" + peerColName));
                foreignEdit.addContent(foreignFormEdit);
              }
            }
            col.addContent(foreignEdit);
          }
        }

        col.setAttribute("nome", columnName.replace('_', ' '));
        nt.addContent(col);

        if(isPrimary)
        {
          col.setAttribute("primary", "true");

          Element ped = new Element("param");
          ped.setAttribute("nome", peerColName);
          ped.addContent("@" + peerColName);
          editInfo.addContent(ped);

          if(numPrimay == 1)
          {
            ped.setAttribute("nome", "ID");
            col.setAttribute("nome", "ID");
            col.setAttribute("editable", "false");
          }
        }
      }

      // aggiunge la lista al documento XML di output
      System.out.println("Generato form " + formName);
      forms.addContent(nt);
    }

    return forms;
  }

  public Element genListeSql()
     throws Exception
  {
    Element liste = new Element("liste-sql");

    Iterator iterTable = doc.getRootElement().getChildren("table", ns).iterator();
    while(iterTable.hasNext())
    {
      Element table = (Element) (iterTable.next());
      String tableName = table.getAttributeValue("name").toLowerCase();
      String listName = htLiSql.get(tableName);
      String formName = htForms.get(tableName);
      String ObjName = tableName;

      String descrizione = getListCaption(table);
      if(descrizione == null)
        descrizione = table.getAttributeValue("description");
      if(descrizione == null)
        descrizione = ObjName;

      String header = table.getAttributeValue("header");
      if(header == null)
        header = preparaHeader(tableName);

      Element nt = new Element(listName);
      nt.addContent(new Element("header").addContent(header));
      nt.addContent(new Element("titolo").addContent(descrizione));
      nt.addContent(new Element("numppage").addContent("" + numPPage));
      nt.addContent(new Element("menu").addContent(ObjName));

      Element editInfo = new Element("edit-info");
      editInfo.addContent(new Element("url").addContent("@form?type=" + formName));
      nt.addContent(editInfo);

      int numPrimay = countPrimaryKeys(table);

      boolean haveStatoRec = false;
      String primaryKey = null, strSelect = "";
      String foreignCodice = null, foreignDescription = null;
      Iterator iterCol = table.getChildren("column", ns).iterator();
      while(iterCol.hasNext())
      {
        Element column = (Element) (iterCol.next());
        String fieldName = column.getAttributeValue("name");

        if(fieldName.equalsIgnoreCase("stato_rec"))
          haveStatoRec = true;

        if(isNoFieldListe(fieldName))
          continue;

        String columnName = getListCaption(column);
        if(columnName == null)
          columnName = fieldName;
        if(columnName.startsWith("ID_"))
          columnName = columnName.substring(3);
        columnName = columnName.replace('_', ' ');

        String peerColName = removeUnderScores(fieldName);
        strSelect += ", T." + fieldName;
        boolean isPrimary = StringOper.checkTrue(column.getAttributeValue("primaryKey"));
        boolean isRequired = StringOper.checkTrue(column.getAttributeValue("required"));
        String typeCol = column.getAttributeValue("type");
        int sizeCol = StringOper.parse(column.getAttributeValue("size"), defSize);
        if(sizeCol > listSizeMax)
          sizeCol = listSizeMax;

        Element col = new Element("colonna");
        col.setAttribute("nome", columnName);
        col.addContent(new Element("campo").addContent(fieldName));
        col.addContent(new Element("size").addContent(Integer.toString(sizeCol)));
        col.addContent(new Element("tipo").addContent(cvtSqlTypeRigelType(typeCol)));
        nt.addContent(col);

        if(isFieldDescrizione(fieldName))
        {
          col.setAttribute("ricerca-semplice", "true");
          foreignDescription = columnName;
        }

        if(isPrimary)
        {
          col.setAttribute("primary", "true");

          Element ped = new Element("param");
          ped.setAttribute("nome", peerColName);
          ped.addContent("@" + fieldName);
          editInfo.addContent(ped);
          primaryKey = fieldName;

          if(numPrimay == 1)
          {
            ped.setAttribute("nome", "ID");
            col.setAttribute("nome", "ID");
            col.setAttribute("editable", "false");
            foreignCodice = "ID";
          }
        }
      }

      nt.addContent(new Element("select").addContent(strSelect.substring(2)));
      nt.addContent(new Element("from").addContent(tableName.toUpperCase() + useAs + "T"));
      nt.addContent(new Element("delete-from").addContent(tableName.toUpperCase()));
      if(haveStatoRec)
        nt.addContent(new Element("where").addContent(new CDATA("(T.STATO_REC IS NULL OR T.STATO_REC < 10)")));
      if(primaryKey != null)
        nt.addContent(new Element("orderby").addContent(primaryKey));

      if(foreignCodice != null && foreignDescription != null)
      {
        Element foreignServer = new Element("foreign-server");
        foreignServer.addContent(new Element("codice").setText(foreignCodice));
        foreignServer.addContent(new Element("descrizione").setText(foreignDescription));
        nt.addContent(foreignServer);
      }

      if(addEditCanc)
      {
        Element editcanc = new Element("colonna");
        editcanc.setAttribute("nome", "Fun.");
        editcanc.setAttribute("align", "center");
        editcanc.addContent(new Element("campo").addContent("#fun"));
        editcanc.addContent(new Element("size").addContent("10"));
        editcanc.addContent(new Element("caratteristiche").addContent(
           new Element("edit")).addContent(new Element("cancella")));
        nt.addContent(editcanc);
      }

      // aggiunge la lista al documento XML di output
      System.out.println("Generata lista sql " + listName);
      liste.addContent(nt);

      if(addSelezione)
      {
        Element lsel = new Element(listName + "Sel");
        lsel.setAttribute("extend", listName);

        Element selezione = new Element("colonna");
        selezione.setAttribute("nome", "Sel.");
        selezione.setAttribute("align", "center");
        selezione.setAttribute("pos", "0");
        selezione.addContent(new Element("campo").addContent("#sel"));
        selezione.addContent(new Element("size").addContent("5"));
        selezione.addContent(new Element("caratteristiche").addContent(
           new Element("selezione")));
        lsel.addContent(selezione);

        // aggiunge la lista al documento XML di output
        System.out.println("Generata lista sql " + listName + "Sel");
        liste.addContent(lsel);
      }
    }

    return liste;
  }

  public void genSqlfile(PrintStream pw)
     throws Exception
  {
    switch(StringOper.okStr(adapter).toLowerCase())
    {
      default:
      case "postgresql":
        pw.print(""
           + "-------------------------------\n"
           + "-- DISTRUZIONE VECCHIE VISTE --\n"
           + "-------------------------------\n\n"
        );
        break;

      case "mysql":
        pw.print(""
           + "#------------------------------\n"
           + "#- DISTRUZIONE VECCHIE VISTE --\n"
           + "#------------------------------\n\n"
        );
        break;
    }

    Iterator iterTableDrop = doc.getRootElement().getChildren("table", ns).iterator();
    while(iterTableDrop.hasNext())
    {
      Element table = (Element) (iterTableDrop.next());
      String tableName = table.getAttributeValue("name").toLowerCase();

      pw.print("DROP VIEW " + tableName.toUpperCase() + "_VIEW CASCADE;\n");
      pw.print("DROP VIEW " + tableName.toUpperCase() + "_VIEW_ALL CASCADE;\n");
    }

    pw.print("\n\n");

    switch(StringOper.okStr(adapter).toLowerCase())
    {
      default:
      case "postgresql":
        pw.print(""
           + "---------------------------\n"
           + "-- CREAZIONE NUOVE VISTE --\n"
           + "---------------------------\n\n"
        );
        break;

      case "mysql":
        pw.print(""
           + "#--------------------------\n"
           + "#- CREAZIONE NUOVE VISTE --\n"
           + "#--------------------------\n\n"
        );
        break;
    }

    Iterator iterTableCreate = doc.getRootElement().getChildren("table", ns).iterator();
    while(iterTableCreate.hasNext())
    {
      Element table = (Element) (iterTableCreate.next());
      String tableName = table.getAttributeValue("name").toLowerCase();

      int count = 0;
      boolean haveStatoRec = false;
      String primaryKey = null, strSelect = "";
      Iterator iterCol = table.getChildren("column", ns).iterator();
      while(iterCol.hasNext())
      {
        Element column = (Element) (iterCol.next());
        String fieldName = column.getAttributeValue("name");

        if(fieldName.equalsIgnoreCase("stato_rec"))
          haveStatoRec = true;

        // byNIK: 2020-03-09 sulla tabella principale servono spesso anche i campi tecnici
        //if(isNoFieldListe(fieldName))
        //  continue;
        strSelect += ", T." + fieldName.toLowerCase();
        boolean isPrimary = StringOper.checkTrue(column.getAttributeValue("primaryKey"));

        if(isPrimary)
          primaryKey = fieldName;
      }

      // <foreign-key foreignTable="DB_LISTINO">
      //   <reference local="IDLISTINO" foreign="IDLISTINO" />
      // </foreign-key>
      String prein = "", inner = "", selin = "";
      Iterator iterFor = table.getChildren("foreign-key", ns).iterator();
      for(int ct = 0; iterFor.hasNext(); ct++)
      {
        String talias = "J" + Integer.toString(ct, 26).toUpperCase();
        Element fk = (Element) (iterFor.next());
        String foreignTable = fk.getAttributeValue("foreignTable");
        if(foreignTable == null)
          throw new Exception("Definizione errata di foreignTable per " + tableName);

        String cond = "";
        Iterator itrRef = fk.getChildren("reference", ns).iterator();
        while(itrRef.hasNext())
        {
          Element ref = (Element) itrRef.next();
          String local = ref.getAttributeValue("local");
          String foreign = ref.getAttributeValue("foreign");
          cond += " AND T." + local.toLowerCase() + "=" + talias + "." + foreign.toLowerCase();
        }

        prein += "(";
        inner += "  INNER JOIN " + foreignTable.toLowerCase() + useAs + talias + " ON "
           + cond.substring(5) + ")\n";

        Element ftable = getTable(foreignTable);
        if(ftable != null)
          selin += "  " + buidlFieldList(ftable, talias) + ",\n";
      }

      // tabelle primo livello join filtra STATO_REC ovvero esclude cancellati logicamente
      pw.print(
         "CREATE VIEW " + tableName.toUpperCase() + "_VIEW AS\n"
         + "SELECT \n"
      );

      if(selin.isEmpty())
      {
        pw.print("  " + strSelect.substring(2) + "\n");
      }
      else
      {
        pw.print("  " + strSelect.substring(2) + ",\n");
        pw.print(selin.substring(0, selin.length() - 2) + "\n");
      }

      pw.print(
         "FROM " + prein + tableName.toUpperCase() + useAs + "T\n"
         + inner
         + (haveStatoRec ? "WHERE (T.STATO_REC IS NULL OR T.STATO_REC < 10)\n" : "")
         + "ORDER BY " + primaryKey + ";\n\n\n"
      );

      // tabelle primo livello join ignora STATO_REC ovvero include cancellati logicamente
      pw.print(
         "CREATE VIEW " + tableName.toUpperCase() + "_VIEW_ALL AS\n"
         + "SELECT \n"
      );

      if(selin.isEmpty())
      {
        pw.print("  " + strSelect.substring(2) + "\n");
      }
      else
      {
        pw.print("  " + strSelect.substring(2) + ",\n");
        pw.print(selin.substring(0, selin.length() - 2) + "\n");
      }

      pw.print(
         "FROM " + prein + tableName.toUpperCase() + useAs + "T\n"
         + inner
         + "ORDER BY " + primaryKey + ";\n\n\n"
      );

    }
  }

  public String preparaHeader(String tableName)
  {
    tableName = tableName.toUpperCase();
    String[] toRemove = getArtoRemove();

    for(int i = 0; i < toRemove.length; i++)
    {
      String sr = toRemove[i];
      if(tableName.startsWith(sr))
        tableName = tableName.substring(sr.length());
    }

    return tableName.replace('_', ' ');
  }

  public int countPrimaryKeys(Element table)
  {
    int count = 0;
    Iterator iterCol = table.getChildren("column", ns).iterator();
    while(iterCol.hasNext())
    {
      Element column = (Element) (iterCol.next());
      boolean isPrimary = StringOper.checkTrue(column.getAttributeValue("primaryKey"));

      if(isPrimary)
        count++;
    }
    return count;
  }

  public String buidlFieldList(Element table, String prefix)
  {
    int count = 0;
    String strSelect = "";
    Iterator iterCol = table.getChildren("column", ns).iterator();
    while(iterCol.hasNext())
    {
      Element column = (Element) (iterCol.next());
      String fieldName = column.getAttributeValue("name");

      if(isNoFieldListe(fieldName))
        continue;

      fieldName = fieldName.toLowerCase();
      strSelect += ", " + prefix + "." + fieldName + useAs + prefix + "_" + fieldName;
    }

    return strSelect.substring(2);
  }

  public String cvtSqlTypeRigelType(String sqlType)
  {
    String rv = sql2Rigel.get(sqlType.toUpperCase());
    return rv == null ? "PDT_STRING" : rv;
  }

  public void run()
     throws Exception
  {
    parseXml();
    ns = doc.getRootElement().getNamespace();

    genNames();

    if(ouputFile != null)
    {
      // creazione documento XML di output
      Document docOutput = new Document(new Element("root"));

      // riempie con dati liste
      // deprecato: utilizzare liset SQL
      // docOutput.getRootElement().addContent(genListe());
      // riempie con dati forms
      docOutput.getRootElement().addContent(genForms());

      // riempie con dati liste sql
      docOutput.getRootElement().addContent(genListeSql());

      // riempie con dati liste edit
      docOutput.getRootElement().addContent(genListeEdit());

      // output del documento
      XMLOutputter xout = new XMLOutputter();
      xout.setFormat(Format.getPrettyFormat());

      try ( FileOutputStream fis = new FileOutputStream(ouputFile))
      {
        xout.output(docOutput, fis);
      }
    }

    if(sqloutput != null)
    {
      try ( PrintStream ps = new PrintStream(sqloutput))
      {
        genSqlfile(ps);
      }
    }
    else
      genSqlfile(System.out);
  }

  public void runArgs(String[] args)
  {
    try
    {
      int c;
      String arg;
      LongOptExt[] longopts = new LongOptExt[]
      {
        new LongOptExt("help", LongOpt.NO_ARGUMENT, null, 'h', ""),
        new LongOptExt("outputfile", LongOpt.REQUIRED_ARGUMENT, null, 'o', ""),
        new LongOptExt("sqloutput", LongOpt.REQUIRED_ARGUMENT, null, 'q', ""),
        new LongOptExt("debug", LongOpt.NO_ARGUMENT, null, 'D', ""),
        new LongOptExt("verbose", LongOpt.NO_ARGUMENT, null, 'v', ""),
      };

      Getopt g = new Getopt("genliste", args, "d:ho:i:c:p:s:l:f:q:vSCD", longopts);
      g.setOpterr(false); // We'll do our own error handling

      while((c = g.getopt()) != -1)
        switch(c)
        {
          case 'd':
            dtdDir = new File(g.getOptarg());
            if(!dtdDir.exists())
            {
              System.err.println("La directory " + dtdDir.getAbsolutePath() + " non esiste.");
              System.exit(-1);
            }
            if(!dtdDir.isDirectory())
            {
              System.err.println(dtdDir.getAbsolutePath() + " esiste ma non e' una directory.");
              System.exit(-1);
            }
            break;

          case 'o':
            ouputFile = g.getOptarg();
            break;

          case 'i':
            numIndent = Integer.parseInt(g.getOptarg());
            break;

          case 'c':
            numColonne = Integer.parseInt(g.getOptarg());
            break;

          case 'p':
            numPPage = Integer.parseInt(g.getOptarg());
            break;

          case 's':
            defSize = StringOper.parse(g.getOptarg(), 10);
            break;

          case 'l':
            lPeerRadix = g.getOptarg();
            break;

          case 'f':
            formsRadix = g.getOptarg();
            break;

          case 'q':
            sqloutput = g.getOptarg();
            break;

          case 'h':
            System.out.println(
               "Modo d'uso\n\n"
               + "-o  specifica un file di output\n"
               + "-q  specifica file SQL di output\n"
               + "-i  caratteri di identazione (2)\n"
               + "-c  numero colonne (3)\n"
               + "-p  elementi per pagina (20)\n"
               + "-s  dimesione di default per campi senza size (10)\n"
               + "-l  nome radice delle liste\n"
               + "-f  nome radice dei forms\n"
               + "-S  sopprime la generazione della colonna selezione\n"
               + "-C  sopprime la generazione delle colonna edit\n\n"
               + "file input di default ele-schema.xml");
            return;

          case 'S':
            addSelezione = false;
            break;

          case 'C':
            addEditCanc = false;
            break;

          case 'v':
            verbose = true;
            break;

          case 'D':
            debug = true;
            break;

          case '?':
            System.out.println("The option '" + (char) g.getOptopt() + "' is not valid");
            break;
        }

      for(int i = g.getOptind(); i < args.length; i++)
      {
        xmlFile = args[i];
        break;
      }

      run();
    }
    catch(Exception ex)
    {
      Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Remove Underscores from a string and replaces first
   * Letters with Capitals.foo_bar becomes FooBar
   * @param data
   * @return
   */
  public static String removeUnderScores(String data)
  {
    String[] arTok = data.split("[\\.|_]");
    StringBuilder out = new StringBuilder();
    for(int i = 0; i < arTok.length; i++)
    {
      String element = arTok[i];
      out.append(StringUtils.capitalize(element));
    }
    return out.toString();
  }

  private HashMap<String, ForeignInfo> genForeignKeyInfo(Element table)
  {
    HashMap<String, ForeignInfo> rv = new HashMap<String, ForeignInfo>();
    Iterator itr = table.getChildren("foreign-key", ns).iterator();
    while(itr.hasNext())
    {
      Element child = (Element) itr.next();
      ForeignInfo fi = new ForeignInfo();
      List lsRef = child.getChildren("reference", ns);
      if(lsRef.size() != 1)
        continue;

      Element ref = (Element) lsRef.get(0);
      fi.table = child.getAttributeValue("foreignTable");
      fi.local = ref.getAttributeValue("local");
      fi.foreign = ref.getAttributeValue("foreign");
      rv.put(fi.local, fi);
    }
    return rv;
  }

  public static class ForeignInfo
  {
    String table, local, foreign;
  }

  public String getListCaption(Element ele)
  {
    String descrizione = ele.getAttributeValue("list-caption");
    if(descrizione == null)
      descrizione = ele.getAttributeValue("caption");
    return descrizione;
  }

  public String getFormCaption(Element ele)
  {
    String descrizione = ele.getAttributeValue("form-caption");
    if(descrizione == null)
      descrizione = ele.getAttributeValue("caption");
    return descrizione;
  }
}
