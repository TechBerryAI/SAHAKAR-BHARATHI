import type { LangCode } from './languages.ts'
import { TRANSLATIONS } from './translations.ts'

export type AppCopy = {
  govChip: string
  brandName: string
  appTitle: string
  welcomeTitle: string
  honour: string
  subtitle: string
  registerNow: string
  alreadyRegistered: string
  verifyMobile: string
  mobileLabel: string
  mobileHint: string
  sendOtp: string
  verifyOtp: string
  otpLabel: string
  resendOtp: string
  mobileRequired: string
  otpRequired: string
  otpSmsHint: string
  otpSent: string
  otpSendFailed: string
  otpSendFirst: string
  otpVerified: string
  otpVerifyFailed: string
  otpInvalid: string
  otpVerifyRequired: string
  pleaseWait: string
  signIn: string
  accessTitle: string
  accessBody: string
  email: string
  emailOptional: string
  loginIdLabel: string
  loginIdHint: string
  pinLabel: string
  enterLoginPin: string
  signingIn: string
  createAccountBtn: string
  registering: string
  fullName: string
  gender: string
  male: string
  female: string
  other: string
  occupation: string
  farmer: string
  fisherman: string
  both: string
  allied: string
  mobile: string
  address: string
  locality: string
  district: string
  state: string
  pincode: string
  showPin: string
  hidePin: string
  fullNameRequired: string
  minAge: string
  validEmail: string
  pinDigits: string
  passwordLength: string
  passwordMatch: string
  registrationFailed: string
  signInFailed: string
  invalidCredentials: string
  duplicateAccount: string
  mobileRegistered: string
  emailRegistered: string
  verificationExpired: string
  otpResendWait: string
  tooManyAttempts: string
  networkError: string
  chooseLanguage: string
  searchLanguages: string
  noLanguages: string
  back: string
  home: string
  profile: string
  mainNav: string
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
  statusActive: string
  statusDisabled: string
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
  dobFull: string
  dobDay: string
  dobMonth: string
  dobYear: string
  monthNames: string
  ageMin: string
  occupationHint: string
  occupationSelect: string
  sos: string
  sosTitle: string
  sosBody: string
  genderRequired: string
  dobRequired: string
  occupationRequired: string
  contactVerifiedHint: string
  contactEmailHint: string
  addressCardBody: string
  houseLabel: string
  localityLabel: string
  districtLabel: string
  stateLabel: string
  pincodeLabel: string
  districtRequired: string
  stateRequired: string
  securityCardTitle: string
  securityCardBody: string
  securityPinLabel: string
  securityPinHint: string
  securityConfirmPin: string
}

