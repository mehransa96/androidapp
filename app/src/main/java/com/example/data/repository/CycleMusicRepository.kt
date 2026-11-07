package com.example.data.repository

import com.example.R
import com.example.data.model.CycleMusic
import com.example.data.model.CyclePhaseType

object CycleMusicRepository {

    val allTracks: List<CycleMusic> = listOf(
        // ================= PERIOD / MENSTRUAL PHASE =================
        CycleMusic(
            id = "period_piano_calm",
            title = "پیانو مینیمال تسکین درد",
            subtitle = "پیانوی آرام فا ماژور با پد گرم",
            phase = CyclePhaseType.PERIOD,
            durationSeconds = 64,
            resId = R.raw.period_piano_calm,
            categoryBadge = "تسکین درد لگن",
            benefitDescription = "آرپژهای نرم پیانو با تمپوی ۶۰ (هم‌آهنگ با تنفس آرام) برای استراحت در روزهای قاعدگی؛ همراه خوبی برای کیسه آب گرم و دراز کشیدن.",
            frequencyLabel = "پیانو F-maj7"
        ),
        CycleMusic(
            id = "period_rain_nature",
            title = "طبیعت آرام و باران شبانه",
            subtitle = "باران ملایم روی برگ‌ها با پد دوردست",
            phase = CyclePhaseType.PERIOD,
            durationSeconds = 64,
            resId = R.raw.period_rain_nature,
            categoryBadge = "صدای باران و دریا",
            benefitDescription = "صدای یکنواخت باران به بسیاری از افراد کمک می‌کند ذهن را از درد و فکرهای مزاحم دور کنند و راحت‌تر به خواب بروند.",
            frequencyLabel = "نویز طبیعی آرامش"
        ),
        CycleMusic(
            id = "period_frequency_528",
            title = "فرکانس معجزه‌آسا ۵۲۸ هرتز",
            subtitle = "تُن ۵۲۸ هرتز با ضربان دوگوش ۴ هرتز",
            phase = CyclePhaseType.PERIOD,
            durationSeconds = 64,
            resId = R.raw.period_frequency_528,
            categoryBadge = "فرکانس ۵۲۸Hz سلفژ",
            benefitDescription = "تُن آرام ۵۲۸ هرتز روی درون آکورد دو، با اختلاف ۴ هرتز بین گوش چپ و راست (با هدفون) و کاسه‌های آوازی؛ برای تن‌آرامی و استراحت عمیق.",
            frequencyLabel = "۵۲۸ Hz • دوگوش ۴ Hz"
        ),
        CycleMusic(
            id = "period_ambient_rest",
            title = "نوای خواب عمیق و استراحت",
            subtitle = "پد صوتی گرم برای استراحت مطلق",
            phase = CyclePhaseType.PERIOD,
            durationSeconds = 64,
            resId = R.raw.period_ambient_rest,
            categoryBadge = "خواب و استراحت عمیق",
            benefitDescription = "پدهای امبینت آهسته در می‌مینور با نت‌های شیشه‌ای دوردست؛ برای چرت کوتاه و استراحت جسمی در روزهای پرخستگی.",
            frequencyLabel = "امبینت گرم E-minor"
        ),

        // ================= FOLLICULAR PHASE =================
        CycleMusic(
            id = "follicular_morning_joy",
            title = "طراوت صبحگاهی و آواز پرندگان",
            subtitle = "گیتار فینگراستایل سل ماژور و آواز پرندگان",
            phase = CyclePhaseType.FOLLICULAR,
            durationSeconds = 60,
            resId = R.raw.follicular_morning_joy,
            categoryBadge = "انرژی صبحگاهی",
            benefitDescription = "گیتار آکوستیک ۹۶ BPM با آواز پرندگان صبح؛ همراه انرژی رو به افزایش روزهای بعد از قاعدگی برای شروع یک روز سرحال.",
            frequencyLabel = "گام شاداب G-Major"
        ),
        CycleMusic(
            id = "follicular_acoustic_start",
            title = "شروع دوباره و پویایی ذهن",
            subtitle = "گیتار ریتم، فلوت و پرکاشن سبک",
            phase = CyclePhaseType.FOLLICULAR,
            durationSeconds = 57,
            resId = R.raw.follicular_acoustic_start,
            categoryBadge = "انگیزه شروع دوباره",
            benefitDescription = "استروم گیتار ر ماژور ۱۰۰ BPM با شیکر و فلوت؛ مناسب برنامه‌ریزی هفتگی، پیاده‌روی و ورزش سبک.",
            frequencyLabel = "آکوستیک D-Major"
        ),
        CycleMusic(
            id = "follicular_positive_vibes",
            title = "ارتعاش مثبت و تمرکز خلاق",
            subtitle = "نوای کالیمبا و زنگ‌های پنتاتونیک",
            phase = CyclePhaseType.FOLLICULAR,
            durationSeconds = 60,
            resId = R.raw.follicular_positive_vibes,
            categoryBadge = "خلاقیت و انگیزه",
            benefitDescription = "ملودی کالیمبا در گام پنتاتونیک دو با پد نرم؛ برای کار خلاقانه، نوشتن و تمرکز آرام.",
            frequencyLabel = "پنتاتونیک کالیمبا"
        ),
        CycleMusic(
            id = "follicular_gentle_breeze",
            title = "نسیم ملایم بهاری",
            subtitle = "فلوت لطیف و حس امیدواری و رهایی",
            phase = CyclePhaseType.FOLLICULAR,
            durationSeconds = 60,
            resId = R.raw.follicular_gentle_breeze,
            categoryBadge = "طراوت و آرامش درون",
            benefitDescription = "ملودی فلوت روی سازهای زهی و نسیم ملایم در فا ماژور؛ حس سبکی و امید روزهای فاز فولیکولار.",
            frequencyLabel = "فلوت و نسیم بهاری"
        ),

        // ================= OVULATION PHASE =================
        CycleMusic(
            id = "ovulation_high_energy",
            title = "اوج انرژی و ریتم سرزندگی",
            subtitle = "الکتروپاپ ۱۲۰ BPM با بیس و سینت پرانرژی",
            phase = CyclePhaseType.OVULATION,
            durationSeconds = 64,
            resId = R.raw.ovulation_high_energy,
            categoryBadge = "اوج انرژی و دنس",
            benefitDescription = "ریتم چهارضرب ۱۲۰ BPM (تمپوی رایج دویدن و ایروبیک) برای روزهایی که انرژی‌تان در اوج است؛ ورزش، رقص و کارهای پرتحرک.",
            frequencyLabel = "۱۲۰ BPM الکتروپاپ"
        ),
        CycleMusic(
            id = "ovulation_empowered_beat",
            title = "قدرت درون و اعتماد‌به‌نفس",
            subtitle = "فانک ۱۰۰ BPM با بیس گرووی و پیانوی الکتریک",
            phase = CyclePhaseType.OVULATION,
            durationSeconds = 62,
            resId = R.raw.ovulation_empowered_beat,
            categoryBadge = "اعتماد‌به‌نفس و قدرت",
            benefitDescription = "گروو فانک در می‌مینور با ریف برنجی؛ برای آماده شدن، جلسه‌ها و روزهایی که می‌خواهید با اعتمادبه‌نفس حاضر شوید.",
            frequencyLabel = "بیس فانک محرک"
        ),
        CycleMusic(
            id = "ovulation_pure_joy",
            title = "جشن سرزندگی و اندورفین",
            subtitle = "آرپژ درخشان می ماژور ۱۲۰ BPM",
            phase = CyclePhaseType.OVULATION,
            durationSeconds = 64,
            resId = R.raw.ovulation_pure_joy,
            categoryBadge = "ترشح اندورفین شادی",
            benefitDescription = "آرپژهای براق، پد و ملودی زنگوله‌ای در می ماژور؛ برای حال خوب و انرژی مثبت روزهای تخمک‌گذاری.",
            frequencyLabel = "آرپژ سرزنده E-Major"
        ),
        CycleMusic(
            id = "ovulation_sunshine_groove",
            title = "رقص نور و آفتاب درخشان",
            subtitle = "گروو تابستانی ۱۱۰ BPM با ماریمبا و استیل‌درام",
            phase = CyclePhaseType.OVULATION,
            durationSeconds = 69,
            resId = R.raw.ovulation_sunshine_groove,
            categoryBadge = "نشاط و شادابی",
            benefitDescription = "ماریمبا، استیل‌درام و شیکر در دو ماژور؛ حس آفتابی و اجتماعی برای وقت گذراندن با دوستان و همسر.",
            frequencyLabel = "ریتم شاد تابستانه"
        ),

        // ================= LUTEAL PHASE & PMS =================
        CycleMusic(
            id = "luteal_lofi_chill",
            title = "بیت‌های لوفای تسکین PMS",
            subtitle = "پیانو الکتریک گرم و ریتم دنج آرامش",
            phase = CyclePhaseType.LUTEAL,
            durationSeconds = 66,
            resId = R.raw.luteal_lofi_chill,
            categoryBadge = "ضد اضطراب PMS",
            benefitDescription = "آکوردهای جاز پیانوی الکتریک ۷۲ BPM با درام سوینگ و خش‌خش صفحه گرامافون؛ برای آرام‌تر شدن حال‌وهوای روزهای قبل از پریود.",
            frequencyLabel = "Lo-Fi ۷۰ BPM"
        ),
        CycleMusic(
            id = "luteal_frequency_432",
            title = "فرکانس ۴۳۲ هرتز تعادل اعصاب",
            subtitle = "کوک ۴۳۲ هرتز با ضربان دوگوش ۶ هرتز",
            phase = CyclePhaseType.LUTEAL,
            durationSeconds = 64,
            resId = R.raw.luteal_frequency_432,
            categoryBadge = "فرکانس آرامش ۴۳۲Hz",
            benefitDescription = "پد و کاسه آوازی با کوک A=432 در لا مینور و اختلاف ملایم ۶ هرتز بین دو گوش (با هدفون)؛ برای کم کردن تنش عصرگاهی.",
            frequencyLabel = "A=432 • دوگوش ۶ Hz"
        ),
        CycleMusic(
            id = "luteal_warm_acoustic",
            title = "آوای گرم سازهای زهی",
            subtitle = "ویولنسل، سازهای زهی و گیتار در سی‌بمل ماژور",
            phase = CyclePhaseType.LUTEAL,
            durationSeconds = 64,
            resId = R.raw.luteal_warm_acoustic,
            categoryBadge = "تسکین تنش و استرس",
            benefitDescription = "مانند یک پتوی نرم و نوشیدنی گرم گیاهی، حس گرما و آرامش در روزهای خسته اواخر چرخه می‌دهد.",
            frequencyLabel = "هارمونی زهی لگاتو"
        ),
        CycleMusic(
            id = "luteal_pink_noise_zen",
            title = "نویز صورتی و الگوی تنفس ۴-۷-۸",
            subtitle = "دم ۴ ثانیه، حبس ۷ ثانیه، بازدم ۸ ثانیه",
            phase = CyclePhaseType.LUTEAL,
            durationSeconds = 76,
            resId = R.raw.luteal_pink_noise_zen,
            categoryBadge = "تنفس ضد اضطراب",
            benefitDescription = "نویز صورتی با دم بالا می‌آید، در حبس ثابت می‌ماند و با بازدم آرام می‌شود؛ زنگ‌های نرم شروع هر مرحله را اعلام می‌کنند. برای کاهش تنش و خواب راحت‌تر.",
            frequencyLabel = "نویز صورتی + تنفس ۴-۷-۸"
        )
    )

    fun getTracksForPhase(phase: CyclePhaseType): List<CycleMusic> {
        return allTracks.filter { it.phase == phase }
    }

    fun getTrackById(id: String): CycleMusic? {
        return allTracks.find { it.id == id }
    }
}
