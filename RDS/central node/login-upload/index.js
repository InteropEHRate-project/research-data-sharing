var express = require('express'),
    exphbs = require('express-handlebars'),
    logger = require('morgan'),
    cookieParser = require('cookie-parser'),
    bodyParser = require('body-parser'),
    methodOverride = require('method-override'),
    session = require('express-session'),
    passport = require('passport'),
    LocalStrategy = require('passport-local'),
    formidable = require('formidable'),
    fs = require('fs'),
    flash = require('express-flash'),
    HTMLParser = require('node-html-parser');


//We will be creating these two files shortly
var config = require('./config.js'), //config file contains all tokens and other private info
    funct = require('./functions.js'); //funct file contains our helper functions for our Passport and database work


var app = express();

//===============PASSPORT===============

//This section will contain our work with Passport

// Passport session setup.
passport.serializeUser(function(user, done) {
    console.log("serializing " + user.username);
    done(null, user);
});
  
passport.deserializeUser(function(obj, done) {
    console.log("deserializing " + obj);
    done(null, obj);
});

// Use the LocalStrategy within Passport to login/"signin" users.
passport.use('local-signin', new LocalStrategy(
    {passReqToCallback : true}, //allows us to pass back the request to the callback
    function(req, username, password, done) {
      funct.localAuth(username, password)
      .then(function (user) {
        if (user) {
          console.log("LOGGED IN AS: " + user.username);
          req.session.success = 'You are successfully logged in ' + user.username + '!';
          done(null, user);
        }
        if (!user) {
          console.log("COULD NOT LOG IN");
          req.session.error = 'Could not log user in. Please try again.'; //inform user could not log them in
          done(null, user);
        }
      })
      .fail(function (err){
        console.log(err.body);
      });
    }
  ));

  // Use the LocalStrategy within Passport to register/"signup" users.
  passport.use('local-signup', new LocalStrategy(
    {passReqToCallback : true}, //allows us to pass back the request to the callback
    function(req, username, password, done) {
      funct.localReg(username, password)
      .then(function (user) {
        if (user) {
          console.log("REGISTERED: " + user.username);
          req.session.success = 'You are successfully registered and logged in ' + user.username + '!';
          done(null, user);
        }
        if (!user) {
          console.log("COULD NOT REGISTER");
          req.session.error = 'That username is already in use, please try a different one.'; //inform user could not log them in
          done(null, user);
        }
      })
      .fail(function (err){
        console.log(err.body);
      });
    }
  ));

//===============EXPRESS================
// Configure Express
app.use(logger('combined'));
app.use(flash());
app.use(cookieParser());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
app.use(methodOverride('X-HTTP-Method-Override'));
app.use(session({secret: 'supernova', saveUninitialized: true, resave: true}));
app.use(passport.initialize());
app.use(passport.session());
app.use('/css',express.static('views/layouts/css/'));



// Session-persisted message middleware
app.use(function(req, res, next){
  var err = req.session.error,
      msg = req.session.notice,
      success = req.session.success;

  delete req.session.error;
  delete req.session.success;
  delete req.session.notice;

  if (err) res.locals.error = err;
  if (msg) res.locals.notice = msg;
  if (success) res.locals.success = success;

  next();
});

// Configure express to use handlebars templates
var hbs = exphbs.create({
    defaultLayout: 'main', //we will be creating this layout shortly
});
app.engine('handlebars', hbs.engine);
app.set('view engine', 'handlebars');

//===============ROUTES===============

//This section will hold our Routes

//displays our homepage
app.get('/', async function(req, res){

  if(req.user){
    var htmlList = {list:[]}
    if(req.user.username == config.admin.user && req.user.password == config.admin.password){
      htmlList.list = await funct.getAllRDD()
      res.render('home-admin', {user: req.user, expressFlashSuccess: req.flash('uploadSuccess'), expressFlashFailed: req.flash('uploadFail'), html: htmlList, config:config})
    } else {
      htmlList.list = await funct.getRDDbyUser(req.user.username)
      res.render('home', {user: req.user, expressFlashSuccess: req.flash('uploadSuccess'), expressFlashFailed: req.flash('uploadFail'), html: htmlList, config:config})
    }
  } else {
    res.render('signin', {user: req.user, config:config});
  }
  
})

