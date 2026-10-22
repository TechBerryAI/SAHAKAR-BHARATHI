# -*- coding: utf-8 -*-
"""Merge splash strings with full UI copy and emit web + Android packs."""
from __future__ import annotations

import re
from pathlib import Path

WEB = Path(__file__).resolve().parents[1]
TS = WEB / "src" / "lib" / "translations.ts"
KT = WEB.parent / "frontend" / "app" / "src" / "main" / "java" / "com" / "example" / "sahakarbharati" / "core" / "copy" / "LanguagePacks.kt"

HI_FORM = {
    "verifyMobile": "मोबाइल सत्यापित करें",
    "mobileTitle": "मोबाइल नंबर",
    "mobileBody": "पंजीकरण से पहले अपना मोबाइल नंबर लिखें। ओटीपी भेजें, सत्यापित करें, फिर व्यक्तिगत विवरण खुलेगा।",
    "mobileLabel": "मोबाइल नंबर",
    "mobileHint": "10 अंकों का भारतीय मोबाइल नंबर।",
    "sendOtp": "ओटीपी भेजें",
    "verifyOtp": "ओटीपी सत्यापित करें",
    "yourOtp": "आपका ओटीपी है",
    "otpLabel": "ओटीपी",
    "otpHint": "इस पृष्ठ पर दिखाया गया 6 अंकों का कोड लिखें।",
    "resendOtp": "ओटीपी फिर भेजें",
    "mobileRequired": "मान्य 10 अंकों का भारतीय मोबाइल नंबर लिखें।",
    "otpRequired": "6 अंकों का ओटीपी लिखें।",
    "otpWrong": "यह ओटीपी गलत है।",
    "signIn": "साइन इन",
    "accessTitle": "पंजीकृत प्रतिभागी प्रवेश",
    "accessBody": "ऐप का उपयोग करने से पहले पंजीकरण और साइन इन आवश्यक है।",
    "email": "ईमेल",
    "emailOptional": "ईमेल (वैकल्पिक)",
    "emailHint": "पंजीकरण वाला ईमेल उपयोग करें।",
    "password": "पासवर्ड",
    "createAccount": "खाता बनाएँ",
    "enterEmailPassword": "अपना ईमेल और पासवर्ड लिखें।",
    "loginIdLabel": "लॉगिन आईडी / मोबाइल",
    "loginIdHint": "पंजीकरण के बाद मिला लॉगिन आईडी या 10 अंकों का मोबाइल।",
    "pinLabel": "पिन",
    "enterLoginPin": "लॉगिन आईडी और पिन लिखें।",
    "signingIn": "साइन इन हो रहा है…",
    "register": "पंजीकरण",
    "continue": "आगे बढ़ें",
    "createAccountBtn": "खाता बनाएँ",
    "registering": "पंजीकरण हो रहा है…",
    "stepPersonal": "व्यक्तिगत",
    "stepContact": "संपर्क",
    "stepAddress": "पता",
    "stepPassword": "पिन",
    "fullName": "पूरा नाम",
    "gender": "लिंग",
    "male": "पुरुष",
    "female": "महिला",
    "other": "अन्य",
    "dob": "जन्म तिथि",
    "dobHint": "वैकल्पिक। आयु 18 वर्ष या अधिक होनी चाहिए।",
    "occupation": "व्यवसाय",
    "farmer": "किसान",
    "fisherman": "मछुआरा",
    "both": "दोनों",
    "allied": "संबद्ध",
    "mobile": "मोबाइल",
    "verifiedOtp": "ओटीपी से सत्यापित।",
    "address": "पता",
    "locality": "इलाका",
    "district": "ज़िला",
    "state": "राज्य",
    "pincode": "पिन कोड",
    "createPassword": "पिन सेट करें",
    "confirmPassword": "पिन की पुष्टि",
    "passwordHint": "4 से 6 अंक। हर साइन इन पर यही पिन लगेगा।",
    "showPin": "पिन दिखाएँ",
    "hidePin": "पिन छिपाएँ",
    "fullNameRequired": "पूरा नाम आवश्यक है।",
    "minAge": "न्यूनतम आयु 18 वर्ष है।",
    "validEmail": "मान्य ईमेल पता लिखें।",
    "pinDigits": "पिन कोड 6 अंकों का होना चाहिए।",
    "passwordLength": "कम से कम 4 अंकों का पिन बनाएँ।",
    "passwordMatch": "पिन मेल नहीं खाते।",
    "emailSignInHint": "वैकल्पिक। साइन इन मोबाइल नंबर से होगा।",
    "registrationFailed": "पंजीकरण नहीं हो सका।",
    "signInFailed": "साइन इन नहीं हो सका।",
    "stepOf": "चरण {current} / {total}",
    "chooseLanguage": "भाषा चुनें",
    "searchLanguages": "भाषा खोजें",
    "noLanguages": "कोई भाषा नहीं मिली।",
    "back": "वापस",
    "home": "होम",
    "profile": "प्रोफ़ाइल",
    "registered": "पंजीकृत",
    "accountReady": "खाता तैयार",
    "keepLoginId": "यह लॉगिन आईडी रखें",
    "keepLoginIdBody": "यह आईडी संभालकर रखें। साइन इन लॉगिन आईडी या मोबाइल और पिन से होगा।",
    "loginId": "लॉगिन आईडी",
    "goHome": "होम पर जाएँ",
    "jaiKisan": "जय किसान",
    "registeredParticipant": "पंजीकृत प्रतिभागी",
    "account": "खाता",
    "yourProfile": "आपकी रथ यात्रा प्रोफ़ाइल",
    "profileStatus": "स्थिति: {status}। साइन इन लॉगिन आईडी या मोबाइल और पिन से करें। विवरण प्रोफ़ाइल से बदलें।",
    "mobileOnFile": "फ़ाइल में मोबाइल",
    "mobileOnFileBody": "यह नंबर आपकी रथ यात्रा प्रोफ़ाइल पर दर्ज है।",
    "loadFailed": "प्रोफ़ाइल नहीं खुल सकी।",
    "participant": "प्रतिभागी",
    "editDetails": "विवरण बदलें",
    "updateProfile": "प्रोफ़ाइल अपडेट करें",
    "saveChanges": "बदलाव सहेजें",
    "saving": "सहेजा जा रहा है…",
    "saved": "प्रोफ़ाइल अपडेट हो गई।",
    "updateFailed": "प्रोफ़ाइल अपडेट नहीं हो सकी।",
    "logOut": "लॉग आउट",
    "logOutConfirm": "लॉग आउट करें?",
    "logOutBody": "ऐप फिर खोलने के लिए लॉगिन आईडी या मोबाइल और पिन चाहिए।",
    "cancel": "रद्द करें",
}

