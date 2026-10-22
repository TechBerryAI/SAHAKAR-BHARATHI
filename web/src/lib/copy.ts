import type { LangCode } from './languages.ts'
import { TRANSLATIONS } from './translations.ts'

export type AppCopy = {
  govChip: string
  honour: string
  subtitle: string
  tagline: string
  registerNow: string
  alreadyRegistered: string
  verifyMobile: string
  mobileTitle: string
  mobileBody: string
  mobileLabel: string
  mobileHint: string
  sendOtp: string
  verifyOtp: string
  yourOtp: string
  otpLabel: string
  otpHint: string
  resendOtp: string
  mobileRequired: string
  otpRequired: string
  otpWrong: string
  otpSmsHint: string
  otpSent: string
  otpSendFailed: string
  otpSendFirst: string
  otpVerified: string
  otpVerifyFailed: string
  otpVerifyRequired: string
  pleaseWait: string
  signIn: string
  accessTitle: string
  accessBody: string
  email: string
  emailOptional: string
  emailHint: string
  password: string
  createAccount: string
  enterEmailPassword: string
  loginIdLabel: string
  loginIdHint: string
  pinLabel: string
  enterLoginPin: string
  signingIn: string
  register: string
  continue: string
  createAccountBtn: string
  registering: string
  stepPersonal: string
  stepContact: string
  stepAddress: string
  stepPassword: string
  fullName: string
  gender: string
  male: string
  female: string
  other: string
  dob: string
  dobHint: string
  occupation: string
  farmer: string
  fisherman: string
  both: string
  allied: string
  mobile: string
  verifiedOtp: string
  address: string
  locality: string
  district: string
  state: string
  pincode: string
  createPassword: string
  confirmPassword: string
  passwordHint: string
  showPin: string
  hidePin: string
  fullNameRequired: string
  minAge: string
  validEmail: string
  pinDigits: string
  passwordLength: string
  passwordMatch: string
  emailSignInHint: string
  registrationFailed: string
  signInFailed: string
  stepOf: string
  chooseLanguage: string
  searchLanguages: string
  noLanguages: string
  back: string
  home: string
  profile: string
  registered: string
  accountReady: string
  keepLoginId: string
  keepLoginIdBody: string
  loginId: string
  goHome: string
  jaiKisan: string
  registeredParticipant: string
  account: string
  yourProfile: string
  profileStatus: string
  mobileOnFile: string
  mobileOnFileBody: string
  loadFailed: string
  participant: string
  editDetails: string
  updateProfile: string
  saveChanges: string
  saving: string
  saved: string
  updateFailed: string
  logOut: string
  logOutConfirm: string
  logOutBody: string
  cancel: string
  registrationTitle: string
  formTitle: string
  contactTitle: string
  profileTitle: string
  securityTitle: string
  stepPersonalTrack: string
  stepContactTrack: string
  stepProfileTrack: string
  stepSecurityTrack: string
  formProgress: string
  govtPortal: string
  legalName: string
  nameAadhaarHint: string
  mandatory: string
  dobFull: string
  dobDay: string
  dobMonth: string
  dobYear: string
  ageMin: string
  occupationHint: string
  occupationSelect: string
  privacyTitle: string
  privacyBody: string
  sos: string
  sosTitle: string
  sosBody: string
  genderRequired: string
  dobRequired: string
  occupationRequired: string
  contactCardTitle: string
  contactCardBody: string
  contactVerifiedHint: string
  contactEmailHint: string
  addressCardTitle: string
  addressCardBody: string
  houseLabel: string
  houseHint: string
  localityLabel: string
  districtLabel: string
  stateLabel: string
  pincodeLabel: string
  useMyLocation: string
  useMyLocationHint: string
  locatingAddress: string
  locationFilled: string
  locationDenied: string
  locationUnavailable: string
  addressRequired: string
  securityCardTitle: string
  securityCardBody: string
  securityPinLabel: string
  securityPinHint: string
  securityConfirmPin: string
}

