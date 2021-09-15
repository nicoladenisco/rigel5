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
package org.rigel5.forms.swing;

import javax.swing.*;
import javax.swing.text.*;

/**
 * Campo di testo avanzato.
 * 
 * @author Nicola De Nisco
 */
public class AdvancedTextField extends JTextField
{
  private boolean readOnly = false;
  private boolean upperCase = false;
  private boolean lowerCase = false;
  private boolean modified = false;
  private boolean numberOnly;

  public AdvancedTextField()
  {
  }

  public boolean isReadOnly()
  {
    return readOnly;
  }

  public void setReadOnly(boolean newReadOnly)
  {
    readOnly = newReadOnly;
  }

  public void setUpperCase(boolean newUpperCase)
  {
    upperCase = newUpperCase;
  }

  public boolean isUpperCase()
  {
    return upperCase;
  }

  public void setLowerCase(boolean newLowerCase)
  {
    lowerCase = newLowerCase;
  }

  public boolean isLowerCase()
  {
    return lowerCase;
  }

  public void setModified(boolean newModified)
  {
    modified = newModified;
  }

  public boolean isModified()
  {
    return modified;
  }

  public void setNumberOnly(boolean newNumberOnly)
  {
    numberOnly = newNumberOnly;
  }

  public boolean isNumberOnly()
  {
    return numberOnly;
  }

  @Override
  protected Document createDefaultModel()
  {
    return new AdvancedDocument();
  }

  class AdvancedDocument extends PlainDocument
  {
    @Override
    public void insertString(int offs, String str, AttributeSet a)
       throws BadLocationException
    {
      if(str == null || readOnly)
        return;

      if(numberOnly)
      {
        int j = 0, c;
        char chin[] = str.toCharArray();
        char chout[] = new char[str.length()];
        for(int i = 0; i < chin.length; i++)
        {
          c = chin[i];
          if(Character.isDigit((char) c)
             || c == '+' || c == '-' || c == 'e')
            chout[j++] = (char) c;
        }
        super.insertString(offs, new String(chout, 0, j), a);
      }
      else if(upperCase)
      {
        char upper[] = str.toCharArray();
        for(int i = 0; i < upper.length; i++)
        {
          upper[i] = Character.toUpperCase(upper[i]);
        }
        super.insertString(offs, new String(upper), a);
      }
      else if(lowerCase)
      {
        char lower[] = str.toCharArray();
        for(int i = 0; i < lower.length; i++)
        {
          lower[i] = Character.toLowerCase(lower[i]);
        }
        super.insertString(offs, new String(lower), a);
      }
      else
      {
        super.insertString(offs, str, a);
      }

      modified = true;
    }
  }

  @Override
  public void setText(String t)
  {
    super.setText(t);
    modified = false;
  }
}
