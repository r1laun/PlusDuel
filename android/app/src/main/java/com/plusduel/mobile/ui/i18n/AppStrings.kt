package com.plusduel.mobile.ui.i18n

import com.plusduel.mobile.data.AppLanguage
import com.plusduel.mobile.engine.BotDifficulty
import com.plusduel.mobile.engine.ValidationErrorCode

object AppStrings {

    fun tr(
        lang: AppLanguage,
        ru: String,
        en: String,
        es: String = en,
        zh: String = en,
        ar: String = en
    ): String = when (lang) {
        AppLanguage.RU -> ru
        AppLanguage.EN -> en
        AppLanguage.ES -> es
        AppLanguage.ZH -> zh
        AppLanguage.AR -> ar
    }

    // Bottom Navigation
    fun navArena(lang: AppLanguage) = tr(lang, "Арена", "Arena", "Arena", "对战", "الساحة")
    fun navSolo(lang: AppLanguage) = tr(lang, "Соло", "Solo", "Solo", "单人", "فردي")
    fun navRankings(lang: AppLanguage) = tr(lang, "Рейтинг", "Rank", "Ranking", "排行", "التصنيف")
    fun navShop(lang: AppLanguage) = tr(lang, "Стиль", "Style", "Estilo", "装扮", "المظهر")
    fun navSettings(lang: AppLanguage) = tr(lang, "Опции", "Options", "Ajustes", "设置", "الإعدادات")

    // Rank Titles
    fun rankTitle(lang: AppLanguage, rawTitle: String): String = when (rawTitle) {
        "Grandmaster" -> tr(lang, "Гроссмейстер", "Grandmaster", "Gran Maestro", "特级大师", "أستاذ كبير")
        "Master" -> tr(lang, "Мастер", "Master", "Maestro", "大师", "أستاذ")
        "Diamond" -> tr(lang, "Алмаз", "Diamond", "Diamante", "钻石", "ألماس")
        "Platinum" -> tr(lang, "Платина", "Platinum", "Platino", "铂金", "بلاتين")
        "Gold" -> tr(lang, "Золото", "Gold", "Oro", "黄金", "ذهبي")
        "Club" -> tr(lang, "Клубный", "Club", "Club", "俱乐部", "نادي")
        else -> tr(lang, "Новичок", "Novice", "Novato", "新手", "مبتدئ")
    }

    fun botDifficultyLabel(lang: AppLanguage, bot: BotDifficulty): String = when (bot) {
        BotDifficulty.NOVICE -> tr(lang, "Новичок · 900", "Novice · 900", "Novato · 900", "新手 · 900", "مبتدئ · 900")
        BotDifficulty.GOLD -> tr(lang, "Золото · 1120", "Gold · 1120", "Oro · 1120", "黄金 · 1120", "ذهبي · 1120")
        BotDifficulty.MASTER -> tr(lang, "Мастер · 1450", "Master · 1450", "Maestro · 1450", "大师 · 1450", "أستاذ · 1450")
        BotDifficulty.GRANDMASTER -> tr(lang, "Гроссмейстер · 1820", "Grandmaster · 1820", "Gran Maestro · 1820", "特级大师 · 1820", "أستاذ كبير · 1820")
    }

    fun targetHeader(lang: AppLanguage) = tr(
        lang,
        "ЦЕЛЕВОЕ ЧИСЛО",
        "TARGET NUMBER",
        "NÚMERO OBJETIVO",
        "目标数字",
        "الرقم المستهدف"
    )

    fun useEveryDigitOnce(lang: AppLanguage, used: Int, total: Int) = tr(
        lang,
        "Используй каждую цифру ровно 1 раз ($used/$total)",
        "Use every issued digit once ($used/$total)",
        "Usa cada dígito una sola vez ($used/$total)",
        "每个数字必须且只能用一次 ($used/$total)",
        "استخدم كل رقم مرة واحدة بالضبط ($used/$total)"
    )

    fun readyToSubmitBanner(lang: AppLanguage) = tr(
        lang,
        "✓ Цель достигнута! Жми «Готово»!",
        "✓ Target reached! Tap Submit!",
        "✓ ¡Objetivo alcanzado! ¡Pulsa Enviar!",
        "✓ 已达成目标！点击“提交”！",
        "✓ تم الوصول للهدف! اضغط «إرسال»!"
    )

