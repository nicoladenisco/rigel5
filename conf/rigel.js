/*
 * Funzioni javascipt per libreria Rigel.
 */

var rigel = {

  pulisciRicercaSemplice(formname)
  {
    // pulisce i campi di ricerca semplice
    var inputs = $("#" + formname + " :input");
    inputs.each(function () {
      if ($(this).prop("type") != "button")
        $(this).val("");
    });

    // imposta filtro a FILTRO_ANNULLA ovvero cancella filtro
    $("#" + formname + " :input[name=filtro]").val("3");
    $("#" + formname).submit();
  }
  ,
  simpleSort(formname, idx)
  {
    var field = $("#" + formname + " :input[name=SSORT]");
    var val = field.val();
    if (idx == Math.abs(val))
    {
      val = -val;
    } else
    {
      val = idx;
    }
    field.val(val);

    // imposta filtro a FILTRO_APPLICA ovvero rigenera filtro
    $("#" + formname + " :input[name=filtro]").val("2");
    $("#" + formname).submit();
  }
  ,
  testInvio(formname, e)
  {
    if (e == null)
      e = event;
    if (e.keyCode == 13) {
      $("#" + formname).submit();
      return false;
    }
    return true;
  }
  ,
  testInvio(baseUri, numPerPage, numPagine, formname, e)
  {
    if (e == null)
      e = event;
    if (e.keyCode == 13) {
      this.goto(baseUri, numPerPage, numPagine, formname)
      return false;
    }
    return true;
  }
  ,
  goto(baseUri, numPerPage, numPagine, formname)
  {
    var nPage = $("#id_in_" + formname).val();
    if (nPage <= 0 || nPage > numPagine) {
      alert("Valore di pagina non consentito.");
    } else {
      rStart = (nPage - 1) * numPerPage;
      window.location.href = baseUri + "?rstart=" + rStart;
    }
    return false;
  }
  ,
  apriEditTool(url, tipo)
  {
    apriFinestraEdit(url, tipo);
  }
  ,
  submitTool(unique, url)
  {
    var formName = "fo_" + unique;
    var bodyName = "body_" + unique;

    $("#search_" + unique).html("");

    jQuery.ajax({
      type: "POST",
      url: url,
      data: $("#" + formName).serialize(), // serializes the form's elements.
      success: function (data)
      {
        $("#" + bodyName).html(data);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  ricercaTool(unique, url)
  {
    var formName = "fo_" + unique;
    var bodyName = "body_" + unique;

    $("#data_" + unique).html("");

    jQuery.ajax({
      type: "POST",
      url: url,
      data: $("#" + formName).serialize(), // serializes the form's elements.
      success: function (data)
      {
        $("#" + bodyName).html(data);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  testInvioTool(baseUri, numPerPage, numPagine, unique, e)
  {
    if (e == null)
      e = event;
    if (e.keyCode == 13) {
      this.gotoForTool(baseUri, numPerPage, numPagine, unique)
      return false;
    }
    return true;
  }
  ,
  gotoForTool(baseUri, numPerPage, numPagine, unique)
  {
    var nPage = $("#id_in_" + unique).val();
    if (nPage <= 0 || nPage > numPagine) {
      alert("Valore di pagina non consentito.");
    } else {
      rStart = (nPage - 1) * numPerPage;
      this.jumpTool(unique, baseUri + "?rstart=" + rStart);
    }
    return false;
  }
  ,
  jumpTool(unique, url)
  {
    var bodyName = "body_" + unique;

    jQuery.ajax({
      type: "GET",
      url: url,
      success: function (data)
      {
        $("#" + bodyName).html(data);
      }
    });
  }
  ,
  showRicTool(unique)
  {
    $("#data_" + unique).hide();
    $("#search_" + unique).show();
  }
  ,
  hideRicTool(unique)
  {
    $("#search_" + unique).hide();
    $("#data_" + unique).show();
  }
  ,
  pulisciRicercaTool(unique, url)
  {
    this.jumpTool(unique, url + "?filtro=3");
    return false;
  }
  ,
  submitDirectLista(type, url)
  {
    jQuery.ajax({
      type: "POST",
      url: url,
      data: $("#fo" + type).serialize(), // serializes the form's elements.
      success: function (data)
      {
        $("#rigel_dialog_body").html(data);

        const re = /<!-- header: (.+) -->/;
        const ok = re.exec(data);
        if (ok)
          setTopDialogTitle(ok[1]);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  jumpDirectLista(url)
  {
    jQuery.ajax({
      type: "GET",
      url: url,
      success: function (data)
      {
        $("#rigel_dialog_body").html(data);
      }
    });
  }
  ,
  submitDirectForm(type, url)
  {
    jQuery.ajax({
      type: "POST",
      url: url,
      data: $("#fo" + type).serialize(), // serializes the form's elements.
      success: function (data)
      {
        $("#rigel_dialog_body").html(data);

        const re = /<!-- header: (.+) -->/;
        const ok = re.exec(data);
        if (ok)
          setTopDialogTitle(ok[1]);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  calarray: []
  ,
  apriCal(nomeform, nomecampo)
  {
    this.calarray.push({nomeform: nomeform, campo: nomecampo, campo1: null, campo2: null});
    apriCalendarioNoscript(nomeform, "rigel.impostaData");
  }
  ,
  apriIntervallo1(nomeform, nomecampo1, nomecampo2)
  {
    this.calarray.push({nomeform: nomeform, campo: nomecampo1, campo1: nomecampo1, campo2: nomecampo2});
    apriCalendarioIntervalloNoscript(nomeform, "rigel.impostaData", "rigel.impostaIntervallo");
  }
  ,
  apriIntervallo2(nomeform, nomecampo1, nomecampo2)
  {
    this.calarray.push({nomeform: nomeform, campo: nomecampo2, campo1: nomecampo1, campo2: nomecampo2});
    apriCalendarioIntervalloNoscript(nomeform, "rigel.impostaData", "rigel.impostaIntervallo");
  }
  ,
  apriCalRic(nomeform, nomecampo)
  {
    this.calarray.push({nomeform: nomeform, campo: "VL" + nomecampo, campo1: null, campo2: null, ricercaSemplice: true});
    apriCalendarioNoscript(nomeform, "rigel.impostaData");
  }
  ,
  apriCalIntR1(nomeform, nomecampo)
  {
    this.calarray.push({
      nomeform: nomeform,
      campo: "VL" + nomecampo,
      campo1: "VL" + nomecampo,
      campo2: "VF" + nomecampo,
      ricerca: true,
      nomecampo: nomecampo,
      valorefiltro: 2
    });
    apriCalendarioIntervalloNoscript(nomeform, "rigel.impostaData", "rigel.impostaIntervallo");
  }
  ,
  apriCalIntR2(nomeform, nomecampo)
  {
    this.calarray.push({
      nomeform: nomeform,
      campo: "VF" + nomecampo,
      campo1: "VL" + nomecampo,
      campo2: "VF" + nomecampo,
      ricerca: true,
      nomecampo: nomecampo,
      valorefiltro: 8
    });
    apriCalendarioIntervalloNoscript(nomeform, "rigel.impostaData", "rigel.impostaIntervallo");
  }
  ,
  impostaData(valore)
  {
    var dati = this.calarray.pop();
    var field = $("#" + dati.nomeform + " :input[name=" + dati.campo + "]");
    field.val(valore);

    if (dati.ricercaSemplice !== undefined) {
      $("#" + dati.nomeform).submit();
    }

    if (dati.ricerca !== undefined) {
      var field3 = $("#" + dati.nomeform + " :input[name=OP" + dati.nomecampo + "]");
      field3.val(dati.valorefiltro);
    }
  }
  ,
  impostaIntervallo(valore)
  {
    var dati = this.calarray.pop();
    var vvvv = valore.split("|");
    var valore1 = vvvv[0];
    var valore2 = vvvv[1];

    var field1 = $("#" + dati.nomeform + " :input[name=" + dati.campo1 + "]");
    field1.val(valore1);
    var field2 = $("#" + dati.nomeform + " :input[name=" + dati.campo2 + "]");
    field2.val(valore2);

    if (dati.ricercaSemplice !== undefined) {
      $("#" + dati.nomeform).submit();
    }

    if (dati.ricerca !== undefined) {
      var field3 = $("#" + dati.nomeform + " :input[name=OP" + dati.nomecampo + "]");
      field3.val(8);
    }
  }
};
