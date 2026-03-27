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
  jumpNavAjax(url) {
    this.runActionJson(url, null, function (data) {
      if (data.htmlBody !== undefined && data.idBody !== undefined) {
        $("#" + data.idBody).html(data.htmlBody);
      }

      if (data.htmlNav !== undefined && data.idNav !== undefined) {
        $("#" + data.idNav).html(data.htmlNav);
      }
    });
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
  moveKeyRicerca(prev, succ, e) {

    switch (getKeyCodeFromEvent(e))
    {
      case 38: // freccia su
      case 40: // freccia giu
        var cprev = $("#" + prev);
        var csucc = $("#" + succ);

        if (cprev.length > 0 && csucc.length > 0)
          return moveKey(cprev[0], csucc[0], e);
    }

    return true;
  }
  ,
  apriEditTool(url, tipo) {
    apriFinestraEdit(url, tipo);
  }
  ,
  submitTool(unique, url) {
    var formName = "fo_" + unique;
    var bodyName = "body_" + unique;
    var dati = $("#" + formName).serialize(); // serializes the form's elements.

    $("#search_" + unique).html("");

    jQuery.ajax({
      type: "POST",
      url: url,
      data: dati,
      success: function (data) {
        $("#" + bodyName).html(data);
      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in submitTool: " + err);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  submitTool2(uniqueBody, uniqueForm, url) {
    var formName = "fo_" + uniqueForm;
    var bodyName = "body_" + uniqueBody;
    var dati = $("#" + formName).serialize(); // serializes the form's elements.

    $("#search_" + uniqueBody).html("");

    jQuery.ajax({
      type: "POST",
      url: url,
      data: dati,
      success: function (data) {
        $("#" + bodyName).html(data);
      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in submitTool2: " + err);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  ricercaTool(unique, url) {
    var formName = "fo_" + unique;
    var bodyName = "body_" + unique;
    var dati = $("#" + formName).serialize(); // serializes the form's elements.

    $("#data_" + unique).html("");

    jQuery.ajax({
      type: "POST",
      url: url,
      data: dati,
      success: function (data) {
        $("#" + bodyName).html(data);
      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in ricercaTool: " + err);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  ricercaTool2(uniqueBody, uniqueForm, url) {
    var formName = "fo_" + uniqueForm;
    var bodyName = "body_" + uniqueBody;
    var dati = $("#" + formName).serialize(); // serializes the form's elements.

    $("#data_" + uniqueBody).html("");

    jQuery.ajax({
      type: "POST",
      url: url,
      data: dati,
      success: function (data) {
        $("#" + bodyName).html(data);
      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in ricercaTool2: " + err);
      }
    });

    return false; // avoid to execute the actual submit of the form.
  }
  ,
  testInvioToolSimpleSearch(unique, url, e) {
    if (e === null)
      e = event;
    if (e.keyCode === 13) {
      this.submitTool2(unique, unique + "_simple", url);
      return false;
    }
    return true;
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
      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in jumpTool: " + err);
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
  cambiaTipoRicercaCombo(formName, fieldName, valore) {
    var nomeComboValori = "VL" + fieldName;
    var nomeComboFiltro = "OP" + fieldName;
    $("#" + formName + " select[name='" + nomeComboFiltro + "'] option[value='" + valore + "']").attr('selected', true);
  }
  ,
  cambiaTipoRicercaInput(formName, fieldName, tipo, valore) {
    // onchange
    var nomeCampoValori = tipo + fieldName;
    var nomeComboFiltro = "OP" + fieldName;
    var contenuto = $("#" + formName + " input[name='" + nomeCampoValori + "']").val();
    var filtroatt = $("#" + formName + " select[name='" + nomeComboFiltro + "']").val();
    var val = trim(contenuto);

    if ((filtroatt === "0" || filtroatt === "1") && val !== "") {
      $("#" + formName + " select[name='" + nomeComboFiltro + "'] option[value='" + valore + "']").attr('selected', true);
    }
  }
  ,
  cambiaTipoRicercaToolCombo(unique, uniqueForm, fieldName, valore) {
    var formName = "fo_" + uniqueForm;
    var nomeComboValori = "VL" + fieldName;
    var nomeComboFiltro = "OP" + fieldName;
    $("#" + formName + " select[name='" + nomeComboFiltro + "'] option[value='" + valore + "']").attr('selected', true);
  }
  ,
  cambiaTipoRicercaToolInput(unique, uniqueForm, fieldName, tipo, valore) {
    // onchange
    var formName = "fo_" + uniqueForm;
    var nomeCampoValori = tipo + fieldName;
    var nomeComboFiltro = "OP" + fieldName;
    var contenuto = $("#" + formName + " input[name='" + nomeCampoValori + "']").val();
    var filtroatt = $("#" + formName + " select[name='" + nomeComboFiltro + "']").val();
    var val = trim(contenuto);

    if ((filtroatt === "0" || filtroatt === "1") && val !== "") {
      $("#" + formName + " select[name='" + nomeComboFiltro + "'] option[value='" + valore + "']").attr('selected', true);
    }
  }
  ,
  submitDirectLista(type, url) {
    var dati = $("#fo" + type).serialize(); // serializes the form's elements.

    jQuery.ajax({
      type: "POST",
      url: url,
      data: dati,
      success: function (data) {
        $("#rigel_dialog_body").html(data);

        const re = /<!-- header: (.+) -->/;
        const ok = re.exec(data);
        if (ok)
          setTopDialogTitle(ok[1]);
      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in submitDirectLista: " + err);
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
      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in jumpDirectLista: " + err);
      }
    });
  }
  ,
  submitDirectForm(type, url) {
    var dati = $("#fo" + type).serialize(); // serializes the form's elements.

    jQuery.ajax({
      type: "POST",
      url: url,
      data: dati,
      success: function (data) {
        $("#rigel_dialog_body").html(data);

        const re = /<!-- header: (.+) -->/;
        const ok = re.exec(data);
        if (ok)
          setTopDialogTitle(ok[1]);
      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in submitDirectForm: " + err);
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
    this.runActionJsonAsync(true, uri, dati, fnExecute, fnReload);
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
  ,
  /**
   * Esegue una chiamata ad action Turbine attraverso action.jsp (vedi sirio).
   * @param {boolean} async vero per chiamata asincrona false per chiamata sincrona (bloccante)
   * @param {String} uri per raggiungere action.jsp
   * @param {object} dati dati da inviare alla action
   * @param {function} fnExecute funzione da eseguire con la risposta
   * @param {function} fnReload se attivo flag reload ricarica la pagina (opzionale)
   */
  runActionJsonAsync(async, uri, dati, fnExecute, fnReload) {
    jQuery.ajax({
      dataType: "json",
      url: uri,
      async: async,
      data: dati,
      success: function (data) {
        if (isOKVar(data.ERROR)) {
          bdError(data.ERROR);
          return;
        }

        if (isOKVar(data.message)) {
          bdAlert(data.message);
        }

        if (data.reload === "1") {
          if (fnReload !== "undefined")
            fnReload();
          return;
        }

        if (fnExecute !== "undefined")
          fnExecute(data);

      },
      error: function (jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.log("Request Failed in runActionJson: " + err);
      }
    });
  }

};

/**
 * Verifica che la variabile esista e sia diversa da stringa vuota.
 * @param {type} variabile
 * @returns {Boolean} vero se esiste ed è valorizzata
 */
function isOKVar(variabile) {
  return (variabile !== undefined && variabile !== "");
}
