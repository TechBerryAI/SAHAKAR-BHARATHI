package com.example.sahakarbharati.core.copy

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(
    val key: String,
    val englishName: String,
    val nativeName: String,
) {
    EN("en", "English", "English"),
    HI("hi", "Hindi", "हिन्दी"),
    BN("bn", "Bengali", "বাংলা"),
    TE("te", "Telugu", "తెలుగు"),
    MR("mr", "Marathi", "मराठी"),
    TA("ta", "Tamil", "தமிழ்"),
    UR("ur", "Urdu", "اردو"),
    GU("gu", "Gujarati", "ગુજરાતી"),
    KN("kn", "Kannada", "ಕನ್ನಡ"),
    OR("or", "Odia", "ଓଡ଼ିଆ"),
    ML("ml", "Malayalam", "മലയാളം"),
    PA("pa", "Punjabi", "ਪੰਜਾਬੀ"),
    AS("as", "Assamese", "অসমীয়া"),
    MAI("mai", "Maithili", "मैथिली"),
    SAT("sat", "Santali", "ᱥᱟᱱᱛᱟᱲᱤ"),
    KS("ks", "Kashmiri", "کٲشُر"),
    NE("ne", "Nepali", "नेपाली"),
    KOK("kok", "Konkani", "कोंकणी"),
    SD("sd", "Sindhi", "سنڌي"),
    DOI("doi", "Dogri", "डोगरी"),
    MNI("mni", "Manipuri", "মৈতৈলোন্"),
    BRX("brx", "Bodo", "बरʼ"),
    SA("sa", "Sanskrit", "संस्कृतम्"),
    BHO("bho", "Bhojpuri", "भोजपुरी"),
    RAJ("raj", "Rajasthani", "राजस्थानी"),
    TCY("tcy", "Tulu", "ತುಳು"),
    ;

    companion object {
        private val hindiFamily = setOf(HI, MAI, BHO, RAJ, DOI, NE, KOK, SA)

        fun fromKey(key: String): AppLanguage {
            val normalized = key.trim().lowercase()
            return entries.find { it.key.equals(normalized, ignoreCase = true) }
                ?: entries.find { it.name.equals(key.trim(), ignoreCase = true) }
                ?: EN
        }

        fun usesHindiCopy(language: AppLanguage): Boolean = language in hindiFamily
    }
}