//displays our signup page
app.get('/signin', function(req, res){
  res.render('signin');
});

//sends the request through our local signup strategy, and if successful takes user to homepage, otherwise returns then to signin page
app.post('/local-reg', passport.authenticate('local-signup', {
  successRedirect: '/',
  failureRedirect: '/signin'
  })
);

//sends the request through our local login/signin strategy, and if successful takes user to homepage, otherwise returns then to signin page
app.post('/login', passport.authenticate('local-signin', {
  successRedirect: '/',
  failureRedirect: '/signin'
  })
);

//logs user out of site, deleting them from the session, and returns to homepage
app.get('/logout', function(req, res){
  var name = req.user.username;
  console.log("LOGGIN OUT " + req.user.username)
  req.logout();
  res.redirect('/');
  req.session.notice = "You have successfully been logged out " + name + "!";
});

// document upload
app.post('/upload', async function(req, res){

  
  var form = new formidable.IncomingForm(
    { 
      uploadDir: config.rdd_directory_path,
      keepExtensions: true
    }
  )

  form.on('file', async function(field, file) {
    //rename the incoming file to the file's name
    fs.rename(file.path, config.rdd_directory_path + file.name, function(){console.log('rename file')});
  });


  var uploadFailed
  var validationLog = ""

  form.parse(req, async (err, fields, files) => {

    if (err) {
      console.error('Error', err)
      throw err
    } 

    var file = Object.entries(files)[0]
    var path = ''
    path = file[1].path.substring(0, file[1].path.lastIndexOf('/')+1) + file[1].name

    console.log("ResourcePath:"+path)

    if(path.endsWith("json")){

      try{
        console.log("qui-path:"+path)
        var rawdata = fs.readFileSync(path)
        var rdd = JSON.parse(rawdata)

      } catch(e){
        console.log(e)
        uploadFailed = true
      }

      /*   check RDD profile compliance   */
      if(rdd.resourceType != "Bundle") uploadFailed = true
      else{
        
        validationLog = await new Promise(resolve => {
          let log = funct.checkCompliance(path)
          resolve(log)
        }) 
        console.log("PROMISE:"+JSON.stringify(validationLog))

        if (validationLog.includes("ERROR")) uploadFailed = true
        if (rdd.entry[0].resource.resourceType != "ResearchStudy"){
          uploadFailed = true
          validationLog = "The resource is not a FHIR Research Study document (RDD)"
        }
      }
      

      if(!uploadFailed){

        let startDate = rdd.entry[0].resource.period.start
        let endDate = rdd.entry[0].resource.period.end
        let studyName = rdd.entry[0].resource.title
        let studyDescription = rdd.entry[0].resource.description

        let uploadResult = await funct.uploadDocument(file, req.user.username, studyName, startDate, endDate)
        if(uploadResult != null) uploadFailed = false
        else uploadFailed = true
        
      } else {
        try{
          if(file[1].name != '') fs.unlinkSync(path)
          else fs.unlinkSync(file[1].path)
        }catch(e){
          console.log(e)
        }
        
        //req.flash('uploadFail', 'Error the upload is failed, file structure not allowed !')
      }


      //}).on('end', async function() {
      let htmlList = {list:[]}
      htmlList.list = await funct.getRDDbyUser(req.user.username)

      if (!uploadFailed){
        req.flash('uploadSuccess', 'You have successfully upload your new study !')
        res.redirect(req.get('referer', {user: req.user, html: htmlList}))

      } else {
        req.flash('uploadFail', 'Error the upload is failed, file structure not allowed !\n '+validationLog)
        res.redirect(req.get('referer', {user: req.user, html: htmlList}))
      }

    } else {
      let htmlList = {list:[]}
      req.flash('uploadFail', 'Error, file not selected')
      res.redirect(req.get('referer', {user: req.user, html: htmlList}))
    }
  })


});
   