# Language-specific replacements on top of a base form pack.
FORMS: dict[str, dict[str, str]] = {
    "bn": {
        "verifyMobile": "মোবাইল যাচাই করুন",
        "mobileTitle": "মোবাইল নম্বর",
        "mobileBody": "নিবন্ধনের আগে মোবাইল নম্বর লিখুন। ওটিপি পাঠান, যাচাই করুন, তারপর ব্যক্তিগত বিবরণ খুলবে।",
        "mobileLabel": "মোবাইল নম্বর",
        "mobileHint": "১০ অঙ্কের ভারতীয় মোবাইল নম্বর।",
        "sendOtp": "ওটিপি পাঠান",
        "verifyOtp": "ওটিপি যাচাই করুন",
        "yourOtp": "আপনার ওটিপি",
        "otpLabel": "ওটিপি",
        "otpHint": "এই পাতায় দেখা ৬ অঙ্কের কোড লিখুন।",
        "resendOtp": "ওটিপি আবার পাঠান",
        "mobileRequired": "সঠিক ১০ অঙ্কের ভারতীয় মোবাইল নম্বর লিখুন।",
        "otpRequired": "৬ অঙ্কের ওটিপি লিখুন।",
        "otpWrong": "ওটিপিটি ভুল।",
        "signIn": "সাইন ইন",
        "accessTitle": "নিবন্ধিত অংশগ্রহণকারীর প্রবেশ",
        "accessBody": "অ্যাপ ব্যবহারের আগে নিবন্ধন ও সাইন ইন প্রয়োজন।",
        "email": "ইমেল",
        "emailOptional": "ইমেল (ঐচ্ছিক)",
        "createAccount": "অ্যাকাউন্ট তৈরি করুন",
        "loginIdLabel": "লগইন আইডি / মোবাইল",
        "pinLabel": "পিন",
        "enterLoginPin": "লগইন আইডি ও পিন লিখুন।",
        "signingIn": "সাইন ইন হচ্ছে…",
        "register": "নিবন্ধন",
        "continue": "এগিয়ে যান",
        "createAccountBtn": "অ্যাকাউন্ট তৈরি করুন",
        "registering": "নিবন্ধন হচ্ছে…",
        "stepPersonal": "ব্যক্তিগত",
        "stepContact": "যোগাযোগ",
        "stepAddress": "ঠিকানা",
        "stepPassword": "পিন",
        "fullName": "পূর্ণ নাম",
        "gender": "লিঙ্গ",
        "male": "পুরুষ",
        "female": "নারী",
        "other": "অন্যান্য",
        "dob": "জন্ম তারিখ",
        "occupation": "পেশা",
        "farmer": "কৃষক",
        "fisherman": "মৎস্যজীবী",
        "both": "উভয়",
        "allied": "সহযোগী",
        "mobile": "মোবাইল",
        "address": "ঠিকানা",
        "locality": "এলাকা",
        "district": "জেলা",
        "state": "রাজ্য",
        "pincode": "পিন কোড",
        "createPassword": "পিন সেট করুন",
        "confirmPassword": "পিন নিশ্চিত করুন",
        "showPin": "পিন দেখান",
        "hidePin": "পিন লুকান",
        "chooseLanguage": "ভাষা বেছে নিন",
        "searchLanguages": "ভাষা খুঁজুন",
        "noLanguages": "কোনো ভাষা পাওয়া যায়নি।",
        "back": "ফিরে যান",
        "home": "হোম",
        "profile": "প্রোফাইল",
        "registered": "নিবন্ধিত",
        "loginId": "লগইন আইডি",
        "goHome": "হোমে যান",
        "jaiKisan": "জয় কৃষক",
        "account": "অ্যাকাউন্ট",
        "logOut": "লগ আউট",
        "cancel": "বাতিল",
    },
    "ta": {
        "verifyMobile": "மொபைலை சரிபார்க்கவும்",
        "mobileTitle": "மொபைல் எண்",
        "mobileLabel": "மொபைல் எண்",
        "sendOtp": "OTP அனுப்பவும்",
        "verifyOtp": "OTP சரிபார்க்கவும்",
        "signIn": "உள்நுழைக",
        "createAccount": "கணக்கை உருவாக்குக",
        "register": "பதிவு",
        "continue": "தொடரவும்",
        "stepPersonal": "தனிப்பட்ட",
        "stepContact": "தொடர்பு",
        "stepAddress": "முகவரி",
        "stepPassword": "பின்",
        "fullName": "முழு பெயர்",
        "gender": "பாலினம்",
        "male": "ஆண்",
        "female": "பெண்",
        "other": "மற்றவை",
        "dob": "பிறந்த தேதி",
        "occupation": "தொழில்",
        "farmer": "விவசாயி",
        "fisherman": "மீனவர்",
        "both": "இரண்டும்",
        "pinLabel": "பின்",
        "createPassword": "பின் அமைக்கவும்",
        "confirmPassword": "பின் உறுதிப்படுத்தவும்",
        "chooseLanguage": "மொழியைத் தேர்ந்தெடுக்கவும்",
        "searchLanguages": "மொழிகளைத் தேடுக",
        "home": "முகப்பு",
        "profile": "சுயவிவரம்",
        "loginId": "உள்நுழைவு ஐடி",
        "jaiKisan": "ஜெய் கிசான்",
        "logOut": "வெளியேறு",
        "cancel": "ரத்து",
        "back": "பின்செல்",
    },
    "as": {
        "signIn": "চাইন ইন",
        "createAccount": "একাউণ্ট সৃষ্টি কৰক",
        "continue": "আগলৈ যাওক",
        "register": "পঞ্জীয়ন",
        "fullName": "সম্পূৰ্ণ নাম",
        "gender": "লিংগ",
        "male": "পুৰুষ",
        "female": "মহিলা",
        "pinLabel": "পিন",
        "createPassword": "পিন ছেট কৰক",
        "confirmPassword": "পিন নিশ্চিত কৰক",
        "chooseLanguage": "ভাষা বাছক",
        "searchLanguages": "ভাষা বিচাৰক",
        "home": "হোম",
        "profile": "প্ৰ'ফাইল",
        "farmer": "কৃষক",
        "fisherman": "মাছমৰীয়া",
        "stepPersonal": "ব্যক্তিগত",
        "stepContact": "যোগাযোগ",
        "stepAddress": "ঠিকনা",
        "stepPassword": "পিন",
    },
    "ur": {
        "signIn": "سائن ان",
        "createAccount": "اکاؤنٹ بنائیں",
        "continue": "آگے بڑھیں",
        "register": "رجسٹریشن",
        "fullName": "پورا نام",
        "gender": "جنس",
        "male": "مرد",
        "female": "خاتون",
        "pinLabel": "پن",
        "createPassword": "پن سیٹ کریں",
        "confirmPassword": "پن کی تصدیق",
        "chooseLanguage": "زبان منتخب کریں",
        "searchLanguages": "زبانیں تلاش کریں",
        "home": "ہوم",
        "profile": "پروفائل",
        "farmer": "کسان",
        "fisherman": "ماہی گیر",
        "stepPersonal": "ذاتی",
        "stepContact": "رابطہ",
        "stepAddress": "پتہ",
        "stepPassword": "پن",
        "logOut": "لاگ آؤٹ",
        "back": "واپس",
    },
    "mr": {
        "signIn": "साइन इन",
        "createAccount": "खाते तयार करा",
        "continue": "पुढे जा",
        "register": "नोंदणी",
        "fullName": "पूर्ण नाव",
        "farmer": "शेतकरी",
        "fisherman": "मच्छीमार",
        "chooseLanguage": "भाषा निवडा",
        "searchLanguages": "भाषा शोधा",
        "home": "मुख्यपृष्ठ",
        "stepPersonal": "वैयक्तिक",
        "stepAddress": "पत्ता",
    },
}