    fun operatorsHint(lang: AppLanguage) = tr(
        lang,
        "Операторы: + − × ÷ ( ) ^ √ ! · Склейка цифр разрешена",
        "Operators: + − × ÷ ( ) ^ √ ! · Concatenation allowed",
        "Operadores: + − × ÷ ( ) ^ √ ! · Concatenar permitido",
        "支持运算符: + − × ÷ ( ) ^ √ ! · 允许拼接数字",
        "العمليات: + − × ÷ ( ) ^ √ ! · دمج الأرقام مسموح"
    )

    fun tapTilesPlaceholder(lang: AppLanguage) = tr(
        lang,
        "Нажимай на плитки цифр и знаков ниже…",
        "Tap digits and operator tiles below…",
        "Toca los dígitos y operadores abajo…",
        "点击下方数字与运算符构建算式…",
        "اضغط على الأرقام والعمليات بالأسفل…"
    )

    fun clearBtn(lang: AppLanguage) = tr(lang, "Сброс", "Clear", "Borrar", "清空", "مسح")
    fun submitBtn(lang: AppLanguage) = tr(lang, "Готово ✓", "Submit ✓", "Enviar ✓", "提交 ✓", "إرسال ✓")

    fun validationError(
        lang: AppLanguage,
        code: ValidationErrorCode?,
        detail: String,
        target: Int
    ): String = when (code) {
        ValidationErrorCode.EMPTY -> tr(lang, "Введите выражение", "Enter an expression", "Introduce una expresión", "请输入算式", "أدخل تعبيراً")
        ValidationErrorCode.INVALID_CHAR -> tr(lang, "Недопустимый символ в формуле", "Invalid character in expression", "Carácter no válido", "包含无效字符", "حرف غير صالح")
        ValidationErrorCode.LEADING_ZERO -> tr(lang, "Ведущий ноль запрещён ($detail)", "Leading zero not allowed ($detail)", "Cero inicial no permitido ($detail)", "不允许前导零 ($detail)", "الصفر البادئ غير مسموح ($detail)")
        ValidationErrorCode.SQRT_NEEDS_ARG -> tr(lang, "После √ должно идти число или скобки", "√ requires a number or parentheses", "√ requiere un número o paréntesis", "√ 后需要数字或括号", "√ يتطلب رقماً أو أقواساً")
        ValidationErrorCode.UNBALANCED_PARENS -> tr(lang, "Незакрытые скобки ( )", "Unbalanced parentheses ( )", "Paréntesis sin cerrar ( )", "括号不匹配 ( )", "أقواس غير متوازنة ( )")
        ValidationErrorCode.UNUSED_DIGITS -> tr(lang, "Не использованы цифры: $detail", "Unused digits: $detail", "Dígitos sin usar: $detail", "未使用的数字: $detail", "أرقام غير مستخدمة: $detail")
        ValidationErrorCode.EXTRA_DIGITS -> tr(lang, "Лишние цифры: $detail", "Extra digits used: $detail", "Dígitos extra: $detail", "多余的数字: $detail", "أرقام إضافية: $detail")
        ValidationErrorCode.NON_EXACT_INTERMEDIATE -> tr(lang, "Деление на 0 или нецелый √ / !", "Division by 0 or non-integer √ / !", "División por 0 o √ / ! no entero", "除以 0 或 √ / ! 非整数", "قسمة على صفر أو جذر غير صحيح")
        ValidationErrorCode.NON_INTEGER_RESULT -> tr(lang, "Результат дробный ($detail), нужно $target", "Result is a fraction ($detail), need $target", "Resultado fraccionario ($detail), se busca $target", "结果为分数 ($detail)，需要 $target", "النتيجة كسر ($detail)، المطلوب $target")
        ValidationErrorCode.WRONG_VALUE -> tr(lang, "Получилось $detail (нужно $target)", "Result is $detail (target is $target)", "Da $detail (objetivo $target)", "计算结果为 $detail（目标 $target）", "النتيجة $detail (الهدف $target)")
        else -> tr(lang, "Некорректная запись выражения", "Syntax error in expression", "Error de sintaxis", "算式语法错误", "خطأ في صيغة المعادلة")
    }
}
