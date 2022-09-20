var bcrypt = require('bcryptjs'),
    Q = require('q'),
    pg = require('pg'),
    config = require('./config.js'),
    moment = require('moment'),
    fs = require('fs'),
    dateFormat = require('dateformat');
const { Code } = require('mongodb');
//const exec = require('child_process').execSync;

const util = require('util');
const exec = util.promisify(require('child_process').exec);


const { async } = require('q');

// psql information

var connectionInfo = config.connectionInfo;
const pool = new pg.Pool(connectionInfo);

//used in local-signup strategy

exports.localReg = function (username, password) {
    var deferred = Q.defer();

    //console.log(username)

    pool.connect(function(err, client, done) {
        if(err) {
          return console.error('error fetching client from pool', err);
        }
        var text = 'select * from account where username = $1::text'
        var values = [username]
        client.query(text, values, async function(err, res) {
          
            //call `done()` to release the client back to the pool
            done();
      
            if(err) {
                console.log('Failed check query Error ! '+err)
                console.log(err.stack)
                validUser = false
                deferred.resolve(false)
            }

            //console.log('Success!')
            
            if (res.rows.length == 0) {
                console.log("USERNAME is valid")
                let newUser = await insertUser(username, password)
                console.log(newUser)
                deferred.resolve(newUser)

            } else {
                console.log("USERNAME already exist")
                deferred.resolve(false)
            }
        })
    })

    return deferred.promise
}

let insertUser = async (username, password) => {

    var newUser = {}
    var textInsert = 'insert into account (username, password) values ($1::text , $2::text) RETURNING *'
    var valuesInsert = [username, password]

    newUser = await(pool.query(textInsert, valuesInsert));
    console.log(newUser.rows[0])

    return newUser.rows[0]
}


//check if user exists
    //if user exists check if passwords match (use bcrypt.compareSync(password, hash); // true where 'hash' is password in DB)
      //if password matches take into website
  //if user doesn't exist or password doesn't match tell them it failed
exports.localAuth = function (username, password) {
    var deferred = Q.defer();

    console.log('Strategy')

    pool.connect(function(err, client, done) {

        if(err) {
          return console.error('error fetching client from pool', err);
        }

        var text = 'select * from account where username = $1::text'
        var values = [username]
        client.query(text, values, function(err, res) {
          
            //call `done()` to release the client back to the pool
            done();
      
            if(err|| res.rows.length == 0) {
                console.log('Failed!')
                console.log(err)
                deferred.resolve(false);
            } else {
                console.log('Success!')
                let dbPwd = res.rows[0].password
                
                if (password == dbPwd) {
                    deferred.resolve(res.rows[0]);
                } else {
                    console.log("AUTHENTICATION FAILED");
                    deferred.resolve(false);
                }
            }
            
        });
    });

    return deferred.promise;
}

exports.uploadDocument = async function (doc, username, studyName, start_date, end_date) {
    console.log('upload for:'+username)

    if(doc == null){
        console.log('ERROR Document null !')
        return null
    }

    //console.log(doc[1].name)
    var newDoc = {}
    var filePath = doc[1].path.substring(0, doc[1].path.indexOf('RDD/')+4)+doc[1].name
    var textInsert = 'insert into rdd (pi, name, path, start_date, end_date, created_at, published, title) values ($1::text , $2::text , $3::text , $4::date , $5::date , $6::timestamp, false, $7::text) RETURNING *'
    var valuesInsert = [username, doc[1].name, filePath, start_date, end_date, moment().format(), studyName]

    return await (pool
        .query(textInsert, valuesInsert)
        .then(
            res => {return res.rows[0]}
        ).catch(
            err => {
                console.error('Error executing query', err.stack)
                return null
            }
        )
    )

/*
    if (newDoc != null){
        console.log(newDoc.rows[0])
        return newDoc.rows[0]
    }

    return null
*/
}

