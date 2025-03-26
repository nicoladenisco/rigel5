/*
 * Funzioni javascipt per libreria Rigel.
 */

var rigel = {

  pulisciRicercaSemplice(formname) {
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
  simpleSort(formname, idx) {
    var field = $("#" + formname + " :input[name=SSORT]");
    var val = field.val();
    if (idx == Math.abs(val)) {
      val = -val;
    }
    else {
      val = idx;
    }
    field.val(val);

    // imposta filtro a FILTRO_APPLICA ovvero rigenera filtro
    $("#" + formname + " :input[name=filtro]").val("2");
    $("#" + formname).submit();
  }
  ,
  testInvio(formname, e) {
    if (e == null)
      e = event;
    if (e.keyCode == 13) {
      $("#" + formname).submit();
      return false;
    }
    return true;
  }
  ,
  testInvioNav(baseUri, numPerPage, numPagine, formname, e) {
    if (e == null)
      e = event;
    if (e.keyCode == 13) {
      this.goto(baseUri, numPerPage, numPagine, formname)
      return false;
    }
    return true;
  }
  ,
  jumpNav(url) {
    goPage(url);
  }
  ,
  gotoPage(baseUri, numPerPage, numPagine) {
    var nPage = $("#id_in_page_number").val();
    if (nPage <= 0 || nPage > numPagine) {
      alert("Valore di pagina non consentito.");
    }
    else {
      rStart = (nPage - 1) * numPerPage;
      this.jumpNav(baseUri + "?rstart=" + rStart);
    }
    return false;
  }
  ,
  gotoNav(baseUri, numPerPage, numPagine, formname) {
    var nPage = $("#id_in_" + formname).val();
    if (nPage <= 0 || nPage > numPagine) {
      alert("Valore di pagina non consentito.");
    }
    else {
      rStart = (nPage - 1) * numPerPage;
      this.jumpNav(baseUri + "?rstart=" + rStart);
    }
    return false;
  }
  ,
  apriEditTool(url, tipo) {
    apriFinestraEdit(url, tipo);
  }
  ,
  submitTool(unique, url) {
    var formName = "fo_" + unique;
    var bodyName = "body_" + unique;

    $("#search_" + unique).html("");

    jQuery.ajax({
      type: "POST",
      url: url,
      data: $("#" + formName).serialize(), // serializes the form's elements.
      success: function (data) {
        $("#" + bodyName).html(data);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  ricercaTool(unique, url) {
    var formName = "fo_" + unique;
    var bodyName = "body_" + unique;

    $("#data_" + unique).html("");

    jQuery.ajax({
      type: "POST",
      url: url,
      data: $("#" + formName).serialize(), // serializes the form's elements.
      success: function (data) {
        $("#" + bodyName).html(data);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  testInvioTool(baseUri, numPerPage, numPagine, unique, e) {
    if (e == null)
      e = event;
    if (e.keyCode == 13) {
      this.gotoForTool(baseUri, numPerPage, numPagine, unique)
      return false;
    }
    return true;
  }
  ,
  gotoForTool(baseUri, numPerPage, numPagine, unique) {
    var nPage = $("#id_in_" + unique).val();
    if (nPage <= 0 || nPage > numPagine) {
      alert("Valore di pagina non consentito.");
    }
    else {
      rStart = (nPage - 1) * numPerPage;
      this.jumpTool(unique, baseUri + "?rstart=" + rStart);
    }
    return false;
  }
  ,
  jumpTool(unique, url) {
    var bodyName = "body_" + unique;

    jQuery.ajax({
      type: "GET",
      url: url,
      success: function (data) {
        $("#" + bodyName).html(data);
      }
    });
  }
  ,
  showRicTool(unique) {
    $("#data_" + unique).hide();
    $("#search_" + unique).show();
  }
  ,
  hideRicTool(unique) {
    $("#search_" + unique).hide();
    $("#data_" + unique).show();
  }
  ,
  pulisciRicercaTool(unique, url) {
    this.jumpTool(unique, url + "?filtro=3");
    return false;
  }
  ,
  submitDirectLista(type, url) {
    jQuery.ajax({
      type: "POST",
      url: url,
      data: $("#fo" + type).serialize(), // serializes the form's elements.
      success: function (data) {
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
  jumpDirectLista(url) {
    jQuery.ajax({
      type: "GET",
      url: url,
      success: function (data) {
        $("#rigel_dialog_body").html(data);
      }
    });
  }
  ,
  submitDirectForm(type, url) {
    jQuery.ajax({
      type: "POST",
      url: url,
      data: $("#fo" + type).serialize(), // serializes the form's elements.
      success: function (data) {
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
  apriCal(nomeform, nomecampo) {
    this.calarray.push({nomeform: nomeform, campo: nomecampo, campo1: null, campo2: null});
    apriCalendarioNoscript(nomeform, "rigel.impostaData");
  }
  ,
  apriIntervallo1(nomeform, nomecampo1, nomecampo2) {
    this.calarray.push({nomeform: nomeform, campo: nomecampo1, campo1: nomecampo1, campo2: nomecampo2});
    apriCalendarioIntervalloNoscript(nomeform, "rigel.impostaData", "rigel.impostaIntervallo");
  }
  ,
  apriIntervallo2(nomeform, nomecampo1, nomecampo2) {
    this.calarray.push({nomeform: nomeform, campo: nomecampo2, campo1: nomecampo1, campo2: nomecampo2});
    apriCalendarioIntervalloNoscript(nomeform, "rigel.impostaData", "rigel.impostaIntervallo");
  }
  ,
  apriCalRic(nomeform, nomecampo) {
    this.calarray.push({nomeform: nomeform, campo: "VL" + nomecampo, campo1: null, campo2: null, ricercaSemplice: true});
    apriCalendarioNoscript(nomeform, "rigel.impostaData");
  }
  ,
  apriCalIntR1(nomeform, nomecampo) {
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
  apriCalIntR2(nomeform, nomecampo) {
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
  impostaData(valore) {
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
  impostaIntervallo(valore) {
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
  ,
  /**
   * Esegue una chiamata ad action Turbine attraverso action.jsp (vedi sirio).
   * @param {type} uri per raggiungere action.jsp
   * @param {type} dati dati da inviare alla action
   * @param {type} fnExecute funzione da eseguire con i la risposta
   * @param {type} fnReload se attivo flag reload ricarica la pagina
   * @returns {undefined}
   */
  runActionJson(uri, dati, fnExecute, fnReload) {
    jQuery.getJSON(uri, dati, function (data) {
      if (typeof data["ERROR"] !== "undefined" && data["ERROR"] !== "") {
        bdError(data["ERROR"]);
        return;
      }

      if (typeof data["message"] !== "undefined" && data["message"] !== "") {
        bdAlert(data["message"]);
      }

      if (data["reload"] === "1") {
        if (fnReload !== "undefined")
          fnReload();
        return;
      }

      if (fnExecute !== "undefined")
        fnExecute(data);

    }).fail(function (jqxhr, textStatus, error) {
      var err = textStatus + ", " + error;
      console.log("Request Failed in runActionJson: " + err);
    });
  }
  ,
  /**
   * Esegue una richiesta json in modalità sincrona.
   * A differenza di jQuery.getJSON() questa è sincrona
   * ovvero aspetta la risposta ed esegue la callback prima di ritornare.
   * @param {type} uri
   * @param {type} callbackDaChiamare
   * @returns {undefined}
   */
  syncJSON(uri, callbackDaChiamare) {
    jQuery.ajax({
      dataType: "json",
      url: uri,
      async: false,
      success: function (data) {
        callbackDaChiamare(data);
      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in syncJSON: " + err);
      }
    });
  }
};
