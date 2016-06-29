$(function() {

    function lockFrame(message) {
        $("#loading-div").show();
        $("#loading-div-background").show();
    }

    function unlockFrame() {
        $("#loading-div").hide();
        $("#loading-div-background").hide();
    }
    
    var afterAddRepo = null;
    var afterCancelRepo = null;
        
    lockFrame("Loading");
    $.ajax({
        url : "noop",
        type : "post",
        dataType : "json"
    }).done(function(data) {
        update(data);
    });

    var tabs = $("#tabs").tabs();
    
    $("#refresh").button().click(function() {
        lockFrame("Refreshing");
        $.ajax({
            url : "noop",
            type : "post",
            dataType : "json"
        }).done(function(data) {
            update(data);
        });
    });


    $("#update_all").button().click(function() {
        lockFrame("Remove repository");
        $.ajax({
            url : "auto-migrate",
            type : "post",
            dataType : "json"
        }).done(function(data) {
            update(data);
        });
    });
    
    var afterError = null;

    $("#dialog-message").dialog({
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

    var clickOnInstall = function() {
    	  $.ajax({
            url : "get-repos",
            type : "post",
            data : {
            },
            dataType : "json"
        }).done(function(data) {
      	  var dialogInstallRepo = $("#dialog-install-repo");
      	  dialogInstallRepo.empty();
      	  if (data.length == 0) {
      	      dialog.dialog("open");
      	      afterAddRepo = clickOnInstall;
      	  } else {
      		  for (var i=0; i<data.length;i++) {
      			  dialogInstallRepo.append($("<option value=" + data[i].name + ">" + data[i].name + " (" + data[i].url + ")</option>"));
      		  }
      		  dialogInstallRepo.selectmenu("refresh");
      		  dialogInstall.dialog("open");
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
                lockFrame("Add repository");
                $.ajax({
                    url : "mk-repo",
                    type : "post",
                    data : {
                        "name" : $("#tab_localname").val(),
                        "url" : $("#tab_url").val()
                    },
                    dataType : "json"
                }).done(function(data) {
                    update(data, p);
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
        buttons : {
            Install : function() {
                lockFrame("Install application");
                $.ajax({
                    url : "install",
                    type : "post",
                    data : {
                        "repo" : $("#dialog-install-repo").val(),
                        "name" : $("#dialog-install-name").val(),
                        "version" : $("#dialog-install-version").val(),
                        "local" : $("#dialog-install-local").val()
                    },
                    dataType : "json"
                }).done(function(data) {
                    update(data);
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
    	  width: 400
    });
    
    $("#dialog-install-add-repo").button().click(function() {
	    dialogInstall.dialog("close");
        dialog.dialog("open");
	    afterAddRepo = clickOnInstall;
	    afterCancelRepo = clickOnInstall;
    });
    
    $("#dialog-install-name").autocomplete({
    	minLength : 0,
        source: function (req, res) {
    	  $.ajax({
              url : "get-repo-app-name",
              type : "post",
              data : {
              	"req" : req.term,
                "repo" : $("#dialog-install-repo").val()
              },
              dataType : "json"
          }).done(function(data) {
              res(data);
          });
      }
    });

    $("#dialog-install-version").autocomplete({
    	minLength : 0,
        source: function (req, res) {
      	  $.ajax({
                url : "get-repo-app-version",
                type : "post",
                data : {
                  	"req" : req.term,
                    "repo" : $("#dialog-install-repo").val(),
                    "name" : $("#dialog-install-name").val()
                },
                dataType : "json"
            }).done(function(data) {
                res(data);
            });
        }
      });    

    
    var migrateData = {};
    
    var dialogMigrate = $("#dialog-migrate").dialog({
        autoOpen : false,
        modal : true,
        buttons : {
            Migrate : function() {
                lockFrame("Migrate");
                $.ajax({
                    url : "migrate",
                    type : "post",
                    data : {
                        "name" : migrateData.appli,
                        "toVersion" : $("#dialog-migrate-version").val()
                    },
                    dataType : "json"
                }).done(function(data) {
                    update(data);
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
        source: function (req, res) {
      	  $.ajax({
                url : "get-repo-app-version",
                type : "post",
                data : {                    
                	"req" : req.term,
                    "name" : migrateData.appli
                },
                dataType : "json"
            }).done(function(data) {
                res(data);
            });
        }
      });    

    
    function updateItems(oldItems, newItems, comparatorFunc, deleteFunc, updateFunc, createFunc) {
        var j = 0;
        var oldItem = null;
        if (j < oldItems.length) {
            oldItem = oldItems[j];
        }

        for ( var i = 0; i < newItems.length; i++) {
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
        unlockFrame();
        if (state.error != null) {
            $("#dialog-message").children().remove();
            $("#dialog-message").append("<pre>" + state.error + "</pre>");
            $("#dialog-message").dialog("open");
            afterError = afterUpdate;
            afterUpdate = null;
        }
        currentState = state;
        if (afterUpdate != null) {
        	afterUpdate();
        }
    }

    var tabCounter = 1;
    var tabTemplate = "<li><a href='#{href}'>#{label}</a> </li>";

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
            lockFrame($(this).text() + " application");
            $.ajax({
                url : $(this).text(),
                type : "post",
                data : {
                    "name" : appli.name
                },
                dataType : "json"
            }).done(function(data) {
                update(data);
            });
        });

        var bugfix = $("<input type='checkbox' id='" + vid + "bf' /><label for='" + vid + "bf'>bugfix</label>");
        var minor = $("<input type='checkbox' id='" + vid + "mi' /><label for='" + vid + "mi'>minor evolution</label>");
        var major = $("<input type='checkbox' id='" + vid + "ma' /><label for='" + vid + "ma'>major evolution</label>");

        var buttonset = $("<span>Update on </span>").append(bugfix, minor, major);
        bugfix = $(bugfix[0]);
        minor = $(minor[0]);
        major = $(major[0]);
        bugfix.prop('checked', appli.bugfix);
        minor.prop('checked', appli.minor);
        major.prop('checked', appli.major);

        bugfix.button().click(function() {
            lockFrame("Toogle bugfix");
            $.ajax({
                url : "bugfix",
                type : "post",
                data : {
                    "name" : appli.name,
                    "value" : $(this).is(':checked')
                },
                dataType : "json"
            }).done(function(data) {
                update(data);
            });
        });

        minor.button().click(function() {
            lockFrame("Toogle minor evolution");
            $.ajax({
                url : "minor-evolution",
                type : "post",
                data : {
                    "name" : appli.name,
                    "value" : $(this).is(':checked')
                },
                dataType : "json"
            }).done(function(data) {
                update(data);
            });
        });

        major.button().click(function() {
            lockFrame("Toogle major evolution");
            $.ajax({
                url : "major-evolution",
                type : "post",
                data : {
                    "name" : appli.name,
                    "value" : $(this).is(':checked')
                },
                dataType : "json"
            }).done(function(data) {
                update(data);
            });
        });
        
        var autoStart = $("<input type='checkbox' id='" + vid + "check'>");
        autoStart.prop('checked', appli.autoStarted);
        
        var textSpan = $("<span>" + appli.name + " (" + appli.path + ") </span>")
        
        var content = $("<p></p>").append($("<span class='ui-widget-header ui-corner-all toolbar'></span>")                
                        .append(textSpan, startStop, autoStart, $("<label for='" + vid + "check'>AutoStart</label>"), buttonset, $("<button>migrate...</button>").button().click(function() {
                            migrateData.appli = appli.name;
                            dialogMigrate.dialog("open");
                        }), $("<button>uninstall</button>").button({
                            text : false,
                            icons : {
                                primary : "ui-icon-close"
                            }
                        }).click(function() {
                            lockFrame("Uninstall application");
                            $.ajax({
                                url : "uninstall",
                                type : "post",
                                data : {
                                    "name" : appli.name
                                },
                                dataType : "json"
                            }).done(function(data) {
                                update(data);
                            });
                        })

                        ));

        autoStart.button().click(function() {
            $.ajax({
                url : "auto-start",
                type : "post",
                data : {
                    "name" : appli.name,
                    "value" : $(this).is(':checked')
                },
                dataType : "json"
            }).done(function(data) {
                update(data);
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

    
    var termVar = $('#term_demo').terminal(function(command, term) {
        if (command !== '') {
            $.ajax({
                url : "execute",
                type : "post",
                data : {
                    "command" : command
                },
                dataType : "json"
            }).done(function(data) {
                term.echo(data);
            });
        } else {
           term.echo('');
        }
    }, {
        greetings: null,
        name: null,
        prompt: 'vestige:~ admin$ ',
        clear: null,
        exit: null,
        completion: function(term, buffer, pos, update) {
            $.ajax({
                url : "complete",
                type : "post",
                data : {
                    "buffer" : buffer,
                    "cursor" : pos
                },
                dataType : "json"
            }).done(function(data) {
                update(data);
            });
        },
    }).attr({
		tabIndex: 0
	}).focusin(function() {
    	termVar.focus(true);
    }).focusout(function() {
    	termVar.focus(false);
    })
    
    $("#open_term").button().click(function(e) {
    	dialogTerminal.dialog("open");
        e.stopPropagation();
    });
    

});
