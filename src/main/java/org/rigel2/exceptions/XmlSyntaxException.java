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
package org.rigel2.exceptions;

import org.jdom2.*;

/**
 * Errore di sintassi nel parsing di XML.
 *
 * @author Nicola De Nisco
 */
public class XmlSyntaxException extends Exception
{
  private Element e = null;
  private String nomeLista = null;

  public XmlSyntaxException(String msg)
  {
    super(msg);
  }

  public XmlSyntaxException(String nomeLista, String msg)
  {
    super(msg);
    this.nomeLista = nomeLista;
  }

  public XmlSyntaxException(Element e, String msg)
  {
    super(msg);
    this.e = e;
  }

  public XmlSyntaxException(Element e, String nomeLista, String msg)
  {
    super(msg);
    this.e = e;
    this.nomeLista = nomeLista;
  }

  @Override
  public String getMessage()
  {
    String rv = super.getMessage();

    if(e == null && nomeLista == null)
      return rv;

    if(e != null)
      rv = "Element "+e.getName()+": "+rv;

    if(nomeLista != null)
      rv = "["+nomeLista+"] "+rv;

    return rv;
  }
}


