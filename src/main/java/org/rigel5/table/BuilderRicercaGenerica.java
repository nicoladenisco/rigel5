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
package org.rigel5.table;

import org.apache.torque.criteria.SqlEnum;
import org.rigel5.RigelI18nInterface;
import org.rigel5.db.torque.CriteriaRigel;

/**
 * Interfaccia di un generatore di filtro astratto.
 * Le classi che implementano questa interfaccia sono
 * utilizzate per costruire una filtro da utilizzare
 * nelle liste e vengono impiegate nelle maschere
 * che implementano MascheraRicercaGenerica per effettuare
 * il parsing della maschera e la realizzazione del
 * relativo filtro.
 * vedi classi derivate
 * @see MascheraRicercaGenerica
 */
public interface BuilderRicercaGenerica
{
  /* TIPI DI FILTRAGGIO: vedi dichiarazione di getTipiConfronto()
  in SqlBuilderRicercaGenerica e/o PeerBuilderRicercaGenerica */
  public static final int IDX_CRITERIA_ALL = 0;
  public static final int IDX_CRITERIA_LIKE = 1;
  public static final int IDX_CRITERIA_EQUAL = 2;
  public static final int IDX_CRITERIA_LESS_THAN = 3;
  public static final int IDX_CRITERIA_GREATER_THAN = 4;
  public static final int IDX_CRITERIA_NOT_EQUAL = 5;
  public static final int IDX_CRITERIA_LESS_EQUAL = 6;
  public static final int IDX_CRITERIA_GREATER_EQUAL = 7;
  public static final int IDX_CRITERIA_BETWEEN = 8;
  public static final int IDX_CRITERIA_ISNULL = 9;
  public static final int IDX_CRITERIA_ISNOTNULL = 10;

  public static final String[] comboItems =
  {
    "IGNORATO",
    "contiene",
    "uguale (=)",
    "minore (<)",
    "maggiore (>)",
    "diverso (<>)",
    "minore o uguale (<=)",
    "maggiore o uguale (>=)",
    "compreso",
    "nullo",
    "non nullo"
  };

  public static final SqlEnum[] criteriaItems =
  {
    CriteriaRigel.ALL,
    CriteriaRigel.LIKE,
    CriteriaRigel.EQUAL,
    CriteriaRigel.LESS_THAN,
    CriteriaRigel.GREATER_THAN,
    CriteriaRigel.NOT_EQUAL,
    CriteriaRigel.LESS_EQUAL,
    CriteriaRigel.GREATER_EQUAL,
    CriteriaRigel.BETWEEN,
    CriteriaRigel.ISNULL,
    CriteriaRigel.ISNOTNULL
  };

  /**
   * Restituisce i tipi di contronto supportati.
   * @return
   */
  public String[] getTipiConfronto();

  /**
   * Restituisce i tipi di criteria supportati.
   * @see SqlEnum.
   * @return
   */
  public SqlEnum[] getTipiCriteria();

  /**
   * Costruisce il filtro.
   * @return
   * @throws Exception
   */
  public Object buildCriteria()
     throws Exception;

  /**
   * Ritorna vero se il tipo di contronto indicato
   * e' del tipo between (ovvero richiede un inizio e una fine).
   * @param idx
   * @return
   */
  public boolean isBetween(int idx);

  /**
   * Ritorna l'HTML per una colonna con la selezione a combo box.
   * @param formName the value of formName
   * @param fieldName the value of fieldName
   * @param cd
   * @param defVal valore di default
   * @throws Exception
   * @return the java.lang.String
   */
  public String getHtmlComboColonnaMaschera(String formName, String fieldName,
     RigelColumnDescriptor cd, String defVal, RigelI18nInterface i18n)
     throws Exception;

  /**
   * Ritorna l'HTML per una colonna con la selezione a combo box.
   * @param formName the value of formName
   * @param fieldName the value of fieldName
   * @param cd
   * @param defVal valore di default
   * @throws Exception
   * @return the java.lang.String
   */
  public String getHtmlComboColonnaRicSemplice(String formName, String fieldName,
     RigelColumnDescriptor cd, String defVal, RigelI18nInterface i18n)
     throws Exception;
}


