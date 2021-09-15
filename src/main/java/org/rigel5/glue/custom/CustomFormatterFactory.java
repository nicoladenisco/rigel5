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
package org.rigel5.glue.custom;

import java.lang.reflect.Constructor;
import java.text.Format;
import org.apache.commons.lang.ArrayUtils;
import org.commonlib5.utils.ClassOper;
import org.jdom2.Element;

/**
 * Factory (istanziatore) degli oggetti custom edit
 * utilizzati dalle liste di edit (lista.xml).
 *
 * @author Nicola De Nisco
 */
public class CustomFormatterFactory
{
  private String myPackage = null;
  private String[] classRadixs = null;
  /** l'unica instanza */
  private static CustomFormatterFactory vf = new CustomFormatterFactory();

  private CustomFormatterFactory()
  {
    myPackage = ClassOper.getClassPackage(this.getClass());
  }

  public static CustomFormatterFactory getInstance()
  {
    return vf;
  }

  public String[] getClassRadixs()
  {
    return classRadixs;
  }

  public void setClassRadixs(String[] classRadixs)
  {
    this.classRadixs = classRadixs;
  }

  public void addClassRadix(String basePath)
  {
    classRadixs = (String[]) ArrayUtils.add(classRadixs, basePath);
  }

  public Format getFormatter(Element xml)
     throws Exception
  {
    String fmtClass = xml.getTextTrim();

    if(fmtClass == null || fmtClass.isEmpty())
      fmtClass = xml.getChildText("class");
    if(fmtClass == null || fmtClass.isEmpty())
      fmtClass = xml.getChildText("classname");

    if(fmtClass == null || fmtClass.isEmpty())
      return null;

    return getFormatter(fmtClass, xml);
  }

  public Format getFormatter(String clname, Element xml)
     throws Exception
  {
    try
    {
      Class cl;
      Format rv;

      // cerca prima il classname intero (potrebbe essere assoluto)
      if((cl = ClassOper.loadClass(clname)) != null)
        if((rv = buildFormatter(cl, xml)) != null)
          return rv;

      // recupera il solo nome della classe
      int pos = clname.lastIndexOf('.');
      if(pos != -1)
        clname = clname.substring(pos + 1);

      // riprova cercando nei package registrati
      if((cl = ClassOper.loadClass(clname, myPackage, classRadixs)) != null)
        if((rv = buildFormatter(cl, xml)) != null)
          return rv;
    }
    catch(ClassCastException ex)
    {
      throw new ClassCastException("La classe " + clname + " non implementa Format.");
    }
    catch(Exception ex)
    {
    }

    throw new Exception("Classe custom edit " + clname + " non trovata o non valida.");
  }

  protected Format buildFormatter(Class cls, Element xml)
  {
    // tenta prima di identificare un costruttore che accetti un riferimento all'XML
    try
    {
      Constructor c = cls.getConstructor(Element.class);
      return (Format) c.newInstance(xml);
    }
    catch(Throwable t)
    {
    }

    // riprova con un costruttore vuoto
    try
    {
      return (Format) cls.newInstance();
    }
    catch(Throwable t)
    {
    }

    return null;
  }
}
