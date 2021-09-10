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
package org.rigel2.glue.custom;

import org.apache.commons.lang.ArrayUtils;
import org.commonlib.utils.ClassOper;
import org.rigel2.table.CustomColumnEdit;

/**
 * Factory (istanziatore) degli oggetti custom edit
 * utilizzati dalle liste di edit (lista.xml).
 *
 * @author Nicola De Nisco
 */
public class CustomEditFactory
{
  private String myPackage = null;
  private String[] classRadixs = null;
  /** l'unica instanza */
  private static CustomEditFactory vf = new CustomEditFactory();

  private CustomEditFactory()
  {
    myPackage = ClassOper.getClassPackage(this.getClass());
  }

  public static CustomEditFactory getInstance()
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

  public CustomColumnEdit getCustomColumnEdit(String clname)
     throws Exception
  {
    try
    {
      Class cl;
      CustomColumnEdit rv;

      // cerca prima il classname intero (potrebbe essere assoluto)
      if((cl = ClassOper.loadClass(clname)) != null)
        if((rv = buildColumnEdit(cl)) != null)
          return rv;

      // recupera il solo nome della classe
      int pos = clname.lastIndexOf('.');
      if(pos != -1)
        clname = clname.substring(pos + 1);

      if((cl = ClassOper.loadClass(clname, myPackage, classRadixs)) != null)
        if((rv = buildColumnEdit(cl)) != null)
          return rv;
    }
    catch(ClassCastException ex)
    {
      throw new ClassCastException("La classe " + clname + " non implementa CustomColumnEdit.");
    }
    catch(Exception ex)
    {
    }

    throw new Exception("Classe custom edit " + clname + " non trovata o non valida.");
  }

  protected CustomColumnEdit buildColumnEdit(Class cls)
  {
    try
    {
      return (CustomColumnEdit) cls.newInstance();
    }
    catch(Throwable t)
    {
      return null;
    }
  }
}
