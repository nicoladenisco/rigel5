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
package org.rigel5.test;

import org.commonlib5.utils.ArrayMap;
import org.junit.*;
import org.rigel5.HtmlUtils;

/**
 * Test componenti di base.
 *
 * @author Nicola De Nisco
 */
public class StaticTest
{
  @Test
  public void testHtmlUtils()
  {
    String url = "http://localhost:8080/app/index.html?primopar=0";
    Assert.assertEquals(url + "&pippo=pluto", HtmlUtils.mergeUrl(url, "pippo", "pluto"));
    Assert.assertEquals(url + "&pippo=1", HtmlUtils.mergeUrl(url, "pippo", 1));
    Assert.assertEquals(url + "&pippo=1", HtmlUtils.mergeUrl(url, "pippo", 1L));
    Assert.assertEquals(url + "&pippo=1.0", HtmlUtils.mergeUrl(url, "pippo", 1.0));
    Assert.assertEquals(url + "&pippo=true", HtmlUtils.mergeUrl(url, "pippo", true));

    ArrayMap<String, String> params1 = new ArrayMap<>();
    params1.put("pippo", "pluto");
    params1.put("topolino", "paperino");
    Assert.assertEquals(url + "&pippo=pluto&topolino=paperino", HtmlUtils.mergeUrl(url, params1));
    Assert.assertEquals(url + "&pippo=pluto&topolino=paperino", HtmlUtils.mergeUrlTestUnique(url, params1));

    Assert.assertEquals(url + "&pippo=pluto&topolino=paperino&topolino=paperino",
       HtmlUtils.mergeUrlPair(url,
          "pippo", "pluto",
          "topolino", "paperino",
          "topolino", "paperino"
       ));

    Assert.assertEquals(url + "&pippo=pluto&topolino=paperino",
       HtmlUtils.mergeUrlPairTestUnique(url,
          "pippo", "pluto",
          "topolino", "paperino",
          "topolino", "paperino"
       ));
  }
}
