//var data = [];
//function write(str){data[data.length]=str;}
this.mysql = {
    insert: function (obj) {
        var name = obj.name;
        data = obj.data;
        var done = [];
        var names = mysqlQuery("SELECT column_name,data_type,column_comment,is_nullable FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'app' AND TABLE_NAME = '" + name + "';").execute();

        var nameList = [];
        var Length = 1;
        for (var i = 0; i < names.length; i++) {
            if (data[names[i][0]] != undefined) {
                if (Array.isArray(data[names[i][0]])) {
                    if (data[names[i][0]].length > Length) {
                        Length = data[names[i][0]].length;
                    }
                }
                // print(names[i][0]);
                nameList.push(names[i][0]);
            }
        }
        //UPDATE `app`.`dms_cuts_names` SET `name` = 'ABC=[100]=2' WHERE `dms_cuts_names`.`id` = 1;
        var Query = "Insert into " + name + " ( ";
        var Values = "VALUES ( "
        for (var i = 0; i < nameList.length; i++) {
            Query += nameList[i];
            if (i != nameList.length - 1) {
                Query += ',';
            } else {
                Query += ') ';
            }
            Values += ":value" + i;
            if (i != nameList.length - 1) {
                Values += ',';
            } else {
                Values += ');';
            }
        }

        for (var i = 0; i < Length; i++) {
            //print(Query+Values);
            var UQuery = mysqlQuery(Query + Values);
            for (var j = 0; j < nameList.length; j++) {
                if (Array.isArray(data[nameList[j]])) {
                    //print('setString isArray: ' + nameList[j]);
                    UQuery.setString('value' + (j), data[nameList[j]][i]);
                } else {
                    //print('setString : ',data[nameList[j]]);
                    UQuery.setString('value' + (j), data[nameList[j]] + '');
                }
            }
            var qr = ('Query : ', UQuery.getQuery());
            if (Length != 1) {
                try {
                    var id = UQuery.insert();
                    done.push(id + '');
                } catch (e) {
                    print(e);
                    done.push('-1');
                }
            } else {
                try {
                    var id = UQuery.insert();
                    done = (id + '');
                } catch (e) {
                    print(e);
                    print(qr);
                    done = ('-1');
                }
            }

        }
        return done;
    },
    update: function (name, data, where) {
        var names = mysqlQuery("SELECT column_name,data_type,column_comment,is_nullable FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'kapdaking' AND TABLE_NAME = '" + name + "';").execute();
        var done = [];
        var Query = "UPDATE " + name + " SET ";
        for (var i = 1; i < names.length; i++) {
            Query += names[i][0] + "= :" + names[i][0];
            if (i != names.length - 1) {
                Query += ',';
            } else {
                Query += ' Where ' + where + ' ;';
            }
        }
        for (var i = 0; i < data.id.length; i++) {
            var UQuery = mysqlQuery(Query);
            for (var j = 1; j < names.length; j++) {
                UQuery.setString(names[j][0] + '', data[names[j][0] + ''][i]);
                if (j == names.length - 1) {
                    UQuery.setString(names.length, data.id[i]);
                }
            }

            print(UQuery.getQuery() + '<br>');
            try {
                done.push(UQuery.update());
            } catch (e) {
                print(e);
                done.push('0');
            }
        }
        return done;
    },
    update2: function (obj) {
        var name = obj.name;
        data = obj.data;
        var done = [];
        var names = mysqlQuery("SELECT column_name,data_type,column_comment,is_nullable FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'app' AND TABLE_NAME = '" + name + "';").execute();

        var nameList = [];
        var Length = 1;
        for (var i = 1; i < names.length; i++) {
            if (data[names[i][0]] != undefined) {
                if (Array.isArray(data[names[i][0]])) {
                    if (data[names[i][0]].length > Length) {
                        Length = data[names[i][0]].length;
                    }
                }
                // print(names[i][0]);
                nameList.push(names[i][0]);
            }
        }
        //UPDATE `app`.`dms_cuts_names` SET `name` = 'ABC=[100]=2' WHERE `dms_cuts_names`.`id` = 1;
        var Query = "UPDATE " + name + " set ";
        var Values = "VALUES ( "
        for (var i = 0; i < nameList.length; i++) {
            Query += nameList[i] + ' = ' + ':value' + i;
            if (i != nameList.length - 1) {
                Query += ',';
            } else {
                Query += ' ';
            }
        }

        for (var i = 0; i < Length; i++) {
            //print(Query+Values);
            var UQuery = mysqlQuery(Query + 'Where ' + (obj.where || '1 = 1') + ';');
            for (var j = 0; j < nameList.length; j++) {
                if (Array.isArray(data[nameList[j]])) {
                    //print('setString isArray: ' + nameList[j]);
                    UQuery.setString('value' + (j), data[nameList[j]][i]);
                } else {
                    //print('setString : ',data[nameList[j]]);
                    UQuery.setString('value' + (j), data[nameList[j]] + '');
                }
            }
            var qr = ('Query : ', UQuery.getQuery());

            if (Length != 1) {
                try {
                    var id = UQuery.update();
                    done.push(id + '');
                } catch (e) {
                    print(e);
                    done.push('-1');
                }
            } else {
                try {
                    var id = UQuery.update();
                    done = (id + '');
                } catch (e) {
                    print(e);
                    print(qr);
                    done = ('-1');
                }
            }

        }
        return done;
    },
    selectF: function (obj) {
        var sqlFile = file(obj);
        if (sqlFile.isExist()) {
            var data = sqlFile.readString0();
            return mysqlQuery(data);
        }
    }
};
this.js = (function () {
    function js() {
        var name = [];
        var json = false;
        this.write = function (str) {
            if (json) {
                return null;
            }
            return name[name.length] = str;
        };
        this.get = function () {
            return name.join('');
        };

        this.reset = function () {
            name = []
        };

        this.getAndReset = function () {
            var str = name.join('');
            name = null;
            name = [];
            json = false;
            return str;
        };

        this.writeJSON = function (obj) {
            setJson();
            name = null;
            name = [];
            json = true;
            name[name.length] = JSON.stringify(obj);
            return obj;
        };

    }

    return new js();
}());

