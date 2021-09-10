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

import java.io.File;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

/**
 * Classe base dei programmi di test.
 * Effettua il parsing del file XML con il database.
 *
 * @author Nicola De Nisco
 */
public class ParseSchemaBase
{
  public int numIndent = 2;
  public Document doc = null;
  public File dtdDir = null;
  public String ouputFile = "/tmp/afr.xml";
  public String xmlFile = null;

  public ParseSchemaBase()
  {
  }

  public Document buildDocument(String fileModelliXml)
     throws Exception
  {
    SAXBuilder builder = new SAXBuilder();

    if(dtdDir != null)
    {
      builder.setEntityResolver((String publicId, String systemId) ->
      {
        if(systemId.startsWith("file:"))
        {
          File fDTD = new File(systemId.substring(5));
          if(!fDTD.exists())
          {
            String fileName = fDTD.getName();
            fDTD = new File(dtdDir, fileName);
            systemId = "file:" + fDTD.getAbsolutePath();
          }
        }

        return new InputSource(systemId);
      });
    }

    return builder.build(new File(fileModelliXml));
  }

  public void parseXml()
     throws Exception
  {
    if(xmlFile == null)
      throw new Exception("Specificare un file xml da leggere.");

    doc = buildDocument(xmlFile);
  }
}