const HI: AppCopy = {
  govChip: 'भारत सरकार',
  brandName: 'सहकार भारती',
  appTitle: 'सहकार भारती रथ यात्रा',
  welcomeTitle: 'सहकार भारती में आपका स्वागत है',
  honour: 'भारत के अन्न नायकों का सम्मान',
  subtitle: 'किसान और मछुआरा उत्सव',
  registerNow: 'अभी पंजीकरण करें',
  alreadyRegistered: 'पहले से पंजीकृत हैं? साइन इन',
  verifyMobile: 'मोबाइल सत्यापित करें',
  mobileLabel: 'मोबाइल नंबर',
  mobileHint: '10 अंकों का भारतीय मोबाइल नंबर।',
  sendOtp: 'ओटीपी भेजें',
  verifyOtp: 'ओटीपी सत्यापित करें',
  otpLabel: 'ओटीपी',
  resendOtp: 'ओटीपी फिर भेजें',
  mobileRequired: 'मान्य 10 अंकों का भारतीय मोबाइल नंबर लिखें।',
  otpRequired: '6 अंकों का ओटीपी लिखें।',
  otpSmsHint: 'एसएमएस से मिला 6 अंकों का कोड।',
  otpSent: 'ओटीपी भेजा गया। यह 5 मिनट में समाप्त होगा।',
  otpSendFailed: 'ओटीपी नहीं भेजा जा सका।',
  otpSendFirst: 'पहले ओटीपी भेजें।',
  otpVerified: 'मोबाइल नंबर सत्यापित हो गया।',
  otpVerifyFailed: 'ओटीपी सत्यापित नहीं हो सका।',
  otpInvalid: 'ओटीपी गलत है या उसकी समय-सीमा समाप्त हो गई है।',
  otpVerifyRequired: 'इस मोबाइल नंबर पर भेजा गया ओटीपी सत्यापित करें।',
  pleaseWait: 'कृपया प्रतीक्षा करें…',
  signIn: 'साइन इन',
  accessTitle: 'पंजीकृत प्रतिभागी प्रवेश',
  accessBody: 'ऐप का उपयोग करने से पहले पंजीकरण और साइन इन आवश्यक है।',
  email: 'ईमेल',
  emailOptional: 'ईमेल (वैकल्पिक)',
  loginIdLabel: 'लॉगिन आईडी / मोबाइल',
  loginIdHint: 'पंजीकरण के बाद मिली लॉगिन आईडी या अपना 10 अंकों का मोबाइल नंबर उपयोग करें।',
  pinLabel: 'पिन',
  enterLoginPin: 'लॉगिन आईडी और पिन लिखें।',
  signingIn: 'साइन इन हो रहा है…',
  createAccountBtn: 'खाता बनाएँ',
  registering: 'पंजीकरण हो रहा है…',
  fullName: 'पूरा नाम',
  gender: 'लिंग',
  male: 'पुरुष',
  female: 'महिला',
  other: 'अन्य',
  occupation: 'व्यवसाय',
  farmer: 'किसान',
  fisherman: 'मछुआरा',
  both: 'दोनों',
  allied: 'संबद्ध',
  mobile: 'मोबाइल',
  address: 'पता',
  locality: 'इलाका',
  district: 'ज़िला',
  state: 'राज्य',
  pincode: 'पिन कोड',
  showPin: 'पिन दिखाएँ',
  hidePin: 'पिन छिपाएँ',
  fullNameRequired: 'पूरा नाम आवश्यक है।',
  minAge: 'न्यूनतम आयु 18 वर्ष है।',
  validEmail: 'मान्य ईमेल पता लिखें।',
  pinDigits: 'पिन कोड 6 अंकों का होना चाहिए।',
  passwordLength: 'कम से कम 4 अंकों का पिन बनाएँ।',
  passwordMatch: 'पिन मेल नहीं खाते।',
  registrationFailed: 'पंजीकरण नहीं हो सका।',
  signInFailed: 'साइन इन नहीं हो सका।',
  invalidCredentials: 'लॉगिन आईडी या पिन गलत है।',
  duplicateAccount: 'इन विवरणों से एक खाता पहले से मौजूद है।',
  mobileRegistered: 'इस मोबाइल नंबर से खाता पहले से मौजूद है। कृपया साइन इन करें।',
  emailRegistered: 'इस ईमेल पते से खाता पहले से मौजूद है।',
  verificationExpired: 'आपके मोबाइल का सत्यापन समाप्त हो गया है। कृपया अपना नंबर फिर से सत्यापित करें।',
  otpResendWait: 'नया ओटीपी माँगने से पहले कृपया एक मिनट प्रतीक्षा करें।',
  tooManyAttempts: 'बहुत अधिक प्रयास हो गए। कुछ देर बाद फिर कोशिश करें।',
  networkError: 'सर्वर से संपर्क नहीं हो सका। अपना इंटरनेट कनेक्शन जाँचें।',
  chooseLanguage: 'भाषा चुनें',
  searchLanguages: 'भाषा खोजें',
  noLanguages: 'कोई भाषा नहीं मिली।',
  back: 'वापस',
  home: 'होम',
  profile: 'प्रोफ़ाइल',
  mainNav: 'मुख्य नेविगेशन',
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
  profileStatus:
    'स्थिति: {status}। साइन इन लॉगिन आईडी या मोबाइल और पिन से करें। विवरण प्रोफ़ाइल से बदलें।',
  statusActive: 'सक्रिय',
  statusDisabled: 'निष्क्रिय',
  mobileOnFile: 'दर्ज मोबाइल नंबर',
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
  dobFull: 'जन्म तिथि (दिन/माह/वर्ष)',
  dobDay: 'दिन',
  dobMonth: 'माह',
  dobYear: 'वर्ष',
  monthNames: 'जनवरी फ़रवरी मार्च अप्रैल मई जून जुलाई अगस्त सितंबर अक्टूबर नवंबर दिसंबर',
  ageMin: 'स्वतंत्र पास के लिए न्यूनतम आयु 18 वर्ष।',
  occupationHint: 'कार्यक्रमों में किसानों और मछुआरों की पहचान के लिए।',
  occupationSelect: 'चुनें',
  sos: 'SOS',
  sosTitle: 'सहायता',
  sosBody: 'निकटतम सहायता केंद्र से संपर्क करें, या आपात स्थिति में 112 पर कॉल करें।',
  genderRequired: 'लिंग चुनें।',
  dobRequired: 'जन्म तिथि लिखें।',
  occupationRequired: 'व्यवसाय चुनें।',
  contactVerifiedHint: 'यह नंबर ओटीपी से सत्यापित है।',
  contactEmailHint: 'केवल पास और सूचनाएँ भेजने के लिए।',
  addressCardBody: 'अपने प्रतिभागी पास के लिए पता लिखें।',
  houseLabel: 'मकान / गली / पता पंक्ति',
  localityLabel: 'गाँव / वार्ड / इलाका',
  districtLabel: 'ज़िला',
  stateLabel: 'राज्य',
  pincodeLabel: 'पिनकोड',
  districtRequired: 'ज़िला लिखें।',
  stateRequired: 'राज्य लिखें।',
  securityCardTitle: 'ऐप पिन',
  securityCardBody: '4 से 6 अंकों का पिन बनाएँ। खाता इस चरण के बाद ही बनेगा।',
  securityPinLabel: 'पिन',
  securityPinHint: 'कम से कम 4 अंक।',
  securityConfirmPin: 'पिन की पुष्टि',
}

