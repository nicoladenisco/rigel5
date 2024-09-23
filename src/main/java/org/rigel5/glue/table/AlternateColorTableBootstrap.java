/*
 *  Copyright (C) 2015 Informatica Medica s.r.l.
 *
 *  Questo software è proprietà di Informatica Medica s.r.l.
 *  Tutti gli usi non esplicitimante autorizzati sono da
 *  considerarsi tutelati ai sensi di legge.
 *
 *  Informatica Medica s.r.l.
 *  Viale dei Tigli, 19
 *  Casalnuovo di Napoli (NA)
 *
 *  Creato il 13-feb-2015, 17.57.26
 */
package org.rigel5.glue.table;

/**
 * Tabella rigel a colori alternati.
 * Versione personalizzata per interfaccia pcsdm (bootstrap).
 *
 * @author Nicola De Nisco
 */
public class AlternateColorTableBootstrap extends AlternateColorTableAppBase
{
  /**
   * La versione pcsdm non introduce classi di riga.
   * @param row
   * @return
   */
  @Override
  public String doRowStatement(int row)
  {
    // ritorna solo TR (senza agginugere altre classi)
    return "TR";
  }

  /**
   * Produce l'header della tabella
   * @throws java.lang.Exception
   */
  @Override
  public void doHeader()
     throws Exception
  {
    html.append("<THEAD>\r\n<TR>\r\n").append(preHeader());

    for(int i = 0; i < tableModel.getColumnCount(); i++)
    {
      html.append(doCellHeader(i));
    }

    html.append(postHeader()).append("</TR>\r\n</THEAD>\r\n");
  }

  @Override
  protected String cellBegin(int row, int col)
     throws Exception
  {
    String align = doAlign(row, col);
    String color = doColor(row, col);
    String style = doStyle(row, col);

    if(row == -1)
    {
      // header della tabella
      if(nosize)
        return "<TH "
           + align + " " + color + " " + style + ">";
      else
        return "<TH WIDTH=\"" + normWidth[col] + "%\""
           + align + " " + color + " " + style + ">";
    }

    // corpo della tabella
    if(nosize)
      return "<" + colStatement + " "
         + align + " " + color + " " + style + ">";
    else
      return "<" + colStatement + " WIDTH=\"" + normWidth[col] + "%\""
         + align + " " + color + " " + style + ">";
  }

  @Override
  protected String cellEnd(int row, int col)
     throws Exception
  {
    if(row == -1)
      return "</TH>\r\n";
    else
      return "</TD>\r\n";
  }

  @Override
  public synchronized void doRows(int rStart, int numRec)
     throws Exception
  {
    html.append("<TBODY>\n");
    super.doRows(rStart, numRec);
    html.append("</TBODY>\n");
  }
}
