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
package org.rigel2.table.peer;

import org.commonlib.utils.ClassOper;

/**
 * Gestore degli oggetti di Torque (Peer, oggetti e TableMap).
 * Viene utilizzato in PeerWrapperXmlMaker e PeerWrapperTmapMaker.
 * 
 * @author Nicola De Nisco
 */
public class TorqueObjectManager
{
  protected String[] basePeer = null;
  protected String[] baseObject = null;

  public String getBaseObject()
  {
    if(baseObject == null || baseObject.length == 0)
      return null;

    String rv = "";
    for(int j = 0; j < baseObject.length; j++)
    {
      rv += ";" + baseObject[j];
    }
    return rv.substring(1);
  }

  public void setBaseObject(String bo)
  {
    if(bo == null)
    {
      baseObject = null;
      return;
    }
    baseObject = bo.split(";|:");
    for(int j = 0; j < baseObject.length; j++)
    {
      if(baseObject[j] != null && baseObject[j].length() > 0 && !baseObject[j].endsWith("."))
        baseObject[j] += ".";
    }
  }

  public String getBasePeer()
  {
    if(basePeer == null || basePeer.length == 0)
      return null;

    String rv = "";
    for(int j = 0; j < basePeer.length; j++)
    {
      rv += ";" + basePeer[j];
    }
    return rv.substring(1);
  }

  public void setBasePeer(String bp)
  {
    if(bp == null)
    {
      basePeer = null;
      return;
    }
    basePeer = bp.split(";|:|,");
    for(int j = 0; j < basePeer.length; j++)
    {
      if(basePeer[j] != null && basePeer[j].length() > 0 && !basePeer[j].endsWith("."))
        basePeer[j] += ".";
    }
  }

  public String[] getBaseObjectArray()
  {
    if(baseObject == null || baseObject.length == 0)
      return null;

    return baseObject;
  }

  public void setBaseObjectArray(String[] bo)
  {
    if(bo == null)
    {
      baseObject = null;
      return;
    }
    baseObject = bo;
  }

  public String[] getBasePeerArray()
  {
    if(basePeer == null || basePeer.length == 0)
      return null;

    return basePeer;
  }

  public void setBasePeerArray(String[] bp)
  {
    if(bp == null)
    {
      basePeer = null;
      return;
    }
    basePeer = bp;
  }

  /**
   * Ritorna una istanza dell'oggetto xxxx per la classe richiesta.
   * @param className
   * @return
   */
  public Class buildObject(String className)
  {
    return ClassOper.loadClass(className, baseObject);
  }

  /**
   * Ritorna una istanza dell'oggetto xxxxPeer per la classe richiesta.
   * @param className
   * @return
   */
  public Class buildPeer(String className)
  {
    return ClassOper.loadClass(className, basePeer);
  }
}