HINDI_FAMILY = {"mai", "bho", "raj", "doi", "ne", "kok", "sa"}
URDU_FAMILY = {"ur", "ks", "sd"}


def parse_splash() -> dict[str, list[str]]:
    text = TS.read_text(encoding="utf-8")
    found: dict[str, list[str]] = {}
    for match in re.finditer(
        r"(\w+):\s*pack\(\s*((?:'(?:\\'|[^'])*'|\"(?:\\\"|[^\"])*\")(?:\s*,\s*)?){6}",
        text,
    ):
        code = match.group(1)
        args = re.findall(r"'(?:\\'|[^'])*'", match.group(0))
        values = [arg[1:-1].replace("\\'", "'") for arg in args[:6]]
        if len(values) == 6:
            found[code] = values
    return found


def q(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"


def form_for(code: str) -> dict[str, str]:
    if code in HINDI_FAMILY:
        base = dict(HI_FORM)
    elif code in URDU_FAMILY:
        base = dict(HI_FORM)
        base.update(FORMS.get("ur", {}))
    elif code in {"tcy"}:
        base = dict(HI_FORM)
        base.update(FORMS.get("kn", {}))
    elif code in {"mni"}:
        base = dict(HI_FORM)
        base.update(FORMS.get("bn", {}))
    elif code in {"brx"}:
        base = dict(HI_FORM)
    else:
        base = dict(HI_FORM) if code in HINDI_FAMILY else dict(HI_FORM)
        # Non-Hindi scheduled languages still get their own overlay on Hindi-quality form
        # so no English leftovers remain.
        if code not in HINDI_FAMILY:
            base = dict(HI_FORM)
    base.update(FORMS.get(code, {}))
    return base


def write_ts(splash: dict[str, list[str]]) -> None:
    lines = [
        "import type { AppCopy } from './copy.ts'",
        "import type { LangCode } from './languages.ts'",
        "",
        "export const TRANSLATIONS: Partial<Record<LangCode, Partial<AppCopy>>> = {",
    ]
    keys = (
        "govChip",
        "honour",
        "subtitle",
        "tagline",
        "registerNow",
        "alreadyRegistered",
    )
    for code, values in splash.items():
        pack = dict(zip(keys, values))
        pack.update(form_for(code))
        lines.append(f"  {code}: {{")
        for key, value in pack.items():
            lines.append(f"    {key}: {q(value)},")
        lines.append("  },")
    lines.append("}")
    lines.append("")
    TS.write_text("\n".join(lines), encoding="utf-8")


def write_kt(splash: dict[str, list[str]]) -> None:
    enum = {
        "bn": "BN",
        "te": "TE",
        "mr": "MR",
        "ta": "TA",
        "ur": "UR",
        "gu": "GU",
        "kn": "KN",
        "or": "OR",
        "ml": "ML",
        "pa": "PA",
        "as": "AS",
        "mai": "MAI",
        "sat": "SAT",
        "ks": "KS",
        "ne": "NE",
        "kok": "KOK",
        "sd": "SD",
        "doi": "DOI",
        "mni": "MNI",
        "brx": "BRX",
        "sa": "SA",
        "bho": "BHO",
        "raj": "RAJ",
        "tcy": "TCY",
    }
    out = [
        "package com.example.sahakarbharati.core.copy",
        "",
        "internal fun guestOverlay(language: AppLanguage, base: RathYatraCopy): RathYatraCopy {",
        "    val pack = overlayFor(language) ?: return base",
        "    return base.cloneCopy().apply {",
        "        portalChip = pack[0]",
        "        honourLine = pack[1]",
        "        subtitle = pack[2]",
        "        tagline = pack[3]",
        "        getStarted = pack[4]",
        "        alreadyRegistered = pack[5]",
        "        signInOfficial = pack[5]",
        "        signIn = pack[6]",
        "        createAccount = pack[7]",
        "        chooseLanguage = pack[8]",
        "        searchLanguages = pack[9]",
        "        legalName = pack[10]",
        "        gender = pack[11]",
        "        male = pack[12]",
        "        female = pack[13]",
        "        continueNext = pack[14]",
        "        farmer = pack[15]",
        "        fisherman = pack[16]",
        "        stepPersonal = pack[17]",
        "        stepContact = pack[18]",
        "        stepProfile = pack[19]",
        "        stepSecurity = pack[20]",
        "    }",
        "}",
        "",
        "private fun overlayFor(language: AppLanguage): Array<String>? = when (language) {",
        "    AppLanguage.EN, AppLanguage.HI -> null",
    ]
    for code, values in splash.items():
        name = enum.get(code)
        if not name:
            continue
        form = form_for(code)
        row = values + [
            form.get("signIn", "Sign In"),
            form.get("createAccount", "Create account"),
            form.get("chooseLanguage", "Choose language"),
            form.get("searchLanguages", "Search languages"),
            form.get("fullName", "Full name"),
            form.get("gender", "Gender"),
            form.get("male", "Male"),
            form.get("female", "Female"),
            form.get("continue", "Continue"),
            form.get("farmer", "Farmer"),
            form.get("fisherman", "Fisherman"),
            form.get("stepPersonal", "Personal"),
            form.get("stepContact", "Contact"),
            form.get("stepAddress", "Address"),
            form.get("stepPassword", "PIN"),
        ]
        quoted = ", ".join(f'"{item.replace("\\", "\\\\").replace("\"", "\\\"")}"' for item in row)
        out.append(f"    AppLanguage.{name} -> arrayOf({quoted})")
    out.append("}")
    out.append("")
    KT.write_text("\n".join(out), encoding="utf-8")


def main() -> None:
    splash = parse_splash()
    if len(splash) < 20:
        raise SystemExit(f"Failed to parse splash packs: {sorted(splash)}")
    write_ts(splash)
    write_kt(splash)
    print(f"Wrote translations for {len(splash)} languages")


if __name__ == "__main__":
    main()
