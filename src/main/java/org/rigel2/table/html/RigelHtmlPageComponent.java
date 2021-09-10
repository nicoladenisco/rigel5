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
package org.rigel2.table.html;

/**
 * Un componente di pagina.
 * Il componente può essere di vari tipi (vedi @see type).
 *
 * @author Nicola De Nisco
 */
public class RigelHtmlPageComponent
{
  /** il tipo di componete */
  private PageComponentType type;
  /** il gruppo di appartenenza */
  private String group = null;
  /** una eventuale sezione di appartenenza (può essere null) */
  private String section = null;
  /** il contenuto del componente */
  private StringBuilder content = new StringBuilder(512);

  /**
   * Costruttore del componente.
   * @param type tipo del componente
   * @param group un gruppo di riferimento
   */
  public RigelHtmlPageComponent(PageComponentType type, String group)
  {
    this.type = type;
    this.group = group;
  }

  public PageComponentType getType()
  {
    return type;
  }

  public void setType(PageComponentType type)
  {
    this.type = type;
  }

  public StringBuilder getContent()
  {
    return content;
  }

  public void setContent(StringBuilder content)
  {
    this.content = content;
  }

  public String getGroup()
  {
    return group;
  }

  public void setGroup(String group)
  {
    this.group = group;
  }

  public String getSection()
  {
    return section;
  }

  public void setSection(String section)
  {
    this.section = section;
  }

  public boolean isEmpty()
  {
    return content.length() == 0;
  }

  public StringBuilder append(CharSequence string)
  {
    return content.append(string);
  }

  public StringBuilder append(int val)
  {
    return content.append(val);
  }

  public StringBuilder append(long val)
  {
    return content.append(val);
  }

  public void clear()
  {
    content = new StringBuilder(512);
  }

  @Override
  public String toString()
  {
    return content.toString();
  }
}
