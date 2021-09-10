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
package org.rigel2.db;

import gnu.getopt.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.commonlib.utils.LongOptExt;
import org.commonlib.utils.StringOper;
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
  protected boolean verbose = false, debug = false;
  protected String nomeTabellaFor = null, nomeColonnaFor = null;
  protected HashMap<String, Element> htTabelle = new HashMap<>();
  protected HashMap<String, Element> htColumns = new HashMap<>();
  protected String[] ignore =
  {
    "ID_USER", "ID_UCREA"
  };

  public AbstractTestSchema()
  {
  }

  public void testId()
     throws Exception
  {
    Namespace ns = doc.getRootElement().getNamespace();

    Iterator iterTable = (doc.getRootElement().getChildren("table", ns)).iterator();
    while(iterTable.hasNext())
    {
      Element table = (Element) (iterTable.next());
      String nomeTable = table.getAttributeValue("name");
      System.out.println("Analizzo tabella " + nomeTable + "...");

      if(htTabelle.get(nomeTable) != null)
      {
        System.out.println("ERROR: tabella " + nomeTable + " duplicata.");
        continue;
      }

      htTabelle.put(nomeTable, table);

      String idMethod = table.getAttributeValue("idMethod");
      boolean autoIncrementTable = StringOper.isEquAny(idMethod, "native", "idbroker");

      int numKeys = 0;
      htColumns.clear();
      Iterator iterColumn = (table.getChildren("column", ns)).iterator();
      while(iterColumn.hasNext())
      {
        Element column = (Element) (iterColumn.next());
        String nomeCol = column.getAttributeValue("name");

        if(htColumns.get(nomeCol) != null)
        {
          System.out.println("ERROR: tabella " + nomeTable + ": colonna " + nomeCol
             + " duplicata.");
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
            System.out.println("WARNING: tabella " + nomeTable + ": colonna " + nomeCol
               + " chiave primaria ma NON required.");

          if(ai && !autoIncrementTable)
            System.out.println("ERROR: tabella " + nomeTable + ": colonna " + nomeCol
               + " dichiarata autoincrement ma non la tabella.");

          if(!ai && autoIncrementTable)
            System.out.println("ERROR: tabella " + nomeTable + ": colonna " + nomeCol
               + " tabella autoincrement ma non la chiave primaria.");
        }

        // controlla se il campo e' chiave esterna
        Element elForeign = cercaForeign(ns, table, nomeCol);

        if(checkID)
        {
          if(nomeCol.startsWith("ID_"))
          {
            // probabile chiave esterna: la cerca nelle foreign keys
            if(elForeign == null)
              System.out.println("WARNING: colonna " + nomeTable + "." + nomeCol + " non presente nelle foreign key.");
          }
        }

        if(checkForeign && elForeign != null)
        {
          try
          {
            Element colForeign = getColumn(ns, nomeTabellaFor, nomeColonnaFor);
            if(!checkType(column, colForeign))
              System.out.println("ERROR: " + nomeTable + "." + nomeCol + " chiave esterna di " + nomeTabellaFor + "." + nomeColonnaFor + " campo di tipo e/o dimensioni differenti!");
          }
          catch(Exception e)
          {
            System.out.println("ERROR in " + nomeTable + " (REMOTE FOREIGN): " + e.getMessage());
          }
        }

        if(checkVarchar)
        {
          if(type.equalsIgnoreCase("varchar") && size == null)
          {
            System.out.println("ERROR: colonna " + nomeTable + "." + nomeCol + " varchar senza dimensione specificata.");
            if(verbose)
              dumpAttribute(column);
          }
        }
      }

      // testa le foreign key per verificare se le colonne specificate
      // esistono realmente sulla tabella corrente
      if(checkForeign)
      {
        Iterator iterFor = (table.getChildren("foreign-key", ns)).iterator();
        while(iterFor.hasNext())
        {
          Element itemFor = (Element) (iterFor.next());
          Iterator iterRef = (itemFor.getChildren("reference", ns)).iterator();
          while(iterRef.hasNext())
          {
            Element itemRef = (Element) (iterRef.next());
            String nomeLocalForeignCol = itemRef.getAttributeValue("local");
            try
            {
              Element localColumn = getColumn(ns, table, nomeLocalForeignCol);
            }
            catch(Exception e)
            {
              System.out.println("ERROR in " + nomeTable + "." + nomeLocalForeignCol + " (LOCAL FOREIGN): " + e.getMessage());
            }
          }
        }
      }

      if(checkIndex)
      {
        Iterator iterIndex = (table.getChildren("index", ns)).iterator();
        while(iterIndex.hasNext())
        {
          Element itemIdx = (Element) (iterIndex.next());
          Iterator iterCol = (itemIdx.getChildren("index-column", ns)).iterator();
          while(iterCol.hasNext())
          {
            Element itemCol = (Element) (iterCol.next());
            String nomeColonna = itemCol.getAttributeValue("name");
            if(htColumns.get(nomeColonna) == null)
            {
              System.out.println("ERROR in " + nomeTable + " indice su colonna " + nomeColonna + " inesistente.");
            }
          }
        }

        iterIndex = (table.getChildren("unique", ns)).iterator();
        while(iterIndex.hasNext())
        {
          Element itemIdx = (Element) (iterIndex.next());
          Iterator iterCol = (itemIdx.getChildren("unique-column", ns)).iterator();
          while(iterCol.hasNext())
          {
            Element itemCol = (Element) (iterCol.next());
            String nomeColonna = itemCol.getAttributeValue("name");
            if(htColumns.get(nomeColonna) == null)
            {
              System.out.println("ERROR in " + nomeTable + " indice su colonna " + nomeColonna + " inesistente.");
            }
          }
        }
      }

    }
  }

  public Element cercaForeign(Namespace ns, Element table, String nomeCampo)
     throws Exception
  {
    Iterator iterFor = (table.getChildren("foreign-key", ns)).iterator();
    while(iterFor.hasNext())
    {
      Element itemFor = (Element) (iterFor.next());
      Iterator iterRef = (itemFor.getChildren("reference", ns)).iterator();
      while(iterRef.hasNext())
      {
        Element itemRef = (Element) (iterRef.next());
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

  public Element getColumn(Namespace ns, String tabella, String colonna)
     throws Exception
  {
    Iterator iterTable = (doc.getRootElement().getChildren("table", ns)).iterator();
    while(iterTable.hasNext())
    {
      Element table = (Element) (iterTable.next());
      String nomeTable = table.getAttributeValue("name");
      if(nomeTable.equals(tabella))
        return getColumn(ns, table, colonna);
    }
    throw new Exception("Tabella " + tabella + " inesistente.");
  }

  public Element getColumn(Namespace ns, Element table, String colonna)
     throws Exception
  {
    String nomeTable = table.getAttributeValue("name");
    Iterator iterColumn = (table.getChildren("column", ns)).iterator();
    while(iterColumn.hasNext())
    {
      Element column = (Element) (iterColumn.next());
      String nomeCol = column.getAttributeValue("name");
      if(nomeCol.equals(colonna))
        return column;
    }
    throw new Exception("Colonna " + nomeTable + "." + colonna + " inesistente.");
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
    try
    {
      int c;
      LongOptExt[] longopts = new LongOptExt[]
      {
        new LongOptExt("dtddir", LongOpt.REQUIRED_ARGUMENT, null, 'd', ""),
        new LongOptExt("schema", LongOpt.REQUIRED_ARGUMENT, null, 's', ""),
        new LongOptExt("verbose", LongOpt.NO_ARGUMENT, null, 'v', ""),
        new LongOptExt("debug", LongOpt.NO_ARGUMENT, null, 'D', ""),
      };

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

          case 's':
            xmlFile = g.getOptarg();
            break;

          case 'v':
            verbose = true;
            break;

          case 'D':
            debug = true;
            break;
        }
      }

      if((c = g.getOptind()) < args.length)
        xmlFile = args[c];

      System.out.println("Working on file " + xmlFile);
      parseXml();
      testId();
    }
    catch(Throwable ex)
    {
      Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
    }
  }
}