const HI: Partial<AppCopy> = {
  govChip: 'भारत सरकार',
  honour: 'भारत के अन्न नायकों का सम्मान',
  subtitle: 'किसान और मछुआरा उत्सव',
  tagline: 'भारत के किसानों और मछुआरों के असाधारण कार्य को राष्ट्रव्यापी श्रद्धांजलि।',
  registerNow: 'अभी पंजीकरण करें',
  alreadyRegistered: 'पहले से पंजीकृत हैं? साइन इन',
  verifyMobile: 'मोबाइल सत्यापित करें',
  mobileTitle: 'मोबाइल नंबर',
  mobileBody: 'पंजीकरण से पहले अपना मोबाइल नंबर लिखें। ओटीपी भेजें, सत्यापित करें, फिर व्यक्तिगत विवरण खुलेगा।',
  mobileLabel: 'मोबाइल नंबर',
  mobileHint: '10 अंकों का भारतीय मोबाइल नंबर।',
  sendOtp: 'ओटीपी भेजें',
  verifyOtp: 'ओटीपी सत्यापित करें',
  yourOtp: 'आपका ओटीपी है',
  otpLabel: 'ओटीपी',
  otpHint: 'इस पृष्ठ पर दिखाया गया 6 अंकों का कोड लिखें।',
  resendOtp: 'ओटीपी फिर भेजें',
  mobileRequired: 'मान्य 10 अंकों का भारतीय मोबाइल नंबर लिखें।',
  otpRequired: '6 अंकों का ओटीपी लिखें।',
  otpWrong: 'यह ओटीपी गलत है।',
  otpSmsHint: 'एसएमएस से मिला 6 अंकों का कोड।',
  otpSent: 'ओटीपी भेजा गया। यह 5 मिनट में समाप्त होगा।',
  otpSendFailed: 'ओटीपी नहीं भेजा जा सका।',
  otpSendFirst: 'पहले ओटीपी भेजें।',
  otpVerified: 'मोबाइल नंबर सत्यापित हो गया।',
  otpVerifyFailed: 'ओटीपी सत्यापित नहीं हो सका।',
  otpVerifyRequired: 'इस मोबाइल नंबर पर भेजा गया ओटीपी सत्यापित करें।',
  pleaseWait: 'कृपया प्रतीक्षा करें…',
  signIn: 'साइन इन',
  accessTitle: 'पंजीकृत प्रतिभागी प्रवेश',
  accessBody: 'ऐप का उपयोग करने से पहले पंजीकरण और साइन इन आवश्यक है।',
  email: 'ईमेल',
  emailOptional: 'ईमेल (वैकल्पिक)',
  emailHint: 'पंजीकरण वाला ईमेल उपयोग करें।',
  password: 'पासवर्ड',
  createAccount: 'खाता बनाएँ',
  enterEmailPassword: 'अपना ईमेल और पासवर्ड लिखें।',
  loginIdLabel: 'लॉगिन आईडी / मोबाइल',
  loginIdHint: 'पंजीकरण के बाद मिला लॉगिन आईडी या 10 अंकों का मोबाइल।',
  pinLabel: 'पिन',
  enterLoginPin: 'लॉगिन आईडी और पिन लिखें।',
  signingIn: 'साइन इन हो रहा है…',
  register: 'पंजीकरण',
  continue: 'आगे बढ़ें',
  createAccountBtn: 'खाता बनाएँ',
  registering: 'पंजीकरण हो रहा है…',
  stepPersonal: 'व्यक्तिगत',
  stepContact: 'संपर्क',
  stepAddress: 'पता',
  stepPassword: 'पिन',
  fullName: 'पूरा नाम',
  gender: 'लिंग',
  male: 'पुरुष',
  female: 'महिला',
  other: 'अन्य',
  dob: 'जन्म तिथि',
  dobHint: 'वैकल्पिक। आयु 18 वर्ष या अधिक होनी चाहिए।',
  occupation: 'व्यवसाय',
  farmer: 'किसान',
  fisherman: 'मछुआरा',
  both: 'दोनों',
  allied: 'संबद्ध',
  mobile: 'मोबाइल',
  verifiedOtp: 'ओटीपी से सत्यापित।',
  address: 'पता',
  locality: 'इलाका',
  district: 'ज़िला',
  state: 'राज्य',
  pincode: 'पिन कोड',
  createPassword: 'पिन सेट करें',
  confirmPassword: 'पिन की पुष्टि',
  passwordHint: '4 से 6 अंक। हर साइन इन पर यही पिन लगेगा।',
  showPin: 'पिन दिखाएँ',
  hidePin: 'पिन छिपाएँ',
  fullNameRequired: 'पूरा नाम आवश्यक है।',
  minAge: 'न्यूनतम आयु 18 वर्ष है।',
  validEmail: 'मान्य ईमेल पता लिखें।',
  pinDigits: 'पिन कोड 6 अंकों का होना चाहिए।',
  passwordLength: 'कम से कम 4 अंकों का पिन बनाएँ।',
  passwordMatch: 'पिन मेल नहीं खाते।',
  emailSignInHint: 'वैकल्पिक। साइन इन मोबाइल नंबर से होगा।',
  registrationFailed: 'पंजीकरण नहीं हो सका।',
  signInFailed: 'साइन इन नहीं हो सका।',
  stepOf: 'चरण {current} / {total}',
  chooseLanguage: 'भाषा चुनें',
  searchLanguages: 'भाषा खोजें',
  noLanguages: 'कोई भाषा नहीं मिली।',
  back: 'वापस',
  home: 'होम',
  profile: 'प्रोफ़ाइल',
  registered: 'पंजीकृत',
  accountReady: 'खाता तैयार',
  keepLoginId: 'यह लॉगिन आईडी रखें',
  keepLoginIdBody: 'यह आईडी संभालकर रखें। साइन इन लॉगिन आईडी या मोबाइल और पिन से होगा।',
  loginId: 'लॉगिन आईडी',
  goHome: 'होम पर जाएँ',
  jaiKisan: 'जय किसान',
  registeredParticipant: 'पंजीकृत प्रतिभागी',
  account: 'खाता',
  yourProfile: 'आपकी रथ यात्रा प्रोफ़ाइल',
  profileStatus: 'स्थिति: {status}। साइन इन लॉगिन आईडी या मोबाइल और पिन से करें। विवरण प्रोफ़ाइल से बदलें।',
  mobileOnFile: 'फ़ाइल में मोबाइल',
  mobileOnFileBody: 'यह नंबर आपकी रथ यात्रा प्रोफ़ाइल पर दर्ज है।',
  loadFailed: 'प्रोफ़ाइल नहीं खुल सकी।',
  participant: 'प्रतिभागी',
  editDetails: 'विवरण बदलें',
  updateProfile: 'प्रोफ़ाइल अपडेट करें',
  saveChanges: 'बदलाव सहेजें',
  saving: 'सहेजा जा रहा है…',
  saved: 'प्रोफ़ाइल अपडेट हो गई।',
  updateFailed: 'प्रोफ़ाइल अपडेट नहीं हो सकी।',
  logOut: 'लॉग आउट',
  logOutConfirm: 'लॉग आउट करें?',
  logOutBody: 'ऐप फिर खोलने के लिए लॉगिन आईडी या मोबाइल और पिन चाहिए।',
  cancel: 'रद्द करें',
  registrationTitle: 'पंजीकरण',
  formTitle: 'व्यक्तिगत विवरण',
  contactTitle: 'संपर्क विवरण',
  profileTitle: 'पता',
  securityTitle: 'सुरक्षा',
  stepPersonalTrack: '1 व्यक्तिगत',
  stepContactTrack: '2 संपर्क',
  stepProfileTrack: '3 पता',
  stepSecurityTrack: '4 सुरक्षा',
  formProgress: '{done} / 4 खंड पूर्ण ({percent}%)',
  govtPortal: 'राष्ट्रीय पोर्टल',
  legalName: 'पूरा कानूनी नाम',
  nameAadhaarHint: 'आधार कार्ड के अनुसार नाम',
  mandatory: 'आवश्यक',
  dobFull: 'जन्म तिथि (दिन/माह/वर्ष)',
  dobDay: 'दिन',
  dobMonth: 'माह',
  dobYear: 'वर्ष',
  ageMin: 'स्वतंत्र पास के लिए न्यूनतम आयु 18 वर्ष।',
  occupationHint: 'उत्सव में किसानों और मत्स्यजीवियों की पहचान के लिए।',
  occupationSelect: 'चुनें',
  privacyTitle: 'गोपनीयता',
  privacyBody: 'पंजीकरण सहकार भारती सर्वर पर संग्रहीत होता है।',
  sos: 'SOS',
  sosTitle: 'सहायता',
  sosBody: 'निकटतम सहायता केंद्र से संपर्क करें, या आपात स्थिति में 112 पर कॉल करें।',
  genderRequired: 'लिंग चुनें।',
  dobRequired: 'जन्म तिथि लिखें।',
  occupationRequired: 'व्यवसाय चुनें।',
  contactCardTitle: 'मोबाइल और ईमेल',
  contactCardBody: 'उत्सव अपडेट और सहायता के लिए पंजीकृत मोबाइल नंबर लिखें।',
  contactVerifiedHint: 'यह नंबर ओटीपी से सत्यापित है।',
  contactEmailHint: 'केवल पास और सूचनाएँ भेजने के लिए।',
  addressCardTitle: 'पता',
  addressCardBody: 'उत्सव पास के लिए अपना पता लिखें।',
  houseLabel: 'मकान / गली / पता पंक्ति',
  houseHint: 'लोकेशन से भरा जा सकता है, फिर ठीक करें।',
  localityLabel: 'गाँव / वार्ड / इलाका',
  districtLabel: 'ज़िला',
  stateLabel: 'राज्य',
  pincodeLabel: 'पिनकोड',
  useMyLocation: 'वर्तमान लोकेशन से पता भरें',
  useMyLocationHint: 'सटीक पता के लिए डिवाइस लोकेशन की अनुमति दें।',
  locatingAddress: 'लोकेशन और पता खोजा जा रहा है…',
  locationFilled: 'पता लोकेशन से भरा गया। जाँच कर ठीक करें।',
  locationDenied: 'लोकेशन अनुमति नहीं मिली। पता स्वयं लिखें।',
  locationUnavailable: 'लोकेशन नहीं मिली। पता स्वयं लिखें।',
  addressRequired: 'पता लिखें या लोकेशन से भरें।',
  securityCardTitle: 'ऐप पिन',
  securityCardBody: '4 से 6 अंकों का पिन बनाएँ। खाता बनाने से पहले यह चरण पूरा करें।',
  securityPinLabel: 'पिन',
  securityPinHint: 'कम से कम 4 अंक।',
  securityConfirmPin: 'पिन की पुष्टि',
}

