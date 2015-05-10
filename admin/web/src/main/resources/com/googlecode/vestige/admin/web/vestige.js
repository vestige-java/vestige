$(function() {

    function lockFrame(message) {
        $("#loading-div").show();
        $("#loading-div-background").show();
    }

    function unlockFrame() {
        $("#loading-div").hide();
        $("#loading-div-background").hide();
    }
    
    var activeRepo = -1;
    var lastCreateName = null;

    lockFrame("Loading");
    $.ajax({
        url : "noop",
        type : "post",
        dataType : "json"
    }).done(function(data) {
        update(data);
    });

    var tabs = $("#tabs").tabs();

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
        }
    });

    var dialog = $("#dialog-addrepo").dialog({
        autoOpen : false,
        modal : true,
        buttons : {
            Add : function() {
                lastCreateName = $("#tab_localname").val();
                lockFrame("Add repository");
                $.ajax({
                    url : "mk-repo",
                    type : "post",
                    data : {
                        "name" : lastCreateName,
                        "url" : $("#tab_url").val()
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

    var installData = {};
    
    var dialogInstall = $("#dialog-install").dialog({
        autoOpen : false,
        modal : true,
        buttons : {
            Install : function() {
                lockFrame("Install application");
                $.ajax({
                    url : "install",
                    type : "post",
                    data : {
                        "repo" : installData.repo,
                        "name" : $("#dialog-install-name").val(),
                        "version" : $("#dialog-install-version").val()
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
                        "repo" : migrateData.repo,
                        "name" : migrateData.appli,
                        "fromVersion" : migrateData.version,
                        "toVersion" : $("#tab_version").val()
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

    $("#add_tab").button().click(function() {
        dialog.dialog("open");
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
        "repository" : []
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

    function update(state) {
        state.repository = state.repository.sort(nameComparator);
        updateItems(currentState.repository, state.repository, nameComparator, function(repo) {
            repo.dom.li.remove();
            repo.dom.div.remove();
        }, updateRepository, createRepository);

        tabs.tabs("refresh");
        unlockFrame();
        if (state.error != null) {
            $("#dialog-message").children().remove();
            $("#dialog-message").append("<pre>" + state.error + "</pre>");
            $("#dialog-message").dialog("open");
        }
        if (lastCreateName == null) {
            if (currentState.repository.length == 0 && state.repository.length != 0) {
                tabs.tabs( "option", "active", 0);
            }
        } else {
            if (activeRepo != -1) {
                tabs.tabs( "option", "active", activeRepo);
                activeRepo = -1;
            }
            lastCreateName = null;
        }
        currentState = state;
    }

    var tabCounter = 1;
    var tabTemplate = "<li><a href='#{href}'>#{label}</a> </li>";

    function updateRepository(repo, currentRepo) {
        repo.nextVersion = currentRepo.nextVersion;
        repo.id = currentRepo.id;
        repo.dom = currentRepo.dom;
        repo.application = repo.application.sort(nameComparator);
        updateItems(currentRepo.application, repo.application, nameComparator, function(appli) {
            appli.dom.remove();
        }, function(appli, currentAppli) {
            updateApplication(repo, appli, currentAppli);
        }, function(appli, afterAppli) {
            createApplication(repo, appli, afterAppli);
        });
    }

    function createRepository(repo, afterRepo) {
        var id = "tabs-" + tabCounter;
        repo.id = id;
        repo.nextVersion = 0;
        var li = $(tabTemplate.replace(/#\{href\}/g, "#" + id).replace(/#\{label\}/g, repo.name)), tabContentHtml = repo.url;
        var content = $("<div id='" + id + "'><p><b>URL</b> <a href='" + tabContentHtml + "'>" + tabContentHtml
                + "</a></p></div>");
        content.append($("<p></p>").append($("<button>Remove this repository</button>").button().click(function() {
            lockFrame("Remove repository");
            $.ajax({
                url : "rm-repo",
                type : "post",
                data : {
                    "name" : repo.name
                },
                dataType : "json"
            }).done(function(data) {
                update(data);
            });
        }), $("<button>Install application...</button>").button().click(function() {
            installData.repo = repo.name;
            dialogInstall.dialog("open");
        })));

        content.append("<hr>Installed applications");

        repo.dom = {
            "li" : li,
            "div" : content
        };

        var applis = repo.application.sort(nameComparator);
        repo.application = applis;
        for ( var i = 0; i < applis.length; i++) {
            var appli = applis[i];
            createApplication(repo, appli, null);
        }

        if (afterRepo == null) {
            tabs.find(".ui-tabs-nav").append(li);
            tabs.append(content);
        } else {
            afterRepo.dom.li.before(li);
            afterRepo.dom.div.before(content);
        }
        if (lastCreateName == repo.name) {
            activeRepo = li.prevAll("li").size();
        }

        tabCounter++;
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

    function updateApplication(repo, appli, currentAppli) {
        appli.dom = currentAppli.dom;
        appli.version = appli.version.sort(versionComparator);
        updateItems(currentAppli.version, appli.version, versionComparator, function(appliVersion) {
            appliVersion.dom.p.remove();
        }, function(appliVersion, currentAppliVersion) {
            updateApplicationVersion(repo, appli, appliVersion, currentAppliVersion);
        }, function(appliVersion, afterAppliVersion) {
            createApplicationVersion(repo, appli, appliVersion, afterAppliVersion);
        });
    }

    function createApplication(repo, appli, afterAppli) {

        appli.dom = $("<div></div>");
        if (afterAppli == null) {
            repo.dom.div.append(appli.dom);
        } else {
            afterAppli.dom.before(appli.dom);
        }

        var versions = appli.version.sort(versionComparator);
        appli.version = versions;
        for ( var i = 0; i < versions.length; i++) {
            var version = versions[i];
            createApplicationVersion(repo, appli, version, null);
        }
    }

    function updateApplicationVersion(repo, appli, appliVersion, currentAppliVersion) {
        appliVersion.dom = currentAppliVersion.dom;
        if (appliVersion.started != currentAppliVersion.started) {
            var state;
            var icon;
            if (appliVersion.started) {
                state = "stop";
                icon = "ui-icon-stop";
            } else {
                state = "start";
                icon = "ui-icon-play";
            }
            var startStop = appliVersion.dom.startStop;
            startStop.button("option", {
                label : state,
                icons : {
                    primary : icon
                }
            });
        }
        appliVersion.dom.bugfix.attr('checked', appliVersion.bugfix);
        appliVersion.dom.minor.attr('checked', appliVersion.minor);
        appliVersion.dom.major.attr('checked', appliVersion.major);
        appliVersion.dom.buttonset.buttonset("refresh");
    }

    function createApplicationVersion(repo, appli, appliVersion, afterAppliVersion) {
        var vid = repo.id + "-v-" + repo.nextVersion;
        repo.nextVersion++;
        var state;
        var icon;
        if (appliVersion.started) {
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
                    "repo" : repo.name,
                    "name" : appli.name,
                    "version" : appliVersion.value
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
        bugfix.attr('checked', appliVersion.bugfix);
        minor.attr('checked', appliVersion.minor);
        major.attr('checked', appliVersion.major);

        bugfix.button().click(function() {
            lockFrame("Toogle bugfix");
            $.ajax({
                url : "bugfix",
                type : "post",
                data : {
                    "repo" : repo.name,
                    "name" : appli.name,
                    "version" : appliVersion.value,
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
                    "repo" : repo.name,
                    "name" : appli.name,
                    "version" : appliVersion.value,
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
                    "repo" : repo.name,
                    "name" : appli.name,
                    "version" : appliVersion.value,
                    "value" : $(this).is(':checked')
                },
                dataType : "json"
            }).done(function(data) {
                update(data);
            });
        });
        

        var content = $("<p></p>").append(
                $("<span class='ui-widget-header ui-corner-all toolbar'> " + appli.name + " " + appliVersion.value + " </span>")
                        .append(startStop, buttonset, $("<button>migrate...</button>").button().click(function() {
                            migrateData.repo = repo.name;
                            migrateData.appli = appli.name;
                            migrateData.version = appliVersion.value;
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
                                    "repo" : repo.name,
                                    "name" : appli.name,
                                    "version" : appliVersion.value
                                },
                                dataType : "json"
                            }).done(function(data) {
                                update(data);
                            });
                        })

                        ));

        buttonset.buttonset();

        appliVersion.dom = {
            "p" : content,
            "startStop" : startStop,
            "bugfix" : bugfix,
            "minor" : minor,
            "major" : major,
            "buttonset" : buttonset
        };

        if (afterAppliVersion == null) {
            appli.dom.append(content);
        } else {
            afterAppliVersion.dom.p.before(content);
        }
    }

});