// document download
app.get('/download', function(req, res){
  console.log('Downloading RDD ...')
  const file = config.rdd_directory_path + req.query.filename;
  res.download(file); // Set disposition and send it.
});

// document publishing
app.post('/publishing', async function(req, res){

  funct.updatePublication(req.query.rdd_id, req.query.status)
  var htmlList = {list:[]}
  htmlList.list = await funct.getAllRDD()

  res.redirect(req.get('referer', {user: req.user, html: htmlList}))
});

// document delete
app.post('/delete', async function(req, res){

  funct.deleteRDD(req.query.rdd_id)
  fs.unlinkSync(req.query.path)
  var htmlList = {list:[]}
  htmlList.list = await funct.getAllRDD()

  res.redirect(req.get('referer', {user: req.user, html: htmlList}))
});

// study's details
app.get('/study', async function(req, res){

  let studyID = req.query.id
  let study_path = req.query.path
  let study_details = {}
  study_details = await funct.getStudyDetails(study_path)

  console.log("STUDY-DETAILS:"+study_details)

  //get fhir resources in the bundle
  let researchStudy = study_details.entry[0].resource
  let group = study_details.entry[1].resource
  //let location = study_details.entry[2].resource
  let endpoint = study_details.entry[study_details.entry.length-1].resource

  let locations = []
  for(let res of study_details.entry){
    if(res.resource.resourceType == "Location"){
      locations.push(res.resource)
    } 
  }
  
  let dataRequirementsList = study_details.entry[0].resource.extension[2].extension
  let enrollmentCriteriaList = study_details.entry[1].resource.characteristic

  let newEnrollmentCriteriaList = []
  for(criteria of enrollmentCriteriaList){
    let newCriteriaObj = {
      "code": criteria.code,
      "value": {},
      "exclude": criteria.exclude
    }
    if(criteria.hasOwnProperty('valueCodeableConcept')){
      newCriteriaObj.value = {
        "value":criteria.valueCodeableConcept.coding[0].display,
        "code":criteria.valueCodeableConcept.coding[0].code,
        "unit":criteria.valueCodeableConcept.coding[0].system

      }
    } 
    else if(criteria.hasOwnProperty('valueRange')){
      newCriteriaObj.value = {
        "value":"< "+criteria.valueRange.low.value + " " + criteria.valueRange.low.unit,
        "code":criteria.valueRange.low.code,
        "unit":criteria.valueRange.low.system
      }
    } 
    else if(criteria.hasOwnProperty('valueBoolean')){
      newCriteriaObj.value = {
        "value":criteria.valueBoolean,
        "code":"",
        "unit":""
      }
    } 
    else newCriteriaObj.value = "missing-value"
    newEnrollmentCriteriaList.push(newCriteriaObj)
  }

  //console.log(JSON.stringify(newEnrollmentCriteriaList))


  if(study_details != null)
    res.render('study', {
                          user: req.user, 
                          study: researchStudy,
                          dataRequirements: dataRequirementsList,
                          group: group,
                          locations: locations,
                          endpoint: endpoint,
                          enrollmentCriteria: newEnrollmentCriteriaList
                        })
  else
    res.render('study', {study: 'error !!'})
});

//===============Central Node External API==============

app.get("/getOpenStudies", async function(req, res){

  var openStudies = await funct.getOpenRDDs(true)

  res.setHeader('Content-Type', 'application/json')
  res.end(JSON.stringify(openStudies))
})

app.get("/open-studies-nosign", async function(req, res){

  var openStudies = await funct.getOpenRDDs(false)

  res.setHeader('Content-Type', 'application/json')
  res.end(JSON.stringify(openStudies))
})


//===============PORT=================
//var port = process.env.PORT || 5000; //select your port or let it pull from your .env file

var port = config.endpoint.port
app.listen(port);
console.log("listening on " + port + "!");