class RathYatraCopy {
    lateinit var portalChip: String
    lateinit var honourLine: String
    lateinit var title: String
    lateinit var subtitle: String
    lateinit var tagline: String
    lateinit var statFarmers: String
    lateinit var statFarmersSub: String
    lateinit var statFishers: String
    lateinit var statFishersSub: String
    lateinit var statHelp: String
    lateinit var statHelpSub: String
    lateinit var statPass: String
    lateinit var statPassSub: String
    lateinit var getStarted: String
    lateinit var signIn: String
    lateinit var signInOfficial: String
    lateinit var digitalIndia: String
    lateinit var govLine: String
    lateinit var ministryLine: String
    lateinit var nationalPortal: String
    lateinit var nationwide: String
    lateinit var platformTitle: String
    lateinit var platformAgency: String
    lateinit var platformBody: String
    lateinit var editionLabel: String
    lateinit var festivalTitle: String
    lateinit var festivalDate: String
    lateinit var editionRibbon: String
    lateinit var editionChip: String
    lateinit var servicesTitle: String
    lateinit var servicesTrail: String
    lateinit var serviceTrack: String
    lateinit var serviceTrackChip: String
    lateinit var serviceTrackBody: String
    lateinit var serviceAccess: String
    lateinit var serviceAccessChip: String
    lateinit var serviceAccessBody: String
    lateinit var serviceUpdates: String
    lateinit var serviceUpdatesChip: String
    lateinit var serviceUpdatesBody: String
    lateinit var footerTitle: String
    lateinit var footerSub: String
    lateinit var officialPortal: String
    lateinit var createAccountTitle: String
    lateinit var createAccountBody: String
    lateinit var trustTitle: String
    lateinit var trustBody: String
    lateinit var privileges: String
    lateinit var featuresCount: String
    lateinit var priv1Title: String
    lateinit var priv1Body: String
    lateinit var priv2Title: String
    lateinit var priv2Body: String
    lateinit var priv3Title: String
    lateinit var priv3Body: String
    lateinit var priv4Title: String
    lateinit var priv4Body: String
    lateinit var priv5Title: String
    lateinit var priv5Body: String
    lateinit var kycTitle: String
    lateinit var kycBody: String
    lateinit var kycTime: String
    lateinit var createAccount: String
    lateinit var alreadyRegistered: String
    lateinit var formTitle: String
    lateinit var stepLabel: String
    lateinit var govtPortal: String
    lateinit var stepPersonal: String
    lateinit var stepContact: String
    lateinit var stepProfile: String
    lateinit var stepSecurity: String
    lateinit var formCardTitle: String
    lateinit var formCardBody: String
    lateinit var legalName: String
    lateinit var asInId: String
    lateinit var nameHint: String
    lateinit var gender: String
    lateinit var mandatory: String
    lateinit var male: String
    lateinit var female: String
    lateinit var other: String
    lateinit var dob: String
    lateinit var ageMin: String
    lateinit var calculatedAge: String
    lateinit var ageValue: String
    lateinit var eligible: String
    lateinit var eligibleNote: String
    lateinit var occupation: String
    lateinit var occupationHint: String
    lateinit var farmer: String
    lateinit var fisherman: String
    lateinit var both: String
    lateinit var allied: String
    lateinit var farmerDesc: String
    lateinit var fishermanDesc: String
    lateinit var bothDesc: String
    lateinit var alliedDesc: String
    lateinit var nextStepTitle: String
    lateinit var upcoming: String
    lateinit var nextStepBody: String
    lateinit var privacyTitle: String
    lateinit var privacyBody: String
    lateinit var continueNext: String
    lateinit var saveLater: String
    lateinit var successTitle: String
    lateinit var successBody: String
    lateinit var docket: String
    lateinit var docketSub: String
    lateinit var active: String
    lateinit var participantId: String
    lateinit var participantIdValue: String
    lateinit var occupationLabel: String
    lateinit var occupationValue: String
    lateinit var regionLabel: String
    lateinit var regionValue: String
    lateinit var passStatus: String
    lateinit var passReady: String
    lateinit var barcodeTitle: String
    lateinit var barcodeBody: String
    lateinit var goHome: String
    lateinit var viewPass: String
    lateinit var successNote: String
    lateinit var successFooter: String
    lateinit var topBarKicker: String
    lateinit var homeTitle: String
    lateinit var greeting: String
    lateinit var roleChip: String
    lateinit var homeId: String
    lateinit var homeZone: String
    lateinit var ePass: String
    lateinit var sessionLine: String
    lateinit var interfaceMode: String
    lateinit var citizenView: String
    lateinit var adminView: String
    lateinit var liveProcession: String
    lateinit var locationChip: String
    lateinit var heroKicker: String
    lateinit var heroTitle: String
    lateinit var heroSub: String
    lateinit var metricCrowd: String
    lateinit var metricCrowdVal: String
    lateinit var metricCrowdCap: String
    lateinit var metricWeather: String
    lateinit var metricWeatherVal: String
    lateinit var metricWeatherCap: String
    lateinit var metricTravel: String
    lateinit var metricTravelVal: String
    lateinit var metricTravelCap: String
    lateinit var upcomingRitual: String
    lateinit var upcomingRitualName: String
    lateinit var upcomingRitualTime: String
    lateinit var liveRouteCta: String
    lateinit var coreServices: String
    lateinit var quickAccess: String
    lateinit var tileTrack: String
    lateinit var tileTrackBody: String
    lateinit var tileTrackChip: String
    lateinit var tileQr: String
    lateinit var tileQrBody: String
    lateinit var tileQrChip: String
    lateinit var tileBenefits: String
    lateinit var tileBenefitsBody: String
    lateinit var tileBenefitsChip: String
    lateinit var tileVolunteer: String
    lateinit var tileVolunteerBody: String
    lateinit var tileVolunteerChip: String
    lateinit var trajectory: String
    lateinit var trajectorySub: String
    lateinit var startPoint: String
    lateinit var currentPoint: String
    lateinit var endPoint: String
    lateinit var advisories: String
    lateinit var viewAll: String
    lateinit var adv1Source: String
    lateinit var adv1Time: String
    lateinit var adv1Title: String
    lateinit var adv1Body: String
    lateinit var adv2Source: String
    lateinit var adv2Time: String
    lateinit var adv2Title: String
    lateinit var adv2Body: String
    lateinit var streamTitle: String
    lateinit var streamLive: String
    lateinit var stream1: String
    lateinit var stream2: String
    lateinit var emergencyTitle: String
    lateinit var emergencyToll: String
    lateinit var emergencyBody: String
    lateinit var policeSos: String
    lateinit var helpline: String
    lateinit var tabHome: String
    lateinit var tabYatra: String
    lateinit var tabAttendance: String
    lateinit var tabBenefits: String
    lateinit var tabProfile: String
    lateinit var trackTitle: String
    lateinit var telemetryLive: String
    lateinit var lastSynced: String
    lateinit var traversal: String
    lateinit var distanceTo: String
    lateinit var estArrival: String
    lateinit var arrivalTime: String
    lateinit var corridor: String
    lateinit var corridorChip: String
    lateinit var node1: String
    lateinit var node2: String
    lateinit var node3: String
    lateinit var node4: String
    lateinit var node5: String
    lateinit var mapLabel: String
    lateinit var feedLabel: String
    lateinit var chariotSection: String
    lateinit var activeCount: String
    lateinit var rath1: String
    lateinit var rath1Sub: String
    lateinit var rath1Spec: String
    lateinit var rath1Status: String
    lateinit var rath1Loc: String
    lateinit var rath1Speed: String
    lateinit var rath1Next: String
    lateinit var rath2: String
    lateinit var rath2Sub: String
    lateinit var rath2Spec: String
    lateinit var rath2Status: String
    lateinit var rath2Loc: String
    lateinit var rath2Speed: String
    lateinit var rath2Next: String
    lateinit var rath3: String
    lateinit var rath3Sub: String
    lateinit var rath3Spec: String
    lateinit var rath3Status: String
    lateinit var rath3Loc: String
    lateinit var rath3Speed: String
    lateinit var rath3Next: String
    lateinit var amenities: String
    lateinit var radius: String
    lateinit var water: String
    lateinit var waterBody: String
    lateinit var waterChip: String
    lateinit var medical: String
    lateinit var medicalBody: String
    lateinit var medicalChip: String
    lateinit var lost: String
    lateinit var lostBody: String
    lateinit var lostChip: String
    lateinit var photoCaption: String
    lateinit var gpsNote: String
    lateinit var attendanceTitle: String
    lateinit var scannerActive: String
    lateinit var scanTab: String
    lateinit var passTab: String
    lateinit var counterChip: String
    lateinit var reticleHint: String
    lateinit var pilgrimName: String
    lateinit var pilgrimReg: String
    lateinit var verified: String
    lateinit var passHint: String
    lateinit var lastRegistered: String
    lateinit var recorded: String
    lateinit var lastPlace: String
    lateinit var lastChip: String
    lateinit var historyTitle: String
    lateinit var historyCount: String
    lateinit var hist1: String
    lateinit var hist1Sub: String
    lateinit var hist2: String
    lateinit var hist2Sub: String
    lateinit var scanHelp: String
    lateinit var scanHelpBody: String
    lateinit var enterPin: String
    lateinit var report: String
    lateinit var pinTitle: String
    lateinit var pinBody: String
    lateinit var pinLabel: String
    lateinit var submitPin: String
    lateinit var pinNote: String
    lateinit var comingSoon: String
    lateinit var comingSoonBody: String
    lateinit var serverWaking: String
    lateinit var signInTitle: String
    lateinit var notificationsTitle: String
    lateinit var benefitsTitle: String
    lateinit var profileTitle: String
    lateinit var sosTitle: String
    lateinit var sosBody: String
    lateinit var adminTitle: String
    lateinit var adminBody: String
    lateinit var langEn: String
    lateinit var langHi: String
    lateinit var chooseLanguage: String
    lateinit var searchLanguages: String

