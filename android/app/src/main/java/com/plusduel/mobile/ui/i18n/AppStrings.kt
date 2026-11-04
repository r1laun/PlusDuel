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
    fun navRankings(lang: AppLanguage) = tr(lang, "Рейтинг", "Ratings", "Ranking", "排行", "التصنيف")
    fun navShop(lang: AppLanguage) = tr(lang, "Стиль", "Style", "Estilo", "外观", "المظهر")
    fun navSettings(lang: AppLanguage) = tr(lang, "Опции", "Settings", "Ajustes", "设置", "الإعدادات")

    // Rank Titles
    fun rankTitle(lang: AppLanguage, rawTitle: String): String = when (rawTitle) {
        "Grandmaster" -> tr(lang, "Гроссмейстер", "Grandmaster", "Gran Maestro", "特级大师", "أستاذ كبير")
        "Master" -> tr(lang, "Мастер", "Master", "Maestro", "大师", "أستاذ")
        "Diamond" -> tr(lang, "Алмаз", "Diamond", "Diamante", "钻石", "ألماس")
        "Platinum" -> tr(lang, "Платина", "Platinum", "Platino", "铂金", "بلاتين")
        "Gold" -> tr(lang, "Золото", "Gold", "Oro", "黄金", "ذهبي")
        "Club" -> tr(lang, "Клуб", "Club", "Club", "俱乐部", "نادي")
        else -> tr(lang, "Новичок", "Novice", "Novato", "新手", "مبتدئ")
    }

    fun botDifficultyLabel(lang: AppLanguage, bot: BotDifficulty): String = when (bot) {
        BotDifficulty.NOVICE -> tr(lang, "Новичок · 900", "Novice · 900", "Novato · 900", "新手 · 900", "مبتدئ · 900")
        BotDifficulty.GOLD -> tr(lang, "Золото · 1120", "Gold · 1120", "Oro · 1120", "黄金 · 1120", "ذهبي · 1120")
        BotDifficulty.MASTER -> tr(lang, "Мастер · 1450", "Master · 1450", "Maestro · 1450", "大师 · 1450", "أستاذ · 1450")
        BotDifficulty.GRANDMASTER -> tr(lang, "ГМ · 1820", "GM · 1820", "GM · 1820", "大师 · 1820", "GM · 1820")
    }

    fun targetHeader(lang: AppLanguage) = tr(
        lang,
        "Цель",
        "Target",
        "Objetivo",
        "目标数",
        "الهدف"
    )

    fun useEveryDigitOnce(lang: AppLanguage, used: Int, total: Int) = tr(
        lang,
        "Использовано цифр: $used из $total",
        "Digits used: $used of $total",
        "Dígitos usados: $used de $total",
        "已用数字: $used / $total",
        "الأرقام المستخدمة: $used من $total"
    )

    fun readyToSubmitBanner(lang: AppLanguage) = tr(
        lang,
        "Цель собрана — нажмите Готово",
        "Target reached — tap Submit",
        "Objetivo listo — pulsa Enviar",
        "已达成目标 — 点击提交",
        "الهدف جاهز — اضغط إرسال"
    )

    fun operatorsHint(lang: AppLanguage) = tr(
        lang,
        "+ − × ÷ ( ) ^ √ ! · склейка цифр разрешена",
        "+ − × ÷ ( ) ^ √ ! · concatenation allowed",
        "+ − × ÷ ( ) ^ √ ! · unir dígitos permitido",
        "+ − × ÷ ( ) ^ √ ! · 允许数字拼接",
        "+ − × ÷ ( ) ^ √ ! · دمج الأرقام مسموح"
    )

    fun tapTilesPlaceholder(lang: AppLanguage) = tr(
        lang,
        "Соберите формулу",
        "Build expression",
        "Crea la expresión",
        "输入算式",
        "كوّن المعادلة"
    )

    fun clearBtn(lang: AppLanguage) = tr(lang, "Сброс", "Clear", "Borrar", "清空", "مسح")
    fun submitBtn(lang: AppLanguage) = tr(lang, "Готово", "Submit", "Enviar", "提交", "إرسال")

    fun validationError(
        lang: AppLanguage,
        code: ValidationErrorCode?,
        detail: String,
        target: Int
    ): String = when (code) {
        ValidationErrorCode.EMPTY -> tr(lang, "Введите формулу", "Enter an expression", "Introduce una expresión", "请输入算式", "أدخل معادلة")
        ValidationErrorCode.INVALID_CHAR -> tr(lang, "Недопустимый символ", "Invalid character", "Carácter inválido", "无效字符", "حرف غير صالح")
        ValidationErrorCode.LEADING_ZERO -> tr(lang, "Ведущий ноль ($detail)", "Leading zero ($detail)", "Cero inicial ($detail)", "前导零 ($detail)", "صفر بادئ ($detail)")
        ValidationErrorCode.SQRT_NEEDS_ARG -> tr(lang, "После √ нужно число", "√ requires a number", "Falta número tras √", "√ 后需数字", "أدخل رقماً بعد √")
        ValidationErrorCode.UNBALANCED_PARENS -> tr(lang, "Незакрытые скобки", "Unbalanced parentheses", "Paréntesis sin cerrar", "括号不匹配", "أقواس غير متوازنة")
        ValidationErrorCode.UNUSED_DIGITS -> tr(lang, "Остались цифры: $detail", "Unused digits: $detail", "Dígitos sin usar: $detail", "未用数字: $detail", "أرقام متبقية: $detail")
        ValidationErrorCode.EXTRA_DIGITS -> tr(lang, "Лишние цифры: $detail", "Extra digits: $detail", "Dígitos extra: $detail", "多余数字: $detail", "أرقام إضافية: $detail")
        ValidationErrorCode.NON_EXACT_INTERMEDIATE -> tr(lang, "Нецелое деление или корень", "Non-integer division or root", "División o raíz no entera", "非整数结果", "قيمة غير صحيحة")
        ValidationErrorCode.NON_INTEGER_RESULT -> tr(lang, "Дробь ($detail) вместо $target", "Fraction ($detail), target is $target", "Fracción ($detail), objetivo $target", "分数 ($detail)，目标 $target", "كسر ($detail)، الهدف $target")
        ValidationErrorCode.WRONG_VALUE -> tr(lang, "$detail вместо $target", "$detail, target is $target", "$detail, objetivo $target", "$detail，目标 $target", "$detail، الهدف $target")
        else -> tr(lang, "Ошибка в записи формулы", "Syntax error", "Error de sintaxis", "算式语法错误", "خطأ في الصيغة")
    }
}
