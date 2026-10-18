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
import org.commonlib5.lambda.FunctionTrowException;
import org.commonlib5.utils.ClassOper;
import org.commonlib5.utils.LongOptExt;
import org.commonlib5.utils.StringJoin;
import org.commonlib5.utils.StringOper;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * <p>
 * Title: Test di coerenza dello schema.</p>
 * <p>
 * Description: Esegue dei test di coerenza sullo schema XML che
 * rappresenta la struttura del database. Questa e' una applicazione
 * da eseguire in una console.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class AbstractTestSchema extends ParseSchemaBase
{
  protected boolean checkID = true;
  protected boolean checkIndex = true;
  protected boolean checkVarchar = true;
  protected boolean checkForeign = true;
  protected boolean checkCircular = true;
  protected boolean verbose = false, debug = false;
  protected int numErrors, numWarnings;
  protected Namespace ns;
  protected String nomeApp = ClassOper.getClassName(this.getClass());
  protected String nomeTabellaFor = null, nomeColonnaFor = null;
  protected HashMap<String, Element> htTabelle = new HashMap<>();
  protected HashMap<String, Element> htColumns = new HashMap<>();
  protected String[] ignore =
  {
    "ID_USER", "ID_UCREA"
  };

  public AbstractTestSchema()
  {
    numErrors = numWarnings = 0;
  }

  public void runTest()
     throws Exception
  {
    ns = doc.getRootElement().getNamespace();

    for(Element table : doc.getRootElement().getChildren("table", ns))
    {
      testTable(table);
    }
  }

  public void testTable(Element table)
     throws Exception
  {
    String nomeTable = table.getAttributeValue("name");
    System.out.println("Analizzo tabella " + nomeTable + "...");

    if(htTabelle.get(nomeTable) != null)
    {
      System.out.println("ERROR: tabella " + nomeTable + " duplicata.");
      numErrors++;
      return;
    }

    htTabelle.put(nomeTable, table);

    if(checkCircular)
    {
      Stack<Element> stkTabelle = new Stack<>();
      if(checkRelazioniCircolari(table, stkTabelle))
      {
        String tabelleCirc = StringJoin.build().addObjects(stkTabelle, (e) -> e.getAttributeValue("name")).join();
        System.out.println("ERROR: tabella " + nomeTable + " in relazione circolare: " + tabelleCirc);
        numErrors++;
      }
    }

    String idMethod = table.getAttributeValue("idMethod");
    boolean autoIncrementTable = StringOper.isEquAny(idMethod, "native", "idbroker");

    int numKeys = 0;
    htColumns.clear();

    for(Element column : table.getChildren("column", ns))
    {
      String nomeCol = column.getAttributeValue("name");

      if(htColumns.get(nomeCol) != null)
      {
        System.out.println("ERROR: tabella " + nomeTable + ": colonna " + nomeCol + " duplicata.");
        numErrors++;
        continue;
      }

      htColumns.put(nomeCol, column);

      if(isIgnore(nomeCol))
        continue;

      String type = column.getAttributeValue("type");
      String size = column.getAttributeValue("size");
      boolean pk = StringOper.checkTrue(column.getAttributeValue("primaryKey"));
      boolean req = StringOper.checkTrue(column.getAttributeValue("required"));
      boolean ai = StringOper.checkTrue(column.getAttributeValue("autoIncrement"));

      if(pk)
      {
        numKeys++;
        if(!req)
        {
          System.out.println("WARNING: tabella " + nomeTable + ": colonna " + nomeCol
             + " chiave primaria ma NON required.");
          numWarnings++;
        }

        if(ai && !autoIncrementTable)
        {
          System.out.println("ERROR: tabella " + nomeTable + ": colonna " + nomeCol
             + " dichiarata autoincrement ma non la tabella.");
          numErrors++;
        }

        if(!ai && autoIncrementTable)
        {
          System.out.println("ERROR: tabella " + nomeTable + ": colonna " + nomeCol
             + " tabella autoincrement ma non la chiave primaria.");
          numErrors++;
        }
      }

      // controlla se il campo e' chiave esterna
      Element elForeign = cercaForeign(table, nomeCol);

      if(checkID)
      {
        if(nomeCol.startsWith("ID_"))
        {
          // probabile chiave esterna: la cerca nelle foreign keys
          if(elForeign == null)
          {
            System.out.println("WARNING: colonna " + nomeTable + "." + nomeCol + " non presente nelle foreign key.");
            numWarnings++;
          }
        }
      }

      if(checkForeign && elForeign != null)
      {
        try
        {
          Element colForeign = getColumn(nomeTabellaFor, nomeColonnaFor);
          if(!checkType(column, colForeign))
          {
            System.out.println("ERROR: " + nomeTable + "." + nomeCol + " chiave esterna di " + nomeTabellaFor + "." + nomeColonnaFor + " campo di tipo e/o dimensioni differenti!");
            numErrors++;
          }
        }
        catch(Exception e)
        {
          System.out.println("ERROR in " + nomeTable + " (REMOTE FOREIGN): " + e.getMessage());
          numErrors++;
        }
      }

      if(checkVarchar)
      {
        if(type.equalsIgnoreCase("varchar") && size == null)
        {
          System.out.println("ERROR: colonna " + nomeTable + "." + nomeCol + " varchar senza dimensione specificata.");
          numErrors++;

          if(verbose)
            dumpAttribute(column);
        }
      }
    }

    // testa le foreign key per verificare se le colonne specificate
    // esistono realmente sulla tabella corrente
    if(checkForeign)
    {
      for(Element itemFor : table.getChildren("foreign-key", ns))
      {
        for(Element itemRef : itemFor.getChildren("reference", ns))
        {
          String nomeLocalForeignCol = itemRef.getAttributeValue("local");
          try
          {
            Element localColumn = getColumn(table, nomeLocalForeignCol);
          }
          catch(Exception e)
          {
            System.out.println("ERROR in " + nomeTable + "." + nomeLocalForeignCol + " (LOCAL FOREIGN): " + e.getMessage());
            numErrors++;
          }
        }
      }
    }

    if(checkIndex)
    {
      for(Element itemIdx : table.getChildren("index", ns))
      {
        for(Element itemCol : itemIdx.getChildren("index-column", ns))
        {
          String nomeColonna = itemCol.getAttributeValue("name");
          if(htColumns.get(nomeColonna) == null)
          {
            System.out.println("ERROR in " + nomeTable + " indice su colonna " + nomeColonna + " inesistente.");
            numErrors++;
          }
        }
      }

      for(Element itemIdx : table.getChildren("unique", ns))
      {
        for(Element itemCol : itemIdx.getChildren("unique-column", ns))
        {
          String nomeColonna = itemCol.getAttributeValue("name");
          if(htColumns.get(nomeColonna) == null)
          {
            System.out.println("ERROR in " + nomeTable + " indice su colonna " + nomeColonna + " inesistente.");
            numErrors++;
          }
        }
      }
    }
  }

  public Element cercaForeign(Element table, String nomeCampo)
     throws Exception
  {
    for(Element itemFor : table.getChildren("foreign-key", ns))
    {
      for(Element itemRef : itemFor.getChildren("reference", ns))
      {
        if(itemRef.getAttributeValue("local").equals(nomeCampo))
        {
          nomeTabellaFor = itemFor.getAttributeValue("foreignTable");
          nomeColonnaFor = itemRef.getAttributeValue("foreign");
          return itemRef;
        }
      }
    }
    return null;
  }

  public Element getTable(String tabella)
     throws Exception
  {
    for(Element table : doc.getRootElement().getChildren("table", ns))
    {
      String nomeTable = table.getAttributeValue("name");
      if(nomeTable.equals(tabella))
        return table;
    }
    throw new Exception("Tabella " + tabella + " inesistente.");
  }

  public Element getColumn(String tabella, String colonna)
     throws Exception
  {
    return getColumn(getTable(tabella), colonna);
  }

  public Element getColumn(Element table, String colonna)
     throws Exception
  {
    String nomeTable = table.getAttributeValue("name");
    for(Element column : table.getChildren("column", ns))
    {
      String nomeCol = column.getAttributeValue("name");
      if(nomeCol.equals(colonna))
        return column;
    }
    throw new Exception("Colonna " + nomeTable + "." + colonna + " inesistente.");
  }

  public boolean checkRelazioniCircolari(Element table, Stack<Element> stkTabelle)
     throws Exception
  {
    String nomeTable = table.getAttributeValue("name");
    if(stkTabelle.stream().anyMatch((e) -> nomeTable.equals(e.getAttributeValue("name"))))
      return true;

    stkTabelle.push(table);
    for(Element itemFor : table.getChildren("foreign-key", ns))
    {
      String nomeTabForeign = itemFor.getAttributeValue("foreignTable");
      Element tableForeign = getTable(nomeTabForeign);
      if(checkRelazioniCircolari(tableForeign, stkTabelle))
        return true;
    }

    stkTabelle.pop();
    return false;
  }

  public boolean checkType(Element colonna1, Element colonna2)
  {
    String type1 = colonna1.getAttributeValue("type");
    String type2 = colonna2.getAttributeValue("type");
    if(type1 == null || type2 == null || !type1.equals(type2))
      return false;

    String size1 = colonna1.getAttributeValue("size");
    String size2 = colonna2.getAttributeValue("size");
    if((size1 == null && size2 != null)
       || (size1 != null && size2 == null))
      return false;

    if(size1 != null && size2 != null
       && !size1.equals(size2))
      return false;

    //System.out.println("**"+type1+":"+type2+"     "+size1+":"+size2);
    return true;
  }

  public boolean isIgnore(String nomeCampo)
  {
    for(int i = 0; i < ignore.length; i++)
    {
      if(nomeCampo.equalsIgnoreCase(ignore[i]))
        return true;
    }

    return false;
  }

  public void dumpAttribute(Element ele)
  {
    List lAttr = ele.getAttributes();
    for(Iterator it = lAttr.iterator(); it.hasNext();)
    {
      Attribute attribute = (Attribute) it.next();
      System.out.println(attribute.getName() + "=" + attribute.getValue());
    }
  }

  public void runArgs(String[] args)
  {
    runArgs(args, Collections.EMPTY_LIST, null);
  }

  public void runArgs(String[] args, Collection<LongOptExt> moreOptions, FunctionTrowException<Character, Boolean> extendOptionHandler)
  {
    try
    {
      int c;
      List<LongOptExt> opzioni = new ArrayList<>();
      opzioni.add(new LongOptExt("dtddir", LongOpt.REQUIRED_ARGUMENT, null, 'd', "directory per dtd"));
      opzioni.add(new LongOptExt("verbose", LongOpt.NO_ARGUMENT, null, 'v', ""));
      opzioni.add(new LongOptExt("debug", LongOpt.NO_ARGUMENT, null, 'D', ""));
      opzioni.add(new LongOptExt("help", LongOpt.NO_ARGUMENT, null, 'h', ""));
      opzioni.addAll(moreOptions);

      LongOptExt[] longopts = opzioni.toArray(LongOptExt[]::new);
      Getopt g = new Getopt("testschema", args, LongOptExt.getOptstring(longopts), longopts);
      g.setOpterr(false); // We'll do our own error handling

      while((c = g.getopt()) != -1)
      {
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

          case 'v':
            verbose = true;
            break;

          case 'D':
            debug = true;
            break;

          default:
            if(extendOptionHandler != null)
            {
              if(extendOptionHandler.apply((char) c))
                break;
            }
          case '?':
            System.out.println("Opzione '" + c + "' sconosciuta.");
          case 'h':
            helpExit(opzioni);
            break;

        }
      }

      for(int i = g.getOptind(); i < args.length; i++)
      {
        xmlFile = args[i];
        System.out.println("Working on file " + xmlFile);
        parseXml();
        runTest();
      }

      System.out.println("Warnings: " + numWarnings);
      System.out.println("Errori:   " + numErrors);

      if(numErrors > 0)
        System.exit(-1);
    }
    catch(Throwable ex)
    {
      Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void helpExit(List<LongOptExt> opzioni)
  {
    System.out.printf(
       "%s\n"
       + "modo d'uso:\n"
       + "  %s [options] file-schema1.xml ... file-schemaN.xml\n", nomeApp, nomeApp);

    for(LongOptExt l : opzioni)
    {
      System.out.println(l.getHelpMsg());
    }
  }
}