    fun cloneCopy(): RathYatraCopy {
        val copy = RathYatraCopy()
            copy.portalChip = portalChip
            copy.honourLine = honourLine
            copy.title = title
            copy.subtitle = subtitle
            copy.tagline = tagline
            copy.statFarmers = statFarmers
            copy.statFarmersSub = statFarmersSub
            copy.statFishers = statFishers
            copy.statFishersSub = statFishersSub
            copy.statHelp = statHelp
            copy.statHelpSub = statHelpSub
            copy.statPass = statPass
            copy.statPassSub = statPassSub
            copy.getStarted = getStarted
            copy.signIn = signIn
            copy.signInOfficial = signInOfficial
            copy.digitalIndia = digitalIndia
            copy.govLine = govLine
            copy.ministryLine = ministryLine
            copy.nationalPortal = nationalPortal
            copy.nationwide = nationwide
            copy.platformTitle = platformTitle
            copy.platformAgency = platformAgency
            copy.platformBody = platformBody
            copy.editionLabel = editionLabel
            copy.festivalTitle = festivalTitle
            copy.festivalDate = festivalDate
            copy.editionRibbon = editionRibbon
            copy.editionChip = editionChip
            copy.servicesTitle = servicesTitle
            copy.servicesTrail = servicesTrail
            copy.serviceTrack = serviceTrack
            copy.serviceTrackChip = serviceTrackChip
            copy.serviceTrackBody = serviceTrackBody
            copy.serviceAccess = serviceAccess
            copy.serviceAccessChip = serviceAccessChip
            copy.serviceAccessBody = serviceAccessBody
            copy.serviceUpdates = serviceUpdates
            copy.serviceUpdatesChip = serviceUpdatesChip
            copy.serviceUpdatesBody = serviceUpdatesBody
            copy.footerTitle = footerTitle
            copy.footerSub = footerSub
            copy.officialPortal = officialPortal
            copy.createAccountTitle = createAccountTitle
            copy.createAccountBody = createAccountBody
            copy.trustTitle = trustTitle
            copy.trustBody = trustBody
            copy.privileges = privileges
            copy.featuresCount = featuresCount
            copy.priv1Title = priv1Title
            copy.priv1Body = priv1Body
            copy.priv2Title = priv2Title
            copy.priv2Body = priv2Body
            copy.priv3Title = priv3Title
            copy.priv3Body = priv3Body
            copy.priv4Title = priv4Title
            copy.priv4Body = priv4Body
            copy.priv5Title = priv5Title
            copy.priv5Body = priv5Body
            copy.kycTitle = kycTitle
            copy.kycBody = kycBody
            copy.kycTime = kycTime
            copy.createAccount = createAccount
            copy.alreadyRegistered = alreadyRegistered
            copy.formTitle = formTitle
            copy.stepLabel = stepLabel
            copy.govtPortal = govtPortal
            copy.stepPersonal = stepPersonal
            copy.stepContact = stepContact
            copy.stepProfile = stepProfile
            copy.stepSecurity = stepSecurity
            copy.formCardTitle = formCardTitle
            copy.formCardBody = formCardBody
            copy.legalName = legalName
            copy.asInId = asInId
            copy.nameHint = nameHint
            copy.gender = gender
            copy.mandatory = mandatory
            copy.male = male
            copy.female = female
            copy.other = other
            copy.dob = dob
            copy.ageMin = ageMin
            copy.calculatedAge = calculatedAge
            copy.ageValue = ageValue
            copy.eligible = eligible
            copy.eligibleNote = eligibleNote
            copy.occupation = occupation
            copy.occupationHint = occupationHint
            copy.farmer = farmer
            copy.fisherman = fisherman
            copy.both = both
            copy.allied = allied
            copy.farmerDesc = farmerDesc
            copy.fishermanDesc = fishermanDesc
            copy.bothDesc = bothDesc
            copy.alliedDesc = alliedDesc
            copy.nextStepTitle = nextStepTitle
            copy.upcoming = upcoming
            copy.nextStepBody = nextStepBody
            copy.privacyTitle = privacyTitle
            copy.privacyBody = privacyBody
            copy.continueNext = continueNext
            copy.saveLater = saveLater
            copy.successTitle = successTitle
            copy.successBody = successBody
            copy.docket = docket
            copy.docketSub = docketSub
            copy.active = active
            copy.participantId = participantId
            copy.participantIdValue = participantIdValue
            copy.occupationLabel = occupationLabel
            copy.occupationValue = occupationValue
            copy.regionLabel = regionLabel
            copy.regionValue = regionValue
            copy.passStatus = passStatus
            copy.passReady = passReady
            copy.barcodeTitle = barcodeTitle
            copy.barcodeBody = barcodeBody
            copy.goHome = goHome
            copy.viewPass = viewPass
            copy.successNote = successNote
            copy.successFooter = successFooter
            copy.topBarKicker = topBarKicker
            copy.homeTitle = homeTitle
            copy.greeting = greeting
            copy.roleChip = roleChip
            copy.homeId = homeId
            copy.homeZone = homeZone
            copy.ePass = ePass
            copy.sessionLine = sessionLine
            copy.interfaceMode = interfaceMode
            copy.citizenView = citizenView
            copy.adminView = adminView
            copy.liveProcession = liveProcession
            copy.locationChip = locationChip
            copy.heroKicker = heroKicker
            copy.heroTitle = heroTitle
            copy.heroSub = heroSub
            copy.metricCrowd = metricCrowd
            copy.metricCrowdVal = metricCrowdVal
            copy.metricCrowdCap = metricCrowdCap
            copy.metricWeather = metricWeather
            copy.metricWeatherVal = metricWeatherVal
            copy.metricWeatherCap = metricWeatherCap
            copy.metricTravel = metricTravel
            copy.metricTravelVal = metricTravelVal
            copy.metricTravelCap = metricTravelCap
            copy.upcomingRitual = upcomingRitual
            copy.upcomingRitualName = upcomingRitualName
            copy.upcomingRitualTime = upcomingRitualTime
            copy.liveRouteCta = liveRouteCta
            copy.coreServices = coreServices
            copy.quickAccess = quickAccess
            copy.tileTrack = tileTrack
            copy.tileTrackBody = tileTrackBody
            copy.tileTrackChip = tileTrackChip
            copy.tileQr = tileQr
            copy.tileQrBody = tileQrBody
            copy.tileQrChip = tileQrChip
            copy.tileBenefits = tileBenefits
            copy.tileBenefitsBody = tileBenefitsBody
            copy.tileBenefitsChip = tileBenefitsChip
            copy.tileVolunteer = tileVolunteer
            copy.tileVolunteerBody = tileVolunteerBody
            copy.tileVolunteerChip = tileVolunteerChip
            copy.trajectory = trajectory
            copy.trajectorySub = trajectorySub
            copy.startPoint = startPoint
            copy.currentPoint = currentPoint
            copy.endPoint = endPoint
            copy.advisories = advisories
            copy.viewAll = viewAll
            copy.adv1Source = adv1Source
            copy.adv1Time = adv1Time
            copy.adv1Title = adv1Title
            copy.adv1Body = adv1Body
            copy.adv2Source = adv2Source
            copy.adv2Time = adv2Time
            copy.adv2Title = adv2Title
            copy.adv2Body = adv2Body
            copy.streamTitle = streamTitle
            copy.streamLive = streamLive
            copy.stream1 = stream1
            copy.stream2 = stream2
            copy.emergencyTitle = emergencyTitle
            copy.emergencyToll = emergencyToll
            copy.emergencyBody = emergencyBody
            copy.policeSos = policeSos
            copy.helpline = helpline
            copy.tabHome = tabHome
            copy.tabYatra = tabYatra
            copy.tabAttendance = tabAttendance
            copy.tabBenefits = tabBenefits
            copy.tabProfile = tabProfile
            copy.trackTitle = trackTitle
            copy.telemetryLive = telemetryLive
            copy.lastSynced = lastSynced
            copy.traversal = traversal
            copy.distanceTo = distanceTo
            copy.estArrival = estArrival
            copy.arrivalTime = arrivalTime
            copy.corridor = corridor
            copy.corridorChip = corridorChip
            copy.node1 = node1
            copy.node2 = node2
            copy.node3 = node3
            copy.node4 = node4
            copy.node5 = node5
            copy.mapLabel = mapLabel
            copy.feedLabel = feedLabel
            copy.chariotSection = chariotSection
            copy.activeCount = activeCount
            copy.rath1 = rath1
            copy.rath1Sub = rath1Sub
            copy.rath1Spec = rath1Spec
            copy.rath1Status = rath1Status
            copy.rath1Loc = rath1Loc
            copy.rath1Speed = rath1Speed
            copy.rath1Next = rath1Next
            copy.rath2 = rath2
            copy.rath2Sub = rath2Sub
            copy.rath2Spec = rath2Spec
            copy.rath2Status = rath2Status
            copy.rath2Loc = rath2Loc
            copy.rath2Speed = rath2Speed
            copy.rath2Next = rath2Next
            copy.rath3 = rath3
            copy.rath3Sub = rath3Sub
            copy.rath3Spec = rath3Spec
            copy.rath3Status = rath3Status
            copy.rath3Loc = rath3Loc
            copy.rath3Speed = rath3Speed
            copy.rath3Next = rath3Next
            copy.amenities = amenities
            copy.radius = radius
            copy.water = water
            copy.waterBody = waterBody
            copy.waterChip = waterChip
            copy.medical = medical
            copy.medicalBody = medicalBody
            copy.medicalChip = medicalChip
            copy.lost = lost
            copy.lostBody = lostBody
            copy.lostChip = lostChip
            copy.photoCaption = photoCaption
            copy.gpsNote = gpsNote
            copy.attendanceTitle = attendanceTitle
            copy.scannerActive = scannerActive
            copy.scanTab = scanTab
            copy.passTab = passTab
            copy.counterChip = counterChip
            copy.reticleHint = reticleHint
            copy.pilgrimName = pilgrimName
            copy.pilgrimReg = pilgrimReg
            copy.verified = verified
            copy.passHint = passHint
            copy.lastRegistered = lastRegistered
            copy.recorded = recorded
            copy.lastPlace = lastPlace
            copy.lastChip = lastChip
            copy.historyTitle = historyTitle
            copy.historyCount = historyCount
            copy.hist1 = hist1
            copy.hist1Sub = hist1Sub
            copy.hist2 = hist2
            copy.hist2Sub = hist2Sub
            copy.scanHelp = scanHelp
            copy.scanHelpBody = scanHelpBody
            copy.enterPin = enterPin
            copy.report = report
            copy.pinTitle = pinTitle
            copy.pinBody = pinBody
            copy.pinLabel = pinLabel
            copy.submitPin = submitPin
            copy.pinNote = pinNote
            copy.comingSoon = comingSoon
            copy.comingSoonBody = comingSoonBody
            copy.serverWaking = serverWaking
            copy.signInTitle = signInTitle
            copy.notificationsTitle = notificationsTitle
            copy.benefitsTitle = benefitsTitle
            copy.profileTitle = profileTitle
            copy.sosTitle = sosTitle
            copy.sosBody = sosBody
            copy.adminTitle = adminTitle
            copy.adminBody = adminBody
            copy.langEn = langEn
            copy.langHi = langHi
            copy.chooseLanguage = chooseLanguage
            copy.searchLanguages = searchLanguages
        return copy
    }

}