exports.getRDDbyUser = async function (username){

    var textSearch = 'select * from rdd where pi = $1::text'
    var valuesSearch = [username]
    return await (pool
        .query(textSearch, valuesSearch)
        .then(
            res => {
                result = res.rows
                for (elem of result){
                    elem.start_date = dateFormat(elem.start_date, "mmmm dS, yyyy")
                    elem.end_date = dateFormat(elem.end_date, "mmmm dS, yyyy")
                }
                return result
            }
        ).catch(
            err => {
                console.error('Error executing query', err.stack)
                return null
            }
        )
    )
}

exports.getAllRDD = async function (){

    var textSearch = 'select * from rdd'
    var valuesSearch = []
    return await (pool
        .query(textSearch, valuesSearch)
        .then(
            res => {
                result = res.rows
                for (elem of result){
                    elem.start_date = dateFormat(elem.start_date, "mmmm dS, yyyy")
                    elem.end_date = dateFormat(elem.end_date, "mmmm dS, yyyy")
                }
                return result
            }
        ).catch(
            err => {
                console.error('Error executing query', err.stack)
                return null
            }
        )
    )
}

exports.getAllRDDOpen = async function (){

    var textSearch = 'select * from rdd where published = true'
    var valuesSearch = []
    return await (pool
        .query(textSearch, valuesSearch)
        .then(
            res => {
                result = res.rows
                for (elem of result){
                    elem.start_date = dateFormat(elem.start_date, "mmmm dS, yyyy")
                    elem.end_date = dateFormat(elem.end_date, "mmmm dS, yyyy")
                }
                return result
            }
        ).catch(
            err => {
                console.error('Error executing query', err.stack)
                return null
            }
        )
    )
}

// Currently adopted returning signed RDD list and certificate. 
exports.getOpenRDDs = async function (signRequested){
    let today = new Date();
    let studies = await this.getAllRDDOpen() 

    let openStudiesBundle = {
                        "resourceType": "Bundle",
                        "id": "RDD-test-open-studies",
                        "type": "collection",
                        "entry": []
                    }

    for(std of studies){
        let rawdata = fs.readFileSync(std.path);
        let study = JSON.parse(rawdata);

        let start = new Date(study.entry[0].resource.extension[0].valuePeriod.start)
        let end = new Date(study.entry[0].resource.extension[0].valuePeriod.end)
        // enrollment period valifity check
        if(today>=start && today<=end){

            let mainBundleEntry = {
                "fullUrl": "http://iehrexampleurl/fhir/ResearchStudy/exampleResearchStudy",
                "resource": {}
            }
            mainBundleEntry.resource = study
            openStudiesBundle.entry.push(mainBundleEntry)
        }
            
    }

    let RDDsignedJSON = {
        "RDD": openStudiesBundle,
        "signature": "",
        "certificate": ""
    }

    if(signRequested){

        //sign the main RDDs Bundle
        let signatureJSON = await new Promise(resolve => {
            let out = signRDD(openStudiesBundle)
            resolve(out)
        }) 
        //console.log(signatureString)

        RDDsignedJSON.signature = signatureJSON.signature
        RDDsignedJSON.certificate = signatureJSON.certificate

        return RDDsignedJSON    

    } else {
        return RDDsignedJSON
    }
}

exports.updatePublication = async function (rdd_id, status){

    var textUpdate = ''
    var valuesUpdate = [rdd_id]

    if(status == 'true'){
        textUpdate = 'update rdd set published = false where rdd_id = $1::integer'
    } else {
        textUpdate = 'update rdd set published = true where rdd_id = $1::integer'
    }

    return await (pool
        .query(textUpdate, valuesUpdate)
        .then(
            res => {
                return res.rows
            }
        ).catch(
            err => {
                console.error('Error executing query', err.stack)
                return null
            }
        )
    )
}

exports.deleteRDD = async function (rdd_id){

    var textDelete = 'delete from rdd where rdd_id = $1::integer'
    var valuesDelete = [rdd_id]

    return await (pool
        .query(textDelete, valuesDelete)
        .then(
            res => {
                return res.rows
            }
        ).catch(
            err => {
                console.error('Error executing query', err.stack)
                return null
            }
        )
    )
}