const EN: AppCopy = {
  govChip: 'Government of India',
  brandName: 'Sahakar Bharati',
  appTitle: 'Sahakar Bharati Rath Yatra',
  welcomeTitle: 'Welcome to Sahakar Bharati',
  honour: "Honouring India's food heroes",
  subtitle: 'Farmers & Fishermen Celebration',
  registerNow: 'Register Now',
  alreadyRegistered: 'Already registered? Sign In',
  verifyMobile: 'Verify mobile',
  mobileLabel: 'Mobile number',
  mobileHint: '10-digit Indian mobile number.',
  sendOtp: 'Send OTP',
  verifyOtp: 'Verify OTP',
  otpLabel: 'OTP',
  resendOtp: 'Resend OTP',
  mobileRequired: 'Enter a valid 10-digit Indian mobile number.',
  otpRequired: 'Enter the 6-digit OTP.',
  otpSmsHint: '6-digit code from the SMS.',
  otpSent: 'OTP sent. It expires in 5 minutes.',
  otpSendFailed: 'Could not send the OTP.',
  otpSendFirst: 'Send an OTP first.',
  otpVerified: 'Mobile number verified.',
  otpVerifyFailed: 'Could not verify the OTP.',
  otpInvalid: 'The OTP is incorrect or has expired.',
  otpVerifyRequired: 'Verify the OTP sent to this mobile number.',
  pleaseWait: 'Please wait…',
  signIn: 'Sign In',
  accessTitle: 'Registered participant access',
  accessBody: 'Registration and sign in are required before anyone can use the app.',
  email: 'Email',
  emailOptional: 'Email (optional)',
  loginIdLabel: 'Login ID / mobile',
  loginIdHint: 'Use the login ID from registration or your 10-digit mobile number.',
  pinLabel: 'PIN',
  enterLoginPin: 'Enter your login ID and PIN.',
  signingIn: 'Signing in…',
  createAccountBtn: 'Create Account',
  registering: 'Registering…',
  fullName: 'Full name',
  gender: 'Gender',
  male: 'Male',
  female: 'Female',
  other: 'Other',
  occupation: 'Occupation',
  farmer: 'Farmer',
  fisherman: 'Fisherman',
  both: 'Both',
  allied: 'Allied',
  mobile: 'Mobile',
  address: 'Address',
  locality: 'Locality',
  district: 'District',
  state: 'State',
  pincode: 'PIN code',
  showPin: 'Show PIN',
  hidePin: 'Hide PIN',
  fullNameRequired: 'Full name is required.',
  minAge: 'Minimum age is 18.',
  validEmail: 'Enter a valid email address.',
  pinDigits: 'PIN code must be 6 digits.',
  passwordLength: 'Create a PIN of at least 4 digits.',
  passwordMatch: 'PIN values do not match.',
  registrationFailed: 'Registration failed.',
  signInFailed: 'Sign in failed.',
  invalidCredentials: 'Login ID or PIN is incorrect.',
  duplicateAccount: 'An account with these details already exists.',
  mobileRegistered: 'An account already exists for this mobile number. Please sign in.',
  emailRegistered: 'An account already exists with this email address.',
  verificationExpired: 'Your mobile verification has expired. Please verify your number again.',
  otpResendWait: 'Please wait a minute before requesting another OTP.',
  tooManyAttempts: 'Too many attempts. Try again later.',
  networkError: 'The server could not be reached. Check your connection.',
  chooseLanguage: 'Choose language',
  searchLanguages: 'Search languages',
  noLanguages: 'No languages found.',
  back: 'Back',
  home: 'Home',
  profile: 'Profile',
  mainNav: 'Main navigation',
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
  profileStatus:
    'Status: {status}. Sign in with your login ID or mobile and PIN, and update your details from Profile.',
  statusActive: 'Active',
  statusDisabled: 'Disabled',
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
  nameAadhaarHint: 'Name as per Aadhaar card',
  dobFull: 'DATE OF BIRTH (DD/MM/YYYY)',
  dobDay: 'Day',
  dobMonth: 'Month',
  dobYear: 'Year',
  monthNames: 'January February March April May June July August September October November December',
  ageMin: 'Minimum age 18 for an independent pass.',
  occupationHint: 'Used to recognise farmers and fishermen at the events.',
  occupationSelect: 'Select',
  sos: 'SOS',
  sosTitle: 'Help',
  sosBody: 'Use the nearest event help desk or call 112 in an emergency.',
  genderRequired: 'Select a gender.',
  dobRequired: 'Enter your date of birth.',
  occupationRequired: 'Select an occupation.',
  contactVerifiedHint: 'This number is verified with OTP.',
  contactEmailHint: 'Used only to send the pass and notices.',
  addressCardBody: 'Add the address for your participant pass.',
  houseLabel: 'HOUSE / STREET / ADDRESS LINE',
  localityLabel: 'VILLAGE / WARD / LOCALITY',
  districtLabel: 'DISTRICT',
  stateLabel: 'STATE',
  pincodeLabel: 'PINCODE',
  districtRequired: 'Enter your district.',
  stateRequired: 'Enter your state.',
  securityCardTitle: 'App PIN',
  securityCardBody: 'Create a 4 to 6 digit PIN. The account is created only after this step.',
  securityPinLabel: 'PIN',
  securityPinHint: 'At least 4 digits.',
  securityConfirmPin: 'CONFIRM PIN',
}

export function occupationLabel(copy: AppCopy, occupation: string): string {
  if (occupation === 'Farmer') return copy.farmer
  if (occupation === 'Fisherman') return copy.fisherman
  if (occupation === 'Both') return copy.both
  if (occupation === 'Allied') return copy.allied
  return occupation
}

export function statusLabel(copy: AppCopy, status: string): string {
  if (status === 'active') return copy.statusActive
  if (status === 'disabled') return copy.statusDisabled
  return status
}

export function copyFor(code: LangCode): AppCopy {
  if (code === 'en') return EN
  if (code === 'hi') return HI
  return TRANSLATIONS[code]
}