private val English: RathYatraCopy = RathYatraCopy().apply {
    portalChip = "GOVERNMENT OF INDIA"
    honourLine = "HONOURING INDIA'S FOOD HEROES"
    title = "Sahakar Bharati"
    subtitle = "Farmers & Fishermen Celebration"
    tagline = "A nationwide tribute to the extraordinary work of India's farmers and fishermen."
    statFarmers = "Farmers"
    statFarmersSub = "Across all states"
    statFishers = "Fishermen"
    statFishersSub = "Coastal & inland"
    statHelp = "Help Desk"
    statHelpSub = "Kisan & fisher care"
    statPass = "Participant Pass"
    statPassSub = "Event entry"
    getStarted = "Get Started"
    signIn = "Sign In"
    signInOfficial = "Already registered? Sign In"
    digitalIndia = "Digital India • National Farmers & Fishers Portal"
    govLine = "Government of India"
    ministryLine = "Ministry of Agriculture & Farmers Welfare • Department of Fisheries"
    nationalPortal = "NATIONAL PORTAL"
    nationwide = "All India"
    platformTitle = "Sahakar Bharati"
    platformAgency = "Government of India • Farmers & Fishermen Celebration"
    platformBody = "Join the national Rath Yatra that honours the extraordinary work of farmers and fishermen across India."
    editionLabel = "NATIONAL EDITION"
    festivalTitle = "Kisan–Matsyajivi Rath Yatra"
    festivalDate = "Nationwide celebration of farm and fishery excellence"
    editionRibbon = "Celebration theme"
    editionChip = "Farmers & Fishermen"
    servicesTitle = "Participant Services"
    servicesTrail = "National programme"
    serviceTrack = "Track the Rath"
    serviceTrackChip = "Not live yet"
    serviceTrackBody = "Follow the celebration raths honouring farmers and fishermen as they travel across participating cities."
    serviceAccess = "Join the Celebration"
    serviceAccessChip = "Register"
    serviceAccessBody = "Register as a farmer, fisherman, or guest and collect your participant pass for local events."
    serviceUpdates = "Stay Updated"
    serviceUpdatesChip = "Notices"
    serviceUpdatesBody = "Receive programme timings, route updates, and welfare scheme information from the central ministries."
    footerTitle = "Central Government Celebration"
    footerSub = "For farmers and fishermen across India"
    officialPortal = "NATIONAL PORTAL"
    createAccountTitle = "Create Your Farmer or Fisherman Account"
    createAccountBody = "Register to take part in the nationwide Rath Yatra celebrating the extraordinary work of India's farmers and fishermen."
    trustTitle = "GOVERNMENT OF INDIA PORTAL"
    trustBody = "Your details are used only to organise participation in this national celebration and connected welfare services."
    privileges = "PROGRAMME BENEFITS"
    featuresCount = "5 Core Features"
    priv1Title = "Celebration Schedule"
    priv1Body = "Timings for local rath events, felicitations, and cultural programmes in your district."
    priv2Title = "Rath Tracking"
    priv2Body = "See where the honour raths are moving and when they reach your city."
    priv3Title = "Event Check-in"
    priv3Body = "Check in at celebration grounds, exhibitions, and recognition ceremonies."
    priv4Title = "Farmer & Fisher Support"
    priv4Body = "Find linked central schemes for agriculture, inland fisheries, and marine livelihoods."
    priv5Title = "Help & Safety"
    priv5Body = "Reach event help desks, medical posts, and the national helpline from the home screen."
    kycTitle = "Quick registration"
    kycBody = "Keep your name and mobile number ready"
    kycTime = "~2 mins"
    createAccount = "Create Account"
    alreadyRegistered = "Already registered? Sign In"
    formTitle = "Personal Details"
    stepLabel = "STEP 2 OF 4: PERSONAL DETAILS (50%)"
    govtPortal = "National Portal"
    stepPersonal = "1 Phone"
    stepContact = "2 Name"
    stepProfile = "3 Address"
    stepSecurity = "4 PIN"
    formCardTitle = "Participant Registration"
    formCardBody = "Enter your name as it should appear on the celebration participant pass."
    legalName = "FULL LEGAL NAME"
    asInId = "As on ID"
    nameHint = "Shown on your participant pass."
    gender = "GENDER"
    mandatory = "Required"
    male = "Male"
    female = "Female"
    other = "Other"
    dob = "DATE OF BIRTH (DD/MM/YYYY)"
    ageMin = "Minimum age 18 for an independent pass."
    calculatedAge = "CALCULATED AGE"
    ageValue = "36 Years"
    eligible = "Eligible adult"
    eligibleNote = "Eligible to join the celebration independently."
    occupation = "OCCUPATION"
    occupationHint = "Used to recognise farmers and fishermen at the events."
    farmer = "Farmer"
    fisherman = "Fisherman"
    both = "Both"
    allied = "Allied"
    farmerDesc = "Agriculture & allied farming"
    fishermanDesc = "Marine or inland fishing"
    bothDesc = "Farm and fishery work"
    alliedDesc = "Allied rural livelihood"
    nextStepTitle = "Step 2: Contact details"
    upcoming = "Upcoming"
    nextStepBody = "Next you will add a mobile number so event updates can reach you."
    privacyTitle = "Privacy"
    privacyBody = "Registration is stored on the Sahakar Bharati server."
    continueNext = "Continue"
    saveLater = "You can finish registration later."
    successTitle = "Account Created Successfully"
    successBody = "Your participant account for the Farmers & Fishermen Rath Yatra is ready."
    docket = "Participant Docket"
    docketSub = "Celebration Pass"
    active = "Registered"
    participantId = "Participant ID"
    participantIdValue = "—"
    occupationLabel = "Occupation"
    occupationValue = "—"
    regionLabel = "Region"
    regionValue = "—"
    passStatus = "Pass status"
    passReady = "Not issued yet"
    barcodeTitle = "Entry pass not issued yet"
    barcodeBody = "Your registration is saved. A QR entry pass will appear in the app once event check-in opens."
    goHome = "Go to My Home"
    viewPass = "View Celebration Pass"
    successNote = "Sign in with your mobile number and PIN. Help desk contacts are on the Home screen."
    successFooter = "Government of India • Farmers & Fishermen Celebration"
    topBarKicker = "GOVERNMENT OF INDIA"
    homeTitle = "Kisan Home"
    greeting = "Jai Kisan"
    roleChip = "Participant"
    homeId = "ID: —"
    homeZone = "—"
    ePass = "E-Pass"
    sessionLine = "Participant session ready"
    interfaceMode = "Interface Mode"
    citizenView = "Farmer / Fisherman View"
    adminView = "Admin View"
    liveProcession = "RATH YATRA"
    locationChip = "National Route"
    heroKicker = "Honouring food heroes"
    heroTitle = "Kisan–Matsyajivi Rath Yatra"
    heroSub = "Raths celebrating farmers and fishermen on the national route"
    metricCrowd = "Participants"
    metricCrowdVal = "Unavailable"
    metricCrowdCap = "No live count"
    metricWeather = "Weather"
    metricWeatherVal = "Unavailable"
    metricWeatherCap = "No weather feed"
    metricTravel = "Covered"
    metricTravelVal = "Unavailable"
    metricTravelCap = "No route feed"
    upcomingRitual = "Next programme"
    upcomingRitualName = "Programme times are not connected yet."
    upcomingRitualTime = "—"
    liveRouteCta = "Rath Route & National Map"
    coreServices = "Core Services"
    quickAccess = "4 Quick Access"
    tileTrack = "Track Raths"
    tileTrackBody = "Follow honour raths on the national map"
    tileTrackChip = "Unavailable"
    tileQr = "Event Check-in"
    tileQrBody = "Check in at celebration grounds"
    tileQrChip = "Pass"
    tileBenefits = "Schemes & Support"
    tileBenefitsBody = "Agriculture and fisheries schemes"
    tileBenefitsChip = "Aid"
    tileVolunteer = "Volunteer"
    tileVolunteerBody = "Help at water, crowd, and medical posts"
    tileVolunteerChip = "Join"
    trajectory = "Rath Trajectory"
    trajectorySub = "Village start ➔ Coastal close"
    startPoint = "Farm cluster"
    currentPoint = "Unavailable"
    endPoint = "Harbour ground"
    advisories = "Programme Advisories"
    viewAll = "View All"
    adv1Source = "Ministry of Agriculture"
    adv1Time = "—"
    adv1Title = "No live advisories"
    adv1Body = "This section will be available in a later update."
    adv2Source = "Department of Fisheries"
    adv2Time = "—"
    adv2Title = "No live advisories"
    adv2Body = "This section will be available in a later update."
    streamTitle = "Celebration Visuals"
    streamLive = "Archive"
    stream1 = "Farm fields"
    stream2 = "Harbour gathering"
    emergencyTitle = "Help Desk"
    emergencyToll = "Toll-free calls"
    emergencyBody = "For medical help, lost companions, or event guidance, contact the nearest help desk."
    policeSos = "Emergency (112)"
    helpline = "Kisan Call Centre"
    tabHome = "Home"
    tabYatra = "Yatra"
    tabAttendance = "Check-in"
    tabBenefits = "Benefits"
    tabProfile = "Profile"
    trackTitle = "Rath Track"
    telemetryLive = "Route unavailable"
    lastSynced = ""
    traversal = "National celebration route"
    distanceTo = "Live tracking is not connected"
    estArrival = "Est. arrival"
    arrivalTime = "—"
    corridor = "Honour Rath Corridor (42 km)"
    corridorChip = "All India"
    node1 = "Farm start"
    node2 = "Mandi"
    node3 = "Town"
    node4 = "Coast road"
    node5 = "Harbour"
    mapLabel = "Route overview"
    feedLabel = "Movement feed"
    chariotSection = "Honour Raths"
    activeCount = "Unavailable"
    rath1 = "Anna Rath"
    rath1Sub = "Farmers of India"
    rath1Spec = "Saffron canopy • Grain honour rath"
    rath1Status = "Unavailable"
    rath1Loc = "No live position"
    rath1Speed = "—"
    rath1Next = "Live rath telemetry is not connected."
    rath2 = "Neel Rath"
    rath2Sub = "Fishermen of India"
    rath2Spec = "Blue-green canopy • Fisher honour rath"
    rath2Status = "Unavailable"
    rath2Loc = "No live position"
    rath2Speed = "—"
    rath2Next = "Live rath telemetry is not connected."
    rath3 = "Sanyukt Rath"
    rath3Sub = "Shared livelihood"
    rath3Spec = "Gold canopy • Joint honour rath"
    rath3Status = "Unavailable"
    rath3Loc = "No live position"
    rath3Speed = "—"
    rath3Next = "Live rath telemetry is not connected."
    amenities = "Support along the route"
    radius = "Unavailable"
    water = "Drinking water"
    waterBody = "Water-point locations are not connected yet."
    waterChip = "Unavailable"
    medical = "Medical tent"
    medicalBody = "Medical-tent locations are not connected yet."
    medicalChip = "Unavailable"
    lost = "Lost & found"
    lostBody = "Lost-and-found locations are not connected yet."
    lostChip = "Unavailable"
    photoCaption = "Celebration gathering • National route"
    gpsNote = "Live GPS tracking is not connected."
    attendanceTitle = "Event Check-in"
    scannerActive = "Scanner unavailable"
    scanTab = "Scan Counter QR"
    passTab = "Show My Pass"
    counterChip = "QR check-in is not connected"
    reticleHint = "A live camera scanner is not available in this release."
    pilgrimName = "Participant"
    pilgrimReg = "Participant ID unavailable"
    verified = "Registered"
    passHint = "A live QR pass is not issued until attendance is connected."
    lastRegistered = "No check-in records"
    recorded = "Unavailable"
    lastPlace = "Attendance history is not connected."
    lastChip = "Unavailable"
    historyTitle = "Check-in history"
    historyCount = "0 records"
    hist1 = "No attendance records"
    hist1Sub = "QR attendance is not operational."
    hist2 = "No attendance records"
    hist2Sub = "QR attendance is not operational."
    scanHelp = "Trouble scanning?"
    scanHelpBody = "If sunlight or network delays the scan, enter the 6-digit counter code from the placard."
    enterPin = "Enter 6-digit PIN"
    report = "Report"
    pinTitle = "Manual check-in"
    pinBody = "Enter the 6-digit number printed under the counter QR."
    pinLabel = "6-digit PIN"
    submitPin = "Submit PIN"
    pinNote = "PIN entry records your presence at this counter."
    comingSoon = "Opening soon"
    comingSoonBody = "This section will be available in a later update."
    serverWaking = "The server is starting up. This can take up to a minute, please wait…"
    signInTitle = "Sign In"
    notificationsTitle = "Notifications"
    benefitsTitle = "Benefits"
    profileTitle = "Profile"
    sosTitle = "Help"
    sosBody = "Use the nearest event help desk or call 112 in an emergency."
    adminTitle = "Admin view"
    adminBody = "The organiser console is not part of this participant app."
    langEn = "EN"
    langHi = "हिंदी"
    chooseLanguage = "Choose language"
    searchLanguages = "Search languages"
}

