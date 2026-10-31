package com.plusduel.mobile.data

import androidx.compose.ui.graphics.Color

enum class AccentSkin(
    val id: String,
    val costCoins: Int,
    val lightAccent: Color,
    val darkAccent: Color,
    val lightTint: Color,
    val darkTint: Color,
    val nameRu: String,
    val nameEn: String,
    val nameEs: String,
    val nameZh: String,
    val nameAr: String
) {
    ORANGE(
        id = "skin_orange",
        costCoins = 0,
        lightAccent = Color(0xFFFF5C00),
        darkAccent = Color(0xFFFF7A2E),
        lightTint = Color(0xFFFFF0E6),
        darkTint = Color(0xFF2C1B10),
        nameRu = "Классический оранжевый",
        nameEn = "Classic Orange",
        nameEs = "Naranja Clásico",
        nameZh = "经典活力橙",
        nameAr = "برتقالي كلاسيكي"
    ),
    EMERALD(
        id = "skin_emerald",
        costCoins = 80,
        lightAccent = Color(0xFF0D9488),
        darkAccent = Color(0xFF2DD4BF),
        lightTint = Color(0xFFE6FFFA),
        darkTint = Color(0xFF0F2926),
        nameRu = "Изумрудный код",
        nameEn = "Matrix Emerald",
        nameEs = "Esmeralda Matriz",
        nameZh = "翡翠矩阵",
        nameAr = "زمردي رقمي"
    ),
    VIOLET(
        id = "skin_violet",
        costCoins = 120,
        lightAccent = Color(0xFF7C3AED),
        darkAccent = Color(0xFFA78BFA),
        lightTint = Color(0xFFF3E8FF),
        darkTint = Color(0xFF221538),
        nameRu = "Кибер-фиолет",
        nameEn = "Cyber Violet",
        nameEs = "Violeta Cyber",
        nameZh = "赛博紫光",
        nameAr = "بنفسجي سيبراني"
    ),
    GOLD(
        id = "skin_gold",
        costCoins = 160,
        lightAccent = Color(0xFFD97706),
        darkAccent = Color(0xFFFBBF24),
        lightTint = Color(0xFFFEF3C7),
        darkTint = Color(0xFF2E210B),
        nameRu = "Золотое сечение",
        nameEn = "Golden Ratio",
        nameEs = "Proporción Áurea",
        nameZh = "黄金分割",
        nameAr = "النسبة الذهبية"
    ),
    CYAN(
        id = "skin_cyan",
        costCoins = 200,
        lightAccent = Color(0xFF0284C7),
        darkAccent = Color(0xFF38BDF8),
        lightTint = Color(0xFFE0F2FE),
        darkTint = Color(0xFF0C2536),
        nameRu = "Арктический неон",
        nameEn = "Arctic Cyan",
        nameEs = "Cian Ártico",
        nameZh = "极地冰蓝",
        nameAr = "سماوي قطبي"
    ),
    CRIMSON(
        id = "skin_crimson",
        costCoins = 250,
        lightAccent = Color(0xFFE11D48),
        darkAccent = Color(0xFFFB7185),
        lightTint = Color(0xFFFFE4E6),
        darkTint = Color(0xFF301219),
        nameRu = "Рубиновый импульс",
        nameEn = "Ruby Pulse",
        nameEs = "Pulso Rubí",
        nameZh = "绯红脉冲",
        nameAr = "نبض ياقوتي"
    );

    fun title(lang: AppLanguage): String = when (lang) {
        AppLanguage.RU -> nameRu
        AppLanguage.EN -> nameEn
        AppLanguage.ES -> nameEs
        AppLanguage.ZH -> nameZh
        AppLanguage.AR -> nameAr
    }
}

enum class TileShapeStyle(
    val id: String,
    val costCoins: Int,
    val cornerDp: Int,
    val nameRu: String,
    val nameEn: String,
    val nameEs: String,
    val nameZh: String,
    val nameAr: String
) {
    ROUNDED(
        id = "shape_rounded",
        costCoins = 0,
        cornerDp = 12,
        nameRu = "Классические плитки",
        nameEn = "Classic Rounded",
        nameEs = "Fichas Clásicas",
        nameZh = "经典圆角",
        nameAr = "مربعات كلاسيكية"
    ),
    PILL(
        id = "shape_pill",
        costCoins = 100,
        cornerDp = 26,
        nameRu = "Мягкие капсулы",
        nameEn = "Soft Capsules",
        nameEs = "Cápsulas Suaves",
        nameZh = "柔和胶囊",
        nameAr = "كبسولات دائرية"
    ),
    TECH(
        id = "shape_tech",
        costCoins = 150,
        cornerDp = 4,
        nameRu = "Строгий геометр",
        nameEn = "Sharp Geometry",
        nameEs = "Geometría Pura",
        nameZh = "硬朗几何",
        nameAr = "هندسة حادة"
    );

    fun title(lang: AppLanguage): String = when (lang) {
        AppLanguage.RU -> nameRu
        AppLanguage.EN -> nameEn
        AppLanguage.ES -> nameEs
        AppLanguage.ZH -> nameZh
        AppLanguage.AR -> nameAr
    }
}
