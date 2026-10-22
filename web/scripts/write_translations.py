# -*- coding: utf-8 -*-
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "src" / "lib" / "translations.ts"

# Existing splash strings plus full guest/signed-in UI for every supported language.
PACKS: dict[str, dict[str, str]] = {}


def add(code: str, **values: str) -> None:
    PACKS[code] = values


add(
    "bn",
    govChip="ভারত সরকার",
    honour="ভারতের অন্ননায়কদের সম্মান",
    subtitle="কৃষক ও মৎস্যজীবী উৎসব",
    tagline="ভারতের কৃষক ও মৎস্যজীবীদের অসাধারণ কাজের প্রতি দেশজোড়া শ্রদ্ধা।",
    registerNow="এখনই নিবন্ধন করুন",
    alreadyRegistered="আগে নিবন্ধিত? সাইন ইন",
    verifyMobile="মোবাইল যাচাই করুন",
    mobileTitle="মোবাইল নম্বর",
    mobileBody="নিবন্ধনের আগে মোবাইল নম্বর লিখুন। ওটিপি পাঠান, যাচাই করুন, তারপর ব্যক্তিগত বিবরণ খুলবে।",
    mobileLabel="মোবাইল নম্বর",
    mobileHint="১০ অঙ্কের ভারতীয় মোবাইল নম্বর।",
    sendOtp="ওটিপি পাঠান",
    verifyOtp="ওটিপি যাচাই করুন",
    yourOtp="আপনার ওটিপি",
    otpLabel="ওটিপি",
    otpHint="এই পাতায় দেখা ৬ অঙ্কের কোড লিখুন।",
    resendOtp="ওটিপি আবার পাঠান",
    mobileRequired="সঠিক ১০ অঙ্কের ভারতীয় মোবাইল নম্বর লিখুন।",
    otpRequired="৬ অঙ্কের ওটিপি লিখুন।",
    otpWrong="ওটিপিটি ভুল।",
    signIn="সাইন ইন",
    accessTitle="নিবন্ধিত অংশগ্রহণকারীর প্রবেশ",
    accessBody="অ্যাপ ব্যবহারের আগে নিবন্ধন ও সাইন ইন প্রয়োজন।",
    email="ইমেল",
    emailOptional="ইমেল (ঐচ্ছিক)",
    emailHint="নিবন্ধনের ইমেল ব্যবহার করুন।",
    password="পাসওয়ার্ড",
    createAccount="অ্যাকাউন্ট তৈরি করুন",
    enterEmailPassword="ইমেল ও পাসওয়ার্ড লিখুন।",
    loginIdLabel="লগইন আইডি / মোবাইল",
    loginIdHint="নিবন্ধনের লগইন আইডি বা ১০ অঙ্কের মোবাইল ব্যবহার করুন।",
    pinLabel="পিন",
    enterLoginPin="লগইন আইডি ও পিন লিখুন।",
    signingIn="সাইন ইন হচ্ছে…",
    register="নিবন্ধন",
    continue_="এগিয়ে যান",
    createAccountBtn="অ্যাকাউন্ট তৈরি করুন",
    registering="নিবন্ধন হচ্ছে…",
    stepPersonal="ব্যক্তিগত",
    stepContact="যোগাযোগ",
    stepAddress="ঠিকানা",
    stepPassword="পিন",
    fullName="পূর্ণ নাম",
    gender="লিঙ্গ",
    male="পুরুষ",
    female="নারী",
    other="অন্যান্য",
    dob="জন্ম তারিখ",
    dobHint="ঐচ্ছিক। বয়স ১৮ বা তার বেশি হতে হবে।",
    occupation="পেশা",
    farmer="কৃষক",
    fisherman="মৎস্যজীবী",
    both="উভয়",
    allied="সহযোগী",
    mobile="মোবাইল",
    verifiedOtp="ওটিপিতে যাচাই হয়েছে।",
    address="ঠিকানা",
    locality="এলাকা",
    district="জেলা",
    state="রাজ্য",
    pincode="পিন কোড",
    createPassword="পিন সেট করুন",
    confirmPassword="পিন নিশ্চিত করুন",
    passwordHint="৪ থেকে ৬ অঙ্ক। প্রতি সাইন ইনে এই পিন লাগবে।",
    showPin="পিন দেখান",
    hidePin="পিন লুকান",
    fullNameRequired="পূর্ণ নাম প্রয়োজন।",
    minAge="ন্যূনতম বয়স ১৮ বছর।",
    validEmail="সঠিক ইমেল লিখুন।",
    pinDigits="পিন কোড ৬ অঙ্কের হতে হবে।",
    passwordLength="কমপক্ষে ৪ অঙ্কের পিন তৈরি করুন।",
    passwordMatch="পিন মিলছে না।",
    emailSignInHint="ঐচ্ছিক। সাইন ইন মোবাইল নম্বর দিয়ে হবে।",
    registrationFailed="নিবন্ধন হয়নি।",
    signInFailed="সাইন ইন হয়নি।",
    stepOf="ধাপ {current} / {total}",
    chooseLanguage="ভাষা বেছে নিন",
    searchLanguages="ভাষা খুঁজুন",
    noLanguages="কোনো ভাষা পাওয়া যায়নি।",
    back="ফিরে যান",
    home="হোম",
    profile="প্রোফাইল",
    registered="নিবন্ধিত",
    accountReady="অ্যাকাউন্ট প্রস্তুত",
    keepLoginId="এই লগইন আইডি রাখুন",
    keepLoginIdBody="এই আইডি সংরক্ষণ করুন। সাইন ইন লগইন আইডি বা মোবাইল ও পিন দিয়ে হবে।",
    loginId="লগইন আইডি",
    goHome="হোমে যান",
    jaiKisan="জয় কৃষক",
    registeredParticipant="নিবন্ধিত অংশগ্রহণকারী",
    account="অ্যাকাউন্ট",
    yourProfile="আপনার রথযাত্রা প্রোফাইল",
    profileStatus="অবস্থা: {status}। সাইন ইন লগইন আইডি বা মোবাইল ও পিন দিয়ে করুন। বিবরণ প্রোফাইল থেকে বদলান।",
    mobileOnFile="ফাইলে মোবাইল",
    mobileOnFileBody="এই নম্বর আপনার রথযাত্রা প্রোফাইলে আছে।",
    loadFailed="প্রোফাইল খোলা যায়নি।",
    participant="অংশগ্রহণকারী",
    editDetails="বিবরণ বদলান",
    updateProfile="প্রোফাইল আপডেট করুন",
    saveChanges="পরিবর্তন সংরক্ষণ করুন",
    saving="সংরক্ষণ হচ্ছে…",
    saved="প্রোফাইল আপডেট হয়েছে।",
    updateFailed="প্রোফাইল আপডেট হয়নি।",
    logOut="লগ আউট",
    logOutConfirm="লগ আউট করবেন?",
    logOutBody="অ্যাপ আবার খুলতে লগইন আইডি বা মোবাইল ও পিন লাগবে।",
    cancel="বাতিল",
)