private val Hindi: RathYatraCopy = English.cloneCopy().apply {
    portalChip = "भारत सरकार"
    honourLine = "भारत के अन्नदाताओं का सम्मान"
    title = "सहकार भारती"
    subtitle = "किसान एवं मत्स्यजीवी उत्सव"
    tagline = "भारत के किसान और मत्स्यजीवियों के असाधारण कार्य का राष्ट्रव्यापी सम्मान।"
    statFarmers = "किसान"
    statFarmersSub = "सभी राज्यों से"
    statFishers = "मत्स्यजीवी"
    statFishersSub = "तटीय और अंतर्देशीय"
    statHelp = "सहायता केंद्र"
    statHelpSub = "किसान व मत्स्य सेवा"
    statPass = "प्रतिभागी पास"
    statPassSub = "कार्यक्रम प्रवेश"
    getStarted = "शुरू करें"
    signIn = "साइन इन"
    signInOfficial = "पहले से पंजीकृत हैं? साइन इन"
    digitalIndia = "डिजिटल इंडिया • राष्ट्रीय किसान-मत्स्य पोर्टल"
    govLine = "भारत सरकार"
    ministryLine = "कृषि एवं किसान कल्याण मंत्रालय • मात्स्यिकी विभाग"
    nationalPortal = "राष्ट्रीय पोर्टल"
    nationwide = "सम्पूर्ण भारत"
    platformTitle = "सहकार भारती"
    platformAgency = "भारत सरकार • किसान एवं मत्स्यजीवी उत्सव"
    platformBody = "उस राष्ट्रीय रथ यात्रा में शामिल हों जो भारत के किसान और मत्स्यजीवियों के असाधारण कार्य का सम्मान करती है।"
    editionLabel = "राष्ट्रीय संस्करण"
    festivalTitle = "किसान–मत्स्यजीवी रथ यात्रा"
    festivalDate = "कृषि और मात्स्यिकी के उत्कृष्ट कार्य का राष्ट्रव्यापी उत्सव"
    editionRibbon = "उत्सव विषय"
    editionChip = "किसान एवं मत्स्यजीवी"
    servicesTitle = "प्रतिभागी सेवाएँ"
    servicesTrail = "राष्ट्रीय कार्यक्रम"
    serviceTrack = "रथ ट्रैक करें"
    serviceTrackChip = "अभी लाइव नहीं"
    serviceTrackBody = "किसान और मत्स्यजीवियों के सम्मान में निकल रहे रथों को अपने शहर तक देखें।"
    serviceAccess = "उत्सव में शामिल हों"
    serviceAccessChip = "पंजीकरण"
    serviceAccessBody = "किसान, मत्स्यजीवी या अतिथि के रूप में पंजीकरण करें और अपना प्रतिभागी पास लें।"
    serviceUpdates = "अपडेट पाएँ"
    serviceUpdatesChip = "सूचनाएँ"
    serviceUpdatesBody = "केन्द्रीय मंत्रालयों से कार्यक्रम समय, मार्ग और कल्याण योजनाओं की जानकारी पाएँ।"
    footerTitle = "केन्द्र सरकार का उत्सव"
    footerSub = "भारत भर के किसान और मत्स्यजीवियों के लिए"
    officialPortal = "राष्ट्रीय पोर्टल"
    createAccountTitle = "अपना किसान या मत्स्यजीवी खाता बनाएँ"
    createAccountBody = "राष्ट्रव्यापी रथ यात्रा में भाग लेने के लिए पंजीकरण करें, जो किसानों और मत्स्यजीवियों के असाधारण कार्य का उत्सव है।"
    trustTitle = "भारत सरकार पोर्टल"
    trustBody = "आपका विवरण इस राष्ट्रीय उत्सव और जुड़ी कल्याण सेवाओं के आयोजन के लिए ही उपयोग होगा।"
    privileges = "कार्यक्रम लाभ"
    featuresCount = "5 मुख्य विशेषताएँ"
    priv1Title = "उत्सव समय-सारणी"
    priv1Body = "आपके जिले में रथ, सम्मान समारोह और सांस्कृतिक कार्यक्रमों का समय।"
    priv2Title = "रथ ट्रैकिंग"
    priv2Body = "सम्मान रथ कहाँ चल रहे हैं और आपके शहर कब पहुँचेंगे, यह देखें।"
    priv3Title = "कार्यक्रम चेक-इन"
    priv3Body = "उत्सव मैदान, प्रदर्शनी और सम्मान समारोह में उपस्थिति दर्ज करें।"
    priv4Title = "किसान व मत्स्य सहायता"
    priv4Body = "कृषि, अंतर्देशीय मात्स्यिकी और समुद्री आजीविका की केन्द्रीय योजनाएँ देखें।"
    priv5Title = "सहायता और सुरक्षा"
    priv5Body = "होम स्क्रीन से सहायता केंद्र, चिकित्सा चौकी और राष्ट्रीय हेल्पलाइन तक पहुँचें।"
    kycTitle = "त्वरित पंजीकरण"
    kycBody = "अपना नाम और मोबाइल नंबर तैयार रखें"
    kycTime = "~2 मिनट"
    createAccount = "खाता बनाएँ"
    alreadyRegistered = "पहले से पंजीकृत हैं? साइन इन"
    formTitle = "व्यक्तिगत विवरण"
    stepLabel = "चरण 2 / 4: व्यक्तिगत विवरण (50%)"
    govtPortal = "राष्ट्रीय पोर्टल"
    stepPersonal = "1 फ़ोन"
    stepContact = "2 नाम"
    stepProfile = "3 पता"
    stepSecurity = "4 पिन"
    formCardTitle = "प्रतिभागी पंजीकरण"
    formCardBody = "वह नाम लिखें जो उत्सव प्रतिभागी पास पर दिखना चाहिए।"
    legalName = "पूरा कानूनी नाम"
    asInId = "पहचान पत्र के अनुसार"
    nameHint = "आपके प्रतिभागी पास पर यही नाम दिखेगा।"
    gender = "लिंग"
    mandatory = "आवश्यक"
    male = "पुरुष"
    female = "महिला"
    other = "अन्य"
    dob = "जन्म तिथि (दिन/माह/वर्ष)"
    ageMin = "स्वतंत्र पास के लिए न्यूनतम आयु 18 वर्ष।"
    calculatedAge = "आयु"
    ageValue = "36 वर्ष"
    eligible = "पात्र वयस्क"
    eligibleNote = "उत्सव में स्वतंत्र रूप से शामिल होने के लिए पात्र।"
    occupation = "व्यवसाय"
    occupationHint = "उत्सव में किसानों और मत्स्यजीवियों की पहचान के लिए।"
    farmer = "किसान"
    fisherman = "मत्स्यजीवी"
    both = "दोनों"
    allied = "संबद्ध"
    farmerDesc = "कृषि एवं संबद्ध खेती"
    fishermanDesc = "समुद्री या अंतर्देशीय मत्स्यन"
    bothDesc = "कृषि और मात्स्यिकी दोनों"
    alliedDesc = "संबद्ध ग्रामीण आजीविका"
    nextStepTitle = "चरण 2: संपर्क विवरण"
    upcoming = "आगामी"
    nextStepBody = "अगले चरण में मोबाइल नंबर जोड़ें, ताकि कार्यक्रम अपडेट मिल सकें।"
    privacyTitle = "गोपनीयता"
    privacyBody = "पंजीकरण सहकार भारती सर्वर पर संग्रहीत होता है।"
    continueNext = "आगे बढ़ें"
    saveLater = "आप पंजीकरण बाद में पूरा कर सकते हैं।"
    successTitle = "खाता सफलतापूर्वक बना"
    successBody = "किसान एवं मत्स्यजीवी रथ यात्रा के लिए आपका प्रतिभागी खाता तैयार है।"
    docket = "प्रतिभागी विवरण"
    docketSub = "उत्सव पास"
    active = "पंजीकृत"
    participantId = "प्रतिभागी आईडी"
    participantIdValue = "—"
    occupationLabel = "व्यवसाय"
    occupationValue = "—"
    regionLabel = "क्षेत्र"
    regionValue = "—"
    passStatus = "पास स्थिति"
    passReady = "अभी जारी नहीं"
    barcodeTitle = "प्रवेश पास अभी जारी नहीं"
    barcodeBody = "आपका पंजीकरण सहेजा गया है। कार्यक्रम चेक-इन शुरू होने पर QR प्रवेश पास ऐप में दिखेगा।"
    goHome = "होम पर जाएँ"
    viewPass = "उत्सव पास देखें"
    successNote = "अपने मोबाइल नंबर और पिन से साइन इन करें। सहायता केंद्र के संपर्क होम स्क्रीन पर हैं।"
    successFooter = "भारत सरकार • किसान एवं मत्स्यजीवी उत्सव"
    topBarKicker = "भारत सरकार • सम्पूर्ण भारत"
    homeTitle = "किसान होम"
    greeting = "जय किसान"
    roleChip = "प्रतिभागी"
    homeId = "आईडी: —"
    homeZone = "—"
    ePass = "ई-पास"
    sessionLine = "प्रतिभागी सत्र तैयार"
    interfaceMode = "इंटरफ़ेस मोड"
    citizenView = "किसान / मत्स्यजीवी दृश्य"
    adminView = "एडमिन दृश्य"
    liveProcession = "रथ यात्रा"
    locationChip = "राष्ट्रीय मार्ग"
    heroKicker = "अन्नदाताओं का सम्मान"
    heroTitle = "किसान–मत्स्यजीवी रथ यात्रा"
    heroSub = "किसान और मत्स्यजीवियों के सम्मान में राष्ट्रीय मार्ग पर रथ"
    metricCrowd = "प्रतिभागी"
    metricCrowdVal = "अनुपलब्ध"
    metricCrowdCap = "लाइव गणना नहीं"
    metricWeather = "मौसम"
    metricWeatherVal = "अनुपलब्ध"
    metricWeatherCap = "मौसम फीड नहीं"
    metricTravel = "दूरी"
    metricTravelVal = "अनुपलब्ध"
    metricTravelCap = "मार्ग फीड नहीं"
    upcomingRitual = "अगला कार्यक्रम"
    upcomingRitualName = "कार्यक्रम समय अभी जुड़ा नहीं है।"
    upcomingRitualTime = "—"
    liveRouteCta = "रथ मार्ग और राष्ट्रीय नक्शा"
    coreServices = "मुख्य सेवाएँ"
    quickAccess = "4 त्वरित पहुँच"
    tileTrack = "रथ ट्रैक करें"
    tileTrackBody = "राष्ट्रीय मानचित्र पर सम्मान रथ देखें"
    tileTrackChip = "अनुपलब्ध"
    tileQr = "कार्यक्रम चेक-इन"
    tileQrBody = "उत्सव मैदान पर उपस्थिति दर्ज करें"
    tileQrChip = "पास"
    tileBenefits = "योजनाएँ व सहायता"
    tileBenefitsBody = "कृषि और मात्स्यिकी योजनाएँ"
    tileBenefitsChip = "सहायता"
    tileVolunteer = "स्वयंसेवक"
    tileVolunteerBody = "जल, भीड़ और चिकित्सा चौकियों पर मदद करें"
    tileVolunteerChip = "जुड़ें"
    trajectory = "रथ पथ"
    trajectorySub = "गाँव प्रारंभ ➔ बंदरगाह समापन"
    startPoint = "कृषि क्षेत्र"
    currentPoint = "अनुपलब्ध"
    endPoint = "बंदरगाह मैदान"
    advisories = "कार्यक्रम सूचनाएँ"
    viewAll = "सभी देखें"
    adv1Source = "कृषि मंत्रालय"
    adv1Time = "—"
    adv1Title = "कोई लाइव सूचना नहीं"
    adv1Body = "यह खंड अगले अद्यतन में उपलब्ध होगा।"
    adv2Source = "मात्स्यिकी विभाग"
    adv2Time = "—"
    adv2Title = "कोई लाइव सूचना नहीं"
    adv2Body = "यह खंड अगले अद्यतन में उपलब्ध होगा।"
    streamTitle = "उत्सव झलकियाँ"
    streamLive = "संग्रह"
    stream1 = "खेत"
    stream2 = "बंदरगाह सभा"
    emergencyTitle = "सहायता केंद्र"
    emergencyToll = "टोल-फ्री कॉल"
    emergencyBody = "चिकित्सा सहायता, बिछड़े साथी या मार्गदर्शन के लिए निकटतम सहायता केंद्र से संपर्क करें।"
    policeSos = "आपातकाल (112)"
    helpline = "किसान कॉल सेंटर"
    tabHome = "होम"
    tabYatra = "यात्रा"
    tabAttendance = "चेक-इन"
    tabBenefits = "लाभ"
    tabProfile = "प्रोफ़ाइल"
    trackTitle = "रथ ट्रैक"
    telemetryLive = "मार्ग अनुपलब्ध"
    lastSynced = ""
    traversal = "राष्ट्रीय उत्सव मार्ग"
    distanceTo = "लाइव ट्रैकिंग जुड़ी नहीं है"
    estArrival = "अनुमानित पहुँच"
    arrivalTime = "—"
    corridor = "सम्मान रथ गलियारा (42 किमी)"
    corridorChip = "सम्पूर्ण भारत"
    node1 = "कृषि प्रारंभ"
    node2 = "मंडी"
    node3 = "नगर"
    node4 = "तटीय मार्ग"
    node5 = "बंदरगाह"
    mapLabel = "मार्ग अवलोकन"
    feedLabel = "गति फीड"
    chariotSection = "सम्मान रथ"
    activeCount = "अनुपलब्ध"
    rath1 = "अन्न रथ"
    rath1Sub = "भारत के किसान"
    rath1Spec = "केसरिया छत्र • अनाज सम्मान रथ"
    rath1Status = "अनुपलब्ध"
    rath1Loc = "कोई लाइव स्थिति नहीं"
    rath1Speed = "—"
    rath1Next = "लाइव रथ टेलीमेट्री जुड़ी नहीं है।"
    rath2 = "नील रथ"
    rath2Sub = "भारत के मत्स्यजीवी"
    rath2Spec = "नीला-हरा छत्र • मत्स्य सम्मान रथ"
    rath2Status = "अनुपलब्ध"
    rath2Loc = "कोई लाइव स्थिति नहीं"
    rath2Speed = "—"
    rath2Next = "लाइव रथ टेलीमेट्री जुड़ी नहीं है।"
    rath3 = "संयुक्त रथ"
    rath3Sub = "साझा आजीविका"
    rath3Spec = "स्वर्ण छत्र • संयुक्त सम्मान रथ"
    rath3Status = "अनुपलब्ध"
    rath3Loc = "कोई लाइव स्थिति नहीं"
    rath3Speed = "—"
    rath3Next = "लाइव रथ टेलीमेट्री जुड़ी नहीं है।"
    amenities = "मार्ग पर सहायता"
    radius = "अनुपलब्ध"
    water = "पेयजल"
    waterBody = "जल बिंदु अभी जुड़े नहीं हैं।"
    waterChip = "अनुपलब्ध"
    medical = "चिकित्सा तंबू"
    medicalBody = "चिकित्सा तंबू स्थान अभी जुड़े नहीं हैं।"
    medicalChip = "अनुपलब्ध"
    lost = "खोया-पाया"
    lostBody = "खोया-पाया स्थान अभी जुड़े नहीं हैं।"
    lostChip = "अनुपलब्ध"
    photoCaption = "उत्सव सभा • राष्ट्रीय मार्ग"
    gpsNote = "लाइव GPS ट्रैकिंग जुड़ी नहीं है।"
    attendanceTitle = "कार्यक्रम चेक-इन"
    scannerActive = "स्कैनर अनुपलब्ध"
    scanTab = "काउंटर QR स्कैन करें"
    passTab = "मेरा पास दिखाएँ"
    counterChip = "QR चेक-इन जुड़ा नहीं है"
    reticleHint = "इस संस्करण में लाइव कैमरा स्कैनर उपलब्ध नहीं है।"
    pilgrimName = "प्रतिभागी"
    pilgrimReg = "प्रतिभागी आईडी अनुपलब्ध"
    verified = "पंजीकृत"
    passHint = "उपस्थिति जुड़ने तक लाइव QR पास जारी नहीं होता।"
    lastRegistered = "कोई चेक-इन रिकॉर्ड नहीं"
    recorded = "अनुपलब्ध"
    lastPlace = "उपस्थिति इतिहास जुड़ा नहीं है।"
    lastChip = "अनुपलब्ध"
    historyTitle = "चेक-इन इतिहास"
    historyCount = "0 रिकॉर्ड"
    hist1 = "कोई उपस्थिति रिकॉर्ड नहीं"
    hist1Sub = "QR उपस्थिति चालू नहीं है।"
    hist2 = "कोई उपस्थिति रिकॉर्ड नहीं"
    hist2Sub = "QR उपस्थिति चालू नहीं है।"
    scanHelp = "स्कैन में समस्या?"
    scanHelpBody = "धूप या नेटवर्क से स्कैन रुकने पर तख्ती पर लिखा 6 अंकों का कोड दर्ज करें।"
    enterPin = "6 अंकों का पिन दर्ज करें"
    report = "रिपोर्ट"
    pinTitle = "मैन्युअल चेक-इन"
    pinBody = "काउंटर QR के नीचे छपा 6 अंकों का नंबर दर्ज करें।"
    pinLabel = "6 अंकों का पिन"
    submitPin = "पिन जमा करें"
    pinNote = "पिन प्रविष्टि इस काउंटर पर आपकी उपस्थिति दर्ज करती है।"
    comingSoon = "जल्द उपलब्ध"
    comingSoonBody = "यह खंड अगले अद्यतन में उपलब्ध होगा।"
    serverWaking = "सर्वर शुरू हो रहा है। इसमें एक मिनट तक लग सकता है, कृपया प्रतीक्षा करें…"
    signInTitle = "साइन इन"
    notificationsTitle = "सूचनाएँ"
    benefitsTitle = "लाभ"
    profileTitle = "प्रोफ़ाइल"
    sosTitle = "सहायता"
    sosBody = "निकटतम सहायता केंद्र से संपर्क करें, या आपात स्थिति में 112 पर कॉल करें।"
    adminTitle = "एडमिन दृश्य"
    adminBody = "आयोजक कंसोल इस प्रतिभागी ऐप का हिस्सा नहीं है।"
    langEn = "EN"
    langHi = "हिंदी"
    chooseLanguage = "भाषा चुनें"
    searchLanguages = "भाषा खोजें"
}