//print(this['js'].write('ok')+'');
var This = this;
//this.js = new js();

this.write = js.write;
this.ssjs = {
    sendReport:function(opt){
        if(opt.msg.msg == undefined){
                        opt.msg.msg = 'undefined';
                    }else if((opt.msg.msg+'').startsWith('[object HTML') == true){
                        opt.msg.msg = opt.msg.msg+'';
                    }else if(typeof opt.msg.msg == 'object'){
                        var object = opt.msg.msg;
                        opt.msg.msg = opt.msg.msg + '';
                        var val = ['{'];
                        var list = Object.keys(object);
                        list.forEach(function(value,no){
                            var $val = object[value];
                            print
                            if(typeof $val == 'number'){
                                val.push('  '+value + ' : ' + $val+'' + (list.length-1 == 0 ? '' : ','));
                            }else if(Array.isArray($val)){
                                val.push('  '+value + ' : [ \n                  ' + $val.join(',\n                  ')+'\n             ]' + (list.length-1 == 0 ? '' : ','));
                            }else if(typeof $val == 'object'){
                                val.push('  '+value + ' : ' + $val+'' + (list.length-1 == 0 ? '' : ','));
                            }else{
                                val.push('  '+value + ' : \'' + $val+'\'' + (list.length-1 == 0 ? '' : ','));
                            }
                        });
                        val.push('}');
                        opt.msg.msg = (val.join('\n'));
                    }else{
                        opt.msg.msg = opt.msg.msg+'';
                    }
        sendRport(opt.no,JSON.stringify(opt.msg));
    }
}
this.writeJSON = js.writeJSON;
this.jsonPrm = function () {
    return JSON.parse(prmJsonStr);
};

this.mysql.escap = function(str){
    return str.replace(/[\0\x08\x09\x1a\n\r"'\\\%]/g, function (char) {
        switch (char) {
            case "\0":
                return "\\0";
            case "\x08":
                return "\\b";
            case "\x09":
                return "\\t";
            case "\x1a":
                return "\\z";
            case "\n":
                return "\\n";
            case "\r":
                return "\\r";
            case "\"":
            case "'":
            case "\\":
            case "%":
                return "\\"+char; // prepends a backslash to backslash, percent,
                                  // and double/single quotes
        }
    });
}




// console.log(js);
// console.log('Person 2 name: ' + js.write('ok'));
Object.defineProperty(this, "write", {enumerable: false, configurable: false, writable: false});
Object.defineProperty(this, "write", {enumerable: false, configurable: false, writable: false});
Object.defineProperty(this, "jsonPrm", {enumerable: false, configurable: false, writable: false});
Object.defineProperty(js, "write", {enumerable: false, configurable: false, writable: false});
Object.defineProperty(js, "get", {enumerable: false, configurable: false, writable: false});
Object.defineProperty(js, "reset", {enumerable: false, configurable: false, writable: false});
Object.defineProperty(js, "writeJSON", {enumerable: false, configurable: false, writable: false});

// delete js.name;
// delete js.get;
//delete js.reset;
// console.log('Person 2 name: ' + js.get());