def q(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"


# Shared compact packs for remaining languages. Keys match AppCopy except continue -> continue_
COMMON_KEYS = [
    "govChip", "honour", "subtitle", "tagline", "registerNow", "alreadyRegistered",
    "verifyMobile", "mobileTitle", "mobileBody", "mobileLabel", "mobileHint",
    "sendOtp", "verifyOtp", "yourOtp", "otpLabel", "otpHint", "resendOtp",
    "mobileRequired", "otpRequired", "otpWrong", "signIn", "accessTitle", "accessBody",
    "email", "emailOptional", "emailHint", "password", "createAccount", "enterEmailPassword",
    "loginIdLabel", "loginIdHint", "pinLabel", "enterLoginPin", "signingIn", "register",
    "continue_", "createAccountBtn", "registering", "stepPersonal", "stepContact",
    "stepAddress", "stepPassword", "fullName", "gender", "male", "female", "other",
    "dob", "dobHint", "occupation", "farmer", "fisherman", "both", "allied", "mobile",
    "verifiedOtp", "address", "locality", "district", "state", "pincode",
    "createPassword", "confirmPassword", "passwordHint", "showPin", "hidePin",
    "fullNameRequired", "minAge", "validEmail", "pinDigits", "passwordLength",
    "passwordMatch", "emailSignInHint", "registrationFailed", "signInFailed", "stepOf",
    "chooseLanguage", "searchLanguages", "noLanguages", "back", "home", "profile",
    "registered", "accountReady", "keepLoginId", "keepLoginIdBody", "loginId", "goHome",
    "jaiKisan", "registeredParticipant", "account", "yourProfile", "profileStatus",
    "mobileOnFile", "mobileOnFileBody", "loadFailed", "participant", "editDetails",
    "updateProfile", "saveChanges", "saving", "saved", "updateFailed", "logOut",
    "logOutConfirm", "logOutBody", "cancel",
]


def pack_from_list(code: str, values: list[str]) -> None:
    if len(values) != len(COMMON_KEYS):
        raise SystemExit(f"{code}: expected {len(COMMON_KEYS)} strings, got {len(values)}")
    PACKS[code] = dict(zip(COMMON_KEYS, values))


def load_more() -> None:
    extra = Path(__file__).with_name("language_packs.json")
    if not extra.exists():
        return
    data = json.loads(extra.read_text(encoding="utf-8"))
    for code, values in data.items():
        pack_from_list(code, values)

def emit() -> None:
    lines = [
        "import type { AppCopy } from './copy.ts'",
        "import type { LangCode } from './languages.ts'",
        "",
        "export const TRANSLATIONS: Partial<Record<LangCode, Partial<AppCopy>>> = {",
    ]
    for code, pack in PACKS.items():
        lines.append(f"  {code}: {{")
        for key, value in pack.items():
            ts_key = "continue" if key == "continue_" else key
            lines.append(f"    {ts_key}: {q(value)},")
        lines.append("  },")
    lines.append("}")
    lines.append("")
    TARGET.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {TARGET} with {len(PACKS)} languages")


if __name__ == "__main__":
    load_more()
    emit()
