$(function() {

  var dialogDisconnect = $("#dialog-disconnect").dialog({
    dialogClass : "no-close",
    closeOnEscape : false,
    autoOpen : false,
    height : "auto",
    width : "auto",
    modal : true,
    buttons : {
      Reconnect : function() {
        comm.reconnect();
      }
    },
  });

  
  var comm = new function() {
    var location = document.location.toString().replace('http://', 'ws://')

    var ws = new WebSocket(location);
    var opened = false;
    var callbackByCommand = {};
    
    function init() {
      ws.onclose = function(e) {
        if (opened) {
          opened = false;
          dialogDisconnect.dialog("open");
        }
      };

      ws.onopen = function() {
        opened = true;
        dialogDisconnect.dialog("close");
      };

      ws.onerror = function(e) {
        if (opened) {
          opened = false;
          dialogDisconnect.dialog("open");
        }
      };

      ws.onmessage = function(event) {
        var o = $.parseJSON(event.data);
        for (command in o) {
          var callback = callbackByCommand[command];
          if (callback != null) {
            callback(o[command]);
          }
        }
      }
      
    }

    init();

    this.register = function(command, callback) {
      callbackByCommand[command] = callback;
    }

    this.send = function(commands) {
      if (opened) {
        ws.send(JSON.stringify(commands));
      }
    }
    
    this.reconnect = function() {
      if (!opened) {
        ws = new WebSocket(location);
        init();
      }
    }

  };

  var dialogJob = $("#dialog-job");

  dialogJob.dialog({
    dialogClass : "no-close",
    closeOnEscape : false,
    autoOpen : false,
    height : 500,
    width : 750,
    modal : true,
    buttons : {
      "Run in background" : function() {
        comm.send({
          "guiBackground" : "1",
        });
      },
      "Interrupt" : function() {
        comm.send({
          "guiInterrupt" : "1",
        });
      }
    },
    close : function() {
    }
  });

  function startJob(command, args) {
    comm.send({
      "guiCommand" : command,
      "guiArgs" : args
    });
  }

  var completeHandlerTabByCommand = {};

  comm.register("completeResponse", function(response) {
    var completeHandlerTab = completeHandlerTabByCommand[response.command];
    if (completeHandlerTab != null) {
      var completeHandler = completeHandlerTab.shift();
      if (completeHandler != null) {
        completeHandler(response.args);
      }
    }
  });

  function complete(command, args, completeHandler) {
    var completeHandlerTab = completeHandlerTabByCommand[command];
    if (completeHandlerTab == null) {
      completeHandlerTab = [ completeHandler ];
      completeHandlerTabByCommand[command] = completeHandlerTab;
    } else {
      completeHandlerTab.push(completeHandler);
    }
    comm.send({
      "complete" : command,
      "args" : args
    });
  }

  var dialogJobProgressbars = $("#dialog-job-progressbars")

  comm.register("guiJob", function(description) {
    dialogJobProgressbars.children().remove();
    dialogJob.dialog("option", "title", description);
    dialogJob.dialog("open");
  });

  comm.register("guiTasks", function(tasks) {
    for (var i = 0; i < tasks.descriptions.length; i++) {
      var pla = $("<div/>", {
        "class" : "progress-label"
      }).text(tasks.descriptions[i]);
      var progressbar = dialogJobProgressbars.append($("<div/>").append(pla)).children().eq(-1);
      progressbar.progressbar({
        value : false,
      });
    }
    var children = dialogJobProgressbars.children();
    for (var i = 0; i < tasks.progress.length; i++) {
      children.eq(i).progressbar("option", "value", tasks.progress[i]);
    }
    dialogJob.scrollTop(dialogJob[0].scrollHeight);
  });

  comm.register("guiDone", function() {
    // dialogJobProgressbars.children().remove();
    dialogJob.dialog("close");
  });

  comm.register("application", function(application) {
    application = application.sort(nameComparator);

    updateItems(currentState.application, application, nameComparator, function(appli) {
      appli.dom.p.remove();
    }, function(appli, currentAppli) {
      updateApplication(appli, currentAppli);
    }, function(appli, afterAppli) {
      createApplication(appli, afterAppli);
    });
    currentState.application = application;
  });

  var afterAddRepo = null;
  var afterCancelRepo = null;

  var tabs = $("#tabs").tabs();

  $("#update_all").button().click(function() {
    startJob("auto-migrate");
  });

  var afterError = null;

  var dialogMessage = $("#dialog-message").dialog({
    autoOpen : false,
    height : "auto",
    width : "auto",
    dialogClass : "alert",
    modal : true,
    buttons : {
      Ok : function() {
        $(this).dialog("close");
      }
    },
    close : function() {
      var p = afterError;
      afterError = null;
      if (p != null) {
        p();
      }
    }
  });

  comm.register("guiError", function(description) {
    dialogMessage.children().remove();
    var pre = $("<pre/>");
    pre.text(description);
    dialogMessage.append(pre);
    dialogMessage.dialog("open");
  });

  var clickOnInstall = function() {
    complete("get-repos", {}, function(data) {
      var dialogInstallRepo = $("#dialog-install-repo");
      dialogInstallRepo.empty();
      if (data.length == 0) {
        dialog.dialog("open");
        afterAddRepo = clickOnInstall;
      } else {
        for (var i = 0; i < data.length; i++) {
          var opt = $("<option/>", {
            value : data[i].name
          });
          opt.text(data[i].name + " (" + data[i].url + ")");
          dialogInstallRepo.append(opt);
        }
        dialogInstallRepo.selectmenu("refresh");
        dialogInstall.dialog("open");
        dialogInstall.dialog('widget').position({
          my : "center",
          at : "center",
          "of" : window
        });
      }
    });
  }

  var dialog = $("#dialog-addrepo").dialog({
    autoOpen : false,
    modal : true,
    buttons : {
      Add : function() {
        var p = afterAddRepo;
        afterCancelRepo = null;
        afterAddRepo = null;
        startJob("mk-repo", {
          "name" : $("#tab_localname").val(),
          "url" : $("#tab_url").val()
        });
        $(this).dialog("close");
      },
      Cancel : function() {
        $(this).dialog("close");
      }
    },
    close : function() {
      $(this).find("form")[0].reset();
      var p = afterCancelRepo;
      afterCancelRepo = null;
      afterAddRepo = null;
      if (p != null) {
        p();
      }
    }
  });

  $("#install_app").button().click(clickOnInstall);

  var dialogInstall = $("#dialog-install").dialog({
    width : 620,
    autoOpen : false,
    modal : true,
    position : "center",
    buttons : {
      Install : function() {
        startJob("install", {
          "repo" : $("#dialog-install-repo").val(),
          "name" : $("#dialog-install-name").val(),
          "version" : $("#dialog-install-version").val(),
          "local" : $("#dialog-install-local").val()
        });
        $(this).dialog("close");
      },
      Cancel : function() {
        $(this).dialog("close");
      }
    },
    close : function() {
      $("#dialog-install-name").val("");
      $("#dialog-install-version").val("");
      $("#dialog-install-local").val("");
    }
  });

  $("#dialog-install-repo").selectmenu({
    width : 400
  });

  $("#dialog-install-add-repo").button().click(function() {
    dialogInstall.dialog("close");
    dialog.dialog("open");
    afterAddRepo = clickOnInstall;
    afterCancelRepo = clickOnInstall;
  });

  $("#dialog-install-name").autocomplete({
    minLength : 0,
    source : function(req, res) {
      complete("get-repo-app-name", {
        "req" : req.term,
        "repo" : $("#dialog-install-repo").val()
      }, res);
    }
  });


  $("#dialog-install-version").autocomplete({
    minLength : 0,
    source : function(req, res) {
      complete("get-repo-app-version", {
        "req" : req.term,
        "repo" : $("#dialog-install-repo").val(),
        "name" : $("#dialog-install-name").val()
      }, res);
    }
  });

  var migrateData = {};

  var dialogMigrate = $("#dialog-migrate").dialog({
    autoOpen : false,
    modal : true,
    buttons : {
      Migrate : function() {
        startJob("migrate", {
          "name" : migrateData.appli,
          "toVersion" : $("#dialog-migrate-version").val()
        });
        $(this).dialog("close");
      },
      Cancel : function() {
        $(this).dialog("close");
      }
    },
    close : function() {
      $(this).find("form")[0].reset();
    }
  });

  $("#dialog-migrate-version").autocomplete({
    minLength : 0,
    source : function(req, res) {
      complete("get-repo-app-version", {
        "req" : req.term,
        "name" : migrateData.appli
      }, res);
    }
  });

  function updateItems(oldItems, newItems, comparatorFunc, deleteFunc, updateFunc, createFunc) {
    var j = 0;
    var oldItem = null;
    if (j < oldItems.length) {
      oldItem = oldItems[j];
    }

    for (var i = 0; i < newItems.length; i++) {
      var newItem = newItems[i];
      while (oldItem != null && comparatorFunc(newItem, oldItem) > 0) {
        // delete
        deleteFunc(oldItem);
        j++;
        if (j < oldItems.length) {
          oldItem = oldItems[j];
        } else {
          oldItem = null;
        }
      }
      if (oldItem != null && comparatorFunc(newItem, oldItem) == 0) {
        // update
        updateFunc(newItem, oldItem);
        j++;
        if (j < oldItems.length) {
          oldItem = oldItems[j];
        } else {
          oldItem = null;
        }
      } else {
        // create
        createFunc(newItem, oldItem);
      }
    }
    while (oldItem != null) {
      // delete
      deleteFunc(oldItem);
      j++;
      if (j < oldItems.length) {
        oldItem = oldItems[j];
      } else {
        oldItem = null;
      }
    }
  }

  currentState = {
    "application" : []
  };

  function nameComparator(obj1, obj2) {
    var cmp = obj1.name.localeCompare(obj2.name);
    if (cmp < 0) {
      return -1;
    } else if (cmp == 0) {
      return 0;
    } else {
      return 1;
    }
  }

  function update(state, afterUpdate) {
    state.application = state.application.sort(nameComparator);

    updateItems(currentState.application, state.application, nameComparator, function(appli) {
      appli.dom.p.remove();
    }, function(appli, currentAppli) {
      updateApplication(appli, currentAppli);
    }, function(appli, afterAppli) {
      createApplication(appli, afterAppli);
    });

    tabs.tabs("refresh");
    // unlockFrame();
    if (state.error != null) {
      $("#dialog-message").children().remove();
      var pre = $("<pre/>");
      pre.text(state.error);
      $("#dialog-message").append(pre);
      $("#dialog-message").dialog("open");
      afterError = afterUpdate;
      afterUpdate = null;
    }
    currentState = state;
    if (afterUpdate != null) {
      afterUpdate();
    }
  }

  function versionComparator(version1, version2) {
    var v1 = version1.value.split(".");
    var v2 = version2.value.split(".");
    var diff = v2[0] - v1[0];
    if (diff == 0) {
      diff = v2[1] - v1[1];
      if (diff == 0) {
        diff = v2[2] - v1[2];
      }
    }
    if (diff < 0) {
      return -1;
    } else if (diff == 0) {
      return 0;
    } else {
      return 1;
    }
  }

  function updateApplication(appli, currentAppli) {
    appli.dom = currentAppli.dom;

    appli.dom.textSpan.text(appli.name + " (" + appli.path + ") ");

    if (appli.started != currentAppli.started) {
      var state;
      var icon;
      if (appli.started) {
        state = "stop";
        icon = "ui-icon-stop";
      } else {
        state = "start";
        icon = "ui-icon-play";
      }
      var startStop = appli.dom.startStop;
      startStop.button("option", {
        label : state,
        icons : {
          primary : icon
        }
      });
    }
    appli.dom.bugfix.prop('checked', appli.bugfix);
    appli.dom.minor.prop('checked', appli.minor);
    appli.dom.major.prop('checked', appli.major);
    appli.dom.buttonset.buttonset("refresh");
  }
  
  function appendUpdateInput(element, id, text) {
    var input = $("<input/>", {
      type: "checkbox",
      id : id
    });
    element.append(input);
    var label = $("<label/>", {
      "for" : id
    });
    label.text(text);
    element.append(label);
    return input;
  }
  
  function createApplication(appli, afterAppli) {
    var vid = "app-" + appli.name;
    var state;
    var icon;
    if (appli.started) {
      state = "stop";
      icon = "ui-icon-stop";
    } else {
      state = "start";
      icon = "ui-icon-play";
    }

    var startStop = $("<button>" + state + "</button>").button({
      text : false,
      icons : {
        primary : icon
      }
    }).click(function() {
      startJob($(this).text(), {
        "name" : appli.name
      });
    });

    var buttonset = $("<span>Update on </span>");
    var bugfix = appendUpdateInput(buttonset, vid + "bf", "bugfix");
    var minor = appendUpdateInput(buttonset, vid + "mi", "minor evolution");
    var major = appendUpdateInput(buttonset, vid + "ma", "major evolution");
    
    bugfix.prop('checked', appli.bugfix);
    minor.prop('checked', appli.minor);
    major.prop('checked', appli.major);

    bugfix.button().click(function() {
      startJob("bugfix", {
        "name" : appli.name,
        "value" : $(this).is(':checked')
      });
    });

    minor.button().click(function() {
      startJob("minor-evolution", {
        "name" : appli.name,
        "value" : $(this).is(':checked')
      });
    });

    major.button().click(function() {
      startJob("major-evolution", {
        "name" : appli.name,
        "value" : $(this).is(':checked')
      });
    });

    var autoStart = $("<input/>", {
      type : "checkbox",
      id : vid + "check"
    });
    autoStart.prop('checked', appli.autoStarted);

    var textSpan = $("<span/>");
    textSpan.text(appli.name + " (" + appli.path + ") ");

    var content = $("<p/>").append(
        $("<span class='ui-widget-header ui-corner-all toolbar'></span>").append(textSpan, startStop, autoStart, $("<label/>", {
          "for" : vid + "check"
        }).text("AutoStart"), buttonset,
            $("<button>migrate...</button>").button().click(function() {
              migrateData.appli = appli.name;
              dialogMigrate.dialog("open");
            }), $("<button>uninstall</button>").button({
              text : false,
              icons : {
                primary : "ui-icon-close"
              }
            }).click(function() {
              startJob("uninstall", {
                "name" : appli.name
              });
            })));

    autoStart.button().click(function() {
      startJob("auto-start", {
        "name" : appli.name,
        "value" : $(this).is(':checked')
      });
    });
    buttonset.buttonset();

    appli.dom = {
      "p" : content,
      "startStop" : startStop,
      "bugfix" : bugfix,
      "minor" : minor,
      "major" : major,
      "buttonset" : buttonset,
      "textSpan" : textSpan
    };

    if (afterAppli == null) {
      $("#tabs").append(content);
    } else {
      afterAppli.dom.p.before(content);
    }

  }

  var dialogTerminal = $("#dialog-terminal").dialog({
    width : 1000,
    height : 620,
    autoOpen : false,
    modal : true,
  });

  var focused = false;
  var termVar = $('#term_demo').terminal(function(command, term) {
    if (command !== '') {
      term.pause();
      comm.send({
        "termCommand" : command
      });
    } else {
      term.echo('');
    }
  }, {
    greetings : null,
    name : null,
    prompt : 'vestige:~ admin$ ',
    clear : null,
    exit : null,
    completion : function(term, buffer, pos, update) {
      complete("termComplete", {
        "buffer" : buffer,
        "cursor" : pos
      }, update);
    },
  }).attr({
    tabIndex : 0
  }).focusin(function() {
    focused = true;
    if (!termVar.paused()) {
      termVar.focus(true);
    }
  }).focusout(function() {
    focused = false;
    if (!termVar.paused()) {
      termVar.focus(false);
    }
  });

  var termTmp = false;

  comm.register("termEcho", function(value) {
    var i = 0;
    if (termTmp) {
      termVar.update(-1, value[0]);
      termTmp = false;
      i++;
    }
    while (i < value.length) {
      termVar.echo(value[i]);
      i++;
    }
  });
  comm.register("termTmpEcho", function(value) {
    if (termTmp) {
      termVar.update(-1, value);
    } else {
      termVar.echo(value);
      termTmp = true;
    }
  });
  comm.register("termDone", function() {
    if (termTmp) {
      termTmp = false;
      termVar.update(-1, "");
    }
    termVar.resume();
    if (!focused) {
      termVar.disable();
    }
  });

  $("#open_term").button().click(function(e) {
    dialogTerminal.dialog("open");
    e.stopPropagation();
  });

});