val LocalCopy = staticCompositionLocalOf { English }
val LocalLanguage = staticCompositionLocalOf { AppLanguage.EN }
val LocalSetLanguage = staticCompositionLocalOf<(AppLanguage) -> Unit> { {} }

val languagePickerOrder: List<AppLanguage> = listOf(
    AppLanguage.EN,
    AppLanguage.HI,
    AppLanguage.BN,
    AppLanguage.MR,
    AppLanguage.TE,
    AppLanguage.TA,
    AppLanguage.GU,
    AppLanguage.UR,
    AppLanguage.KN,
    AppLanguage.OR,
    AppLanguage.ML,
    AppLanguage.PA,
    AppLanguage.AS,
    AppLanguage.MAI,
    AppLanguage.SAT,
    AppLanguage.KS,
    AppLanguage.NE,
    AppLanguage.KOK,
    AppLanguage.SD,
    AppLanguage.DOI,
    AppLanguage.MNI,
    AppLanguage.BRX,
    AppLanguage.SA,
    AppLanguage.BHO,
    AppLanguage.RAJ,
    AppLanguage.TCY,
)

fun copyFor(language: AppLanguage): RathYatraCopy {
    val base = if (AppLanguage.usesHindiCopy(language)) Hindi else English
    return guestOverlay(language, base)
}
