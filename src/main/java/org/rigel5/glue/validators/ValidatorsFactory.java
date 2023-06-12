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
package org.rigel5.glue.validators;

import org.apache.commons.lang3.ArrayUtils;
import org.commonlib5.utils.ClassOper;

/**
 * Factory (istanziatore) degli oggetti validator
 * utilizzati dalle liste di edit (lista.xml).
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class ValidatorsFactory
{
  private String myPackage = null;
  private String[] classRadixs = null;
  /** l'unica instanza */
  private static ValidatorsFactory vf = new ValidatorsFactory();

  private ValidatorsFactory()
  {
    myPackage = ClassOper.getClassPackage(this.getClass());
  }

  public static ValidatorsFactory getInstance()
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

  public PreParseValidator getPreParseValidator(String clname)
     throws Exception
  {
    try
    {
      Class cl = ClassOper.loadClass(clname, myPackage, classRadixs);
      if(cl != null)
        return (PreParseValidator) (cl.newInstance());
    }
    catch(ClassCastException ex)
    {
      throw new ClassCastException("La classe " + clname + " non implementa PreParseValidator");
    }
    catch(Exception ex)
    {
    }

    throw new Exception("Classe validator " + clname + " non trovata o non valida.");
  }

  public PostParseValidator getPostParseValidator(String clname)
     throws Exception
  {
    try
    {
      Class cl = ClassOper.loadClass(clname, myPackage, classRadixs);
      if(cl != null)
        return (PostParseValidator) (cl.newInstance());
    }
    catch(ClassCastException ex)
    {
      throw new ClassCastException("La classe " + clname + " non implementa PostParseValidator");
    }
    catch(Exception ex)
    {
    }

    throw new Exception("Classe validator " + clname + " non trovata o non valida.");
  }

  public PostSaveAction getPostSaveAction(String clname)
     throws Exception
  {
    try
    {
      Class cl = ClassOper.loadClass(clname, myPackage, classRadixs);
      if(cl != null)
        return (PostSaveAction) (cl.newInstance());
    }
    catch(ClassCastException ex)
    {
      throw new ClassCastException("La classe " + clname + " non implementa PostSaveAction");
    }
    catch(Exception ex)
    {
    }

    throw new Exception("Classe validator " + clname + " non trovata o non valida.");
  }

  public SaveMasterDetailValidator getSaveMasterDetailValidator(String clname)
     throws Exception
  {
    try
    {
      Class cl = ClassOper.loadClass(clname, myPackage, classRadixs);
      if(cl != null)
        return (SaveMasterDetailValidator) (cl.newInstance());
    }
    catch(ClassCastException ex)
    {
      throw new ClassCastException("La classe " + clname + " non implementa SaveMasterDetailValidator");
    }
    catch(Exception ex)
    {
    }

    throw new Exception("Classe validator " + clname + " non trovata o non valida.");
  }
}