exports.getStudyDetails = async function (path){
    try{
        let rawdata = fs.readFileSync(path)
        let rdd = JSON.parse(rawdata)
        
        console.log("ROW:"+rawdata)
        


        //dates formatting
        rdd.entry[0].resource.period.start = dateFormat(rdd.entry[0].resource.period.start, "mmmm dS, yyyy")
        rdd.entry[0].resource.period.end = dateFormat(rdd.entry[0].resource.period.end, "mmmm dS, yyyy")
        rdd.entry[0].resource.extension[0].valuePeriod.start = dateFormat(rdd.entry[0].resource.extension[0].valuePeriod.start, "mmmm dS, yyyy")
        rdd.entry[0].resource.extension[0].valuePeriod.end = dateFormat(rdd.entry[0].resource.extension[0].valuePeriod.end, "mmmm dS, yyyy")

        

        //dates formatting for data requiremnts
        let dataRequirements = rdd.entry[0].resource.extension[2].extension
        for(req of dataRequirements){

            if(req.valueDataRequirement.dateFilter != null){
                if(req.valueDataRequirement.dateFilter[0].valuePeriod.start != null)
                    req.valueDataRequirement.dateFilter[0].valuePeriod.start = dateFormat(req.valueDataRequirement.dateFilter[0].valuePeriod.start, "mmmm dS, yyyy")
                if(req.valueDataRequirement.dateFilter[0].valuePeriod.end != null)
                    req.valueDataRequirement.dateFilter[0].valuePeriod.end = dateFormat(req.valueDataRequirement.dateFilter[0].valuePeriod.end, "mmmm dS, yyyy")
            }
        }
        rdd.entry[0].resource.extension[2].extension = dataRequirements

        

        return rdd
    }
    catch(e){
        console.log("Error, "+e)
        return null
    }
}

exports.checkCompliance = async function (resourcePath){
    /*
    let result = exec("java -jar validator_cli.jar "+resourcePath)
    console.log("result:"+result.toString("utf8"))
    return result.toString("utf8")
*/
    return new Promise(resolve => {

        var spawn = require('child_process').spawn,
            //res = spawn('java', ['-jar', 'ValidatorFHIR-1.0-jar-with-dependencies.jar', resourcePath]);
            //res = spawn('java', ['-jar', 'ValidatorFHIR-1.0-shaded.jar', resourcePath]);
            res = spawn('java', ['-jar', 'CombinedValidator-0.4.jar', resourcePath]);
            
        var validationLog = ""
        var completed = false

        res.stdout.on('data', function (data) {

            //console.log("line:"+data.toString())
            


            if(data.toString().startsWith("ERROR")){
                //console.log("line:"+data.toString())
/*
                let logs = data.toString().split('\n')
                console.log(logs[0])
*/
                validationLog = validationLog + data.toString().split('\n')[0]
            }
        });
        
        res.stderr.on('data', function (data) {
            console.log('stderr: ' + data.toString());
        });
        
        res.on('exit', function (code) {

            console.log('child process exited with code ' + code.toString());
            console.log("OUT:"+validationLog)
            resolve(validationLog)
        })
    });
}

let signRDD = async function (resource){

    return new Promise(resolve => {

        var spawn = require('child_process').spawn,
        res = spawn('java', ['-jar', 'RDSSecurity-1.0-jar-with-dependencies.jar', resource]);
    
        var sign = ""
        var cert = ""
        var outputJSON = {
            "signature": "",
            "certificate": ""  
        }

        res.stdout.on('data', function (data) {
            //console.log(data.toString())
            let sout = data.toString()

            if(sout.includes("signed:")){
                outputJSON.signature = sout.substring(sout.indexOf("signed:")+7).replace(/\r?\n|\r/g, "")
            }
            if(sout.includes("cert:")){
                outputJSON.certificate = sout.substring(sout.indexOf("cert:")+5).replace(/\r?\n|\r/g, "")
            }
                     
        });
        
        res.stderr.on('data', function (data) {
            console.log('stderr: ' + data.toString());
        });
        
        res.on('exit', function (code) {
            resolve(outputJSON)
        })
    });

}