const EN: AppCopy = {
  govChip: 'Government of India',
  honour: "Honouring India's food heroes",
  subtitle: 'Farmers & Fishermen Celebration',
  tagline: "A nationwide tribute to the extraordinary work of India's farmers and fishermen.",
  registerNow: 'Register Now',
  alreadyRegistered: 'Already registered? Sign In',
  verifyMobile: 'Verify mobile',
  mobileTitle: 'Mobile number',
  mobileBody:
    'Enter your mobile number before registration. Send the OTP, verify it, and the personal details page will open.',
  mobileLabel: 'Mobile number',
  mobileHint: '10-digit Indian mobile number.',
  sendOtp: 'Send OTP',
  verifyOtp: 'Verify OTP',
  yourOtp: 'Your OTP is',
  otpLabel: 'OTP',
  otpHint: 'Enter the 6-digit code on this page.',
  resendOtp: 'Resend OTP',
  mobileRequired: 'Enter a valid 10-digit Indian mobile number.',
  otpRequired: 'Enter the 6-digit OTP.',
  otpWrong: 'That OTP is incorrect.',
  otpSmsHint: '6-digit code from the SMS.',
  otpSent: 'OTP sent. It expires in 5 minutes.',
  otpSendFailed: 'Could not send the OTP.',
  otpSendFirst: 'Send an OTP first.',
  otpVerified: 'Mobile number verified.',
  otpVerifyFailed: 'Could not verify the OTP.',
  otpVerifyRequired: 'Verify the OTP sent to this mobile number.',
  pleaseWait: 'Please wait…',
  signIn: 'Sign In',
  accessTitle: 'Registered participant access',
  accessBody: 'Registration and sign in are required before anyone can use the app.',
  email: 'Email',
  emailOptional: 'Email (optional)',
  emailHint: 'Use the email from registration.',
  password: 'Password',
  createAccount: 'Create account',
  enterEmailPassword: 'Enter your email and password.',
  loginIdLabel: 'Login ID / mobile',
  loginIdHint: 'Use the login ID from registration or your 10-digit mobile number.',
  pinLabel: 'PIN',
  enterLoginPin: 'Enter your login ID and PIN.',
  signingIn: 'Signing in…',
  register: 'Register',
  continue: 'Continue',
  createAccountBtn: 'Create Account',
  registering: 'Registering…',
  stepPersonal: 'Personal',
  stepContact: 'Contact',
  stepAddress: 'Address',
  stepPassword: 'PIN',
  fullName: 'Full name',
  gender: 'Gender',
  male: 'Male',
  female: 'Female',
  other: 'Other',
  dob: 'Date of birth',
  dobHint: 'Optional. Participants must be 18 or older.',
  occupation: 'Occupation',
  farmer: 'Farmer',
  fisherman: 'Fisherman',
  both: 'Both',
  allied: 'Allied',
  mobile: 'Mobile',
  verifiedOtp: 'Verified with OTP.',
  address: 'Address',
  locality: 'Locality',
  district: 'District',
  state: 'State',
  pincode: 'PIN code',
  createPassword: 'Set a PIN',
  confirmPassword: 'Confirm PIN',
  passwordHint: '4 to 6 digits. Needed every time you sign in.',
  showPin: 'Show PIN',
  hidePin: 'Hide PIN',
  fullNameRequired: 'Full name is required.',
  minAge: 'Minimum age is 18.',
  validEmail: 'Enter a valid email address.',
  pinDigits: 'PIN code must be 6 digits.',
  passwordLength: 'Create a PIN of at least 4 digits.',
  passwordMatch: 'PIN values do not match.',
  emailSignInHint: 'Optional. You sign in with your mobile number.',
  registrationFailed: 'Registration failed.',
  signInFailed: 'Sign in failed.',
  stepOf: 'Step {current} of {total}',
  chooseLanguage: 'Choose language',
  searchLanguages: 'Search languages',
  noLanguages: 'No languages found.',
  back: 'Back',
  home: 'Home',
  profile: 'Profile',
  registered: 'Registered',
  accountReady: 'Account ready',
  keepLoginId: 'Keep this login ID',
  keepLoginIdBody: 'Keep this ID for your records. Sign in with your login ID or mobile and PIN.',
  loginId: 'Login ID',
  goHome: 'Go to Home',
  jaiKisan: 'Jai Kisan',
  registeredParticipant: 'Registered participant',
  account: 'Account',
  yourProfile: 'Your Rath Yatra profile',
  profileStatus: 'Status: {status}. Sign in with your login ID or mobile and PIN, and update your details from Profile.',
  mobileOnFile: 'Mobile on file',
  mobileOnFileBody: 'This number is on file for your Rath Yatra profile.',
  loadFailed: 'Could not load profile.',
  participant: 'Participant',
  editDetails: 'Edit details',
  updateProfile: 'Update profile',
  saveChanges: 'Save changes',
  saving: 'Saving…',
  saved: 'Profile updated.',
  updateFailed: 'Could not update profile.',
  logOut: 'Log out',
  logOutConfirm: 'Log out?',
  logOutBody: 'You will need your login ID or mobile and PIN to open the app again.',
  cancel: 'Cancel',
  registrationTitle: 'Registration',
  formTitle: 'Personal Details',
  contactTitle: 'Contact Details',
  profileTitle: 'Address',
  securityTitle: 'Security',
  stepPersonalTrack: '1 Personal',
  stepContactTrack: '2 Contact',
  stepProfileTrack: '3 Address',
  stepSecurityTrack: '4 Security',
  formProgress: '{done} OF 4 SECTIONS COMPLETE ({percent}%)',
  govtPortal: 'National Portal',
  legalName: 'FULL LEGAL NAME',
  nameAadhaarHint: 'Name as per aadhaar card',
  mandatory: 'Required',
  dobFull: 'DATE OF BIRTH (DD/MM/YYYY)',
  dobDay: 'Date',
  dobMonth: 'Month',
  dobYear: 'Year',
  ageMin: 'Minimum age 18 for an independent pass.',
  occupationHint: 'Used to recognise farmers and fishermen at the events.',
  occupationSelect: 'Select',
  privacyTitle: 'Privacy',
  privacyBody: 'Registration is stored on the Sahakar Bharati server.',
  sos: 'SOS',
  sosTitle: 'Help',
  sosBody: 'Use the nearest event help desk or call 112 in an emergency.',
  genderRequired: 'Select a gender.',
  dobRequired: 'Enter your date of birth.',
  occupationRequired: 'Select an occupation.',
  contactCardTitle: 'Mobile and email',
  contactCardBody: 'Add a registered mobile number so celebration updates and help can reach you.',
  contactVerifiedHint: 'This number is verified with OTP.',
  contactEmailHint: 'Used only to send the pass and notices.',
  addressCardTitle: 'Address',
  addressCardBody: 'Add the address for your participant pass.',
  houseLabel: 'HOUSE / STREET / ADDRESS LINE',
  houseHint: 'Filled from location when possible. Edit if needed.',
  localityLabel: 'VILLAGE / WARD / LOCALITY',
  districtLabel: 'DISTRICT',
  stateLabel: 'STATE',
  pincodeLabel: 'PINCODE',
  useMyLocation: 'Fill address from current location',
  useMyLocationHint: 'Allow location so the address matches where you are.',
  locatingAddress: 'Finding location and address…',
  locationFilled: 'Address filled from location. Check and correct it.',
  locationDenied: 'Location permission denied. Enter the address yourself.',
  locationUnavailable: 'Location unavailable. Enter the address yourself.',
  addressRequired: 'Enter an address or fill it from location.',
  securityCardTitle: 'App PIN',
  securityCardBody: 'Create a 4 to 6 digit PIN. The account is created only after this step.',
  securityPinLabel: 'PIN',
  securityPinHint: 'At least 4 digits.',
  securityConfirmPin: 'CONFIRM PIN',
}

const INDIC_HINDI_FALLBACK: LangCode[] = ['mai', 'bho', 'raj', 'doi', 'ne', 'kok', 'sa']

export function occupationLabel(copy: AppCopy, occupation: string): string {
  if (occupation === 'Farmer') return copy.farmer
  if (occupation === 'Fisherman') return copy.fisherman
  if (occupation === 'Both') return copy.both
  if (occupation === 'Allied') return copy.allied
  return occupation
}

export function copyFor(code: LangCode): AppCopy {
  if (code === 'en') return EN
  if (code === 'hi') return { ...EN, ...HI }
  const overlay = TRANSLATIONS[code] ?? {}
  const base = INDIC_HINDI_FALLBACK.includes(code) ? { ...EN, ...HI } : EN
  return { ...base, ...overlay }
}
